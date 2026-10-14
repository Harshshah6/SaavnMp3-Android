package com.harsh.shah.saavnmp3.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.google.android.material.color.DynamicColors
import com.harsh.shah.saavnmp3.R

/**
 * Central Theme Manager for Material 3 design and theming.
 * Supports:
 *  - Theme mode: System Default, Light, Dark
 *  - Material You dynamic colors (Android 12+ API 31+)
 *  - Curated Material 3 color palettes: Emerald, Ocean Blue, Sunset Purple, Crimson Rose, Amber Gold
 *  - AMOLED Pure Black mode for OLED screens in dark mode
 */
object ThemeManager {
    const val PREFS_NAME = "settings"
    const val KEY_THEME_MODE = "theme" // "system", "dark", "light"
    const val KEY_THEME_PALETTE = "theme_palette" // "emerald", "ocean", "purple", "crimson", "amber"
    const val KEY_AMOLED_MODE = "amoled_mode" // Boolean
    const val KEY_DYNAMIC_COLOR = "dynamic_color" // Boolean

    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeConfig(context: Context): String =
        "${getThemeMode(context)}_${isDynamicColorEnabled(context)}_${getThemePalette(context)}_${isAmoledEnabled(context)}"

    fun getThemeMode(context: Context): String =
        getPrefs(context).getString(KEY_THEME_MODE, "system") ?: "system"

    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit(commit = true) { putString(KEY_THEME_MODE, mode) }
        applyNightMode(mode)
        MusicPlayerManager.updateWidget()
    }

    fun isDynamicColorEnabled(context: Context): Boolean {
        val available = DynamicColors.isDynamicColorAvailable()
        return getPrefs(context).getBoolean(KEY_DYNAMIC_COLOR, available)
    }

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit(commit = true) { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
        MusicPlayerManager.updateWidget()
    }

    fun getThemePalette(context: Context): String =
        getPrefs(context).getString(KEY_THEME_PALETTE, "emerald") ?: "emerald"

    fun setThemePalette(context: Context, palette: String) {
        getPrefs(context).edit(commit = true) { putString(KEY_THEME_PALETTE, palette) }
        MusicPlayerManager.updateWidget()
    }

    fun isAmoledEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AMOLED_MODE, false)

    fun setAmoledEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit(commit = true) { putBoolean(KEY_AMOLED_MODE, enabled) }
        MusicPlayerManager.updateWidget()
    }

    fun getPrimaryColor(context: Context): Int {
        val typedValue = android.util.TypedValue()
        if (context.theme != null && context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            val color = if (typedValue.resourceId != 0) {
                androidx.core.content.ContextCompat.getColor(context, typedValue.resourceId)
            } else {
                typedValue.data
            }
            if (color != 0) return color
        }

        val dynamic = isDynamicColorEnabled(context)
        val palette = getThemePalette(context)
        val baseTheme = when (palette) {
            "ocean" -> R.style.Theme_Melotune_Ocean
            "purple" -> R.style.Theme_Melotune_Purple
            "crimson" -> R.style.Theme_Melotune_Crimson
            "amber" -> R.style.Theme_Melotune_Amber
            else -> R.style.Theme_Melotune_Emerald
        }
        var themedCtx: Context = android.view.ContextThemeWrapper(context, baseTheme)
        if (dynamic && DynamicColors.isDynamicColorAvailable()) {
            themedCtx = DynamicColors.wrapContextIfAvailable(themedCtx)
        }
        val tv = android.util.TypedValue()
        if (themedCtx.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)) {
            return if (tv.resourceId != 0) {
                androidx.core.content.ContextCompat.getColor(themedCtx, tv.resourceId)
            } else {
                tv.data
            }
        }
        return androidx.core.content.ContextCompat.getColor(context, R.color.emerald_primary_dark)
    }

    fun getOnPrimaryColor(context: Context): Int {
        val typedValue = android.util.TypedValue()
        if (context.theme != null && context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)) {
            val color = if (typedValue.resourceId != 0) {
                androidx.core.content.ContextCompat.getColor(context, typedValue.resourceId)
            } else {
                typedValue.data
            }
            if (color != 0) return color
        }

        val dynamic = isDynamicColorEnabled(context)
        val palette = getThemePalette(context)
        val baseTheme = when (palette) {
            "ocean" -> R.style.Theme_Melotune_Ocean
            "purple" -> R.style.Theme_Melotune_Purple
            "crimson" -> R.style.Theme_Melotune_Crimson
            "amber" -> R.style.Theme_Melotune_Amber
            else -> R.style.Theme_Melotune_Emerald
        }
        var themedCtx: Context = android.view.ContextThemeWrapper(context, baseTheme)
        if (dynamic && DynamicColors.isDynamicColorAvailable()) {
            themedCtx = DynamicColors.wrapContextIfAvailable(themedCtx)
        }
        val tv = android.util.TypedValue()
        if (themedCtx.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, tv, true)) {
            return if (tv.resourceId != 0) {
                androidx.core.content.ContextCompat.getColor(themedCtx, tv.resourceId)
            } else {
                tv.data
            }
        }
        return androidx.core.content.ContextCompat.getColor(context, R.color.emerald_on_primary_dark)
    }

    fun applyNightMode(mode: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun init(application: Application) {
        applyNightMode(getThemeMode(application))
    }

    fun applyTheme(activity: Activity) {
        val context = activity.applicationContext
        val dynamic = isDynamicColorEnabled(context)
        val palette = getThemePalette(context)
        val amoled = isAmoledEnabled(context)

        val isDark = when (getThemeMode(context)) {
            "dark" -> true
            "light" -> false
            else -> {
                val nightMode = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }

        val themeRes = when (palette) {
            "ocean" -> R.style.Theme_Melotune_Ocean
            "purple" -> R.style.Theme_Melotune_Purple
            "crimson" -> R.style.Theme_Melotune_Crimson
            "amber" -> R.style.Theme_Melotune_Amber
            else -> R.style.Theme_Melotune_Emerald
        }
        activity.setTheme(themeRes)

        if (dynamic && DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }

        if (amoled && isDark) {
            activity.theme.applyStyle(R.style.ThemeOverlay_Melotune_Amoled, true)
        }
    }
}
