package io.kelly.extension

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import io.kelly.KellyInternal

fun String?.toast(duration: Int = Toast.LENGTH_LONG) {
    SingleToast.toast(this, duration)
}

fun @receiver:StringRes Int.toast(duration: Int = Toast.LENGTH_LONG) {
    SingleToast.toast(this, duration)
}

internal object SingleToast {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val context: Context get() = KellyInternal.topActivity?.get() ?: KellyInternal.context

    @Volatile
    private var currentToast: Toast? = null

    fun toast(@StringRes resId: Int, duration: Int) {
        tryResult {
            context.getString(resId)
        }.onSuccess {
            toast(it, duration)
        }.onFailure {
            toast(resId.toString(), duration)
        }
    }

    fun toast(text: String?, duration: Int) {
        val validText = text?.trim()?.takeIf { it.isNotEmpty() } ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showToast(validText, duration)
        } else {
            mainHandler.post {
                showToast(validText, duration)
            }
        }
    }

    private fun showToast(text: String, duration: Int) {
        tryResult {
            currentToast?.cancel()
        }
        currentToast = Toast.makeText(context, text, duration)
        tryResult {
            currentToast?.view
                ?.findViewById<TextView>(android.R.id.message)
                ?.maxLines = Int.MAX_VALUE
        }
        currentToast?.setGravity(Gravity.CENTER, 0, 0)
        currentToast?.show()
    }
}