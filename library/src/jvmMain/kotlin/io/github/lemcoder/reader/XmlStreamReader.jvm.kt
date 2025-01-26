package io.github.lemcoder.reader

import io.github.lemcoder.inputStream.InputStream
import org.codehaus.plexus.util.xml.XmlReader
import java.io.File
import java.net.URL
import java.net.URLConnection
import java.nio.file.Path

actual class XmlStreamReader actual constructor(
    private val inputStream: InputStream,
    private val lenient: Boolean
) : Reader() {

    private val reader = JXmlStreamReader(inputStream, lenient)

    override fun close() {
        reader.close()
    }

    override actual fun read(array: CharArray, offset: Int, length: Int): Int {
        return reader.read(array, offset, length)
    }
}


/**
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
 * <P>
 * By default the charset encoding detection is lenient, the constructor with the lenient flag can be used for an script
 * (following HTTP MIME and XML specifications). All this is nicely explained by Mark Pilgrim in his blog,
 * [ Determining the character encoding of a
 * feed](http://diveintomark.org/archives/2004/02/13/xml-media-types).
</P> *
 *
 *
 * @author Alejandro Abdelnur
 * @version revision 1.17 taken on 26/06/2007 from Rome (see
 * https://rome.dev.java.net/source/browse/rome/src/java/com/sun/syndication/io/XmlReader.java)
 * @since 1.4.4
 */
class JXmlStreamReader : XmlReader {
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
    constructor(path: Path?) : super(path)

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
    constructor(file: File) : this(file.toPath())

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
    constructor(`is`: java.io.InputStream?) : super(`is`)

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
    constructor(`is`: java.io.InputStream?, lenient: Boolean) : super(`is`, lenient)

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
    constructor(url: URL?) : super(url)

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
    constructor(conn: URLConnection?) : super(conn)

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
    constructor(`is`: java.io.InputStream?, httpContentType: String?) : super(`is`, httpContentType)

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
    constructor(`is`: java.io.InputStream?, httpContentType: String?, lenient: Boolean, defaultEncoding: String?) : super(`is`, httpContentType, lenient, defaultEncoding)

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
    constructor(`is`: java.io.InputStream?, httpContentType: String?, lenient: Boolean) : super(`is`, httpContentType, lenient)
}