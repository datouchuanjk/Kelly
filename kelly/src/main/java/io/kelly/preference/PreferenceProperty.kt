package io.kelly.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.kelly.KellyInternal
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class PreferenceProperty<T : Any> (
    private val key: String? = null,
    private val defaultValue: T,
    private val commit: Boolean = false,
    private val getter: SharedPreferences.(String, T) -> T?,
    private val setter: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
) : ReadWriteProperty<Any?, T> {

    companion object {
        private val sp by lazy {
            KellyInternal.context.getSharedPreferences(
                "Kelly_PreferenceDelegate_SP",
                Context.MODE_PRIVATE
            )
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val realKey = key ?: property.name
        return sp.getter(realKey, defaultValue) ?: defaultValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val realKey = key ?: property.name
        sp.edit(commit) { setter(realKey, value) }
    }
}