package com.module.app.ui

import android.content.Context
import android.os.Build
import android.provider.Settings
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.TimeZone

object DeviceInfoUtils {

    /**
     * 构建完整的设备信息 JSON
     */
    fun getDevicesInfo(
        context: Context,
        googleAdId: String? = "",
        oldGoogleAdId: String? = "",
        androidId: String? = "",
        oldAndroidId: String? = "",
        longitude: Double = 0.0,
        latitude: Double = 0.0
    ): JSONObject {


        return JSONObject().apply {
            put("googleAdId", googleAdId ?: "")
            put("oldGoogleAdId", oldGoogleAdId ?: "")
            put("androidId", androidId)
            put("oldAndroidId", oldAndroidId)
            put("longitude", longitude)
            put("latitude", latitude)
            // 组装硬件及环境风险信息
            put("deviceInfo", getBasicDeviceInfo(context))
        }
    }

    /**
     * 获取详细的硬件参数和风险检测项
     */
    private fun getBasicDeviceInfo(context: Context): JSONObject {
        val metrics = context.resources.displayMetrics

        return JSONObject().apply {
            // 1. 基本硬件与系统信息
            put("brand", Build.BRAND)                         // 品牌 (如 Xiaomi, Huawei)
            put("model", Build.MODEL)                         // 型号 (如 M2012K11C)
            put("manufacturer", Build.MANUFACTURER)           // 制造商 (如 Xiaomi)
            put("device", Build.DEVICE)                       // 设备名
            put("product", Build.PRODUCT)                     // 产品名
            put("hardware", Build.HARDWARE)                   // 硬件名
            put("fingerprint", Build.FINGERPRINT)             // 构建指纹
            put("osVersion", Build.VERSION.RELEASE)           // Android 系统版本 (如 13)
            put("sdkInt", Build.VERSION.SDK_INT)              // API 级别 (如 33)
            put("board", Build.BOARD)                         // 主板名称

            // 2. 屏幕与环境参数
            put("screenWidth", metrics.widthPixels)           // 屏幕像素宽
            put("screenHeight", metrics.heightPixels)         // 屏幕像素高
            put("densityDpi", metrics.densityDpi)             // 屏幕 DPI
            put("language", Locale.getDefault().language)     // 系统语言 (如 zh, en)
            put("country", Locale.getDefault().country)       // 国家/地区 (如 CN, US)
            put("timeZone", TimeZone.getDefault().id)         // 时区 (如 Asia/Shanghai)

            // 3. 风控与模拟器检测项
            put("isEmulator", isEmulator())                   // 是否为模拟器
            put("isRooted", isRooted())                       // 是否 Root
            put("isAdbEnabled", isAdbEnabled(context))        // 是否开启了 ADB 调试
        }
    }

    /**
     * 综合检测是否为模拟器 (针对 Android SDK 模拟器、夜神、雷电、逍遥、网易 MuMu 等)
     */
    fun isEmulator(): Boolean {
        // 1. 检查特征指纹和硬件关键字
        val isGeneric = Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT

        // 2. 检查 CPU/主板硬件特征 (金鱼、Ranchu、VBox、Nox 等)
        val hardware = Build.HARDWARE.lowercase(Locale.ROOT)
        val board = Build.BOARD.lowercase(Locale.ROOT)
        val product = Build.PRODUCT.lowercase(Locale.ROOT)

        val isHardwareEmu = hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("vbox86")
                || board.contains("nox")
                || hardware.contains("nox")
                || product.contains("nox")
                || product.contains("vbox86p")
                || product.contains("sdk_x86")

        // 3. 检查模拟器特有的驱动文件/管道文件
        val emulatorFiles = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/bin/qemu-props",
            "/fstab.nox",
            "/init.nox.rc",
            "/system/app/LDPlayer",
            "/dev/vboxguest"
        )
        val hasEmuFiles = emulatorFiles.any { File(it).exists() }

        return isGeneric || isHardwareEmu || hasEmuFiles
    }

    /**
     * 检查设备是否 Root
     */
    private fun isRooted(): Boolean {
        // 检查是否有 test-keys 标签
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // 检查常见的 su 二进制文件路径
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }

    /**
     * 检查是否开启了开发者 ADB 调试模式
     */
    private fun isAdbEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }
}