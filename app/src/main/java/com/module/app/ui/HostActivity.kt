package com.module.app.ui


import android.Manifest
import android.os.*
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Updater
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.module.app.R
import com.module.app.ui.HostViewModel.Update
import io.kelly.Kelly
import io.kelly.app
import io.kelly.composable.webview.PersistentWebView
import io.kelly.composable.webview.WebPageState
import io.kelly.composable.webview.addJavascriptInterface
import io.kelly.composable.webview.clearCache
import io.kelly.composable.webview.goBack
import io.kelly.composable.webview.load
import io.kelly.composable.webview.reload
import io.kelly.composable.webview.rememberWebViewState
import io.kelly.extension.FlowCollectEffect
import io.kelly.extension.saveToGallery
import io.kelly.extension.suspendResult
import io.kelly.extension.toast
import io.kelly.extension.tryJumpToUrl
import io.kelly.extension.versionName
import io.kelly.launcher.PermissionState
import io.kelly.launcher.rememberLauncherForExternalStorage
import io.kelly.launcher.rememberLauncherForRequestPermission
import io.kelly.remember.display.DisplayController
import io.kelly.remember.display.content
import io.kelly.remember.display.rememberDisplayController
import io.kelly.remember.display.show
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import kotlin.io.encoding.ExperimentalEncodingApi


class HostActivity : ComponentActivity() {
    @OptIn(ExperimentalEncodingApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Kelly.init(this)
        enableEdgeToEdge()
        setContent {
            val viewModel = viewModel<HostViewModel>()
            val updateDialog = rememberUpdateAppDialogController()
            FlowCollectEffect(viewModel.updateAppFlow) {
                updateDialog.show(it)
            }
            val controller = rememberLocationDialogController(viewModel) {
                viewModel.saveLocation()
            }
            val scope = rememberCoroutineScope()
            var isLoadSuccess by remember { mutableStateOf(false) }
            var base64Url by remember { mutableStateOf<String?>(null) }
            val launcher = rememberLauncherForExternalStorage(
                useSaf = false,
                requireWrite = true
            ) {
                if (it) {
                    base64Url?.let { urlStr ->
                        val cleanBase64 = if (urlStr.contains(",")) {
                            urlStr.substringAfter(",")
                        } else {
                            urlStr
                        }
                        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        scope.launch {
                            suspendResult {
                                ByteArrayInputStream(bytes).saveToGallery(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    Environment.DIRECTORY_PICTURES,
                                    "${System.nanoTime()}.png",
                                    "image/png"
                                )
                            }
                            "save success".toast()
                        }
                    }
                }
            }
            Scaffold { innerPadding ->
                var isLoading by remember { mutableStateOf(true) }
                val localActivity = LocalActivity.current
                val state = rememberWebViewState("key")
                    .setOnPermissionRequest {
                        it.grant(it.resources)
                    }.setOnLoadFailed {
                        viewModel.getNextUrl()
                    }.setOnLoadSuccess {
                        isLoadSuccess = true
                        if (viewModel.isFirstLaunchApp || viewModel.getGPS == 1) {
                            viewModel.isFirstLaunchApp = false
                            controller.show()
                        }
                    }.addJavascriptInterface("Android") {
                        object : WebInterface {
                            override fun getDevicesInfo(): String {
                                Log.e("1234","getDevicesInfo")
                                return viewModel.getDevicesInfo()
                            }

                            override fun savePic(base64: String) {
                                Log.e("1234","savePic")
                                if (base64.isEmpty()) return
                                base64Url = base64
                                scope.launch {
                                    launcher.launch()
                                }
                            }

                            override fun callapp(url: String) {
                                Log.e("1234","callapp")
                                scope.launch {
                                    tryJumpToUrl(url)
                                }
                            }

                            override fun clearCacheAndReload() {
                                scope.launch {
                                    clearCache()
                                    reload()
                                }
                            }
                        }
                    }
                PersistentWebView(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
                if (isLoading) {
                    WebViewLoading(isLoadSuccess) {
                        isLoading = false
                    }
                }
                BackHandler {
                    if (state.canGoBack) {
                        state.goBack()
                    } else {
                        localActivity?.finish()
                    }
                }
                FlowCollectEffect(viewModel.clearCacheFlow) {
                    state.clearCache()
                }
                FlowCollectEffect(viewModel.urlFlow) {
                    Log.e("1234", "地址拿到了${it}")
                    state.load(it)
                }
            }
        }
    }


}


@Composable
private fun rememberLocationDialogController(
    viewModel: HostViewModel,
    block: () -> Unit
): DisplayController<Unit> {
    val currentBlock by rememberUpdatedState(block)
    val launcher = rememberLauncherForRequestPermission(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) {
        currentBlock()
    }
    return rememberDisplayController<Unit>()
        .content {
            if (
                PermissionState(Manifest.permission.ACCESS_FINE_LOCATION).isGranted
                && PermissionState(Manifest.permission.ACCESS_COARSE_LOCATION).isGranted
            ) {
                LaunchedEffect(Unit) {
                    currentBlock()
                    dismiss()
                }
                return@content
            }
            AlertDialog(
                properties = DialogProperties(
                    dismissOnClickOutside = false
                ),
                onDismissRequest = {
                    viewModel.isFirstLaunchApp = false
                    dismiss()
                },
                title = {
                    Text("Tips")
                },
                text = {
                    Text("We'd love to know where you are so we can tailor our services for you. Please turn on location access")
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.isFirstLaunchApp = false
                        launcher.launch()
                        dismiss()
                    }) {
                        Text("Turn On")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.isFirstLaunchApp = false
                        dismiss()
                    }) {
                        Text("Maybe Later")
                    }
                }
            )
        }
}

@Composable
private fun rememberUpdateAppDialogController(): DisplayController<Update> {
    val localActivity = LocalActivity.current
    return rememberDisplayController<Update>()
        .content {
            AlertDialog(
                properties = DialogProperties(
                    dismissOnClickOutside = false
                ),
                onDismissRequest = {
                    dismiss()
                },
                title = {
                    Text("Tips")
                },
                text = {
                    Text(it.updateInfo)
                },
                confirmButton = {
                    TextButton(onClick = {
                        tryJumpToUrl(it.apkUrl)
                    }) {
                        Text("update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        if (it.forceUpdate == 1) {
                            localActivity?.finish()
                        } else {
                            dismiss()
                        }
                    }) {
                        Text(if (it.forceUpdate == 1) "exit" else "cancel")
                    }
                }
            )
        }
}

@Composable
private fun WebViewLoading(isLoadSuccess: Boolean, onFinish: () -> Unit) {
    val currentOnFinish by rememberUpdatedState(onFinish)
    val animatable = remember { Animatable(0f) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(R.drawable.bg_logo),
                contentScale = ContentScale.FillBounds
            )
    ) {
        Canvas(
            modifier = Modifier
                .align(alignment = BiasAlignment(0f, 0.8f))
                .fillMaxWidth()
                .height(10.dp)
                .padding(horizontal = 15.dp)
        ) {
            drawRoundRect(
                cornerRadius = CornerRadius(5.dp.toPx()),
                size = Size(size.width, size.height),
                brush = SolidColor(Color(0xff563585))
            )
            drawRoundRect(
                cornerRadius = CornerRadius(5.dp.toPx()),
                size = Size(size.width * animatable.value, size.height),
                brush = Brush.linearGradient(listOf(Color(0xffc3b3fe), Color(0xfff8bc2a)))
            )
        }
        Text(
            text = "version:${app.versionName}",
            fontSize = 12.sp,
            color = Color.White.copy(0.3f),
            modifier = Modifier
                .align(alignment = BiasAlignment(1f, 1f))
                .fillMaxWidth()
                .wrapContentWidth(align = Alignment.End)
                .padding(horizontal = 15.dp)
                .padding(bottom = 10.dp)
        )
        if (isLoadSuccess) {
            LaunchedEffect(Unit) {
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(500),
                    block = {
                        if (this.value >= 1f) {
                            currentOnFinish()
                        }
                    }
                )
            }
        } else {
            LaunchedEffect(Unit) {
                animatable.animateTo(
                    targetValue = 0.99f,
                    animationSpec = tween(10 * 1000),
                )
            }
        }
    }
}



