package io.kelly.compile.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.toRoute
import io.kelly.extension.tryResult
import kotlin.reflect.KType

inline fun <reified T> NavBackStackEntry.toRouteOrNull(): T? = tryResult {
    toRoute<T>()
}.getOrNull()


inline fun <reified T : Any> SavedStateHandle.toRouteOrNull(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap()
): T? = tryResult {
    toRoute<T>(typeMap)
}.getOrNull()