package io.github.lemcoder.reader

import io.github.lemcoder.inputStream.InputStream

expect class InputStreamReader constructor(
    inputStream: InputStream,
    inputEncoding: String
) : Reader