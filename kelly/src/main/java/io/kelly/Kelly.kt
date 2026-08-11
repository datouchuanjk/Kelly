package io.kelly

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
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
    val activities = mutableListOf<WeakReference<Activity>>()
    var isDebug: Boolean = false
}

val isProcessOnly get() =topActivityOrNull== null
        && activities.isEmpty()
val topActivityOrNull get() = KellyInternal.topActivity?.get()
val activities get() = KellyInternal.activities.mapNotNull { it.get() }