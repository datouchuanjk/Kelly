package io.kelly.db

import android.content.ContentValues

fun stringColumn(name: String): Column<String> = Column.create(name, "TEXT")
fun intColumn(name: String): Column<Int> = Column.create(name, "INTEGER")
fun longColumn(name: String): Column<Long> = Column.create(name, "INTEGER")
fun doubleColumn(name: String): Column<Double> = Column.create(name, "REAL")
fun floatColumn(name: String): Column<Float> = Column.create(name, "REAL")
fun booleanColumn(name: String): Column<Boolean> = Column.create(name, "BOOLEAN")

interface Column<T> {
    val name: String
    val type: String
    fun ContentValues.put(value: T?) {
        putAny(name, value)
    }

    companion object {
        internal fun <T> create(
            name: String,
            type: String,
        ) = object : Column<T> {
            override val name: String = name
            override val type: String = type
        }
    }
}

private fun <T> ContentValues.putAny(key: String, value: T) {
    if (value == null) {
        putNull(key)
    } else {
        when (value) {
            is String -> put(key, value)
            is Byte -> put(key, value)
            is Short -> put(key, value)
            is Int -> put(key, value)
            is Long -> put(key, value)
            is Float -> put(key, value.toDouble())
            is Double -> put(key, value)
            is Boolean -> put(key, value)
            is ByteArray -> put(key, value)
            else -> throw IllegalArgumentException("Unsupported type: ${value::class.java.name}")
        }
    }
}