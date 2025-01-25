package io.github.lemcoder.reader

import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlin.math.min

/*
* Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This code is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License version 2 only, as
* published by the Free Software Foundation.  Oracle designates this
* particular file as subject to the "Classpath" exception as provided
* by Oracle in the LICENSE file that accompanied this code.
*
* This code is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
* FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
* version 2 for more details (a copy is included in the LICENSE file that
* accompanied this code).
*
* You should have received a copy of the GNU General Public License version
* 2 along with this work; if not, write to the Free Software Foundation,
* Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
*
* Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
* or visit www.oracle.com if you need additional information or have any
* questions.
*/

/**
 * Abstract class for reading character streams.  The only methods that a
 * subclass must implement are read(char[], int, int) and close().  Most
 * subclasses, however, will override some of the methods defined here in order
 * to provide higher efficiency, additional functionality, or both.
 *
 *
 * @see BufferedReader
 *
 * @see LineNumberReader
 *
 * @see CharArrayReader
 *
 * @see InputStreamReader
 *
 * @see FileReader
 *
 * @see FilterReader
 *
 * @see PushbackReader
 *
 * @see PipedReader
 *
 * @see StringReader
 *
 * @see Writer
 *
 *
 * @author      Mark Reinhold
 * @since       1.1
 */
abstract class Reader : Readable, AutoCloseable {
    /**
     * The object used to synchronize operations on this stream.  For
     * efficiency, a character-stream object may use an object other than
     * itself to protect critical sections.  A subclass should therefore use
     * the object in this field rather than `this` or a synchronized
     * method.
     */
    protected var lock: Any? = null

    /**
     * Creates a new character-stream reader whose critical sections will
     * synchronize on the reader itself.
     */
    protected constructor() {
        this.lock = this
    }

    /**
     * Creates a new character-stream reader whose critical sections will
     * synchronize on the given object.
     *
     * @param lock  The Object to synchronize on.
     */
    protected constructor(lock: Any) {
        if (lock == null) {
            throw java.lang.NullPointerException()
        }
        this.lock = lock
    }

    /**
     * For use by BufferedReader to create a character-stream reader that uses an
     * internal lock when BufferedReader is not extended and the given reader is
     * trusted, otherwise critical sections will synchronize on the given reader.
     */
    internal constructor(`in`: java.io.Reader) {
        val clazz: java.lang.Class<*> = `in`.javaClass
        if (javaClass == BufferedReader::class.java &&
            (clazz == java.io.InputStreamReader::class.java || clazz == java.io.FileReader::class.java)
        ) {
            this.lock = InternalLock.newLockOr(`in`)
        } else {
            this.lock = `in`
        }
    }

    /**
     * Attempts to read characters into the specified character buffer.
     * The buffer is used as a repository of characters as-is: the only
     * changes made are the results of a put operation. No flipping or
     * rewinding of the buffer is performed.
     *
     * @param target the buffer to read characters into
     * @return The number of characters added to the buffer, or
     * -1 if this source of characters is at its end
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if target is null
     * @throws java.nio.ReadOnlyBufferException if target is a read only buffer
     * @since 1.5
     */
    @Throws(java.io.IOException::class)
    override fun read(target: java.nio.CharBuffer): Int {
        if (target.isReadOnly()) throw ReadOnlyBufferException()

        val nread: Int
        if (target.hasArray()) {
            val cbuf: CharArray = target.array()
            val pos: Int = target.position()
            val rem = max((target.limit() - pos).toDouble(), 0.0).toInt()
            val off: Int = target.arrayOffset() + pos
            nread = this.read(cbuf, off, rem)
            if (nread > 0) target.position(pos + nread)
        } else {
            val len: Int = target.remaining()
            val cbuf = CharArray(len)
            nread = read(cbuf, 0, len)
            if (nread > 0) target.put(cbuf, 0, nread)
        }
        return nread
    }

    /**
     * Reads a single character.  This method will block until a character is
     * available, an I/O error occurs, or the end of the stream is reached.
     *
     *
     *  Subclasses that intend to support efficient single-character input
     * should override this method.
     *
     * @return     The character read, as an integer in the range 0 to 65535
     * (`0x00-0xffff`), or -1 if the end of the stream has
     * been reached
     *
     * @throws     IOException  If an I/O error occurs
     */
    @Throws(java.io.IOException::class)
    open fun read(): Int {
        val cb = CharArray(1)
        return if (read(cb, 0, 1) == -1) -1
        else cb[0].code
    }

    /**
     * Reads characters into an array.  This method will block until some input
     * is available, an I/O error occurs, or the end of the stream is reached.
     *
     *
     *  If the length of `cbuf` is zero, then no characters are read
     * and `0` is returned; otherwise, there is an attempt to read at
     * least one character.  If no character is available because the stream is
     * at its end, the value `-1` is returned; otherwise, at least one
     * character is read and stored into `cbuf`.
     *
     * @param       cbuf  Destination buffer
     *
     * @return      The number of characters read, or -1
     * if the end of the stream
     * has been reached
     *
     * @throws      IOException  If an I/O error occurs
     */
    @Throws(java.io.IOException::class)
    open fun read(cbuf: CharArray): Int {
        return read(cbuf, 0, cbuf.size)
    }

    /**
     * Reads characters into a portion of an array.  This method will block
     * until some input is available, an I/O error occurs, or the end of the
     * stream is reached.
     *
     *
     *  If `len` is zero, then no characters are read and `0` is
     * returned; otherwise, there is an attempt to read at least one character.
     * If no character is available because the stream is at its end, the value
     * `-1` is returned; otherwise, at least one character is read and
     * stored into `cbuf`.
     *
     * @param      cbuf  Destination buffer
     * @param      off   Offset at which to start storing characters
     * @param      len   Maximum number of characters to read
     *
     * @return     The number of characters read, or -1 if the end of the
     * stream has been reached
     *
     * @throws     IndexOutOfBoundsException
     * If `off` is negative, or `len` is negative,
     * or `len` is greater than `cbuf.length - off`
     * @throws     IOException  If an I/O error occurs
     */
    @Throws(java.io.IOException::class)
    abstract fun read(cbuf: CharArray?, off: Int, len: Int): Int

    /** Skip buffer, null until allocated  */
    private var skipBuffer: CharArray? = null

    /**
     * Skips characters.  This method will block until some characters are
     * available, an I/O error occurs, or the end of the stream is reached.
     * If the stream is already at its end before this method is invoked,
     * then no characters are skipped and zero is returned.
     *
     * @param  n  The number of characters to skip
     *
     * @return    The number of characters actually skipped
     *
     * @throws     IllegalArgumentException  If `n` is negative.
     * @throws     IOException  If an I/O error occurs
     */
    @Throws(java.io.IOException::class)
    open fun skip(n: Long): Long {
        require(n >= 0L) { "skip value is negative" }
        val lock = this.lock
        if (lock is InternalLock) {
            lock.lock()
            try {
                return implSkip(n)
            } finally {
                lock.unlock()
            }
        } else {
            synchronized(lock) {
                return implSkip(n)
            }
        }
    }

    @Throws(java.io.IOException::class)
    private fun implSkip(n: Long): Long {
        val nn = min(n.toDouble(), java.io.Reader.Companion.maxSkipBufferSize.toDouble()).toInt()
        if ((skipBuffer == null) || (skipBuffer!!.size < nn)) skipBuffer = CharArray(nn)
        var r = n
        while (r > 0) {
            val nc = read(skipBuffer, 0, min(r.toDouble(), nn.toDouble()).toInt())
            if (nc == -1) break
            r -= nc.toLong()
        }
        return n - r
    }

    /**
     * Tells whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input,
     * false otherwise.  Note that returning false does not guarantee that the
     * next read will block.
     *
     * @throws     IOException  If an I/O error occurs
     */
    @Throws(java.io.IOException::class)
    open fun ready(): Boolean {
        return false
    }

    /**
     * Tells whether this stream supports the mark() operation. The default
     * implementation always returns false. Subclasses should override this
     * method.
     *
     * @return true if and only if this stream supports the mark operation.
     */
    open fun markSupported(): Boolean {
        return false
    }

    /**
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point.  Not all
     * character-input streams support the mark() operation.
     *
     * @param  readAheadLimit  Limit on the number of characters that may be
     * read while still preserving the mark.  After
     * reading this many characters, attempting to
     * reset the stream may fail.
     *
     * @throws     IOException  If the stream does not support mark(),
     * or if some other I/O error occurs
     */
    @Throws(java.io.IOException::class)
    open fun mark(readAheadLimit: Int) {
        throw java.io.IOException("mark() not supported")
    }

    /**
     * Resets the stream.  If the stream has been marked, then attempt to
     * reposition it at the mark.  If the stream has not been marked, then
     * attempt to reset it in some way appropriate to the particular stream,
     * for example by repositioning it to its starting point.  Not all
     * character-input streams support the reset() operation, and some support
     * reset() without supporting mark().
     *
     * @throws     IOException  If the stream has not been marked,
     * or if the mark has been invalidated,
     * or if the stream does not support reset(),
     * or if some other I/O error occurs
     */
    @Throws(java.io.IOException::class)
    open fun reset() {
        throw java.io.IOException("reset() not supported")
    }

    /**
     * Closes the stream and releases any system resources associated with
     * it.  Once the stream has been closed, further read(), ready(),
     * mark(), reset(), or skip() invocations will throw an IOException.
     * Closing a previously closed stream has no effect.
     *
     * @throws     IOException  If an I/O error occurs
     */
    @Throws(java.io.IOException::class)
    abstract override fun close()

    /**
     * Reads all characters from this reader and writes the characters to the
     * given writer in the order that they are read. On return, this reader
     * will be at end of the stream. This method does not close either reader
     * or writer.
     *
     *
     * This method may block indefinitely reading from the reader, or
     * writing to the writer. The behavior for the case where the reader
     * and/or writer is *asynchronously closed*, or the thread
     * interrupted during the transfer, is highly reader and writer
     * specific, and therefore not specified.
     *
     *
     * If the total number of characters transferred is greater than [ ][Long.MAX_VALUE], then `Long.MAX_VALUE` will be returned.
     *
     *
     * If an I/O error occurs reading from the reader or writing to the
     * writer, then it may do so after some characters have been read or
     * written. Consequently the reader may not be at end of the stream and
     * one, or both, streams may be in an inconsistent state. It is strongly
     * recommended that both streams be promptly closed if an I/O error occurs.
     *
     * @param  out the writer, non-null
     * @return the number of characters transferred
     * @throws IOException if an I/O error occurs when reading or writing
     * @throws NullPointerException if `out` is `null`
     *
     * @since 10
     */
    @Throws(java.io.IOException::class)
    fun transferTo(out: java.io.Writer): Long {
        java.util.Objects.requireNonNull<java.io.Writer>(out, "out")
        var transferred: Long = 0
        val buffer = CharArray(java.io.Reader.Companion.TRANSFER_BUFFER_SIZE)
        var nRead: Int
        while ((read(buffer, 0, java.io.Reader.Companion.TRANSFER_BUFFER_SIZE).also { nRead = it }) >= 0) {
            out.write(buffer, 0, nRead)
            if (transferred < Long.MAX_VALUE) {
                transferred = try {
                    java.lang.Math.addExact(transferred, nRead.toLong())
                } catch (ignore: java.lang.ArithmeticException) {
                    Long.MAX_VALUE
                }
            }
        }
        return transferred
    }

    companion object {
        private const val TRANSFER_BUFFER_SIZE = 8192

        /**
         * Returns a new `Reader` that reads no characters. The returned
         * stream is initially open.  The stream is closed by calling the
         * `close()` method.  Subsequent calls to `close()` have no
         * effect.
         *
         *
         *  While the stream is open, the `read()`, `read(char[])`,
         * `read(char[], int, int)`, `read(CharBuffer)`, `ready()`, `skip(long)`, and `transferTo()` methods all
         * behave as if end of stream has been reached. After the stream has been
         * closed, these methods all throw `IOException`.
         *
         *
         *  The `markSupported()` method returns `false`.  The
         * `mark()` and `reset()` methods throw an `IOException`.
         *
         *
         *  The [object][.lock] used to synchronize operations on the
         * returned `Reader` is not specified.
         *
         * @return a `Reader` which reads no characters
         *
         * @since 11
         */
        fun nullReader(): java.io.Reader {
            return object : java.io.Reader() {
                @Volatile
                private var closed = false

                @Throws(java.io.IOException::class)
                fun ensureOpen() {
                    if (closed) {
                        throw java.io.IOException("Stream closed")
                    }
                }

                @Throws(java.io.IOException::class)
                override fun read(): Int {
                    ensureOpen()
                    return -1
                }

                @Throws(java.io.IOException::class)
                override fun read(cbuf: CharArray, off: Int, len: Int): Int {
                    java.util.Objects.checkFromIndexSize(off, len, cbuf.size)
                    ensureOpen()
                    if (len == 0) {
                        return 0
                    }
                    return -1
                }

                @Throws(java.io.IOException::class)
                override fun read(target: java.nio.CharBuffer): Int {
                    java.util.Objects.requireNonNull<java.nio.CharBuffer>(target)
                    ensureOpen()
                    if (target.hasRemaining()) {
                        return -1
                    }
                    return 0
                }

                @Throws(java.io.IOException::class)
                override fun ready(): Boolean {
                    ensureOpen()
                    return false
                }

                @Throws(java.io.IOException::class)
                override fun skip(n: Long): Long {
                    ensureOpen()
                    return 0L
                }

                @Throws(java.io.IOException::class)
                override fun transferTo(out: java.io.Writer?): Long {
                    java.util.Objects.requireNonNull<java.io.Writer>(out)
                    ensureOpen()
                    return 0L
                }

                override fun close() {
                    closed = true
                }
            }
        }

        /** Maximum skip-buffer size  */
        private const val maxSkipBufferSize = 8192
    }
}