package io.github.lemcoder.exceptions

import com.fleeksoft.io.InputStream


/**
 * The XmlReaderException is thrown by the XmlReader constructors if the charset encoding can not be determined
 * according to the XML 1.0 specification and RFC 3023.
 *
 *
 * The exception returns the unconsumed InputStream to allow the application to do an alternate processing with the
 * stream. Note that the original InputStream given to the XmlReader cannot be used as that one has been already read.
 *
 *
 *
 * @author Alejandro Abdelnur
 * @version revision 1.1 taken on 26/06/2007 from Rome (see
 * https://rome.dev.java.net/source/browse/rome/src/java/com/sun/syndication/io/XmlReaderException.java)
 */
abstract class XmlReaderException(
    val msg: String?,
    /**
     * Returns the MIME type in the content-type used to attempt determining the encoding.
     *
     *
     *
     * @return the MIME type in the content-type, null if there was not content-type or the encoding detection did not
     * involve HTTP.
     */
    val contentTypeMime: String?,

    /**
     * Returns the encoding in the content-type used to attempt determining the encoding.
     *
     *
     *
     * @return the encoding in the content-type, null if there was not content-type, no encoding in it or the encoding
     * detection did not involve HTTP.
     */
    val contentTypeEncoding: String?,

    /**
     * Returns the BOM encoding found in the InputStream.
     *
     *
     *
     * @return the BOM encoding, null if none.
     */
    val bomEncoding: String?,

    /**
     * Returns the encoding guess based on the first bytes of the InputStream.
     *
     *
     *
     * @return the encoding guess, null if it couldn't be guessed.
     */
    val xmlGuessEncoding: String?,

    /**
     * Returns the encoding found in the XML prolog of the InputStream.
     *
     *
     *
     * @return the encoding of the XML prolog, null if none.
     */
    val xmlEncoding: String?, inputStream: InputStream?
) : Exception(msg) {
    private val _is: InputStream? = inputStream

    /**
     * Creates an exception instance if the charset encoding could not be determined.
     *
     *
     * Instances of this exception are thrown by the XmlReader.
     *
     *
     *
     * @param msg message describing the reason for the exception.
     * @param bomEnc BOM encoding.
     * @param xmlGuessEnc XML guess encoding.
     * @param xmlEnc XML prolog encoding.
     * @param inputStream the unconsumed InputStream.
     */
    constructor(msg: String?, bomEnc: String?, xmlGuessEnc: String?, xmlEnc: String?, inputStream: InputStream?) : this(msg, null, null, bomEnc, xmlGuessEnc, xmlEnc, inputStream)

    val inputStream: InputStream?
        /**
         * Returns the unconsumed InputStream to allow the application to do an alternate encoding detection on the
         * InputStream.
         *
         *
         *
         * @return the unconsumed InputStream.
         */
        get() = _is
}
