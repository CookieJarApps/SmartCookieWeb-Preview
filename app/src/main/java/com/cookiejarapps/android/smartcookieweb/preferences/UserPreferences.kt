package com.cookiejarapps.android.smartcookieweb.preferences

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.cookiejarapps.android.smartcookieweb.browser.HomepageChoice
import com.cookiejarapps.android.smartcookieweb.browser.ThemeChoice
import com.cookiejarapps.android.smartcookieweb.components.toolbar.ToolbarPosition
import mozilla.components.support.ktx.android.content.*

class UserPreferences(appContext: Context): PreferencesHolder {

    override val preferences: SharedPreferences =
        appContext.getSharedPreferences(SCW_PREFERENCES, MODE_PRIVATE)

    // Saved values
    var bookmarkFolder by booleanPreference("save_bookmark_folder", false)
    var bookmarkFolderId by longPreference("save_bookmark_folder_id", -1L)
    var shortcutDrawerOpen by booleanPreference("shortcut_drawer", true)
    var lastKnownPrivate by booleanPreference("last_known_mode_private", false)
    var firstLaunch by booleanPreference("first_launch", true)

    // Preferences
    var javaScriptEnabled by booleanPreference(JAVA_SCRIPT_ENABLED, true)
    var showAddonsInBar by booleanPreference(SHOW_ADDONS_IN_BAR, false)
    var searchEngineChoice by intPreference(SEARCH_ENGINE, 0)
    var toolbarPosition by intPreference(TOOLBAR_POSITION, ToolbarPosition.TOP.ordinal)
    var homepageType by intPreference(HOMEPAGE_TYPE, HomepageChoice.VIEW.ordinal)
    var appThemeChoice by intPreference(APP_THEME_CHOICE, ThemeChoice.SYSTEM.ordinal)
    var webThemeChoice by intPreference(WEB_THEME_CHOICE, ThemeChoice.SYSTEM.ordinal)
    var launchInApp by booleanPreference(LAUNCH_IN_APP, true)
    var customAddonCollection by booleanPreference(CUSTOM_ADDON_BOOL, false)
    var shownCollectionDisclaimer by booleanPreference(SHOWN_ADDON_DISCLAIMER, false)
    var customAddonCollectionUser by stringPreference(COLLECTION_USER, "")
    var customAddonCollectionName by stringPreference(COLLECTION_NAME, "")
    var autoFontSize by booleanPreference(AUTO_FONT_SIZE, true)
    var fontSizeFactor by floatPreference(FONT_SIZE_FACTOR, 1f)
    var hideBarWhileScrolling by booleanPreference(HIDE_URL_BAR, true)
    var swapDrawers by booleanPreference(SWAP_DRAWERS, false)
    var stackFromBottom by booleanPreference(STACK_FROM_BOTTOM, false)
    var showTabsInGrid by booleanPreference(SHOW_TABS_IN_GRID, false)
    var swipeToRefresh by booleanPreference(SWIPE_TO_REFRESH, true)

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

    companion object {
        const val SCW_PREFERENCES = "scw_preferences"

        const val JAVA_SCRIPT_ENABLED = "java_script_enabled"
        const val SHOW_ADDONS_IN_BAR = "show_addons_in_bar"
        const val SEARCH_ENGINE = "search_engine"
        const val TOOLBAR_POSITION = "toolbar_position"
        const val HOMEPAGE_TYPE = "homepage_type"
        const val APP_THEME_CHOICE = "app_theme_choice"
        const val WEB_THEME_CHOICE = "web_theme_choice"
        const val LAUNCH_IN_APP = "launch_in_app"
        const val CUSTOM_ADDON_BOOL = "custom_addon_bool"
        const val SHOWN_ADDON_DISCLAIMER = "shown_disclaimer"
        const val COLLECTION_NAME = "collection_name"
        const val COLLECTION_USER = "collection_user"
        const val AUTO_FONT_SIZE = "auto_font_size"
        const val FONT_SIZE_FACTOR = "font_size_factor"
        const val HIDE_URL_BAR = "hide_url_bar"
        const val SWAP_DRAWERS = "swap_drawers"
        const val STACK_FROM_BOTTOM = "stack_from_bottom"
        const val SHOW_TABS_IN_GRID = "show_tabs_in_grid"
        const val SWIPE_TO_REFRESH = "swipe_to_refresh"
    }
}