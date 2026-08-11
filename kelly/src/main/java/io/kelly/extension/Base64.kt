package io.kelly.extension

import android.util.Base64

fun String.encodeBase64(flags: Int = Base64.NO_WRAP or Base64.URL_SAFE): String {
    return Base64.encodeToString(toByteArray(Charsets.UTF_8), flags)
}

fun String.decodeBase64(flags: Int = Base64.NO_WRAP or Base64.URL_SAFE): String {
    return Base64.decode(this, flags).toString(Charsets.UTF_8)
}