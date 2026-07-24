package io.kelly.remember.display

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect


@Composable
fun <T> DisplayController<T>.content(
    content: @Composable (DisplayController<T>.(T) -> Unit)
): DisplayController<T> {
    if (isShow) {
        data?.let { currentData ->
            content(currentData)
        }
    }
    return this
}

fun DisplayController<Unit>.show() = show(Unit)

inline fun <reified T> DisplayController<*>.getParameterOrNull(index: Int = 0): T? {
    return parameters.getOrNull(index) as? T
}

@Composable
fun DisplayController<Unit>.bindVisibility(isVisible: Boolean) = apply {
    LaunchedEffect(this, isVisible) {
        if (isVisible) show() else dismiss()
    }
}




