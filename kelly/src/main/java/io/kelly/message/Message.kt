package io.kelly.message

import io.kelly.extension.tryResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.receiveAsFlow
import java.lang.ref.WeakReference

fun <T> sendMessage(
    key: String,
    value: T,
): Response {
    val channel = CommunicationManager.instance.getMessage(key)
    val deferred = CompletableDeferred<Any?>()
    val request = Request(deferred, value)
    val isSuccess = channel.trySend(request).isSuccess
    return Response(deferred, isSuccess)
}

suspend inline fun <reified T> receiveMessage(
    key: String,
    crossinline onHandle: suspend (T) -> Any?
) {
    CommunicationManager.instance.getMessage(key)
        .receiveAsFlow()
        .collect { request ->
            val actualValue = request.value
            if (actualValue is T) {
                tryResult {
                    val result = onHandle(actualValue)
                    request.resultDeferred.complete(result)
                }.onFailure {
                    request.resultDeferred.completeExceptionally(it)
                }
            } else {
                request.resultDeferred.cancel()
            }
        }
}

fun <T> sendWeakMessage(
    key: String,
    value: T,
): Response {
    return sendMessage(key, value as? WeakReference<*> ?: WeakReference(value))
}

suspend inline fun <reified T> receiveWeakMessage(
    key: String,
    crossinline onHandle: suspend (T) -> Any?
) {
    receiveMessage<WeakReference<T>>(key) { weakReference ->
        weakReference.get()?.let {
            val result = onHandle(it)
            result as? WeakReference<*> ?: WeakReference(result)
        }
    }
}