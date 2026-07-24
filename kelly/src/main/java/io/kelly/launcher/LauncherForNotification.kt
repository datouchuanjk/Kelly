package io.kelly.launcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.app.NotificationManagerCompat
import io.kelly.KellyInternal
import io.kelly.preference.boolPreference
import io.kelly.extension.appPackageName
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


@Composable
fun rememberLauncherForNotification(
    channelId: String? = null,
    postOnly: Boolean = false,
    onResult: (Boolean) -> Unit
): ActivityResultLauncher<Unit> {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        NotificationPermissionContract(channelId, postOnly)
    ) {
        currentOnResult(it)
    }
    return launcher
}

internal class NotificationPermissionContract(
    private val channelId: String? = null,
    private val postOnly: Boolean = false
) : ActivityResultContract<Unit, Boolean>() {

    companion object {
        private var KEY_REQUESTED_POST_NOTIFICATIONS by boolPreference(commit = true)
    }

    val areChannelEnabled by notificationPermission(channelId)
    val areNotificationsEnabled by notificationPermission()

    override fun createIntent(context: Context, input: Unit): Intent {
        val shouldAskSystemPermission = if (postOnly) {
            true
        } else {
            !KEY_REQUESTED_POST_NOTIFICATIONS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && shouldAskSystemPermission
        ) {
            KEY_REQUESTED_POST_NOTIFICATIONS = true
            return ActivityResultContracts.RequestPermission()
                .createIntent(context, Manifest.permission.POST_NOTIFICATIONS)
        }
        return Intent().apply {
            if (areNotificationsEnabled) {
                action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, appPackageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            } else {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, appPackageName)
            }
        }
    }

    override fun getSynchronousResult(
        context: Context,
        input: Unit
    ): SynchronousResult<Boolean>? {
        if (areChannelEnabled) return SynchronousResult(true)
        if (postOnly) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || KEY_REQUESTED_POST_NOTIFICATIONS
            ) {
                return SynchronousResult(false)
            }
        }
        return null
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return areChannelEnabled
    }
}

internal fun notificationPermission(
    channelId: String? = null
) = NotificationPermissionProperty(channelId)

internal class NotificationPermissionProperty(
    private val channelId: String?
) : ReadOnlyProperty<Any?, Boolean> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        val context = KellyInternal.context
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (channelId.isNullOrEmpty()) {
            return true
        }
        val channel = manager.getNotificationChannel(channelId)
        return channel != null && channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }
}