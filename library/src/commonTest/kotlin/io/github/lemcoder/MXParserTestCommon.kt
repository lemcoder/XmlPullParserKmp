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
import com.fleeksoft.charset.Charsets
import com.fleeksoft.charset.toByteArray
import com.fleeksoft.io.InputStreamReader
import com.fleeksoft.io.StringReader
import com.fleeksoft.io.byteInputStream
import com.fleeksoft.io.exception.IOException
import com.goncalossilva.resources.Resource
import io.github.lemcoder.TestUtilsCommon.readAllFrom
import io.github.lemcoder.exceptions.XmlPullParserException
import io.github.lemcoder.reader.XmlStreamReader
import io.github.lemcoder.utils.codePointAt
import kotlin.test.*

/**
 *
 * io.github.lemcoder.MXParserTest class.
 *
 * @author [Trygve Laugstl](mailto:trygvis@inamo.no)
 * @version $Id: $Id
 * @since 3.4.0
 */
internal class MXParserTestCommon {


    val res = "src/commonTest/resources/xml"

    /**
     * Issue 163: [Issue 163](https://github.com/codehaus-plexus/plexus-utils/issues/163)
     *
     * @throws IOException if IO error.
     *
     * @since 3.4.1
     */
    @Test
    @Throws(IOException::class)
    fun encodingISO88591InputStream() {
        try {
            val resource = Resource("$res/test-encoding-ISO-8859-1.xml").readText()
            val parser: MXParser = MXParser()
            parser.setInput(resource.byteInputStream(), null)
            while (parser.nextToken() !== XmlPullParser.END_DOCUMENT);
            assertTrue(true)
        } catch (e: XmlPullParserException) {
            fail("should not raise exception: $e")
        }
    }

    /**
     * Issue 163: [Issue 163](https://github.com/codehaus-plexus/plexus-utils/issues/163)
     *
     *
     * Another case of bug #163: File encoding information is lost after the input file is copied to a String.
     *
     * @throws IOException if IO error.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(IOException::class)
    fun encodingISO88591StringReader() {
        var xmlFileContents: String
        val resource = Resource("$res/test-encoding-ISO-8859-1.xml").readText()
        val inputStream = resource.byteInputStream()
        val reader = XmlStreamReader(inputStream, false)
        xmlFileContents = readAllFrom(reader)

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(xmlFileContents))
        while (parser.nextToken() !== XmlPullParser.END_DOCUMENT);
        assertTrue(true)
    }

    /**
     * Issue 163: [Issue 163](https://github.com/codehaus-plexus/plexus-utils/issues/163)
     *
     * Another case of bug #163: Reader generated with ReaderFactory.newReader and the right file encoding.
     *
     * @throws IOException if IO error.
     *
     * @since 3.5.2
     */
    @Test
    @Throws(IOException::class)
    fun encodingISO88591NewReader() {
        // NOTE: if using Files.newBufferedReader(path, StandardCharsets.UTF-8), the reader will throw an exception
        // because the decoder created by new InputStreamReader() is lenient while the one created by
        // Files.newBufferedReader() is not.
        try {
            val resource = Resource("$res/test-encoding-ISO-8859-1.xml").readText().byteInputStream()

            InputStreamReader(
                resource,
                Charsets.UTF8
            ).use { reader ->
                val parser: MXParser = MXParser()
                parser.setInput(reader)
                while (parser.nextToken() !== XmlPullParser.END_DOCUMENT);
                assertTrue(true)
            }
        } catch (e: XmlPullParserException) {
            fail("should not raise exception: $e")
        }
    }

    /**
     * Issue 163: [Issue 163](https://github.com/codehaus-plexus/plexus-utils/issues/163)
     *
     * Another case of bug #163: InputStream supplied with the right file encoding.
     *
     * @throws IOException if IO error.
     *
     * @since 3.5.2
     */
    @Test
    @Throws(IOException::class)
    fun encodingISO88591InputStreamEncoded() {
        try {
            val input = Resource("$res/test-encoding-ISO-8859-1.xml").readText().byteInputStream()
            val parser: MXParser = MXParser()
            parser.setInput(input, "UTF-8")
            while (parser.nextToken() !== XmlPullParser.END_DOCUMENT);
            assertTrue(true)
        } catch (e: XmlPullParserException) {
            fail("should not raise exception: $e")
        }
    }

    /**
     * Issue 163: [Issue 163](https://github.com/codehaus-plexus/plexus-utils/issues/163)
     *
     * @throws IOException if IO error.
     *
     * @since 3.4.1
     */
    @Test
    @Throws(IOException::class)
    fun encodingUTF8NewXmlReader() {
        try {
            val input = Resource("$res/test-encoding-ISO-8859-1.xml").readText().byteInputStream()
            val reader = XmlStreamReader(input, false)
            val parser: MXParser = MXParser()
            parser.setInput(reader)
            while (parser.nextToken() !== XmlPullParser.END_DOCUMENT);
            assertTrue(true)
        } catch (e: XmlPullParserException) {
            fail("should not raise exception: $e")
        }
    }

    /**
     *
     * Issue #194: Incorrect getText() after parsing the DOCDECL section
     *
     *
     * test DOCDECL text with myCustomEntity that cannot be resolved, Unix line separator.
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws IOException if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(IOException::class)
    fun docdeclTextWithEntitiesUnix() {
        testDocdeclTextWithEntities("test-entities-UNIX.xml")
    }

    /**
     *
     * Issue #194: Incorrect getText() after parsing the DOCDECL section
     *
     *
     * test DOCDECL text with myCustomEntity that cannot be resolved, DOS line separator.
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws IOException if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(IOException::class)
    fun docdeclTextWithEntitiesDOS() {
        testDocdeclTextWithEntities("test-entities-DOS.xml")
    }

    @Throws(IOException::class)
    private fun testDocdeclTextWithEntities(filename: String) {
        try {
            val inputStream = Resource("$res/$filename").readText().byteInputStream()

            val reader = XmlStreamReader(inputStream, false)
            val parser: MXParser = MXParser()
            parser.setInput(reader)
            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
            assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
            assertEquals(XmlPullParser.DOCDECL, parser.nextToken())
            assertEquals(
                """ document [
<!ENTITY flo "&#x159;">
<!ENTITY myCustomEntity "&flo;">
]""",
                parser.getText()
            )
            assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            assertEquals("document", parser.getName())
            assertEquals(XmlPullParser.TEXT, parser.next())
            fail("should fail to resolve 'myCustomEntity' entity")
        } catch (e: XmlPullParserException) {
            assertTrue(e.message!!.contains("could not resolve entity named 'myCustomEntity'"))
        }
    }

    /**
     *
     * Issue #194: Incorrect getText() after parsing the DOCDECL section
     *
     *
     * test DOCDECL text with entities appearing in attributes, Unix line separator.
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws IOException if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(IOException::class)
    fun docdeclTextWithEntitiesInAttributesUnix() {
        testDocdeclTextWithEntitiesInAttributes("test-entities-in-attr-UNIX.xml")
    }

    /**
     *
     * Issue #194: Incorrect getText() after parsing the DOCDECL section
     *
     *
     * test DOCDECL text with entities appearing in attributes, DOS line separator.
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws IOException if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(IOException::class)
    fun docdeclTextWithEntitiesInAttributesDOS() {
        testDocdeclTextWithEntitiesInAttributes("test-entities-in-attr-DOS.xml")
    }

    @Throws(IOException::class)
    private fun testDocdeclTextWithEntitiesInAttributes(filename: String) {
        try {
            val input = Resource("$res/$filename").readText().byteInputStream()
            val parser: MXParser = MXParser()
            parser.setInput(input, null)
            parser.defineEntityReplacementText("nbsp", "&#160;")
            parser.defineEntityReplacementText("Alpha", "&#913;")
            parser.defineEntityReplacementText("tritPos", "&#x1d7ed;")
            parser.defineEntityReplacementText("flo", "&#x159;")
            parser.defineEntityReplacementText("myCustomEntity", "&flo;")
            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
            assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
            assertEquals(XmlPullParser.DOCDECL, parser.nextToken())
            assertEquals(
                (""" document [
<!ENTITY nbsp   "&#160;"> <!-- no-break space = non-breaking space, U+00A0 ISOnum -->
<!ENTITY Alpha    "&#913;"> <!-- greek capital letter alpha, U+0391 -->
<!ENTITY tritPos  "&#x1d7ed;"> <!-- MATHEMATICAL SANS-SERIF BOLD DIGIT ONE -->
<!ENTITY flo "&#x159;">
<!ENTITY myCustomEntity "&flo;">
]"""),
                parser.getText()
            )
            assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            assertEquals("document", parser.getName())
            assertEquals(1, parser.getAttributeCount())
            assertEquals("name", parser.getAttributeName(0))
            assertEquals(
                "section name with entities: '&' '&#913;' '<' '&#160;' '>' '&#x1d7ed;' ''' '&#x159;' '\"'",
                parser.getAttributeValue(0)
            )

            assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
            assertEquals("myCustomEntity", parser.getName())
            assertEquals("&#x159;", parser.getText())

            assertEquals(XmlPullParser.END_TAG, parser.nextToken())
            assertEquals(XmlPullParser.END_DOCUMENT, parser.nextToken())
        } catch (e: XmlPullParserException) {
            fail("should not raise exception: $e")
        }
    }

    // non file tests
    /**
     *
     * testHexadecimalEntities.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun hexadecimalEntities() {
        val parser: MXParser = MXParser()
//        val parser: PlexusMXParser = PlexusMXParser()

        parser.defineEntityReplacementText("test", "replacement")

//        val input = "<root>&#x41;</root>"
        val input = "<root>A</root>"

        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_TAG, parser.next())
        println(parser.getName())

        assertEquals(XmlPullParser.TEXT, parser.next())
        println(parser.getName())


        assertEquals("A", parser.getText())
        println(parser.getName())

        assertEquals(XmlPullParser.END_TAG, parser.next())
    }

    /**
     *
     * testDecimalEntities.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun decimalEntities() {
        val parser: MXParser = MXParser()

        parser.defineEntityReplacementText("test", "replacement")

        val input = "<root>&#65;</root>"

        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_TAG, parser.next())

        assertEquals(XmlPullParser.TEXT, parser.next())

        assertEquals("A", parser.getText())

        assertEquals(XmlPullParser.END_TAG, parser.next())
    }

    /**
     *
     * testPredefinedEntities.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun predefinedEntities() {
        val parser: MXParser = MXParser()

        parser.defineEntityReplacementText("test", "replacement")

        val input = "<root>&lt;&gt;&amp;&apos;&quot;</root>"

        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_TAG, parser.next())

        assertEquals(XmlPullParser.TEXT, parser.next())

        assertEquals("<>&'\"", parser.getText())

        assertEquals(XmlPullParser.END_TAG, parser.next())
    }

    /**
     *
     * testEntityReplacementMap.
     *
     * @throws XmlPullParserException if any.
     * @throws IOException if any.
     */
    @Test
    @Throws(XmlPullParserException::class, IOException::class)
    fun entityReplacementMap() {
        val erm: EntityReplacementMap = EntityReplacementMap(arrayOf<Array<String>>(arrayOf<String>("abc", "CDE"), arrayOf<String>("EFG", "HIJ")))
        val parser: MXParser = MXParser(erm)

        val input = "<root>&EFG;</root>"
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_TAG, parser.next())
        assertEquals(XmlPullParser.TEXT, parser.next())
        assertEquals("HIJ", parser.getText())
        assertEquals(XmlPullParser.END_TAG, parser.next())
    }

    /**
     *
     * testCustomEntities.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun customEntities() {
        var parser: MXParser = MXParser()

        var input = "<root>&myentity;</root>"
        parser.setInput(StringReader(input))
        parser.defineEntityReplacementText("myentity", "replacement")
        assertEquals(XmlPullParser.START_TAG, parser.next())
        assertEquals(XmlPullParser.TEXT, parser.next())
        assertEquals("replacement", parser.getText())
        assertEquals(XmlPullParser.END_TAG, parser.next())

        parser = MXParser()
        input = "<root>&myCustom;</root>"
        parser.setInput(StringReader(input))
        parser.defineEntityReplacementText("fo", "&#65;")
        parser.defineEntityReplacementText("myCustom", "&fo;")
        assertEquals(XmlPullParser.START_TAG, parser.next())
        assertEquals(XmlPullParser.TEXT, parser.next())
        assertEquals("&#65;", parser.getText())
        assertEquals(XmlPullParser.END_TAG, parser.next())
    }

    /**
     *
     * testUnicodeEntities.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun unicodeEntities() {
        var parser: MXParser = MXParser()
        var input = "<root>&#x1d7ed;</root>"
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("\uD835\uDFED", parser.getText())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())

        parser = MXParser()
        input = "<root>&#x159;</root>"
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("\u0159", parser.getText())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    }

    /**
     *
     * testInvalidCharacterReferenceHexa.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun invalidCharacterReferenceHexa() {
        val parser: MXParser = MXParser()
        val input = "<root>&#x110000;</root>"
        parser.setInput(StringReader(input))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
            fail("Should fail since &#x110000; is an illegal character reference")
        } catch (e: XmlPullParserException) {
            assertTrue(e.message!!.contains("character reference (with hex value 110000) is invalid"))
        }
    }

    /**
     *
     * testValidCharacterReferenceHexa.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun validCharacterReferenceHexa() {
        val parser: MXParser = MXParser()
        val input = "<root>&#x9;&#xA;&#xD;&#x20;&#x200;&#xD7FF;&#xE000;&#xFFA2;&#xFFFD;&#x10000;&#x10FFFD;&#x10FFFF;</root>"
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0x9, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0xA, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0xD, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0x20, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0x200, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0xD7FF, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0xE000, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0xFFA2, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0xFFFD, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0x10000, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0x10FFFD, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(0x10FFFF, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    }

    /**
     *
     * testInvalidCharacterReferenceDecimal.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun invalidCharacterReferenceDecimal() {
        val parser: MXParser = MXParser()
        val input = "<root>&#1114112;</root>"
        parser.setInput(StringReader(input))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
            fail("Should fail since &#1114112; is an illegal character reference")
        } catch (e: XmlPullParserException) {
            assertTrue(e.message!!.contains("character reference (with decimal value 1114112) is invalid"))
        }
    }

    /**
     *
     * testValidCharacterReferenceDecimal.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun validCharacterReferenceDecimal() {
        val parser: MXParser = MXParser()
        val input = "<root>&#9;&#10;&#13;&#32;&#512;&#55295;&#57344;&#65442;&#65533;&#65536;&#1114109;&#1114111;</root>"
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(9, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(10, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(13, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(32, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(512, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(55295, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(57344, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(65442, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(65533, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(65536, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(1114109, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals(1114111, parser.getText()!!.codePointAt(0))
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    }

    /**
     *
     * testProcessingInstruction.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun parserPosition() {
        val input =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- A --> \n <!-- B --><test>\tnnn</test>\n<!-- C\nC -->"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertPosition(1, 39, parser)
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertPosition(1, 49, parser)
        assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
        assertPosition(2, 3, parser) // end when next token starts
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertPosition(2, 12, parser)
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertPosition(2, 18, parser)
        assertEquals(XmlPullParser.TEXT, parser.nextToken())
        assertPosition(2, 23, parser) // end when next token starts
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
        assertPosition(2, 29, parser)
        assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
        assertPosition(3, 2, parser) // end when next token starts
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertPosition(4, 6, parser)
    }

    @Test
    @Throws(Exception::class)
    fun processingInstruction() {
        val input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>nnn</test>"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.TEXT, parser.nextToken())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    }

    /**
     *
     * testProcessingInstructionsContainingXml.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun processingInstructionsContainingXml() {
        val sb = StringBuilder()

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.append("<project>\n")
        sb.append(" <?pi\n")
        sb.append("   <tag>\n")
        sb.append("   </tag>\n")
        sb.append(" ?>\n")
        sb.append("</project>")

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(sb.toString()))

        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.TEXT, parser.nextToken()) // whitespace
        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals(XmlPullParser.TEXT, parser.nextToken()) // whitespace
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    }

    /**
     *
     * testSubsequentProcessingInstructionShort.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun malformedProcessingInstructionsContainingXmlNoClosingQuestionMark() {
        val sb: StringBuilder = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<project />\n")
        sb.append("<?pi\n")
        sb.append("   <tag>\n")
        sb.append("   </tag>>\n")

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(sb.toString()))

        try {
            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
            assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            assertEquals(XmlPullParser.END_TAG, parser.nextToken())
            assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())

            fail("Should fail since it has invalid PI")
        } catch (ex: XmlPullParserException) {
            assertTrue(
                ex.message!!.contains("processing instruction started on line 3 and column 1 was not closed")
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun subsequentProcessingInstructionShort() {
        val sb: StringBuilder = StringBuilder()

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.append("<project>")
        sb.append("<!-- comment -->")
        sb.append("<?m2e ignore?>")
        sb.append("</project>")

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(sb.toString()))

        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    }

    /**
     *
     * testSubsequentProcessingInstructionMoreThan8k.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun subsequentProcessingInstructionMoreThan8k() {
        val sb: StringBuilder = StringBuilder()

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.append("<project>")

        // add ten times 1000 chars as comment
        for (j in 0..9) {
            sb.append("<!-- ")
            for (i in 0..1999) {
                sb.append("ten bytes ")
            }
            sb.append(" -->")
        }

        sb.append("<?m2e ignore?>")
        sb.append("</project>")

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(sb.toString()))

        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    }

    /**
     *
     * testLargeText_NoOverflow.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun largeTextNoOverflow() {
        val sb: StringBuilder = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.append("<largetextblock>")
        // Anything above 33,554,431 would fail without a fix for
        // https://web.archive.org/web/20070831191548/http://www.extreme.indiana.edu/bugzilla/show_bug.cgi?id=228
        // with IOException: error reading input, returned 0
        sb.append(CharArray(33554432).concatToString())
        sb.append("</largetextblock>")

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(sb.toString()))

        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.TEXT, parser.nextToken())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    }

    /**
     *
     * testMalformedProcessingInstructionAfterTag.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun malformedProcessingInstructionAfterTag() {
        val parser: MXParser = MXParser()

        val input = "<project /><?>"

        parser.setInput(StringReader(input))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.next())

            assertEquals(XmlPullParser.END_TAG, parser.next())

            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.next())

            fail("Should fail since it has an invalid Processing Instruction")
        } catch (ex: XmlPullParserException) {
            assertTrue(ex.message!!.contains("processing instruction PITarget name not found"))
        }
    }

    /**
     *
     * testMalformedProcessingInstructionBeforeTag.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun malformedProcessingInstructionBeforeTag() {
        val parser: MXParser = MXParser()

        val input = "<?><project />"

        parser.setInput(StringReader(input))

        try {
            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.next())

            assertEquals(XmlPullParser.START_TAG, parser.next())

            assertEquals(XmlPullParser.END_TAG, parser.next())

            fail("Should fail since it has invalid PI")
        } catch (ex: XmlPullParserException) {
            assertTrue(ex.message!!.contains("processing instruction PITarget name not found"))
        }
    }

    /**
     *
     * testMalformedProcessingInstructionSpaceBeforeName.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun malformedProcessingInstructionSpaceBeforeName() {
        val parser: MXParser = MXParser()

        val sb: StringBuilder = StringBuilder()
        sb.append("<? shouldhavenospace>")
        sb.append("<project />")

        parser.setInput(StringReader(sb.toString()))

        try {
            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.next())

            assertEquals(XmlPullParser.START_TAG, parser.next())

            assertEquals(XmlPullParser.END_TAG, parser.next())

            fail("Should fail since it has invalid PI")
        } catch (ex: XmlPullParserException) {
            assertTrue(
                ex.message!!
                    .contains(
                        "processing instruction PITarget must be exactly after <? and not white space character"
                    )
            )
        }
    }

    /**
     *
     * testMalformedProcessingInstructionNoClosingQuestionMark.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun malformedProcessingInstructionNoClosingQuestionMark() {
        val parser: MXParser = MXParser()

        val sb: StringBuilder = StringBuilder()
        sb.append("<?shouldhavenospace>")
        sb.append("<project />")

        parser.setInput(StringReader(sb.toString()))

        try {
            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.next())

            assertEquals(XmlPullParser.START_TAG, parser.next())

            assertEquals(XmlPullParser.END_TAG, parser.next())

            fail("Should fail since it has invalid PI")
        } catch (ex: XmlPullParserException) {
            assertTrue(
                ex.message!!.contains("processing instruction started on line 1 and column 1 was not closed")
            )
        }
    }

    /**
     *
     * testSubsequentMalformedProcessingInstructionNoClosingQuestionMark.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun subsequentMalformedProcessingInstructionNoClosingQuestionMark() {
        val parser: MXParser = MXParser()

        val sb: StringBuilder = StringBuilder()
        sb.append("<project />")
        sb.append("<?shouldhavenospace>")

        parser.setInput(StringReader(sb.toString()))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.next())

            assertEquals(XmlPullParser.END_TAG, parser.next())

            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.next())

            fail("Should fail since it has invalid PI")
        } catch (ex: XmlPullParserException) {
            assertTrue(
                ex.message!!.contains("processing instruction started on line 1 and column 12 was not closed")
            )
        }
    }

    /**
     *
     * testMalformedXMLRootElement.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun subsequentAbortedProcessingInstruction() {
        val parser: MXParser = MXParser()
        val sb: StringBuilder = StringBuilder()
        sb.append("<project />")
        sb.append("<?aborted")

        parser.setInput(StringReader(sb.toString()))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.next())
            assertEquals(XmlPullParser.END_TAG, parser.next())
            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.next())

            fail("Should fail since it has aborted PI")
        } catch (ex: XmlPullParserException) {
            assertTrue(ex.message!!.contains("@1:21"))
            assertTrue(
                ex.message!!.contains("processing instruction started on line 1 and column 12 was not closed")
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun subsequentAbortedComment() {
        val parser: MXParser = MXParser()
        val sb: StringBuilder = StringBuilder()
        sb.append("<project />")
        sb.append("<!-- aborted")

        parser.setInput(StringReader(sb.toString()))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.next())
            assertEquals(XmlPullParser.END_TAG, parser.next())
            assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.next())

            fail("Should fail since it has aborted comment")
        } catch (ex: XmlPullParserException) {
            assertTrue(ex.message!!.contains("@1:24"))
            assertTrue(ex.message!!.contains("comment started on line 1 and column 12 was not closed"))
        }
    }

    @Test
    @Throws(Exception::class)
    fun malformedXMLRootElement() {
        val input = "<Y"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())

            fail("Should throw EOFException")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("no more data available - expected the opening tag <Y...>"))
        }
    }

    /**
     *
     * testMalformedXMLRootElement2.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun malformedXMLRootElement2() {
        val input = "<hello"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())

            fail("Should throw EOFException")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("no more data available - expected the opening tag <hello...>"))
        }
    }

    /**
     *
     * testMalformedXMLRootElement3.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun malformedXMLRootElement3() {
        val input = "<hello><how"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())

            fail("Should throw EOFException")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("no more data available - expected the opening tag <how...>"))
        }
    }

    /**
     *
     * testMalformedXMLRootElement4.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun malformedXMLRootElement4() {
        val input = "<hello>some text<how"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))


        try {
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            assertEquals(XmlPullParser.TEXT, parser.nextToken())
            assertEquals("some text", parser.getText())
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())

            fail("Should throw EOFException")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("no more data available - expected the opening tag <how...>"))
        }
    }

    /**
     *
     * testMalformedXMLRootElement5.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun malformedXMLRootElement5() {
        val input = "<hello>some text</hello"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        try {
            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            assertEquals(XmlPullParser.TEXT, parser.nextToken())
            assertEquals("some text", parser.getText())
            assertEquals(XmlPullParser.END_TAG, parser.nextToken())

            fail("Should throw EOFException")
        } catch (e: Exception) {
            assertTrue(
                e.message!!.contains("no more data available - expected end tag </hello> to close start tag <hello>")
            )
        }
    }

    /**
     *
     * testXMLDeclVersionOnly.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun xmlDeclVersionOnly() {
        val input = "<?xml version='1.0'?><hello/>"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
    }

    /**
     *
     * testXMLDeclVersionEncodingStandaloneNoSpace.
     *
     * @throws Exception if any.
     */
    @Test
    @Throws(Exception::class)
    fun xmlDeclVersionEncodingStandaloneNoSpace() {
        val input = "<?xml version='1.0' encoding='ASCII'standalone='yes'?><hello/>"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        try {
            parser.nextToken()
        } catch (e: XmlPullParserException) {
            assertTrue(e.message!!.contains("expected a space after encoding and not s"))
        }
    }

    /**
     *
     *
     * Test custom Entity not found.
     *
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws Exception if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(Exception::class)
    fun customEntityNotFoundInText() {
        val parser: MXParser = MXParser()

        val input = "<root>&otherentity;</root>"
        parser.setInput(StringReader(input))
        parser.defineEntityReplacementText("myentity", "replacement")

        try {
            assertEquals(XmlPullParser.START_TAG, parser.next())
            assertEquals(XmlPullParser.TEXT, parser.next())
            fail("should raise exception")
        } catch (e: XmlPullParserException) {
            assertTrue(
                e.message!!
                    .contains(
                        "could not resolve entity named 'otherentity' (position: START_TAG seen <root>&otherentity;... @1:20)"
                    )
            )
            assertEquals(XmlPullParser.START_TAG, parser.getEventType()) // not an ENTITY_REF
            assertEquals("otherentity", parser.getText())
        }
    }

    /**
     *
     *
     * Test custom Entity not found, with tokenize.
     *
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws Exception if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(Exception::class)
    fun customEntityNotFoundInTextTokenize() {
        val parser: MXParser = MXParser()

        val input = "<root>&otherentity;</root>"
        parser.setInput(StringReader(input))
        parser.defineEntityReplacementText("myentity", "replacement")

        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertNull(parser.getText())
    }

    /**
     *
     *
     * Test custom Entity not found in attribute.
     *
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws Exception if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(Exception::class)
    fun customEntityNotFoundInAttr() {
        val parser: MXParser = MXParser()

        val input = "<root name=\"&otherentity;\">sometext</root>"
        parser.setInput(StringReader(input))
        parser.defineEntityReplacementText("myentity", "replacement")

        try {
            assertEquals(XmlPullParser.START_TAG, parser.next())
            fail("should raise exception")
        } catch (e: XmlPullParserException) {
            assertTrue(
                e.message!!
                    .contains(
                        "could not resolve entity named 'otherentity' (position: START_DOCUMENT seen <root name=\"&otherentity;... @1:26)"
                    )
            )
            assertEquals(XmlPullParser.START_DOCUMENT, parser.getEventType()) // not an ENTITY_REF
            assertNull(parser.getText())
        }
    }

    /**
     *
     *
     * Test custom Entity not found in attribute, with tokenize.
     *
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     * @throws XmlPullParserException
     *
     * @throws Exception if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(Exception::class)
    fun customEntityNotFoundInAttrTokenize() {
        val parser: MXParser = MXParser()

        val input = "<root name=\"&otherentity;\">sometext</root>"

        try {
            parser.setInput(StringReader(input))
            parser.defineEntityReplacementText("myentity", "replacement")

            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            fail("should raise exception")
        } catch (e: XmlPullParserException) {
            assertTrue(
                e.message!!
                    .contains(
                        "could not resolve entity named 'otherentity' (position: START_DOCUMENT seen <root name=\"&otherentity;... @1:26)"
                    )
            )
            assertEquals(XmlPullParser.START_DOCUMENT, parser.getEventType()) // not an ENTITY_REF
            assertNull(parser.getText())
        }
    }

    /**
     *
     * test entity ref with entities appearing in tags, Unix line separator.
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws IOException if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(IOException::class)
    fun entityRefTextUnix() {
        testEntityRefText("\n")
    }

    /**
     *
     * test entity ref with entities appearing in tags, DOS line separator.
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws IOException if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(IOException::class)
    fun entityRefTextDOS() {
        testEntityRefText("\r\n")
    }

    @Throws(IOException::class)
    private fun testEntityRefText(newLine: String) {
        val sb: StringBuilder = StringBuilder()
        sb.append("<!DOCTYPE test [").append(newLine)
        sb.append("<!ENTITY foo \"&#x159;\">").append(newLine)
        sb.append("<!ENTITY foo1 \"&nbsp;\">").append(newLine)
        sb.append("<!ENTITY foo2 \"&#x161;\">").append(newLine)
        sb.append("<!ENTITY tritPos \"&#x1d7ed;\">").append(newLine)
        sb.append("]>").append(newLine)
        sb.append("<b>&foo;&foo1;&foo2;&tritPos;</b>")

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(sb.toString()))
        parser.defineEntityReplacementText("foo", "&#x159;")
        parser.defineEntityReplacementText("nbsp", "&#160;")
        parser.defineEntityReplacementText("foo1", "&nbsp;")
        parser.defineEntityReplacementText("foo2", "&#x161;")
        parser.defineEntityReplacementText("tritPos", "&#x1d7ed;")

        assertEquals(XmlPullParser.DOCDECL, parser.nextToken())
        assertEquals(
            (""" test [
<!ENTITY foo "&#x159;">
<!ENTITY foo1 "&nbsp;">
<!ENTITY foo2 "&#x161;">
<!ENTITY tritPos "&#x1d7ed;">
]"""),
            parser.getText()
        )
        assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals("b", parser.getName())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("&#x159;", parser.getText())
        assertEquals("foo", parser.getName())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("&#160;", parser.getText())
        assertEquals("foo1", parser.getName())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("&#x161;", parser.getText())
        assertEquals("foo2", parser.getName())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("&#x1d7ed;", parser.getText())
        assertEquals("tritPos", parser.getName())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
        assertEquals("b", parser.getName())
        assertEquals(XmlPullParser.END_DOCUMENT, parser.nextToken())
    }

    /**
     * **Ensures that entity ref getText() and name return the correct value.**
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws IOException if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(IOException::class)
    fun entityReplacement() {
        val input = "<p><!-- a pagebreak: --><!-- PB -->&#160;&nbsp;<unknown /></p>"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))
        parser.defineEntityReplacementText("nbsp", "&#160;")

        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals("p", parser.getName())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(" a pagebreak: ", parser.getText())
        assertEquals(XmlPullParser.COMMENT, parser.nextToken())
        assertEquals(" PB ", parser.getText())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("\u00A0", parser.getText())
        assertEquals("#160", parser.getName())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("&#160;", parser.getText())
        assertEquals("nbsp", parser.getName())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals("unknown", parser.getName())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
        assertEquals("unknown", parser.getName())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
        assertEquals("p", parser.getName())
        assertEquals(XmlPullParser.END_DOCUMENT, parser.nextToken())
    }

    /**
     * **Ensures correct replacements inside the internal PC array when the new copied array size is shorter than
     * previous ones.**
     *
     * Regression test: assure same behavior of MXParser from plexus-utils 3.3.0.
     *
     * @throws IOException if any.
     *
     * @since 3.4.2
     */
    @Test
    @Throws(IOException::class)
    fun replacementInPCArrayWithShorterCharArray() {
        val input = ("<!DOCTYPE test [<!ENTITY foo \"&#x159;\"><!ENTITY tritPos  \"&#x1d7ed;\">]>"
                + "<section name=\"&amp;&foo;&tritPos;\"><p>&amp;&foo;&tritPos;</p></section>")

        val parser: MXParser = MXParser()
        parser.setInput(
            StringReader(
                input.toByteArray(Charsets.forName("ISO-8859-1")).decodeToString()
            )
        )
        parser.defineEntityReplacementText("foo", "&#x159;")
        parser.defineEntityReplacementText("tritPos", "&#x1d7ed;")

        assertEquals(XmlPullParser.DOCDECL, parser.nextToken())
        assertEquals(" test [<!ENTITY foo \"&#x159;\"><!ENTITY tritPos  \"&#x1d7ed;\">]", parser.getText())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals("section", parser.getName())
        assertEquals(1, parser.getAttributeCount())
        assertEquals("name", parser.getAttributeName(0))
        assertEquals("&&#x159;&#x1d7ed;", parser.getAttributeValue(0))
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals("<p>", parser.getText())
        assertEquals("p", parser.getName())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("&", parser.getText())
        assertEquals("amp", parser.getName())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("&#x159;", parser.getText())
        assertEquals("foo", parser.getName())
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken())
        assertEquals("&#x1d7ed;", parser.getText())
        assertEquals("tritPos", parser.getName())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
        assertEquals("p", parser.getName())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
        assertEquals("section", parser.getName())
        assertEquals(XmlPullParser.END_DOCUMENT, parser.nextToken())
    }

    /**
     * Ensures emoji can be parsed correctly
     */
    @Test
    @Throws(IOException::class)
    fun unicode() {
        val input = "<project><!--ALL TEH BOMS!  \uD83D\uDCA3  --></project>"

        try {
            val parser: MXParser = MXParser()
            parser.setInput(StringReader(input))

            assertEquals(XmlPullParser.START_TAG, parser.nextToken())
            assertEquals("project", parser.getName())
            assertEquals(XmlPullParser.COMMENT, parser.nextToken())
            assertEquals("ALL TEH BOMS!  \uD83D\uDCA3  ", parser.getText())
            assertEquals(XmlPullParser.END_TAG, parser.nextToken())
            assertEquals("project", parser.getName())
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
            fail("should not raise exception: $e")
        }
    }

    @Test
    @Throws(Exception::class)
    fun processingInstructionTokenizeBeforeFirstTag() {
        val input = "<?a?><test>nnn</test>"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_DOCUMENT, parser.getEventType())
        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals("a", parser.getText())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals("test", parser.getName())
        assertEquals(XmlPullParser.TEXT, parser.nextToken())
        assertEquals("nnn", parser.getText())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
        assertEquals(XmlPullParser.END_DOCUMENT, parser.nextToken())
    }

    @Test
    @Throws(Exception::class)
    fun processingInstructionTokenizeAfterXMLDeclAndBeforeFirstTag() {
        val input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><?a?><test>nnn</test>"

        val parser: MXParser = MXParser()
        parser.setInput(StringReader(input))

        assertEquals(XmlPullParser.START_DOCUMENT, parser.getEventType())
        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals("xml version=\"1.0\" encoding=\"UTF-8\"", parser.getText())
        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken())
        assertEquals("a", parser.getText())
        assertEquals(XmlPullParser.START_TAG, parser.nextToken())
        assertEquals("test", parser.getName())
        assertEquals(XmlPullParser.TEXT, parser.nextToken())
        assertEquals("nnn", parser.getText())
        assertEquals(XmlPullParser.END_TAG, parser.nextToken())
        assertEquals(XmlPullParser.END_DOCUMENT, parser.nextToken())
    }

    @Test
    fun blankAtBeginning() {
        val whiteSpaces = listOf(" ", "\n", "\r", "\r\n", "  ", "\n ")
        val xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test>nnn</test>"

        for (ws in whiteSpaces) {
            val parser = MXParser()
            parser.setInput(StringReader(ws + xml))

            val message = assertFailsWith<XmlPullParserException> { parser.next() }.message!!
            assertNotNull(message)
            assertTrue(message.contains("XMLDecl is only allowed as first characters in input"), message)

            parser.setInput(StringReader(ws + xml))
            assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken())

            val secondMessage = assertFailsWith<XmlPullParserException> { parser.nextToken() }.message!!
            assertNotNull(secondMessage)
            assertTrue(secondMessage.contains("processing instruction can not have PITarget with reserved xml name"), secondMessage)
        }
    }

    companion object {
        private fun assertPosition(row: Int, col: Int, parser: MXParser) {
            assertEquals(row, parser.getLineNumber(), "Current line")
            assertEquals(col, parser.getColumnNumber(), "Current column")
        }
    }
}