package com.cookiejarapps.android.smartcookieweb.browser.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.cookiejarapps.android.smartcookieweb.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_tabstray.tabsTray
import kotlinx.android.synthetic.main.fragment_tabstray.toolbar
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import com.cookiejarapps.android.smartcookieweb.ext.components

// A fragment for displaying the tabs tray.

class TabsTrayFragment : Fragment() {
    private val tabsFeature: ViewBoundFeatureWrapper<TabsFeature> = ViewBoundFeatureWrapper()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_tabstray, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.tabstray_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.newTab -> {
                    components.tabsUseCases.addTab.invoke("about:blank", selectTab = true)
                    closeTabsTray()
                }
            }
            true
        }

        val tabsAdapter = createTabsAdapter()
        tabsTray.adapter = tabsAdapter
        tabsTray.layoutManager = GridLayoutManager(context, 1)

        tabsFeature.set(
            feature = TabsFeature(
                tabsTray = tabsAdapter,
                store = components.store,
                selectTabUseCase = components.tabsUseCases.selectTab,
                removeTabUseCase = RemoveTabWithUndoUseCase(
                    components.tabsUseCases.removeTab,
                    view,
                    components.tabsUseCases.undo
                ),
                closeTabsTray = ::closeTabsTray
            ),
            owner = this,
            view = view
        )
    }

    private fun closeTabsTray() {
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        val tabsDrawer = activity?.findViewById<FrameLayout>(R.id.left_drawer)

        if (tabsDrawer != null) {
            drawerLayout?.closeDrawer(tabsDrawer)
        }
    }

    private fun createTabsAdapter(): TabListAdapter {
        return TabListAdapter()
    }
}

private class RemoveTabWithUndoUseCase(
    private val actual: TabsUseCases.RemoveTabUseCase,
    private val view: View,
    private val undo: TabsUseCases.UndoTabRemovalUseCase
) : TabsUseCases.RemoveTabUseCase {
    override fun invoke(sessionId: String) {
        actual.invoke(sessionId)
        showSnackbar()
    }

    private fun showSnackbar() {
        Snackbar.make(
            view,
            view.resources.getString(R.string.tab_removed),
            Snackbar.LENGTH_LONG
        ).setAction(
            view.resources.getString(R.string.undo)
        ) {
            undo.invoke()
        }.show()
    }
}
