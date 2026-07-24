package io.kelly.extension

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest

fun String.hexDigestOrNull(): String? {
    return tryResult {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        digest.joinToString("") { "%02x".format(it) }
    }.getOrNull()
}

fun InputStream.hexDigestOrNull(): String? {
    return tryResult {
        use { input ->
            val md = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                md.update(buffer, 0, bytesRead)
            }
            val digest = md.digest()
            digest.joinToString("") { "%02x".format(it) }
        }
    }.getOrNull()
}

fun Bitmap.hexDigestOrNull(
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.WEBP,
    quality: Int = 80
): String? {
    return tryResult {
        val md = MessageDigest.getInstance("MD5")
        val outputStream = ByteArrayOutputStream()
        compress(format, quality, outputStream)
        val digest = md.digest(outputStream.toByteArray())
        digest.joinToString("") { "%02x".format(it) }
    }.getOrNull()
}