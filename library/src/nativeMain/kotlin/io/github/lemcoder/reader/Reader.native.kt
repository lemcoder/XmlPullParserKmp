package io.github.lemcoder.reader

import io.github.lemcoder.charBuffer.CharBuffer

actual open class Reader : Readable {
    actual override fun read(cb: CharBuffer): Int {
        TODO("Not yet implemented")
    }

    actual override fun read(array: CharArray, offset: Int, length: Int): Int {
        TODO("Not yet implemented")
    }
}