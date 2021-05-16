package com.cookiejarapps.android.smartcookieweb

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.cookiejarapps.android.smartcookieweb.*
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingMode
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui.BookmarkFragment
import com.cookiejarapps.android.smartcookieweb.browser.tabs.TabsTrayFragment
import com.cookiejarapps.android.smartcookieweb.components.StoreProvider
import com.cookiejarapps.android.smartcookieweb.components.toolbar.*
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.integration.FindInPageIntegration
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import kotlinx.android.synthetic.main.fragment_browser.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.thumbnails.BrowserThumbnails
import mozilla.components.feature.app.links.AppLinksFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.share.ShareDownloadFeature
import mozilla.components.feature.intent.ext.EXTRA_SESSION_ID
import mozilla.components.feature.media.fullscreen.MediaSessionFullscreenFeature
import mozilla.components.feature.privatemode.feature.SecureWindowFeature
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.readerview.ReaderViewFeature
import mozilla.components.feature.search.SearchFeature
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.session.PictureInPictureFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SwipeRefreshFeature
import mozilla.components.feature.session.behavior.EngineViewBrowserToolbarBehavior
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.ktx.android.view.enterToImmersiveMode
import mozilla.components.support.ktx.android.view.exitImmersiveModeIfNeeded
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import com.cookiejarapps.android.smartcookieweb.components.toolbar.ToolbarIntegration
import com.cookiejarapps.android.smartcookieweb.downloads.DownloadService
import com.cookiejarapps.android.smartcookieweb.integration.ContextMenuIntegration
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.support.base.log.logger.Logger.Companion.debug
import com.cookiejarapps.android.smartcookieweb.components.toolbar.ToolbarPosition
import com.cookiejarapps.android.smartcookieweb.integration.ReaderModeIntegration
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.feature.prompts.share.ShareDelegate
import org.mozilla.fenix.home.HomeScreenViewModel
import org.mozilla.fenix.home.SharedViewModel
import java.lang.ref.WeakReference
import mozilla.components.feature.session.behavior.ToolbarPosition as MozacToolbarPosition

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
    private val shareDownloadsFeature = ViewBoundFeatureWrapper<ShareDownloadFeature>()
    private val appLinksFeature = ViewBoundFeatureWrapper<AppLinksFeature>()
    private val promptsFeature = ViewBoundFeatureWrapper<PromptFeature>()
    private val findInPageIntegration = ViewBoundFeatureWrapper<FindInPageIntegration>()
    private val toolbarIntegration = ViewBoundFeatureWrapper<ToolbarIntegration>()
    private val sitePermissionsFeature = ViewBoundFeatureWrapper<SitePermissionsFeature>()
    private val fullScreenFeature = ViewBoundFeatureWrapper<FullScreenFeature>()
    private val swipeRefreshFeature = ViewBoundFeatureWrapper<SwipeRefreshFeature>()
    private val secureWindowFeature = ViewBoundFeatureWrapper<SecureWindowFeature>()
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
    private val homeViewModel: HomeScreenViewModel by activityViewModels()

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        customTabSessionId = requireArguments().getString(EXTRA_SESSION_ID)

        val view = inflater.inflate(R.layout.fragment_browser, container, false)

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
            engineView = WeakReference(engineView),
            swipeRefresh = WeakReference(swipeRefresh),
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
            engineView = engineView,
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
            container = view.browserLayout,
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
                stub = view.stubFindInPage,
                engineView = engineView,
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
                readerViewBar,
                readerViewAppearanceButton
            ),
            owner = this,
            view = view
        )

        promptsFeature.set(
                feature = PromptFeature(
                        activity = activity,
                        store = components.store,
                        customTabId = customTabSessionId,
                        fragmentManager = parentFragmentManager,
                        onNeedToRequestPermissions = { permissions ->
                            requestPermissions(permissions, REQUEST_CODE_PROMPT_PERMISSIONS)
                        }),
                owner = this,
                view = view
        )

        fullScreenMediaSessionFeature.set(
            feature = MediaSessionFullscreenFeature(
                requireActivity(),
                context.components.store
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
                view.engineView,
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
                onDownloadStopped = { download, id, status ->
                    debug("Download ID#$id $download with status $status is done.")
                },
                downloadManager = FetchDownloadManager(
                    requireContext().applicationContext,
                    components.store,
                    DownloadService::class
                ),
                tabId = customTabSessionId,
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
                }),
            owner = this,
            view = view
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

        expandToolbarOnNavigation(store)

        store.flowScoped(viewLifecycleOwner) { flow ->
            flow.mapNotNull { state -> state.findTabOrCustomTabOrSelectedTab(customTabSessionId) }
                .ifChanged { tab -> tab.content.pictureInPictureEnabled }
                .collect { tab -> pipModeChanged(tab) }
        }

        view.swipeRefresh.isEnabled = shouldPullToRefreshBeEnabled(false)

        if (view.swipeRefresh.isEnabled) {
            val primaryTextColor = ContextCompat.getColor(context, R.color.primary_icon)
            view.swipeRefresh.setColorSchemeColors(primaryTextColor)
            swipeRefreshFeature.set(
                feature = SwipeRefreshFeature(
                    requireContext().components.store,
                    context.components.sessionUseCases.reload,
                    view.swipeRefresh,
                    customTabSessionId
                ),
                owner = this,
                view = view
            )
        }

        initializeEngineView(toolbarHeight)
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
     * Preserves current state of the [DynamicDownloadDialog] to persist through tab changes and
     * other fragments navigation.
     * */
    private fun saveDownloadDialogState(
        sessionId: String?,
        downloadState: DownloadState,
        downloadJobStatus: DownloadState.Status
    ) {
        sessionId?.let { id ->
            sharedViewModel.downloadDialogState[id] = Pair(
                downloadState,
                downloadJobStatus == DownloadState.Status.FAILED
            )
        }
    }

    /**
     * Re-initializes [DynamicDownloadDialog] if the user hasn't dismissed the dialog
     * before navigating away from it's original tab.
     * onTryAgain it will use [ContentAction.UpdateDownloadAction] to re-enqueue the former failed
     * download, because [DownloadsFeature] clears any queued downloads onStop.
     * */
    @VisibleForTesting
    internal fun resumeDownloadDialogState(
        sessionId: String?,
        store: BrowserStore,
        view: View,
        context: Context,
        toolbarHeight: Int
    ) {
        val savedDownloadState =
            sharedViewModel.downloadDialogState[sessionId]

        if (savedDownloadState == null || sessionId == null) {
            //view.viewDynamicDownloadDialog.visibility = View.GONE
            return
        }

        val onTryAgain: (String) -> Unit = {
            savedDownloadState.first?.let { dlState ->
                store.dispatch(
                    ContentAction.UpdateDownloadAction(
                        sessionId, dlState.copy(skipConfirmation = true)
                    )
                )
            }
        }

        val onDismiss: () -> Unit =
            { sharedViewModel.downloadDialogState.remove(sessionId) }

        /*DynamicDownloadDialog(
            container = view.browserLayout,
            downloadState = savedDownloadState.first,
            metrics = requireComponents.analytics.metrics,
            didFail = savedDownloadState.second,
            tryAgain = onTryAgain,
            onCannotOpenFile = {
                showCannotOpenFileError(view.browserLayout, context, it)
            },
            view = view.viewDynamicDownloadDialog,
            toolbarHeight = toolbarHeight,
            onDismiss = onDismiss
        ).show()*/

        browserToolbarView.expand()
    }

    @VisibleForTesting
    internal fun shouldPullToRefreshBeEnabled(inFullScreen: Boolean): Boolean {
        return UserPreferences(requireContext()).swipeToRefresh
    }

    @VisibleForTesting
    internal fun initializeEngineView(toolbarHeight: Int) {
        val context = requireContext()

        if (UserPreferences(context).hideBarWhileScrolling) {
            engineView.setDynamicToolbarMaxHeight(toolbarHeight)

            val toolbarPosition = if (UserPreferences(context).shouldUseBottomToolbar) {
                MozacToolbarPosition.BOTTOM
            } else {
                MozacToolbarPosition.TOP
            }
            (swipeRefresh.layoutParams as CoordinatorLayout.LayoutParams).behavior =
                EngineViewBrowserToolbarBehavior(
                    context,
                    null,
                    swipeRefresh,
                    toolbarHeight,
                    toolbarPosition
                )
        } else {
            engineView.setDynamicToolbarMaxHeight(0)

            val swipeRefreshParams =
                swipeRefresh.layoutParams as CoordinatorLayout.LayoutParams
            if (UserPreferences(context).shouldUseBottomToolbar) {
                swipeRefreshParams.bottomMargin = toolbarHeight
            } else {
                swipeRefreshParams.topMargin = toolbarHeight
            }
        }
    }

    //TODO: Custom add-on collections, final clean up, VideoDL + update SCW/BB

    @VisibleForTesting
    internal fun observeRestoreComplete(store: BrowserStore, navController: NavController) {
        val activity = activity as BrowserActivity
        consumeFlow(store) { flow ->
            flow.map { state -> state.restoreComplete }
                .ifChanged()
                .collect { restored ->
                    if (restored) {
                        val tabs =
                            store.state.getNormalOrPrivateTabs(
                                activity.browsingModeManager.mode.isPrivate
                            )
                        if (tabs.isEmpty() || store.state.selectedTabId == null) {
                            navController.popBackStack(R.id.homeFragment, false)
                        }
                    }
                }
        }
    }

    @VisibleForTesting
    internal fun observeTabSelection(store: BrowserStore) {
        consumeFlow(store) { flow ->
            flow.ifChanged {
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

                val toolbarHeight = resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)
                val context = requireContext()
                resumeDownloadDialogState(selectedTab.id, context.components.store, view, context, toolbarHeight)
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
        grantResults: IntArray
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

    /**
     * Removes the session if it was opened by an ACTION_VIEW intent
     * or if it has a parent session and no more history
     */
    protected open fun removeSessionIfNeeded(): Boolean {
        getCurrentTab()?.let { session ->
            return if (session.source == SessionState.Source.ACTION_VIEW) {
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

            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            requireActivity().window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

            browserToolbarView.collapse()
            browserToolbarView.view.isVisible = false
            val browserEngine = swipeRefresh.layoutParams as CoordinatorLayout.LayoutParams
            browserEngine.bottomMargin = 0
            browserEngine.topMargin = 0
            swipeRefresh.translationY = 0f

            engineView.setDynamicToolbarMaxHeight(0)
            // Without this, fullscreen has a margin at the top.
            engineView.setVerticalClipping(0)

        } else {
            activity?.exitImmersiveModeIfNeeded()
            if (webAppToolbarShouldBeVisible) {
                browserToolbarView.view.isVisible = true
                val toolbarHeight = resources.getDimensionPixelSize(R.dimen.browser_toolbar_height)
                initializeEngineView(toolbarHeight)
                browserToolbarView.expand()
            }
        }

        activity?.swipeRefresh?.isEnabled = shouldPullToRefreshBeEnabled(inFullScreen)
    }

    /*
     * Dereference these views when the fragment view is destroyed to prevent memory leaks
     */
    override fun onDestroyView() {
        super.onDestroyView()

        _browserToolbarView = null
        _browserInteractor = null
    }

    companion object {
        private const val KEY_CUSTOM_TAB_SESSION_ID = "custom_tab_session_id"
        private const val REQUEST_CODE_DOWNLOAD_PERMISSIONS = 1
        private const val REQUEST_CODE_PROMPT_PERMISSIONS = 2
        private const val REQUEST_CODE_APP_PERMISSIONS = 3

        val intentSourcesList: List<SessionState.Source> = listOf(
            SessionState.Source.ACTION_SEARCH,
            SessionState.Source.ACTION_SEND,
            SessionState.Source.ACTION_VIEW
        )
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