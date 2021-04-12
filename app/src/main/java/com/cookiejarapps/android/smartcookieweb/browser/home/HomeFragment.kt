package com.cookiejarapps.android.smartcookieweb.browser.home

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Display.FLAG_SECURE
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
import androidx.constraintlayout.widget.ConstraintSet.TOP
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.room.Room
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingMode
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui.BookmarkFragment
import com.cookiejarapps.android.smartcookieweb.browser.shortcuts.ShortcutDatabase
import com.cookiejarapps.android.smartcookieweb.browser.shortcuts.ShortcutEntity
import com.cookiejarapps.android.smartcookieweb.browser.shortcuts.ShortcutGridAdapter
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.ext.nav
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.settings.activity.SettingsActivity
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.menu.view.MenuButton
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.behavior.ToolbarPosition
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import mozilla.components.ui.tabcounter.TabCounterMenu
import org.mozilla.gecko.util.ThreadUtils
import java.lang.ref.WeakReference

@ExperimentalCoroutinesApi
@Suppress("TooManyFunctions", "LargeClass")
class HomeFragment : Fragment() {
    private var database: ShortcutDatabase? = null

    private val args by navArgs<HomeFragmentArgs>()
    private lateinit var bundleArgs: Bundle

    //private val homeViewModel: HomeScreenViewModel by activityViewModels()

    private val snackbarAnchorView: View?
        get() = when (UserPreferences(requireContext()).toolbarPosition) {
            ToolbarPosition.BOTTOM.ordinal -> toolbarLayout
            else -> null
        }

    private val browsingModeManager get() = (activity as BrowserActivity).browsingModeManager

    private val store: BrowserStore
        get() = components.store

    private var appBarLayout: AppBarLayout? = null

    @VisibleForTesting
    internal var getMenuButton: () -> MenuButton? = { menuButton }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bundleArgs = args.toBundle()
    }

    @Suppress("LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val activity = activity as BrowserActivity
        val components = requireContext().components

        activity.supportFragmentManager.beginTransaction().apply {
            replace(R.id.right_drawer, BookmarkFragment())
            commit()
        }

        updateLayout(view)

        if(!UserPreferences(requireContext()).shortcutDrawerOpen){
            view.shortcut_name.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_shortcuts, 0, R.drawable.ic_baseline_chevron_up, 0)
            view.shortcut_grid.visibility = View.GONE
        }

        view.shortcut_name.setOnClickListener {
            if(UserPreferences(requireContext()).shortcutDrawerOpen){
                UserPreferences(requireContext()).shortcutDrawerOpen = false
                view.shortcut_name.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_shortcuts, 0, R.drawable.ic_baseline_chevron_up, 0)
                view.shortcut_grid.visibility = View.GONE
            }
            else{
                UserPreferences(requireContext()).shortcutDrawerOpen = true
                view.shortcut_name.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_shortcuts, 0, R.drawable.ic_baseline_chevron_down, 0)
                view.shortcut_grid.visibility = View.VISIBLE
            }
        }

        GlobalScope.launch {
            database = Room.databaseBuilder(
                requireContext(),
                ShortcutDatabase::class.java, "shortcut-database"
            ).build()

            val shortcutDao = database?.shortcutDao()
            val shortcuts: MutableList<ShortcutEntity> = shortcutDao?.getAll() as MutableList

            val adapter = ShortcutGridAdapter(requireContext(), getList(shortcuts))

            ThreadUtils.runOnUiThread {
                view.shortcut_grid.adapter = adapter
            }
        }

        view.shortcut_grid.setOnItemClickListener { _, _, position, _ ->
            if((view.shortcut_grid.adapter.getItem(position) as ShortcutEntity).add){
                showCreateShortcutDialog(view.shortcut_grid.adapter as ShortcutGridAdapter)
            }
            else{
                findNavController().navigate(
                    R.id.browserFragment
                )

                if (browsingModeManager.mode == BrowsingMode.Normal) {
                    components.tabsUseCases.addTab.invoke(
                        (view.shortcut_grid.adapter.getItem(position) as ShortcutEntity).url!!,
                        selectTab = true
                    )
                }
                else{
                    components.tabsUseCases.addPrivateTab.invoke(
                        (view.shortcut_grid.adapter.getItem(position) as ShortcutEntity).url!!,
                        selectTab = true
                    )
                }
            }
        }

        view.shortcut_grid.setOnItemLongClickListener { _, _, position, _ ->
            if(position == view.shortcut_grid.adapter.count - 1){
                return@setOnItemLongClickListener true
            }

            val items = arrayOf(resources.getString(R.string.edit_shortcut), resources.getString(R.string.delete_shortcut))

            AlertDialog.Builder(requireContext())
                .setTitle(resources.getString(R.string.edit_shortcut))
                .setItems(items) { _, which ->
                    when(which){
                        0 -> showEditShortcutDialog(position, view.shortcut_grid.adapter as ShortcutGridAdapter)
                        1 -> deleteShortcut(view.shortcut_grid.adapter.getItem(which) as ShortcutEntity, view.shortcut_grid.adapter as ShortcutGridAdapter)
                    }
                }
                .show()

            return@setOnItemLongClickListener true
        }

        /*sessionControlView = SessionControlView(
            view.sessionControlRecyclerView,
            viewLifecycleOwner,
            sessionControlInteractor,
            homeViewModel
        )

        updateSessionControlView(view)*/

        appBarLayout = view.homeAppBar

        //activity.themeManager.applyStatusBarTheme(activity)
        return view
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        getMenuButton()?.dismissMenu()
    }

    /*private fun dismissTip(tip: Tip) {
        sessionControlInteractor.onCloseTip(tip)
    }*/

    /**
     * Returns a [TopSitesConfig] which specifies how many top sites to display and whether or
     * not frequently visited sites should be displayed.
     */
    /* @VisibleForTesting
     internal fun getTopSitesConfig(): TopSitesConfig {
         val settings = requireContext().settings()
         return TopSitesConfig(
             settings.topSitesMaxLimit,
             if (settings.showTopFrecentSites) FrecencyThresholdOption.SKIP_ONE_TIME_PAGES else null
         )
     }*/

    /**
     * The [SessionControlView] is forced to update with our current state when we call
     * [HomeFragment.onCreateView] in order to be able to draw everything at once with the current
     * data in our store. The [View.consumeFrom] coroutine dispatch
     * doesn't get run right away which means that we won't draw on the first layout pass.
     */
    /*private fun updateSessionControlView(view: View) {
        if (browsingModeManager.mode == BrowsingMode.Private) {
            view.consumeFrom(homeFragmentStore, viewLifecycleOwner) {
                sessionControlView?.update(it)
            }
        } else {
            sessionControlView?.update(homeFragmentStore.state)

            view.consumeFrom(homeFragmentStore, viewLifecycleOwner) {
                sessionControlView?.update(it)
            }
        }
    }*/

    private fun getList(shortcutEntity: MutableList<ShortcutEntity>): MutableList<ShortcutEntity> {
        shortcutEntity.add(shortcutEntity.size, ShortcutEntity(url = "test", add = true))
        return shortcutEntity
    }

    private fun showEditShortcutDialog(position: Int, adapter: ShortcutGridAdapter){
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.edit_shortcut))
        val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.add_shortcut_dialog, view as ViewGroup?, false)
        val input = viewInflated.findViewById<View>(R.id.urlEditText) as EditText
        input.setText(adapter.list[position].url)
        builder.setView(viewInflated)

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            val item = adapter.list[position]
            item.url = input.text.toString()
            adapter.notifyDataSetChanged()

            GlobalScope.launch {
                database?.shortcutDao()?.update(item)
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showCreateShortcutDialog(adapter: ShortcutGridAdapter){
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.add_shortcut))
        val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.add_shortcut_dialog, view as ViewGroup?, false)
        val input = viewInflated.findViewById<View>(R.id.urlEditText) as EditText
        builder.setView(viewInflated)

        builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
            val list = adapter.list
            list.add(ShortcutEntity(url = input.text.toString()))
            list.removeAt(adapter.list.size - 2)
            adapter.list = getList(list)
            adapter.notifyDataSetChanged()

            GlobalScope.launch {
                database?.shortcutDao()?.insertAll(ShortcutEntity(url = input.text.toString()))
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun deleteShortcut(shortcutEntity: ShortcutEntity, adapter: ShortcutGridAdapter) {
        adapter.list.remove(shortcutEntity)
        adapter.notifyDataSetChanged()

        GlobalScope.launch {
            database?.shortcutDao()?.delete(shortcutEntity)
        }
    }

    private fun updateLayout(view: View) {
        when (UserPreferences(view.context).toolbarPosition) {
            ToolbarPosition.TOP.ordinal -> {
                view.toolbarLayout.layoutParams = CoordinatorLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP
                }

                ConstraintSet().apply {
                    clone(view.toolbarLayout)
                    clear(view.bottom_bar.id, BOTTOM)
                    clear(view.bottomBarShadow.id, BOTTOM)
                    connect(view.bottom_bar.id, TOP, PARENT_ID, TOP)
                    connect(view.bottomBarShadow.id, TOP, view.bottom_bar.id, BOTTOM)
                    connect(view.bottomBarShadow.id, BOTTOM, PARENT_ID, BOTTOM)
                    applyTo(view.toolbarLayout)
                }

                view.homeAppBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin =
                        resources.getDimensionPixelSize(R.dimen.home_fragment_top_toolbar_header_margin)
                }
            }
            ToolbarPosition.BOTTOM.ordinal -> {
            }
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeSearchEngineChanges()
        createHomeMenu(requireContext(), WeakReference(view.menuButton))
        createTabCounterMenu(view)

        view.menuButton.setColorFilter(
            /*ContextCompat.getColor(
                requireContext(),
                ThemeManager.resolveAttribute(R.attr.primaryText, requireContext())
            )*/
        Color.BLACK
        )

        //view.toolbar.compoundDrawablePadding =
        //    view.resources.getDimensionPixelSize(R.dimen.search_bar_search_engine_icon_padding)
        view.toolbar_wrapper.setOnClickListener {
            navigateToSearch()
        }

        view.toolbar_wrapper.setOnLongClickListener {
            /*ToolbarPopupWindow.show(
                WeakReference(it),
                handlePasteAndGo = sessionControlInteractor::onPasteAndGo,
                handlePaste = sessionControlInteractor::onPaste,
                copyVisible = false
            )*/
            true
        }

        view.tab_button.setOnClickListener {
            openTabDrawer()
        }

        /*PrivateBrowsingButtonView(
            privateBrowsingButton,
            browsingModeManager
        ) { newMode ->
            if (newMode == BrowsingMode.Private) {
                requireContext().settings().incrementNumTimesPrivateModeOpened()
            }

            if (onboarding.userHasBeenOnboarded()) {
                homeFragmentStore.dispatch(
                    HomeFragmentAction.ModeChange(Mode.fromBrowsingMode(newMode))
                )
            }
        }*/

        if (browsingModeManager.mode.isPrivate) {
            requireActivity().window.addFlags(FLAG_SECURE)
        } else {
            requireActivity().window.clearFlags(FLAG_SECURE)
        }

        /*consumeFrom(requireComponents.core.store) {
            updateTabCounter(it)
        }*/

        /*homeViewModel.sessionToDelete?.also {
            if (it == ALL_NORMAL_TABS || it == ALL_PRIVATE_TABS) {
                removeAllTabsAndShowSnackbar(it)
            } else {
                removeTabAndShowSnackbar(it)
            }
        }*/

        //homeViewModel.sessionToDelete = null

        updateTabCounter(components.store.state)

        if (bundleArgs.getBoolean(FOCUS_ON_ADDRESS_BAR)) {
            navigateToSearch()
        } else if (bundleArgs.getLong(FOCUS_ON_COLLECTION, -1) >= 0) {
            // No need to scroll to async'd loaded TopSites if we want to scroll to collections.
            //homeViewModel.shouldScrollToTopSites = false
            /* Triggered when the user has added a tab to a collection and has tapped
            * the View action on the [TabsTrayDialogFragment] snackbar.*/
        }
    }

    private fun observeSearchEngineChanges() {
        consumeFlow(store) { flow ->
            flow.map { state -> state.search.selectedOrDefaultSearchEngine }
                .ifChanged()
                .collect { searchEngine ->
                    if (searchEngine != null) {
                        val iconSize =
                            requireContext().resources.getDimensionPixelSize(R.dimen.icon_width)
                        val searchIcon =
                            BitmapDrawable(requireContext().resources, searchEngine.icon)
                        searchIcon.setBounds(0, 0, iconSize, iconSize)
                        search_engine_icon?.setImageDrawable(searchIcon)
                    } else {
                        search_engine_icon.setImageDrawable(null)
                    }
                }
        }
    }

    private fun createTabCounterMenu(view: View) {
        val browsingModeManager = (activity as BrowserActivity).browsingModeManager
        val mode = browsingModeManager.mode

        val onItemTapped: (TabCounterMenu.Item) -> Unit = {
            if (it is TabCounterMenu.Item.NewTab) {
                browsingModeManager.mode = BrowsingMode.Normal
            } else if (it is TabCounterMenu.Item.NewPrivateTab) {
                browsingModeManager.mode = BrowsingMode.Private
            }
        }

        /*val tabCounterMenu = FenixTabCounterMenu(
            view.context,
            onItemTapped,
            iconColor = if (mode == BrowsingMode.Private) {
                ContextCompat.getColor(requireContext(), R.color.primary_text_private_theme)
            } else {
                null
            }
        )*/

        val inverseBrowsingMode = when (mode) {
            BrowsingMode.Normal -> BrowsingMode.Private
            BrowsingMode.Private -> BrowsingMode.Normal
        }

        //tabCounterMenu.updateMenu(showOnly = inverseBrowsingMode)
        view.tab_button.setOnLongClickListener {
            //tabCounterMenu.menuController.show(anchor = it)
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        //_sessionControlInteractor = null
        //sessionControlView = null
        appBarLayout = null
        bundleArgs.clear()
        requireActivity().window.clearFlags(FLAG_SECURE)
    }

    override fun onStart() {
        super.onStart()

        //subscribeToTabCollections()

        val context = requireContext()
        val components = context.components

        /*homeFragmentStore.dispatch(
            HomeFragmentAction.Change(
                mode = currentMode.getCurrentMode()
            )
        )*/

        if (browsingModeManager.mode.isPrivate &&
            // We will be showing the search dialog and don't want to show the CFR while the dialog shows
            !bundleArgs.getBoolean(FOCUS_ON_ADDRESS_BAR)
            //&&
            //context.settings().shouldShowPrivateModeCfr
        ) {
            recommendPrivateBrowsingShortcut()
        }
    }

    override fun onResume() {
        super.onResume()
        if (browsingModeManager.mode == BrowsingMode.Private) {
            //activity?.window?.setBackgroundDrawableResource(R.drawable.private_home_background_gradient)
        }

        //hideToolbar()
    }

    override fun onPause() {
        super.onPause()
        if (browsingModeManager.mode == BrowsingMode.Private) {
            activity?.window?.setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.photonPurple50
                    )
                )
            )
        }
    }

    private fun recommendPrivateBrowsingShortcut() {
        /*context?.let { context ->
            val layout = LayoutInflater.from(context)
                .inflate(R.layout.pbm_shortcut_popup, null)
            val privateBrowsingRecommend =
                PopupWindow(
                    layout,
                    min(
                        (resources.displayMetrics.widthPixels / CFR_WIDTH_DIVIDER).toInt(),
                        (resources.displayMetrics.heightPixels / CFR_WIDTH_DIVIDER).toInt()
                    ),
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    true
                )
            layout.findViewById<Button>(R.id.cfr_pos_button).apply {
                setOnClickListener {
                    PrivateShortcutCreateManager.createPrivateShortcut(context)
                    privateBrowsingRecommend.dismiss()
                }
            }
            layout.findViewById<Button>(R.id.cfr_neg_button).apply {
                setOnClickListener {
                    privateBrowsingRecommend.dismiss()
                }
            }
            // We want to show the popup only after privateBrowsingButton is available.
            // Otherwise, we will encounter an activity token error.
            privateBrowsingButton.post {
                runIfFragmentIsAttached {
                    context.settings().showedPrivateModeContextualFeatureRecommender = true
                    context.settings().lastCfrShownTimeInMillis = System.currentTimeMillis()
                    privateBrowsingRecommend.showAsDropDown(
                        privateBrowsingButton, 0, CFR_Y_OFFSET, Gravity.TOP or Gravity.END
                    )
                }
            }
        }*/
    }

    private fun hideOnboardingIfNeeded() {
        /*if (!onboarding.userHasBeenOnboarded()) {
            onboarding.finish()
            homeFragmentStore.dispatch(
                HomeFragmentAction.ModeChange(
                    mode = currentMode.getCurrentMode()
                )
            )
        }*/
    }


    private fun navigateToSearch() {
        // Dismisses the search dialog when the home content is scrolled
        // val recyclerView = sessionControlView!!.view
        /* val listener = object : RecyclerView.OnScrollListener() {
             override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                 super.onScrollStateChanged(recyclerView, newState)
                 if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                     findNavController().navigateUp()
                     recyclerView.removeOnScrollListener(this)
                 }
             }
         }*/

        //recyclerView.addOnScrollListener(listener)

        val directions =
            HomeFragmentDirections.actionGlobalSearchDialog(
                sessionId = null
            )

        // TODO: OPTIONS
        nav(R.id.homeFragment, directions, null)
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    private fun createHomeMenu(context: Context, menuButtonView: WeakReference<MenuButton>) =
        HomeMenu(
            this.viewLifecycleOwner,
            context,
            onItemTapped = {
                when (it) {
                    HomeMenu.Item.Settings -> {
                        hideOnboardingIfNeeded()

                        val settings = Intent(activity, SettingsActivity::class.java)
                        settings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        requireActivity().startActivity(settings)
                    }
                    HomeMenu.Item.Bookmarks -> {
                        hideOnboardingIfNeeded()
                       /* nav(
                            R.id.homeFragment,
                            HomeFragmentDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id)
                        )*/
                    }
                    HomeMenu.Item.History -> {
                        hideOnboardingIfNeeded()
                        /*nav(
                            R.id.homeFragment,
                            HomeFragmentDirections.actionGlobalHistoryFragment()
                        )*/
                    }

                    HomeMenu.Item.Downloads -> {
                        hideOnboardingIfNeeded()
                        /*nav(
                            R.id.homeFragment,
                            HomeFragmentDirections.actionGlobalDownloadsFragment()
                        )*/
                    }

                    HomeMenu.Item.Help -> {
                        hideOnboardingIfNeeded()
                        /*(activity as HomeActivity).openToBrowserAndLoad(
                            searchTermOrURL = SupportUtils.getSumoURLForTopic(context, HELP),
                            newTab = true,
                            from = BrowserDirection.FromHome
                        )*/
                    }
                    HomeMenu.Item.WhatsNew -> {
                        hideOnboardingIfNeeded()
                        /*WhatsNew.userViewedWhatsNew(context)
                        (activity as HomeActivity).openToBrowserAndLoad(
                            searchTermOrURL = SupportUtils.getWhatsNewUrl(context),
                            newTab = true,
                            from = BrowserDirection.FromHome
                        )*/
                    }
                    HomeMenu.Item.AddonsManager -> {
                        /*nav(
                            R.id.homeFragment,
                            HomeFragmentDirections.actionGlobalAddonsManagementFragment()
                        )*/
                    }
                }
            },
            onHighlightPresent = { menuButtonView.get()?.setHighlight(it) },
            onMenuBuilderChanged = { menuButtonView.get()?.menuBuilder = it }
        )

    private fun openTabDrawer() {
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        val tabsDrawer = activity?.findViewById<FrameLayout>(R.id.left_drawer)

        if (tabsDrawer != null) {
            drawerLayout?.openDrawer(tabsDrawer)
        }
    }

    private fun updateTabCounter(browserState: BrowserState) {
        val tabCount = if (browsingModeManager.mode.isPrivate) {
            browserState.privateTabs.size
        } else {
            browserState.normalTabs.size
        }

        view?.tab_button?.setCountWithAnimation(tabCount)
    }

    companion object {
        private const val FOCUS_ON_ADDRESS_BAR = "focusOnAddressBar"
        private const val FOCUS_ON_COLLECTION = "focusOnCollection"
    }
}
