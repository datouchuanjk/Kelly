package io.kelly.flow

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> effectProperty(
    capacity: Int = Channel.BUFFERED,
    onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST,
    onUndeliveredElement: ((T) -> Unit)? = null
): EffectProperty<T> = EffectPropertyImpl(
    capacity = capacity,
    onBufferOverflow = onBufferOverflow,
    onUndeliveredElement = onUndeliveredElement
)

interface EffectProperty<T> : ReadOnlyProperty<Any?, Flow<T>> {
    fun trySend(value: T): Boolean
    suspend fun send(value: T)
}

internal class EffectPropertyImpl<T>(
    capacity: Int = Channel.BUFFERED,
    onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST,
    onUndeliveredElement: ((T) -> Unit)? = null
) : EffectProperty<T> {
    private val channel = Channel(
        capacity = capacity,
        onBufferOverflow = onBufferOverflow,
        onUndeliveredElement = onUndeliveredElement
    )
    private val flow = channel.receiveAsFlow()


    override suspend fun send(value: T) {
        channel.send(value)
    }

    override fun trySend(value: T): Boolean {
        return channel.trySend(value).isSuccess
    }

    override operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): Flow<T> {
        return flow
    }
}

fun EffectProperty<Unit>.trySend() = trySend(Unit)
suspend fun EffectProperty<Unit>.send() = send(Unit)