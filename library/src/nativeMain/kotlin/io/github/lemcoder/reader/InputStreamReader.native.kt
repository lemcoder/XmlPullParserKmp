package io.github.lemcoder.reader

import io.github.lemcoder.inputStream.InputStream

actual class InputStreamReader actual constructor(
    inputStream: InputStream,
    inputEncoding: String
) : Reader()