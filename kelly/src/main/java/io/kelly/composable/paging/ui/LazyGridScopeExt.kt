package io.kelly.composable.paging.ui

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import io.kelly.composable.paging.Paging

fun <T> LazyGridScope.items(
    paging: Paging<T>,
    span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    contentType: (item: T) -> Any? = { null },
    itemContent: @Composable (LazyGridItemScope.(T) -> Unit)
) {
    items(
        count = paging.size,
        key = paging.itemKey,
        span = span?.let { { index -> it(paging.snapshotList[index]) } },
        contentType = { index -> contentType(paging.snapshotList[index]) }
    ) { index ->
        itemContent(paging[index])
    }
}

fun <T> LazyGridScope.itemsIndex(
    paging: Paging<T>,
    span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable (LazyGridItemScope.(index: Int, item: T) -> Unit)
) {
    items(
        count = paging.size,
        key = paging.itemKey,
        span = span?.let { { index -> it(index, paging.snapshotList[index]) } },
        contentType = { index -> contentType(index, paging.snapshotList[index]) }
    ) { index ->
        itemContent(index, paging[index])
    }
}
