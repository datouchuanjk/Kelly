package io.kelly.remember.display

import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.kelly.extension.ConfigureDialogWindow
import io.kelly.extension.ConfigureDialogWindowAttributes
import io.kelly.modifier.clickableNoRipple

@Composable
inline fun <T> DisplayController<T>.dialog(
    properties: DialogProperties = DialogProperties(),
    crossinline onDismissRequest: DisplayController<T>.() -> Unit = {  },
    crossinline content: @Composable (DisplayController<T>.(T) -> Unit)
) = content { data ->
    Dialog(
        properties = properties,
        onDismissRequest = {
            onDismissRequest()
            dismiss()
        }
    ) {
        content(data)
    }
}

@Composable
inline fun <T> DisplayController<T>.bottomDialog(
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false
    ),
    crossinline onDismissRequest: DisplayController<T>.() -> Unit = {  },
    crossinline content: @Composable (DisplayController<T>.(T) -> Unit)
) = dialog(properties, onDismissRequest) { data ->
    ConfigureDialogWindowAttributes {
        gravity = Gravity.BOTTOM
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickableNoRipple(properties.dismissOnClickOutside) {
                    dismiss()
                }
        )
        content(data)
    }
}

@Composable
inline fun DisplayController<Unit>.loadingDialog(
    properties: DialogProperties = DialogProperties(
        decorFitsSystemWindows = false,
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
    ),
    crossinline onDismissRequest: DisplayController<Unit>.() -> Unit = { dismiss() },
) = dialog(properties, onDismissRequest) {
    ConfigureDialogWindow {
        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        setBackgroundDrawableResource(android.R.color.transparent)
    }
    CircularProgressIndicator(
        color = Color.White,
        strokeWidth = 1.dp,
        modifier = Modifier
            .background(
                color = Color.Black.copy(0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
            .size(25.dp)
    )
}




