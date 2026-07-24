package io.kelly.composable.paging.ui

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import io.kelly.composable.paging.Paging

fun <T> LazyListScope.items(
    paging: Paging<T>,
    contentType: (index: T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    items(
        count = paging.size,
        key = paging.itemKey,
        contentType = { index -> contentType(paging.snapshotList[index]) },
    ) { index ->
        itemContent(paging[index])
    }
}

fun <T> LazyListScope.itemsIndex(
    paging: Paging<T>,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit
) {
    items(
        count = paging.size,
        key = paging.itemKey,
        contentType = { index -> contentType(index, paging.snapshotList[index]) }) { index ->
        itemContent(index, paging[index])
    }
}






