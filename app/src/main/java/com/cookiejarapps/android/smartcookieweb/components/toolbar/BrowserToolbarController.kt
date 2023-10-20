package com.cookiejarapps.android.smartcookieweb.components.toolbar

import androidx.navigation.NavController
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.BrowserAnimator.Companion.getToolbarNavOptions
import com.cookiejarapps.android.smartcookieweb.BrowserFragmentDirections
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingMode
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.ext.nav
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineView
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.ui.tabcounter.TabCounterMenu.Item
import com.cookiejarapps.android.smartcookieweb.components.toolbar.TabCounterMenu

interface BrowserToolbarController {
    fun handleScroll(offset: Int)
    fun handleToolbarPaste(text: String)
    fun handleToolbarPasteAndGo(text: String)
    fun handleToolbarClick()
    fun handleTabCounterClick()
    fun handleTabCounterItemInteraction(item: Item)
}

class DefaultBrowserToolbarController(
    private val store: BrowserStore,
    private val activity: BrowserActivity,
    private val navController: NavController,
    private val engineView: EngineView,
    private val customTabSessionId: String?,
    private val onTabCounterClicked: () -> Unit
) : BrowserToolbarController {

    private val currentSession
        get() = store.state.findCustomTabOrSelectedTab(customTabSessionId)

    override fun handleToolbarPaste(text: String) {
        navController.nav(
            R.id.browserFragment,
            BrowserFragmentDirections.actionGlobalSearchDialog(
                sessionId = currentSession?.id,
                pastedText = text
            ),
            getToolbarNavOptions(activity)
        )
    }

    override fun handleToolbarPasteAndGo(text: String) {
        if (text.isUrl()) {
            store.updateSearchTermsOfSelectedSession("")
            activity.components.sessionUseCases.loadUrl.invoke(text)
            return
        }

        store.updateSearchTermsOfSelectedSession(text)
        activity.components.searchUseCases.defaultSearch.invoke(
            text,
            sessionId = store.state.selectedTabId
        )
    }

    override fun handleToolbarClick() {
        navController.nav(
            R.id.browserFragment,
            BrowserFragmentDirections.actionGlobalSearchDialog(
                currentSession?.id
            ),
            getToolbarNavOptions(activity)
        )
    }

    override fun handleTabCounterClick() {
        onTabCounterClicked.invoke()
    }

    override fun handleTabCounterItemInteraction(item: Item) {
        when (item) {
            is Item.NewTab -> {
                activity.browsingModeManager.mode = BrowsingMode.Normal
                navController.navigate(
                    BrowserFragmentDirections.actionGlobalHome(focusOnAddressBar = true)
                )
            }
            is Item.NewPrivateTab -> {
                activity.browsingModeManager.mode = BrowsingMode.Private
                navController.navigate(
                    BrowserFragmentDirections.actionGlobalHome(focusOnAddressBar = true)
                )
            }
            is Item.CloseTab -> {
                store.state.selectedTab?.let {
                    if (store.state.getNormalOrPrivateTabs(it.content.private).count() == 1) {
                        navController.navigate(
                            BrowserFragmentDirections.actionGlobalHome()
                        )
                    } else {
                        activity.components.tabsUseCases.removeTab(it.id, selectParentIfExists = true)
                    }
                }
            }
            is Item.DuplicateTab -> {
                store.state.selectedTab?.let {
                    if(activity.browsingModeManager.mode == BrowsingMode.Normal){
                        activity.components.tabsUseCases.addTab.invoke(it.content.url, true)
                    }
                    else{
                        activity.components.tabsUseCases.addTab.invoke(it.content.url, true, private = true)
                    }
                }
            }
        }
    }

    override fun handleScroll(offset: Int) {
        if (UserPreferences(activity).hideBarWhileScrolling) {
            engineView.setVerticalClipping(offset)
        }
    }
}

private fun BrowserStore.updateSearchTermsOfSelectedSession(
    searchTerms: String
) {
    val selectedTabId = state.selectedTabId ?: return

    dispatch(ContentAction.UpdateSearchTermsAction(
        selectedTabId,
        searchTerms
    ))
}