package io.kelly.composable.webview

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import org.json.JSONObject.quote

@SuppressLint("JavascriptInterface")
@Composable
fun <T : Any> WebViewState.addJavascriptInterface(name: String, block: WebViewState.() -> T) = apply {
    val obj = remember(this, name) { block() }
    DisposableEffect(this, name, obj) {
        addJsInterfaceInternal(name, obj)
        onDispose {
            removeJsInterfaceInternal(name)
        }
    }
}

@Composable
fun WebViewState.openDebug() = apply {
    DisposableEffect(this) {
        WebView.setWebContentsDebuggingEnabled(true)
        onDispose {
            WebView.setWebContentsDebuggingEnabled(false)
        }
    }
}

fun WebViewState.goBack() {
    webViewProvider()?.let {
        if (it.canGoBack()) {
            it.goBack()
            refreshNav(it)
        }
    }
}

fun WebViewState.goForward() {
    webViewProvider()?.let {
        if (it.canGoForward()) {
            it.goForward()
            refreshNav(it)
        }
    }
}

fun WebViewState.reload() {
    load(url)
}

fun WebViewState.clearCache() {
    webViewProvider()?.clearCache(true)
}

fun WebViewState.load(url: String?) {
    if (url.isNullOrEmpty()) {
        return
    }
    webViewProvider()?.loadUrl(url)
}

fun WebViewState.evaluateJavascript(
    method: String,
    vararg params: String?,
    callback: (String) -> Unit = {}
) {
    val formattedParams = params.joinToString(",") { param ->
        if (param == null) "null"
        else quote(param)
    }
    webViewProvider()?.evaluateJavascript(
        "javascript:${method}($formattedParams)",
        callback
    )
}
