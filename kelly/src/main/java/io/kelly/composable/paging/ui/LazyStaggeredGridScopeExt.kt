package io.kelly.composable.paging.ui

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import io.kelly.composable.paging.Paging

fun <T> LazyStaggeredGridScope.items(
    paging: Paging<T>,
    span: ((T) -> StaggeredGridItemSpan)? = null,
    contentType: (T) -> Any? = { null },
    itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
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

fun <T> LazyStaggeredGridScope.itemsIndex(
    paging: Paging<T>,
    span: ((index: Int, item: T) -> StaggeredGridItemSpan)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyStaggeredGridItemScope.(index: Int, item: T) -> Unit
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