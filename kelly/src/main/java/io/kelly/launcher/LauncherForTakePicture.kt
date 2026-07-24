package io.kelly.launcher

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.kelly.extension.toContentUri
import java.io.File

@Composable
fun rememberLauncherForTakePicture(
    handlePermanentlyDenied: Boolean = true,
    onResult: (File?) -> Unit,
): ActivityResultLauncher<Unit> {
    val currentOnResult by rememberUpdatedState(onResult)
    var takeFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    val localContext = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) {
        if (it && !takeFilePath.isNullOrEmpty()) {
            val file = File(takeFilePath.orEmpty())
            if (file.exists() && file.length() > 0) {
                currentOnResult(file)
                takeFilePath = null
            } else {
                currentOnResult(null)
            }
        } else {
            takeFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                takeFilePath = null
            }
            currentOnResult(null)
        }
    }
    return rememberLauncherForRequestPermission(
        Manifest.permission.CAMERA,
        handlePermanentlyDenied = handlePermanentlyDenied
    ) {
        if (it.isAllGranted) {
            val file = File(localContext.cacheDir, "${System.nanoTime()}.PNG")
            takeFilePath = file.absolutePath
            launcher.launch(file.toContentUri())
        }else{
            currentOnResult(null)
        }
    }
}
