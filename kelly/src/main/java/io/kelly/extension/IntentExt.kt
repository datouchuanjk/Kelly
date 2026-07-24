package io.kelly.extension

import android.content.*
import android.os.*
import androidx.core.net.*
import io.kelly.*
import java.io.*

fun tryJumpToUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return tryResult {
        Intent(Intent.ACTION_VIEW, url.toUri())
            .startActivityWithNewTask()
        true
    }.getOrDefault(false)
}

fun tryJumpToAction(action: String?): Boolean {
    if (action.isNullOrBlank()) return false
    return tryResult {
        Intent(action).startActivityWithNewTask()
        true
    }.getOrDefault(false)
}

fun tryJumpToPackage(packageName: String): Boolean {
    val intent = KellyInternal.context.packageManager
        .getLaunchIntentForPackage(packageName) ?: return false
    intent.startActivityWithNewTask()
    return true
}

internal fun Intent.startActivityWithNewTask() {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    KellyInternal.context.startActivity(this)
}

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        getParcelableExtra(key) as? T
    }
}

inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(key, T::class.java)
    } else {
        getSerializableExtra(key) as? T
    }
}

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        getParcelable(key) as? T
    }
}

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, T::class.java)
    } else {
        getSerializable(key) as? T
    }
}
