package com.cookiejarapps.android.smartcookieweb.preferences

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.cookiejarapps.android.smartcookieweb.browser.HomepageChoice
import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.booleanPreference
import mozilla.components.support.ktx.android.content.intPreference
import mozilla.components.support.ktx.android.content.longPreference

class UserPreferences(private val appContext: Context): PreferencesHolder {

    override val preferences: SharedPreferences =
        appContext.getSharedPreferences(SCW_PREFERENCES, MODE_PRIVATE)

    // Saved values
    var bookmarkFolder by booleanPreference("save_bookmark_folder", false)
    var bookmarkFolderId by longPreference("save_bookmark_folder_id", -1L)
    var shortcutDrawerOpen by booleanPreference("shortcut_drawer", true)

    // Preferences
    var javaScriptEnabled by booleanPreference(JAVA_SCRIPT_ENABLED, true)
    var darkModeEnabled by booleanPreference(DARK_MODE, false)
    var followSystem by booleanPreference(FOLLOW_SYSTEM, false)
    var showAddonsInBar by booleanPreference(SHOW_ADDONS_IN_BAR, true)
    var searchEngineChoice by intPreference(SEARCH_ENGINE, 0)
    var homepageType by intPreference(HOMEPAGE_TYPE, HomepageChoice.VIEW.ordinal)

    companion object {
        const val SCW_PREFERENCES = "scw_preferences"

        const val DARK_MODE = "dark_mode_enabled"
        const val JAVA_SCRIPT_ENABLED = "java_script_enabled"
        const val FOLLOW_SYSTEM = "follow_system"
        const val SHOW_ADDONS_IN_BAR = "show_addons_in_bar"
        const val SEARCH_ENGINE = "search_engine"
        const val HOMEPAGE_TYPE = "homepage_type"
    }
}