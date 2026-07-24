package io.kelly.remember.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kelly.extension.FlowCollectEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun <T : Any> QueueController<T>.bindController(
    flow: Flow<T>,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    state: Lifecycle.State = Lifecycle.State.STARTED,
) = apply {
    FlowCollectEffect(flow,state,lifecycleOwner) {
        enqueue(it)
    }
}

@Composable
fun <T> QueueController<T>.itemContent(
    keyProvider: (T) -> Any,
    content: @Composable QueueHolder<T>.() -> Unit
): QueueController<T> {
    list.forEach {
        key(keyProvider(it.item)) {
            it.content()
        }
    }
    return this
}









