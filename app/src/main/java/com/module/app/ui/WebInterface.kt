package com.module.app.ui

import android.webkit.JavascriptInterface
import androidx.annotation.Keep

@Keep
interface WebInterface {

    @JavascriptInterface
    fun getDevicesInfo(): String

    @JavascriptInterface
    fun savePic(base64: String)

    @JavascriptInterface
    fun callapp(url: String)

    @JavascriptInterface
    fun clearCacheAndReload()

    @JavascriptInterface
    fun takePic()
}