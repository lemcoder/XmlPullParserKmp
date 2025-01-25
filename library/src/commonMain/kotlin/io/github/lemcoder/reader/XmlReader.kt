package io.github.lemcoder.reader

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
class XmlReader : java.io.Reader {
    private var _reader: java.io.Reader? = null

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
     * Creates a Reader for a Path.
     *
     *
     * It looks for the UTF-8 BOM first, if none sniffs the XML prolog charset, if this is also missing defaults to
     * UTF-8.
     *
     *
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     *
     *
     *
     * @param path Path to create a Reader from.
     * @throws IOException thrown if there is a problem reading the file.
     */
    constructor(path: java.nio.file.Path) : this(java.nio.file.Files.newInputStream(path))

    /**
     * Creates a Reader for a File.
     *
     *
     * It looks for the UTF-8 BOM first, if none sniffs the XML prolog charset, if this is also missing defaults to
     * UTF-8.
     *
     *
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     *
     *
     *
     * @param file File to create a Reader from.
     * @throws IOException thrown if there is a problem reading the file.
     */
    constructor(file: java.io.File) : this(file.toPath())

    /**
     * Creates a Reader for a raw InputStream.
     *
     *
     * It follows the same logic used for files.
     *
     *
     * If lenient detection is indicated and the detection above fails as per specifications it then attempts the
     * following:
     *
     *
     * If the content type was 'text/html' it replaces it with 'text/xml' and tries the detection again.
     *
     *
     * Else if the XML prolog had a charset encoding that encoding is used.
     *
     *
     * Else if the content type had a charset encoding that encoding is used.
     *
     *
     * Else 'UTF-8' is used.
     *
     *
     * If lenient detection is indicated an XmlStreamReaderException is never thrown.
     *
     *
     *
     * @param is InputStream to create a Reader from.
     * @param lenient indicates if the charset encoding detection should be relaxed.
     * @throws IOException thrown if there is a problem reading the stream.
     * @throws XmlStreamReaderException thrown if the charset encoding could not be determined according to the specs.
     */
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
     * @param is InputStream to create a Reader from.
     * @throws IOException thrown if there is a problem reading the stream.
     */
    @JvmOverloads
    constructor(`is`: java.io.InputStream, lenient: Boolean = true) {
        _defaultEncoding = defaultEncoding
        try {
            doRawStream(`is`, lenient)
        } catch (ex: XmlStreamReaderException) {
            if (!lenient) {
                throw ex
            } else {
                doLenientDetection(null, ex)
            }
        }
    }

    /**
     * Creates a Reader using the InputStream of a URL.
     *
     *
     * If the URL is not of type HTTP and there is not 'content-type' header in the fetched data it uses the same logic
     * used for Files.
     *
     *
     * If the URL is a HTTP Url or there is a 'content-type' header in the fetched data it uses the same logic used for
     * an InputStream with content-type.
     *
     *
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     *
     *
     *
     * @param url URL to create a Reader from.
     * @throws IOException thrown if there is a problem reading the stream of the URL.
     */
    constructor(url: java.net.URL) : this(url.openConnection())

    /**
     * Creates a Reader using the InputStream of a URLConnection.
     *
     *
     * If the URLConnection is not of type HttpURLConnection and there is not 'content-type' header in the fetched data
     * it uses the same logic used for files.
     *
     *
     * If the URLConnection is a HTTP Url or there is a 'content-type' header in the fetched data it uses the same logic
     * used for an InputStream with content-type.
     *
     *
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     *
     *
     *
     * @param conn URLConnection to create a Reader from.
     * @throws IOException thrown if there is a problem reading the stream of the URLConnection.
     */
    constructor(conn: java.net.URLConnection) {
        _defaultEncoding = defaultEncoding
        val lenient = true
        if (conn is java.net.HttpURLConnection) {
            try {
                doHttpStream(conn.getInputStream(), conn.getContentType(), lenient)
            } catch (ex: XmlStreamReaderException) {
                doLenientDetection(conn.getContentType(), ex)
            }
        } else if (conn.getContentType() != null) {
            try {
                doHttpStream(conn.getInputStream(), conn.getContentType(), lenient)
            } catch (ex: XmlStreamReaderException) {
                doLenientDetection(conn.getContentType(), ex)
            }
        } else {
            try {
                doRawStream(conn.getInputStream(), lenient)
            } catch (ex: XmlStreamReaderException) {
                doLenientDetection(null, ex)
            }
        }
    }

    /**
     * Creates a Reader using an InputStream an the associated content-type header. This constructor is lenient
     * regarding the encoding detection.
     *
     *
     * First it checks if the stream has BOM. If there is not BOM checks the content-type encoding. If there is not
     * content-type encoding checks the XML prolog encoding. If there is not XML prolog encoding uses the default
     * encoding mandated by the content-type MIME type.
     *
     *
     * If lenient detection is indicated and the detection above fails as per specifications it then attempts the
     * following:
     *
     *
     * If the content type was 'text/html' it replaces it with 'text/xml' and tries the detection again.
     *
     *
     * Else if the XML prolog had a charset encoding that encoding is used.
     *
     *
     * Else if the content type had a charset encoding that encoding is used.
     *
     *
     * Else 'UTF-8' is used.
     *
     *
     * If lenient detection is indicated an XmlStreamReaderException is never thrown.
     *
     *
     *
     * @param is InputStream to create the reader from.
     * @param httpContentType content-type header to use for the resolution of the charset encoding.
     * @param lenient indicates if the charset encoding detection should be relaxed.
     * @param defaultEncoding encoding to use
     * @throws IOException thrown if there is a problem reading the file.
     * @throws XmlStreamReaderException thrown if the charset encoding could not be determined according to the specs.
     */
    /**
     * Creates a Reader using an InputStream an the associated content-type header. This constructor is lenient
     * regarding the encoding detection.
     *
     *
     * First it checks if the stream has BOM. If there is not BOM checks the content-type encoding. If there is not
     * content-type encoding checks the XML prolog encoding. If there is not XML prolog encoding uses the default
     * encoding mandated by the content-type MIME type.
     *
     *
     * If lenient detection is indicated and the detection above fails as per specifications it then attempts the
     * following:
     *
     *
     * If the content type was 'text/html' it replaces it with 'text/xml' and tries the detection again.
     *
     *
     * Else if the XML prolog had a charset encoding that encoding is used.
     *
     *
     * Else if the content type had a charset encoding that encoding is used.
     *
     *
     * Else 'UTF-8' is used.
     *
     *
     * If lenient detection is indicated an XmlStreamReaderException is never thrown.
     *
     *
     *
     * @param is InputStream to create the reader from.
     * @param httpContentType content-type header to use for the resolution of the charset encoding.
     * @param lenient indicates if the charset encoding detection should be relaxed.
     * @throws IOException thrown if there is a problem reading the file.
     * @throws XmlStreamReaderException thrown if the charset encoding could not be determined according to the specs.
     */
    /**
     * Creates a Reader using an InputStream an the associated content-type header.
     *
     *
     * First it checks if the stream has BOM. If there is not BOM checks the content-type encoding. If there is not
     * content-type encoding checks the XML prolog encoding. If there is not XML prolog encoding uses the default
     * encoding mandated by the content-type MIME type.
     *
     *
     * It does a lenient charset encoding detection, check the constructor with the lenient parameter for details.
     *
     *
     *
     * @param is InputStream to create the reader from.
     * @param httpContentType content-type header to use for the resolution of the charset encoding.
     * @throws IOException thrown if there is a problem reading the file.
     */
    @JvmOverloads
    constructor(`is`: java.io.InputStream, httpContentType: String, lenient: Boolean = true, defaultEncoding: String? = null) {
        _defaultEncoding = defaultEncoding ?: Companion.defaultEncoding
        try {
            doHttpStream(`is`, httpContentType, lenient)
        } catch (ex: XmlStreamReaderException) {
            if (!lenient) {
                throw ex
            } else {
                doLenientDetection(httpContentType, ex)
            }
        }
    }

    @Throws(java.io.IOException::class)
    private fun doLenientDetection(httpContentType: String?, ex: XmlStreamReaderException?) {
        var httpContentType = httpContentType
        var ex: XmlStreamReaderException? = ex
        if (httpContentType != null) {
            if (httpContentType.startsWith("text/html")) {
                httpContentType = httpContentType.substring("text/html".length)
                httpContentType = "text/xml$httpContentType"
                try {
                    doHttpStream(ex.getInputStream(), httpContentType, true)
                    ex = null
                } catch (ex2: XmlStreamReaderException) {
                    ex = ex2
                }
            }
        }
        if (ex != null) {
            var encoding: String = ex.getXmlEncoding()
            if (encoding == null) {
                encoding = ex.getContentTypeEncoding()
            }
            if (encoding == null) {
                encoding = _defaultEncoding ?: UTF_8
            }
            prepareReader(ex.getInputStream(), encoding)
        }
    }

    @Throws(java.io.IOException::class)
    override fun read(buf: CharArray, offset: Int, len: Int): Int {
        return _reader.read(buf, offset, len)
    }

    /**
     * Closes the XmlReader stream.
     *
     *
     *
     * @throws IOException thrown if there was a problem closing the stream.
     */
    @Throws(java.io.IOException::class)
    override fun close() {
        _reader.close()
    }

    @Throws(java.io.IOException::class)
    private fun doRawStream(`is`: java.io.InputStream, lenient: Boolean) {
        val pis: BufferedInputStream = BufferedInputStream(`is`, BUFFER_SIZE)
        val bomEnc = getBOMEncoding(pis)
        val xmlGuessEnc = getXMLGuessEncoding(pis)
        val xmlEnc = getXmlProlog(pis, xmlGuessEnc)
        val encoding = calculateRawEncoding(bomEnc, xmlGuessEnc, xmlEnc, pis)
        prepareReader(pis, encoding)
    }

    @Throws(java.io.IOException::class)
    private fun doHttpStream(`is`: java.io.InputStream, httpContentType: String, lenient: Boolean) {
        val pis: BufferedInputStream = BufferedInputStream(`is`, BUFFER_SIZE)
        val cTMime = getContentTypeMime(httpContentType)
        val cTEnc = getContentTypeEncoding(httpContentType)
        val bomEnc = getBOMEncoding(pis)
        val xmlGuessEnc = getXMLGuessEncoding(pis)
        val xmlEnc = getXmlProlog(pis, xmlGuessEnc)
        val encoding = calculateHttpEncoding(cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc, pis, lenient)
        prepareReader(pis, encoding!!)
    }

    @Throws(java.io.IOException::class)
    private fun prepareReader(`is`: java.io.InputStream, encoding: String) {
        _reader = java.io.InputStreamReader(`is`, encoding)
        this.encoding = encoding
    }

    // InputStream is passed for XmlStreamReaderException creation only
    @Throws(java.io.IOException::class)
    private fun calculateRawEncoding(bomEnc: String?, xmlGuessEnc: String?, xmlEnc: String?, `is`: java.io.InputStream): String {
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
                    RAW_EX_1.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, `is`
                )
            }
            if (xmlEnc != null && xmlEnc != UTF_8) {
                throw XmlStreamReaderException(
                    RAW_EX_1.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, `is`
                )
            }
            encoding = UTF_8
        } else if (bomEnc == UTF_16BE || bomEnc == UTF_16LE) {
            if (xmlGuessEnc != null && xmlGuessEnc != bomEnc
                || xmlEnc != null && (xmlEnc != UTF_16) && (xmlEnc != bomEnc)
            ) {
                throw XmlStreamReaderException(
                    RAW_EX_1.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, `is`
                )
            }
            encoding = bomEnc
        } else {
            throw XmlStreamReaderException(
                RAW_EX_2.format(arrayOf<Any?>(bomEnc, xmlGuessEnc, xmlEnc)), bomEnc, xmlGuessEnc, xmlEnc, `is`
            )
        }
        return encoding
    }

    // InputStream is passed for XmlStreamReaderException creation only
    @Throws(java.io.IOException::class)
    private fun calculateHttpEncoding(
        cTMime: String?,
        cTEnc: String?,
        bomEnc: String?,
        xmlGuessEnc: String?,
        xmlEnc: String?,
        `is`: java.io.InputStream,
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
                        calculateRawEncoding(bomEnc, xmlGuessEnc, xmlEnc, `is`)
                    } else {
                        _defaultEncoding ?: US_ASCII
                    }
                } else if (bomEnc != null && (cTEnc == UTF_16BE || cTEnc == UTF_16LE)) {
                    throw XmlStreamReaderException(
                        HTTP_EX_1.format(arrayOf<Any?>(cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc)),
                        cTMime,
                        cTEnc,
                        bomEnc,
                        xmlGuessEnc,
                        xmlEnc,
                        `is`
                    )
                } else if (cTEnc == UTF_16) {
                    if (bomEnc != null && bomEnc.startsWith(UTF_16)) {
                        bomEnc
                    } else {
                        throw XmlStreamReaderException(
                            HTTP_EX_2.format(arrayOf<Any?>(cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc)),
                            cTMime,
                            cTEnc,
                            bomEnc,
                            xmlGuessEnc,
                            xmlEnc,
                            `is`
                        )
                    }
                } else {
                    cTEnc
                }
            } else {
                throw XmlStreamReaderException(
                    HTTP_EX_3.format(arrayOf<Any?>(cTMime, cTEnc, bomEnc, xmlGuessEnc, xmlEnc)),
                    cTMime,
                    cTEnc,
                    bomEnc,
                    xmlGuessEnc,
                    xmlEnc,
                    `is`
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

        private val CHARSET_PATTERN: java.util.regex.Pattern = java.util.regex.Pattern.compile("charset=([.[^; ]]*)")

        // returns charset parameter value, NULL if not present, NULL if httpContentType is NULL
        private fun getContentTypeEncoding(httpContentType: String?): String? {
            var encoding: String? = null
            if (httpContentType != null) {
                val i = httpContentType.indexOf(";")
                if (i > -1) {
                    val postMime = httpContentType.substring(i + 1)
                    val m: java.util.regex.Matcher = CHARSET_PATTERN.matcher(postMime)
                    encoding = if (m.find()) m.group(1) else null
                    encoding = if (encoding != null) encoding.uppercase() else null
                }
            }
            return encoding
        }

        // returns the BOM in the stream, NULL if not present,
        // if there was BOM the in the stream it is consumed
        @Throws(java.io.IOException::class)
        private fun getBOMEncoding(`is`: BufferedInputStream): String? {
            var encoding: String? = null
            val bytes = IntArray(3)
            `is`.mark(3)
            bytes[0] = `is`.read()
            bytes[1] = `is`.read()
            bytes[2] = `is`.read()

            if (bytes[0] == 0xFE && bytes[1] == 0xFF) {
                encoding = UTF_16BE
                `is`.reset()
                `is`.read()
                `is`.read()
            } else if (bytes[0] == 0xFF && bytes[1] == 0xFE) {
                encoding = UTF_16LE
                `is`.reset()
                `is`.read()
                `is`.read()
            } else if (bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF) {
                encoding = UTF_8
            } else {
                `is`.reset()
            }
            return encoding
        }

        // returns the best guess for the encoding by looking the first bytes of the stream, '<?'
        @Throws(java.io.IOException::class)
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

        val ENCODING_PATTERN: java.util.regex.Pattern = java.util.regex.Pattern.compile("<\\?xml.*encoding[\\s]*=[\\s]*((?:\".[^\"]*\")|(?:'.[^']*'))", java.util.regex.Pattern.MULTILINE)

        // returns the encoding declared in the <?xml encoding=...?>, NULL if none
        @Throws(java.io.IOException::class)
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
                    xmlProlog = String(bytes, 0, offset, charset(guessedEnc))
                    firstGT = xmlProlog!!.indexOf('>')
                }
                if (firstGT == -1) {
                    if (c == -1) {
                        throw java.io.IOException("Unexpected end of XML stream")
                    } else {
                        throw java.io.IOException("XML prolog or ROOT element not found on first $offset bytes")
                    }
                }
                val bytesRead = offset
                if (bytesRead > 0) {
                    `is`.reset()
                    val bReader: BufferedReader = BufferedReader(java.io.StringReader(xmlProlog!!.substring(0, firstGT + 1)))
                    val prolog: java.lang.StringBuilder = java.lang.StringBuilder()
                    var line: String = bReader.readLine()
                    while (line != null) {
                        prolog.append(line)
                        line = bReader.readLine()
                    }
                    val m: java.util.regex.Matcher = ENCODING_PATTERN.matcher(prolog)
                    if (m.find()) {
                        encoding = m.group(1).uppercase()
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

        private val RAW_EX_1: MessageFormat = MessageFormat("Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] encoding mismatch")

        private val RAW_EX_2: MessageFormat = MessageFormat("Invalid encoding, BOM [{0}] XML guess [{1}] XML prolog [{2}] unknown BOM")

        private val HTTP_EX_1: MessageFormat = MessageFormat(
            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], BOM must be NULL"
        )

        private val HTTP_EX_2: MessageFormat = MessageFormat(
            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], encoding mismatch"
        )

        private val HTTP_EX_3: MessageFormat = MessageFormat(
            "Invalid encoding, CT-MIME [{0}] CT-Enc [{1}] BOM [{2}] XML guess [{3}] XML prolog [{4}], Invalid MIME"
        )
    }
}