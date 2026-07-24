package io.kelly.remember.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Composable
fun <T> rememberDisplayController(): DisplayController<T> {
    return remember {
        DisplayControllerImpl()
    }
}

@Stable
interface DisplayController<T> {
    val parameters: List<Any?>
    val data: T?
    val isShow: Boolean
    fun show(data: T, vararg params: Any?)
    fun dismiss()
}


