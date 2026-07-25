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
    child: String?,
    displayName: String,
    mimeType: String,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveToGalleryAboveQ(roomUri, path, child, displayName, mimeType)
    } else {
        saveToGalleryBelowQ(path, child, displayName)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun InputStream.saveToGalleryAboveQ(
    roomUri: Uri,
    path: String,
    child: String?,
    displayName: String,
    mimeType: String,
) {
    val path = if (child.isNullOrEmpty()) {
        path
    } else {
        "${path}${File.separator}${child}"
    }
    val uri = roomUri.insert {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, path)
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
    child: String?,
    displayName: String,
) {
    val picturesDir = if (child.isNullOrEmpty()) {
        Environment.getExternalStoragePublicDirectory(path)
    } else {
        File(
            Environment.getExternalStoragePublicDirectory(path),
            child
        )
    }
    if (!picturesDir.exists() && !picturesDir.mkdirs()) {
        throw FileNotFoundException("Failed to create gallery directory: ${picturesDir.absolutePath}")
    }
    val destFile = picturesDir.getUniqueFile(displayName)
    writeTo(destFile)
    MediaScannerConnection.scanFile(
        KellyInternal.context,
        arrayOf(destFile.absolutePath),
        null,
        null
    )
    try {
        val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
            data = Uri.fromFile(destFile)
        }
        KellyInternal.context.sendBroadcast(mediaScanIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
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