package io.kelly.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.kelly.KellyInternal
import io.kelly.extension.appPackageName
import io.kelly.extension.tryResult
import io.kelly.remember.display.DisplayController
import io.kelly.remember.display.content
import io.kelly.remember.display.rememberDisplayController

val List<PermissionState>.isAllGranted get() = all { it.isGranted }
val List<PermissionState>.permanentlyDeniedPermissions get() = filter { it.isPermanentlyDenied }

class PermissionState (val name: String) {
    val isGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            KellyInternal.context,
            name
        ) == PackageManager.PERMISSION_GRANTED

    val isPermanentlyDenied: Boolean
        get() = !isGranted && (KellyInternal.topActivity?.get()?.let { activity ->
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, name)
        } ?: false)


    val label: String by lazy {
        tryResult {
            val packageManager = KellyInternal.context.packageManager
            val permissionInfo = packageManager.getPermissionInfo(name, 0)
            permissionInfo.loadLabel(packageManager).toString()
        }.getOrDefault(name)
    }
}

@Composable
fun rememberLauncherForRequestPermission(
    vararg permissions: String,
    handlePermanentlyDenied: Boolean = true,
    onResult: (List<PermissionState>) -> Unit
): ActivityResultLauncher<Unit> {
    val currentOnResult by rememberUpdatedState(onResult)
    val dialog = createJumpSettingDialog {
        permissions.map {
            PermissionState(it)
        }.let { results ->
            currentOnResult(results)
        }
    }
    return rememberLauncherForActivityResult(
        RequestPermissionsContract(permissions.map { it }.toTypedArray())
    ) { result ->
        result.map {
            PermissionState(it.key)
        }.let { results ->
            val allPermanentlyDenied = results.permanentlyDeniedPermissions
            if (allPermanentlyDenied.isNotEmpty() && handlePermanentlyDenied) {
                dialog.show(allPermanentlyDenied)
            } else {
                currentOnResult(results)
            }
        }
    }
}

private class RequestPermissionsContract(
    val permissions: Array<String>
) : ActivityResultContract<Unit, Map<String, Boolean>>() {

    private val delegate = ActivityResultContracts.RequestMultiplePermissions()

    override fun createIntent(context: Context, input: Unit) =
        delegate.createIntent(context, permissions)

    override fun getSynchronousResult(context: Context, input: Unit) =
        delegate.getSynchronousResult(context, permissions)

    override fun parseResult(resultCode: Int, intent: Intent?) =
        delegate.parseResult(resultCode, intent)
}

@Composable
private fun createJumpSettingDialog(
    onResult: () -> Unit
): DisplayController<List<PermissionState>> {
    val currentOnResult by rememberUpdatedState(onResult)
    val settingLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        currentOnResult()
    }
    return rememberDisplayController<List<PermissionState>>()
        .content { list ->
            AlertDialog(
                properties = DialogProperties(
                    dismissOnClickOutside = false
                ),
                onDismissRequest = {
                    currentOnResult()
                    dismiss()
                },
                title = {
                    Text("Tips")
                },
                text = {
                    val content = list.joinToString(",") { it.label }
                    Text("The following permissions have been permanently denied: $content. Please grant them in Settings.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", appPackageName, null)
                        }
                        settingLauncher.launch(intent)
                        dismiss()
                    }) {
                        Text("Go to Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        currentOnResult()
                        dismiss()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
}
