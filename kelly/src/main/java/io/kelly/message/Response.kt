package io.kelly.message

import io.kelly.extension.onFinally
import io.kelly.extension.tryResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.lang.ref.WeakReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Response internal constructor(
    @PublishedApi
    internal val deferred: CompletableDeferred<Any?>,
    val isSuccess: Boolean
) {
    suspend inline fun <reified T> onResponse(
        timeout: Duration? = 5.seconds,
        crossinline block: suspend (T) -> Unit
    ) {
        if (!isSuccess) return
        tryResult {
            val rawResult = if (timeout == null) {
                deferred.await()
            } else {
                withTimeout(timeout) { deferred.await() }
            }
            val finalResult = if (rawResult is WeakReference<*>) rawResult.get() else rawResult
            if (finalResult is T) {
                block(finalResult)
            }
        }.onFinally {
            deferred.cancel()
        }
    }
}