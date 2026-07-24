package io.kelly.composable.paging

data class LoadParams<Key>(
    val key: Key?,
    val pageSize: Int,
)
