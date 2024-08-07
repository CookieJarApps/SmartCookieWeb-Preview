package com.cookiejarapps.android.smartcookieweb.components.toolbar

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.tabs.toolbar.TabCounterToolbarButton
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.ToolbarBehaviorController
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.feature.toolbar.ToolbarPresenter
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.view.hideKeyboard

@ExperimentalCoroutinesApi
abstract class ToolbarIntegration(
    context: Context,
    toolbar: BrowserToolbar,
    toolbarMenu: ToolbarMenu,
    sessionId: String?,
    isPrivate: Boolean,
    renderStyle: ToolbarFeature.RenderStyle
) : LifecycleAwareFeature {

    val store = context.components.store
    private val toolbarPresenter: ToolbarPresenter = ToolbarPresenter(
        toolbar,
        store,
        sessionId,
        false,
        ToolbarFeature.UrlRenderConfiguration(
            PublicSuffixList(context),
            ContextCompat.getColor(context, R.color.primary_icon),
            renderStyle = renderStyle
        )
    )

    private val toolbarController = ToolbarBehaviorController(toolbar, store, sessionId)

    private val menuPresenter =
        MenuPresenter(toolbar, context.components.store, sessionId)

    init {
        toolbar.display.menuBuilder = toolbarMenu.menuBuilder
        toolbar.private = isPrivate
    }

    override fun start() {
        menuPresenter.start()
        toolbarPresenter.start()
        toolbarController.start()
    }

    override fun stop() {
        menuPresenter.stop()
        toolbarPresenter.stop()
        toolbarController.stop()
    }

    fun invalidateMenu() {
        menuPresenter.invalidateActions()
    }
}

@ExperimentalCoroutinesApi
class DefaultToolbarIntegration(
    context: Context,
    toolbar: BrowserToolbar,
    toolbarMenu: ToolbarMenu,
    historyStorage: HistoryStorage,
    lifecycleOwner: LifecycleOwner,
    sessionId: String? = null,
    isPrivate: Boolean,
    interactor: BrowserToolbarViewInteractor,
    engine: Engine
) : ToolbarIntegration(
    context = context,
    toolbar = toolbar,
    toolbarMenu = toolbarMenu,
    sessionId = sessionId,
    isPrivate = isPrivate,
    renderStyle = ToolbarFeature.RenderStyle.UncoloredUrl
) {

    init {
        toolbar.display.menuBuilder = toolbarMenu.menuBuilder
        toolbar.private = isPrivate

        toolbar.display.indicators =
             listOf(
                    DisplayToolbar.Indicators.SECURITY,
                    DisplayToolbar.Indicators.EMPTY,
                    DisplayToolbar.Indicators.HIGHLIGHT
                )


        toolbar.display.colors = toolbar.display.colors.copy(
            securityIconInsecure = 0xFFd9534f.toInt(),
            securityIconSecure = 0xFF5cb85c.toInt(),
            text = context.getColorFromAttr(android.R.attr.textColorPrimary),
            menu = context.getColorFromAttr(android.R.attr.textColorPrimary),
            separator = 0x1E15141a,
            trackingProtection = 0xFF20123a.toInt(),
            emptyIcon = 0xFF20123a.toInt(),
            hint = 0x1E15141a
        )

        toolbar.edit.colors = toolbar.edit.colors.copy(
            text = context.getColorFromAttr(android.R.attr.textColorPrimary),
            clear = context.getColorFromAttr(android.R.attr.textColorPrimary),
            icon = context.getColorFromAttr(android.R.attr.textColorPrimary)
        )

        toolbar.display.setUrlBackground(AppCompatResources.getDrawable(context, R.drawable.toolbar_background))

        if(isPrivate) {
            toolbar.display.setUrlBackground(AppCompatResources.getDrawable(context, R.drawable.toolbar_background_private))
        }

        val tabsAction = TabCounterToolbarButton(
            lifecycleOwner = lifecycleOwner,
            showTabs = {
                toolbar.hideKeyboard()
                interactor.onTabCounterClicked()
            },
            store = store
        )

        val tabCount = if (isPrivate) {
            store.state.privateTabs.size
        } else {
            store.state.normalTabs.size
        }

        tabsAction.updateCount(tabCount)

        toolbar.addNavigationAction(tabsAction)
    }
}
