package io.kelly.preference

import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty

fun boolPreference(
    key: String? = null,
    default: Boolean = false,
    commit: Boolean = false,
): ReadWriteProperty<Any?, Boolean> = PreferenceProperty(
    key,
    default,
    commit,
    SharedPreferences::getBoolean,
    SharedPreferences.Editor::putBoolean
)

fun stringPreference(
    key: String? = null,
    default: String = "",
    commit: Boolean = false,
): ReadWriteProperty<Any?, String> = PreferenceProperty(
    key,
    default,
    commit,
    SharedPreferences::getString,
    SharedPreferences.Editor::putString
)

fun longPreference(
    key: String? = null,
    default: Long = 0L,
    commit: Boolean = false,
): ReadWriteProperty<Any?, Long> = PreferenceProperty(
    key,
    default,
    commit,
    SharedPreferences::getLong,
    SharedPreferences.Editor::putLong
)

fun intPreference(
    key: String? = null,
    default: Int = 0,
    commit: Boolean = false,
): ReadWriteProperty<Any?, Int> = PreferenceProperty(
    key,
    default,
    commit,
    SharedPreferences::getInt,
    SharedPreferences.Editor::putInt
)



