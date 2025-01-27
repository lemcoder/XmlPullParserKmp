/*
 * Copyright 2004 Sun Microsystems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.lemcoder.reader

import com.fleeksoft.charset.Charset
import com.fleeksoft.charset.Charsets
import com.fleeksoft.io.*
import com.fleeksoft.io.exception.IOException
import io.github.lemcoder.exceptions.XmlStreamReaderException
import kotlin.jvm.JvmOverloads

/**
 *
 * Character stream that handles (or at least attempts to) all the necessary Voodo to figure out the charset encoding of
 * the XML document within the stream.
 *
 *
 * IMPORTANT: This class is not related in any way to the org.xml.sax.XMLReader. This one IS a character stream.
 *
 *
 * All this has to be done without consuming characters from the stream, if not the XML parser will not recognized the
 * document as a valid XML. This is not 100% true, but it's close enough (UTF-8 BOM is not handled by all parsers right
 * now, XmlReader handles it and things work in all parsers).
 *
 *
 * The XmlReader class handles the charset encoding of XML documents in Files, raw streams and HTTP streams by offering
 * a wide set of constructors.
 *
 *
 * By default the charset encoding detection is lenient, the constructor with the lenient flag can be used for an script
 * (following HTTP MIME and XML specifications). All this is nicely explained by Mark Pilgrim in his blog,
 * [ Determining the character encoding of a
 * feed](http://diveintomark.org/archives/2004/02/13/xml-media-types).
 *
 * @author Alejandro Abdelnur
 * @version revision 1.17 taken on 26/06/2007 from Rome (see
 * https://rome.dev.java.net/source/browse/rome/src/java/com/sun/syndication/io/XmlReader.java)
 * @since 1.4.3
 */
@Deprecated(
    """use XmlStreamReader
  """
)
open class XmlReader : Reader {
    private var _reader: Reader? = null

    /**
     * Returns the charset encoding of the XmlReader.
     *
     *
     *
     * @return charset encoding.
     */
    var encoding: String? = null
        private set

    private var _defaultEncoding: String? = null

    /**
     * Creates a Reader for a raw InputStream.
     *
     *
     * It follows the same logic used for files.
     *
     *
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     *
     *
     *
     * @param `is` InputStream to create a Reader from.
     * @throws IOException thrown if there is a problem reading the stream.
     */
    @JvmOverloads
    constructor(inputStream: InputStream, lenient: Boolean = true) {
        _defaultEncoding = defaultEncoding
        try {
            doRawStream(inputStream, lenient)
        } catch (ex: XmlStreamReaderException) {
            if (!lenient) {
                throw ex
            } else {
                // doLenientDetection(null, ex)
            }
        }
    }

    override fun read(buf: CharArray, offset: Int, len: Int): Int {
        return _reader?.read(buf, offset, len) ?: throw Exception("Reader is null")
    }

    /**
     * Closes the XmlReader stream.
     *
     *
     *
     * @throws IOException thrown if there was a problem closing the stream.
     */
    override fun close() {
        _reader?.close()
    }

    @Throws(IOException::class)
    private fun doRawStream(`is`: InputStream, lenient: Boolean) {
        val pis: BufferedInputStream = BufferedInputStream(`is`, BUFFER_SIZE)
        val bomEnc = getBOMEncoding(pis)
        val xmlGuessEnc = getXMLGuessEncoding(pis)
        val xmlEnc = getXmlProlog(pis, xmlGuessEnc)
        val encoding = calculateRawEncoding(bomEnc, xmlGuessEnc, xmlEnc, pis)
        prepareReader(pis, encoding)
    }

    @Throws(IOException::class)
    private fun prepareReader(`is`: InputStream, encoding: String) {
        _reader = InputStreamReader(`is`, encoding)
        this.encoding = encoding
    }

    // InputStream is passed for XmlStreamReaderException creation only
    @Throws(IOException::class)
    private fun calculateRawEncoding(bomEnc: String?, xmlGuessEnc: String?, xmlEnc: String?, inputStream: InputStream): String {
        val encoding: String
        if (bomEnc == null) {
            encoding = if (xmlGuessEnc == null || xmlEnc == null) {
                _defaultEncoding ?: UTF_8
            } else if (xmlEnc == UTF_16 && (xmlGuessEnc == UTF_16BE || xmlGuessEnc == UTF_16LE)) {
                xmlGuessEnc
            } else {
                xmlEnc
            }
        } else if (bomEnc == UTF_8) {
            if (xmlGuessEnc != null && xmlGuessEnc != UTF_8) {
                throw XmlStreamReaderException(
//                    RAW_EX_1.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, inputStream
                )
            }
            if (xmlEnc != null && xmlEnc != UTF_8) {
                throw XmlStreamReaderException(
//                    RAW_EX_1.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, inputStream
                )
            }
            encoding = UTF_8
        } else if (bomEnc == UTF_16BE || bomEnc == UTF_16LE) {
            if (xmlGuessEnc != null && xmlGuessEnc != bomEnc
                || xmlEnc != null && (xmlEnc != UTF_16) && (xmlEnc != bomEnc)
            ) {
                throw XmlStreamReaderException(
//                    RAW_EX_1.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, inputStream
                )
            }
            encoding = bomEnc
        } else {
            throw XmlStreamReaderException(
//                RAW_EX_2.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, inputStream
            )
        }
        return encoding
    }

    // InputStream is passed for XmlStreamReaderException creation only
    @Throws(IOException::class)
    private fun calculateHttpEncoding(
        cTMime: String?,
        cTEnc: String?,
        bomEnc: String?,
        xmlGuessEnc: String?,
        xmlEnc: String?,
        inputStream: InputStream,
        lenient: Boolean
    ): String? {
        val encoding = if (lenient and (xmlEnc != null)) {
            xmlEnc
        } else {
            val appXml = isAppXml(cTMime)
            val textXml = isTextXml(cTMime)
            if (appXml || textXml) {
                if (cTEnc == null) {
                    if (appXml) {
                        calculateRawEncoding(bomEnc, xmlGuessEnc, xmlEnc, inputStream)
                    } else {
                        _defaultEncoding ?: US_ASCII
                    }
                } else if (bomEnc != null && (cTEnc == UTF_16BE || cTEnc == UTF_16LE)) {
                    throw XmlStreamReaderException(
//                        HTTP_EX_1.format(arrayOf<Any?>(cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc)), cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc, inputStream
                    )
                } else if (cTEnc == UTF_16) {
                    if (bomEnc != null && bomEnc.startsWith(UTF_16)) {
                        bomEnc
                    } else {
                        throw XmlStreamReaderException(
//                            HTTP_EX_2.format(arrayOf<Any?>(cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc)), cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc, inputStream
                        )
                    }
                } else {
                    cTEnc
                }
            } else {
                throw XmlStreamReaderException(
//                    HTTP_EX_3.format(arrayOf<Any?>(cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc)), cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc, inputStream
                )
            }
        }
        return encoding
    }

    companion object {
        private const val BUFFER_SIZE = 4096

        private const val UTF_8 = "UTF-8"

        private const val US_ASCII = "US-ASCII"

        private const val UTF_16BE = "UTF-16BE"

        private const val UTF_16LE = "UTF-16LE"

        private const val UTF_16 = "UTF-16"

        private const val EBCDIC = "CP1047"

        /**
         *
         * Returns the default encoding to use if none is set in HTTP content-type, XML prolog and the rules based on
         * content-type are not adequate.
         *
         *
         * If it is NULL the content-type based rules are used.
         *
         * @return the default encoding to use.
         */
        /**
         *
         * Sets the default encoding to use if none is set in HTTP content-type, XML prolog and the rules based on
         * content-type are not adequate.
         *
         *
         * If it is set to NULL the content-type based rules are used.
         *
         *
         * By default it is NULL.
         *
         * @param encoding charset encoding to default to.
         */
        var defaultEncoding: String? = null

        // returns MIME type or NULL if httpContentType is NULL
        private fun getContentTypeMime(httpContentType: String?): String? {
            var mime: String? = null
            if (httpContentType != null) {
                val i = httpContentType.indexOf(";")
                mime = (if (i == -1) httpContentType else httpContentType.substring(0, i)).trim { it <= ' ' }
            }
            return mime
        }

        private val CHARSET_PATTERN = Regex("charset=([.[^; ]]*)")

        // returns charset parameter value, NULL if not present, NULL if httpContentType is NULL
        private fun getContentTypeEncoding(httpContentType: String?): String? {
            var encoding: String? = null
            if (httpContentType != null) {
                val i = httpContentType.indexOf(";")
                if (i > -1) {
                    val postMime = httpContentType.substring(i + 1)
                    val m = CHARSET_PATTERN.matchEntire(postMime)
                    encoding = if (m != null) m.groupValues[1] else null
                    encoding = if (encoding != null) encoding.uppercase() else null
                }
            }
            return encoding
        }

        // returns the BOM in the stream, NULL if not present,
        // if there was BOM the in the stream it is consumed
        @Throws(IOException::class)
        private fun getBOMEncoding(inputStream: BufferedInputStream): String? {
            var encoding: String? = null
            val bytes = IntArray(3)
            inputStream.mark(3)
            bytes[0] = inputStream.read()
            bytes[1] = inputStream.read()
            bytes[2] = inputStream.read()

            if (bytes[0] == 0xFE && bytes[1] == 0xFF) {
                encoding = UTF_16BE
                inputStream.reset()
                inputStream.read()
                inputStream.read()
            } else if (bytes[0] == 0xFF && bytes[1] == 0xFE) {
                encoding = UTF_16LE
                inputStream.reset()
                inputStream.read()
                inputStream.read()
            } else if (bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF) {
                encoding = UTF_8
            } else {
                inputStream.reset()
            }
            return encoding
        }

        // returns the best guess for the encoding by looking the first bytes of the stream, '<?'
        @Throws(IOException::class)
        private fun getXMLGuessEncoding(`is`: BufferedInputStream): String? {
            var encoding: String? = null
            val bytes = IntArray(4)
            `is`.mark(4)
            bytes[0] = `is`.read()
            bytes[1] = `is`.read()
            bytes[2] = `is`.read()
            bytes[3] = `is`.read()
            `is`.reset()

            if (bytes[0] == 0x00 && bytes[1] == 0x3C && bytes[2] == 0x00 && bytes[3] == 0x3F) {
                encoding = UTF_16BE
            } else if (bytes[0] == 0x3C && bytes[1] == 0x00 && bytes[2] == 0x3F && bytes[3] == 0x00) {
                encoding = UTF_16LE
            } else if (bytes[0] == 0x3C && bytes[1] == 0x3F && bytes[2] == 0x78 && bytes[3] == 0x6D) {
                encoding = UTF_8
            } else if (bytes[0] == 0x4C && bytes[1] == 0x6F && bytes[2] == 0xA7 && bytes[3] == 0x94) {
                encoding = EBCDIC
            }
            return encoding
        }

        val ENCODING_PATTERN = Regex("<\\?xml.*encoding[\\s]*=[\\s]*((?:\".[^\"]*\")|(?:'.[^']*'))", RegexOption.MULTILINE)

        // returns the encoding declared in the <?xml encoding=...?>, NULL if none
        @Throws(IOException::class)
        private fun getXmlProlog(`is`: BufferedInputStream, guessedEnc: String?): String? {
            var encoding: String? = null
            if (guessedEnc != null) {
                val bytes = ByteArray(BUFFER_SIZE)
                `is`.mark(BUFFER_SIZE)
                var offset = 0
                var max = BUFFER_SIZE
                var c: Int = `is`.read(bytes, offset, max)
                var firstGT = -1
                var xmlProlog: String? = null
                while (c != -1 && firstGT == -1 && offset < BUFFER_SIZE) {
                    offset += c
                    max -= c
                    c = `is`.read(bytes, offset, max)
                    xmlProlog = Charsets.forName(guessedEnc).decode(ByteBufferFactory.wrap(bytes.copyOfRange(0, offset))).toString() // decode // FIXME
                    firstGT = xmlProlog.indexOf('>')
                }
                if (firstGT == -1) {
                    if (c == -1) {
                        throw IOException("Unexpected end of XML stream")
                    } else {
                        throw IOException("XML prolog or ROOT element not found on first $offset bytes")
                    }
                }
                val bytesRead = offset
                if (bytesRead > 0) {
                    `is`.reset()
                    val bReader: BufferedReader = BufferedReader(StringReader(xmlProlog!!.substring(0, firstGT + 1)))
                    val prolog: StringBuilder = StringBuilder()
                    var line: String? = bReader.readLine()
                    while (line != null) {
                        prolog.append(line)
                        line = bReader.readLine()
                    }
                    val m = ENCODING_PATTERN.matchEntire(prolog) // FIXME
                    if (m != null) {
                        encoding = m.groupValues[1].uppercase()
                        encoding = encoding.substring(1, encoding.length - 1)
                    }
                }
            }
            return encoding
        }

        // indicates if the MIME type belongs to the APPLICATION XML family
        private fun isAppXml(mime: String?): Boolean {
            return mime != null
                    && (mime == "application/xml"
                    || mime == "application/xml-dtd"
                    || mime == "application/xml-external-parsed-entity"
                    || (mime.startsWith("application/") && mime.endsWith("+xml")))
        }

        // indicates if the MIME type belongs to the TEXT XML family
        private fun isTextXml(mime: String?): Boolean {
            return mime != null
                    && (mime == "text/xml"
                    || mime == "text/xml-external-parsed-entity"
                    || (mime.startsWith("text/") && mime.endsWith("+xml")))
        }

//        private val RAW_EX_1: MessageFormat = MessageFormat("Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] encoding mismatch")
//
//        private val RAW_EX_2: MessageFormat = MessageFormat("Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] unknown BOM")
//
//        private val HTTP_EX_1: MessageFormat = MessageFormat(
//            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], BOM must be NULL"
//        )
//
//        private val HTTP_EX_2: MessageFormat = MessageFormat(
//            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], encoding mismatch"
//        )
//
//        private val HTTP_EX_3: MessageFormat = MessageFormat(
//            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], Invalid MIME"
//        )
    }
}