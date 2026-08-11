package io.kelly.source

import android.util.Base64
import android.webkit.MimeTypeMap
import io.kelly.KellyInternal
import io.kelly.extension.hexDigestOrNull
import io.kelly.extension.tryResult
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

internal class Base64Source(
    override val source: String,
    private val flags: Int = Base64.DEFAULT,
    internal val cacheDir: File = File(KellyInternal.context.cacheDir, "source/base64")
) : Source<String> {

    private val dataPrefix = "data:"
    private val base64Prefix = ";base64,"

    private val metadata by lazy {
        val commaIndex = source.indexOf(',')
        if (source.startsWith(dataPrefix) && source.contains(base64Prefix) && commaIndex != -1) {
            val mime = source.substring(dataPrefix.length, source.indexOf(';'))
            val data = source.substring(commaIndex + 1)
            Base64Metadata(mime, data)
        } else {
            Base64Metadata("application/octet-stream", source)
        }
    }

    private class Base64Metadata(val mimeType: String, val data: String)

    override val cacheKey by lazy {
        source.hexDigestOrNull() ?: source.hashCode().toString()
    }

    override val cacheFile by lazy {
        File(cacheDir, cacheKey)
    }

    override val mimeType: String by lazy {
        metadata.mimeType
    }

    override val extension by lazy {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty()
    }

    override val size by lazy {
        tryResult {
            Base64.decode(metadata.data, flags).size.toLong()
        }.getOrDefault((metadata.data.length * 3 / 4).toLong())
    }

    override fun inputStream(): InputStream {
        return if (isCacheFileReady) {
            cacheFile.inputStream()
        } else {
            originInputStream()
        }
    }

    override fun originInputStream(): InputStream {
        val bytes = Base64.decode(metadata.data, flags)
        return ByteArrayInputStream(bytes)
    }
}
