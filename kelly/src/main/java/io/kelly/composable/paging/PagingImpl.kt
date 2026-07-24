package io.kelly.composable.paging

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import io.kelly.extension.tryResult
import io.kelly.flow.effectProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * 我来说一下我设计的几个点
 * 第一个 为什么 修改是一个 list job
 * 因为我可能有 5 个修改，第一个正在做
 * 如果这个时候刷新？
 * 那么我是不是 5个都要取消
 * 因为里面有锁 等于说 其实 5 个 job 都创建了，只是后面 4 个在等第一个完成对不对？
 *
 * 其次为什么我的 state 里面要加 flow 其实这个 flow 是个 channel
 * 为什么我要这么设计，
 * 如果是无数据的情况下 可以看我的 pagingBox 我会显示空界面！
 * 但是在我有数据的情况下？ 你错了 去显示错误界面吗？ 不 你应该 toast
 * 那么这个时候 toast 作为事件 如果你通过 state==error 这个状态去处理 那么只要遇到重建 那么就废了，所以这里我使用事件！
 * 而且是必达事件，channel
 *
 * 所以 现在给我一份完整代码。就这个impl类，主要是时间那块，的修改 其他的应该没什么
 */
internal class PagingImpl<Key, Value>(
    val scope: CoroutineScope,
    val context: CoroutineContext,
    val pagingStart: PagingStart = PagingStart.START,
    val initialKey: Key? = null,
    val keyProvider: ((Value) -> Any?)? = null,
    val distinctStrategy: DistinctStrategy = DistinctStrategy.DropOldest,
    val config: PagingConfig = PagingConfig(),
    val load: suspend PagingScope.(LoadParams<Key>) -> LoadResult<Key, Value>
) : Paging<Value>, CoroutineScope by scope {

    private var _nextKey: Key? = initialKey
    private val _mutex = Mutex()
    private var _refreshJob: Job? = null
    private var _appendJob: Job? = null
    private var _transformJobs = CopyOnWriteArrayList<Job>()
    private val _isRefreshing get() = _refreshJob?.isActive == true
    private val _isLoadingMore get() = _appendJob?.isActive == true
    private var _pagingStart = pagingStart
    private val _list = mutableStateListOf<Value>()
    private val _event = effectProperty<Throwable>()
    private val _eventFlow by _event
    override var refreshState: LoadState by mutableStateOf(LoadState.Loading)
    override var loadMoreState: LoadState by mutableStateOf(LoadState.NotLoading(false))
    override val snapshotList: List<Value> by derivedStateOf { _list.toList() }

    private fun List<Value>.distinctByStrategy(): List<Value> {
        if (keyProvider == null) {
            return this
        }
        return when (distinctStrategy) {
            DistinctStrategy.DropLatest -> {
                distinctBy(keyProvider)
            }

            DistinctStrategy.DropOldest -> {
                asReversed()
                    .distinctBy(keyProvider)
                    .asReversed()
            }
        }
    }

    init {
        if (_pagingStart == PagingStart.START) {
            refresh()
        }
    }

    override fun start() {
        if (_pagingStart == PagingStart.LAZY) {
            refresh()
        }
    }

    override fun refresh() {
        _pagingStart = PagingStart.START
        if (_isRefreshing) return
        _appendJob?.cancel()
        _transformJobs.forEach { it.cancel() }
        _refreshJob = launch {
            _mutex.withLock {
                val startMark = TimeSource.Monotonic.markNow()
                tryResult {
                    refreshState = LoadState.Loading
                    loadMoreState = LoadState.NotLoading(false)
                    val result = withContext(context) {
                        load(
                            PagingScopeImpl,
                            LoadParams(
                                key = initialKey,
                                pageSize = config.pageSize,
                            )
                        )
                    }
                    delayMinimumLoadingTime(startMark)
                    _nextKey = result.nextKey
                    val isEnd = _nextKey == null
                    refreshState = LoadState.NotLoading(isEnd)
                    if (isEnd) loadMoreState = LoadState.NotLoading(true)
                    Snapshot.withMutableSnapshot {
                        val rawData = result.data
                        _list.clear()
                        _list.addAll(rawData.distinctByStrategy())
                    }
                } .onFailure {
                    delayMinimumLoadingTime(startMark)
                    refreshState = LoadState.Error(it, _eventFlow)
                    _event.trySend(it)
                }
            }
        }
    }

    private fun load() {
        if (_pagingStart == PagingStart.LAZY) return
        if (_isRefreshing || _isLoadingMore) return
        val currentLoadMore = loadMoreState
        if (currentLoadMore is LoadState.NotLoading && currentLoadMore.endOfPaginationReached) return
        if (currentLoadMore is LoadState.Error) return
        _appendJob = launch {
            _mutex.withLock {
                val startMark = TimeSource.Monotonic.markNow()
                tryResult {
                    loadMoreState = LoadState.Loading
                    val result = withContext(context) {
                        load(
                            PagingScopeImpl,
                            LoadParams(key = _nextKey, pageSize = config.pageSize)
                        )
                    }
                    delayMinimumLoadingTime(startMark)
                    _nextKey = result.nextKey
                    loadMoreState = LoadState.NotLoading(_nextKey == null)
                    Snapshot.withMutableSnapshot {
                        val rawData = result.data
                        val list = _list + rawData.distinctByStrategy()
                        val newList = list.distinctByStrategy()
                        _list.clear()
                        _list.addAll(newList)
                    }
                }.onFailure {
                    delayMinimumLoadingTime(startMark)
                    loadMoreState = LoadState.Error(it, _eventFlow)
                    _event.trySend(it)
                }
            }
        }
    }

    override fun retry() {
        if (_isRefreshing) return
        if (refreshState is LoadState.Error) {
            refresh()
            return
        }
        if (loadMoreState is LoadState.Error) {
            loadMoreState = LoadState.NotLoading(false)
            load()
        }
    }


    private suspend fun delayMinimumLoadingTime(
        startMark: TimeSource.Monotonic.ValueTimeMark
    ) {
        val elapsed = startMark.elapsedNow()
        if (elapsed < config.requiredLoadDuration) {
            delay(config.requiredLoadDuration - elapsed)
        }
    }

    override val itemKey: ((Int) -> Any)?
        get() {
            if (keyProvider == null) {
                return null
            } else {
                return { index ->
                    val item = snapshotList[index]
                    keyProvider(item) ?: index
                }
            }
        }

    override fun get(index: Int): Value {
        val prefetchDistance = config.prefetchDistance.coerceAtLeast(0)
        if (index + prefetchDistance >= _list.lastIndex) {
            load()
        }
        return _list[index]
    }

    override val size: Int get() = snapshotList.size

    override fun transform(block: suspend (MutableList<Value>) -> List<Value>) {
        val job = launch {
            _mutex.withLock {
                tryResult {
                    val tempList = snapshotList.toMutableList()
                    val result = block(tempList)
                    Snapshot.withMutableSnapshot {
                        _list.clear()
                        _list.addAll(result.distinctByStrategy())
                    }
                }
            }
        }
        _transformJobs.add(job)
        job.invokeOnCompletion {
            _transformJobs.remove(job)
        }
    }
}