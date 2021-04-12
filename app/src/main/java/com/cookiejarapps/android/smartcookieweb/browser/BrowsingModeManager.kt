package com.cookiejarapps.android.smartcookieweb.browser

import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences

enum class BrowsingMode {
    Normal, Private;

    val isPrivate get() = this == Private

    companion object {
        fun fromBoolean(isPrivate: Boolean) = if (isPrivate) Private else Normal
    }
}

interface BrowsingModeManager {
    var mode: BrowsingMode
}

class DefaultBrowsingModeManager(
    private var _mode: BrowsingMode,
    private val userPreferences: UserPreferences,
    private val modeDidChange: (BrowsingMode) -> Unit
) : BrowsingModeManager {

    override var mode: BrowsingMode
        get() = _mode
        set(value) {
            _mode = value
            modeDidChange(value)
            userPreferences.lastKnownPrivate = value == BrowsingMode.Private
        }
}
