package io.kelly.composable.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.ViewModel

@SuppressLint("StaticFieldLeak")
internal class WebManager : ViewModel() {
    private var contextWrapper: MutableContextWrapper? = null
    private var webView: WebView? = null
    internal val state = WebViewState { webView }
    @SuppressLint("JavascriptInterface")
    fun requireWebView(
        activityContext: Context,
        initialUrl: String?
    ): WebView {
        if (webView == null) {
            val wrapper = MutableContextWrapper(activityContext).apply { contextWrapper = this }
            webView = WebView(wrapper).apply {
                state.initWebView(this)
                state.initWebSetting(this)
                state.initWebViewClient(this)
                state.initWebChromeClient(this)
                state.jsInterfaces.forEach { (name, obj) ->
                    addJavascriptInterface(obj, name)
                }
                initialUrl?.let {
                    loadUrl(initialUrl)
                }
            }
        } else {
            contextWrapper?.baseContext = activityContext
            (webView?.parent as? ViewGroup)?.removeView(webView)
            state.jsInterfaces.forEach { (name, obj) ->
                webView?.addJavascriptInterface(obj, name)
            }
            state.refreshNav(webView)
        }
        return requireNotNull(webView)
    }

    fun detachContext() {
        contextWrapper?.let { wrapper ->
            wrapper.baseContext = wrapper.baseContext.applicationContext
        }
    }

    override fun onCleared() {
        super.onCleared()
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.removeAllViews()
            it.stopLoading()
            it.webChromeClient = null
            it.destroy()
        }
        webView = null
        contextWrapper = null
    }
}