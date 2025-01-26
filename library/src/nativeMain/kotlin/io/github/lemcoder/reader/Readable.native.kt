package io.github.lemcoder.reader

import io.github.lemcoder.charBuffer.CharBuffer
import io.github.lemcoder.exceptions.IOException

/**
 * A `Readable` is a source of characters. Characters from
 * a `Readable` are made available to callers of the read
 * method via a [CharBuffer][CharBuffer].
 *
 * @since 1.5
 */
actual interface Readable {
    /**
     * Attempts to read characters into the specified character buffer.
     * The buffer is used as a repository of characters as-is: the only
     * changes made are the results of a put operation. No flipping or
     * rewinding of the buffer is performed.
     *
     * @param cb the buffer to read characters into
     * @return The number of `char` values added to the buffer,
     * or -1 if this source of characters is at its end
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if cb is null
    // * @throws ReadOnlyBufferException if cb is a read only buffer // TODO
     */
    @Throws(IOException::class)
    actual fun read(cb: CharBuffer): Int

    @Throws(IOException::class)
    actual fun read(array: CharArray, offset: Int, length: Int): Int
}