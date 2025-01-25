package io.github.lemcoder.utils

fun <T>arraycopy(src: Array<T>, srcPos: Int, dest: Array<T>, destPos: Int, length: Int) {
    src.copyInto(dest, destinationOffset = destPos, startIndex = srcPos, endIndex = srcPos + length)
}