package com.cookiejarapps.android.smartcookieweb.browser.tabs

import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import com.cookiejarapps.android.smartcookieweb.NavGraphDirections
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingMode
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingModeManager
import com.cookiejarapps.android.smartcookieweb.browser.HomepageChoice
import com.cookiejarapps.android.smartcookieweb.browser.home.HomeFragmentDirections
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.ext.nav
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import kotlinx.android.synthetic.main.fragment_tabstray.*
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.DefaultTabViewHolder
import mozilla.components.browser.tabstray.TabsAdapter
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.browser.tabstray.TabsTrayStyling
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
                    when (browsingModeManager.mode) {
                            BrowsingMode.Normal -> {
                                when(UserPreferences(requireContext()).homepageType){
                                    HomepageChoice.VIEW.ordinal -> {
                                        components.tabsUseCases.addTab.invoke(
                                            "about:homepage",
                                            selectTab = true
                                        )
                                    }
                                    HomepageChoice.BLANK_PAGE.ordinal -> {
                                        components.tabsUseCases.addTab.invoke(
                                            "about:blank",
                                            selectTab = true
                                        )
                                    }
                                    HomepageChoice.CUSTOM_PAGE.ordinal -> {
                                        components.tabsUseCases.addTab.invoke(
                                            UserPreferences(requireContext()).customHomepageUrl,
                                            selectTab = true
                                        )
                                    }
                                }
                            }
                            BrowsingMode.Private -> {
                                when(UserPreferences(requireContext()).homepageType){
                                    HomepageChoice.VIEW.ordinal -> {
                                        components.tabsUseCases.addPrivateTab.invoke(
                                            "about:homepage",
                                            selectTab = true
                                        )
                                    }
                                    HomepageChoice.BLANK_PAGE.ordinal -> {
                                        components.tabsUseCases.addPrivateTab.invoke(
                                            "about:blank",
                                            selectTab = true
                                        )
                                    }
                                    HomepageChoice.CUSTOM_PAGE.ordinal -> {
                                        components.tabsUseCases.addPrivateTab.invoke(
                                            UserPreferences(requireContext()).customHomepageUrl,
                                            selectTab = true
                                        )
                                    }
                                }
                        }
                    }
                    closeTabsTray()
                }
                R.id.removeTabs -> {
                   removeTabsDialog(view)
                }
            }
            true
        }

        val tabsAdapter = createTabsTray()

        tabsFeature.set(
            feature = TabsFeature(
                tabsTray = tabsAdapter,
                store = components.store,
                defaultTabsFilter = { it.filterFromConfig(configuration) },
                onCloseTray = ::closeTabsTray
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

        tabLayout.selectTab(tabLayout.getTabAt(browsingModeManager.mode.ordinal))
    }

    private fun removeTabsDialog(view: View) {
        val items = arrayOf(
            requireContext().resources.getString(R.string.close_current_tab),
            requireContext().resources.getString(R.string.close_other_tabs),
            requireContext().resources.getString(R.string.close_all_tabs),
            requireContext().resources.getString(R.string.close_app)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.mozac_feature_addons_remove))
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> {
                        components.store.state.selectedTabId?.let { id ->
                            components.tabsUseCases.removeTab(
                                id
                            )
                        }
                        if (view.context.components.store.state.tabs.isEmpty() && UserPreferences(
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
                        val tabList = components.store.state.tabs.toMutableList()
                        tabList.remove(components.store.state.selectedTab)
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

    private fun closeTabsTray() {
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        val tabsDrawer = activity?.findViewById<FrameLayout>(R.id.left_drawer)

        if (tabsDrawer != null) {
            drawerLayout?.closeDrawer(tabsDrawer)
        }
    }

    private fun createTabsTray(): TabsTray {
        val thumbnailLoader = ThumbnailLoader(components.thumbnailStorage)

        val adapter = TabListAdapter(
            thumbnailLoader = thumbnailLoader,
            delegate = object : TabsTray.Delegate {
                override fun onTabSelected(tab: TabSessionState, source: String?) {
                    components.tabsUseCases.selectTab(tab.id)
                    closeTabsTray()

                    if(tab.content.url == "about:homepage"){
                        requireContext().components.sessionUseCases.reload(tab.id)
                    }
                    else if (requireActivity().findNavController(R.id.container).currentDestination?.id == R.id.browserFragment) {
                        return
                    } else if (!requireActivity().findNavController(R.id.container).popBackStack(R.id.browserFragment, false)) {
                        requireActivity().findNavController(R.id.container).navigate(R.id.browserFragment)
                    }
                }

                override fun onTabClosed(tab: TabSessionState, source: String?) {
                    components.tabsUseCases.removeTab(tab.id)

                    if(tab.content.url == "about:homepage"){
                        requireContext().components.sessionUseCases.reload(tab.id)
                    }
                    else{
                        if (!requireActivity().findNavController(R.id.container).popBackStack(R.id.browserFragment, false)) {
                            requireActivity().findNavController(R.id.container).navigate(R.id.browserFragment)
                        }
                    }

                    if(requireActivity().components.store.state.tabs.isEmpty() && UserPreferences(requireActivity()).homepageType == HomepageChoice.VIEW.ordinal){
                        requireActivity().finish()
                    }

                    //TODO: Snackbar
                }
            }
        )

        tabsTray.adapter = adapter
        val layoutManager = if(UserPreferences(requireContext()).showTabsInGrid) GridLayoutManager(
            context,
            2
        ) else LinearLayoutManager(context)
        layoutManager.stackFromEnd = !UserPreferences(requireContext()).showTabsInGrid && UserPreferences(
            requireContext()
        ).stackFromBottom
        tabsTray.layoutManager = layoutManager

        return adapter
    }
}

private class SelectTabWithHomepageUseCase(
    private val actual: TabsUseCases.SelectTabUseCase,
    private val activity: BrowserActivity
) : TabsUseCases.SelectTabUseCase {
    override fun invoke(tabId: String) {
        actual(tabId)

        if(activity.components.store.state.findTabOrCustomTab(tabId)?.content?.url == "about:homepage"){
            activity.components.sessionUseCases.reload(activity.components.store.state.findTabOrCustomTab(tabId)?.id)
        }
        else if (activity.findNavController(R.id.container).currentDestination?.id == R.id.browserFragment) {
            return
        } else if (!activity.findNavController(R.id.container).popBackStack(R.id.browserFragment, false)) {
            activity.findNavController(R.id.container).navigate(R.id.browserFragment)
        }
    }
}

private class RemoveTabWithUndoUseCase(
    private val actual: TabsUseCases.RemoveTabUseCase,
    private val view: View,
    private val undo: TabsUseCases.UndoTabRemovalUseCase,
    private val activity: BrowserActivity
) : TabsUseCases.RemoveTabUseCase {
    override fun invoke(sessionId: String) {
        actual(sessionId)

        if(activity.components.store.state.selectedTab?.content?.url == "about:homepage"){
            activity.components.sessionUseCases.reload(activity.components.store.state.selectedTabId)
        }
        else{
            if (!activity.findNavController(R.id.container).popBackStack(R.id.browserFragment, false)) {
                activity.findNavController(R.id.container).navigate(R.id.browserFragment)
            }
        }

        if(activity.components.store.state.tabs.isEmpty() && UserPreferences(activity).homepageType == HomepageChoice.VIEW.ordinal){
            activity.finish()
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