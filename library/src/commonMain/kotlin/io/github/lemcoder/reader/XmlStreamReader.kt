package io.github.lemcoder.reader

import io.github.lemcoder.charBuffer.CharBuffer
import io.github.lemcoder.inputStream.InputStream

expect class XmlStreamReader(
    inputStream: InputStream,
    lenient: Boolean
) : Reader {
    override fun read(cb: CharBuffer): Int
    override fun read(array: CharArray, offset: Int, length: Int): Int
}