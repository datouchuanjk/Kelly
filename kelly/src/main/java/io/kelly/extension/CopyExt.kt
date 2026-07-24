package io.kelly.extension

import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.getSystemService
import io.kelly.KellyInternal

fun String.copyToClipboard(label: String? = null): Boolean {
    return KellyInternal.context
        .getSystemService<ClipboardManager>()
        ?.setPrimaryClip(ClipData.newPlainText(label, this)) != null
}

fun getClipboardTextOrNull(): String? {
    val clipboardManager = KellyInternal.context.getSystemService<ClipboardManager>() ?: return null
    val clipData = clipboardManager.primaryClip ?: return null
    return if (clipData.itemCount > 0) clipData.getItemAt(0).text?.toString() else null
}

