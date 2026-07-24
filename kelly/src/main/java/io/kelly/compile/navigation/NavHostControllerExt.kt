package io.kelly.compile.navigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.navOptions
import io.kelly.extension.appLabel
import io.kelly.extension.tryResult
import java.io.Serializable

fun NavHostController.safePopBackStack(
    onDestroyed: (() -> Unit)? = null
): Boolean {
    val currentEntry = currentBackStackEntry ?: return false
    previousBackStackEntry ?: return false
    val isStarted = currentEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    if (!isStarted) {
        return false
    }
    if (popBackStack()) {
        if (onDestroyed != null) {
            currentEntry.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        onDestroyed()
                        source.lifecycle.removeObserver(this)
                    }
                }
            })
        }
        return true
    }
    return false
}

fun <T : Any> NavHostController.navigateReplace(route: T) {
    val currentId = currentBackStackEntry?.destination?.id ?: return
    navigate(route) {
        popUpTo(currentId) {
            inclusive = true
        }
    }
}

fun <T : Any> NavHostController.navigateReset(route: T) {
    navigate(route) {
        popUpTo(graph.id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

inline fun <reified T : Any> NavHostController.navigateOrPopBack(
    route: T,
    predicate: (T) -> Boolean
) {
    val isPrevious = previousBackStackEntry?.checkRoute(predicate) ?: false
    if (isPrevious) {
        popBackStack()
    } else {
        navigate(route)
    }
}

fun NavHostController.navigateDeepLink(
    link: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    val uri = if (link.startsWith("${appLabel.lowercase()}://")) {
        link
    } else {
        "${appLabel.lowercase()}://${link}"
    }.toUri()
    navigate(uri, navOptions(builder))
}

@SuppressLint("RestrictedApi")
inline fun <reified T : Any> NavHostController.isCurrent(
    ignoreDialogNavigator: Boolean = true,
    predicate: (T) -> Boolean = { true }
): Boolean {
    val find = tryResult { getBackStackEntry<T>() }.getOrNull()
    if (find == null) {
        return false
    }
    return if (ignoreDialogNavigator) {
        if (find.isDialogNavigator()) {
            currentBackStackEntry?.checkRoute<T>(predicate) ?: false
        } else {
            currentBackStack.value
                .findLast { it.isComposeNavigator() }
                ?.checkRoute<T>(predicate)
                ?: false
        }
    } else {
        currentBackStackEntry?.checkRoute<T>(predicate) ?: false
    }
}

@SuppressLint("RestrictedApi")
inline fun <reified T : Any> NavHostController.filterBackStackEntry(
    predicate: (T) -> Boolean = { true }
): List<NavBackStackEntry> {
    return currentBackStack.value.filter { entry ->
        entry.checkRoute<T>(predicate)
    }
}

@JvmName("setResultSerializable")
fun <T : Serializable?> NavHostController.setResult(value: T) {
    previousBackStackEntry?.savedStateHandle?.set(NavigationConstant.NAV_RESULT, value)
}

@JvmName("setResultParcelable")
fun <T : Parcelable?> NavHostController.setResult(value: T) {
    previousBackStackEntry?.savedStateHandle?.set(NavigationConstant.NAV_RESULT, value)
}

fun NavHostController.navigateForPopBack(
    route: Any,
    onBack: (NavBackStackEntry) -> Unit
) {
    val sourceEntry = currentBackStackEntry ?: return
    val sourceId = sourceEntry.id

    var lifecycleObserver: LifecycleEventObserver? = null
    val listener = object : NavController.OnDestinationChangedListener {
        var hasNavigatedAway = false
        override fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        ) {
            val currentId = controller.currentBackStackEntry?.id
            if (currentId != sourceId) {
                hasNavigatedAway = true
            }
            if (hasNavigatedAway && currentId == sourceId) {
                onBack(sourceEntry)
                controller.removeOnDestinationChangedListener(this)
                lifecycleObserver?.let { sourceEntry.lifecycle.removeObserver(it) }
            }
        }
    }
    lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            removeOnDestinationChangedListener(listener)
        }
    }
    sourceEntry.lifecycle.addObserver(lifecycleObserver)
    addOnDestinationChangedListener(listener)
    navigate(route)
}

inline fun <reified T> NavHostController.navigateForResult(
    route: Any,
    crossinline onResult: (T) -> Unit
) = navigateForPopBack(route) { currentEntry ->
    if (currentEntry.savedStateHandle.contains("NAV_RESULT")) {
        val result = currentEntry.savedStateHandle.get<Any>("NAV_RESULT")
        if (result is T) {
            onResult(result)
        }
        currentEntry.savedStateHandle.remove<T>("NAV_RESULT")
    }
}


