package io.kelly.extension

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.kelly.KellyInternal

@Composable
fun LayoutDirectionProvider(useCurrent: Boolean, content: @Composable () -> Unit) {
    val layoutDirection = LocalLayoutDirection.current
    CompositionLocalProvider(
        LocalLayoutDirection provides if (useCurrent) layoutDirection
        else {
            if (layoutDirection == LayoutDirection.Ltr) LayoutDirection.Rtl else LayoutDirection.Ltr
        },
        content
    )
}

@Composable
fun SystemLayoutDirectionProvider(
    content: @Composable () -> Unit
) {
    val layoutDirectionInt = KellyInternal.context.resources.configuration.layoutDirection
    val layoutDirection = if (layoutDirectionInt == Configuration.SCREENLAYOUT_LAYOUTDIR_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        content
    )
}