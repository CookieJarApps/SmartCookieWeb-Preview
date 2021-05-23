package com.cookiejarapps.android.smartcookieweb.browser.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingMode
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingModeManager
import com.cookiejarapps.android.smartcookieweb.browser.HomepageChoice
import com.cookiejarapps.android.smartcookieweb.browser.home.HomeFragmentDirections
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import kotlinx.android.synthetic.main.fragment_tabstray.*
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper


// A fragment for displaying the tabs tray.

class TabsTrayFragment : Fragment() {
    private val tabsFeature: ViewBoundFeatureWrapper<TabsFeature> = ViewBoundFeatureWrapper()

    lateinit var browsingModeManager: BrowsingModeManager
    lateinit var configuration: Configuration

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_tabstray, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        browsingModeManager =  (activity as BrowserActivity).browsingModeManager
        configuration = Configuration(if (browsingModeManager.mode == BrowsingMode.Normal) BrowserTabType.NORMAL else BrowserTabType.PRIVATE)

        toolbar.inflateMenu(R.menu.tabstray_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.newTab -> {
                    if (UserPreferences(requireContext()).homepageType == HomepageChoice.VIEW.ordinal) {
                        findNavController().navigate(
                            HomeFragmentDirections.actionGlobalHome(
                                focusOnAddressBar = true
                            )
                        )
                    } else {
                        when (browsingModeManager.mode) {
                            BrowsingMode.Normal -> components.tabsUseCases.addTab.invoke(
                                "about:blank",
                                selectTab = true
                            )
                            BrowsingMode.Private -> components.tabsUseCases.addPrivateTab.invoke(
                                "about:blank",
                                selectTab = true
                            )
                        }
                    }
                    closeTabsTray()
                }
                R.id.removeTabs -> {
                    val items = arrayOf(
                        "Close current tab",
                        "Close other tabs",
                        "Close all tabs",
                        "Close browser"
                    )

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(resources.getString(R.string.mozac_feature_addons_remove))
                        .setItems(items) { dialog, which ->
                            when (which) {
                                0 -> {
                                    components.sessionManager.selectedSession?.id?.let { id ->
                                        components.tabsUseCases.removeTab(
                                            id
                                        )
                                    }
                                    if (view.context.components.sessionManager.sessions.isEmpty() && UserPreferences(
                                            requireContext()
                                        ).homepageType == HomepageChoice.VIEW.ordinal
                                    ) {
                                        findNavController().navigate(
                                            HomeFragmentDirections.actionGlobalHome(
                                                focusOnAddressBar = false
                                            )
                                        )
                                    }
                                    // TODO: this doesn't appear if the last tab is closed and bottom toolbar is on, as the toolbar view on the homepage is R.id.toolbarLayout
                                    val snackbar = Snackbar.make(
                                        view,
                                        view.resources.getString(R.string.tab_removed),
                                        Snackbar.LENGTH_LONG
                                    ).setAction(
                                        view.resources.getString(R.string.undo)
                                    ) {
                                        components.tabsUseCases.undo.invoke()
                                    }
                                    if(UserPreferences(requireContext()).shouldUseBottomToolbar) snackbar.anchorView =
                                        requireActivity().findViewById(R.id.toolbar)
                                        snackbar.show()
                                }
                                1 -> {
                                    val tabList = components.sessionManager.sessions.toMutableList()
                                    tabList.remove(components.sessionManager.selectedSession)
                                    val idList: MutableList<String> =
                                        emptyList<String>().toMutableList()
                                    for (i in tabList) idList.add(i.id)
                                    components.tabsUseCases.removeTabs.invoke(idList.toList())
                                }
                                2 -> {
                                    components.tabsUseCases.removeAllTabs.invoke()
                                    findNavController().navigate(
                                        HomeFragmentDirections.actionGlobalHome(
                                            focusOnAddressBar = false
                                        )
                                    )
                                }
                                3 -> {
                                    requireActivity().finishAndRemoveTask()
                                }
                            }
                        }
                        .show()
                }
            }
            true
        }

        val tabsAdapter = createTabsAdapter()
        tabsTray.adapter = tabsAdapter
        val layoutManager = if(UserPreferences(requireContext()).showTabsInGrid) GridLayoutManager(
            context,
            2
        ) else LinearLayoutManager(context)
        layoutManager.stackFromEnd = !UserPreferences(requireContext()).showTabsInGrid && UserPreferences(
            requireContext()
        ).stackFromBottom
        tabsTray.layoutManager = layoutManager

        tabsFeature.set(
            feature = TabsFeature(
                tabsTray = tabsAdapter,
                store = components.store,
                selectTabUseCase = SelectTabWithHomepageUseCase(
                    components.tabsUseCases.selectTab,
                    activity as BrowserActivity
                ),
                removeTabUseCase = RemoveTabWithUndoUseCase(
                    components.tabsUseCases.removeTab,
                    view,
                    components.tabsUseCases.undo,
                    requireActivity() as BrowserActivity
                ),
                defaultTabsFilter = { it.filterFromConfig(configuration) },
                closeTabsTray = ::closeTabsTray
            ),
            owner = this,
            view = view
        )

        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        browsingModeManager.mode = BrowsingMode.Normal
                        tabsFeature.get()?.filterTabs {
                            it.filterFromConfig(
                                Configuration(
                                    BrowserTabType.NORMAL
                                )
                            )
                        }
                    }
                    1 -> {
                        browsingModeManager.mode = BrowsingMode.Private
                        tabsFeature.get()?.filterTabs {
                            it.filterFromConfig(
                                Configuration(
                                    BrowserTabType.PRIVATE
                                )
                            )
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun closeTabsTray() {
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        val tabsDrawer = activity?.findViewById<FrameLayout>(R.id.left_drawer)

        if (tabsDrawer != null) {
            drawerLayout?.closeDrawer(tabsDrawer)
        }
    }

    private fun createTabsAdapter(): TabListAdapter {
        val thumbnailLoader = ThumbnailLoader(components.thumbnailStorage)
        return TabListAdapter(thumbnailLoader)
    }
}

private class SelectTabWithHomepageUseCase(
    private val actual: TabsUseCases.SelectTabUseCase,
    private val activity: BrowserActivity
) : TabsUseCases.SelectTabUseCase {
    override fun invoke(tabId: String) {
        if(UserPreferences(activity).homepageType == HomepageChoice.VIEW.ordinal) {
            activity.findNavController(R.id.container).navigate(
                R.id.browserFragment
            )
        }
        actual.invoke(tabId)
    }
}

private class RemoveTabWithUndoUseCase(
    private val actual: TabsUseCases.RemoveTabUseCase,
    private val view: View,
    private val undo: TabsUseCases.UndoTabRemovalUseCase,
    private val activity: BrowserActivity
) : TabsUseCases.RemoveTabUseCase {
    override fun invoke(sessionId: String) {
        actual.invoke(sessionId)
        if(view.context.components.sessionManager.sessions.isEmpty() && UserPreferences(activity).homepageType == HomepageChoice.VIEW.ordinal){
            activity.findNavController(R.id.container).navigate(
                R.id.homeFragment
            )
        }
        else{
            showSnackbar()
        }
    }

    private fun showSnackbar() {
        val snackbar = Snackbar.make(
            view,
            view.resources.getString(R.string.tab_removed),
            Snackbar.LENGTH_LONG
        ).setAction(
            view.resources.getString(R.string.undo)
        ) {
            undo.invoke()
        }
        if(UserPreferences(activity).shouldUseBottomToolbar) snackbar.anchorView = activity.findViewById(R.id.toolbar)
        snackbar.show()
    }
}

enum class BrowserTabType { NORMAL, PRIVATE }

data class Configuration(val browserTabType: BrowserTabType)

fun TabSessionState.filterFromConfig(configuration: Configuration): Boolean {
    val isPrivate = configuration.browserTabType == BrowserTabType.PRIVATE

    return content.private == isPrivate
}