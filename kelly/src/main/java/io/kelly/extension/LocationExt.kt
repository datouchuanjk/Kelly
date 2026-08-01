package io.kelly.extension

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import io.kelly.KellyInternal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

private val LocationManager.isLocationEnabledCompat: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        isLocationEnabled
    } else {
        isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

private val LocationManager.cachedLocation: Location?
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    get() = getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

private val locationEnabledCallbackFlow: Flow<Boolean>
    get() = callbackFlow {
        val context = KellyInternal.context
        val service = context.getSystemService<LocationManager>()
        trySend(service?.isLocationEnabledCompat ?: false)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    trySend(service?.isLocationEnabledCompat ?: false)
                }
            }
        }
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(receiver, filter)
        awaitClose {
            tryResult {
                context.unregisterReceiver(receiver)
            }
        }
    }


@OptIn(ExperimentalCoroutinesApi::class)
@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
fun locationCallbackFlow(
    includeLastKnown: Boolean = true,
    minTimeMs: Long = 2000L,
    minDistanceM: Float = 0f,
    provider: String = LocationManager.GPS_PROVIDER,
    onDisabled: (() -> Unit)? = null
): Flow<Location> = locationEnabledCallbackFlow
    .onEach { isEnabled ->
        if (!isEnabled) {
            onDisabled?.invoke()
        }
    }
    .filter { it }
    .flatMapLatest {
        val service = KellyInternal.context.getSystemService<LocationManager>()
        callbackFlow {
            if (includeLastKnown) {
                service?.cachedLocation?.let { trySend(it) }
            }
            val listener = LocationListener { location ->
                trySend(location)
            }
            tryResult {
                service?.requestLocationUpdates(
                    provider,
                    minTimeMs,
                    minDistanceM,
                    listener,
                    Looper.getMainLooper()
                )
            }.onFailure { e ->
                close(e)
            }
            awaitClose {
                service?.removeUpdates(listener)
            }
        }
    }