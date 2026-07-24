package io.kelly.extension

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateMap

fun <T, R> Map<T?, R?>.filterNotNull(): Map<T, R> {
    val destination = mutableMapOf<T, R>()
    for (element in this) {
        val key = element.key
        val value = element.value
        if (key != null && value != null) {
            destination[key] = value
        }
    }
    return destination
}

fun <T, R> SnapshotStateMap<T, R>.mutable(block: SnapshotStateMap<T, R>.() -> Unit) {
    Snapshot.withMutableSnapshot {
        block()
    }
}

