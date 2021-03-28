package com.cookiejarapps.android.smartcookieweb

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.integration.FindInPageIntegration
import com.cookiejarapps.android.smartcookieweb.settings.activity.SettingsActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.item.BrowserMenuCheckbox
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.support.ktx.android.content.getColorFromAttr

class BrowserMenu(
    private val context: Context,
    private val onItemTapped: (Item) -> Unit = {}
) {
    sealed class Item {
        object Bookmarks : Item()
    }

    private val BROWSER_PREFERENCES = "browser_preferences"

    val preferences: SharedPreferences =
        context.getSharedPreferences(BROWSER_PREFERENCES, Context.MODE_PRIVATE)

    val coreMenuItems by lazy {val back = BrowserMenuItemToolbar.TwoStateButton(
            primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_back,
            primaryContentDescription = "Back",
            primaryImageTintResource = R.color.primary_icon,
            isInPrimaryState = {
                context.components.store.state.selectedTab?.content?.canGoBack ?: true
            },
            disableInSecondaryState = true,
            secondaryImageTintResource = R.color.secondary_icon
    ) {
        context.components.sessionUseCases.goBack()
    }

        val forward = BrowserMenuItemToolbar.TwoStateButton(
                primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_forward,
                primaryContentDescription = "Forward",
                primaryImageTintResource = R.color.primary_icon,
                isInPrimaryState = {
                    context.components.store.state.selectedTab?.content?.canGoForward ?: true
                },
                disableInSecondaryState = true,
                secondaryImageTintResource = R.color.secondary_icon
        ) {
            context.components.sessionUseCases.goForward()
        }

        val refresh = BrowserMenuItemToolbar.TwoStateButton(
                primaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_refresh,
                primaryContentDescription = "Refresh",
                primaryImageTintResource = R.color.primary_icon,
                isInPrimaryState = {
                    context.components.sessionManager.selectedSession?.loading == false
                },
                secondaryImageResource = mozilla.components.ui.icons.R.drawable.mozac_ic_stop,
                secondaryContentDescription = "Stop",
                secondaryImageTintResource = R.color.secondary_icon,
                disableInSecondaryState = false
        ) {
            if (context.components.sessionManager.selectedSession?.loading == true) {
                context.components.sessionUseCases.stopLoading()
            } else {
                context.components.sessionUseCases.reload()
            }
        }

        val toolbar = BrowserMenuItemToolbar(listOf(back, forward, refresh))

        val newTabItem = BrowserMenuImageText(
                context.getString(R.string.new_tab),
                R.drawable.ic_round_add
            ) {
                context.components.tabsUseCases.addTab.invoke(
                    "about:blank",
                    selectTab = true
                )
            }

        val shareItem = BrowserMenuImageText(
                context.resources.getString(R.string.mozac_selection_context_menu_share),
                R.drawable.ic_baseline_share
            ) {
                MainScope().launch {
                    context.components.sessionManager.selectedSession?.let {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        if (it.title != "") {
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, it.title)
                        }
                        shareIntent.putExtra(Intent.EXTRA_TEXT, it.url)
                        ContextCompat.startActivity(
                            context,
                            Intent.createChooser(
                                shareIntent,
                                context.resources.getString(R.string.mozac_selection_context_menu_share)
                            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            null
                        )
                    }
                }
            }.apply {
                visible =
                    { context.components.webAppUseCases.isPinningSupported() && context.components.sessionManager.selectedSession != null }
            }

        val homeScreenItem = BrowserMenuImageText(
                context.resources.getString(R.string.action_add_to_homescreen),
                R.drawable.ic_round_smartphone
            ) {
                MainScope().launch {
                    context.components.webAppUseCases.addToHomescreen()
                }
            }.apply {
                visible =
                    { context.components.webAppUseCases.isPinningSupported() && context.components.sessionManager.selectedSession != null }
            }

        val externalAppItem = BrowserMenuImageText(
                context.getString(R.string.mozac_feature_contextmenu_open_link_in_external_app),
                R.drawable.ic_baseline_open_in_new
            ) {
                val getRedirect = context.components.appLinksUseCases.appLinkRedirect
                context.components.sessionManager.selectedSession?.let {
                    val redirect = getRedirect.invoke(it.url)
                    redirect.appIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.components.appLinksUseCases.openAppLink.invoke(redirect.appIntent)
                }
            }.apply {
                visible = {
                    context.components.sessionManager.selectedSession?.let {
                        context.components.appLinksUseCases.appLinkRedirect(it.url).hasExternalApp()
                    } ?: false
                }
            }

        val desktopSiteItem = BrowserMenuCheckbox(context.getString(R.string.desktop_mode), {
                context.components.store.state.selectedTab?.content?.desktopMode == true
            }) { checked ->
                context.components.sessionUseCases.requestDesktopSite(checked)
            }.apply {
                visible = { context.components.store.state.selectedTab != null }
            }

        val openLinksInAppItem = BrowserMenuCheckbox("Open links in apps", {
                preferences.getBoolean(Components.PREF_LAUNCH_EXTERNAL_APP, false)
            }) { checked ->
                preferences.edit().putBoolean(Components.PREF_LAUNCH_EXTERNAL_APP, checked).apply()
            }

        val findInPageItem = BrowserMenuImageText(
                context.getString(R.string.mozac_feature_findindpage_input),
                R.drawable.ic_baseline_find_in_page
            ) {
                FindInPageIntegration.launch?.invoke()
            }

        val settingsItem =
            BrowserMenuImageText(
                context.resources.getString(R.string.settings),
                R.drawable.ic_round_settings
            ) {
                val settings = Intent(context, SettingsActivity::class.java)
                settings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(settings)
        }

        val bookmarksItem = BrowserMenuImageText(
                context.resources.getString(R.string.action_bookmarks),
                R.drawable.ic_baseline_bookmark
            ) {
                onItemTapped.invoke(Item.Bookmarks)
            }

        val menuItems = listOfNotNull(
            toolbar,
            newTabItem,
            shareItem,
            findInPageItem,
            externalAppItem,
            openLinksInAppItem,
            desktopSiteItem,
            settingsItem,
            bookmarksItem
        )

        menuItems
    }
}