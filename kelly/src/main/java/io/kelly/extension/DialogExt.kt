package io.kelly.extension

import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

internal val dialogWindow: Window?
    @Composable
    get() = (LocalView.current.parent as? DialogWindowProvider)?.window

@Composable
fun ConfigureDialogWindow(configure: Window.() -> Unit) {
    val window = dialogWindow ?: return
    SideEffect {
        configure(window)
    }
}

@Composable
fun ConfigureDialogWindowAttributes(configure: WindowManager.LayoutParams.() -> Unit) {
    val window = dialogWindow ?: return
    SideEffect {
        val attributes = window.attributes
        configure(attributes)
        window.attributes = attributes
    }
}

