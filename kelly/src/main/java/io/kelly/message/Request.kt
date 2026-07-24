package io.kelly.message

import kotlinx.coroutines.CompletableDeferred

@PublishedApi
internal class Request(
    val resultDeferred: CompletableDeferred<Any?>,
    val value: Any?,
)