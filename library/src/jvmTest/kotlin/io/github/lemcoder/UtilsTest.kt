package io.github.lemcoder

import io.github.lemcoder.utils.arraycopy
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class UtilsTest {

    @Test
    fun `test arraycopy with valid input`() {
        val src = arrayOf(1, 2, 3, 4, 5)
        val dest = arrayOf(0, 0, 0, 0, 0)
        arraycopy(src, 1, dest, 2, 3)
        assertArrayEquals(arrayOf(0, 0, 2, 3, 4), dest)
    }

    @Test
    fun `test arraycopy with overlapping ranges`() {
        val src = arrayOf(1, 2, 3, 4, 5)
        val dest = arrayOf(1, 2, 3, 4, 5)
        arraycopy(src, 1, dest, 2, 3)
        assertArrayEquals(arrayOf(1, 2, 2, 3, 4), dest)
    }

    @Test
    fun `test arraycopy with zero length`() {
        val src = arrayOf(1, 2, 3, 4, 5)
        val dest = arrayOf(0, 0, 0, 0, 0)
        arraycopy(src, 1, dest, 2, 0)
        assertArrayEquals(arrayOf(0, 0, 0, 0, 0), dest)
    }

    @Test
    fun `test arraycopy with full copy`() {
        val src = arrayOf(1, 2, 3, 4, 5)
        val dest = arrayOf(0, 0, 0, 0, 0)
        arraycopy(src, 0, dest, 0, 5)
        assertArrayEquals(arrayOf(1, 2, 3, 4, 5), dest)
    }

    @Test
    fun `test arraycopy with different types`() {
        val src = arrayOf("a", "b", "c", "d", "e")
        val dest = arrayOf("", "", "", "", "")
        arraycopy(src, 1, dest, 2, 3)
        assertArrayEquals(arrayOf("", "", "b", "c", "d"), dest)
    }
}