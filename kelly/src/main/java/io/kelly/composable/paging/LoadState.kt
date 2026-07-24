package io.kelly.composable.paging

import kotlinx.coroutines.flow.Flow

sealed class LoadState(
    open val endOfPaginationReached: Boolean
) {
    data class NotLoading(
        override val endOfPaginationReached: Boolean
    ) : LoadState(endOfPaginationReached)

    data object Loading : LoadState(false)

    data class Error(val error: Throwable,  val event: Flow<Throwable>) : LoadState(false)
}

val LoadState.isLoading get() = this is LoadState.Loading
val LoadState.isNotLoading get() = this is LoadState.NotLoading
val LoadState.isError get() = this is LoadState.Error