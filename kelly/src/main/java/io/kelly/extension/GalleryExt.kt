package io.kelly.extension

import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import io.kelly.KellyInternal
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

fun InputStream.saveToGallery(
    roomUri: Uri,
    path: String,
    displayName: String,
    mimeType: String,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveToGalleryAboveQ(roomUri, path, displayName, mimeType)
    } else {
        saveToGalleryBelowQ(path, displayName, mimeType)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun InputStream.saveToGalleryAboveQ(
    roomUri: Uri,
    path: String,
    displayName: String,
    mimeType: String,
) {
    val uri = roomUri.insert {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, "${path}${File.separator}${appLabel}")
    } ?: throw IOException("Failed to insert media row to MediaStore")

    try {
        val outputStream = uri.openOutputStream()
            ?: throw IOException("Failed to open output stream for Uri: $uri")
        outputStream.use { output ->
            use { input ->
                input.copyTo(output)
            }
        }
        uri.updateToComplete()
    } catch (e: Exception) {
        uri.delete()
        throw e
    }
}

private fun InputStream.saveToGalleryBelowQ(
    path: String,
    displayName: String,
    mimeType: String,
) {
    val picturesDir = File(
        Environment.getExternalStoragePublicDirectory(path),
        appLabel
    )
    if (!picturesDir.exists() && !picturesDir.mkdirs()) {
        throw FileNotFoundException("Failed to create gallery directory: ${picturesDir.absolutePath}")
    }
    val destFile = picturesDir.getUniqueFile(displayName)
    writeTo(destFile)
    MediaScannerConnection.scanFile(
        KellyInternal.context,
        arrayOf(destFile.absolutePath),
        arrayOf(mimeType),
        null
    )
}

private fun File.getUniqueFile(displayName: String): File {
    var file = File(this, displayName)
    if (!file.exists()) return file
    val name = file.nameWithoutExtension
    val ext = file.extension
    val extString = if (ext.isEmpty()) "" else ".$ext"
    var count = 1
    while (file.exists()) {
        file = File(this, "$name ($count)$extString")
        count++
    }
    return file
}