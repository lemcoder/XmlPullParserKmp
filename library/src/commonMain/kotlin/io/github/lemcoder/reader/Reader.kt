package io.github.lemcoder.reader

import io.github.lemcoder.charBuffer.CharBuffer

expect open class Reader() : Readable {
    override fun read(cb: CharBuffer): Int
    override fun read(array: CharArray, offset: Int, length: Int): Int
}