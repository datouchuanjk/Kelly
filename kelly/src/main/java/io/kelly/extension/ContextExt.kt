package io.kelly.extension

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import io.kelly.KellyInternal

@PublishedApi
internal val appLabel: String by lazy {
    KellyInternal.context.label
}

@PublishedApi
internal val appPackageName: String by lazy {
    KellyInternal.context.packageName
}

val Context.label: String
    get() = applicationInfo.loadLabel(KellyInternal.context.packageManager).toString()

val Context.versionCode: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfoCompat.longVersionCode
    } else {
        packageInfoCompat.versionCode.toLong()
    }

val Context.versionName: String
    get() = packageInfoCompat.versionName.orEmpty()

private val Context.packageInfoCompat: PackageInfo
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        packageManager.getPackageInfo(KellyInternal.context.packageName, 0)
    }




