package io.kelly.launcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.Companion.ACTION_REQUEST_PERMISSIONS
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.Companion.EXTRA_PERMISSIONS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import io.kelly.KellyInternal
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Composable
fun rememberLauncherForExternalStorage(
    useSaf: Boolean = true,
    requireReadImages: Boolean = false,
    requireReadVideo: Boolean = false,
    requireReadAudio: Boolean = false,
    requireWrite: Boolean = false,
    handlePermanentlyDenied: Boolean = true,
    onResult: (Boolean) -> Unit
): ActivityResultLauncher<Unit> {
    val currentOnResult by rememberUpdatedState(onResult)
    val externalStoragePermissions = remember(
        useSaf,
        requireReadImages,
        requireReadVideo,
        requireReadAudio,
        requireWrite
    ) {
        getExternalStorageRequestPermission(
            useSaf,
            requireReadImages,
            requireReadVideo,
            requireReadAudio,
            requireWrite
        )
    }
    return rememberLauncherForRequestPermission(
        *externalStoragePermissions,
        handlePermanentlyDenied = handlePermanentlyDenied
    ) {
        currentOnResult(it.isAllGranted)
    }
}

private fun getExternalStorageRequestPermission(
    useSaf: Boolean = true,
    requireReadImages: Boolean = false,
    requireReadVideo: Boolean = false,
    requireReadAudio: Boolean = false,
    requireWrite: Boolean = false
): Array<String> {
    if (useSaf && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return emptyArray<String>()
    }
    val requestPermissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (requireReadImages) {
            requestPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }
        if (requireReadVideo) {
            requestPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        }
        if (requireReadAudio) {
            requestPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
    } else if (requireReadImages || requireReadVideo || requireReadAudio) {
        requestPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    if (requireWrite && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        requestPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    return requestPermissions.toTypedArray()
}