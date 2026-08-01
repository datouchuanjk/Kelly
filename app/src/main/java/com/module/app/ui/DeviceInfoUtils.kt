package com.module.app.ui

import android.content.Context
import android.os.Build
import android.provider.Settings
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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
            put("androidId", androidId ?: "")
            put("oldAndroidId", oldAndroidId ?: "")
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
            put("abis", Build.SUPPORTED_ABIS.joinToString(","))
            put("cpuRead", getCpuInfo())

            // 模拟器驱动/节点存在性检测
            put("qemud_file", File("/dev/socket/qemud").exists().toString())
            put("qemu_pipe_file", File("/dev/qemu_pipe").exists().toString())
            put("qemu_trace_file", File("/dev/qemu_trace").exists().toString())
            put("libc_malloc_debug_qemu_file", File("/system/lib/libc_malloc_debug_qemu.so").exists().toString())
            put("qemu_trace_file_sys", File("/sys/qemu_trace").exists().toString())
            put("qemu-props_file", File("/system/bin/qemu-props").exists().toString())
        }
    }

    fun isEmulator(): Boolean {
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        val device = Build.DEVICE.lowercase(Locale.ROOT)
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.ROOT)
        val hardware = Build.HARDWARE.lowercase(Locale.ROOT)
        val model = Build.MODEL.lowercase(Locale.ROOT)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val product = Build.PRODUCT.lowercase(Locale.ROOT)
        val board = Build.BOARD.lowercase(Locale.ROOT)

        val isGeneric = fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for x86")
                || manufacturer.contains("genymotion")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || "google_sdk" == product
                || model.contains("droid4x")
                || product.contains("sdk_gphone") // 新版 Android Studio 官方模拟器

        // 2. 检查 CPU/主板/产品特征 (新增了雷电、MuMu、逍遥、蓝叠等国内主流模拟器关键字)
        val isHardwareEmu = hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("vbox86")
                || hardware.contains("nox")
                || hardware.contains("ttvm")
                || board.contains("nox")
                || board.contains("vbox86")
                || product.contains("nox")
                || product.contains("vbox86p")
                || product.contains("sdk_x86")
                || product.contains("sdk_gphone")
                || product.contains("dnplayer")   // 雷电模拟器
                || product.contains("ldplayer")   // 雷电模拟器
                || product.contains("mumu")       // 网易 MuMu
                || product.contains("nemu")       // 网易 MuMu
                || product.contains("microvirt")  // 逍遥模拟器
                || product.contains("memu")       // 逍遥模拟器
                || product.contains("bluestacks") // 蓝叠模拟器
                || brand.contains("bluestacks")

        if (isGeneric || isHardwareEmu) return true

        // 3. 检查 /proc/cpuinfo (核心补充：利用 PC 端 CPU 信息的特征检测)
        val cpuInfo = getCpuInfo()
        if (cpuInfo.contains("intel") ||
            cpuInfo.contains("amd") ||
            cpuInfo.contains("qemu") ||
            cpuInfo.contains("goldfish") ||
            cpuInfo.contains("ranchu") ||
            cpuInfo.contains("hypervisor") ||
            cpuInfo.contains("vbox")
        ) {
            // 排除含有移动端 SoC 厂商字样的真机，防止误判某些特殊芯片
            val isArmArmSoC = cpuInfo.contains("qualcomm") ||
                    cpuInfo.contains("exynos") ||
                    cpuInfo.contains("kirin") ||
                    cpuInfo.contains("mediatek")
            if (!isArmArmSoC) return true
        }

        // 4. 检查架构 ABI (如果全是 x86/x86_64 且无任何 arm 架构，极大可能是 PC 模拟器)
        val abis = Build.SUPPORTED_ABIS.joinToString(",").lowercase(Locale.ROOT)
        if (abis.contains("x86") && !abis.contains("arm")) {
            return true
        }

        // 5. 检查模拟器特有的驱动/节点/文件 (补充了完整 QEMU 节点及各大模拟器文件)
        val emulatorFiles = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/dev/qemu_trace",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/system/lib64/libc_malloc_debug_qemu.so",
            "/dev/vboxguest",
            "/dev/vboxuser",
            "/fstab.nox",
            "/init.nox.rc",
            "/ueventd.nox.rc",
            "/fstab.mumu",
            "/system/app/LDPlayer",
            "/system/bin/droid4x",
            "/fstab.andy"
        )
        if (emulatorFiles.any { File(it).exists() }) {
            return true
        }

        return false
    }

    /**
     * 读取 /proc/cpuinfo 内容
     */
    private fun getCpuInfo(): String {
        return try {
            val process = ProcessBuilder("/system/bin/cat", "/proc/cpuinfo").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val sb = StringBuilder()
            reader.useLines { lines ->
                lines.forEach { sb.append(it) }
            }
            sb.toString().lowercase(Locale.ROOT)
        } catch (_: Exception) {
            ""
        }
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