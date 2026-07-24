package io.kelly.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import io.kelly.KellyInternal
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import androidx.core.graphics.scale

fun File.toContentUri(): Uri {
    val authority = "${KellyInternal.context.packageName}.kelly.fileProvider"
    return FileProvider.getUriForFile(KellyInternal.context, authority, this)
}

val File.mimeType: String
    get() = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension.lowercase())
        ?.takeIf {
            it.isNotEmpty()
        } ?: "application/octet-stream"

fun File.moveTo(targetFile: File) {
    if (targetFile.exists()) {
        targetFile.delete()
    }
    if (!renameTo(targetFile)) {
        tryResult {
            Files.move(
                toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.recoverCatching {
            Files.move(
                toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }.getOrThrow()
    }
}

fun File.scaleTo(
    reqWidth: Int,
    reqHeight: Int,
    targetFile: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.WEBP,
    quality: Int = 80,
) {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        scaleToBitmapAboveP(reqWidth, reqHeight)
    } else {
        scaleToBitmapBelowP(reqWidth, reqHeight)
    }
    bitmap.writeTo(targetFile, format, quality, true)
}

@RequiresApi(Build.VERSION_CODES.P)
private fun File.scaleToBitmapAboveP(
    reqWidth: Int,
    reqHeight: Int,
): Bitmap {
    val source = ImageDecoder.createSource(this)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val w = info.size.width
        val h = info.size.height
        if (w <= reqWidth && h <= reqHeight) {
            decoder.isMutableRequired = true
            return@decodeBitmap
        }
        val ratio = w.toFloat() / h.toFloat()
        val (targetW, targetH) = if (w > h) {
            reqWidth to (reqWidth / ratio).toInt()
        } else {
            (reqHeight * ratio).toInt() to reqHeight
        }
        decoder.setTargetSize(targetW, targetH)
        decoder.isMutableRequired = true
    }
}

private fun File.scaleToBitmapBelowP(
    reqWidth: Int,
    reqHeight: Int,
): Bitmap {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(absolutePath, options)
    val w = options.outWidth
    val h = options.outHeight
    val ratio = w.toFloat() / h.toFloat()
    val (targetW, targetH) = if (w > h) {
        reqWidth to (reqWidth / ratio).toInt()
    } else {
        (reqHeight * ratio).toInt() to reqHeight
    }
    var inSampleSize = 1
    if (h > targetH || w > targetW) {
        val halfHeight: Int = h / 2
        val halfWidth: Int = w / 2
        while (halfHeight / inSampleSize >= targetH && halfWidth / inSampleSize >= targetW) {
            inSampleSize *= 2
        }
    }
    options.inJustDecodeBounds = false
    options.inSampleSize = inSampleSize
    options.inMutable = true

    val approximateBitmap = BitmapFactory.decodeFile(absolutePath, options)
    if (approximateBitmap.width == targetW && approximateBitmap.height == targetH) {
        return approximateBitmap
    }
    val exactBitmap = approximateBitmap.scale(targetW, targetH)
    if (exactBitmap != approximateBitmap) {
        approximateBitmap.recycle()
    }
    return exactBitmap
}