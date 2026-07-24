package io.kelly.extension

import android.app.Service
import android.content.Intent
import io.kelly.KellyInternal

inline fun <reified T : Service> startForegroundService(
    intentAction: (Intent) -> Unit = { },
) {
    if (!isAppInForeground ) {
        throw RuntimeException()
    }
    val intent = Intent(KellyInternal.context, T::class.java).also(intentAction)
    KellyInternal.context.startForegroundService(intent)
}

inline fun <reified T : Service> stopService() {
    val intent = Intent(KellyInternal.context, T::class.java)
    KellyInternal.context.stopService(intent)
}

