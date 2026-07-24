package io.kelly.composable.webview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PersistentWebView(
    modifier: Modifier = Modifier,
    state: WebViewState,
    initialUrl: String? = null
) {
    val webViewModel: WebManager = viewModel(key = state.key)
    DisposableEffect(state.key) {
        onDispose {
            webViewModel.detachContext()
        }
    }
    key(state.key) {
        AndroidView(
            factory = {
                webViewModel.requireWebView(it, initialUrl)
            },
            modifier = modifier
        )
    }
}