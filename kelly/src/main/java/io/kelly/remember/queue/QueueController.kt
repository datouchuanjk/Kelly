package io.kelly.remember.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore

@Composable
fun <T> rememberQueueController(
    capacity: Int = 1,
    onQueueOverflow: QueueOverflow<T> = QueueOverflow.Wait,
): QueueController<T> {
    val scope = rememberCoroutineScope()
    val controller = remember(scope, capacity, onQueueOverflow) {
        QueueController(
            scope = scope,
            capacity = capacity.coerceAtLeast(1),
            onQueueOverflow = onQueueOverflow
        )
    }
    DisposableEffect(controller) {
        onDispose {
            controller.close()
        }
    }
    return controller
}

@Stable
class QueueController<T> internal constructor(
    scope: CoroutineScope,
    capacity: Int,
    private val onQueueOverflow: QueueOverflow<T>,
) {

    internal var onFilter: ((T) -> Boolean)? = null

    @Composable
    fun setFilter(block: (T) -> Boolean) = apply {
        val currentBlock by rememberUpdatedState(block)
        DisposableEffect(this) {
            onFilter = { currentBlock(it) }
            onDispose {
                onFilter = null
            }
        }
    }

    private val _input = Channel<T>(Channel.BUFFERED, BufferOverflow.DROP_OLDEST)

    private val _semaphore = Semaphore(capacity)

    private val _queueHolders = mutableStateListOf<QueueHolder<T>>()

    val list: List<QueueHolder<T>> get() = _queueHolders

    init {
        scope.launch {
            for (item in _input) {
                processItem(item)
            }
        }
    }

    private suspend fun processItem(item: T) {
        lateinit var queueHolder: QueueHolder<T>
        queueHolder = QueueHolder(
            item = item,
            dropItem = null,
            release = {
                if (_queueHolders.remove(queueHolder)) {
                    _semaphore.release()
                    true
                } else {
                    false
                }
            }
        )

        if (!_semaphore.tryAcquire()) {

            when (onQueueOverflow) {

                is QueueOverflow.Wait -> {
                    _semaphore.acquire()
                }

                is QueueOverflow.Drop -> {
                    val currentItems = _queueHolders.map { it.item }
                    val dropItem = onQueueOverflow.onDrop.invoke(currentItems, item)
                    if (dropItem != null) {
                        val dropRelease = _queueHolders.find {
                            it.item == dropItem
                        }
                        if (dropRelease?.release() == true) {
                            queueHolder = queueHolder.copy(
                                dropItem = dropItem
                            )
                        }
                    }
                    _semaphore.acquire()
                }
            }
        }
        if (onFilter?.invoke(item) ?: true) {
            _queueHolders.add(queueHolder)
        } else {
            queueHolder.release()
        }
    }

    fun enqueue(item: T): Boolean {
        if (onFilter?.invoke(item) ?: true) {
            return _input.trySend(item).isSuccess
        }
        return false
    }

    internal fun close() {
        _input.close()
    }
}