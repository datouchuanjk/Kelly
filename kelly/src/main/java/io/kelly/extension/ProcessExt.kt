package io.kelly.extension

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Duration.Companion.milliseconds

val isAppInForeground: Boolean
    get() = ProcessLifecycleOwner.get()
        .lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

val isAppInBackground: Boolean
    get() = !isAppInForeground

val backToForegroundCallbackFlow
    get() = callbackFlow {
        val observer = object : DefaultLifecycleObserver {
            private var count = if (isAppInForeground) 0 else 2
            private var lastTime: Long = if (count == 2) System.currentTimeMillis() else 0L
            override fun onStart(owner: LifecycleOwner) {
                if (count == 2) {
                    val currentTimeMillis = System.currentTimeMillis()
                    trySend((currentTimeMillis - lastTime).milliseconds)
                    lastTime = 0L
                    count = 0
                } else {
                    count = 1
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                count = 2
                lastTime = System.currentTimeMillis()
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        awaitClose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }
