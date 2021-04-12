package com.cookiejarapps.android.smartcookieweb.preferences

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.cookiejarapps.android.smartcookieweb.browser.HomepageChoice
import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.booleanPreference
import mozilla.components.support.ktx.android.content.intPreference
import mozilla.components.support.ktx.android.content.longPreference
import com.cookiejarapps.android.smartcookieweb.components.toolbar.ToolbarPosition

class UserPreferences(appContext: Context): PreferencesHolder {

    override val preferences: SharedPreferences =
        appContext.getSharedPreferences(SCW_PREFERENCES, MODE_PRIVATE)

    // Saved values
    var bookmarkFolder by booleanPreference("save_bookmark_folder", false)
    var bookmarkFolderId by longPreference("save_bookmark_folder_id", -1L)
    var shortcutDrawerOpen by booleanPreference("shortcut_drawer", true)
    var lastKnownPrivate by booleanPreference("last_known_mode_private", false)

    // Preferences
    var javaScriptEnabled by booleanPreference(JAVA_SCRIPT_ENABLED, true)
    var darkModeEnabled by booleanPreference(DARK_MODE, false)
    var followSystem by booleanPreference(FOLLOW_SYSTEM, false)
    var showAddonsInBar by booleanPreference(SHOW_ADDONS_IN_BAR, false)
    var searchEngineChoice by intPreference(SEARCH_ENGINE, 0)
    var toolbarPosition by intPreference(TOOLBAR_POSITION, ToolbarPosition.TOP.ordinal)
    var homepageType by intPreference(HOMEPAGE_TYPE, HomepageChoice.VIEW.ordinal)
    var launchInApp by booleanPreference(LAUNCH_IN_APP, true)

    // TODO: make these configurable & clean up duplicates
    var shouldUseBottomToolbar: Boolean
        get(){
            return toolbarPosition == ToolbarPosition.BOTTOM.ordinal
        }
        set(value){
            if(value) toolbarPosition = ToolbarPosition.BOTTOM.ordinal else toolbarPosition = ToolbarPosition.TOP.ordinal
        }

    val toolbarPositionType: ToolbarPosition
        get(){
            return if(toolbarPosition == ToolbarPosition.BOTTOM.ordinal) ToolbarPosition.BOTTOM else ToolbarPosition.TOP
        }

    val isDynamicToolbarEnabled: Boolean
        get() {
            return true
        }

    val shouldUseFixedTopToolbar: Boolean
        get() {
            return false
        }

    companion object {
        const val SCW_PREFERENCES = "scw_preferences"

        const val DARK_MODE = "dark_mode_enabled"
        const val JAVA_SCRIPT_ENABLED = "java_script_enabled"
        const val FOLLOW_SYSTEM = "follow_system"
        const val SHOW_ADDONS_IN_BAR = "show_addons_in_bar"
        const val SEARCH_ENGINE = "search_engine"
        const val TOOLBAR_POSITION = "toolbar_position"
        const val HOMEPAGE_TYPE = "homepage_type"
        const val LAUNCH_IN_APP = "homepage_type"
    }
}