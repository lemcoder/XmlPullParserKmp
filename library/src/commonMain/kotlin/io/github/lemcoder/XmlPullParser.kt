package io.github.lemcoder

import io.github.lemcoder.exceptions.IOException
import io.github.lemcoder.exceptions.XmlPullParserException


/**
 * XML Pull Parser is an interface that defines parsing functionality provided in
 * [XMLPULL V1 API](http://www.xmlpull.org/) (visit this website to learn more about API and its
 * implementations).
 *
 *
 * There are following different kinds of parser depending on which features are set:
 *
 *  * **non-validating** parser as defined in XML 1.0 spec when FEATURE_PROCESS_DOCDECL is set to true
 *  * **validating parser** as defined in XML 1.0 spec when FEATURE_VALIDATION is true (and that implies that
 * FEATURE_PROCESS_DOCDECL is true)
 *  * when FEATURE_PROCESS_DOCDECL is false (this is default and if different value is required necessary must be
 * changed before parsing is started) then parser behaves like XML 1.0 compliant non-validating parser under condition
 * that *no DOCDECL is present* in XML documents (internal entities can still be defined with
 * defineEntityReplacementText()). This mode of operation is intended **for operation in constrained environments**
 * such as J2ME.
 *
 *
 *
 * There are two key methods: next() and nextToken(). While next() provides access to high level parsing events,
 * nextToken() allows access to lower level tokens.
 *
 *
 * The current event state of the parser can be determined by calling the [getEventType()](#getEventType())
 * method. Initially, the parser is in the [START_DOCUMENT](#START_DOCUMENT) state.
 *
 *
 * The method [next()](#next()) advances the parser to the next event. The int value returned from next
 * determines the current parser state and is identical to the value returned from following calls to getEventType ().
 *
 *
 * The following event types are seen by next()
 * <dl>
 * <dt>[START_TAG](#START_TAG)
</dt> * <dd>An XML start tag was read.
</dd> * <dt>[TEXT](#TEXT)
</dt> * <dd>Text content was read; the text content can be retrieved using the getText() method. (when in validating mode
 * next() will not report ignorable whitespaces, use nextToken() instead)
</dd> * <dt>[END_TAG](#END_TAG)
</dt> * <dd>An end tag was read
</dd> * <dt>[END_DOCUMENT](#END_DOCUMENT)
</dt> * <dd>No more events are available
</dd></dl> *
 *
 *
 * after first next() or nextToken() (or any other next*() method) is called user application can obtain XML version,
 * standalone and encoding from XML declaration in following ways:
 *
 *  * **version**: getProperty(&quot;[http://xmlpull.org/v1/doc/properties.html#xmldecl-version](http://xmlpull.org/v1/doc/properties.html#xmldecl-version)&quot;)
 * returns String ("1.0") or null if XMLDecl was not read or if property is not supported
 *  * **standalone**: getProperty(&quot;[http://xmlpull.org/v1/doc/features.html#xmldecl-standalone](http://xmlpull.org/v1/doc/features.html#xmldecl-standalone)&quot;)
 * returns Boolean: null if there was no standalone declaration or if property is not supported otherwise returns
 * Boolean(true) if standalone="yes" and Boolean(false) when standalone="no"
 *  * **encoding**: obtained from getInputEncoding() null if stream had unknown encoding (not set in setInputStream)
 * and it was not declared in XMLDecl
 *
 * A minimal example for using this API may look as follows:
 *
 * <pre>
 * import java.io.IOException;
 * import java.io.StringReader;
 *
 * import org.xmlpull.v1.XmlPullParser;
 * import org.xmlpull.v1.XmlPullParserException;
 * import org.xmlpull.v1.XmlPullParserFactory;
 *
 * public class SimpleXmlPullApp
 * {
 *
 * public static void main (String args[])
 * throws XmlPullParserException, IOException
 * {
 * XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
 * factory.setNamespaceAware(true);
 * XmlPullParser xpp = factory.newPullParser();
 *
 * xpp.setInput( new StringReader ( "&lt;foo%gt;Hello World!&lt;/foo&gt;" ) );
 * int eventType = xpp.getEventType();
 * while (eventType != xpp.END_DOCUMENT) {
 * if(eventType == xpp.START_DOCUMENT) {
 * System.out.println("Start document");
 * } else if(eventType == xpp.END_DOCUMENT) {
 * System.out.println("End document");
 * } else if(eventType == xpp.START_TAG) {
 * System.out.println("Start tag "+xpp.getName());
 * } else if(eventType == xpp.END_TAG) {
 * System.out.println("End tag "+xpp.getName());
 * } else if(eventType == xpp.TEXT) {
 * System.out.println("Text "+xpp.getText());
 * }
 * eventType = xpp.next();
 * }
 * }
 * }
</pre> *
 *
 *
 * The above example will generate the following output:
 *
 * <pre>
 * Start document
 * Start tag foo
 * Text Hello World!
 * End tag foo
</pre> *
 *
 * For more details on API usage, please refer to the quick Introduction available at
 * [http://www.xmlpull.org](http://www.xmlpull.org)
 *
 * @see .defineEntityReplacementText
 *
 * @see .getName
 *
 * @see .getNamespace
 *
 * @see .getText
 *
 * @see .next
 *
 * @see .nextToken
 *
 * @see .setInput
 *
 * @see .FEATURE_PROCESS_DOCDECL
 *
 * @see .FEATURE_VALIDATION
 *
 * @see .START_DOCUMENT
 *
 * @see .START_TAG
 *
 * @see .TEXT
 *
 * @see .END_TAG
 *
 * @see .END_DOCUMENT
 *
 * @author [Stefan Haustein](http://www-ai.cs.uni-dortmund.de/PERSONAL/haustein.html)
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
interface XmlPullParser {
    /**
     * Use this call to change the general behaviour of the parser, such as namespace processing or doctype declaration
     * handling. This method must be called before the first call to next or nextToken. Otherwise, an exception is
     * thrown.
     *
     *
     * Example: call setFeature(FEATURE_PROCESS_NAMESPACES, true) in order to switch on namespace processing. The
     * initial settings correspond to the properties requested from the XML Pull Parser factory. If none were requested,
     * all features are deactivated by default.
     * @param name feature name
     * @param state feature state
     * @exception XmlPullParserException If the feature is not supported or can not be set
     * @exception IllegalArgumentException If string with the feature name is null
     */
    @Throws(XmlPullParserException::class)
    fun setFeature(name: String?, state: Boolean)

    /**
     * Returns the current value of the given feature.
     *
     *
     * **Please note:** unknown features are **always** returned as false.
     *
     * @param name The name of feature to be retrieved.
     * @return The value of the feature.
     * @exception IllegalArgumentException if string the feature name is null
     */
    fun getFeature(name: String?): Boolean

    /**
     * Set the value of a property. The property name is any fully-qualified URI.
     * @param name property name
     * @param value property value
     * @exception XmlPullParserException If the property is not supported or can not be set
     * @exception IllegalArgumentException If string with the property name is null
     * @throws XmlPullParserException parsing issue
     */
    @Throws(XmlPullParserException::class)
    fun setProperty(name: String?, value: Any?)

    /**
     * Look up the value of a property. The property name is any fully-qualified URI.
     *
     *
     * **NOTE:** unknown properties are **always** returned as null.
     *
     * @param name The name of property to be retrieved.
     * @return The value of named property.
     */
    fun getProperty(name: String?): Any?

    /**
     * Set the input source for parser to the given reader and resets the parser. The event type is set to the initial
     * value START_DOCUMENT. Setting the reader to null will just stop parsing and reset parser state, allowing the
     * parser to free internal resources such as parsing buffers.
     * @param in the Reader
     * @throws XmlPullParserException parsing issue
     */
    @Throws(XmlPullParserException::class)
    fun setInput(`in`: Reader?)

    /**
     * Sets the input stream the parser is going to process. This call resets the parser state and sets the event type
     * to the initial value START_DOCUMENT.
     *
     *
     * **NOTE:** If an input encoding string is passed, it MUST be used. Otherwise, if inputEncoding is
     * null, the parser SHOULD try to determine input encoding following XML 1.0 specification (see below). If encoding
     * detection is supported then following feature [http://xmlpull.org/v1/doc/features.html#detect-encoding](http://xmlpull.org/v1/doc/features.html#detect-encoding)
     * MUST be true and otherwise it must be false
     *
     * @param inputStream contains a raw byte input stream of possibly unknown encoding (when inputEncoding is null).
     * @param inputEncoding if not null it MUST be used as encoding for inputStream
     * @throws XmlPullParserException parsing issue
     */
    @Throws(XmlPullParserException::class)
    fun setInput(inputStream: InputStream?, inputEncoding: String?)

    /**
     * @return the input encoding if known, null otherwise. If setInput(InputStream, inputEncoding) was called with an
     * inputEncoding value other than null, this value must be returned from this method. Otherwise, if inputEncoding is
     * null and the parser supports the encoding detection feature
     * (http://xmlpull.org/v1/doc/features.html#detect-encoding), it must return the detected encoding. If
     * setInput(Reader) was called, null is returned. After first call to next if XML declaration was present this
     * method will return encoding declared.
     */
    val inputEncoding: String?

    /**
     * Set new value for entity replacement text as defined in
     * [XML 1.0 Section 4.5 Construction of Internal Entity
 * Replacement Text](http://www.w3.org/TR/REC-xml#intern-replacement). If FEATURE_PROCESS_DOCDECL or FEATURE_VALIDATION are set, calling this function will result
     * in an exception -- when processing of DOCDECL is enabled, there is no need to the entity replacement text
     * manually.
     *
     *
     * The motivation for this function is to allow very small implementations of XMLPULL that will work in J2ME
     * environments. Though these implementations may not be able to process the document type declaration, they still
     * can work with known DTDs by using this function.
     *
     *
     * **Please notes:** The given value is used literally as replacement text and it corresponds to declaring entity
     * in DTD that has all special characters escaped: left angle bracket is replaced with &amp;lt;, ampersand with
     * &amp;amp; and so on.
     *
     *
     * **Note:** The given value is the literal replacement text and must not contain any other entity reference (if
     * it contains any entity reference there will be no further replacement).
     *
     *
     * **Note:** The list of pre-defined entity names will always contain standard XML entities such as amp
     * (&amp;amp;), lt (&amp;lt;), gt (&amp;gt;), quot (&amp;quot;), and apos (&amp;apos;). Those cannot be redefined by
     * this method!
     * @param entityName entity name
     * @param replacementText remplacement
     * @see .setInput
     *
     * @see .FEATURE_PROCESS_DOCDECL
     *
     * @see .FEATURE_VALIDATION
     *
     * @throws XmlPullParserException parsing issue
     */
    @Throws(XmlPullParserException::class)
    fun defineEntityReplacementText(entityName: String?, replacementText: String?)

    /**
     * @return the numbers of elements in the namespace stack for the given depth. If namespaces are not enabled, 0 is
     * returned.
     *
     *
     * **NOTE:** when parser is on END_TAG then it is allowed to call this function with getDepth()+1 argument to
     * retrieve position of namespace prefixes and URIs that were declared on corresponding START_TAG.
     *
     *
     * **NOTE:** to retrieve lsit of namespaces declared in current element:
     *
     * <pre>
     * XmlPullParser pp = ...
     * int nsStart = pp.getNamespaceCount(pp.getDepth()-1);
     * int nsEnd = pp.getNamespaceCount(pp.getDepth());
     * for (int i = nsStart; i &gt; nsEnd; i++) {
     * String prefix = pp.getNamespacePrefix(i);
     * String ns = pp.getNamespaceUri(i);
     * // ...
     * }
    </pre> *
     *
     * @see .getNamespacePrefix
     *
     * @see .getNamespaceUri
     *
     * @see .getNamespace
     * @see .getNamespace
     * @param depth depth
     * @throws XmlPullParserException parsing issue
     */
    @Throws(XmlPullParserException::class)
    fun getNamespaceCount(depth: Int): Int

    /**
     * @return Returns the namespace prefix for the given position in the namespace stack. Default namespace declaration
     * (xmlns='...') will have null as prefix. If the given index is out of range, an exception is thrown.
     *
     * **Please note:** when the parser is on an END_TAG, namespace prefixes that were declared in the corresponding
     * START_TAG are still accessible although they are no longer in scope.
     * namespace prefix
     * @param pos namespace stack position
     * @throws XmlPullParserException parsing issue
     */
    @Throws(XmlPullParserException::class)
    fun getNamespacePrefix(pos: Int): String?

    /**
     * @return Returns the namespace URI for the given position in the namespace stack If the position is out of range, an
     * exception is thrown.
     *
     * **NOTE:** when parser is on END_TAG then namespace prefixes that were declared in corresponding START_TAG are
     * still accessible even though they are not in scope
     * @throws XmlPullParserException parsing issue
     * @param pos namespace stack position
     */
    @Throws(XmlPullParserException::class)
    fun getNamespaceUri(pos: Int): String?

    /**
     * @return the URI corresponding to the given prefix, depending on current state of the parser.
     *
     *
     * If the prefix was not declared in the current scope, null is returned. The default namespace is included in the
     * namespace table and is available via getNamespace (null).
     *
     *
     * This method is a convenience method for
     *
     * <pre>
     * for ( int i = getNamespaceCount( getDepth() ) - 1; i &gt;= 0; i-- )
     * {
     * if ( getNamespacePrefix( i ).equals( prefix ) )
     * {
     * return getNamespaceUri( i );
     * }
     * }
     * return null;
    </pre> *
     *
     *
     * **Please note:** parser implementations may provide more efficient lookup, e.g. using a Hashtable.
     * The 'xml' prefix is bound to "http://www.w3.org/XML/1998/namespace", as defined in the
     * [Namespaces in XML](http://www.w3.org/TR/REC-xml-names/#ns-using) specification. Analogous, the
     * 'xmlns' prefix is resolved to [http://www.w3.org/2000/xmlns/](http://www.w3.org/2000/xmlns/)
     * @param prefix given prefix
     * @see .getNamespaceCount
     *
     * @see .getNamespacePrefix
     *
     * @see .getNamespaceUri
     */
    fun getNamespace(prefix: String?): String?

    // --------------------------------------------------------------------------
    // miscellaneous reporting methods
    /**
     * @return the current depth of the element. Outside the root element, the depth is 0. The depth is incremented by 1
     * when a start tag is reached. The depth is decremented AFTER the end tag event was observed.
     *
     * <pre>
     * &lt;!-- outside --&gt;     0
     * &lt;root&gt;                  1
     * sometext                 1
     * &lt;foobar&gt;         2
     * &lt;/foobar&gt;        2
     * &lt;/root&gt;              1
     * &lt;!-- outside --&gt;     0
    </pre> *
     */
    val depth: Int

    /**
     * @return a short text describing the current parser state, including the position, a description of the current
     * event and the data source if known. This method is especially useful to provide meaningful error messages and for
     * debugging purposes.
     */
    val positionDescription: String?

    /**
     * Returns the current line number, starting from 1. When the parser does not know the current line number or can
     * not determine it, -1 is returned (e.g. for WBXML).
     *
     * @return current line number or -1 if unknown.
     */
    val lineNumber: Int

    /**
     * Returns the current column number, starting from 0. When the parser does not know the current column number or
     * can not determine it, -1 is returned (e.g. for WBXML).
     *
     * @return current column number or -1 if unknown.
     */
    val columnNumber: Int

    // --------------------------------------------------------------------------
    // TEXT related methods
    @get:Throws(XmlPullParserException::class)
    val isWhitespace: Boolean

    /**
     * @return  the text content of the current event as String. The value returned depends on current event type, for
     * example for TEXT event it is element content (this is typical case when next() is used). See description of
     * nextToken() for detailed description of possible returned values for different types of events.
     *
     *
     * **NOTE:** in case of ENTITY_REF, this method returns the entity replacement text (or null if not
     * available). This is the only case where getText() and getTextCharacters() return different values.
     *
     * @see .getEventType
     *
     * @see .next
     *
     * @see .nextToken
     */
    fun getText(): String?

    /**
     * Returns the buffer that contains the text of the current event, as well as the start offset and length relevant
     * for the current event. See getText(), next() and nextToken() for description of possible returned values.
     *
     *
     * **Please note:** this buffer must not be modified and its content MAY change after a call to next()
     * or nextToken(). This method will always return the same value as getText(), except for ENTITY_REF. In the case of
     * ENTITY ref, getText() returns the replacement text and this method returns the actual input buffer containing the
     * entity name. If getText() returns null, this method returns null as well and the values returned in the holder
     * array MUST be -1 (both start and length).
     *
     * @see .getText
     *
     * @see .next
     *
     * @see .nextToken
     *
     * @param holderForStartAndLength Must hold an 2-element int array into which the start offset and length values
     * will be written.
     * @return char buffer that contains the text of the current event (null if the current event has no text
     * associated).
     */
    fun getTextCharacters(holderForStartAndLength: IntArray?): CharArray?

    // --------------------------------------------------------------------------
    // START_TAG / END_TAG shared methods
    /**
     * @return the namespace URI of the current element. The default namespace is represented as empty string. If
     * namespaces are not enabled, an empty String ("") is always returned. The current event must be START_TAG or
     * END_TAG; otherwise, null is returned.
     */
    val namespace: String?

    /**
     * @return For START_TAG or END_TAG events, the (local) name of the current element is returned when namespaces are enabled.
     * When namespace processing is disabled, the raw name is returned. For ENTITY_REF events, the entity name is
     * returned. If the current event is not START_TAG, END_TAG, or ENTITY_REF, null is returned.
     *
     *
     * **Please note:** To reconstruct the raw element name when namespaces are enabled and the prefix is not null,
     * you will need to add the prefix and a colon to localName..
     */
    val name: String?

    /**
     * @return the prefix of the current element. If the element is in the default namespace (has no prefix), null is
     * returned. If namespaces are not enabled, or the current event is not START_TAG or END_TAG, null is returned.
     */
    val prefix: String?

    @get:Throws(XmlPullParserException::class)
    val isEmptyElementTag: Boolean

    // --------------------------------------------------------------------------
    // START_TAG Attributes retrieval methods
    /**
     * @return the number of attributes of the current start tag, or -1 if the current event type is not START_TAG
     *
     * @see .getAttributeNamespace
     *
     * @see .getAttributeName
     *
     * @see .getAttributePrefix
     *
     * @see .getAttributeValue
     */
    fun getAttributeCount(): Int

    /**
     * Returns the namespace URI of the attribute with the given index (starts from 0). Returns an empty string ("") if
     * namespaces are not enabled or the attribute has no namespace. Throws an IndexOutOfBoundsException if the index is
     * out of range or the current event type is not START_TAG.
     *
     *
     * **NOTE:** if FEATURE_REPORT_NAMESPACE_ATTRIBUTES is set then namespace attributes (xmlns:ns='...')
     * must be reported with namespace [http://www.w3.org/2000/xmlns/](http://www.w3.org/2000/xmlns/) (visit
     * this URL for description!). The default namespace attribute (xmlns="...") will be reported with empty namespace.
     *
     *
     * **NOTE:**The xml prefix is bound as defined in
     * [Namespaces in XML](http://www.w3.org/TR/REC-xml-names/#ns-using) specification to
     * "http://www.w3.org/XML/1998/namespace".
     *
     * @param index zero based index of attribute
     * @return attribute namespace, empty string ("") is returned if namespaces processing is not enabled or namespaces
     * processing is enabled but attribute has no namespace (it has no prefix).
     */
    fun getAttributeNamespace(index: Int): String?

    /**
     * Returns the local name of the specified attribute if namespaces are enabled or just attribute name if namespaces
     * are disabled. Throws an IndexOutOfBoundsException if the index is out of range or current event type is not
     * START_TAG.
     *
     * @param index zero based index of attribute
     * @return attribute name (null is never returned)
     */
    fun getAttributeName(index: Int): String?

    /**
     * Returns the prefix of the specified attribute Returns null if the element has no prefix. If namespaces are
     * disabled it will always return null. Throws an IndexOutOfBoundsException if the index is out of range or current
     * event type is not START_TAG.
     *
     * @param index zero based index of attribute
     * @return attribute prefix or null if namespaces processing is not enabled.
     */
    fun getAttributePrefix(index: Int): String?

    /**
     * Returns the type of the specified attribute If parser is non-validating it MUST return CDATA.
     *
     * @param index zero based index of attribute
     * @return attribute type (null is never returned)
     */
    fun getAttributeType(index: Int): String?

    /**
     * Returns if the specified attribute was not in input was declared in XML. If parser is non-validating it MUST
     * always return false. This information is part of XML infoset:
     *
     * @param index zero based index of attribute
     * @return false if attribute was in input
     */
    fun isAttributeDefault(index: Int): Boolean

    /**
     * Returns the given attributes value. Throws an IndexOutOfBoundsException if the index is out of range or current
     * event type is not START_TAG.
     *
     *
     * **NOTE:** attribute value must be normalized (including entity replacement text if PROCESS_DOCDECL
     * is false) as described in [XML 1.0 section 3.3.3
 * Attribute-Value Normalization](http://www.w3.org/TR/REC-xml#AVNormalize)
     *
     * @see .defineEntityReplacementText
     *
     * @param index zero based index of attribute
     * @return value of attribute (null is never returned)
     */
    fun getAttributeValue(index: Int): String?

    /**
     * Returns the attributes value identified by namespace URI and namespace localName. If namespaces are disabled
     * namespace must be null. If current event type is not START_TAG then IndexOutOfBoundsException will be thrown.
     *
     *
     * **NOTE:** attribute value must be normalized (including entity replacement text if PROCESS_DOCDECL
     * is false) as described in [XML 1.0 section 3.3.3
 * Attribute-Value Normalization](http://www.w3.org/TR/REC-xml#AVNormalize)
     *
     * @see .defineEntityReplacementText
     *
     * @param namespace Namespace of the attribute if namespaces are enabled otherwise must be null
     * @param name If namespaces enabled local name of attribute otherwise just attribute name
     * @return value of attribute or null if attribute with given name does not exist
     */
    fun getAttributeValue(namespace: String?, name: String?): String?

    // --------------------------------------------------------------------------
    // actual parsing methods
    @get:Throws(XmlPullParserException::class)
    val eventType: Int

    /**
     * @return Get next parsing event - element content wil be coalesced and only one TEXT event must be returned for whole
     * element content (comments and processing instructions will be ignored and entity references must be expanded or
     * exception mus be thrown if entity reference can not be expanded). If element content is empty (content is "")
     * then no TEXT event will be reported.
     *
     *
     * **NOTE:** empty element (such as &lt;tag/&gt;) will be reported with two separate events: START_TAG, END_TAG - it
     * must be so to preserve parsing equivalency of empty element to &lt;tag&gt;&lt;/tag&gt;. (see isEmptyElementTag ())
     *
     * @see .isEmptyElementTag
     *
     * @see .START_TAG
     *
     * @see .TEXT
     *
     * @see .END_TAG
     *
     * @see .END_DOCUMENT
     *
     * @throws XmlPullParserException parsing issue
     * @throws IOException io issue
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun next(): Int

    /**
     * This method works similarly to next() but will expose additional event types (COMMENT, CDSECT, DOCDECL,
     * ENTITY_REF, PROCESSING_INSTRUCTION, or IGNORABLE_WHITESPACE) if they are available in input.
     *
     *
     * If special feature [FEATURE_XML_ROUNDTRIP](http://xmlpull.org/v1/doc/features.html#xml-roundtrip)
     * (identified by URI: http://xmlpull.org/v1/doc/features.html#xml-roundtrip) is enabled it is possible to do XML
     * document round trip ie. reproduce exactly on output the XML input using getText(): returned content is always
     * unnormalized (exactly as in input). Otherwise returned content is end-of-line normalized as described
     * [XML 1.0 End-of-Line Handling](http://www.w3.org/TR/REC-xml#sec-line-ends) and. Also when this feature
     * is enabled exact content of START_TAG, END_TAG, DOCDECL and PROCESSING_INSTRUCTION is available.
     *
     *
     * Here is the list of tokens that can be returned from nextToken() and what getText() and getTextCharacters()
     * @return
     * <dl>
     * <dt>START_DOCUMENT
    </dt> * <dd>null
    </dd> * <dt>END_DOCUMENT
    </dt> * <dd>null
    </dd> * <dt>START_TAG
    </dt> * <dd>null unless FEATURE_XML_ROUNDTRIP enabled and then returns XML tag, ex: &lt;tag attr='val'&gt;
    </dd> * <dt>END_TAG
    </dt> * <dd>null unless FEATURE_XML_ROUNDTRIP id enabled and then returns XML tag, ex: &lt;/tag&gt;
    </dd> * <dt>TEXT
    </dt> * <dd>return element content. <br></br>
     * Note: that element content may be delivered in multiple consecutive TEXT events.
    </dd> * <dt>IGNORABLE_WHITESPACE
    </dt> * <dd>return characters that are determined to be ignorable white space. If the FEATURE_XML_ROUNDTRIP is enabled
     * all whitespace content outside root element will always reported as IGNORABLE_WHITESPACE otherwise reporting is
     * optional. <br></br>
     * Note: that element content may be delivered in multiple consecutive IGNORABLE_WHITESPACE events.
    </dd> * <dt>CDSECT
    </dt> * <dd>return text *inside* CDATA (ex. 'fo&lt;o' from &lt;!CDATA[fo&lt;o]]&gt;)
    </dd> * <dt>PROCESSING_INSTRUCTION
    </dt> * <dd>if FEATURE_XML_ROUNDTRIP is true return exact PI content ex: 'pi foo' from &lt;?pi foo?&gt; otherwise it may be
     * exact PI content or concatenation of PI target, space and data so for example for &lt;?target data?&gt; string
     * &quot;target data&quot; may be returned if FEATURE_XML_ROUNDTRIP is false.
    </dd> * <dt>COMMENT
    </dt> * <dd>return comment content ex. 'foo bar' from &lt;!--foo bar--&gt;
    </dd> * <dt>ENTITY_REF
    </dt> * <dd>getText() MUST return entity replacement text if PROCESS_DOCDECL is false otherwise getText() MAY return
     * null, additionally getTextCharacters() MUST return entity name (for example 'entity_name' for &amp;entity_name;).
     * <br></br>
     * **NOTE:** this is the only place where value returned from getText() and getTextCharacters() **are
     * different** <br></br>
     * **NOTE:** it is user responsibility to resolve entity reference if PROCESS_DOCDECL is false and there is no
     * entity replacement text set in defineEntityReplacementText() method (getText() will be null) <br></br>
     * **NOTE:** character entities (ex. &amp;#32;) and standard entities such as &amp;amp; &amp;lt; &amp;gt;
     * &amp;quot; &amp;apos; are reported as well and are **not** reported as TEXT tokens but as ENTITY_REF tokens!
     * This requirement is added to allow to do roundtrip of XML documents!
    </dd> * <dt>DOCDECL
    </dt> * <dd>if FEATURE_XML_ROUNDTRIP is true or PROCESS_DOCDECL is false then return what is inside of DOCDECL for
     * example it returns:
     *
     * <pre>
     * &quot; titlepage SYSTEM "http://www.foo.bar/dtds/typo.dtd"
     * [&lt;!ENTITY % active.links "INCLUDE"&gt;]&quot;
    </pre> *
     *
     *
     * for input document that contained:
     *
     * <pre>
     * &lt;!DOCTYPE titlepage SYSTEM "http://www.foo.bar/dtds/typo.dtd"
     * [&lt;!ENTITY % active.links "INCLUDE"&gt;]&gt;
    </pre> *
     *
     * otherwise if FEATURE_XML_ROUNDTRIP is false and PROCESS_DOCDECL is true then what is returned is undefined (it
     * may be even null)</dd>
    </dl> *
     *
     *
     * **NOTE:** there is no guarantee that there will only one TEXT or IGNORABLE_WHITESPACE event from
     * nextToken() as parser may chose to deliver element content in multiple tokens (dividing element content into
     * chunks)
     *
     *
     * **NOTE:** whether returned text of token is end-of-line normalized is depending on
     * FEATURE_XML_ROUNDTRIP.
     *
     *
     * **NOTE:** XMLDecl (&lt;?xml ...?&gt;) is not reported but its content is available through optional
     * properties (see class description above).
     * @throws XmlPullParserException parsing issue
     * @throws IOException io issue
     * @see .next
     *
     * @see .START_TAG
     *
     * @see .TEXT
     *
     * @see .END_TAG
     *
     * @see .END_DOCUMENT
     *
     * @see .COMMENT
     *
     * @see .DOCDECL
     *
     * @see .PROCESSING_INSTRUCTION
     *
     * @see .ENTITY_REF
     *
     * @see .IGNORABLE_WHITESPACE
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun nextToken(): Int

    // -----------------------------------------------------------------------------
    // utility methods to mak XML parsing easier ...
    /**
     * Test if the current event is of the given type and if the namespace and name do match. null will match any
     * namespace and any name. If the test is not passed, an exception is thrown. The exception text indicates the
     * parser position, the expected event and the current event that is not meeting the requirement.
     *
     *
     * Essentially it does this
     *
     * <pre>
     * if ( type != getEventType() || ( namespace != null &amp;&amp; !namespace.equals( getNamespace() ) )
     * || ( name != null &amp;&amp; !name.equals( getName() ) ) )
     * throw new XmlPullParserException( "expected " + TYPES[type] + getPositionDescription() );
    </pre> *
     * @param type type
     * @param name name
     * @param namespace namespace
     * @throws XmlPullParserException parsing issue
     * @throws IOException io issue
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun require(type: Int, namespace: String?, name: String?)

    /**
     * If current event is START_TAG then if next element is TEXT then element content is returned or if next event is
     * END_TAG then empty string is returned, otherwise exception is thrown. After calling this function successfully
     * parser will be positioned on END_TAG.
     *
     *
     * The motivation for this function is to allow to parse consistently both empty elements and elements that has non
     * empty content, for example for input:
     *
     *  1. &lt;tag&gt;foo&lt;/tag&gt;
     *  1. &lt;tag&gt;&lt;/tag&gt; (which is equivalent to &lt;tag/&gt; both input can be parsed with the same code:
     *
     * <pre>
     * p.nextTag()
     * p.requireEvent(p.START_TAG, "", "tag");
     * String content = p.nextText();
     * p.requireEvent(p.END_TAG, "", "tag");
    </pre> *
     *
     * This function together with nextTag make it very easy to parse XML that has no mixed content.
     *
     *
     * Essentially it does this
     *
     * <pre>
     * if ( getEventType() != START_TAG )
     * {
     * throw new XmlPullParserException( "parser must be on START_TAG to read next text", this, null );
     * }
     * int eventType = next();
     * if ( eventType == TEXT )
     * {
     * String result = getText();
     * eventType = next();
     * if ( eventType != END_TAG )
     * {
     * throw new XmlPullParserException( "event TEXT it must be immediately followed by END_TAG", this, null );
     * }
     * return result;
     * }
     * else if ( eventType == END_TAG )
     * {
     * return "";
     * }
     * else
     * {
     * throw new XmlPullParserException( "parser must be on START_TAG or TEXT to read text", this, null );
     * }
    </pre> *
     * @return see description
     * @throws XmlPullParserException parsing issue
     * @throws IOException io issue
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun nextText(): String?

    /**
     * Call next() and return event if it is START_TAG or END_TAG otherwise throw an exception. It will skip whitespace
     * TEXT before actual tag if any.
     *
     *
     * essentially it does this
     *
     * <pre>
     * int eventType = next();
     * if ( eventType == TEXT &amp;&amp; isWhitespace() )
     * { // skip whitespace
     * eventType = next();
     * }
     * if ( eventType != START_TAG &amp;&amp; eventType != END_TAG )
     * {
     * throw new XmlPullParserException( "expected start or end tag", this, null );
     * }
     * return eventType;
    </pre> *
     * @return see description
     * @throws XmlPullParserException parsing issue
     * @throws
     * IOException io issue
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun nextTag(): Int

    companion object {
        /** This constant represents the default namespace (empty string "")  */
        const val NO_NAMESPACE: String = ""

        // ----------------------------------------------------------------------------
        // EVENT TYPES as reported by next()
        /**
         * Signalize that parser is at the very beginning of the document and nothing was read yet. This event type can only
         * be observed by calling getEvent() before the first call to next(), nextToken, or nextTag()).
         *
         * @see .next
         *
         * @see .nextToken
         */
        const val START_DOCUMENT: Int = 0

        /**
         * Logical end of the xml document. Returned from getEventType, next() and nextToken() when the end of the input
         * document has been reached.
         *
         *
         * **NOTE:** calling again [next()](#next()) or [nextToken()](#nextToken()) will
         * result in exception being thrown.
         *
         * @see .next
         *
         * @see .nextToken
         */
        const val END_DOCUMENT: Int = 1

        /**
         * Returned from getEventType(), [next()](#next()), [nextToken()](#nextToken()) when a start
         * tag was read. The name of start tag is available from getName(), its namespace and prefix are available from
         * getNamespace() and getPrefix() if [namespaces are enabled](#FEATURE_PROCESS_NAMESPACES). See
         * getAttribute* methods to retrieve element attributes. See getNamespace* methods to retrieve newly declared
         * namespaces.
         *
         * @see .next
         *
         * @see .nextToken
         *
         * @see .getName
         *
         * @see .getPrefix
         *
         * @see .getNamespace
         *
         * @see .getAttributeCount
         *
         * @see .getDepth
         *
         * @see .getNamespaceCount
         *
         * @see .getNamespace
         *
         * @see .FEATURE_PROCESS_NAMESPACES
         */
        const val START_TAG: Int = 2

        /**
         * Returned from getEventType(), [next()](#next()), or [nextToken()](#nextToken()) when an end
         * tag was read. The name of start tag is available from getName(), its namespace and prefix are available from
         * getNamespace() and getPrefix().
         *
         * @see .next
         *
         * @see .nextToken
         *
         * @see .getName
         *
         * @see .getPrefix
         *
         * @see .getNamespace
         *
         * @see .FEATURE_PROCESS_NAMESPACES
         */
        const val END_TAG: Int = 3

        /**
         * Character data was read and will is available by calling getText().
         *
         *
         * **Please note:** [next()](#next()) will accumulate multiple events into one TEXT event,
         * skipping IGNORABLE_WHITESPACE, PROCESSING_INSTRUCTION and COMMENT events, In contrast,
         * [nextToken()](#nextToken()) will stop reading text when any other event is observed. Also, when the
         * state was reached by calling next(), the text value will be normalized, whereas getText() will return
         * unnormalized content in the case of nextToken(). This allows an exact roundtrip without changing line ends when
         * examining low level events, whereas for high level applications the text is normalized appropriately.
         *
         * @see .next
         *
         * @see .nextToken
         *
         * @see .getText
         */
        const val TEXT: Int = 4

        // ----------------------------------------------------------------------------
        // additional events exposed by lower level nextToken()
        /**
         * A CDATA sections was just read; this token is available only from calls to
         * [nextToken()](#nextToken()). A call to next() will accumulate various text events into a single event
         * of type TEXT. The text contained in the CDATA section is available by calling getText().
         *
         * @see .nextToken
         *
         * @see .getText
         */
        const val CDSECT: Int = 5

        /**
         * An entity reference was just read; this token is available from [nextToken()](#nextToken()) only. The
         * entity name is available by calling getName(). If available, the replacement text can be obtained by calling
         * getTextt(); otherwise, the user is responsible for resolving the entity reference. This event type is never
         * returned from next(); next() will accumulate the replacement text and other text events to a single TEXT event.
         *
         * @see .nextToken
         *
         * @see .getText
         */
        const val ENTITY_REF: Int = 6

        /**
         * Ignorable whitespace was just read. This token is available only from [nextToken()](#nextToken())).
         * For non-validating parsers, this event is only reported by nextToken() when outside the root element. Validating
         * parsers may be able to detect ignorable whitespace at other locations. The ignorable whitespace string is
         * available by calling getText()
         *
         *
         * **NOTE:** this is different from calling the isWhitespace() method, since text content may be
         * whitespace but not ignorable. Ignorable whitespace is skipped by next() automatically; this event type is never
         * returned from next().
         *
         * @see .nextToken
         *
         * @see .getText
         */
        const val IGNORABLE_WHITESPACE: Int = 7

        /**
         * An XML processing instruction declaration was just read. This event type is available only via
         * [nextToken()](#nextToken()). getText() will return text that is inside the processing instruction.
         * Calls to next() will skip processing instructions automatically.
         *
         * @see .nextToken
         *
         * @see .getText
         */
        const val PROCESSING_INSTRUCTION: Int = 8

        /**
         * An XML comment was just read. This event type is this token is available via
         * [nextToken()](#nextToken()) only; calls to next() will skip comments automatically. The content of the
         * comment can be accessed using the getText() method.
         *
         * @see .nextToken
         *
         * @see .getText
         */
        const val COMMENT: Int = 9

        /**
         * An XML document type declaration was just read. This token is available from
         * [nextToken()](#nextToken()) only. The unparsed text inside the doctype is available via the getText()
         * method.
         *
         * @see .nextToken
         *
         * @see .getText
         */
        const val DOCDECL: Int = 10

        /**
         * This array can be used to convert the event type integer constants such as START_TAG or TEXT to to a string. For
         * example, the value of TYPES[START_TAG] is the string "START_TAG". This array is intended for diagnostic output
         * only. Relying on the contents of the array may be dangerous since malicious applications may alter the array,
         * although it is final, due to limitations of the Java language.
         */
        val TYPES: Array<String> = arrayOf(
            "START_DOCUMENT",
            "END_DOCUMENT",
            "START_TAG",
            "END_TAG",
            "TEXT",
            "CDSECT",
            "ENTITY_REF",
            "IGNORABLE_WHITESPACE",
            "PROCESSING_INSTRUCTION",
            "COMMENT",
            "DOCDECL"
        )

        // ----------------------------------------------------------------------------
        // namespace related features
        /**
         * This feature determines whether the parser processes namespaces. As for all features, the default value is false.
         *
         *
         * **NOTE:** The value can not be changed during parsing an must be set before parsing.
         *
         * @see .getFeature
         *
         * @see .setFeature
         */
        const val FEATURE_PROCESS_NAMESPACES: String = "http://xmlpull.org/v1/doc/features.html#process-namespaces"

        /**
         * This feature determines whether namespace attributes are exposed via the attribute access methods. Like all
         * features, the default value is false. This feature cannot be changed during parsing.
         *
         * @see .getFeature
         *
         * @see .setFeature
         */
        const val FEATURE_REPORT_NAMESPACE_ATTRIBUTES: String = "http://xmlpull.org/v1/doc/features.html#report-namespace-prefixes"

        /**
         * This feature determines whether the document declaration is processed. If set to false, the DOCDECL event type is
         * reported by nextToken() and ignored by next(). If this feature is activated, then the document declaration must
         * be processed by the parser.
         *
         *
         * **Please note:** If the document type declaration was ignored, entity references may cause
         * exceptions later in the parsing process. The default value of this feature is false. It cannot be changed during
         * parsing.
         *
         * @see .getFeature
         *
         * @see .setFeature
         */
        const val FEATURE_PROCESS_DOCDECL: String = "http://xmlpull.org/v1/doc/features.html#process-docdecl"

        /**
         * If this feature is activated, all validation errors as defined in the XML 1.0 specification are reported. This
         * implies that FEATURE_PROCESS_DOCDECL is true and both, the internal and external document type declaration will
         * be processed.
         *
         *
         * **Please Note:** This feature can not be changed during parsing. The default value is false.
         *
         * @see .getFeature
         *
         * @see .setFeature
         */
        const val FEATURE_VALIDATION: String = "http://xmlpull.org/v1/doc/features.html#validation"
    }
}