package io.kelly.extension

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import io.kelly.KellyInternal
import java.util.Locale

fun Configuration.withDensity(widthDp: Float?) = apply {
    if (widthDp == null || widthDp <= 0f) return this
    val displayMetrics = KellyInternal.context.resources.displayMetrics
    val widthPixels = displayMetrics.widthPixels
    val targetDensityDpi = (widthPixels / widthDp * 160).toInt()
    densityDpi = targetDensityDpi
    fontScale = 1.0f
}

fun Configuration.withLocale(lang: String?) = apply {
    if (lang.isNullOrBlank()) return this
    val locale = Locale.forLanguageTag(lang)
    Locale.setDefault(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = KellyInternal.context.getSystemService(LocaleManager::class.java)
        localeManager?.applicationLocales = LocaleList(locale)
    }
    setLocales(LocaleList(locale))
    setLayoutDirection(locale)
}

