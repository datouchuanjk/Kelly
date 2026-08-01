package com.module.app.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import io.kelly.app
import io.kelly.extension.locationCallbackFlow
import io.kelly.extension.suspendResult
import io.kelly.extension.toJsonArrayOrNull
import io.kelly.extension.toJsonObjectOrNull
import io.kelly.extension.toURLOrNull
import io.kelly.extension.toast
import io.kelly.extension.versionCode
import io.kelly.flow.effectProperty
import io.kelly.preference.boolPreference
import io.kelly.preference.intPreference
import io.kelly.preference.stringPreference
import io.kelly.source.asSource
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.json.JSONObject

@SuppressLint("AdvertisingIdPolicy")
class HostViewModel : ViewModel() {

    private var cacheverPre by intPreference()
    private val urls = mutableListOf<String>()
    private val _urlFlow = effectProperty<String>(1) //其实是channle的封装
    val urlFlow by _urlFlow

    private val _clearCacheFlow = effectProperty<Unit>(1)
    val clearCacheFlow by _clearCacheFlow

    private val _updateAppFlow = effectProperty<Update>(1)
    val updateAppFlow by _updateAppFlow

    init {
        viewModelScope.launch {
            suspendResult {
                parseUrl()
            }
        }
    }

    @Keep
    data class Update(
        val updateInfo: String,
        val apkUrl: String,
        val forceUpdate: Int,
    )

    var isFirstLaunchApp by boolPreference(default = true)
    var getGPS = 0

    /**
     * 解析远程地址，开始加载
     */
    private fun parseUrl() {
//        val source = "https://static.in77club.com/static/api.json".toURLOrNull()?.asSource() //测试地址。
        val source = "https://static.in77club.com/static/api-prod.json".toURLOrNull()?.asSource()//正式地址。
        val text = source?.originInputStream()
            ?.bufferedReader()
            ?.readText()
            ?.toJsonObjectOrNull()
            ?: return
        getGPS = text.optInt("getGPS")
        val appver = text.optInt("appver")
        if (app.versionCode < appver) {
            //需要更新
            val updateinfo = text.optString("updateinfo")
            val apkurl = text.optString("apkurl")
            val forceupdate = text.optInt("forceupdate")
            _updateAppFlow.trySend(
                Update(
                    updateInfo = updateinfo,
                    apkUrl = apkurl,
                    forceUpdate = forceupdate,
                )
            )
            if (forceupdate == 1) {
                return //强制升级才return
            }
        }
        val cachever = text.optInt("cachever")
        if (cacheverPre < cachever) {
            //需要清理webview的缓存
            _clearCacheFlow.trySend(Unit)
            cacheverPre = cachever
        }
        val servers = text.optJSONArray("servers") ?: return
        repeat(servers.length()) {
            urls.add(servers.getJSONObject(it).optString("url"))
        }
        if (urls.isNotEmpty()) {
            val url = urls[0]
            urls.remove(url)
            _urlFlow.trySend(url)
            return
        }
    }

    fun getNextUrl() {
        if (urls.isNotEmpty()) {
            val url = urls[0]
            urls.remove(url)
            _urlFlow.trySend(url)
            return
        } else {
            "load failed".toast()
        }
    }

    private var longitude: Double = 0.0
    private var latitude: Double = 0.0


    @SuppressLint("MissingPermission")
    fun saveLocation() {
        viewModelScope.launch {
            locationCallbackFlow(
                onDisabled = {
                    "Location services are disabled. Please enable them in System Settings.".toast()
                }
            ).take(1).collect { location ->
                latitude = location.latitude
                longitude = location.longitude
                Log.e("1234", "收到位置: ${location.latitude}, ${location.longitude}")
            }
        }

    }

    private var androidIdPre by stringPreference()
    private var googleAdIdPre by stringPreference()

    val oldAndroidId = androidIdPre

    val oldGoogleAdId = googleAdIdPre

    @SuppressLint("HardwareIds")
    val androidId: String? = Settings.Secure.getString(
        app.contentResolver,
        Settings.Secure.ANDROID_ID
    ).apply {
        androidIdPre = this.orEmpty()
    }

    var googleAdId: String? = null

    init {
        viewModelScope.launch {
            suspendResult {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(app)
                googleAdId = adInfo.id
                googleAdIdPre = adInfo.id.orEmpty()
            }
        }
    }

    fun getDevicesInfo(): String {
        return DeviceInfoUtils.getDevicesInfo(
            context = app,
            googleAdId = googleAdId,
            oldGoogleAdId = oldGoogleAdId,
            androidId = androidId,
            oldAndroidId = oldAndroidId,
            longitude = longitude,
            latitude = latitude
        ).toString()
    }
}