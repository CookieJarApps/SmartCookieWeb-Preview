package com.cookiejarapps.android.smartcookieweb

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.cookiejarapps.android.smartcookieweb.*
import com.cookiejarapps.android.smartcookieweb.addons.WebExtensionPromptFeature
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingMode
import com.cookiejarapps.android.smartcookieweb.settings.HomepageChoice
import com.cookiejarapps.android.smartcookieweb.browser.SwipeGestureLayout
import com.cookiejarapps.android.smartcookieweb.browser.home.HomeFragmentDirections
import com.cookiejarapps.android.smartcookieweb.components.StoreProvider
import com.cookiejarapps.android.smartcookieweb.components.toolbar.*
import com.cookiejarapps.android.smartcookieweb.components.toolbar.ToolbarPosition
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentBrowserBinding
import com.cookiejarapps.android.smartcookieweb.downloads.DownloadService
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.integration.ContextMenuIntegration
import com.cookiejarapps.android.smartcookieweb.integration.FindInPageIntegration
import com.cookiejarapps.android.smartcookieweb.integration.ReaderModeIntegration
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.ssl.showSslDialog
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.thumbnails.BrowserThumbnails
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.app.links.AppLinksFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.feature.intent.ext.EXTRA_SESSION_ID
import mozilla.components.feature.media.fullscreen.MediaSessionFullscreenFeature
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.search.SearchFeature
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.session.PictureInPictureFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SwipeRefreshFeature
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.base.log.logger.Logger.Companion.debug
import mozilla.components.support.ktx.android.view.enterImmersiveMode
import mozilla.components.support.ktx.android.view.exitImmersiveMode
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import mozilla.components.support.locale.ActivityContextWrapper
import mozilla.components.support.utils.ext.requestInPlacePermissions
import com.cookiejarapps.android.smartcookieweb.browser.home.SharedViewModel
import java.lang.ref.WeakReference
import mozilla.components.ui.widgets.behavior.EngineViewClippingBehavior as OldEngineViewClippingBehavior
import mozilla.components.ui.widgets.behavior.ToolbarPosition as OldToolbarPosition

/**
 * Base fragment extended by [BrowserFragment].
 * This class only contains shared code focused on the main browsing content.
 * UI code specific to the app or to custom tabs can be found in the subclasses.
 */
@ExperimentalCoroutinesApi
@Suppress("TooManyFunctions", "LargeClass")
abstract class BaseBrowserFragment : Fragment(), UserInteractionHandler, ActivityResultHandler, AccessibilityManager.AccessibilityStateChangeListener {

    private lateinit var browserFragmentStore: BrowserFragmentStore
    private lateinit var browserAnimator: BrowserAnimator

    private var _browserInteractor: BrowserToolbarViewInteractor? = null
    protected val browserInteractor: BrowserToolbarViewInteractor
        get() = _browserInteractor!!

    @VisibleForTesting
    @Suppress("VariableNaming")
    internal var _browserToolbarView: BrowserToolbarView? = null
    @VisibleForTesting
    internal val browserToolbarView: BrowserToolbarView
        get() = _browserToolbarView!!

    protected val thumbnailsFeature = ViewBoundFeatureWrapper<BrowserThumbnails>()

    private val sessionFeature = ViewBoundFeatureWrapper<SessionFeature>()
    private val contextMenuIntegration = ViewBoundFeatureWrapper<ContextMenuIntegration>()
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()
    private val appLinksFeature = ViewBoundFeatureWrapper<AppLinksFeature>()
    private val promptsFeature = ViewBoundFeatureWrapper<PromptFeature>()
    private val findInPageIntegration = ViewBoundFeatureWrapper<FindInPageIntegration>()
    private val toolbarIntegration = ViewBoundFeatureWrapper<ToolbarIntegration>()
    private val sitePermissionsFeature = ViewBoundFeatureWrapper<SitePermissionsFeature>()
    private val fullScreenFeature = ViewBoundFeatureWrapper<FullScreenFeature>()
    private val swipeRefreshFeature = ViewBoundFeatureWrapper<SwipeRefreshFeature>()
    private val webExtensionPromptFeature = ViewBoundFeatureWrapper<WebExtensionPromptFeature>()
    private var fullScreenMediaSessionFeature =
        ViewBoundFeatureWrapper<MediaSessionFullscreenFeature>()
    private val searchFeature = ViewBoundFeatureWrapper<SearchFeature>()
    private var pipFeature: PictureInPictureFeature? = null
    val readerViewFeature = ViewBoundFeatureWrapper<ReaderModeIntegration>()

    var customTabSessionId: String? = null

    @VisibleForTesting
    internal var browserInitialized: Boolean = false
    private var initUIJob: Job? = null
    protected var webAppToolbarShouldBeVisible = true

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private var _binding: FragmentBrowserBinding? = null
    protected val binding get() = _binding!!

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        customTabSessionId = requireArguments().getString(EXTRA_SESSION_ID)

        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        val view = binding.root

        val activity = activity as BrowserActivity
        val originalContext = ActivityContextWrapper.getOriginalContext(activity)
        binding.engineView.setActivityContext(originalContext)

        browserFragmentStore = StoreProvider.get(this) {
            BrowserFragmentStore(
                BrowserFragmentState()
            )
        }

        return view
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            initializeUI(view)

            if (customTabSessionId == null) {
                // We currently only need this observer to navigate to home
                // in case all tabs have been removed on startup. No need to
                // this if we have a known session to display.
                observeRestoreComplete(requireContext().components.store, findNavController())
            }

            observeTabSelection(requireContext().components.store)
        }

    private fun initializeUI(view: View) {
        val tab = getCurrentTab()
        browserInitialized = if (tab != null) {
            initializeUI(view, tab)
            true
        } else {
            false
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    @CallSuper
    internal open fun initializeUI(view: View, tab: SessionState) {
        val context = requireContext()
        val store = context.components.store
        val activity = requireActivity() as BrowserActivity

        val toolbarHeight = resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)

        browserAnimator = BrowserAnimator(
            fragment = WeakReference(this),
            engineView = WeakReference(binding.engineView),
            swipeRefresh = WeakReference(binding.swipeRefresh),
            viewLifecycleScope = WeakReference(viewLifecycleOwner.lifecycleScope)
        ).apply {
            beginAnimateInIfNecessary()
        }

        val openInFenixIntent = Intent(context, IntentReceiverActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(BrowserActivity.OPEN_TO_BROWSER, true)
        }

        val browserToolbarController = DefaultBrowserToolbarController(
            store = store,
            activity = activity,
            navController = findNavController(),
            engineView = binding.engineView,
            customTabSessionId = customTabSessionId,
            onTabCounterClicked = {
                thumbnailsFeature.get()?.requestScreenshot()

                val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawer_layout)
                val tabDrawer = if(UserPreferences(activity).swapDrawers) activity.findViewById<FrameLayout>(R.id.right_drawer) else activity.findViewById<FrameLayout>(R.id.left_drawer)

                if (tabDrawer != null) {
                    drawerLayout?.openDrawer(tabDrawer)
                }
            }
        )
        val browserToolbarMenuController = DefaultBrowserToolbarMenuController(
            activity = activity,
            navController = findNavController(),
            findInPageLauncher = { findInPageIntegration.withFeature { it.launch() } },
            browserAnimator = browserAnimator,
            customTabSessionId = customTabSessionId,
            store = store,
        )

        _browserInteractor = BrowserInteractor(
            browserToolbarController,
            browserToolbarMenuController
        )

        _browserToolbarView = BrowserToolbarView(
            container = binding.browserLayout,
            toolbarPosition = if(UserPreferences(context).toolbarPosition == ToolbarPosition.BOTTOM.ordinal) ToolbarPosition.BOTTOM else ToolbarPosition.TOP,
            interactor = browserInteractor,
            customTabSession = customTabSessionId?.let { store.state.findCustomTab(it) },
            lifecycleOwner = viewLifecycleOwner
        )

        toolbarIntegration.set(
            feature = browserToolbarView.toolbarIntegration,
            owner = this,
            view = view
        )

        findInPageIntegration.set(
            feature = FindInPageIntegration(
                store = store,
                sessionId = customTabSessionId,
                stub = binding.stubFindInPage,
                engineView = binding.engineView,
                toolbarInfo = FindInPageIntegration.ToolbarInfo(
                    browserToolbarView.view,
                    UserPreferences(context).hideBarWhileScrolling,
                    !UserPreferences(context).shouldUseBottomToolbar
                )
            ),
            owner = this,
            view = view
        )

        contextMenuIntegration.set(
            feature = ContextMenuIntegration(
                context = requireContext(),
                fragmentManager = parentFragmentManager,
                browserStore = components.store,
                tabsUseCases = components.tabsUseCases,
                contextMenuUseCases = components.contextMenuUseCases,
                parentView = view,
                sessionId = customTabSessionId
            ),
            owner = this,
            view = view
        )

        readerViewFeature.set(
            feature = ReaderModeIntegration(
                requireContext(),
                components.engine,
                components.store,
                browserToolbarView.view,
                binding.readerViewBar,
                binding.readerViewAppearanceButton
            ),
            owner = this,
            view = view
        )

        promptsFeature.set(
            PromptFeature(
                fragment = this,
                store = components.store,
                tabsUseCases = components.tabsUseCases,
                fragmentManager = parentFragmentManager,
                fileUploadsDirCleaner = components.fileUploadsDirCleaner,
                onNeedToRequestPermissions = { permissions ->
                    requestInPlacePermissions(REQUEST_KEY_PROMPT_PERMISSIONS, permissions) { result ->
                        promptsFeature.get()?.onPermissionsResult(
                            result.keys.toTypedArray(),
                            result.values.map {
                                when (it) {
                                    true -> PackageManager.PERMISSION_GRANTED
                                    false -> PackageManager.PERMISSION_DENIED
                                }
                            }.toIntArray(),
                        )
                    }
                },
            ),
            this,
            view,
        )

        fullScreenMediaSessionFeature.set(
            feature = MediaSessionFullscreenFeature(
                requireActivity(),
                context.components.store,
                customTabSessionId
            ),
            owner = this,
            view = view
        )

        pipFeature = PictureInPictureFeature(
            store = store,
            activity = requireActivity(),
            tabId = customTabSessionId
        )

        appLinksFeature.set(
            feature = AppLinksFeature(
                context,
                store = store,
                sessionId = customTabSessionId,
                fragmentManager = parentFragmentManager,
                launchInApp = { UserPreferences(context).launchInApp },
                loadUrlUseCase = context.components.sessionUseCases.loadUrl
            ),
            owner = this,
            view = view
        )

        sessionFeature.set(
            feature = SessionFeature(
                requireContext().components.store,
                requireContext().components.sessionUseCases.goBack,
                requireContext().components.sessionUseCases.goForward,
                binding.engineView,
                customTabSessionId
            ),
            owner = this,
            view = view
        )

        searchFeature.set(
            feature = SearchFeature(store, customTabSessionId) { request, tabId ->
                val parentSession = store.state.findTabOrCustomTab(tabId)
                val useCase = if (request.isPrivate) {
                    requireContext().components.searchUseCases.newPrivateTabSearch
                } else {
                    requireContext().components.searchUseCases.newTabSearch
                }

                if (parentSession is CustomTabSessionState) {
                    useCase.invoke(request.query)
                    requireActivity().startActivity(openInFenixIntent)
                } else {
                    useCase.invoke(request.query, parentSessionId = parentSession?.id)
                }
            },
            owner = this,
            view = view
        )

        val accentHighContrastColor = R.color.secondary_icon

        sitePermissionsFeature.set(
            feature = SitePermissionsFeature(
                context = context,
                storage = context.components.permissionStorage,
                fragmentManager = parentFragmentManager,
                promptsStyling = SitePermissionsFeature.PromptsStyling(
                    gravity = getAppropriateLayoutGravity(),
                    shouldWidthMatchParent = true,
                    positiveButtonBackgroundColor = accentHighContrastColor,
                    positiveButtonTextColor = R.color.photonWhite
                ),
                sessionId = customTabSessionId,
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_APP_PERMISSIONS)
                },
                onShouldShowRequestPermissionRationale = {
                    shouldShowRequestPermissionRationale(
                        it
                    )
                },
                store = store
            ),
            owner = this,
            view = view
        )

        downloadsFeature.set(
            feature = DownloadsFeature(
                requireContext().applicationContext,
                store = components.store,
                useCases = components.downloadsUseCases,
                fragmentManager = childFragmentManager,
                shouldForwardToThirdParties = { UserPreferences(requireContext()).promptExternalDownloader },
                onDownloadStopped = { download, id, status ->
                    debug("Download ID#$id $download with status $status is done.")
                },
                downloadManager = FetchDownloadManager(
                    requireContext().applicationContext,
                    components.store,
                    DownloadService::class,
                   notificationsDelegate = components.notificationsDelegate
                ),
                tabId = customTabSessionId,
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
                }),
            owner = this,
            view = view,
        )

        fullScreenFeature.set(
            feature = FullScreenFeature(
                requireContext().components.store,
                requireContext().components.sessionUseCases,
                customTabSessionId,
                ::viewportFitChange,
                ::fullScreenChanged
            ),
            owner = this,
            view = view
        )

        browserToolbarView.view.display.setOnSiteSecurityClickedListener {
            activity.showSslDialog()
        }

        expandToolbarOnNavigation(store)

        binding.swipeRefresh.isEnabled = shouldPullToRefreshBeEnabled()

        if (binding.swipeRefresh.isEnabled) {
            val primaryTextColor = ContextCompat.getColor(context, R.color.primary_icon)
            binding.swipeRefresh.setColorSchemeColors(primaryTextColor)
            swipeRefreshFeature.set(
                feature = SwipeRefreshFeature(
                    requireContext().components.store,
                    context.components.sessionUseCases.reload,
                    binding.swipeRefresh,
                    ({}),
                    customTabSessionId
                ),
                owner = this,
                view = view
            )
        }

        webExtensionPromptFeature.set(
            feature = WebExtensionPromptFeature(
                store = components.store,
                provideAddons = ::provideAddons,
                context = requireContext(),
                fragmentManager = parentFragmentManager,
                onLinkClicked = { url, _ ->
                    components.tabsUseCases.addTab.invoke(url, selectTab = true)
                },
                view = view,
            ),
            owner = this,
            view = view,
        )

        initializeEngineView(toolbarHeight)
    }

    private suspend fun provideAddons(): List<Addon> {
        return withContext(IO) {
            val addons = requireContext().components.addonManager.getAddons(allowCache = false)
            addons
        }
    }

    @VisibleForTesting
    internal fun expandToolbarOnNavigation(store: BrowserStore) {
        consumeFlow(store) { flow ->
            flow.mapNotNull {
                    state -> state.findCustomTabOrSelectedTab(customTabSessionId)
            }
                .ifAnyChanged {
                        tab -> arrayOf(tab.content.url, tab.content.loadRequest)
                }
                .collect {
                    findInPageIntegration.onBackPressed()
                    browserToolbarView.expand()
                }
        }
    }

    /**
     * The tab associated with this fragment.
     */
    val tab: SessionState
        get() = customTabSessionId?.let { components.store.state.findTabOrCustomTab(it) }
        // Workaround for tab not existing temporarily.
            ?: createTab("about:blank")

    /**
     * Re-initializes [DynamicDownloadDialog] if the user hasn't dismissed the dialog
     * before navigating away from it's original tab.
     * onTryAgain it will use [ContentAction.UpdateDownloadAction] to re-enqueue the former failed
     * download, because [DownloadsFeature] clears any queued downloads onStop.
     * */
    @VisibleForTesting
    internal fun resumeDownloadDialogState(
        sessionId: String?
    ) {
        val savedDownloadState =
            sharedViewModel.downloadDialogState[sessionId]

        if (savedDownloadState == null || sessionId == null) {
            return
        }

        browserToolbarView.expand()
    }

    @VisibleForTesting
    internal fun shouldPullToRefreshBeEnabled(): Boolean {
        return UserPreferences(requireContext()).swipeToRefresh
    }

    @VisibleForTesting
    internal fun initializeEngineView(toolbarHeight: Int) {
        val context = requireContext()

        if (UserPreferences(context).hideBarWhileScrolling) {
            binding.engineView.setDynamicToolbarMaxHeight(toolbarHeight)

            val toolbarPosition = if (UserPreferences(context).shouldUseBottomToolbar) {
                OldToolbarPosition.BOTTOM
            } else {
                OldToolbarPosition.TOP
            }
            (binding.swipeRefresh.layoutParams as CoordinatorLayout.LayoutParams).behavior =
                OldEngineViewClippingBehavior(
                    context,
                    null,
                    binding.swipeRefresh,
                    toolbarHeight,
                    toolbarPosition
                )
        } else {
            binding.engineView.setDynamicToolbarMaxHeight(0)

            val swipeRefreshParams =
                binding.swipeRefresh.layoutParams as CoordinatorLayout.LayoutParams
            if (UserPreferences(context).shouldUseBottomToolbar) {
                swipeRefreshParams.bottomMargin = toolbarHeight
            } else {
                swipeRefreshParams.topMargin = toolbarHeight
            }
        }
    }

    @VisibleForTesting
    internal fun observeRestoreComplete(store: BrowserStore, navController: NavController) {
        val activity = activity as BrowserActivity
        consumeFlow(store) { flow ->
            flow.map { state -> state.restoreComplete }
                .distinctUntilChanged()
                .collect { restored ->
                    if (restored) {
                        val tabs =
                            store.state.getNormalOrPrivateTabs(
                                activity.browsingModeManager.mode.isPrivate
                            )
                        if (tabs.isEmpty() || store.state.selectedTabId == null) {
                            when(UserPreferences(requireContext()).homepageType){
                                HomepageChoice.VIEW.ordinal -> {
                                    components.tabsUseCases.addTab.invoke(
                                        "about:homepage",
                                        selectTab = true
                                    )
                                    navController.navigate(
                                        HomeFragmentDirections.actionGlobalHome(
                                            focusOnAddressBar = false
                                        )
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
                    }
                }
        }
    }

    @VisibleForTesting
    internal fun observeTabSelection(store: BrowserStore) {
        consumeFlow(store) { flow ->
            flow.distinctUntilChangedBy {
                it.selectedTabId
            }
                .mapNotNull {
                    it.selectedTab
                }
                .collect {
                    handleTabSelected(it)
                }
        }
    }

    private fun handleTabSelected(selectedTab: TabSessionState) {
        if (!this.isRemoving) {
            updateThemeForSession(selectedTab)
        }

        if (browserInitialized) {
            view?.let { view ->
                fullScreenChanged(false)
                browserToolbarView.expand()
                resumeDownloadDialogState(selectedTab.id)
            }
        } else {
            view?.let { view -> initializeUI(view) }
        }
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        val components = requireContext().components

        val preferredColorScheme = components.darkEnabled()
        if (components.engine.settings.preferredColorScheme != preferredColorScheme) {
            components.engine.settings.preferredColorScheme = preferredColorScheme
            components.sessionUseCases.reload()
        }
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        components.store.state.findTabOrCustomTabOrSelectedTab(customTabSessionId)?.let {
            updateThemeForSession(it)
        }
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        if (findNavController().currentDestination?.id != R.id.searchDialogFragment) {
            view?.hideKeyboard()
        }
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        initUIJob?.cancel()

        requireContext().components.store.state.findTabOrCustomTabOrSelectedTab(customTabSessionId)
            ?.let { session ->
                // If we didn't enter PiP, exit full screen on stop
                if (!session.content.pictureInPictureEnabled && fullScreenFeature.onBackPressed()) {
                    fullScreenChanged(false)
                }
            }
    }

    @CallSuper
    override fun onBackPressed(): Boolean {
        return readerViewFeature.onBackPressed() ||
                findInPageIntegration.onBackPressed() ||
                fullScreenFeature.onBackPressed() ||
                promptsFeature.onBackPressed() ||
                sessionFeature.onBackPressed() ||
                removeSessionIfNeeded()
    }

    /**
     * Saves the external app session ID to be restored later in [onViewStateRestored].
     */
    final override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CUSTOM_TAB_SESSION_ID, customTabSessionId)
    }

    /**
     * Retrieves the external app session ID saved by [onSaveInstanceState].
     */
    final override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getString(KEY_CUSTOM_TAB_SESSION_ID)?.let {
            if (requireContext().components.store.state.findCustomTab(it) != null) {
                customTabSessionId = it
            }
        }
    }

    /**
     * Forwards permission grant results to one of the features.
     */
    final override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        val feature: PermissionsFeature? = when (requestCode) {
            REQUEST_CODE_DOWNLOAD_PERMISSIONS -> downloadsFeature.get()
            REQUEST_CODE_PROMPT_PERMISSIONS -> promptsFeature.get()
            REQUEST_CODE_APP_PERMISSIONS -> sitePermissionsFeature.get()
            else -> null
        }
        feature?.onPermissionsResult(permissions, grantResults)
    }

    /**
     * Forwards activity results to the [ActivityResultHandler] features.
     */
    override fun onActivityResult(requestCode: Int, data: Intent?, resultCode: Int): Boolean {
        return listOf(
            promptsFeature
        ).any { it.onActivityResult(requestCode, data, resultCode) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        promptsFeature.withFeature { it.onActivityResult(requestCode, data, resultCode) }
    }

    /**
     * Removes the session if it was opened by an ACTION_VIEW intent
     * or if it has a parent session and no more history
     */
    protected open fun removeSessionIfNeeded(): Boolean {
        getCurrentTab()?.let { session ->
            return if (session.source is SessionState.Source.External && !session.restored) {
                activity?.finish()
                requireContext().components.tabsUseCases.removeTab(session.id)
                true
            } else {
                val hasParentSession = session is TabSessionState && session.parentId != null
                if (hasParentSession) {
                    requireContext().components.tabsUseCases.removeTab(session.id, selectParentIfExists = true)
                }
                // We want to return to home if this session didn't have a parent session to select.
                val goToOverview = !hasParentSession
                !goToOverview
            }
        }
        return false
    }

    /**
     * Returns the layout [android.view.Gravity] for the quick settings and ETP dialog.
     */
    protected fun getAppropriateLayoutGravity(): Int =
        UserPreferences(requireContext()).toolbarPositionType.androidGravity

    /**
     * Set the activity normal/private theme to match the current session.
     */
    @VisibleForTesting
    internal fun updateThemeForSession(session: SessionState) {
        val sessionMode = BrowsingMode.fromBoolean(session.content.private)
        (activity as BrowserActivity).browsingModeManager.mode = sessionMode
    }

    @VisibleForTesting
    internal fun getCurrentTab(): SessionState? {
        return requireContext().components.store.state.findCustomTabOrSelectedTab(customTabSessionId)
    }

    override fun onHomePressed() = pipFeature?.onHomePressed() ?: false

    /**
     * Exit fullscreen mode when exiting PIP mode
     */
    private fun pipModeChanged(session: SessionState) {
        if (!session.content.pictureInPictureEnabled && session.content.fullScreen) {
            onBackPressed()
            fullScreenChanged(false)
        }
    }

    final override fun onPictureInPictureModeChanged(enabled: Boolean) {
        pipFeature?.onPictureInPictureModeChanged(enabled)
    }

    private fun viewportFitChange(layoutInDisplayCutoutMode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val layoutParams = activity?.window?.attributes
            layoutParams?.layoutInDisplayCutoutMode = layoutInDisplayCutoutMode
            activity?.window?.attributes = layoutParams
        }
    }

    @VisibleForTesting
    internal fun fullScreenChanged(inFullScreen: Boolean) {
        if (inFullScreen) {
            // Close find in page bar if opened
            findInPageIntegration.onBackPressed()

            requireActivity().enterImmersiveMode(
                setOnApplyWindowInsetsListener = { key: String, listener: OnApplyWindowInsetsListener ->
                    binding.engineView.addWindowInsetsListener(key, listener)
                },
            )
            (view as? SwipeGestureLayout)?.isSwipeEnabled = false

            browserToolbarView.collapse()
            browserToolbarView.view.isVisible = false
            val browserEngine = binding.swipeRefresh.layoutParams as CoordinatorLayout.LayoutParams
            browserEngine.bottomMargin = 0
            browserEngine.topMargin = 0
            binding.swipeRefresh.translationY = 0f

            browserEngine.behavior = null

            binding.engineView.setDynamicToolbarMaxHeight(0)
            binding.engineView.setVerticalClipping(0)
        } else {
            requireActivity().exitImmersiveMode(
                unregisterOnApplyWindowInsetsListener = binding.engineView::removeWindowInsetsListener,
            )
            (view as? SwipeGestureLayout)?.isSwipeEnabled = true

            if (webAppToolbarShouldBeVisible) {
                browserToolbarView.view.isVisible = true
                val toolbarHeight = resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)
                initializeEngineView(toolbarHeight)
                browserToolbarView.expand()
            }
        }

        binding.swipeRefresh.isEnabled = shouldPullToRefreshBeEnabled()
    }

    /*
     * Dereference these views when the fragment view is destroyed to prevent memory leaks
     */
    override fun onDestroyView() {
        super.onDestroyView()

        binding.engineView.setActivityContext(null)
        _browserToolbarView = null
        _browserInteractor = null
        _binding = null
    }

    companion object {
        private const val KEY_CUSTOM_TAB_SESSION_ID = "custom_tab_session_id"
        private const val REQUEST_KEY_PROMPT_PERMISSIONS = "promptFeature"
        private const val REQUEST_CODE_DOWNLOAD_PERMISSIONS = 1
        private const val REQUEST_CODE_PROMPT_PERMISSIONS = 2
        private const val REQUEST_CODE_APP_PERMISSIONS = 3
    }

    override fun onAccessibilityStateChanged(enabled: Boolean) {
        if (_browserToolbarView != null) {
            browserToolbarView.setToolbarBehavior(enabled)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        _browserToolbarView?.dismissMenu()
    }

    // This method is called in response to native web extension messages from
    // content scripts (e.g the reader view extension). By the time these
    // messages are processed the fragment/view may no longer be attached.
    internal fun safeInvalidateBrowserToolbarView() {
        context?.let {
            val toolbarView = _browserToolbarView
            if (toolbarView != null) {
                toolbarView.view.invalidateActions()
                toolbarView.toolbarIntegration.invalidateMenu()
            }
        }
    }
}