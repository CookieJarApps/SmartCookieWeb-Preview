package com.cookiejarapps.android.smartcookieweb.preferences

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.booleanPreference

class UserPreferences(private val appContext: Context): PreferencesHolder {

    override val preferences: SharedPreferences =
        appContext.getSharedPreferences(SCW_PREFERENCES, MODE_PRIVATE)

    var javaScriptEnabled by booleanPreference(JAVA_SCRIPT_ENABLED, true)
    var darkModeEnabled by booleanPreference(DARK_MODE, false)
    var followSystem by booleanPreference(FOLLOW_SYSTEM, false)

    companion object {
        const val SCW_PREFERENCES = "scw_preferences"

        const val DARK_MODE = "dark_mode_enabled"
        const val JAVA_SCRIPT_ENABLED = "java_script_enabled"
        const val FOLLOW_SYSTEM = "follow_system"
    }
}