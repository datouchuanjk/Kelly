package io.kelly.composable.paging

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

interface Paging<Value> {
    val refreshState: LoadState
    val loadMoreState: LoadState
    fun start()
    fun refresh()
    fun retry()
    val snapshotList: List<Value>
    val itemKey: ((Int) -> Any)?
    val size: Int
    operator fun get(index: Int): Value
    fun transform(block: suspend (MutableList<Value>) -> List<Value>)
}

fun <Key, Value> buildPaging(
    scope: CoroutineScope,
    context: CoroutineContext = Dispatchers.IO,
    pagingStart: PagingStart = PagingStart.START,
    initialKey: Key? = null,
    keyProvider: ((Value) -> Any?)? = null,
    config: PagingConfig = PagingConfig(),
    distinctStrategy: DistinctStrategy = DistinctStrategy.DropOldest,
    load: suspend PagingScope.(LoadParams<Key>) -> LoadResult<Key, Value>
): Paging<Value> {
    return PagingImpl(
        scope = scope,
        context = context,
        pagingStart = pagingStart,
        initialKey = initialKey,
        keyProvider = keyProvider,
        distinctStrategy = distinctStrategy,
        config = config,
        load = load
    )
}

fun <Value> buildEmptyPaging(
    scope: CoroutineScope,
    initialList: List<Value> = emptyList(),
    keyProvider: ((Value) -> Any?)? = null,
    distinctStrategy: DistinctStrategy = DistinctStrategy.DropOldest,
) = buildPaging(
    scope = scope,
    keyProvider = keyProvider,
    distinctStrategy = distinctStrategy,
) {
    LoadResult(null, initialList)
}




