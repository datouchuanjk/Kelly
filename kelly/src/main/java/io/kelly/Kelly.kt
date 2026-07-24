package io.kelly

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import io.kelly.extension.isAppInBackground
import io.kelly.extension.isAppInForeground
import java.lang.ref.WeakReference

object Kelly {
    private var isInitialized = false
    fun init(context: Context, isDebug: Boolean = true) {
        if (isInitialized) return
        val application = context.applicationContext as Application
        KellyInternal.context = application
        KellyInternal.isDebug = isDebug
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                WeakReference(activity).let {
                    KellyInternal.topActivity = it
                    KellyInternal.activities.add(it)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                KellyInternal.topActivity = WeakReference(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (KellyInternal.topActivity?.get() === activity) {
                    KellyInternal.topActivity = null
                }
                KellyInternal.activities.removeIf { it.get() == activity }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
        isInitialized = true
    }
}

@PublishedApi
internal object KellyInternal {
    lateinit var context: Application
    var topActivity: WeakReference<Activity>? = null
    internal val activities = mutableListOf<WeakReference<Activity>>()
    var isDebug: Boolean = false
}

val isProcessOnly
    get() = KellyInternal.topActivity?.get() == null
            && KellyInternal.activities.isEmpty()
            && isAppInBackground

val app get() = KellyInternal.context

val topActivityOrNull get() = KellyInternal.topActivity?.get()

val activities get() = KellyInternal.activities.map { it.get() }
