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

    companion object {
        const val SCW_PREFERENCES = "scw_preferences"

        const val ENABLE_TRACKING_PROTECTION = "enable_tracking_protection"
        const val ALLOW_AUTO_PLAY = "allow_auto_play"
        const val JAVA_SCRIPT_ENABLED = "java_script_enabled"
    }
}