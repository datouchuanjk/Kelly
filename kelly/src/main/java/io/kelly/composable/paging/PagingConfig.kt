package io.kelly.composable.paging

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PagingConfig(
    val pageSize: Int = 20,
    val prefetchDistance: Int = 0,
    val requiredLoadDuration: Duration = 1.seconds,
    val distinctStrategy: DistinctStrategy = DistinctStrategy.DropOldest,
)
