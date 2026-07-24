package io.kelly.flow

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> eventProperty(
    extraBufferCapacity: Int = 64,
    onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST
): EventProperty<T> = EventPropertyImpl(
    extraBufferCapacity = extraBufferCapacity,
    onBufferOverflow = onBufferOverflow
)

interface EventProperty<T> : ReadOnlyProperty<Any?, SharedFlow<T>> {
    fun tryEmit(value: T): Boolean
    suspend fun emit(value: T)
}

internal class EventPropertyImpl<T>(
    extraBufferCapacity: Int = 0,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
) : EventProperty<T> {

    private val _flow = MutableSharedFlow<T>(
        replay = 0,
        extraBufferCapacity = extraBufferCapacity,
        onBufferOverflow = onBufferOverflow
    )

    private val flow = _flow.asSharedFlow()

    override suspend fun emit(value: T) {
        _flow.emit(value)
    }

    override fun tryEmit(value: T): Boolean {
        return _flow.tryEmit(value)
    }

    override operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): SharedFlow<T> {
        return flow
    }
}

fun EventProperty<Unit>.tryEmit() = tryEmit(Unit)
suspend fun EventProperty<Unit>.emit() = emit(Unit)

