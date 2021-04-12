package com.cookiejarapps.android.smartcookieweb.browser.home

import com.cookiejarapps.android.smartcookieweb.browser.BrowsingMode

sealed class Mode {
    object Normal : Mode()
    object Private : Mode()

    companion object {
        fun fromBrowsingMode(browsingMode: BrowsingMode) = when (browsingMode) {
            BrowsingMode.Normal -> Normal
            BrowsingMode.Private -> Private
        }
    }
}
