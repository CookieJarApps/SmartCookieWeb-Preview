package com.cookiejarapps.android.smartcookieweb.integration

import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.findinpage.FindInPageFeature
import mozilla.components.feature.findinpage.view.FindInPageView
import mozilla.components.support.base.feature.LifecycleAwareFeature

@Suppress("UndocumentedPublicClass")
class FindInPageIntegration(
    private val store: BrowserStore,
    private val sessionId: String? = null,
    stub: ViewStub,
    private val engineView: EngineView,
    private val toolbarInfo: ToolbarInfo,
    private val prepareLayout: () -> Unit,
    private val restorePreviousLayout: () -> Unit
) : InflationAwareFeature(stub) {
    override fun onViewInflated(view: View): LifecycleAwareFeature {
        return FindInPageFeature(store, view as FindInPageView, engineView) {
            restorePreviousLayout()

            view.visibility = View.GONE
        }
    }

    override fun onLaunch(view: View, feature: LifecycleAwareFeature) {
        store.state.findCustomTabOrSelectedTab(sessionId)?.let { tab ->
            prepareLayout()

            view.visibility = View.VISIBLE
            (feature as FindInPageFeature).bind(tab)
            view.layoutParams.height = toolbarInfo.toolbar.height
        }
    }

    /**
     * Holder of all details needed about the Toolbar.
     * Used to modify the layout of BrowserToolbar while the find in page bar is shown.
     */
    data class ToolbarInfo(
        val toolbar: BrowserToolbar,
        val isToolbarDynamic: Boolean,
        val isToolbarPlacedAtTop: Boolean
    )
}