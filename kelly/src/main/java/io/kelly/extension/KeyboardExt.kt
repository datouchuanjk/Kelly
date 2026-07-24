package io.kelly.extension

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import java.util.WeakHashMap

private object LegacyKeyboardProvider {
    private val caches = WeakHashMap<View, State<Int>>()
    fun getSharedState(activity: Activity): State<Int> {
        val decorView = activity.window.decorView
        caches[decorView]?.let { return it }
        val keyboardState = mutableIntStateOf(0)
        caches[decorView] = keyboardState
        var baseHeight = 0
        val dummyView = View(activity)
        val popupWindow = PopupWindow(activity).apply {
            width = 0
            height = WindowManager.LayoutParams.MATCH_PARENT
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
            setBackgroundDrawable(null)
            contentView = dummyView
        }

        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            val currentHeight = dummyView.height
            if (baseHeight == 0 || currentHeight > baseHeight) {
                baseHeight = currentHeight
            }
            val diff = baseHeight - currentHeight
            keyboardState.intValue = if (diff > baseHeight / 3) diff else 0
        }
        dummyView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        decorView.post {
            if (activity.isFinishing || activity.isDestroyed) {
                return@post
            }
            runCatching {
                popupWindow.showAtLocation(decorView, Gravity.NO_GRAVITY, 0, 0)
            }
        }

        decorView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}

            override fun onViewDetachedFromWindow(v: View) {
                if (dummyView.viewTreeObserver.isAlive) {
                    dummyView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
                if (popupWindow.isShowing) {
                    runCatching { popupWindow.dismiss() }
                }
                caches.remove(decorView)
                decorView.removeOnAttachStateChangeListener(this)
            }
        })
        return keyboardState
    }
}

@Composable
fun rememberKeyboardState(): KeyboardState {
    val localDensity = LocalDensity.current
    val state = remember {
        KeyboardStateImpl(localDensity)
    }
    val px = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        WindowInsets.ime.getBottom(LocalDensity.current)
    } else {
        val context = LocalContext.current
        val activity = context as? Activity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            0
        } else {
            val keyboardState by remember(activity) {
                LegacyKeyboardProvider.getSharedState(activity)
            }
            keyboardState
        }
    }
    LaunchedEffect(px) {
        state.height = px
    }
    return state
}

fun Modifier.imePaddingCompat(): Modifier = composed {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        imePadding()
    } else {
        val targetPadding = rememberKeyboardState()
        val animatedPadding by animateDpAsState(
            targetValue = targetPadding.heightDp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "KeyboardPadding"
        )
        padding(bottom = animatedPadding)
    }
}

interface KeyboardState {
    val height: Int
    val heightDp: Dp
    val isVisible: Boolean
}

internal class KeyboardStateImpl(density: Density) : KeyboardState, Density by density {

    override var height by mutableIntStateOf(0)

    override val heightDp by derivedStateOf {
        with(density) {
            height.toDp()
        }
    }

    override val isVisible by derivedStateOf {
        height > 0f
    }
}