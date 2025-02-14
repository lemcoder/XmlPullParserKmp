package io.github.lemcoder/*
* Copyright The Codehaus Foundation.
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
*/
import com.fleeksoft.charset.*
import com.fleeksoft.io.BufferedInputStream
import com.fleeksoft.io.ByteArrayInputStream
import com.fleeksoft.io.InputStream
import com.fleeksoft.io.exception.IOException
import io.github.lemcoder.TestUtilsCommon.readAllFrom
import io.github.lemcoder.reader.XmlStreamReader
import io.github.lemcoder.utils.CombinedInputStream
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails


/**
 *
 * io.github.lemcoder.XmlStreamReaderTest class.
 *
 * @author herve
 * @version $Id: $Id
 * @since 3.4.0
 */
internal class XmlStreamReaderTestCommon {
    /**
     *
     * testNoXmlHeader.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun noXmlHeader() {
        val xml = "<text>text with no XML header</text>"
        checkXmlContent(xml, "UTF-8")
        checkXmlContent(xml, "UTF-8", *BOM_UTF8.toTypedArray())
    }

    /**
     *
     * testDefaultEncoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun defaultEncoding() {
        checkXmlStreamReader(TEXT_UNICODE, null, "UTF-8")
        checkXmlStreamReader(TEXT_UNICODE, null, "UTF-8", *BOM_UTF8.toTypedArray())
    }

    /**
     *
     * testUTF8Encoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun utf8Encoding() {
        checkXmlStreamReader(TEXT_UNICODE, "UTF-8")
        checkXmlStreamReader(TEXT_UNICODE, "UTF-8", *BOM_UTF8)
    }

    /**
     *
     * testUTF16Encoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun utf16Encoding() {
//        checkXmlStreamReader(TEXT_UNICODE, "UTF-16", "UTF-16BE", null)
        checkXmlStreamReader(TEXT_UNICODE, "UTF-16", "UTF-16LE", *BOM_UTF16LE.toTypedArray())
        checkXmlStreamReader(TEXT_UNICODE, "UTF-16", "UTF-16BE", *BOM_UTF16BE.toTypedArray())
    }

    /**
     *
     * testUTF16BEEncoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun utf16beEncoding() {
        checkXmlStreamReader(TEXT_UNICODE, "UTF-16BE")
    }

    /**
     *
     * testUTF16LEEncoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun utf16leEncoding() {
        checkXmlStreamReader(TEXT_UNICODE, "UTF-16LE")
    }

    /**
     *
     * testLatin1Encoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun latin1Encoding() {
        checkXmlStreamReader(TEXT_LATIN1, "ISO-8859-1")
    }

    /**
     *
     * testLatin7Encoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun latin7Encoding() {
        checkXmlStreamReader(TEXT_LATIN7, "ISO-8859-7")
    }

    /**
     *
     * testLatin15Encoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun latin15Encoding() {
        checkXmlStreamReader(TEXT_LATIN15, "ISO-8859-15")
    }

    /**
     *
     * testEUC_JPEncoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun euc_jpEncoding() {
        checkXmlStreamReader(TEXT_EUC_JP, "EUC-JP")
    }

    /**
     *
     * testEBCDICEncoding.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    @Ignore // https://github.com/fleeksoft/fleeksoft-io/issues/5
    fun ebcdicEncoding() {
        checkXmlStreamReader("simple text in EBCDIC", "CP1047")
    }

    /**
     *
     * testInappropriateEncoding.
     *
     */
    @Test
    fun inappropriateEncoding() {
        // expected failure, since the encoding does not contain some characters
        assertFails(message = "Check should have failed, since some characters are not available in the specified encoding") {
            checkXmlStreamReader(TEXT_UNICODE, "ISO-8859-2")
        }
    }

    /**
     *
     * testEncodingAttribute.
     *
     * @throws java.io.IOException if any.
     */
    @Test
    @Throws(IOException::class)
    fun encodingAttribute() {
        var xml = "<?xml version='1.0' encoding='US-ASCII'?><element encoding='attribute value'/>"
        checkXmlContent(xml, "US-ASCII")

        xml = "<?xml version='1.0' encoding  =  'US-ASCII'  ?><element encoding='attribute value'/>"
        checkXmlContent(xml, "US-ASCII")

        xml = "<?xml version='1.0'?><element encoding='attribute value'/>"
        checkXmlContent(xml, "UTF-8")

        xml = "<?xml\nversion='1.0'\nencoding\n=\n'US-ASCII'\n?>\n<element encoding='attribute value'/>"
        checkXmlContent(xml, "US-ASCII")

        xml = "<?xml\nversion='1.0'\n?>\n<element encoding='attribute value'/>"
        checkXmlContent(xml, "UTF-8")

        xml = "<element encoding='attribute value'/>"
        checkXmlContent(xml, "UTF-8")
    }

    companion object {
        /** french  */
        private const val TEXT_LATIN1 = "eacute: \u00E9"

        /** greek  */
        private const val TEXT_LATIN7 = "alpha: \u03B1"

        /** euro support  */
        private const val TEXT_LATIN15 = "euro: \u20AC"

        /** japanese  */
        private const val TEXT_EUC_JP = "hiragana A: \u3042"

        /** Unicode: support everything  */
        private const val TEXT_UNICODE = TEXT_LATIN1 + ", " + TEXT_LATIN7 + ", " + TEXT_LATIN15 + ", " + TEXT_EUC_JP

        /** see http://unicode.org/faq/utf_bom.html#BOM  */
        private val BOM_UTF8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        private val BOM_UTF16BE = byteArrayOf(0xFE.toByte(), 0xFF.toByte())

        private val BOM_UTF16LE = byteArrayOf(0xFF.toByte(), 0xFE.toByte())

        private val BOM_UTF32BE = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFE.toByte())

        private val BOM_UTF32LE = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00.toByte(), 0x00.toByte())

        private fun createXmlContent(text: String, encoding: String?): String {
            var xmlDecl = "<?xml version=\"1.0\"?>"
            if (encoding != null) {
                xmlDecl = "<?xml version=\"1.0\" encoding=\"$encoding\"?>"
            }
            return "$xmlDecl\n<text>$text</text>"
        }

        @Throws(IOException::class)
        private fun checkXmlContent(xml: String, encoding: String) {
//            checkXmlContent(xml, encoding, null)
        }

        @Throws(IOException::class)
        private fun checkXmlContent(xml: String, encoding: String, vararg bom: Byte?) {
            val xmlContent = xml.toByteArray(Charsets.forName(encoding))
            var inputStream: InputStream = ByteArrayInputStream(xmlContent)

            if (!bom.any { it == null }) {
                val bomBytes = bom.filterNotNull().toByteArray()
                val combinedStream = CombinedInputStream(ByteArrayInputStream(bomBytes), inputStream)
                inputStream = BufferedInputStream(combinedStream)
            }
            val reader = XmlStreamReader(inputStream, false)
            // assertEquals(encoding, reader.encoding)
            assertEquals(xml, readAllFrom(reader))
        }

        @Throws(IOException::class)
        private fun checkXmlStreamReader(text: String, encoding: String?, effectiveEncoding: String) {
            checkXmlStreamReader(text, encoding, effectiveEncoding, null)
        }

        @Throws(IOException::class)
        private fun checkXmlStreamReader(text: String, encoding: String) {
            checkXmlStreamReader(text, encoding, encoding, null)
        }

        @Throws(IOException::class)
        private fun checkXmlStreamReader(text: String, encoding: String, vararg bom: Byte) {
            checkXmlStreamReader(text, encoding, encoding, *bom.toTypedArray())
        }

        @Throws(IOException::class)
        private fun checkXmlStreamReader(text: String, encoding: String?, effectiveEncoding: String, vararg bom: Byte?) {
            val xml = createXmlContent(text, encoding)
            checkXmlContent(xml, effectiveEncoding, *bom)
        }
    }
}