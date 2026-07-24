package io.kelly.remember.queue

data class QueueHolder<T>(
    val item: T,
    val dropItem: T?,
    val release: () -> Boolean
)

