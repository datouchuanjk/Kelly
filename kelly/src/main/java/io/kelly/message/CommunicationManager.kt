package io.kelly.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.kelly.owner.ScopedOwner
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap

@PublishedApi
internal class CommunicationManager : ViewModel() {

    companion object {
        val instance = ViewModelProvider(ScopedOwner())[CommunicationManager::class.java]
    }

    private val channels = ConcurrentHashMap<String, Channel<Request>>()
    private val sharedFlows = ConcurrentHashMap<String, MutableSharedFlow<Any>>()

    @PublishedApi
    internal fun getMessage(key: String): Channel<Request> {
        return channels.getOrPut(key) {
            Channel(
                capacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            ) { undelivered ->
                undelivered.resultDeferred.cancel()
            }
        }
    }

    @PublishedApi
    internal fun getEvent(key: String): MutableSharedFlow<Any> {
        return sharedFlows.getOrPut(key) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        channels.values.forEach { it.close() }
        channels.clear()
        sharedFlows.clear()
    }
}