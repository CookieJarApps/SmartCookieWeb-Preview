package com.cookiejarapps.android.smartcookieweb.browser.tabs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.cookiejarapps.android.smartcookieweb.settings.HomepageChoice
import com.cookiejarapps.android.smartcookieweb.browser.home.HomeFragmentDirections
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentTabstrayBinding
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import mozilla.components.feature.tabs.tabstray.TabsFeature
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper


// A fragment for displaying the tabs tray.

class TabsTrayFragment : Fragment() {
    private val tabsFeature: ViewBoundFeatureWrapper<TabsFeature> = ViewBoundFeatureWrapper()

    lateinit var browsingModeManager: BrowsingModeManager
    lateinit var configuration: Configuration

    private var _binding: FragmentTabstrayBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTabstrayBinding.inflate(inflater, container, false)
        val view = binding.root

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        browsingModeManager = (activity as BrowserActivity).browsingModeManager
        configuration =
            Configuration(if (browsingModeManager.mode == BrowsingMode.Normal) BrowserTabType.NORMAL else BrowserTabType.PRIVATE)

        binding.toolbar.inflateMenu(R.menu.tabstray_menu)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.newTab -> {
                    when (binding.tabLayout.selectedTabPosition) {
                        0 -> {
                            browsingModeManager.mode = BrowsingMode.Normal
                            when (UserPreferences(requireContext()).homepageType) {
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

                        1 -> {
                            browsingModeManager.mode = BrowsingMode.Private
                            when (UserPreferences(requireContext()).homepageType) {
                                HomepageChoice.VIEW.ordinal -> {
                                    components.tabsUseCases.addTab.invoke(
                                        "about:homepage",
                                        selectTab = true,
                                        private = true
                                    )
                                }

                                HomepageChoice.BLANK_PAGE.ordinal -> {
                                    components.tabsUseCases.addTab.invoke(
                                        "about:blank",
                                        selectTab = true,
                                        private = true
                                    )
                                }

                                HomepageChoice.CUSTOM_PAGE.ordinal -> {
                                    components.tabsUseCases.addTab.invoke(
                                        UserPreferences(requireContext()).customHomepageUrl,
                                        selectTab = true,
                                        private = true
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

        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        tabsFeature.get()?.filterTabs {
                            it.filterFromConfig(
                                Configuration(
                                    BrowserTabType.NORMAL
                                )
                            )
                        }
                    }

                    1 -> {
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

        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(browsingModeManager.mode.ordinal))
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
                        val tabs = components.store.state.tabs
                        val tabIndex =
                            tabs.map { it.id }.indexOf(components.store.state.selectedTabId)

                        if (tabs.size > 1) {
                            val nextTab = if (tabIndex == 0) tabs[1] else tabs[tabIndex - 1]

                            if (nextTab.content.url == "about:homepage" && nextTab.content.url != components.store.state.selectedTab?.content?.url) {
                                requireContext().components.sessionUseCases.reload(nextTab.id)
                            } else if (nextTab.content.url != "about:homepage") {
                                requireActivity().findNavController(R.id.container)
                                    .navigate(R.id.browserFragment)
                            }
                            components.store.state.selectedTabId?.let {
                                components.tabsUseCases.removeTab(
                                    it
                                )
                            }
                        } else {
                            components.store.state.selectedTabId?.let {
                                components.tabsUseCases.removeTab(
                                    it
                                )
                            }
                            requireActivity().finishAndRemoveTask()
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
                        if (UserPreferences(requireContext()).shouldUseBottomToolbar) snackbar.anchorView =
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
                        requireActivity().finishAndRemoveTask()
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
        val tabsDrawer = if(UserPreferences(requireContext()).swapDrawers) requireActivity().findViewById<FrameLayout>(R.id.right_drawer) else requireActivity().findViewById<FrameLayout>(R.id.left_drawer)

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

                    if (tab.content.url == "about:homepage") {
                        // Homepage will not correctly set private / normal mode
                        if (tab.content.private && browsingModeManager.mode == BrowsingMode.Normal) {
                            browsingModeManager.mode = BrowsingMode.Private
                        } else if (!tab.content.private && browsingModeManager.mode == BrowsingMode.Private) {
                            browsingModeManager.mode = BrowsingMode.Normal
                        }
                        requireContext().components.sessionUseCases.reload(tab.id)
                    } else if (requireActivity().findNavController(R.id.container).currentDestination?.id == R.id.browserFragment) {
                        return
                    } else if (!requireActivity().findNavController(R.id.container)
                            .popBackStack(R.id.browserFragment, false)
                    ) {
                        requireActivity().findNavController(R.id.container)
                            .navigate(R.id.browserFragment)
                    }
                }

                override fun onTabClosed(tab: TabSessionState, source: String?) {
                    val tabs =
                        if (configuration.browserTabType == BrowserTabType.NORMAL) components.store.state.normalTabs else components.store.state.privateTabs
                    val tabIndex = tabs.map { it.id }.indexOf(tab.id)

                    if (tabs.size > 1 && components.store.state.selectedTabId == tab.id) {
                        val nextTab = if (tabIndex == 0) tabs[1] else tabs[tabIndex - 1]

                        if (nextTab.content.url == "about:homepage" && nextTab.content.url != tab.content.url) {
                            requireContext().components.sessionUseCases.reload(nextTab.id)
                        } else if (nextTab.content.url != "about:homepage") {
                            requireActivity().findNavController(R.id.container)
                                .navigate(R.id.browserFragment)
                        }
                        components.tabsUseCases.removeTab(tab.id)
                    } else if (tabs.size == 1 && configuration.browserTabType == BrowserTabType.NORMAL) {
                        components.tabsUseCases.removeTab(tab.id)
                        requireActivity().finishAndRemoveTask()
                    } else if (tabs.size == 1 && configuration.browserTabType == BrowserTabType.PRIVATE) {
                        components.tabsUseCases.removeTab(tab.id)
                        browsingModeManager.mode = BrowsingMode.Normal
                        val lastNormalTab = components.store.state.normalTabs.last()
                        components.tabsUseCases.selectTab(lastNormalTab.id)
                        // Update private / normal status
                        if (lastNormalTab.content.url == "about:homepage") {
                            requireContext().components.sessionUseCases.reload(lastNormalTab.id)
                        } else {
                            requireActivity().findNavController(R.id.container)
                                .navigate(R.id.browserFragment)
                        }
                    } else {
                        components.tabsUseCases.removeTab(tab.id)
                    }
                }
            }
        )

        binding.tabsTray.adapter = adapter
        val layoutManager = if (UserPreferences(requireContext()).showTabsInGrid) GridLayoutManager(
            context,
            2
        ) else LinearLayoutManager(context)
        if (UserPreferences(requireContext()).showTabsInGrid) layoutManager.reverseLayout =
            UserPreferences(
                requireContext()
            ).stackFromBottom
        else layoutManager.stackFromEnd = UserPreferences(
            requireContext()
        ).stackFromBottom
        binding.tabsTray.layoutManager = layoutManager

        return adapter
    }

    fun notifyBrowsingModeStateChanged() {
        browsingModeManager = (activity as BrowserActivity).browsingModeManager
        configuration =
            Configuration(if (browsingModeManager.mode == BrowsingMode.Normal) BrowserTabType.NORMAL else BrowserTabType.PRIVATE)

        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(browsingModeManager.mode.ordinal))
    }
}

enum class BrowserTabType { NORMAL, PRIVATE }

data class Configuration(val browserTabType: BrowserTabType)

fun TabSessionState.filterFromConfig(configuration: Configuration): Boolean {
    val isPrivate = configuration.browserTabType == BrowserTabType.PRIVATE

    return content.private == isPrivate
}
