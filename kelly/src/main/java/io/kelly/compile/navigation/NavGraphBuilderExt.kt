package io.kelly.compile.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import io.kelly.extension.appLabel
import io.kelly.extension.tryResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KType

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> NavGraphBuilder.composableWithDeepLinked(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    noinline enterTransition:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    EnterTransition?)? =
        null,
    noinline exitTransition:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    ExitTransition?)? =
        null,
    noinline popEnterTransition:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    EnterTransition?)? =
        enterTransition,
    noinline popExitTransition:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    ExitTransition?)? =
        exitTransition,
    noinline sizeTransform:
    (AnimatedContentTransitionScope<NavBackStackEntry>.() -> @JvmSuppressWildcards
    SizeTransform?)? =
        null,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    val withDeepLinked = withDeepLinked<T>()
    composable<T>(
        typeMap,
        deepLinks + withDeepLinked,
        enterTransition,
        exitTransition,
        popEnterTransition,
        popExitTransition,
        sizeTransform,
    ) {
        it.savedStateHandle[NavigationConstant.NAV_DEEP_LINKED] =
            withDeepLinked.getOrNull(0)?.uriPattern
        content(it)
    }
}

inline fun <reified T : Any> NavGraphBuilder.dialogWithDeepLinked(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    dialogProperties: DialogProperties = DialogProperties(),
    noinline content: @Composable (NavBackStackEntry) -> Unit
) {
    val withDeepLinked = withDeepLinked<T>()
    dialog<T>(typeMap, deepLinks + withDeepLinked, dialogProperties) {
        it.savedStateHandle[NavigationConstant.NAV_DEEP_LINKED] =
            withDeepLinked.getOrNull(0)?.uriPattern
        content(it)
    }
}

@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T> withDeepLinked(): List<NavDeepLink> {
    return tryResult {
        val scheme = appLabel.lowercase()
        val host = T::class.java.simpleName.lowercase()
        val descriptor = serializer<T>().descriptor
        val count = descriptor.elementsCount
        val manualUriPattern = if (count > 0) {
            val queryParams = (0 until count).joinToString("&") { index ->
                val name = descriptor.getElementName(index)
                "$name={$name}"
            }
            "$scheme://$host?$queryParams"
        } else {
            "$scheme://$host"
        }
        listOf(
            navDeepLink {
                uriPattern = manualUriPattern
            }
        )
    }.getOrDefault(emptyList())
}