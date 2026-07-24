package io.kelly.extension

import android.Manifest
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import io.kelly.KellyInternal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

val currentNetworkCapabilities: NetworkCapabilities?
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    get() = KellyInternal.context.getSystemService<ConnectivityManager>()?.let { manager ->
        manager.getNetworkCapabilities(manager.activeNetwork)
    }

val networkCapabilitiesCallbackFlow
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    get() = callbackFlow {
        val connectivityManager = KellyInternal.context.getSystemService<ConnectivityManager>()
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)

            override fun onLost(network: Network) {
                trySend(null)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                trySend(capabilities)
            }
        }
        connectivityManager?.registerDefaultNetworkCallback( networkCallback)
        val activeNetwork = connectivityManager?.activeNetwork
        if (activeNetwork != null) {
            trySend(connectivityManager.getNetworkCapabilities(activeNetwork))
        } else {
            trySend(null)
        }
        awaitClose {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        }
    }

val NetworkCapabilities?.isWifi: Boolean
    get() = this?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false

val NetworkCapabilities?.isCellular: Boolean
    get() = this?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false

val NetworkCapabilities?.isVpn: Boolean
    get() = this?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false

val NetworkCapabilities?.hasValidated: Boolean
    get() = (this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false) &&
            (this.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))

