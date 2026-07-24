package io.kelly.compile.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator

inline fun <reified T : Any> NavBackStackEntry.checkRoute(
    predicate: (T) -> Boolean = { true }
): Boolean {
    return destination.hasRoute<T>()
            && (toRouteOrNull<T>()?.let { predicate(it) } ?: false)
}

val NavBackStackEntry.deepLinked: String?
    get() {
        var linked = savedStateHandle.get<String?>(NavigationConstant.NAV_DEEP_LINKED)
        destination.arguments.keys.forEach { key ->
            val oldValue = "{${key}}"
            val newValue = "${savedStateHandle.get<Any?>(key)}"
            linked = linked?.replace(oldValue, newValue)
        }
        return linked
    }

fun NavBackStackEntry.onResume(block: () -> Unit) {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        block()
        return
    }
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_RESUME) {
                block()
                source.lifecycle.removeObserver(this)
            }
        }
    })
}

fun NavBackStackEntry.onDestroyed(block: () -> Unit) {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.DESTROYED)) {
        block()
        return
    }
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                block()
                source.lifecycle.removeObserver(this)
            }
        }
    })
}

fun NavBackStackEntry.isDialogNavigator(): Boolean {
    return destination is DialogNavigator.Destination
}

fun NavBackStackEntry.isComposeNavigator(): Boolean {
    return destination is ComposeNavigator.Destination
}

