package io.kelly.source

import io.kelly.extension.mimeType
import java.io.File

internal class FileSource(
    override val source: File
) : Source<File> {

    override val cacheKey = source.canonicalPath.orEmpty()

    override val cacheFile = source

    override val mimeType by lazy {
        source.mimeType
    }

    override val extension by lazy {
        source.extension
    }

    override val size by lazy {
        source.length()
    }

    override fun inputStream() = source.inputStream()

    override fun originInputStream() = source.inputStream()
}

