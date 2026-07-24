package io.kelly.extension

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.lang.ref.WeakReference

fun <T> List<T?>.padToMultiple(multiple: Int, paddingValue: T? = null): List<T?> {
    if (isNullOrEmpty()) {
        return List(multiple) { paddingValue }
    }
    val remainder = size % multiple
    if (remainder == 0) return this
    return this + List(multiple - remainder) { paddingValue }
}

fun <T> List<T>.findIndexOrNull(predicate: (T) -> Boolean): Int? {
    return indexOfFirst(predicate).takeUnless { it == -1 }
}

fun <T> List<T>.findIndexValueOrNull(predicate: (T) -> Boolean): IndexedValue<T>? {
    return asSequence().withIndex().firstOrNull { predicate(it.value) }
}

fun <T> SnapshotStateList<T>.mutable(block: SnapshotStateList<T>.() -> Unit) {
    Snapshot.withMutableSnapshot { block() }
}

