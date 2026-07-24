package io.kelly.composable.paging

interface PagingScope {

    fun <Key, Value> List<Value>.withNextKey(
        nextKey: Key?
    ): LoadResult<Key, Value> {
        return LoadResult(nextKey, this)
    }
}

internal object PagingScopeImpl : PagingScope


