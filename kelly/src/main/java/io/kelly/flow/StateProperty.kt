package io.kelly.flow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> stateProperty(value: T): StateProperty<T> = StatePropertyImpl(value)

interface StateProperty<T> : ReadOnlyProperty<Any?, StateFlow<T>> {
    var value: T
    fun update(function: (T) -> T)
}

internal class StatePropertyImpl<T>(value: T) : StateProperty<T> {

    private val _flow = MutableStateFlow(value)

    private val flow = _flow.asStateFlow()

    override var value: T
        get() = flow.value
        set(value) {
            _flow.value = value
        }

    override fun update(function: (T) -> T) {
        _flow.update(function)
    }

    override operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): StateFlow<T> {
        return flow
    }
}

fun <T> StateFlow<T>?.orNull() = this ?: MutableStateFlow(null)

