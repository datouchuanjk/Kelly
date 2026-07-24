package io.kelly.composable.paging


sealed interface DistinctStrategy {
    data  object DropLatest : DistinctStrategy
    data  object DropOldest : DistinctStrategy
}

