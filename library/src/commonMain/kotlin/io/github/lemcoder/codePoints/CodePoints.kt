package io.github.lemcoder.codePoints

object CodePoints {
    private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000
    private const val MAX_CODE_POINT = 0x10FFFF

    private const val MIN_HIGH_SURROGATE = 0xD800
    private const val MIN_LOW_SURROGATE = 0xDC00

    private const val SURROGATE_DECODE_OFFSET =
        MIN_SUPPLEMENTARY_CODE_POINT - (MIN_HIGH_SURROGATE shl 10) - MIN_LOW_SURROGATE

    private const val HIGH_SURROGATE_ENCODE_OFFSET =
        (MIN_HIGH_SURROGATE - (MIN_SUPPLEMENTARY_CODE_POINT ushr 10))

    fun isValidCodePoint(codePoint: Int): Boolean {
        return codePoint in 0..MAX_CODE_POINT
    }

    fun isBmpCodePoint(codePoint: Int): Boolean {
        return codePoint ushr 16 == 0
    }

    fun isSupplementaryCodePoint(codePoint: Int): Boolean {
        return codePoint in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT
    }

    fun charCount(codePoint: Int): Int {
        return if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) 1 else 2
    }

    fun isSurrogatePair(highSurrogate: Char, lowSurrogate: Char): Boolean {
        return highSurrogate.isHighSurrogate() && lowSurrogate.isLowSurrogate()
    }

    fun highSurrogate(codePoint: Int): Char {
        return ((codePoint ushr 10) + HIGH_SURROGATE_ENCODE_OFFSET).toChar()
    }

    fun lowSurrogate(codePoint: Int): Char {
        return ((codePoint and 0x3FF) + MIN_LOW_SURROGATE).toChar()
    }

    fun toCodePoint(highSurrogate: Char, lowSurrogate: Char): Int {
        return (highSurrogate.code shl 10) + lowSurrogate.code + SURROGATE_DECODE_OFFSET
    }

    fun toChars(codePoint: Int): CharArray {
        return if (isBmpCodePoint(codePoint)) {
            charArrayOf(codePoint.toChar())
        } else {
            charArrayOf(highSurrogate(codePoint), lowSurrogate(codePoint))
        }
    }

    fun toChars(codePoint: Int, destination: CharArray, offset: Int): Int {
        if (isBmpCodePoint(codePoint)) {
            destination.setSafe(offset, codePoint.toChar())
            return 1
        } else {
            // When writing the low surrogate succeeds but writing the high surrogate fails (offset = -1), the
            // destination will be modified even though the method throws. This feels wrong, but matches the behavior
            // of the Java stdlib implementation.
            destination.setSafe(offset + 1, lowSurrogate(codePoint))
            destination.setSafe(offset, highSurrogate(codePoint))
            return 2
        }
    }

    private fun CharArray.setSafe(index: Int, value: Char) {
        if (index !in this.indices) {
            throw IndexOutOfBoundsException("Size: $size, offset: $index")
        }

        this[index] = value
    }
}