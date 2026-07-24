package io.kelly.composable.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun rememberWebViewState(key: String): WebViewState {
    if (key.isEmpty()) {
        throw NullPointerException("key is empty")
    }
    val webViewModel = viewModel<WebManager>(key = key)
    webViewModel.state.key = key
    return webViewModel.state
}

@Stable
class WebViewState internal constructor(internal val webViewProvider: () -> WebView?) {

    var title by mutableStateOf<String?>(null)
        internal set

    var url by mutableStateOf<String?>(null)
        internal set

    var webPageState by mutableStateOf<WebPageState>(WebPageState.Initial)
        internal set

    var canGoBack by mutableStateOf(false)
        internal set

    var canGoForward by mutableStateOf(false)
        internal set

    internal lateinit var key: String

    internal var onUrlInterceptor: ((String) -> Boolean)? = null

    internal var onPermissionRequest: ((PermissionRequest) -> Unit)? = null

    internal var onImageChooser: ((ValueCallback<Array<Uri>>) -> Unit)? = null

    internal var onLoadFailed: ((Int) -> Unit)? = null


    internal var onLoadSuccess: ((String) -> Unit)? = null

    internal var onSetting: ((WebSettings) -> Unit)? = null

    internal val jsInterfaces = mutableMapOf<String, Any>()

    @SuppressLint("JavascriptInterface")
    internal  fun addJsInterfaceInternal(name: String, obj: Any) {
        jsInterfaces[name] = obj
        webViewProvider()?.addJavascriptInterface(obj, name)
    }

    internal   fun removeJsInterfaceInternal(name: String) {
        jsInterfaces.remove(name)
        webViewProvider()?.removeJavascriptInterface(name)
    }

    @Composable
    fun setUrlInterceptor(block: (String) -> Boolean) = apply {
        val currentBlock by rememberUpdatedState(block)
        DisposableEffect(this) {
            onUrlInterceptor = { currentBlock(it) }
            onDispose {
                onUrlInterceptor = null
            }
        }
    }

    @Composable
    fun setOnLoadSuccess(block: (String) -> Unit) = apply {
        val currentBlock by rememberUpdatedState(block)
        DisposableEffect(this) {
            onLoadSuccess = { currentBlock(it) }
            onDispose {
                onLoadSuccess = null
            }
        }
    }

    @Composable
    fun setOnLoadFailed(block: (Int) -> Unit) = apply {
        val currentBlock by rememberUpdatedState(block)
        DisposableEffect(this) {
            onLoadFailed = { currentBlock(it) }
            onDispose {
                onLoadFailed = null
            }
        }
    }

    @Composable
    fun setOnPermissionRequest(block: (PermissionRequest) -> Unit) = apply {
        val currentBlock by rememberUpdatedState(block)
        DisposableEffect(this) {
            onPermissionRequest = { currentBlock(it) }
            onDispose {
                onPermissionRequest = null
            }
        }
    }

    @Composable
    fun setOnSetting(block: (WebSettings) -> Unit) = apply {
        val currentBlock by rememberUpdatedState(block)
        DisposableEffect(this) {
            onSetting = { currentBlock(it) }
            onDispose {
                onSetting = null
            }
        }
    }

    @Composable
    fun setOnImageChooser(block: (ValueCallback<Array<Uri>>) -> Unit) = apply {
        val currentBlock by rememberUpdatedState(block)
        DisposableEffect(this) {
            onImageChooser = { currentBlock(it) }
            onDispose {
                onImageChooser = null
            }
        }
    }

    internal fun refreshNav(view: WebView?) {
        canGoBack = view?.canGoBack() ?: false
        canGoForward = view?.canGoForward() ?: false
    }

    internal fun initWebView(webView: WebView) {
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    internal fun initWebSetting(webView: WebView) {
        if (onSetting == null) {
            @SuppressLint("SetJavaScriptEnabled")
            webView.settings.javaScriptEnabled = true
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
            webView.settings.domStorageEnabled = true
            webView.settings.mediaPlaybackRequiresUserGesture = true
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            webView.settings.databaseEnabled = true
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
        } else {
            onSetting?.invoke(webView.settings)
        }
    }

    internal fun initWebViewClient(webView: WebView) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                this@WebViewState.url = url
                refreshNav(view)
                webPageState = WebPageState.Loading(0f)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                this@WebViewState.url = url
                refreshNav(view)
                if (webPageState is WebPageState.Loading) {
                    onLoadSuccess?.invoke(url.orEmpty())
                    webPageState = WebPageState.Success
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return if (onUrlInterceptor == null) {
                    super.shouldOverrideUrlLoading(view, request)
                } else {
                    val requestUrl = request?.url?.toString().orEmpty()
                    onUrlInterceptor?.invoke(requestUrl) ?: false
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    onLoadFailed?.invoke(error?.errorCode ?: -1)
                    webPageState = WebPageState.Error.NetworkError(
                        failingUrl = request.url?.toString(),
                        errorCode = error?.errorCode,
                        description = error?.description?.toString()
                    )
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true) {
                    onLoadFailed?.invoke(errorResponse?.statusCode ?: -1)
                    webPageState = WebPageState.Error.HttpError(
                        failingUrl = request.url?.toString(),
                        statusCode = errorResponse?.statusCode,
                        description = errorResponse?.reasonPhrase
                    )
                }
            }
        }
    }

    internal fun initWebChromeClient(webView: WebView) {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (webPageState is WebPageState.Loading) {
                    webPageState = WebPageState.Loading(newProgress / 100f)
                }
                refreshNav(view)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                this@WebViewState.title = title
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null || onPermissionRequest == null) {
                    return
                }
                onPermissionRequest?.invoke(request)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (fileChooserParams?.acceptTypes.isNullOrEmpty()
                    || filePathCallback == null
                    || onImageChooser == null
                ) {
                    return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                }
                val acceptType = fileChooserParams.acceptTypes[0]
                if (acceptType.startsWith("image/")) {
                    onImageChooser?.invoke(filePathCallback)
                    return true
                }
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
            }
        }
    }
}