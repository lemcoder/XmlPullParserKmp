package io.github.lemcoder.utils

import io.github.lemcoder.XmlPullParser

fun <T> arraycopy(src: Array<T>, srcPos: Int, dest: Array<T>, destPos: Int, length: Int) {
    src.copyInto(dest, destinationOffset = destPos, startIndex = srcPos, endIndex = srcPos + length)
}


fun XmlPullParser.Companion.getTypeOf(type: Int): String {
    return XmlPullParser.TYPES[type]
}