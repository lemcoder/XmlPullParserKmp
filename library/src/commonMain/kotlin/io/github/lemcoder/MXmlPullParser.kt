package io.github.lemcoder

import io.github.lemcoder.utils.codePointAt
import io.github.lemcoder.utils.codePointCount
import io.github.lemcoder.codePoints.CodePoints
import io.github.lemcoder.codePoints.CodePoints.highSurrogate
import io.github.lemcoder.codePoints.CodePoints.isBmpCodePoint
import io.github.lemcoder.codePoints.CodePoints.lowSurrogate
import io.github.lemcoder.exceptions.EOFException
import io.github.lemcoder.exceptions.IOException
import io.github.lemcoder.exceptions.XmlPullParserException
import io.github.lemcoder.reader.Reader
import io.github.lemcoder.utils.arraycopy
import kotlin.math.min

// TODO best handling of interning issues
//   have isAllNewStringInterned ???
// TODO handling surrogate pairs: http://www.unicode.org/unicode/faq/utf_bom.html#6
// TODO review code for use of bufAbsoluteStart when keeping pos between next()/fillBuf()
/**
 * Absolutely minimal implementation of XMLPULL V1 API. Encoding handling done with XmlReader
 *
 * @see org.codehaus.plexus.util.xml.XmlReader
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class MXParser : XmlPullParser {
    /**
     * Implementation notice: the is instance variable that controls if newString() is interning.
     *
     *
     * **NOTE:** newStringIntern **always** returns interned strings and newString MAY return interned String
     * depending on this variable.
     *
     *
     * **NOTE:** by default in this minimal implementation it is false!
     */
    private val allStringsInterned = false

    private fun resetStringCache() {
        // System.out.println("resetStringCache() minimum called");
    }

    private fun newString(cbuf: CharArray, off: Int, len: Int): String {
        return cbuf.concatToString(off, off + len)
    }

    private fun newStringIntern(cbuf: CharArray, off: Int, len: Int): String {
        return (cbuf.concatToString(off, off + len)) // .intern() // CHANGE
    }

    // NOTE: features are not resetable and typically defaults to false ...
    private var processNamespaces = false

    private var roundtripSupported = false

    // global parser state
    private var location: String? = null

    override var lineNumber: Int = 0
        private set

    override var columnNumber: Int = 0
        private set

    private var seenRoot = false

    private var reachedEnd = false

    @get:Throws(XmlPullParserException::class)
    override var eventType: Int = 0
        private set

    private var emptyElementTag = false

    // element stack
    override var depth: Int = 0
        private set

    private var elRawName: Array<CharArray?>? = null

    private var elRawNameEnd: IntArray = IntArray(8)

    private var elRawNameLine: IntArray = IntArray(8)

    private var elName: Array<String?>? = null

    private var elPrefix: Array<String?> = arrayOfNulls(8)

    private var elUri: Array<String?> = arrayOfNulls(8)

    // private String elValue[];
    private var elNamespaceCount: IntArray = IntArray(8)

    private val fileEncoding: String? = null

    /**
     * Make sure that we have enough space to keep element stack if passed size. It will always create one additional
     * slot then current depth
     */
    private fun ensureElementsCapacity() {
        val elStackSize = if (elName != null) elName!!.size else 0
        if ((depth + 1) >= elStackSize) {
            // we add at least one extra slot ...
            val newSize = (if (depth >= 7) 2 * depth else 8) + 2 // = lucky 7 + 1 //25
            if (TRACE_SIZING) {
                println("TRACE_SIZING elStackSize $elStackSize ==> $newSize")
            }
            val needsCopying = elStackSize > 0
            var arr: Array<String?>? = null
            // resue arr local variable slot
            arr = arrayOfNulls(newSize)
            if (needsCopying) {
                arraycopy(elName ?: arrayOf(), 0, arr, 0, elStackSize) // CHANGE
            }

            elName = arr
            arr = arrayOfNulls(newSize)
            if (needsCopying) arraycopy(elPrefix, 0, arr, 0, elStackSize)
            elPrefix = arr
            arr = arrayOfNulls(newSize)
            if (needsCopying) arraycopy(elUri, 0, arr, 0, elStackSize)
            elUri = arr

            var iarr = IntArray(newSize)
            if (needsCopying) {
                arraycopy(elNamespaceCount.toTypedArray(), 0, iarr.toTypedArray(), 0, elStackSize)
            } else {
                // special initialization
                iarr[0] = 0
            }
            elNamespaceCount = iarr

            // TODO: avoid using element raw name ...
            iarr = IntArray(newSize)
            if (needsCopying) {
                arraycopy(elRawNameEnd.toTypedArray(), 0, iarr.toTypedArray(), 0, elStackSize)
            }
            elRawNameEnd = iarr

            iarr = IntArray(newSize)
            if (needsCopying) {
                arraycopy(elRawNameLine.toTypedArray(), 0, iarr.toTypedArray(), 0, elStackSize)
            }
            elRawNameLine = iarr

            val carr = arrayOfNulls<CharArray>(newSize)
            if (needsCopying) {
                arraycopy(elRawName ?: arrayOf(), 0, carr, 0, elStackSize) // CHANGE
            }
            elRawName = carr
            // arr = new String[newSize];
            // if(needsCopying) System.arraycopy(elLocalName, 0, arr, 0, elStackSize);
            // elLocalName = arr;
            // arr = new String[newSize];
            // if(needsCopying) System.arraycopy(elDefaultNs, 0, arr, 0, elStackSize);
            // elDefaultNs = arr;
            // int[] iarr = new int[newSize];
            // if(needsCopying) System.arraycopy(elNsStackPos, 0, iarr, 0, elStackSize);
            // for (int i = elStackSize; i < iarr.length; i++)
            // {
            // iarr[i] = (i > 0) ? -1 : 0;
            // }
            // elNsStackPos = iarr;
            // assert depth < elName.length;
        }
    }

    // attribute stack
    private var attributeCount: Int = 0

    private var attributeName: Array<String?>? = null

    private var attributeNameHash: IntArray = intArrayOf()

    // private int attributeNameStart[];
    // private int attributeNameEnd[];
    private var attributePrefix: Array<String?> = arrayOfNulls(8)

    private var attributeUri: Array<String?> = arrayOfNulls(8)

    private var attributeValue: Array<String?> = arrayOfNulls(8)

    // private int attributeValueStart[];
    // private int attributeValueEnd[];
    // Make sure that in attributes temporary array is enough space.
    private fun ensureAttributesCapacity(size: Int) {
        val attrPosSize = if (attributeName != null) attributeName!!.size else 0
        if (size >= attrPosSize) {
            val newSize = if (size > 7) 2 * size else 8 // = lucky 7 + 1 //25
            if (TRACE_SIZING) {
                println("TRACE_SIZING attrPosSize $attrPosSize ==> $newSize")
            }
            val needsCopying = attrPosSize > 0
            var arr: Array<String?>? = null

            arr = arrayOfNulls(newSize)
            if (needsCopying) arraycopy(attributeName ?: arrayOf(), 0, arr, 0, attrPosSize) // CHANGE
            attributeName = arr

            arr = arrayOfNulls(newSize)
            if (needsCopying) arraycopy(attributePrefix, 0, arr, 0, attrPosSize)
            attributePrefix = arr

            arr = arrayOfNulls(newSize)
            if (needsCopying) arraycopy(attributeUri, 0, arr, 0, attrPosSize)
            attributeUri = arr

            arr = arrayOfNulls(newSize)
            if (needsCopying) arraycopy(attributeValue, 0, arr, 0, attrPosSize)
            attributeValue = arr

            if (!allStringsInterned) {
                val iarr = IntArray(newSize)
                if (needsCopying) arraycopy(attributeNameHash.toTypedArray(), 0, iarr.toTypedArray(), 0, attrPosSize)
                attributeNameHash = iarr
            }

            arr = null
            // //assert attrUri.length > size
        }
    }

    // namespace stack
    private var namespaceEnd = 0

    private var namespacePrefix: Array<String?>? = null

    private var namespacePrefixHash: IntArray? = intArrayOf()

    private var namespaceUri: Array<String?> = arrayOfNulls(8)

    private fun ensureNamespacesCapacity(size: Int) {
        val namespaceSize = if (namespacePrefix != null) namespacePrefix!!.size else 0
        if (size >= namespaceSize) {
            val newSize = if (size > 7) 2 * size else 8 // = lucky 7 + 1 //25
            if (TRACE_SIZING) {
                println("TRACE_SIZING namespaceSize $namespaceSize ==> $newSize")
            }
            val newNamespacePrefix = arrayOfNulls<String>(newSize)
            val newNamespaceUri = arrayOfNulls<String>(newSize)
            namespacePrefix?.let {
                arraycopy(it, 0, newNamespacePrefix, 0, namespaceEnd)
                arraycopy(namespaceUri, 0, newNamespaceUri, 0, namespaceEnd)
            }
            namespacePrefix = newNamespacePrefix
            namespaceUri = newNamespaceUri

            if (!allStringsInterned) {
                val newNamespacePrefixHash = IntArray(newSize)
                namespacePrefixHash?.let {
                    arraycopy(it.toTypedArray(), 0, newNamespacePrefixHash.toTypedArray(), 0, namespaceEnd)
                }
                namespacePrefixHash = newNamespacePrefixHash
            }
            // prefixesSize = newSize;
            // //assert nsPrefixes.length > size && nsPrefixes.length == newSize
        }
    }

    // entity replacement stack
    private var entityEnd = 0

    private var entityName: Array<String?> = emptyArray()

    private var entityNameBuf: Array<CharArray?> = emptyArray()

    private var entityReplacement: Array<String> = emptyArray()

    private var entityReplacementBuf: Array<CharArray> = emptyArray()

    private var entityNameHash: IntArray? = null

    private val replacementMapTemplate: EntityReplacementMap?

    private fun ensureEntityCapacity() {
        val entitySize = entityReplacementBuf.size
        if (entityEnd >= entitySize) {
            val newSize = if (entityEnd > 7) 2 * entityEnd else 8 // = lucky 7 + 1 //25
            if (TRACE_SIZING) {
                println("TRACE_SIZING entitySize $entitySize ==> $newSize")
            }
            val newEntityName = arrayOfNulls<String>(newSize)
            val newEntityNameBuf = arrayOfNulls<CharArray>(newSize)
            val newEntityReplacement = Array(newSize) { "" }
            val newEntityReplacementBuf = Array<CharArray>(newSize) { charArrayOf() }
            arraycopy(entityName, 0, newEntityName, 0, entityEnd)
            arraycopy(entityNameBuf, 0, newEntityNameBuf, 0, entityEnd)
            arraycopy(entityReplacement, 0, newEntityReplacement, 0, entityEnd)
            arraycopy(entityReplacementBuf, 0, newEntityReplacementBuf, 0, entityEnd)
            entityName = newEntityName
            entityNameBuf = newEntityNameBuf
            entityReplacement = newEntityReplacement
            entityReplacementBuf = newEntityReplacementBuf

            if (!allStringsInterned) {
                val newEntityNameHash = IntArray(newSize)
                entityNameHash?.let {
                    arraycopy(it.toTypedArray(), 0, newEntityNameHash.toTypedArray(), 0, entityEnd)
                }
                entityNameHash = newEntityNameHash
            }
        }
    }

    private var reader: Reader? = null

    override var inputEncoding: String? = null
        private set

    private val bufLoadFactor = 95 // 99%

    // private int bufHardLimit; // only matters when expanding
    private val bufferLoadFactor = bufLoadFactor / 100f

    private var buf = CharArray(256)

    private var bufSoftLimit = (bufferLoadFactor * buf.size).toInt() // desirable size of buffer

    private var preventBufferCompaction = false

    private var bufAbsoluteStart = 0 // this is buf

    private var bufStart = 0

    private var bufEnd = 0

    private var pos = 0

    private var posStart = 0

    private var posEnd = 0

    private var pc = CharArray(64)

    private var pcStart = 0

    private var pcEnd = 0

    // parsing state
    // private boolean needsMore;
    // private boolean seenMarkup;
    private var usePC = false

    private var seenStartTag = false

    private var seenEndTag = false

    private var pastEndTag = false

    private var seenAmpersand = false

    private var seenMarkup = false

    private var seenDocdecl = false

    // transient variable set during each call to next/Token()
    private var tokenize = false

    private var text: String? = null

    private var entityRefName: String? = null

    private var xmlDeclVersion: String? = null

    private var xmlDeclStandalone: Boolean? = null

    private var xmlDeclContent: String? = null

    private fun reset() {
        // System.out.println("reset() called");
        location = null
        lineNumber = 1
        columnNumber = 1
        seenRoot = false
        reachedEnd = false
        eventType = XmlPullParser.START_DOCUMENT
        emptyElementTag = false

        depth = 0

        attributeCount = 0

        namespaceEnd = 0

        entityEnd = 0
        setupFromTemplate()

        reader = null
        inputEncoding = null

        preventBufferCompaction = false
        bufAbsoluteStart = 0
        bufStart = 0
        bufEnd = bufStart
        posEnd = 0
        posStart = posEnd
        pos = posStart

        pcStart = 0
        pcEnd = pcStart

        usePC = false

        seenStartTag = false
        seenEndTag = false
        pastEndTag = false
        seenAmpersand = false
        seenMarkup = false
        seenDocdecl = false

        xmlDeclVersion = null
        xmlDeclStandalone = null
        xmlDeclContent = null

        resetStringCache()
    }

    constructor() {
        replacementMapTemplate = null
    }

    constructor(entityReplacementMap: EntityReplacementMap?) {
        this.replacementMapTemplate = entityReplacementMap
    }

    fun setupFromTemplate() {
        if (replacementMapTemplate != null) {
            val length: Int = replacementMapTemplate.entityEnd

            // This is a bit cheeky, since the EntityReplacementMap contains exact-sized arrays,
            // and elements are always added to the array, we can use the array from the template.
            // Kids; dont do this at home.
            entityName = replacementMapTemplate.entityName
            entityNameBuf = replacementMapTemplate.entityNameBuf
            entityReplacement = replacementMapTemplate.entityReplacement
            entityReplacementBuf = replacementMapTemplate.entityReplacementBuf
            entityNameHash = replacementMapTemplate.entityNameHash
            entityEnd = length
        }
    }

    /**
     * Method setFeature
     *
     * @param name a String
     * @param state a boolean
     * @throws XmlPullParserException issue
     */
    @Throws(XmlPullParserException::class)
    override fun setFeature(name: String?, state: Boolean) {
        requireNotNull(name) { "feature name should not be null" }
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES == name) {
            if (eventType != XmlPullParser.START_DOCUMENT) throw XmlPullParserException(
                "namespace processing feature can only be changed before parsing", this, null
            )
            processNamespaces = state
            // } else if(FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name)) {
            // if(type != START_DOCUMENT) throw new XmlPullParserException(
            // "namespace reporting feature can only be changed before parsing", this, null);
            // reportNsAttribs = state;
        } else if (FEATURE_NAMES_INTERNED == name) {
            if (state != false) {
                throw XmlPullParserException("interning names in this implementation is not supported")
            }
        } else if (XmlPullParser.FEATURE_PROCESS_DOCDECL == name) {
            if (state != false) {
                throw XmlPullParserException("processing DOCDECL is not supported")
            }
            // } else if(REPORT_DOCDECL.equals(name)) {
            // paramNotifyDoctype = state;
        } else if (FEATURE_XML_ROUNDTRIP == name) {
            // if(state == false) {
            // throw new XmlPullParserException(
            // "roundtrip feature can not be switched off");
            // }
            roundtripSupported = state
        } else {
            throw XmlPullParserException("unsupported feature $name")
        }
    }

    /**
     * Unknown properties are **always** returned as false
     */
    override fun getFeature(name: String?): Boolean {
        requireNotNull(name) { "feature name should not be null" }
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES == name) {
            return processNamespaces
            // } else if(FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name)) {
            // return reportNsAttribs;
        } else if (FEATURE_NAMES_INTERNED == name) {
            return false
        } else if (XmlPullParser.FEATURE_PROCESS_DOCDECL == name) {
            return false
            // } else if(REPORT_DOCDECL.equals(name)) {
            // return paramNotifyDoctype;
        } else if (FEATURE_XML_ROUNDTRIP == name) {
            // return true;
            return roundtripSupported
        }
        return false
    }

    @Throws(XmlPullParserException::class)
    override fun setProperty(name: String?, value: Any?) {
        if (PROPERTY_LOCATION == name) {
            location = value as String?
        } else {
            throw XmlPullParserException("unsupported property: '$name'")
        }
    }

    override fun getProperty(name: String?): Any? {
        requireNotNull(name) { "property name should not be null" }
        if (PROPERTY_XMLDECL_VERSION == name) {
            return xmlDeclVersion
        } else if (PROPERTY_XMLDECL_STANDALONE == name) {
            return xmlDeclStandalone
        } else if (PROPERTY_XMLDECL_CONTENT == name) {
            return xmlDeclContent
        } else if (PROPERTY_LOCATION == name) {
            return location
        }
        return null
    }

    @Throws(XmlPullParserException::class)
    override fun setInput(`in`: Reader?) {
        reset()
        reader = `in`
    }

    @Throws(XmlPullParserException::class)
    override fun setInput(inputStream: InputStream, inputEncoding: String?) {
        requireNotNull(inputStream) { "input stream can not be null" }
        val reader: Reader
        try {
            if (inputEncoding != null) {
                reader = InputStreamReader(inputStream, inputEncoding)
            } else {
                reader = XmlStreamReader(inputStream, false)
            }
        } catch (une: Exception) { // TODO catch reader exception
            throw XmlPullParserException(
                "could not create reader for encoding $inputEncoding : $une", this, une
            )
        } catch (e: XmlStreamReaderException) {
            if ("UTF-8" == e.getBomEncoding()) {
                throw XmlPullParserException(
                    "UTF-8 BOM plus xml decl of " + e.getXmlEncoding() + " is incompatible", this, e
                )
            }
            if (e.getBomEncoding() != null && e.getBomEncoding().startsWith("UTF-16")) {
                throw XmlPullParserException(
                    "UTF-16 BOM in a " + e.getXmlEncoding() + " encoded file is incompatible", this, e
                )
            }
            throw XmlPullParserException("could not create reader : $e", this, e)
        } catch (e: IOException) {
            throw XmlPullParserException("could not create reader : $e", this, e)
        }
        setInput(reader)
        // must be here as reset() was called in setInput() and has set this.inputEncoding to null ...
        this.inputEncoding = inputEncoding
    }

    @Throws(XmlPullParserException::class)
    override fun defineEntityReplacementText(entityName: String?, replacementText: String?) {
        // throw new XmlPullParserException("not allowed");
        requireNotNull(entityName) { "entity name must not be null" }
        requireNotNull(replacementText) { "entity replacement text must not be null" }

        var replacementTextCopy: String = replacementText
        if (!replacementTextCopy.startsWith("&#") && this.entityName != null && replacementTextCopy.length > 1) {
            val tmp = replacementTextCopy.substring(1, replacementTextCopy.length - 1)
            for (i in this.entityName!!.indices) {
                if (this.entityName!![i] != null && this.entityName!![i] == tmp) {
                    replacementTextCopy = entityReplacement[i]
                }
            }
        }

        // private char[] entityReplacement[];
        ensureEntityCapacity()

        // this is to make sure that if interning works we will take advantage of it ...
        val entityNameCharData = entityName.toCharArray()
        this.entityName!![entityEnd] = newString(entityNameCharData, 0, entityName.length)
        entityNameBuf[entityEnd] = entityNameCharData

        entityReplacement[entityEnd] = replacementTextCopy
        entityReplacementBuf!![entityEnd] = replacementTextCopy.toCharArray()
        if (!allStringsInterned) {
            entityNameHash!![entityEnd] = fastHash(entityNameBuf[entityEnd], 0, entityNameBuf[entityEnd]!!.size)
        }
        ++entityEnd
        // TODO disallow < or & in entity replacement text (or ]]>???)
        // TOOD keepEntityNormalizedForAttributeValue cached as well ...
    }

    @Throws(XmlPullParserException::class)
    override fun getNamespaceCount(depth: Int): Int {
        if (!processNamespaces || depth == 0) {
            return 0
        }
        // int maxDepth = eventType == END_TAG ? this.depth + 1 : this.depth;
        // if(depth < 0 || depth > maxDepth) throw new IllegalArgumentException(
        require(!(depth < 0 || depth > this.depth)) { "namespace count may be for depth 0.." + this.depth + " not " + depth }
        return elNamespaceCount[depth]
    }

    @Throws(XmlPullParserException::class)
    override fun getNamespacePrefix(pos: Int): String? {
        // int end = eventType == END_TAG ? elNamespaceCount[ depth + 1 ] : namespaceEnd;
        // if(pos < end) {

        if (pos < namespaceEnd) {
            return namespacePrefix!![pos]
        } else {
            throw XmlPullParserException(
                "position $pos exceeded number of available namespaces $namespaceEnd"
            )
        }
    }

    @Throws(XmlPullParserException::class)
    override fun getNamespaceUri(pos: Int): String? {
        // int end = eventType == END_TAG ? elNamespaceCount[ depth + 1 ] : namespaceEnd;
        // if(pos < end) {
        if (pos < namespaceEnd) {
            return namespaceUri[pos]
        } else {
            throw XmlPullParserException(
                "position $pos exceeded number of available namespaces $namespaceEnd"
            )
        }
    }

    override fun getNamespace(prefix: String?): String? // throws XmlPullParserException
    {
        // int count = namespaceCount[ depth ];
        if (prefix != null) {
            for (i in namespaceEnd - 1 downTo 0) {
                if (prefix == namespacePrefix!![i]) {
                    return namespaceUri[i]
                }
            }
            if ("xml" == prefix) {
                return XML_URI
            } else if ("xmlns" == prefix) {
                return XMLNS_URI
            }
        } else {
            for (i in namespaceEnd - 1 downTo 0) {
                if (namespacePrefix!![i] == null) { // "") { //null ) { //TODO check FIXME Alek
                    return namespaceUri[i]
                }
            }
        }
        return null
    }

    override val positionDescription: String
        /**
         * Return string describing current position of parsers as text 'STATE [seen %s...] @line:column'.
         */
        get() {
            var fragment: String? = null
            if (posStart <= pos) {
                val start = findFragment(0, buf, posStart, pos)
                // System.err.println("start="+start);
                if (start < pos) {
                    fragment = buf.concatToString(start, start + (pos - start))
                }
                if (bufAbsoluteStart > 0 || start > 0) fragment = "...$fragment"
            }
            // return " at line "+tokenizerPosRow
            // +" and column "+(tokenizerPosCol-1)
            // +(fragment != null ? " seen "+printable(fragment)+"..." : "");
            return (" " + eventType. + (if (fragment != null) " seen " + printable(fragment) + "..." else "") + " "
                    + (if (location != null) location else "") + "@" + lineNumber + ":" + columnNumber)
        }

    @get:Throws(XmlPullParserException::class)
    override val isWhitespace: Boolean
        get() {
            if (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.CDSECT) {
                if (usePC) {
                    for (i in pcStart..<pcEnd) {
                        if (!isS(pc[i])) return false
                    }
                    return true
                } else {
                    for (i in posStart..<posEnd) {
                        if (!isS(buf[i])) return false
                    }
                    return true
                }
            } else if (eventType == XmlPullParser.IGNORABLE_WHITESPACE) {
                return true
            }
            throw XmlPullParserException("no content available to check for whitespaces")
        }

    override fun getText(): String? {
        if (eventType == XmlPullParser.START_DOCUMENT || eventType == XmlPullParser.END_DOCUMENT) {
            // throw new XmlPullParserException("no content available to read");
            // if(roundtripSupported) {
            // text = new String(buf, posStart, posEnd - posStart);
            // } else {
            return null
            // }
        } else if (eventType == XmlPullParser.ENTITY_REF) {
            return text
        }
        if (text == null) {
            text = if (!usePC || eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG) {
                buf.concatToString(posStart, posStart + (posEnd - posStart))
            } else {
                pc.concatToString(pcStart, pcStart + (pcEnd - pcStart))
            }
        }
        return text
    }

    override fun getTextCharacters(holderForStartAndLength: IntArray?): CharArray? {
        requireNotNull(holderForStartAndLength) { "holderForStartAndLength can not be null" }

        if (eventType == XmlPullParser.TEXT) {
            if (usePC) {
                holderForStartAndLength[0] = pcStart
                holderForStartAndLength[1] = pcEnd - pcStart
                return pc
            } else {
                holderForStartAndLength[0] = posStart
                holderForStartAndLength[1] = posEnd - posStart
                return buf
            }
        } else if (eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG || eventType == XmlPullParser.CDSECT || eventType == XmlPullParser.COMMENT || eventType == XmlPullParser.ENTITY_REF || eventType == XmlPullParser.PROCESSING_INSTRUCTION || eventType == XmlPullParser.IGNORABLE_WHITESPACE || eventType == XmlPullParser.DOCDECL) {
            holderForStartAndLength[0] = posStart
            holderForStartAndLength[1] = posEnd - posStart
            return buf
        } else if (eventType == XmlPullParser.START_DOCUMENT || eventType == XmlPullParser.END_DOCUMENT) {
            // throw new XmlPullParserException("no content available to read");
            holderForStartAndLength[1] = -1
            holderForStartAndLength[0] = holderForStartAndLength[1]
            return null
        } else {
            throw IllegalArgumentException("unknown text eventType: $eventType")
        }
        // String s = getText();
        // char[] cb = null;
        // if(s!= null) {
        // cb = s.toCharArray();
        // holderForStartAndLength[0] = 0;
        // holderForStartAndLength[1] = s.length();
        // } else {
        // }
        // return cb;
    }

    override val namespace: String?
        get() {
            if (eventType == XmlPullParser.START_TAG) {
                // return processNamespaces ? elUri[ depth - 1 ] : NO_NAMESPACE;
                return if (processNamespaces) elUri[depth] else XmlPullParser.NO_NAMESPACE
            } else if (eventType == XmlPullParser.END_TAG) {
                return if (processNamespaces) elUri[depth] else XmlPullParser.NO_NAMESPACE
            }
            return null
            // String prefix = elPrefix[ maxDepth ];
            // if(prefix != null) {
            // for( int i = namespaceEnd -1; i >= 0; i--) {
            // if( prefix.equals( namespacePrefix[ i ] ) ) {
            // return namespaceUri[ i ];
            // }
            // }
            // } else {
            // for( int i = namespaceEnd -1; i >= 0; i--) {
            // if( namespacePrefix[ i ] == null ) {
            // return namespaceUri[ i ];
            // }
            // }
            //
            // }
            // return "";
        }

    override val name: String?
        get() {
            if (eventType == XmlPullParser.START_TAG) {
                // return elName[ depth - 1 ] ;
                return elName!![depth]
            } else if (eventType == XmlPullParser.END_TAG) {
                return elName!![depth]
            } else if (eventType == XmlPullParser.ENTITY_REF) {
                if (entityRefName == null) {
                    entityRefName = newString(buf, posStart, posEnd - posStart)
                }
                return entityRefName
            } else {
                return null
            }
        }

    override val prefix: String?
        get() {
            if (eventType == XmlPullParser.START_TAG) {
                // return elPrefix[ depth - 1 ] ;
                return elPrefix[depth]
            } else if (eventType == XmlPullParser.END_TAG) {
                return elPrefix[depth]
            }
            return null
            // if(eventType != START_TAG && eventType != END_TAG) return null;
            // int maxDepth = eventType == END_TAG ? depth : depth - 1;
            // return elPrefix[ maxDepth ];
        }

    override var isEmptyElementTag: Boolean =
        if (eventType != XmlPullParser.START_TAG) throw XmlPullParserException("parser must be on START_TAG to check for empty element", this, null) else {

            emptyElementTag
        }

    override fun getAttributeCount(): Int {
        if (eventType != XmlPullParser.START_TAG) return -1
        return attributeCount
    }

    override fun getAttributeNamespace(index: Int): String? {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")
        if (!processNamespaces) return XmlPullParser.NO_NAMESPACE
        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )
        return attributeUri[index]
    }

    override fun getAttributeName(index: Int): String? {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")
        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )
        return attributeName!![index]
    }

    override fun getAttributePrefix(index: Int): String? {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")
        if (!processNamespaces) return null
        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )
        return attributePrefix[index]
    }

    override fun getAttributeType(index: Int): String? {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")
        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )
        return "CDATA"
    }

    override fun isAttributeDefault(index: Int): Boolean {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")
        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )
        return false
    }

    override fun getAttributeValue(index: Int): String? {
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes")
        if (index < 0 || index >= attributeCount) throw IndexOutOfBoundsException(
            "attribute position must be 0.." + (attributeCount - 1) + " and not " + index
        )
        return attributeValue[index]
    }

    override fun getAttributeValue(namespace: String?, name: String?): String? {
        var namespace = namespace
        if (eventType != XmlPullParser.START_TAG) throw IndexOutOfBoundsException("only START_TAG can have attributes$positionDescription")
        requireNotNull(name) { "attribute name can not be null" }
        // TODO make check if namespace is interned!!! etc. for names!!!
        if (processNamespaces) {
            if (namespace == null) {
                namespace = ""
            }

            for (i in 0..<attributeCount) {
                if ((namespace === attributeUri[i] || namespace == attributeUri[i]) // (namespace != null && namespace.equals(attributeUri[ i ]))
                    // taking advantage of String.intern()
                    && name == attributeName!![i]
                ) {
                    return attributeValue[i]
                }
            }
        } else {
            if (namespace != null && namespace.length == 0) {
                namespace = null
            }
            require(namespace == null) { "when namespaces processing is disabled attribute namespace must be null" }
            for (i in 0..<attributeCount) {
                if (name == attributeName!![i]) {
                    return attributeValue[i]
                }
            }
        }
        return null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun require(type: Int, namespace: String?, name: String?) {
        if (!processNamespaces && namespace != null) {
            throw XmlPullParserException(
                ("processing namespaces must be enabled on parser (or factory)"
                        + " to have possible namespaces declared on elements" + (" (position:$positionDescription")
                        + ")")
            )
        }
        if (type != eventType || (namespace != null && namespace != namespace)
            || (name != null && name != name)
        ) {
            throw XmlPullParserException(
                ("expected event " + jdk.internal.org.objectweb.asm.util.Printer.TYPES.get(type) // TODO what is it?!
                        + (if (name != null) " with name '$name'" else "")
                        + (if (namespace != null && name != null) " and" else "")
                        + (if (namespace != null) " with namespace '$namespace'" else "") + " but got"
                        + (if (type != eventType) " " + jdk.internal.org.objectweb.asm.util.Printer.TYPES.get(eventType) else "") // TODO what is it?!
                        + (if (name != null && name != null && (name != name)) " name '$name'" else "")
                        + (if (namespace != null && name != null && name != null && (name != name) && namespace != null && (namespace != namespace))
                    " and"
                else
                    "")
                        + (if (namespace != null && namespace != null && (namespace != namespace))
                    " namespace '$namespace'"
                else
                    "")
                        + (" (position:$positionDescription") + ")")
            )
        }
    }

    /**
     *
     * Skip sub tree that is currently parser positioned on.
     * NOTE: parser must be on START_TAG and when function returns parser will be positioned on corresponding END_TAG
     * @throws XmlPullParserException issue
     * @throws IOException io
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun skipSubTree() {
        require(XmlPullParser.START_TAG, null, null)
        var level = 1
        while (level > 0) {
            val eventType = next()
            if (eventType == XmlPullParser.END_TAG) {
                --level
            } else if (eventType == XmlPullParser.START_TAG) {
                ++level
            }
        }
    }

    // public String readText() throws XmlPullParserException, IOException
    // {
    // if (getEventType() != TEXT) return "";
    // String result = getText();
    // next();
    // return result;
    // }
    @Throws(XmlPullParserException::class, IOException::class)
    override fun nextText(): String? {
        // String result = null;
        // boolean onStartTag = false;
        // if(eventType == START_TAG) {
        // onStartTag = true;
        // next();
        // }
        // if(eventType == TEXT) {
        // result = getText();
        // next();
        // } else if(onStartTag && eventType == END_TAG) {
        // result = "";
        // } else {
        // throw new XmlPullParserException(
        // "parser must be on START_TAG or TEXT to read text", this, null);
        // }
        // if(eventType != END_TAG) {
        // throw new XmlPullParserException(
        // "event TEXT it must be immediately followed by END_TAG", this, null);
        // }
        // return result;
        if (eventType != XmlPullParser.START_TAG) {
            throw XmlPullParserException("parser must be on START_TAG to read next text", this, null)
        }
        var eventType = next()
        if (eventType == XmlPullParser.TEXT) {
            val result = text
            eventType = next()
            if (eventType != XmlPullParser.END_TAG) {
                throw XmlPullParserException(
                    "TEXT must be immediately followed by END_TAG and not " + jdk.internal.org.objectweb.asm.util.Printer.TYPES.get(eventType), this, null
                )
            }
            return result
        } else if (eventType == XmlPullParser.END_TAG) {
            return ""
        } else {
            throw XmlPullParserException("parser must be on START_TAG or TEXT to read text", this, null)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun nextTag(): Int {
        next()
        if (eventType == XmlPullParser.TEXT && isWhitespace) { // skip whitespace
            next()
        }
        if (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_TAG) {
            throw XmlPullParserException("expected START_TAG or END_TAG not " + jdk.internal.org.objectweb.asm.util.Printer.TYPES.get(eventType), this, null)
        }
        return eventType
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun next(): Int {
        tokenize = false
        return nextImpl()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    override fun nextToken(): Int {
        tokenize = true
        return nextImpl()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun nextImpl(): Int {
        text = null
        pcStart = 0
        pcEnd = pcStart
        usePC = false
        bufStart = posEnd
        if (pastEndTag) {
            pastEndTag = false
            --depth
            namespaceEnd = elNamespaceCount[depth] // less namespaces available
        }
        if (emptyElementTag) {
            emptyElementTag = false
            pastEndTag = true
            return XmlPullParser.END_TAG.also { eventType = it }
        }

        // [1] document ::= prolog element Misc*
        if (depth > 0) {
            if (seenStartTag) {
                seenStartTag = false
                return parseStartTag().also { eventType = it }
            }
            if (seenEndTag) {
                seenEndTag = false
                return parseEndTag().also { eventType = it }
            }

            // ASSUMPTION: we are _on_ first character of content or markup!!!!
            // [43] content ::= CharData? ((element | Reference | CDSect | PI | Comment) CharData?)*
            var ch: Char
            if (seenMarkup) { // we have read ahead ...
                seenMarkup = false
                ch = '<'
            } else if (seenAmpersand) {
                seenAmpersand = false
                ch = '&'
            } else {
                ch = more()
            }
            posStart = pos - 1 // VERY IMPORTANT: this is correct start of event!!!

            // when true there is some potential event TEXT to return - keep gathering
            var hadCharData = false

            // when true TEXT data is not continuous (like <![CDATA[text]]>) and requires PC merging
            var needsMerging = false

            MAIN_LOOP@ while (true) {
                // work on MARKUP
                if (ch == '<') {
                    if (hadCharData) {
                        // posEnd = pos - 1;
                        if (tokenize) {
                            seenMarkup = true
                            return XmlPullParser.TEXT.also { eventType = it }
                        }
                    }
                    ch = more()
                    if (ch == '/') {
                        if (!tokenize && hadCharData) {
                            seenEndTag = true
                            // posEnd = pos - 2;
                            return XmlPullParser.TEXT.also { eventType = it }
                        }
                        return parseEndTag().also { eventType = it }
                    } else if (ch == '!') {
                        ch = more()
                        if (ch == '-') {
                            // note: if(tokenize == false) posStart/End is NOT changed!!!!
                            parseComment()
                            if (tokenize) return XmlPullParser.COMMENT.also { eventType = it }
                            if (!usePC && hadCharData) {
                                needsMerging = true
                            } else {
                                posStart = pos // completely ignore comment
                            }
                        } else if (ch == '[') {
                            // posEnd = pos - 3;
                            // must remember previous posStart/End as it merges with content of CDATA
                            // int oldStart = posStart + bufAbsoluteStart;
                            // int oldEnd = posEnd + bufAbsoluteStart;
                            parseCDSect(hadCharData)
                            if (tokenize) return XmlPullParser.CDSECT.also { eventType = it }
                            val cdStart = posStart
                            val cdEnd = posEnd
                            val cdLen = cdEnd - cdStart

                            if (cdLen > 0) { // was there anything inside CDATA section?
                                hadCharData = true
                                if (!usePC) {
                                    needsMerging = true
                                }
                            }

                            // posStart = oldStart;
                            // posEnd = oldEnd;
                            // if(cdLen > 0) { // was there anything inside CDATA section?
                            // if(hadCharData) {
                            // // do merging if there was anything in CDSect!!!!
                            // // if(!usePC) {
                            // // // posEnd is correct already!!!
                            // // if(posEnd > posStart) {
                            // // joinPC();
                            // // } else {
                            // // usePC = true;
                            // // pcStart = pcEnd = 0;
                            // // }
                            // // }
                            // // if(pcEnd + cdLen >= pc.length) ensurePC(pcEnd + cdLen);
                            // // // copy [cdStart..cdEnd) into PC
                            // // System.arraycopy(buf, cdStart, pc, pcEnd, cdLen);
                            // // pcEnd += cdLen;
                            // if(!usePC) {
                            // needsMerging = true;
                            // posStart = cdStart;
                            // posEnd = cdEnd;
                            // }
                            // } else {
                            // if(!usePC) {
                            // needsMerging = true;
                            // posStart = cdStart;
                            // posEnd = cdEnd;
                            // hadCharData = true;
                            // }
                            // }
                            // //hadCharData = true;
                            // } else {
                            // if( !usePC && hadCharData ) {
                            // needsMerging = true;
                            // }
                            // }
                        } else {
                            throw XmlPullParserException(
                                "unexpected character in markup " + printable(ch.code), this, null
                            )
                        }
                    } else if (ch == '?') {
                        parsePI()
                        if (tokenize) return XmlPullParser.PROCESSING_INSTRUCTION.also { eventType = it }
                        if (!usePC && hadCharData) {
                            needsMerging = true
                        } else {
                            posStart = pos // completely ignore PI
                        }
                    } else if (isNameStartChar(ch)) {
                        if (!tokenize && hadCharData) {
                            seenStartTag = true
                            // posEnd = pos - 2;
                            return XmlPullParser.TEXT.also { eventType = it }
                        }
                        return parseStartTag().also { eventType = it }
                    } else {
                        throw XmlPullParserException("unexpected character in markup " + printable(ch.code), this, null)
                    }

                    // do content compaction if it makes sense!!!!
                } else if (ch == '&') {
                    // work on ENTITY
                    // posEnd = pos - 1;
                    if (tokenize && hadCharData) {
                        seenAmpersand = true
                        return XmlPullParser.TEXT.also { eventType = it }
                    }
                    val oldStart = posStart + bufAbsoluteStart
                    val oldEnd = posEnd + bufAbsoluteStart
                    parseEntityRef()
                    if (tokenize) return XmlPullParser.ENTITY_REF.also { eventType = it }
                    // check if replacement text can be resolved !!!
                    if (resolvedEntityRefCharBuf == BUF_NOT_RESOLVED) {
                        if (entityRefName == null) {
                            entityRefName = newString(buf, posStart, posEnd - posStart)
                        }
                        throw XmlPullParserException(
                            "could not resolve entity named '" + printable(entityRefName) + "'", this, null
                        )
                    }
                    // int entStart = posStart;
                    // int entEnd = posEnd;
                    posStart = oldStart - bufAbsoluteStart
                    posEnd = oldEnd - bufAbsoluteStart
                    if (!usePC) {
                        if (hadCharData) {
                            joinPC() // posEnd is already set correctly!!!
                            needsMerging = false
                        } else {
                            usePC = true
                            pcEnd = 0
                            pcStart = pcEnd
                        }
                    }
                    // assert usePC == true;
                    // write into PC replacement text - do merge for replacement text!!!!
                    for (aResolvedEntity in resolvedEntityRefCharBuf) {
                        if (pcEnd >= pc.size) {
                            ensurePC(pcEnd)
                        }
                        pc[pcEnd++] = aResolvedEntity
                    }
                    hadCharData = true
                    // assert needsMerging == false;
                } else {
                    if (needsMerging) {
                        // assert usePC == false;
                        joinPC() // posEnd is already set correctly!!!
                        // posStart = pos - 1;
                        needsMerging = false
                    }

                    // no MARKUP not ENTITIES so work on character data ...

                    // [14] CharData ::= [^<&]* - ([^<&]* ']]>' [^<&]*)
                    hadCharData = true

                    var normalizedCR = false
                    val normalizeInput = !tokenize || !roundtripSupported
                    // use loop locality here!!!!
                    var seenBracket = false
                    var seenBracketBracket = false
                    do {
                        // check that ]]> does not show in

                        if (ch == ']') {
                            if (seenBracket) {
                                seenBracketBracket = true
                            } else {
                                seenBracket = true
                            }
                        } else if (seenBracketBracket && ch == '>') {
                            throw XmlPullParserException("characters ]]> are not allowed in content", this, null)
                        } else {
                            if (seenBracket) {
                                seenBracket = false
                                seenBracketBracket = seenBracket
                            }
                            // assert seenTwoBrackets == seenBracket == false;
                        }
                        if (normalizeInput) {
                            // deal with normalization issues ...
                            if (ch == '\r') {
                                normalizedCR = true
                                posEnd = pos - 1
                                // posEnd is already set
                                if (!usePC) {
                                    if (posEnd > posStart) {
                                        joinPC()
                                    } else {
                                        usePC = true
                                        pcEnd = 0
                                        pcStart = pcEnd
                                    }
                                }
                                // assert usePC == true;
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = '\n'
                            } else if (ch == '\n') {
                                // if(!usePC) { joinPC(); } else { if(pcEnd >= pc.length) ensurePC(); }
                                if (!normalizedCR && usePC) {
                                    if (pcEnd >= pc.size) ensurePC(pcEnd)
                                    pc[pcEnd++] = '\n'
                                }
                                normalizedCR = false
                            } else {
                                if (usePC) {
                                    if (pcEnd >= pc.size) ensurePC(pcEnd)
                                    pc[pcEnd++] = ch
                                }
                                normalizedCR = false
                            }
                        }

                        ch = more()
                    } while (ch != '<' && ch != '&')
                    posEnd = pos - 1
                    continue@MAIN_LOOP  // skip ch = more() from below - we are already ahead ...
                }
                ch = more()
            } // endless while(true)
        } else {
            return if (seenRoot) {
                parseEpilog()
            } else {
                parseProlog()
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseProlog(): Int {
        // [2] prolog: ::= XMLDecl? Misc* (doctypedecl Misc*)? and look for [39] element

        var ch: Char
        ch = if (seenMarkup) {
            buf[pos - 1]
        } else {
            more()
        }

        if (eventType == XmlPullParser.START_DOCUMENT) {
            // bootstrap parsing with getting first character input!
            // deal with BOM
            // detect BOM and crop it (Unicode int Order Mark)
            if (ch == '\uFFFE') {
                throw XmlPullParserException(
                    "first character in input was UNICODE noncharacter (0xFFFE)" + "- input requires int swapping",
                    this,
                    null
                )
            }
            if (ch == '\uFEFF') {
                // skipping UNICODE int Order Mark (so called BOM)
                ch = more()
            } else if (ch == '\uFFFD') {
                // UTF-16 BOM in an UTF-8 encoded file?
                // This is a hack...not the best way to check for BOM in UTF-16
                ch = more()
                if (ch == '\uFFFD') {
                    throw XmlPullParserException("UTF-16 BOM in a UTF-8 encoded file is incompatible", this, null)
                }
            }
        }
        seenMarkup = false
        var gotS = false
        posStart = pos - 1
        val normalizeIgnorableWS = tokenize && !roundtripSupported
        var normalizedCR = false
        while (true) {
            // deal with Misc
            // [27] Misc ::= Comment | PI | S
            // deal with docdecl --> mark it!
            // else parseStartTag seen <[^/]
            if (ch == '<') {
                if (gotS && tokenize) {
                    posEnd = pos - 1
                    seenMarkup = true
                    return XmlPullParser.IGNORABLE_WHITESPACE.also { eventType = it }
                }
                ch = more()
                if (ch == '?') {
                    // check if it is 'xml'
                    // deal with XMLDecl
                    parsePI()
                    if (tokenize) {
                        return XmlPullParser.PROCESSING_INSTRUCTION.also { eventType = it }
                    }
                } else if (ch == '!') {
                    ch = more()
                    if (ch == 'D') {
                        if (seenDocdecl) {
                            throw XmlPullParserException("only one docdecl allowed in XML document", this, null)
                        }
                        seenDocdecl = true
                        parseDocdecl()
                        if (tokenize) return XmlPullParser.DOCDECL.also { eventType = it }
                    } else if (ch == '-') {
                        parseComment()
                        if (tokenize) return XmlPullParser.COMMENT.also { eventType = it }
                    } else {
                        throw XmlPullParserException("unexpected markup <!" + printable(ch.code), this, null)
                    }
                } else if (ch == '/') {
                    throw XmlPullParserException("expected start tag name and not " + printable(ch.code), this, null)
                } else if (isNameStartChar(ch)) {
                    seenRoot = true
                    return parseStartTag()
                } else {
                    throw XmlPullParserException("expected start tag name and not " + printable(ch.code), this, null)
                }
            } else if (isS(ch)) {
                gotS = true
                if (normalizeIgnorableWS) {
                    if (ch == '\r') {
                        normalizedCR = true
                        // posEnd = pos -1;
                        // joinPC();
                        // posEnd is already set
                        if (!usePC) {
                            posEnd = pos - 1
                            if (posEnd > posStart) {
                                joinPC()
                            } else {
                                usePC = true
                                pcEnd = 0
                                pcStart = pcEnd
                            }
                        }
                        // assert usePC == true;
                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                        pc[pcEnd++] = '\n'
                    } else if (ch == '\n') {
                        if (!normalizedCR && usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = '\n'
                        }
                        normalizedCR = false
                    } else {
                        if (usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = ch
                        }
                        normalizedCR = false
                    }
                }
            } else {
                throw XmlPullParserException(
                    "only whitespace content allowed before start tag and not " + printable(ch.code), this, null
                )
            }
            ch = more()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseEpilog(): Int {
        if (eventType == XmlPullParser.END_DOCUMENT) {
            throw XmlPullParserException("already reached end of XML input", this, null)
        }
        if (reachedEnd) {
            return XmlPullParser.END_DOCUMENT.also { eventType = it }
        }
        var gotS = false
        val normalizeIgnorableWS = tokenize && !roundtripSupported
        var normalizedCR = false
        try {
            // epilog: Misc*
            var ch: Char
            ch = if (seenMarkup) {
                buf[pos - 1]
            } else {
                more()
            }
            seenMarkup = false
            posStart = pos - 1
            if (!reachedEnd) {
                while (true) {
                    // deal with Misc
                    // [27] Misc ::= Comment | PI | S
                    if (ch == '<') {
                        if (gotS && tokenize) {
                            posEnd = pos - 1
                            seenMarkup = true
                            return XmlPullParser.IGNORABLE_WHITESPACE.also { eventType = it }
                        }
                        ch = more()
                        if (reachedEnd) {
                            break
                        }
                        if (ch == '?') {
                            // check if it is 'xml'
                            // deal with XMLDecl
                            parsePI()
                            if (tokenize) return XmlPullParser.PROCESSING_INSTRUCTION.also { eventType = it }
                        } else if (ch == '!') {
                            ch = more()
                            if (reachedEnd) {
                                break
                            }
                            if (ch == 'D') {
                                parseDocdecl() // FIXME
                                if (tokenize) return XmlPullParser.DOCDECL.also { eventType = it }
                            } else if (ch == '-') {
                                parseComment()
                                if (tokenize) return XmlPullParser.COMMENT.also { eventType = it }
                            } else {
                                throw XmlPullParserException("unexpected markup <!" + printable(ch.code), this, null)
                            }
                        } else if (ch == '/') {
                            throw XmlPullParserException(
                                "end tag not allowed in epilog but got " + printable(ch.code), this, null
                            )
                        } else if (isNameStartChar(ch)) {
                            throw XmlPullParserException(
                                "start tag not allowed in epilog but got " + printable(ch.code), this, null
                            )
                        } else {
                            throw XmlPullParserException(
                                "in epilog expected ignorable content and not " + printable(ch.code), this, null
                            )
                        }
                    } else if (isS(ch)) {
                        gotS = true
                        if (normalizeIgnorableWS) {
                            if (ch == '\r') {
                                normalizedCR = true
                                // posEnd = pos -1;
                                // joinPC();
                                // posEnd is already set
                                if (!usePC) {
                                    posEnd = pos - 1
                                    if (posEnd > posStart) {
                                        joinPC()
                                    } else {
                                        usePC = true
                                        pcEnd = 0
                                        pcStart = pcEnd
                                    }
                                }
                                // assert usePC == true;
                                if (pcEnd >= pc.size) ensurePC(pcEnd)
                                pc[pcEnd++] = '\n'
                            } else if (ch == '\n') {
                                if (!normalizedCR && usePC) {
                                    if (pcEnd >= pc.size) ensurePC(pcEnd)
                                    pc[pcEnd++] = '\n'
                                }
                                normalizedCR = false
                            } else {
                                if (usePC) {
                                    if (pcEnd >= pc.size) ensurePC(pcEnd)
                                    pc[pcEnd++] = ch
                                }
                                normalizedCR = false
                            }
                        }
                    } else {
                        throw XmlPullParserException(
                            "in epilog non whitespace content is not allowed but got " + printable(ch.code), this, null
                        )
                    }
                    ch = more()
                    if (reachedEnd) {
                        break
                    }
                }
            }

            // throw Exception("unexpected content in epilog
            // catch EOFException return END_DOCUMENT
            // try {
        } catch (ex: EOFException) {
            reachedEnd = true
        }
        if (tokenize && gotS) {
            posEnd = pos // well - this is LAST available character pos
            return XmlPullParser.IGNORABLE_WHITESPACE.also { eventType = it }
        }
        return XmlPullParser.END_DOCUMENT.also { eventType = it }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseEndTag(): Int {
        // ASSUMPTION ch is past "</"
        // [42] ETag ::= '</' Name S? '>'
        var ch = more()
        if (!isNameStartChar(ch)) {
            throw XmlPullParserException("expected name start and not " + printable(ch.code), this, null)
        }
        posStart = pos - 3
        val nameStart = pos - 1 + bufAbsoluteStart
        do {
            ch = more()
        } while (isNameChar(ch))

        // now we go one level down -- do checks
        // --depth; //FIXME

        // check that end tag name is the same as start tag
        // String name = new String(buf, nameStart - bufAbsoluteStart,
        // (pos - 1) - (nameStart - bufAbsoluteStart));
        // int last = pos - 1;
        var off = nameStart - bufAbsoluteStart
        // final int len = last - off;
        val len = (pos - 1) - off
        val cbuf = elRawName!![depth]
        if (elRawNameEnd[depth] != len) {
            // construct strings for exception
            val startname = cbuf!!.concatToString(0, 0 + elRawNameEnd[depth]) // CHANGE
            val endname = buf.concatToString(off, off + len) // CHANGE
            throw XmlPullParserException(
                ("end tag name </" + endname + "> must match start tag name <" + startname + ">" + " from line "
                        + elRawNameLine[depth]),
                this,
                null
            )
        }
        for (i in 0..<len) {
            if (buf[off++] != cbuf!![i]) {
                // construct strings for exception
                val startname = cbuf.concatToString(0, 0 + len) // CHANGE
                val offset = off - i - 1
                val endname = buf.concatToString(offset, offset + len) // CHANGE
                throw XmlPullParserException(
                    ("end tag name </" + endname + "> must be the same as start tag <" + startname + ">"
                            + " from line " + elRawNameLine[depth]),
                    this,
                    null
                )
            }
        }

        while (isS(ch)) {
            ch = more()
        } // skip additional white spaces

        if (ch != '>') {
            throw XmlPullParserException(
                "expected > to finsh end tag not " + printable(ch.code) + " from line " + elRawNameLine[depth],
                this,
                null
            )
        }

        // namespaceEnd = elNamespaceCount[ depth ]; //FIXME
        posEnd = pos
        pastEndTag = true
        return XmlPullParser.END_TAG.also { eventType = it }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parseStartTag(): Int {
        // ASSUMPTION ch is past <T
        // [40] STag ::= '<' Name (S Attribute)* S? '>'
        // [44] EmptyElemTag ::= '<' Name (S Attribute)* S? '/>'
        ++depth // FIXME

        posStart = pos - 2

        emptyElementTag = false
        attributeCount = 0
        // retrieve name
        val nameStart = pos - 1 + bufAbsoluteStart
        var colonPos = -1
        var ch = buf[pos - 1]
        if (ch == ':' && processNamespaces) throw XmlPullParserException(
            "when namespaces processing enabled colon can not be at element name start", this, null
        )
        while (true) {
            ch = more()
            if (!isNameChar(ch)) break
            if (ch == ':' && processNamespaces) {
                if (colonPos != -1) throw XmlPullParserException(
                    "only one colon is allowed in name of element when namespaces are enabled", this, null
                )
                colonPos = pos - 1 + bufAbsoluteStart
            }
        }

        // retrieve name
        ensureElementsCapacity()

        // TODO check for efficient interning and then use elRawNameInterned!!!!
        val elLen = (pos - 1) - (nameStart - bufAbsoluteStart)
        if (elRawName!![depth] == null || elRawName!![depth]!!.size < elLen) {
            elRawName!![depth] = CharArray(2 * elLen)
        }
        arraycopy(buf.toTypedArray(), nameStart - bufAbsoluteStart, elRawName!![depth]!!.toTypedArray(), 0, elLen) // CHANGE
        elRawNameEnd[depth] = elLen
        elRawNameLine[depth] = lineNumber

        var name: String? = null

        // work on prefixes and namespace URI
        var prefix: String? = null
        if (processNamespaces) {
            if (colonPos != -1) {
                elPrefix[depth] = newString(buf, nameStart - bufAbsoluteStart, colonPos - nameStart)
                prefix = elPrefix[depth]
                elName!![depth] = newString(
                    buf,
                    colonPos + 1 - bufAbsoluteStart,  // (pos -1) - (colonPos + 1));
                    pos - 2 - (colonPos - bufAbsoluteStart)
                )
                name = elName!![depth]
            } else {
                elPrefix[depth] = null
                prefix = elPrefix[depth]
                elName!![depth] = newString(buf, nameStart - bufAbsoluteStart, elLen)
                name = elName!![depth]
            }
        } else {
            elName!![depth] = newString(buf, nameStart - bufAbsoluteStart, elLen)
            name = elName!![depth]
        }

        while (true) {
            while (isS(ch)) {
                ch = more()
            } // skip additional white spaces


            if (ch == '>') {
                break
            } else if (ch == '/') {
                if (emptyElementTag) throw XmlPullParserException("repeated / in tag declaration", this, null)
                emptyElementTag = true
                ch = more()
                if (ch != '>') throw XmlPullParserException("expected > to end empty tag not " + printable(ch.code), this, null)
                break
            } else if (isNameStartChar(ch)) {
                ch = parseAttribute()
                ch = more()
            } else {
                throw XmlPullParserException("start tag unexpected character " + printable(ch.code), this, null)
            }
            // ch = more(); // skip space
        }

        // now when namespaces were declared we can resolve them
        if (processNamespaces) {
            var uri = getNamespace(prefix)
            if (uri == null) {
                if (prefix == null) { // no prefix and no uri => use default namespace
                    uri = XmlPullParser.NO_NAMESPACE
                } else {
                    throw XmlPullParserException(
                        "could not determine namespace bound to element prefix $prefix", this, null
                    )
                }
            }
            elUri[depth] = uri

            // String uri = getNamespace(prefix);
            // if(uri == null && prefix == null) { // no prefix and no uri => use default namespace
            // uri = "";
            // }
            // resolve attribute namespaces
            for (i in 0..<attributeCount) {
                val attrPrefix = attributePrefix[i]
                if (attrPrefix != null) {
                    val attrUri = getNamespace(attrPrefix)
                        ?: throw XmlPullParserException(
                            "could not determine namespace bound to attribute prefix $attrPrefix", this, null
                        )
                    attributeUri[i] = attrUri
                } else {
                    attributeUri[i] = XmlPullParser.NO_NAMESPACE
                }
            }

            // TODO
            // [ WFC: Unique Att Spec ]
            // check namespaced attribute uniqueness constraint!!!
            for (i in 1..<attributeCount) {
                for (j in 0..<i) {
                    if (attributeUri[j] === attributeUri[i]
                        && (allStringsInterned && attributeName!![j] == attributeName!![i]
                                || (!allStringsInterned && attributeNameHash[j] == attributeNameHash[i] && attributeName!![j] == attributeName!![i]))
                    ) {
                        // prepare data for nice error message?

                        var attr1 = attributeName!![j]
                        if (attributeUri[j] != null) attr1 = attributeUri[j] + ":" + attr1
                        var attr2 = attributeName!![i]
                        if (attributeUri[i] != null) attr2 = attributeUri[i] + ":" + attr2
                        throw XmlPullParserException(
                            "duplicated attributes $attr1 and $attr2", this, null
                        )
                    }
                }
            }
        } else { // ! processNamespaces

            // [ WFC: Unique Att Spec ]
            // check raw attribute uniqueness constraint!!!

            for (i in 1..<attributeCount) {
                for (j in 0..<i) {
                    if ((allStringsInterned && attributeName!![j] == attributeName!![i]
                                || (!allStringsInterned && attributeNameHash[j] == attributeNameHash[i] && attributeName!![j] == attributeName!![i]))
                    ) {
                        // prepare data for nice error message?

                        val attr1 = attributeName!![j]
                        val attr2 = attributeName!![i]
                        throw XmlPullParserException(
                            "duplicated attributes $attr1 and $attr2", this, null
                        )
                    }
                }
            }
        }

        elNamespaceCount[depth] = namespaceEnd
        posEnd = pos
        return XmlPullParser.START_TAG.also { eventType = it }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseAttribute(): Char {
        // parse attribute
        // [41] Attribute ::= Name Eq AttValue
        // [WFC: No External Entity References]
        // [WFC: No < in Attribute Values]
        val prevPosStart = posStart + bufAbsoluteStart
        val nameStart = pos - 1 + bufAbsoluteStart
        var colonPos = -1
        var ch = buf[pos - 1]
        if (ch == ':' && processNamespaces) throw XmlPullParserException(
            "when namespaces processing enabled colon can not be at attribute name start", this, null
        )

        var startsWithXmlns = processNamespaces && ch == 'x'
        var xmlnsPos = 0

        ch = more()
        while (isNameChar(ch)) {
            if (processNamespaces) {
                if (startsWithXmlns && xmlnsPos < 5) {
                    ++xmlnsPos
                    if (xmlnsPos == 1) {
                        if (ch != 'm') startsWithXmlns = false
                    } else if (xmlnsPos == 2) {
                        if (ch != 'l') startsWithXmlns = false
                    } else if (xmlnsPos == 3) {
                        if (ch != 'n') startsWithXmlns = false
                    } else if (xmlnsPos == 4) {
                        if (ch != 's') startsWithXmlns = false
                    } else if (xmlnsPos == 5) {
                        if (ch != ':') throw XmlPullParserException(
                            "after xmlns in attribute name must be colon" + "when namespaces are enabled",
                            this,
                            null
                        )
                        // colonPos = pos - 1 + bufAbsoluteStart;
                    }
                }
                if (ch == ':') {
                    if (colonPos != -1) throw XmlPullParserException(
                        "only one colon is allowed in attribute name" + " when namespaces are enabled",
                        this,
                        null
                    )
                    colonPos = pos - 1 + bufAbsoluteStart
                }
            }
            ch = more()
        }

        ensureAttributesCapacity(attributeCount)

        // --- start processing attributes
        var name: String? = null
        var prefix: String? = null
        // work on prefixes and namespace URI
        if (processNamespaces) {
            if (xmlnsPos < 4) startsWithXmlns = false
            if (startsWithXmlns) {
                if (colonPos != -1) {
                    // prefix = attributePrefix[ attributeCount ] = null;
                    val nameLen = pos - 2 - (colonPos - bufAbsoluteStart)
                    if (nameLen == 0) {
                        throw XmlPullParserException(
                            "namespace prefix is required after xmlns: " + " when namespaces are enabled",
                            this,
                            null
                        )
                    }
                    name =  // attributeName[ attributeCount ] =
                        newString(buf, colonPos - bufAbsoluteStart + 1, nameLen)
                    // pos - 1 - (colonPos + 1 - bufAbsoluteStart)
                }
            } else {
                if (colonPos != -1) {
                    val prefixLen = colonPos - nameStart
                    attributePrefix[attributeCount] = newString(buf, nameStart - bufAbsoluteStart, prefixLen)
                    prefix = attributePrefix[attributeCount]
                    // colonPos - (nameStart - bufAbsoluteStart));
                    val nameLen = pos - 2 - (colonPos - bufAbsoluteStart)
                    attributeName!![attributeCount] = newString(buf, colonPos - bufAbsoluteStart + 1, nameLen)
                    name = attributeName!![attributeCount]

                    // pos - 1 - (colonPos + 1 - bufAbsoluteStart));

                    // name.substring(0, colonPos-nameStart);
                } else {
                    attributePrefix[attributeCount] = null
                    prefix = attributePrefix[attributeCount]
                    attributeName!![attributeCount] =
                        newString(buf, nameStart - bufAbsoluteStart, pos - 1 - (nameStart - bufAbsoluteStart))
                    name = attributeName!![attributeCount]
                }
                if (!allStringsInterned) {
                    attributeNameHash[attributeCount] = name.hashCode()
                }
            }
        } else {
            // retrieve name
            attributeName!![attributeCount] =
                newString(buf, nameStart - bufAbsoluteStart, pos - 1 - (nameStart - bufAbsoluteStart))
            name = attributeName!![attributeCount]
            /** assert name != null; */
            if (!allStringsInterned) {
                attributeNameHash[attributeCount] = name.hashCode()
            }
        }

        // [25] Eq ::= S? '=' S?
        while (isS(ch)) {
            ch = more()
        } // skip additional spaces

        if (ch != '=') throw XmlPullParserException("expected = after attribute name", this, null)
        ch = more()
        while (isS(ch)) {
            ch = more()
        } // skip additional spaces


        // [10] AttValue ::= '"' ([^<&"] | Reference)* '"'
        // | "'" ([^<&'] | Reference)* "'"
        val delimit = ch
        if (delimit != '"' && delimit != '\'') throw XmlPullParserException(
            "attribute value must start with quotation or apostrophe not " + printable(delimit.code), this, null
        )

        // parse until delimit or < and resolve Reference
        // [67] Reference ::= EntityRef | CharRef
        // int valueStart = pos + bufAbsoluteStart;
        var normalizedCR = false
        usePC = false
        pcStart = pcEnd
        posStart = pos

        while (true) {
            ch = more()
            if (ch == delimit) {
                break
            }
            if (ch == '<') {
                throw XmlPullParserException("markup not allowed inside attribute value - illegal < ", this, null)
            }
            if (ch == '&') {
                extractEntityRef()
            } else if (ch == '\t' || ch == '\n' || ch == '\r') {
                // do attribute value normalization
                // as described in http://www.w3.org/TR/REC-xml#AVNormalize
                // TODO add test for it form spec ...
                // handle EOL normalization ...
                if (!usePC) {
                    posEnd = pos - 1
                    if (posEnd > posStart) {
                        joinPC()
                    } else {
                        usePC = true
                        pcStart = 0
                        pcEnd = pcStart
                    }
                }
                // assert usePC == true;
                if (pcEnd >= pc.size) ensurePC(pcEnd)
                if (ch != '\n' || !normalizedCR) {
                    pc[pcEnd++] = ' ' // '\n';
                }
            } else {
                if (usePC) {
                    if (pcEnd >= pc.size) ensurePC(pcEnd)
                    pc[pcEnd++] = ch
                }
            }
            normalizedCR = ch == '\r'
        }

        if (processNamespaces && startsWithXmlns) {
            var ns: String? = null
            ns = if (!usePC) {
                newStringIntern(buf, posStart, pos - 1 - posStart)
            } else {
                newStringIntern(pc, pcStart, pcEnd - pcStart)
            }
            ensureNamespacesCapacity(namespaceEnd)
            var prefixHash = -1
            if (colonPos != -1) {
                if (ns.length == 0) {
                    throw XmlPullParserException(
                        "non-default namespace can not be declared to be empty string", this, null
                    )
                }
                // declare new namespace
                namespacePrefix!![namespaceEnd] = name
                if (!allStringsInterned) {
                    namespacePrefixHash!![namespaceEnd] = name.hashCode()
                    prefixHash = namespacePrefixHash!![namespaceEnd]
                }
            } else {
                // declare new default namespace...
                namespacePrefix!![namespaceEnd] = null // ""; //null; //TODO check FIXME Alek
                if (!allStringsInterned) {
                    namespacePrefixHash!![namespaceEnd] = -1
                    prefixHash = namespacePrefixHash!![namespaceEnd]
                }
            }
            namespaceUri[namespaceEnd] = ns

            // detect duplicate namespace declarations!!!
            val startNs = elNamespaceCount[depth - 1]
            for (i in namespaceEnd - 1 downTo startNs) {
                if (((allStringsInterned || name == null) && namespacePrefix!![i] === name)
                    || (!allStringsInterned && name != null && namespacePrefixHash!![i] == prefixHash && name == namespacePrefix!![i])
                ) {
                    val s = if (name == null) "default" else "'$name'"
                    throw XmlPullParserException(
                        "duplicated namespace declaration for $s prefix", this, null
                    )
                }
            }

            ++namespaceEnd
        } else {
            if (!usePC) {
                attributeValue[attributeCount] = buf.concatToString(posStart, posStart + (pos - 1 - posStart))
            } else {
                attributeValue[attributeCount] = pc.concatToString(pcStart, pcStart + (pcEnd - pcStart))
            }
            ++attributeCount
        }
        posStart = prevPosStart - bufAbsoluteStart
        return ch
    }

    private var resolvedEntityRefCharBuf = BUF_NOT_RESOLVED

    /**
     * parse Entity Ref, either a character entity or one of the predefined name entities.
     *
     * @return the length of the valid found character reference, which may be one of the predefined character reference
     * names (resolvedEntityRefCharBuf contains the replaced chars). Returns the length of the not found entity
     * name, otherwise.
     * @throws XmlPullParserException if invalid XML is detected.
     * @throws IOException if an I/O error is found.
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseCharOrPredefinedEntityRef(): Int {
        // entity reference http://www.w3.org/TR/2000/REC-xml-20001006#NT-Reference
        // [67] Reference ::= EntityRef | CharRef

        // ASSUMPTION just after &

        entityRefName = null
        posStart = pos
        var len = 0
        resolvedEntityRefCharBuf = BUF_NOT_RESOLVED
        var ch = more()
        if (ch == '#') {
            // parse character reference

            var charRef = 0.toChar()
            ch = more()
            val sb: StringBuilder = StringBuilder()
            val isHex = (ch == 'x')

            if (isHex) {
                // encoded in hex
                while (true) {
                    ch = more()
                    if (ch >= '0' && ch <= '9') {
                        charRef = (charRef.code * 16 + (ch.code - '0'.code)).toChar()
                        sb.append(ch)
                    } else if (ch >= 'a' && ch <= 'f') {
                        charRef = (charRef.code * 16 + (ch.code - ('a'.code - 10))).toChar()
                        sb.append(ch)
                    } else if (ch >= 'A' && ch <= 'F') {
                        charRef = (charRef.code * 16 + (ch.code - ('A'.code - 10))).toChar()
                        sb.append(ch)
                    } else if (ch == ';') {
                        break
                    } else {
                        throw XmlPullParserException(
                            "character reference (with hex value) may not contain " + printable(ch.code), this, null
                        )
                    }
                }
            } else {
                // encoded in decimal
                while (true) {
                    if (ch >= '0' && ch <= '9') {
                        charRef = (charRef.code * 10 + (ch.code - '0'.code)).toChar()
                        sb.append(ch)
                    } else if (ch == ';') {
                        break
                    } else {
                        throw XmlPullParserException(
                            "character reference (with decimal value) may not contain " + printable(ch.code),
                            this,
                            null
                        )
                    }
                    ch = more()
                }
            }

            var isValidCodePoint = true
            try {
                val codePoint: Int = sb.toString().toInt(if (isHex) 16 else 10)
                isValidCodePoint = isValidCodePoint(codePoint)
                if (isValidCodePoint) {
                    resolvedEntityRefCharBuf = Character.toChars(codePoint) // TODO
                }
            } catch (e: IllegalArgumentException) {
                isValidCodePoint = false
            }

            if (!isValidCodePoint) {
                throw XmlPullParserException(
                    ("character reference (with " + (if (isHex) "hex" else "decimal") + " value " + sb.toString()
                            + ") is invalid"),
                    this,
                    null
                )
            }

            if (tokenize) {
                text = newString(resolvedEntityRefCharBuf, 0, resolvedEntityRefCharBuf.size)
            }
            len = resolvedEntityRefCharBuf.size
        } else {
            // [68] EntityRef ::= '&' Name ';'
            // scan name until ;
            if (!isNameStartChar(ch)) {
                throw XmlPullParserException(
                    "entity reference names can not start with character '" + printable(ch.code) + "'", this, null
                )
            }
            while (true) {
                ch = more()
                if (ch == ';') {
                    break
                }
                if (!isNameChar(ch)) {
                    throw XmlPullParserException(
                        "entity reference name can not contain character " + printable(ch.code) + "'", this, null
                    )
                }
            }
            // determine what name maps to
            len = (pos - 1) - posStart
            if (len == 2 && buf[posStart] == 'l' && buf[posStart + 1] == 't') {
                if (tokenize) {
                    text = "<"
                }
                resolvedEntityRefCharBuf = BUF_LT
                // if(paramPC || isParserTokenizing) {
                // if(pcEnd >= pc.length) ensurePC();
                // pc[pcEnd++] = '<';
                // }
            } else if (len == 3 && buf[posStart] == 'a' && buf[posStart + 1] == 'm' && buf[posStart + 2] == 'p') {
                if (tokenize) {
                    text = "&"
                }
                resolvedEntityRefCharBuf = BUF_AMP
            } else if (len == 2 && buf[posStart] == 'g' && buf[posStart + 1] == 't') {
                if (tokenize) {
                    text = ">"
                }
                resolvedEntityRefCharBuf = BUF_GT
            } else if (len == 4 && buf[posStart] == 'a' && buf[posStart + 1] == 'p' && buf[posStart + 2] == 'o' && buf[posStart + 3] == 's') {
                if (tokenize) {
                    text = "'"
                }
                resolvedEntityRefCharBuf = BUF_APO
            } else if (len == 4 && buf[posStart] == 'q' && buf[posStart + 1] == 'u' && buf[posStart + 2] == 'o' && buf[posStart + 3] == 't') {
                if (tokenize) {
                    text = "\""
                }
                resolvedEntityRefCharBuf = BUF_QUOT
            }
        }

        posEnd = pos

        return len
    }

    /**
     * Parse an entity reference inside the DOCDECL section.
     *
     * @throws XmlPullParserException if invalid XML is detected.
     * @throws IOException if an I/O error is found.
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseEntityRefInDocDecl() {
        parseCharOrPredefinedEntityRef()
        if (usePC) {
            posStart-- // include in PC the starting '&' of the entity
            joinPC()
        }

        if (resolvedEntityRefCharBuf != BUF_NOT_RESOLVED) return
        if (tokenize) text = null
    }

    /**
     * Parse an entity reference inside a tag or attribute.
     *
     * @throws XmlPullParserException if invalid XML is detected.
     * @throws IOException if an I/O error is found.
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseEntityRef() {
        val len = parseCharOrPredefinedEntityRef()

        posEnd-- // don't involve the final ';' from the entity in the search

        if (resolvedEntityRefCharBuf != BUF_NOT_RESOLVED) {
            return
        }

        resolvedEntityRefCharBuf = lookuEntityReplacement(len)
        if (resolvedEntityRefCharBuf != BUF_NOT_RESOLVED) {
            return
        }
        if (tokenize) text = null
    }

    private fun lookuEntityReplacement(entityNameLen: Int): CharArray {
        if (!allStringsInterned) {
            val hash = fastHash(buf, posStart, posEnd - posStart)
            LOOP@ for (i in entityEnd - 1 downTo 0) {
                if (hash == entityNameHash!![i] && entityNameLen == entityNameBuf[i]!!.size) {
                    val entityBuf = entityNameBuf[i]
                    for (j in 0..<entityNameLen) {
                        if (buf[posStart + j] != entityBuf!![j]) continue@LOOP
                    }
                    if (tokenize) text = entityReplacement[i]
                    return entityReplacementBuf!![i]!!
                }
            }
        } else {
            entityRefName = newString(buf, posStart, posEnd - posStart)
            for (i in entityEnd - 1 downTo 0) {
                // take advantage that interning for newString is enforced
                if (entityRefName === entityName!![i]) {
                    if (tokenize) text = entityReplacement[i]
                    return entityReplacementBuf!![i]!!
                }
            }
        }
        return BUF_NOT_RESOLVED
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseComment() {
        // implements XML 1.0 Section 2.5 Comments

        // ASSUMPTION: seen <!-

        var cch = more()
        if (cch != '-') throw XmlPullParserException("expected <!-- for comment start", this, null)
        if (tokenize) posStart = pos

        val curLine = lineNumber
        val curColumn = columnNumber - 4
        try {
            val normalizeIgnorableWS = tokenize && !roundtripSupported
            var normalizedCR = false

            var seenDash = false
            var seenDashDash = false
            while (true) {
                // scan until it hits -->
                cch = more()
                val ch: Int
                val cch2: Char
                if (cch.isHighSurrogate()) {
                    cch2 = more()
                    ch = CodePoints.toCodePoint(cch, cch2)
                } else {
                    cch2 = 0.toChar()
                    ch = cch.code
                }
                if (seenDashDash && ch != '>'.code) {
                    throw XmlPullParserException(
                        "in comment after two dashes (--) next character must be >" + " not " + printable(ch),
                        this,
                        null
                    )
                }
                if (ch == '-'.code) {
                    if (!seenDash) {
                        seenDash = true
                    } else {
                        seenDashDash = true
                    }
                } else if (ch == '>'.code) {
                    if (seenDashDash) {
                        break // found end sequence!!!!
                    }
                    seenDash = false
                } else if (isValidCodePoint(ch)) {
                    seenDash = false
                } else {
                    throw XmlPullParserException(
                        "Illegal character 0x" + Integer.toHexString(ch) + " found in comment", this, null
                    )
                }
                if (normalizeIgnorableWS) {
                    if (ch == '\r'.code) {
                        normalizedCR = true
                        // posEnd = pos -1;
                        // joinPC();
                        // posEnd is alreadys set
                        if (!usePC) {
                            posEnd = pos - 1
                            if (posEnd > posStart) {
                                joinPC()
                            } else {
                                usePC = true
                                pcEnd = 0
                                pcStart = pcEnd
                            }
                        }
                        // assert usePC == true;
                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                        pc[pcEnd++] = '\n'
                    } else if (ch == '\n'.code) {
                        if (!normalizedCR && usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = '\n'
                        }
                        normalizedCR = false
                    } else {
                        if (usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = cch
                            if (cch2.code != 0) {
                                pc[pcEnd++] = cch2
                            }
                        }
                        normalizedCR = false
                    }
                }
            }
        } catch (ex: EOFException) {
            // detect EOF and create meaningful error ...
            throw XmlPullParserException(
                "comment started on line $curLine and column $curColumn was not closed", this, ex
            )
        }
        if (tokenize) {
            posEnd = pos - 3
            if (usePC) {
                pcEnd -= 2
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parsePI() {
        // implements XML 1.0 Section 2.6 Processing Instructions

        // [16] PI ::= '<?' PITarget (S (Char* - (Char* '?>' Char*)))? '?>'
        // [17] PITarget ::= Name - (('X' | 'x') ('M' | 'm') ('L' | 'l'))
        // ASSUMPTION: seen <?

        if (tokenize) posStart = pos
        val curLine = lineNumber
        val curColumn = columnNumber - 2
        val piTargetStart = pos
        var piTargetEnd = -1
        val normalizeIgnorableWS = tokenize && !roundtripSupported
        var normalizedCR = false

        try {
            var seenPITarget = false
            var seenInnerTag = false
            var seenQ = false
            var ch = more()
            if (isS(ch)) {
                throw XmlPullParserException(
                    "processing instruction PITarget must be exactly after <? and not white space character",
                    this,
                    null
                )
            }
            while (true) {
                // scan until it hits ?>
                // ch = more();

                if (ch == '?') {
                    if (!seenPITarget) {
                        throw XmlPullParserException("processing instruction PITarget name not found", this, null)
                    }
                    seenQ = true
                } else if (ch == '>') {
                    if (seenQ) {
                        break // found end sequence!!!!
                    }

                    if (!seenPITarget) {
                        throw XmlPullParserException("processing instruction PITarget name not found", this, null)
                    } else if (!seenInnerTag) {
                        // seenPITarget && !seenQ
                        throw XmlPullParserException(
                            ("processing instruction started on line " + curLine + " and column " + curColumn
                                    + " was not closed"),
                            this,
                            null
                        )
                    } else {
                        seenInnerTag = false
                    }
                } else if (ch == '<') {
                    seenInnerTag = true
                } else {
                    if (piTargetEnd == -1 && isS(ch)) {
                        piTargetEnd = pos - 1

                        // [17] PITarget ::= Name - (('X' | 'x') ('M' | 'm') ('L' | 'l'))
                        if ((piTargetEnd - piTargetStart) >= 3) {
                            if ((buf[piTargetStart] == 'x' || buf[piTargetStart] == 'X')
                                && (buf[piTargetStart + 1] == 'm' || buf[piTargetStart + 1] == 'M')
                                && (buf[piTargetStart + 2] == 'l' || buf[piTargetStart + 2] == 'L')
                            ) {
                                if (piTargetStart > 2) { // <?xml is allowed as first characters in input ...
                                    throw XmlPullParserException(
                                        if (eventType == 0)
                                            "XMLDecl is only allowed as first characters in input"
                                        else
                                            "processing instruction can not have PITarget with reserved xml name",
                                        this,
                                        null
                                    )
                                } else {
                                    if (buf[piTargetStart] != 'x' && buf[piTargetStart + 1] != 'm' && buf[piTargetStart + 2] != 'l') {
                                        throw XmlPullParserException(
                                            "XMLDecl must have xml name in lowercase", this, null
                                        )
                                    }
                                }
                                parseXmlDecl(ch)
                                if (tokenize) posEnd = pos - 2
                                val off = piTargetStart + 3
                                val len = pos - 2 - off
                                xmlDeclContent = newString(buf, off, len)
                                return
                            }
                        }
                    }

                    seenQ = false
                }
                if (normalizeIgnorableWS) {
                    if (ch == '\r') {
                        normalizedCR = true
                        // posEnd = pos -1;
                        // joinPC();
                        // posEnd is alreadys set
                        if (!usePC) {
                            posEnd = pos - 1
                            if (posEnd > posStart) {
                                joinPC()
                            } else {
                                usePC = true
                                pcEnd = 0
                                pcStart = pcEnd
                            }
                        }
                        // assert usePC == true;
                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                        pc[pcEnd++] = '\n'
                    } else if (ch == '\n') {
                        if (!normalizedCR && usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = '\n'
                        }
                        normalizedCR = false
                    } else {
                        if (usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = ch
                        }
                        normalizedCR = false
                    }
                }
                seenPITarget = true
                ch = more()
            }
        } catch (ex: EOFException) {
            // detect EOF and create meaningful error ...
            throw XmlPullParserException(
                ("processing instruction started on line " + curLine + " and column " + curColumn
                        + " was not closed"),
                this,
                ex
            )
        }
        if (piTargetEnd == -1) {
            piTargetEnd = pos - 2 + bufAbsoluteStart
            // throw new XmlPullParserException(
            // "processing instruction must have PITarget name", this, null);
        }
        if (tokenize) {
            posEnd = pos - 2
            if (normalizeIgnorableWS) {
                --pcEnd
            }
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseXmlDecl(ch: Char) {
        // [23] XMLDecl ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'

        // first make sure that relative positions will stay OK

        var ch = ch
        preventBufferCompaction = true
        bufStart = 0 // necessary to keep pos unchanged during expansion!

        // --- parse VersionInfo

        // [24] VersionInfo ::= S 'version' Eq ("'" VersionNum "'" | '"' VersionNum '"')
        // parse is positioned just on first S past <?xml
        ch = skipS(ch)
        ch = requireInput(ch, VERSION)
        // [25] Eq ::= S? '=' S?
        ch = skipS(ch)
        if (ch != '=') {
            throw XmlPullParserException(
                "expected equals sign (=) after version and not " + printable(ch.code), this, null
            )
        }
        ch = more()
        ch = skipS(ch)
        if (ch != '\'' && ch != '"') {
            throw XmlPullParserException(
                "expected apostrophe (') or quotation mark (\") after version and not " + printable(ch.code),
                this,
                null
            )
        }
        val quotChar = ch
        // int versionStart = pos + bufAbsoluteStart; // required if preventBufferCompaction==false
        val versionStart = pos
        ch = more()
        // [26] VersionNum ::= ([a-zA-Z0-9_.:] | '-')+
        while (ch != quotChar) {
            if ((ch < 'a' || ch > 'z')
                && (ch < 'A' || ch > 'Z')
                && (ch < '0' || ch > '9')
                && ch != '_' && ch != '.' && ch != ':' && ch != '-'
            ) {
                throw XmlPullParserException(
                    "<?xml version value expected to be in ([a-zA-Z0-9_.:] | '-')" + " not " + printable(ch.code),
                    this,
                    null
                )
            }
            ch = more()
        }
        val versionEnd = pos - 1
        parseXmlDeclWithVersion(versionStart, versionEnd)
        preventBufferCompaction = false // allow again buffer compaction - pos MAY change
    }

    // private String xmlDeclVersion;
    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseXmlDeclWithVersion(versionStart: Int, versionEnd: Int) {
        // check version is "1.0"
        if ((versionEnd - versionStart != 3)
            || buf[versionStart] != '1' || buf[versionStart + 1] != '.' || buf[versionStart + 2] != '0'
        ) {
            throw XmlPullParserException(
                ("only 1.0 is supported as <?xml version not '"
                        + printable(buf.concatToString(versionStart, versionStart + (versionEnd - versionStart))) + "'"),
                this,
                null
            )
        }
        xmlDeclVersion = newString(buf, versionStart, versionEnd - versionStart)

        var lastParsedAttr = "version"

        // [80] EncodingDecl ::= S 'encoding' Eq ('"' EncName '"' | "'" EncName "'" )
        var ch = more()
        var prevCh = ch
        ch = skipS(ch)

        if (ch != 'e' && ch != 's' && ch != '?' && ch != '>') {
            throw XmlPullParserException("unexpected character " + printable(ch.code), this, null)
        }

        if (ch == 'e') {
            if (!isS(prevCh)) {
                throw XmlPullParserException(
                    "expected a space after " + lastParsedAttr + " and not " + printable(ch.code), this, null
                )
            }
            ch = more()
            ch = requireInput(ch, NCODING)
            ch = skipS(ch)
            if (ch != '=') {
                throw XmlPullParserException(
                    "expected equals sign (=) after encoding and not " + printable(ch.code), this, null
                )
            }
            ch = more()
            ch = skipS(ch)
            if (ch != '\'' && ch != '"') {
                throw XmlPullParserException(
                    "expected apostrophe (') or quotation mark (\") after encoding and not " + printable(ch.code),
                    this,
                    null
                )
            }
            val quotChar = ch
            val encodingStart = pos
            ch = more()
            // [81] EncName ::= [A-Za-z] ([A-Za-z0-9._] | '-')*
            if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z')) {
                throw XmlPullParserException(
                    "<?xml encoding name expected to start with [A-Za-z]" + " not " + printable(ch.code), this, null
                )
            }
            ch = more()
            while (ch != quotChar) {
                if ((ch < 'a' || ch > 'z')
                    && (ch < 'A' || ch > 'Z')
                    && (ch < '0' || ch > '9')
                    && ch != '.' && ch != '_' && ch != '-'
                ) {
                    throw XmlPullParserException(
                        "<?xml encoding value expected to be in ([A-Za-z0-9._] | '-')" + " not " + printable(ch.code),
                        this,
                        null
                    )
                }
                ch = more()
            }
            val encodingEnd = pos - 1

            // TODO reconcile with setInput encodingName
            inputEncoding = newString(buf, encodingStart, encodingEnd - encodingStart)

            lastParsedAttr = "encoding"

            ch = more()
            prevCh = ch
            ch = skipS(ch)
        }

        // [32] SDDecl ::= S 'standalone' Eq (("'" ('yes' | 'no') "'") | ('"' ('yes' | 'no') '"'))
        if (ch == 's') {
            if (!isS(prevCh)) {
                throw XmlPullParserException(
                    "expected a space after " + lastParsedAttr + " and not " + printable(ch.code), this, null
                )
            }

            ch = more()
            ch = requireInput(ch, TANDALONE)
            ch = skipS(ch)
            if (ch != '=') {
                throw XmlPullParserException(
                    "expected equals sign (=) after standalone and not " + printable(ch.code), this, null
                )
            }
            ch = more()
            ch = skipS(ch)
            if (ch != '\'' && ch != '"') {
                throw XmlPullParserException(
                    "expected apostrophe (') or quotation mark (\") after standalone and not " + printable(ch.code),
                    this,
                    null
                )
            }
            val quotChar = ch
            ch = more()
            if (ch == 'y') {
                ch = requireInput(ch, YES)
                // Boolean standalone = new Boolean(true);
                xmlDeclStandalone = true
            } else if (ch == 'n') {
                ch = requireInput(ch, NO)
                // Boolean standalone = new Boolean(false);
                xmlDeclStandalone = false
            } else {
                throw XmlPullParserException(
                    "expected 'yes' or 'no' after standalone and not " + printable(ch.code), this, null
                )
            }
            if (ch != quotChar) {
                throw XmlPullParserException(
                    "expected " + quotChar + " after standalone value not " + printable(ch.code), this, null
                )
            }
            ch = more()
            ch = skipS(ch)
        }

        if (ch != '?') {
            throw XmlPullParserException("expected ?> as last part of <?xml not " + printable(ch.code), this, null)
        }
        ch = more()
        if (ch != '>') {
            throw XmlPullParserException("expected ?> as last part of <?xml not " + printable(ch.code), this, null)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseDocdecl() {
        // ASSUMPTION: seen <!D
        var ch = more()
        if (ch != 'O') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'C') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'T') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'Y') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'P') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        ch = more()
        if (ch != 'E') throw XmlPullParserException("expected <!DOCTYPE", this, null)
        posStart = pos

        // do simple and crude scanning for end of doctype

        // [28] doctypedecl ::= '<!DOCTYPE' S Name (S ExternalID)? S? ('['
        // (markupdecl | DeclSep)* ']' S?)? '>'
        var bracketLevel = 0
        val normalizeIgnorableWS = tokenize && !roundtripSupported
        var normalizedCR = false
        while (true) {
            ch = more()
            if (ch == '[') ++bracketLevel
            else if (ch == ']') --bracketLevel
            else if (ch == '>' && bracketLevel == 0) break
            else if (ch == '&') {
                extractEntityRefInDocDecl()
                continue
            }
            if (normalizeIgnorableWS) {
                if (ch == '\r') {
                    normalizedCR = true
                    // posEnd = pos -1;
                    // joinPC();
                    // posEnd is alreadys set
                    if (!usePC) {
                        posEnd = pos - 1
                        if (posEnd > posStart) {
                            joinPC()
                        } else {
                            usePC = true
                            pcEnd = 0
                            pcStart = pcEnd
                        }
                    }
                    // assert usePC == true;
                    if (pcEnd >= pc.size) ensurePC(pcEnd)
                    pc[pcEnd++] = '\n'
                } else if (ch == '\n') {
                    if (!normalizedCR && usePC) {
                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                        pc[pcEnd++] = '\n'
                    }
                    normalizedCR = false
                } else {
                    if (usePC) {
                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                        pc[pcEnd++] = ch
                    }
                    normalizedCR = false
                }
            }
        }
        posEnd = pos - 1
        text = null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun extractEntityRefInDocDecl() {
        // extractEntityRef
        posEnd = pos - 1

        val prevPosStart = posStart
        parseEntityRefInDocDecl()

        posStart = prevPosStart
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun extractEntityRef() {
        // extractEntityRef
        posEnd = pos - 1
        if (!usePC) {
            val hadCharData = posEnd > posStart
            if (hadCharData) {
                // posEnd is already set correctly!!!
                joinPC()
            } else {
                usePC = true
                pcEnd = 0
                pcStart = pcEnd
            }
        }

        // assert usePC == true;
        parseEntityRef()
        // check if replacement text can be resolved !!!
        if (resolvedEntityRefCharBuf == BUF_NOT_RESOLVED) {
            if (entityRefName == null) {
                entityRefName = newString(buf, posStart, posEnd - posStart)
            }
            throw XmlPullParserException(
                "could not resolve entity named '" + printable(entityRefName) + "'", this, null
            )
        }
        // write into PC replacement text - do merge for replacement text!!!!
        for (aResolvedEntity in resolvedEntityRefCharBuf) {
            if (pcEnd >= pc.size) {
                ensurePC(pcEnd)
            }
            pc[pcEnd++] = aResolvedEntity
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseCDSect(hadCharData: Boolean) {
        // implements XML 1.0 Section 2.7 CDATA Sections

        // [18] CDSect ::= CDStart CData CDEnd
        // [19] CDStart ::= '<![CDATA['
        // [20] CData ::= (Char* - (Char* ']]>' Char*))
        // [21] CDEnd ::= ']]>'

        // ASSUMPTION: seen <![

        var ch = more()
        if (ch != 'C') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != 'D') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != 'A') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != 'T') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != 'A') throw XmlPullParserException("expected <[CDATA[ for comment start", this, null)
        ch = more()
        if (ch != '[') throw XmlPullParserException("expected <![CDATA[ for comment start", this, null)

        // if(tokenize) {
        val cdStart = pos + bufAbsoluteStart
        val curLine = lineNumber
        val curColumn = columnNumber
        val normalizeInput = !tokenize || !roundtripSupported
        try {
            if (normalizeInput) {
                if (hadCharData) {
                    if (!usePC) {
                        // posEnd is correct already!!!
                        if (posEnd > posStart) {
                            joinPC()
                        } else {
                            usePC = true
                            pcEnd = 0
                            pcStart = pcEnd
                        }
                    }
                }
            }
            var seenBracket = false
            var seenBracketBracket = false
            var normalizedCR = false
            while (true) {
                // scan until it hits "]]>"
                ch = more()
                if (ch == ']') {
                    if (!seenBracket) {
                        seenBracket = true
                    } else {
                        seenBracketBracket = true
                        // seenBracket = false;
                    }
                } else if (ch == '>') {
                    if (seenBracket && seenBracketBracket) {
                        break // found end sequence!!!!
                    } else {
                        seenBracketBracket = false
                    }
                    seenBracket = false
                } else {
                    if (seenBracket) {
                        seenBracket = false
                    }
                }
                if (normalizeInput) {
                    // deal with normalization issues ...
                    if (ch == '\r') {
                        normalizedCR = true
                        posStart = cdStart - bufAbsoluteStart
                        posEnd = pos - 1 // posEnd is alreadys set
                        if (!usePC) {
                            if (posEnd > posStart) {
                                joinPC()
                            } else {
                                usePC = true
                                pcEnd = 0
                                pcStart = pcEnd
                            }
                        }
                        // assert usePC == true;
                        if (pcEnd >= pc.size) ensurePC(pcEnd)
                        pc[pcEnd++] = '\n'
                    } else if (ch == '\n') {
                        if (!normalizedCR && usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = '\n'
                        }
                        normalizedCR = false
                    } else {
                        if (usePC) {
                            if (pcEnd >= pc.size) ensurePC(pcEnd)
                            pc[pcEnd++] = ch
                        }
                        normalizedCR = false
                    }
                }
            }
        } catch (ex: EOFException) {
            // detect EOF and create meaningful error ...
            throw XmlPullParserException(
                "CDATA section started on line $curLine and column $curColumn was not closed",
                this,
                ex
            )
        }
        if (normalizeInput) {
            if (usePC) {
                pcEnd = pcEnd - 2
            }
        }
        posStart = cdStart - bufAbsoluteStart
        posEnd = pos - 3
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun fillBuf() {
        if (reader == null) throw XmlPullParserException("reader must be set before parsing is started")

        // see if we are in compaction area
        if (bufEnd > bufSoftLimit) {
            // check if we need to compact or expand the buffer

            val compact = !preventBufferCompaction && (bufStart > bufSoftLimit || bufStart >= buf.size / 2)

            // if buffer almost full then compact it
            if (compact) {
                // TODO: look on trashing
                // //assert bufStart > 0
                arraycopy(buf.toTypedArray(), bufStart, buf.toTypedArray(), 0, bufEnd - bufStart)
                if (TRACE_SIZING) println(
                    ("TRACE_SIZING fillBuf() compacting " + bufStart + " bufEnd=" + bufEnd + " pos="
                            + pos + " posStart=" + posStart + " posEnd=" + posEnd + " buf first 100 chars:"
                            + buf.concatToString(bufStart, bufStart + min((bufEnd - bufStart).toDouble(), 100.0).toInt()))
                )
            } else {
                val newSize = 2 * buf.size
                val newBuf = CharArray(newSize)
                if (TRACE_SIZING) println("TRACE_SIZING fillBuf() " + buf.size + " => " + newSize)
                arraycopy(buf.toTypedArray(), bufStart, newBuf.toTypedArray(), 0, bufEnd - bufStart)
                buf = newBuf
                if (bufLoadFactor > 0) {
                    // Include a fix for
                    // https://web.archive.org/web/20070831191548/http://www.extreme.indiana.edu/bugzilla/show_bug.cgi?id=228
                    bufSoftLimit = (bufferLoadFactor * buf.size).toInt()
                }
            }
            bufEnd -= bufStart
            pos -= bufStart
            posStart -= bufStart
            posEnd -= bufStart
            bufAbsoluteStart += bufStart
            bufStart = 0
            if (TRACE_SIZING) println(
                ("TRACE_SIZING fillBuf() after bufEnd=" + bufEnd + " pos=" + pos + " posStart="
                        + posStart + " posEnd=" + posEnd + " buf first 100 chars:"
                        + buf.concatToString(0, 0 + min(bufEnd.toDouble(), 100.0).toInt()))
            )
        }
        // at least one character must be read or error
        val len = min((buf.size - bufEnd).toDouble(), READ_CHUNK_SIZE.toDouble()).toInt()
        val ret: Int = reader.read(buf, bufEnd, len)
        if (ret > 0) {
            bufEnd += ret
            if (TRACE_SIZING) println(
                ("TRACE_SIZING fillBuf() after filling in buffer" + " buf first 100 chars:"
                        + buf.concatToString(0, 0 + min(bufEnd.toDouble(), 100.0).toInt()))
            )

            return
        }
        if (ret == -1) {
            if (bufAbsoluteStart == 0 && pos == 0) {
                throw EOFException("input contained no data")
            } else {
                if (seenRoot && depth == 0) { // inside parsing epilog!!!
                    reachedEnd = true
                    return
                } else {
                    val expectedTagStack: StringBuilder = StringBuilder()
                    if (depth > 0) {
                        if (elRawName == null || elRawName!![depth] == null) {
                            val offset = posStart + 1
                            val tagName = buf.concatToString(offset, offset + (pos - posStart - 1))
                            expectedTagStack
                                .append(" - expected the opening tag <")
                                .append(tagName)
                                .append("...>")
                        } else {
                            // final char[] cbuf = elRawName[depth];
                            // final String startname = new String(cbuf, 0, elRawNameEnd[depth]);
                            expectedTagStack.append(" - expected end tag")
                            if (depth > 1) {
                                expectedTagStack.append("s") // more than one end tag
                            }
                            expectedTagStack.append(" ")

                            for (i in depth downTo 1) {
                                if (elRawName == null || elRawName!![i] == null) {
                                    val offset = posStart + 1
                                    val tagName = buf.concatToString(offset, offset + (pos - posStart - 1))
                                    expectedTagStack
                                        .append(" - expected the opening tag <")
                                        .append(tagName)
                                        .append("...>")
                                } else {
                                    val tagName = elRawName!![i]!!.concatToString(0, 0 + elRawNameEnd[i])
                                    expectedTagStack
                                        .append("</")
                                        .append(tagName)
                                        .append('>')
                                }
                            }
                            expectedTagStack.append(" to close")
                            for (i in depth downTo 1) {
                                if (i != depth) {
                                    expectedTagStack.append(" and") // more than one end tag
                                }
                                if (elRawName == null || elRawName!![i] == null) {
                                    val offset = posStart + 1
                                    val tagName = buf.concatToString(offset, offset + (pos - posStart - 1))
                                    expectedTagStack
                                        .append(" start tag <")
                                        .append(tagName)
                                        .append(">")
                                    expectedTagStack.append(" from line ").append(elRawNameLine[i])
                                } else {
                                    val tagName = elRawName!![i]!!.concatToString(0, 0 + elRawNameEnd[i]) // FIXME
                                    expectedTagStack
                                        .append(" start tag <")
                                        .append(tagName)
                                        .append(">")
                                    expectedTagStack.append(" from line ").append(elRawNameLine[i])
                                }
                            }
                            expectedTagStack.append(", parser stopped on")
                        }
                    }
                    throw EOFException(
                        "no more data available" + expectedTagStack.toString() + positionDescription
                    )
                }
            }
        } else {
            throw IOException("error reading input, returned $ret")
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun more(): Char {
        if (pos >= bufEnd) {
            fillBuf()
            // this return value should be ignored as it is used in epilog parsing ...
            if (reachedEnd) throw EOFException("no more data available$positionDescription")
        }
        val ch = buf[pos++]
        // line/columnNumber
        if (ch == '\n') {
            ++lineNumber
            columnNumber = 1
        } else {
            ++columnNumber
        }
        // System.out.print(ch);
        return ch
    }

    // /**
    // * This function returns position of parser in XML input stream
    // * (how many <b>characters</b> were processed.
    // * <p><b>NOTE:</b> this logical position and not byte offset as encodings
    // * such as UTF8 may use more than one byte to encode one character.
    // */
    // public int getCurrentInputPosition() {
    // return pos + bufAbsoluteStart;
    // }
    private fun ensurePC(end: Int) {
        // assert end >= pc.length;
        val newSize = if (end > READ_CHUNK_SIZE) 2 * end else 2 * READ_CHUNK_SIZE
        val newPC = CharArray(newSize)
        if (TRACE_SIZING) println("TRACE_SIZING ensurePC() " + pc.size + " ==> " + newSize + " end=" + end)
        arraycopy(pc.toTypedArray(), 0, newPC.toTypedArray(), 0, pcEnd)
        pc = newPC
        // assert end < pc.length;
    }

    private fun joinPC() {
        // assert usePC == false;
        // assert posEnd > posStart;
        val len = posEnd - posStart
        val newEnd = pcEnd + len + 1
        if (newEnd >= pc.size) ensurePC(newEnd) // add 1 for extra space for one char

        // assert newEnd < pc.length;
        arraycopy(buf.toTypedArray(), posStart, pc.toTypedArray(), pcEnd, len)
        pcEnd += len
        usePC = true
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun requireInput(ch: Char, input: CharArray): Char {
        var ch = ch
        for (anInput in input) {
            if (ch != anInput) {
                throw XmlPullParserException(
                    "expected " + printable(anInput.code) + " in " + input.concatToString() + " and not " + printable(ch.code),
                    this,
                    null
                )
            }
            ch = more()
        }
        return ch
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skipS(ch: Char): Char {
        var ch = ch
        while (isS(ch)) {
            ch = more()
        } // skip additional spaces

        return ch
    }

    companion object {
        // NOTE: no interning of those strings --> by Java leng spec they MUST be already interned
        private const val XML_URI = "http://www.w3.org/XML/1998/namespace"

        private const val XMLNS_URI = "http://www.w3.org/2000/xmlns/"

        private const val FEATURE_XML_ROUNDTRIP =  // "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";
            "http://xmlpull.org/v1/doc/features.html#xml-roundtrip"

        private const val FEATURE_NAMES_INTERNED = "http://xmlpull.org/v1/doc/features.html#names-interned"

        private const val PROPERTY_XMLDECL_VERSION = "http://xmlpull.org/v1/doc/properties.html#xmldecl-version"

        private const val PROPERTY_XMLDECL_STANDALONE = "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone"

        private const val PROPERTY_XMLDECL_CONTENT = "http://xmlpull.org/v1/doc/properties.html#xmldecl-content"

        private const val PROPERTY_LOCATION = "http://xmlpull.org/v1/doc/properties.html#location"

        private const val TRACE_SIZING = false

        // simplistic implementation of hash function that has <b>constant</b> time to compute - so it also means
        // diminishing hash quality for long strings but for XML parsing it should be good enough ...
        private fun fastHash(ch: CharArray?, off: Int, len: Int): Int {
            if (len == 0) return 0
            // assert len >0
            var hash = ch!![off].code // hash at beginning
            // try {
            hash = (hash shl 7) + ch[off + len - 1].code // hash at the end
            // } catch(ArrayIndexOutOfBoundsException aie) {
            // aie.printStackTrace(); //should never happen ...
            // throw new RuntimeException("this is violation of pre-condition");
            // }
            if (len > 16) hash = (hash shl 7) + ch[off + (len / 4)].code // 1/4 from beginning

            if (len > 8) hash = (hash shl 7) + ch[off + (len / 2)].code // 1/2 of string size ...

            // notice that hash is at most done 3 times <<7 so shifted by 21 bits 8 bit value
            // so max result == 29 bits so it is quite just below 31 bits for long (2^32) ...
            // assert hash >= 0;
            return hash
        }

        // input buffer management
        private const val READ_CHUNK_SIZE = 8 * 1024 // max data chars in one read() call

        private fun findFragment(bufMinPos: Int, b: CharArray, start: Int, end: Int): Int {
            // System.err.println("bufStart="+bufStart+" b="+printable(new String(b, start, end - start))+" start="+start+"
            // end="+end);
            var start = start
            if (start < bufMinPos) {
                start = bufMinPos
                if (start > end) start = end
                return start
            }
            if (end - start > 65) {
                start = end - 10 // try to find good location
            }
            var i = start + 1
            while (--i > bufMinPos) {
                if ((end - i) > 65) break
                val c = b[i]
                if (c == '<' && (start - i) > 10) break
            }
            return i
        }

        // state representing that no entity ref have been resolved
        private val BUF_NOT_RESOLVED = CharArray(0)

        // predefined entity refs
        private val BUF_LT = charArrayOf('<')
        private val BUF_AMP = charArrayOf('&')
        private val BUF_GT = charArrayOf('>')
        private val BUF_APO = charArrayOf('\'')
        private val BUF_QUOT = charArrayOf('"')

        /**
         * Check if the provided parameter is a valid Char. According to
         * [https://www.w3.org/TR/REC-xml/#NT-Char](https://www.w3.org/TR/REC-xml/#NT-Char)
         *
         * @param codePoint the numeric value to check
         * @return true if it is a valid numeric character reference. False otherwise.
         */
        private fun isValidCodePoint(codePoint: Int): Boolean {
            // Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
            return codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD || (0x20 <= codePoint && codePoint <= 0xD7FF)
                    || (0xE000 <= codePoint && codePoint <= 0xFFFD)
                    || (0x10000 <= codePoint && codePoint <= 0x10FFFF)
        }

        // protected final static char[] VERSION = {'v','e','r','s','i','o','n'};
        // protected final static char[] NCODING = {'n','c','o','d','i','n','g'};
        // protected final static char[] TANDALONE = {'t','a','n','d','a','l','o','n','e'};
        // protected final static char[] YES = {'y','e','s'};
        // protected final static char[] NO = {'n','o'};
        private val VERSION = "version".toCharArray()

        private val NCODING = "ncoding".toCharArray()

        private val TANDALONE = "tandalone".toCharArray()

        private val YES = "yes".toCharArray()

        private val NO = "no".toCharArray()

        // nameStart / name lookup tables based on XML 1.1 http://www.w3.org/TR/2001/WD-xml11-20011213/
        private const val LOOKUP_MAX = 0x400

        private const val LOOKUP_MAX_CHAR = LOOKUP_MAX.toChar()

        // private static int lookupNameStartChar[] = new int[ LOOKUP_MAX_CHAR / 32 ];
        // private static int lookupNameChar[] = new int[ LOOKUP_MAX_CHAR / 32 ];
        private val lookupNameStartChar = BooleanArray(LOOKUP_MAX)

        private val lookupNameChar = BooleanArray(LOOKUP_MAX)

        private fun setName(ch: Char) // { lookupNameChar[ (int)ch / 32 ] |= (1 << (ch % 32)); }
        {
            lookupNameChar[ch.code] = true
        }

        private fun setNameStart(ch: Char) // { lookupNameStartChar[ (int)ch / 32 ] |= (1 << (ch % 32)); setName(ch); }
        {
            lookupNameStartChar[ch.code] = true
            setName(ch)
        }

        init {
            setNameStart(':')
            run {
                var ch = 'A'
                while (ch <= 'Z') {
                    setNameStart(ch)
                    ++ch
                }
            }
            setNameStart('_')
            run {
                var ch = 'a'
                while (ch <= 'z') {
                    setNameStart(ch)
                    ++ch
                }
            }
            run {
                var ch = '\u00c0'
                while (ch <= '\u02FF') {
                    setNameStart(ch)
                    ++ch
                }
            }
            run {
                var ch = '\u0370'
                while (ch <= '\u037d') {
                    setNameStart(ch)
                    ++ch
                }
            }
            run {
                var ch = '\u037f'
                while (ch < '\u0400') {
                    setNameStart(ch)
                    ++ch
                }
            }

            setName('-')
            setName('.')
            run {
                var ch = '0'
                while (ch <= '9') {
                    setName(ch)
                    ++ch
                }
            }
            setName('\u00b7')
            var ch = '\u0300'
            while (ch <= '\u036f') {
                setName(ch)
                ++ch
            }
        }

        // protected boolean isNameStartChar( char ch )
        private fun isNameStartChar(ch: Char): Boolean {
            return if (ch < LOOKUP_MAX_CHAR)
                lookupNameStartChar[ch.code]
            else
                (ch <= '\u2027') || (ch >= '\u202A' && ch <= '\u218F') || (ch >= '\u2800' && ch <= '\uFFEF')

            // if(ch < LOOKUP_MAX_CHAR) return lookupNameStartChar[ ch ];
            // else return ch <= '\u2027'
            // || (ch >= '\u202A' && ch <= '\u218F')
            // || (ch >= '\u2800' && ch <= '\uFFEF')
            // ;
            // return false;
            // return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == ':'
            // || (ch >= '0' && ch <= '9');
            // if(ch < LOOKUP_MAX_CHAR) return (lookupNameStartChar[ (int)ch / 32 ] & (1 << (ch % 32))) != 0;
            // if(ch <= '\u2027') return true;
            // //[#x202A-#x218F]
            // if(ch < '\u202A') return false;
            // if(ch <= '\u218F') return true;
            // // added parts [#x2800-#xD7FF] | [#xE000-#xFDCF] | [#xFDE0-#xFFEF] | [#x10000-#x10FFFF]
            // if(ch < '\u2800') return false;
            // if(ch <= '\uFFEF') return true;
            // return false;

            // else return (supportXml11 && ( (ch < '\u2027') || (ch > '\u2029' && ch < '\u2200') ...
        }

        // protected boolean isNameChar( char ch )
        private fun isNameChar(ch: Char): Boolean {
            // return isNameStartChar(ch);

            // if(ch < LOOKUP_MAX_CHAR) return (lookupNameChar[ (int)ch / 32 ] & (1 << (ch % 32))) != 0;

            return if (ch < LOOKUP_MAX_CHAR)
                lookupNameChar[ch.code]
            else
                (ch <= '\u2027') || (ch >= '\u202A' && ch <= '\u218F') || (ch >= '\u2800' && ch <= '\uFFEF')

            // return false;
            // return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == ':'
            // || (ch >= '0' && ch <= '9');
            // if(ch < LOOKUP_MAX_CHAR) return (lookupNameStartChar[ (int)ch / 32 ] & (1 << (ch % 32))) != 0;

            // else return
            // else if(ch <= '\u2027') return true;
            // //[#x202A-#x218F]
            // else if(ch < '\u202A') return false;
            // else if(ch <= '\u218F') return true;
            // // added parts [#x2800-#xD7FF] | [#xE000-#xFDCF] | [#xFDE0-#xFFEF] | [#x10000-#x10FFFF]
            // else if(ch < '\u2800') return false;
            // else if(ch <= '\uFFEF') return true;
            // else return false;
        }

        private fun isS(ch: Char): Boolean {
            return (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t')
            // || (supportXml11 && (ch == '\u0085' || ch == '\u2028');
        }

        // protected boolean isChar(char ch) { return (ch < '\uD800' || ch > '\uDFFF')
        // ch != '\u0000' ch < '\uFFFE'
        // private char printable(char ch) { return ch; }
        private fun printable(ch: Int): String = when {
            ch == '\n'.code     -> "\\n"
            ch == '\r'.code     -> "\\r"
            ch == '\t'.code     -> "\\t"
            ch == '\''.code     -> "\\'"
            ch > 127 || ch < 32 -> "\\u" + toHexString(ch) // TODO implement
            else                -> if (isBmpCodePoint(ch)) {
                toString(ch.toChar()) // TODO implement
            } else {
                charArrayOf(highSurrogate(ch), lowSurrogate(ch)).concatToString()
            }
        }

        private fun printable(s: String?): String? {
            var s = s ?: return null
            val sLen: Int = s.codePointCount(0, s.length)
            val buf = StringBuilder(sLen + 10)
            for (i in 0..<sLen) {
                buf.append(printable(s.codePointAt(i)))
            }
            s = buf.toString()
            return s
        }
    }
} /*
 * Indiana University Extreme! Lab Software License, Version 1.2 Copyright (C) 2003 The Trustees of Indiana University.
 * All rights reserved. Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1) All redistributions of source code must retain the above copyright
 * notice, the list of authors in the original source code, this list of conditions and the disclaimer listed in this
 * license; 2) All redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the disclaimer listed in this license in the documentation and/or other materials provided with the distribution; 3)
 * Any documentation included with all redistributions must include the following acknowledgement: "This product
 * includes software developed by the Indiana University Extreme! Lab. For further information please visit
 * http://www.extreme.indiana.edu/" Alternatively, this acknowledgment may appear in the software itself, and wherever
 * such third-party acknowledgments normally appear. 4) The name "Indiana University" or "Indiana University Extreme!
 * Lab" shall not be used to endorse or promote products derived from this software without prior written permission
 * from Indiana University. For written permission, please contact http://www.extreme.indiana.edu/. 5) Products derived
 * from this software may not use "Indiana University" name nor may "Indiana University" appear in their name, without
 * prior written permission of the Indiana University. Indiana University provides no reassurances that the source code
 * provided does not infringe the patent or any other intellectual property rights of any other entity. Indiana
 * University disclaims any liability to any recipient for claims brought by any other entity based on infringement of
 * intellectual property rights or otherwise. LICENSEE UNDERSTANDS THAT SOFTWARE IS PROVIDED "AS IS" FOR WHICH NO
 * WARRANTIES AS TO CAPABILITIES OR ACCURACY ARE MADE. INDIANA UNIVERSITY GIVES NO WARRANTIES AND MAKES NO
 * REPRESENTATION THAT SOFTWARE IS FREE OF INFRINGEMENT OF THIRD PARTY PATENT, COPYRIGHT, OR OTHER PROPRIETARY RIGHTS.
 * INDIANA UNIVERSITY MAKES NO WARRANTIES THAT SOFTWARE IS FREE FROM "BUGS", "VIRUSES", "TROJAN HORSES", "TRAP
 * DOORS", "WORMS", OR OTHER HARMFUL CODE. LICENSEE ASSUMES THE ENTIRE RISK AS TO THE PERFORMANCE OF SOFTWARE AND/OR
 * ASSOCIATED MATERIALS, AND TO THE PERFORMANCE AND VALIDITY OF INFORMATION GENERATED USING SOFTWARE.
 */