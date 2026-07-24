package io.kelly.source

import android.content.ContentResolver
import android.net.Uri
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




