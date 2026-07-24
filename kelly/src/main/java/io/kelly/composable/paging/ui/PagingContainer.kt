package io.kelly.composable.paging.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.kelly.extension.FlowCollectEffect
import io.kelly.composable.paging.LoadState
import io.kelly.composable.paging.Paging
import io.kelly.extension.toast
import io.kelly.composable.paging.isError
import io.kelly.composable.paging.isLoading
import io.kelly.composable.paging.isNotLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> PagingContainer(
    modifier: Modifier = Modifier,
    paging: Paging<T>,
    enablePullToRefresh: Boolean = true,
    isRefreshing: Boolean = paging.refreshState.isLoading,
    onRefresh: () -> Unit = { paging.refresh() },
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    indicator: @Composable BoxScope.() -> Unit = {
        Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = state
        )
    },
    skeletonContent: (@Composable (Modifier) -> Unit)? = null,
    emptyContent: (@Composable (Modifier) -> Unit)? = null,
    failureContent: (@Composable (Modifier) -> Unit)? = null,
    loadingContent: (@Composable (Modifier) -> Unit)? = null,
    loadErrorContent: (@Composable (Modifier) -> Unit)? = null,
    notLoadingContent: (@Composable (Modifier) -> Unit)? = null,
    onRefreshFailed: ((Throwable) -> Unit)? = {
        if (paging.size > 0 || failureContent == null) {
            it.message.toast()
        }
    },
    onLoadFailed: ((Throwable) -> Unit)? = {
        if (loadErrorContent == null) {
            it.message.toast()
        }
    },
    content: @Composable BoxScope.(@Composable (Modifier) -> Unit) -> Unit
) {
    val refreshState = paging.refreshState
    val loadMoreState = paging.loadMoreState
    if (refreshState is LoadState.Error) {
        FlowCollectEffect(refreshState.event) {
            onRefreshFailed?.invoke(refreshState.error)
        }
    }
    if (loadMoreState is LoadState.Error) {
        FlowCollectEffect(loadMoreState.event) {
            onLoadFailed?.invoke(loadMoreState.error)
        }
    }
    if (paging.size == 0 && refreshState.isLoading && skeletonContent != null) {
        skeletonContent(modifier)
    } else if (paging.size == 0 && refreshState.isError && failureContent != null) {
        failureContent(modifier)
    } else if (paging.size == 0 && refreshState.isNotLoading && emptyContent != null) {
        emptyContent(modifier)
    } else {
        val footer: @Composable (Modifier) -> Unit = { modifier ->
            when (loadMoreState) {
                is LoadState.Loading -> {
                    loadingContent?.invoke(modifier)
                }

                is LoadState.NotLoading -> {
                    notLoadingContent?.invoke(modifier)
                }

                is LoadState.Error -> {
                    loadErrorContent?.invoke(modifier)
                }
            }
        }
        if (enablePullToRefresh) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier,
                state = state,
                contentAlignment = contentAlignment,
                indicator = indicator
            ) {
                content(footer)
            }
        } else {
            Box(modifier = modifier) {
                content(footer)
            }
        }
    }
}

