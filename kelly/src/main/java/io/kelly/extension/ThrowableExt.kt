package io.kelly.extension

import io.kelly.KellyInternal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

inline fun <T> tryResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        e.handleCancellation()
        Result.failure(e)
    }
}

suspend inline fun <T> suspendResult(
    context: CoroutineContext = Dispatchers.IO,
    crossinline block: suspend CoroutineScope.() -> T
): Result<T> {
    return withContext(context) {
        tryResult { block() }
    }
}

inline fun <T> Result<T>.onFinally(block: () -> Unit) = apply {
    block()
}

@PublishedApi
internal fun Throwable.handleCancellation() {
    if (this is CancellationException) {
        throw this
    }
    if (KellyInternal.isDebug) {
        printStackTrace()
    }
}


