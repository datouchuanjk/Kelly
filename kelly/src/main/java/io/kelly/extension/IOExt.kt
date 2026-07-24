package io.kelly.extension

import android.graphics.Bitmap
import java.io.File
import java.io.InputStream
import kotlin.io.copyTo

fun InputStream.writeTo(targetFile: File) {
    targetFile.parentFile?.mkdirs()
    val tempFile = File(targetFile.parentFile, "${System.nanoTime()}.tmp")
    tryResult {
        use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile.moveTo(targetFile)
    }.onFinally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}

fun Bitmap.writeTo(
    targetFile: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.WEBP,
    quality: Int = 80,
    recycle: Boolean = false
) {
    targetFile.parentFile?.mkdirs()
    val tempFile = File(targetFile.parentFile, "${System.nanoTime()}.tmp")
    tryResult {
        tempFile.outputStream().use { output ->
            compress(format, quality, output)
        }
        tempFile.moveTo(targetFile)
    }.onFinally {
        if (recycle) {
            recycle()
        }
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
