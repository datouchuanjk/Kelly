package io.kelly.source

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.InputStream
import androidx.core.net.toUri
import java.net.URL

sealed interface Source<T> {
    val source: T
    val cacheKey: String
    val cacheFile: File
    val mimeType: String
    val extension: String
    val size: Long
    fun inputStream(): InputStream
    fun originInputStream(): InputStream
}

fun URL.asSource(): Source<*> = URLSource(this)

fun Uri.asSource(): Source<*> {
    return when (scheme) {
        ContentResolver.SCHEME_FILE -> {
            val filePath = path
            if (filePath != null) {
                FileSource(File(filePath))
            } else {
                ContentUriSource(this)
            }
        }

        "http", "https" -> {
            URLSource(URL(toString()))
        }

        else -> {
            ContentUriSource(this)
        }
    }
}

fun File.asSource(): Source<File> = FileSource(this)

fun String.asSource(flags: Int? = null): Source<*> {
    if (startsWith("http://", true) || startsWith("https://", true)) {
        return URL(this).asSource()
    }
    val actualFlags = flags ?: if (startsWith("data:", true)) {
        Base64.DEFAULT
    } else {
        Base64.NO_WRAP or Base64.URL_SAFE
    }
    return Base64Source(this, actualFlags)
}




