package io.kelly.source

import android.webkit.MimeTypeMap
import io.kelly.KellyInternal
import io.kelly.extension.head
import io.kelly.extension.hexDigestOrNull
import io.kelly.extension.inputStream
import java.io.File
import java.net.URL
import kotlin.io.inputStream

internal class URLSource(
    override val source: URL,
    internal val cacheDir: File = File(KellyInternal.context.cacheDir, "source/url")
) : Source<URL> {

    private val url = source.toString()

    override val cacheKey by lazy {
        url.hexDigestOrNull() ?: url.hashCode().toString()
    }

    override val cacheFile by lazy {
        File(cacheDir, cacheKey)
    }

    private class NetworkMetadata(val size: Long, val mimeType: String)

    private val networkMetadata by lazy {
        source.head { conn ->
            val size = conn.contentLengthLong
            val contentType = conn.contentType?.substringBefore(';').orEmpty()
            NetworkMetadata(size, contentType)
        }
    }

    override val mimeType by lazy {
        networkMetadata.mimeType.takeIf { it.isNotEmpty() }
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.lowercase())
                ?.takeIf { it.isNotEmpty() }
            ?: "application/octet-stream"
    }

    override val extension by lazy {
        MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(networkMetadata.mimeType)
            ?.takeIf { it.isNotEmpty() }
            ?: MimeTypeMap.getFileExtensionFromUrl(url)
                .orEmpty()
    }

    override val size by lazy {
        networkMetadata.size
    }

    override fun inputStream() = if (isCacheFileReady) {
        cacheFile.inputStream()
    } else {
        originInputStream()
    }

    override fun originInputStream() = source.inputStream()
}