package io.github.lemcoder.reader

import io.github.lemcoder.charBuffer.CharBuffer
import java.io.CharArrayReader
import java.io.Reader as JReader
import java.nio.CharBuffer as JCharBuffer

actual open class Reader : Readable, JReader() {
    private val reader = object : JReader() {
        private var _reader = CharArrayReader(CharArray(0))

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            _reader = CharArrayReader(cbuf)
            return _reader.read(cbuf, off, len)
        }

        override fun close() {
            _reader.close()
        }
    }

    actual override fun read(cb: CharBuffer): Int {
        cb as JCharBuffer
        return reader.read(cb)
    }

    override fun close() {
        reader.close()
    }

    actual override fun read(array: CharArray, offset: Int, length: Int): Int {
        return reader.read(array, offset, length)
    }
}