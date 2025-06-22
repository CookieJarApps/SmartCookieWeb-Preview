package com.cookiejarapps.android.smartcookieweb.ext

import android.content.Context
import android.content.res.Configuration
import com.cookiejarapps.android.smartcookieweb.BrowserApp
import com.cookiejarapps.android.smartcookieweb.components.Components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.settings.ThemeChoice

// get app from context
val Context.application: BrowserApp
    get() = applicationContext as BrowserApp

// get components from context
val Context.components: Components
    get() = application.components

fun Context.isAppInDarkTheme(): Boolean {
    val prefs = UserPreferences(this)
    val isSystemDark =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    return when (prefs.appThemeChoice) {
        ThemeChoice.LIGHT.ordinal -> false
        ThemeChoice.DARK.ordinal -> true
        else -> isSystemDark
    }
}