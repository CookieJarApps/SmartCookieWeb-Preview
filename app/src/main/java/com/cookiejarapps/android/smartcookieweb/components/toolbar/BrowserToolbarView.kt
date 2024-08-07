package com.cookiejarapps.android.smartcookieweb.components.toolbar

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.VisibleForTesting
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.utils.ToolbarPopupWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.support.ktx.util.URLStringUtils.toDisplayUrl
import mozilla.components.ui.tabcounter.TabCounterMenu
import mozilla.components.ui.widgets.behavior.EngineViewScrollingBehavior
import java.lang.ref.WeakReference
import mozilla.components.ui.widgets.behavior.ViewPosition as MozacToolbarPosition

interface BrowserToolbarViewInteractor {
    fun onBrowserToolbarPaste(text: String)
    fun onBrowserToolbarPasteAndGo(text: String)
    fun onBrowserToolbarClicked()
    fun onBrowserToolbarMenuItemTapped(item: ToolbarMenu.Item)
    fun onTabCounterClicked()
    fun onScrolled(offset: Int)
}

@ExperimentalCoroutinesApi
@SuppressWarnings("LargeClass")
class BrowserToolbarView(
    private val container: ViewGroup,
    private val toolbarPosition: ToolbarPosition,
    private val interactor: BrowserToolbarViewInteractor,
    private val customTabSession: CustomTabSessionState?,
    private val lifecycleOwner: LifecycleOwner
) {

    private val settings = UserPreferences(container.context)

    @LayoutRes
    private val toolbarLayout = when (settings.toolbarPosition) {
        ToolbarPosition.BOTTOM.ordinal -> R.layout.component_bottom_browser_toolbar
        else -> R.layout.component_browser_top_toolbar
    }

    private val layout = LayoutInflater.from(container.context)
        .inflate(toolbarLayout, container, true)

    @VisibleForTesting
    internal var view: BrowserToolbar = layout
        .findViewById(R.id.toolbar)

    val toolbarIntegration: ToolbarIntegration

    @VisibleForTesting
    internal val isPwaTabOrTwaTab: Boolean
        get() = false

    init {
        val isCustomTabSession = customTabSession != null

        view.display.setOnUrlLongClickListener {
            ToolbarPopupWindow.show(
                WeakReference(view),
                customTabSession?.id,
                interactor::onBrowserToolbarPasteAndGo,
                interactor::onBrowserToolbarPaste
            )
            true
        }

        with(container.context) {
            val isPinningSupported = components.webAppUseCases.isPinningSupported()

            view.apply {
                setToolbarBehavior()

                elevation = resources.getDimension(R.dimen.browser_fragment_toolbar_elevation)

                if (!isCustomTabSession) {
                    display.setUrlBackground(ContextCompat.getDrawable(context, R.drawable.toolbar_background))
                }

                display.onUrlClicked = {
                    interactor.onBrowserToolbarClicked()
                    false
                }

                display.progressGravity = when (toolbarPosition) {
                    ToolbarPosition.BOTTOM -> DisplayToolbar.Gravity.TOP
                    ToolbarPosition.TOP -> DisplayToolbar.Gravity.BOTTOM
                }

                val primaryTextColor = ContextCompat.getColor(
                    container.context,
                    R.color.primary_icon
                )
                val secondaryTextColor = ContextCompat.getColor(
                    container.context,
                    R.color.secondary_icon
                )
                val separatorColor = ContextCompat.getColor(
                    container.context,
                    R.color.primary_icon
                )

                display.urlFormatter =
                    if (UserPreferences(context).showUrlProtocol) {
                            url -> url
                    } else {
                            url -> toDisplayUrl(url)
                    }

                display.colors = display.colors.copy(
                    text = primaryTextColor,
                    securityIconSecure = primaryTextColor,
                    securityIconInsecure = primaryTextColor,
                    menu = primaryTextColor,
                    hint = secondaryTextColor,
                    separator = separatorColor,
                    trackingProtection = primaryTextColor
                )

                display.hint = context.getString(R.string.search)
            }

            val menuToolbar: ToolbarMenu
            BrowserMenu(
                context = this,
                store = components.store,
                onItemTapped = {
                    it.performHapticIfNeeded(view)
                    interactor.onBrowserToolbarMenuItemTapped(it)
                },
                lifecycleOwner = lifecycleOwner,
                isPinningSupported = isPinningSupported,
                shouldReverseItems = settings.toolbarPosition == ToolbarPosition.TOP.ordinal
            ).also { menuToolbar = it }

            view.display.setMenuDismissAction {
                view.invalidateActions()
            }

            toolbarIntegration = DefaultToolbarIntegration(
                    this,
                    view,
                    menuToolbar,
                    components.historyStorage,
                    lifecycleOwner,
                    sessionId = null,
                    isPrivate = components.store.state.selectedTab?.content?.private ?: false,
                    interactor = interactor,
                    engine = components.engine
                )
        }
    }

    fun expand() {
        // expand only for normal tabs and custom tabs not for PWA or TWA
        if (isPwaTabOrTwaTab) {
            return
        }

        (view.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            (behavior as? EngineViewScrollingBehavior)?.forceExpand(view)
        }
    }

    fun collapse() {
        // collapse only for normal tabs and custom tabs not for PWA or TWA. Mirror expand()
        if (isPwaTabOrTwaTab) {
            return
        }

        (view.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            (behavior as? EngineViewScrollingBehavior)?.forceCollapse(view)
        }
    }

    fun dismissMenu() {
        view.dismissMenu()
    }

    /**
     * Sets whether the toolbar will have a dynamic behavior (to be scrolled) or not.
     *
     * This will intrinsically check and disable the dynamic behavior if
     *  - this is disabled in app settings
     *  - toolbar is placed at the bottom and tab shows a PWA or TWA
     *
     *  Also if the user has not explicitly set a toolbar position and has a screen reader enabled
     *  the toolbar will be placed at the top and in a fixed position.
     *
     * @param shouldDisableScroll force disable of the dynamic behavior irrespective of the intrinsic checks.
     */
    fun setToolbarBehavior(shouldDisableScroll: Boolean = false) {
        when (settings.toolbarPosition) {
            ToolbarPosition.BOTTOM.ordinal -> {
                if (settings.hideBarWhileScrolling && !isPwaTabOrTwaTab) {
                    setDynamicToolbarBehavior(MozacToolbarPosition.BOTTOM)
                } else {
                    expandToolbarAndMakeItFixed()
                }
            }
            ToolbarPosition.TOP.ordinal -> {
                if (!settings.hideBarWhileScrolling ||
                    shouldDisableScroll
                ) {
                    expandToolbarAndMakeItFixed()
                } else {
                    setDynamicToolbarBehavior(MozacToolbarPosition.TOP)
                }
            }
        }
    }

    @VisibleForTesting
    internal fun expandToolbarAndMakeItFixed() {
        expand()
        (view.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            behavior = null
        }
    }

    @VisibleForTesting
    internal fun setDynamicToolbarBehavior(toolbarPosition: MozacToolbarPosition) {
        (view.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            behavior = EngineViewScrollingBehavior(view.context, null, toolbarPosition)
        }
    }

    @Suppress("ComplexCondition")
    private fun ToolbarMenu.Item.performHapticIfNeeded(view: View) {
        if (this is ToolbarMenu.Item.Reload && this.bypassCache ||
            this is ToolbarMenu.Item.Back && this.viewHistory ||
            this is ToolbarMenu.Item.Forward && this.viewHistory
        ) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}