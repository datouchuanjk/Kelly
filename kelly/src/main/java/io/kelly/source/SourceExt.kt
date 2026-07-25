package io.kelly.source

import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import io.kelly.extension.appLabel
import io.kelly.extension.writeTo
import io.kelly.extension.saveToGallery
import io.kelly.extension.scaleTo
import java.io.File

fun <T> Source<T>.isImage(): Boolean {
    return mimeType.lowercase().startsWith("image/")
}

fun <T> Source<T>.isVideo(): Boolean {
    return mimeType.lowercase().startsWith("video/")
}

fun <T> Source<T>.isAudio(): Boolean {
    return mimeType.lowercase().startsWith("audio/")
}

fun <T> Source<T>.isAnimatedImage(): Boolean {
    val lowerMimeType = mimeType.lowercase()
    return isImage() && when (lowerMimeType) {
        "image/gif" -> true
        "image/apng" -> true
        "image/webp" -> inputStream().use { inputStream ->
            val header = ByteArray(21)
            if (inputStream.read(header) < 21) return false
            if (header.decodeToString(0, 4) != "RIFF") return false
            if (header.decodeToString(8, 12) != "WEBP") return false
            if (header.decodeToString(12, 16) != "VP8X") return false
            val flags = header[20].toInt() and 0xFF
            (flags and 0x02) != 0
        }
        else -> false
    }
}

fun <T> Source<T>.fetch(): File {
    if (isCacheFileReady) return cacheFile
    cacheFile.withLock {
        if (isCacheFileReady) return@withLock cacheFile
        originInputStream().writeTo(cacheFile)
    }
    return cacheFile
}

fun <T> Source<T>.saveToGallery() {
    val isVideo = isVideo()
    val isAudio = isAudio()
    inputStream().saveToGallery(
        roomUri = when {
            isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            isAudio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        },
        path = when {
            isVideo -> Environment.DIRECTORY_MOVIES
            isAudio -> Environment.DIRECTORY_MUSIC
            else -> Environment.DIRECTORY_PICTURES
        },
        child = appLabel,
        displayName = "${System.nanoTime()}.${extension}",
        mimeType = mimeType
    )
}

fun <T> Source<T>.scaleTo(
    reqWidth: Int,
    reqHeight: Int,
    targetFile: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.WEBP,
    quality: Int = 80,
) {
    val file = fetch()
    targetFile.withLock {
        file.scaleTo(reqWidth, reqHeight, targetFile, format, quality)
    }
}

val <T> Source<T>.isCacheFileReady
    get() = cacheFile.exists()
            && cacheFile.isFile
            && cacheFile.length() > 0


val <T> Source<T>.cacheFilePathWhenReady
    get() = if (isCacheFileReady) {
        cacheFile.absolutePath
    } else {
        null
    }

