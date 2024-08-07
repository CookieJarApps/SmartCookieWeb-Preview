package com.cookiejarapps.android.smartcookieweb.browser.home

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.ext.getHighlight
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuImageText

class HomeMenu(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val onItemTapped: (Item) -> Unit = {},
    private val onMenuBuilderChanged: (BrowserMenuBuilder) -> Unit = {},
    private val onHighlightPresent: (BrowserMenuHighlight) -> Unit = {}
) {
    sealed class Item {
        data object NewTab : Item()
        data object NewPrivateTab : Item()
        data object AddonsManager : Item()
        data object Settings : Item()
        data object History : Item()
        data object Bookmarks : Item()
    }

    private val shouldUseBottomToolbar = UserPreferences(context).shouldUseBottomToolbar

    private val coreMenuItems by lazy {

        val newTabItem = BrowserMenuImageText(
            context.getString(R.string.mozac_browser_menu_new_tab),
            R.drawable.mozac_ic_tab_new_24,
            R.color.primary_icon
        ) {
            onItemTapped.invoke(Item.NewTab)
        }

        val newPrivateTabItem = BrowserMenuImageText(
            context.getString(R.string.mozac_browser_menu_new_private_tab),
            R.drawable.ic_incognito,
            R.color.primary_icon
        ) {
            onItemTapped.invoke(Item.NewPrivateTab)
        }

        val bookmarksIcon = R.drawable.ic_baseline_bookmark

        val bookmarksItem = BrowserMenuImageText(
            context.getString(R.string.action_bookmarks),
            bookmarksIcon,
            R.color.primary_icon
        ) {
            onItemTapped.invoke(Item.Bookmarks)
        }

        val historyItem = BrowserMenuImageText(
            context.getString(R.string.action_history),
            R.drawable.ic_baseline_history,
            R.color.primary_icon
        ) {
            onItemTapped.invoke(Item.History)
        }

        val addons = BrowserMenuImageText(
            context.getString(R.string.mozac_browser_menu_extensions),
            R.drawable.mozac_ic_extension_24,
            R.color.primary_icon
        ) {
            onItemTapped.invoke(Item.AddonsManager)
        }

        val settingsItem = BrowserMenuImageText(
            context.getString(R.string.settings),
            R.drawable.ic_round_settings,
            R.color.primary_icon
        ) {
            onItemTapped.invoke(Item.Settings)
        }

        val menuItems = listOfNotNull(
            newTabItem,
            newPrivateTabItem,
            BrowserMenuDivider(),
            historyItem,
            bookmarksItem,
            BrowserMenuDivider(),
            settingsItem,
            addons
        ).also { items ->
            items.getHighlight()?.let { onHighlightPresent(it) }
        }

        if (shouldUseBottomToolbar) {
            menuItems.reversed()
        } else {
            menuItems
        }
    }

    init {
        onMenuBuilderChanged(BrowserMenuBuilder(coreMenuItems))
    }
}
