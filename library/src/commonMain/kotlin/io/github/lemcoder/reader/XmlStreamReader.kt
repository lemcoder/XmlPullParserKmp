package io.github.lemcoder.reader

import io.github.lemcoder.inputStream.InputStream

expect class XmlStreamReader(
    inputStream: InputStream,
    lenient: Boolean
) : Reader