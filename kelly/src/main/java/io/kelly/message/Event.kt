package io.kelly.message

fun <T> sendEvent(
    key: String,
    value: T,
) {
    val sharedFlow = CommunicationManager.instance.getEvent(key)
    sharedFlow.tryEmit(value as Any)
}

suspend inline fun <reified T> receiveEvent(
    key: String,
    crossinline onHandle: suspend (T) -> Unit
) {
    CommunicationManager.instance.getEvent(key)
        .collect { request ->
            if (request is T) {
                onHandle(request)
            }
        }
}
