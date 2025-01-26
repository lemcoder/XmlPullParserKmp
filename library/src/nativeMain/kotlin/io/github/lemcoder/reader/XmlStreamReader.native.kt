package io.github.lemcoder.reader

import io.github.lemcoder.charBuffer.CharBuffer
import io.github.lemcoder.inputStream.InputStream

actual class XmlStreamReader actual constructor(inputStream: InputStream, lenient: Boolean) : Reader() {
    actual override fun read(cb: CharBuffer): Int {
        TODO("Not yet implemented")
    }

    actual override fun read(array: CharArray, offset: Int, length: Int): Int {
        TODO("Not yet implemented")
    }
}