package io.kelly.remember.queue

sealed interface QueueOverflow<out T> {

    data object Wait : QueueOverflow<Nothing>

    data class Drop<T>(
        val onDrop: (List<T>, T) -> T? = { list, _ -> list.firstOrNull() }
    ) : QueueOverflow<T>
}