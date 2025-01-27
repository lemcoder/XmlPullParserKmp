package io.github.lemcoder.reader

import com.fleeksoft.io.InputStream

class XmlStreamReader : XmlReader {
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
     * @param inputStream InputStream to create a Reader from.
     * @param lenient indicates if the charset encoding detection should be relaxed.
     * @throws IOException thrown if there is a problem reading the stream.
     * @throws XmlStreamReaderException thrown if the charset encoding could not be determined according to the specs.
     */
    constructor(inputStream: InputStream, lenient: Boolean) : super(inputStream, lenient)
}