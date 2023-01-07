package com.cookiejarapps.android.smartcookieweb.components.toolbar

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged

@ExperimentalCoroutinesApi
class MenuPresenter(
    private val menuToolbar: BrowserToolbar,
    private val store: BrowserStore,
    private val sessionId: String? = null
) : View.OnAttachStateChangeListener {

    private var scope: CoroutineScope? = null

    fun start() {
        menuToolbar.addOnAttachStateChangeListener(this)
        scope = store.flowScoped { flow ->
            flow.mapNotNull { state -> state.findCustomTabOrSelectedTab(sessionId) }
                .ifAnyChanged { tab ->
                    arrayOf(
                        tab.content.loading,
                        tab.content.canGoBack,
                        tab.content.canGoForward,
                        tab.content.webAppManifest
                    )
                }
                .collect {
                    invalidateActions()
                }
        }
    }

    fun stop() {
        scope?.cancel()
    }

    fun invalidateActions() {
        menuToolbar.invalidateActions()
    }

    override fun onViewAttachedToWindow(p0: View) {
        // no-op
    }

    override fun onViewDetachedFromWindow(p0: View) {
        menuToolbar.onStop()
    }
}
