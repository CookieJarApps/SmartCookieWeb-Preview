package com.cookiejarapps.android.smartcookieweb.preferences

import android.content.SharedPreferences
import com.cookiegames.smartcookie.preference.delegates.booleanPreference
import com.cookiejarapps.android.smartcookieweb.di.UserPrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @UserPrefs preferences: SharedPreferences
) {

    var enableTrackingProtection by preferences.booleanPreference(ENABLE_TRACKING_PROTECTION, true)

    var allowAutoPlay by preferences.booleanPreference(ALLOW_AUTO_PLAY, false)

    var javaScriptEnabled by preferences.booleanPreference(JAVA_SCRIPT_ENABLED, true)

}

const val ENABLE_TRACKING_PROTECTION = "enable_tracking_protection"
const val ALLOW_AUTO_PLAY = "allow_auto_play"
const val JAVA_SCRIPT_ENABLED = "java_script_enabled"