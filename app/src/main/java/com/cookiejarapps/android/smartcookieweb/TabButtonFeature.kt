package com.cookiejarapps.android.smartcookieweb

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.tabs.toolbar.TabCounterToolbarButton
import mozilla.components.ui.tabcounter.TabCounterMenu

// TabsToolbarFeature, but places the tabs button on the left side
@ExperimentalCoroutinesApi
@Suppress("LongParameterList")
class TabButtonFeature(
    toolbar: Toolbar,
    store: BrowserStore,
    sessionId: String? = null,
    lifecycleOwner: LifecycleOwner,
    showTabs: () -> Unit,
    tabCounterMenu: TabCounterMenu? = null,
    countBasedOnSelectedTabType: Boolean = true
) {
    init {
        run {
            // this feature is not used for Custom Tabs
            if (sessionId != null && store.state.findCustomTab(sessionId) != null) return@run

            val tabsAction = TabCounterToolbarButton(
                lifecycleOwner = lifecycleOwner,
                showTabs = showTabs,
                store = store,
                menu = tabCounterMenu,
                countBasedOnSelectedTabType = countBasedOnSelectedTabType
            )
            toolbar.addNavigationAction(tabsAction)
        }
    }
}