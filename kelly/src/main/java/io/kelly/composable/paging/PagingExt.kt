package io.kelly.composable.paging

fun <T> Paging<T>.clear() = transform { emptyList() }

fun <T> Paging<T>.isEmpty() = size == 0

fun <T> Paging<T>.removeIf(predicate: (T) -> Boolean) {
    transform { list ->
        list.removeIf(predicate)
        list
    }
}

fun <T> Paging<T>.updateIf(
    predicate: (T) -> Boolean,
    newItem: (T) -> T
) {
    transform { list ->
        list.toList().forEachIndexed { i, item ->
            if (predicate(item)) {
                list[i] = newItem(item)
            }
        }
        list
    }
}
