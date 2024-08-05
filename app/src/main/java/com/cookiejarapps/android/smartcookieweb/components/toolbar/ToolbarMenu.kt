package com.cookiejarapps.android.smartcookieweb.components.toolbar

import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar

interface ToolbarMenu {
    sealed class Item {
        data object Settings : Item()
        data class RequestDesktop(val isChecked: Boolean) : Item()
        data object FindInPage : Item()
        data object Share : Item()
        data class Back(val viewHistory: Boolean) : Item()
        data class Forward(val viewHistory: Boolean) : Item()
        data class Reload(val bypassCache: Boolean) : Item()
        data object Stop : Item()
        data object AddToHomeScreen : Item()
        data object NewTab : Item()
        data object NewPrivateTab : Item()
        data object OpenInApp : Item()
        data object Bookmarks : Item()
        data object History : Item()
        data object Print : Item()
        data object PDF : Item()
    }

    val menuBuilder: BrowserMenuBuilder
    val menuToolbar: BrowserMenuItemToolbar
}
