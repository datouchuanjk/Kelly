package io.kelly.extension

import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class CoroutineEffectScope {
    inline fun dispose(
        crossinline onDisposeEffect: () -> Unit
    ) = CoroutineEffectResult {
        onDisposeEffect()
    }
}

fun interface CoroutineEffectResult {
    fun dispose()
}

fun CoroutineScope.disposableEffect(effect: CoroutineEffectScope.() -> CoroutineEffectResult) {
    val handler = CoroutineEffectScope().let(effect)
    coroutineContext[Job]?.invokeOnCompletion {
        handler.dispose()
    }
}