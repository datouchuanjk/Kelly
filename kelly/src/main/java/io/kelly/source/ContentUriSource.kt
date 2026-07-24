package io.kelly.source

import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import io.kelly.KellyInternal
import io.kelly.extension.hexDigestOrNull
import io.kelly.extension.openInputStream
import io.kelly.extension.querySelf
import java.io.File

internal class ContentUriSource(
    override val source: Uri,
    internal val cacheDir: File = File(KellyInternal.context.cacheDir, "source/uri")
) : Source<Uri> {

    private class UriMetadata(
        val dateModified: Long?,
        val mimeType: String?,
        val size: Long?,
        val displayName: String?
    )

    private val uriMetadata by lazy {
        val result = source.querySelf {
            addProjection(MediaStore.MediaColumns.DATE_MODIFIED)
            addProjection(MediaStore.MediaColumns.MIME_TYPE)
            addProjection(MediaStore.MediaColumns.SIZE)
            addProjection(MediaStore.MediaColumns.DISPLAY_NAME)
        }
        UriMetadata(
            dateModified = result?.getOrNull(MediaStore.MediaColumns.DATE_MODIFIED),
            mimeType = result?.getOrNull(MediaStore.MediaColumns.MIME_TYPE),
            size = result?.getOrNull(MediaStore.MediaColumns.SIZE),
            displayName = result?.getOrNull(MediaStore.MediaColumns.DISPLAY_NAME)
        )
    }

    override val cacheKey by lazy {
        val date = uriMetadata.dateModified
        if (date == null) {
            source.openInputStream()?.use { it.hexDigestOrNull() }
        } else {
            "${source}_$date".hexDigestOrNull()
        } ?: source.hashCode().toString()
    }

    override val cacheFile by lazy {
        File(cacheDir, cacheKey)
    }

    override val mimeType by lazy {
        uriMetadata.mimeType
            ?: KellyInternal.context.contentResolver.getType(source)
            ?: "application/octet-stream"
    }

    override val extension by lazy {
        val extFromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (extFromMime.isNullOrEmpty()) {
            uriMetadata.displayName?.substringAfterLast('.', "").orEmpty()
        } else {
            extFromMime
        }
    }

    override val size by lazy {
        uriMetadata.size ?: 0L
    }

    override fun inputStream() = if (isCacheFileReady) {
        cacheFile.inputStream()
    } else {
        originInputStream()
    }

    override fun originInputStream() = requireNotNull(source.openInputStream())

}