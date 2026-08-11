package com.module.app.ui


import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import io.kelly.Kelly
import io.kelly.composable.webview.PersistentWebView
import io.kelly.composable.webview.WebPageState
import io.kelly.composable.webview.goBack
import io.kelly.composable.webview.rememberWebViewState

class HostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Kelly.init(this)
        enableEdgeToEdge()
        setContent {
            Scaffold { innerPadding ->
                val backPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
                val state = rememberWebViewState(
                    "baidu",
                    "http://www.baidu.com"
                )
                PersistentWebView(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
                if (state.webPageState is WebPageState.Loading) {
                    CircularProgressIndicator()
                }
                BackHandler {
                    if (state.canGoBack) {
                        state.goBack()
                    } else {
                        backPressedDispatcherOwner?.onBackPressedDispatcher?.onBackPressed()
                    }
                }
            }
        }
    }
}









