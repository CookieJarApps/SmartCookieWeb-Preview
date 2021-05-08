package com.cookiejarapps.android.smartcookieweb.components.toolbar

import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar

interface ToolbarMenu {
    sealed class Item {
        object Settings : Item()
        data class RequestDesktop(val isChecked: Boolean) : Item()
        object FindInPage : Item()
        object Share : Item()
        data class Back(val viewHistory: Boolean) : Item()
        data class Forward(val viewHistory: Boolean) : Item()
        data class Reload(val bypassCache: Boolean) : Item()
        object Stop : Item()
        object AddToHomeScreen : Item()
        object NewTab : Item()
        object OpenInApp : Item()
        object Bookmarks : Item()
        object History : Item()
        object Print : Item()
    }

    val menuBuilder: BrowserMenuBuilder
    val menuToolbar: BrowserMenuItemToolbar
}
