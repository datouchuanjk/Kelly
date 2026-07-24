package io.kelly.extension

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteInput
import io.kelly.KellyInternal
import io.kelly.launcher.notificationPermission
import kotlin.random.Random

fun notificationOf(
    channelId: String,
    identifier: Int,
    block: NotificationDsl.() -> Unit
): Notification {
    val context = KellyInternal.context
    val builder = NotificationCompat.Builder(context, channelId)
    val dsl = NotificationDsl(identifier, builder, context)
    dsl.block()
    return builder.build()
}

fun interface CancellationNotification {
    fun cancel()
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun notify(
    channelId: String,
    id: Int = Random.nextInt(),
    block: NotificationDsl.() -> Unit
): CancellationNotification? {
    val context = KellyInternal.context
    val isEnabled by notificationPermission(channelId)
    if (!isEnabled) return null
    val notification = notificationOf(channelId, id, block)
    NotificationManagerCompat.from(context).notify(id, notification)
    return CancellationNotification {
        NotificationManagerCompat.from(KellyInternal.context).cancel(id)
    }
}

fun createNotificationChannel(
    channelId: String,
    name: String = channelId,
    importance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT,
    block: (NotificationChannelCompat.Builder.() -> Unit)? = null
) {
    val manager = NotificationManagerCompat.from(KellyInternal.context)
    val builder = NotificationChannelCompat.Builder(channelId, importance)
        .setName(name)
    block?.invoke(builder)
    manager.createNotificationChannel(builder.build())
}

class NotificationDsl internal constructor(
    @PublishedApi
    internal val identifier: Int,
    @PublishedApi
    internal val builder: NotificationCompat.Builder,
    @PublishedApi
    internal val context: Context
) {
    init {
        builder.setSmallIcon(context.applicationInfo.icon)
    }

    var title: CharSequence?
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setContentTitle(value)
        }

    var text: CharSequence?
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setContentText(value)
        }

    var smallIcon: Int
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(@DrawableRes value) {
            builder.setSmallIcon(value)
        }

    var bigText: CharSequence?
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(value))
        }

    var bigPicture: Bitmap?
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(value))
        }

    var autoCancel: Boolean
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setAutoCancel(value)
        }

    var priority: Int
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setPriority(value)
        }

    var silent: Boolean
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setSilent(value)
        }

    var ongoing: Boolean
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setOngoing(value)
        }

    var showWhen: Boolean
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setShowWhen(value)
        }

    var onlyAlertOnce: Boolean
        @Deprecated("Write-only property", level = DeprecationLevel.ERROR)
        get() = throw UnsupportedOperationException()
        set(value) {
            builder.setOnlyAlertOnce(value)
        }

    fun progress(max: Int, progress: Int, indeterminate: Boolean = false) {
        builder.setProgress(max, progress, indeterminate)
    }

    inline fun <reified T> addAction(
        title: String,
        @DrawableRes icon: Int = 0,
        intentAction: (Intent) -> Intent = { it }
    ) {
        val pendingIntent = getPendingIntent<T>(
            context,
            title.hashCode() + identifier.hashCode(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            intentAction
        )
        builder.addAction(icon, title, pendingIntent)
    }

    inline fun <reified T : BroadcastReceiver> addInput(
        title: String,
        @DrawableRes icon: Int = 0,
        resultKey: String,
        label: String,
        intentAction: (Intent) -> Intent = { it }
    ) {
        val pendingIntent = getPendingIntent<T>(
            context,
            title.hashCode() + identifier.hashCode(),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            intentAction
        )
        val action = NotificationCompat.Action.Builder(icon, title, pendingIntent)
            .addRemoteInput(
                RemoteInput.Builder(resultKey)
                    .setLabel(label)
                    .build()
            ).build()
        builder.addAction(action)
    }

    fun clickToLaunchApp(intentAction: (Intent) -> Intent = { it }) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.let { intentAction(it) } ?: return
        val pendingIntent = PendingIntent.getActivity(
            context,
            identifier.hashCode(),
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(pendingIntent)
    }

    inline fun <reified T : Activity> onClick(intentAction: (Intent) -> Intent = { it }) {
        val pendingIntent = getPendingIntent<T>(
            context,
            identifier.hashCode(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            intentAction
        )
        builder.setContentIntent(pendingIntent)
    }

    fun raw(block: NotificationCompat.Builder.() -> Unit) {
        builder.block()
    }
}

@PublishedApi
internal inline fun <reified T> getPendingIntent(
    context: Context,
    requestCode: Int,
    @PendingIntentCompat.Flags flags: Int,
    intentAction: (Intent) -> Intent
): PendingIntent {
    val intent = Intent(context, T::class.java).let(intentAction)
        .setPackage(context.packageName)
    return when {
        Activity::class.java.isAssignableFrom(T::class.java) -> {
            PendingIntent.getActivity(context, requestCode, intent, flags)
        }

        Service::class.java.isAssignableFrom(T::class.java) -> {
            PendingIntent.getService(context, requestCode, intent, flags)
        }

        BroadcastReceiver::class.java.isAssignableFrom(T::class.java) -> {
            PendingIntent.getBroadcast(context, requestCode, intent, flags)
        }

        else -> throw IllegalArgumentException(
            "Unsupported target component: ${T::class.java.canonicalName}. " +
                    "It must be an Activity, Service, or BroadcastReceiver."
        )
    }
}
