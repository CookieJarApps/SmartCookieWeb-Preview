package com.cookiejarapps.android.smartcookieweb.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.settings.ThemeChoice

fun applyAppTheme(choice: Int) {
    val mode = when (choice) {
        ThemeChoice.SYSTEM.ordinal -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeChoice.LIGHT.ordinal -> AppCompatDelegate.MODE_NIGHT_NO
        else -> AppCompatDelegate.MODE_NIGHT_YES
    }
    AppCompatDelegate.setDefaultNightMode(mode)
}

fun applyAppTheme(context: Context) {
    applyAppTheme(UserPreferences(context).appThemeChoice)
}