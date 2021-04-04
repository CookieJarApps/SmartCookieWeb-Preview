package com.cookiejarapps.android.smartcookieweb

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.cookiejarapps.android.smartcookieweb.addons.AddonsActivity
import com.cookiejarapps.android.smartcookieweb.components.BrowserMenu
import com.cookiejarapps.android.smartcookieweb.components.Components
import kotlinx.android.synthetic.main.fragment_browser.view.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.feature.app.links.AppLinksFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.feature.privatemode.feature.SecureWindowFeature
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.session.CoordinateScrollingFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SwipeRefreshFeature
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.feature.sitepermissions.SitePermissionsRules
import mozilla.components.feature.sitepermissions.SitePermissionsRules.AutoplayAction
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.support.base.feature.ActivityResultHandler
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.arch.lifecycle.addObservers
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import com.cookiejarapps.android.smartcookieweb.downloads.DownloadService
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.integration.ContextMenuIntegration
import com.cookiejarapps.android.smartcookieweb.integration.FindInPageIntegration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import mozilla.components.browser.menu.WebExtensionBrowserMenuBuilder
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

// Base browser fragment
@SuppressWarnings("LargeClass")
abstract class BaseBrowserFragment : Fragment(), UserInteractionHandler, ActivityResultHandler {

    private val appLinksFeature = ViewBoundFeatureWrapper<AppLinksFeature>()
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()
    private val promptFeature = ViewBoundFeatureWrapper<PromptFeature>()
    private val swipeRefreshFeature = ViewBoundFeatureWrapper<SwipeRefreshFeature>()
    private val sitePermissionsFeature = ViewBoundFeatureWrapper<SitePermissionsFeature>()
    private val sessionFeature = ViewBoundFeatureWrapper<SessionFeature>()
    private val toolbarFeature = ViewBoundFeatureWrapper<ToolbarFeature>()

    private val findInPageIntegration = ViewBoundFeatureWrapper<FindInPageIntegration>()
    private val contextMenuIntegration = ViewBoundFeatureWrapper<ContextMenuIntegration>()

    protected val sessionId: String?
        get() = arguments?.getString(SESSION_ID_KEY)

    private val activityResultHandler: List<ViewBoundFeatureWrapper<*>> = listOf(
        promptFeature
    )

    //TODO: DUPLICATE FUNCTION
    internal fun observeRestoreComplete(store: BrowserStore, navController: NavController) {
        activity as BrowserActivity
        consumeFlow(store) { flow ->
            flow.map { state -> state.restoreComplete }
                .ifChanged()
                .collect { restored ->
                    if (restored) {
                        // Once tab restoration is complete, if there are no tabs to show in the browser, go home
                        val tabs =
                            store.state.tabs
                        if (tabs.isEmpty() || store.state.selectedTabId == null) {
                            navController.popBackStack(R.id.homeFragment, false)
                        }
                    }
                }
        }
    }

    @CallSuper
    @Suppress("LongMethod")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        if (components.sessionManager.selectedSession == null) {
            observeRestoreComplete(components.store, findNavController())
        }

        layout.toolbar.display.menuBuilder = WebExtensionBrowserMenuBuilder(
            BrowserMenu(
                requireContext(),
                onItemTapped = {
                    onMenuItemPressed(it)
                }).coreMenuItems,
            store = components.store,
            webExtIconTintColorResource = R.color.photonGrey50,
            onAddonsManagerTapped = {
                val intent = Intent(context, AddonsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                requireContext().startActivity(intent)
            }
        )

        sessionFeature.set(
            feature = SessionFeature(
                components.store,
                components.sessionUseCases.goBack,
                layout.engineView,
                sessionId),
            owner = this,
            view = layout)

        toolbarFeature.set(
            feature = ToolbarFeature(
                layout.toolbar,
                components.store,
                components.sessionUseCases.loadUrl,
                components.defaultSearchUseCase,
                sessionId),
            owner = this,
            view = layout)

        layout.toolbar.display.indicators += listOf(
            DisplayToolbar.Indicators.HIGHLIGHT
        )

        layout.toolbar.display.setUrlBackground(
            ContextCompat.getDrawable(requireContext(), R.drawable.toolbar_background))

        layout.toolbar.setBackgroundColor(requireContext().getColorFromAttr(R.attr.colorSurface))

        layout.toolbar.display.colors = layout.toolbar.display.colors.copy(
            securityIconInsecure = 0xFFd9534f.toInt(),
            securityIconSecure = 0xFF5cb85c.toInt(),
            text = 0xFF0c0c0d.toInt(),
            menu = requireContext().getColorFromAttr(android.R.attr.textColorPrimary),
            separator = 0x1E15141a,
            trackingProtection = 0xFF20123a.toInt(),
            emptyIcon = 0xFF20123a.toInt(),
            hint = 0x1E15141a
        )

        layout.toolbar.edit.colors = layout.toolbar.edit.colors.copy(
            text = requireContext().getColorFromAttr(android.R.attr.textColorPrimary),
            clear = requireContext().getColorFromAttr(android.R.attr.textColorPrimary),
            icon = requireContext().getColorFromAttr(android.R.attr.textColorPrimary)
        )

        layout.toolbar.edit.setUrlBackground(
                ContextCompat.getDrawable(requireContext(), R.drawable.edit_url_background))
        layout.toolbar.edit.setIcon(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_round_search)!!, resources.getString(R.string.search))

        layout.toolbar.elevation = 8f

        swipeRefreshFeature.set(
            feature = SwipeRefreshFeature(
                components.store,
                components.sessionUseCases.reload,
                layout.swipeToRefresh),
            owner = this,
            view = layout)

        downloadsFeature.set(
            feature = DownloadsFeature(
                requireContext().applicationContext,
                store = components.store,
                useCases = components.downloadsUseCases,
                fragmentManager = childFragmentManager,
                onDownloadStopped = { download, id, status ->
                    Logger.debug("Download ID#$id $download with status $status is done.")
                },
                downloadManager = FetchDownloadManager(
                    requireContext().applicationContext,
                    components.store,
                    DownloadService::class
                ),
                tabId = sessionId,
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
                }),
            owner = this,
            view = layout
        )

        val scrollFeature = CoordinateScrollingFeature(components.store, layout.engineView, layout.toolbar)

        contextMenuIntegration.set(
            feature = ContextMenuIntegration(
                context = requireContext(),
                fragmentManager = parentFragmentManager,
                browserStore = components.store,
                tabsUseCases = components.tabsUseCases,
                contextMenuUseCases = components.contextMenuUseCases,
                parentView = layout,
                sessionId = sessionId
            ),
            owner = this,
            view = layout)

        appLinksFeature.set(
            feature = AppLinksFeature(
                context = requireContext(),
                store = components.store,
                sessionId = sessionId,
                fragmentManager = parentFragmentManager,
                launchInApp = { components.preferences.getBoolean(Components.PREF_LAUNCH_EXTERNAL_APP, false) },
                loadUrlUseCase = components.sessionUseCases.loadUrl
            ),
            owner = this,
            view = layout
        )

        promptFeature.set(
            feature = PromptFeature(
                fragment = this,
                store = components.store,
                customTabId = sessionId,
                fragmentManager = parentFragmentManager,
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_PROMPT_PERMISSIONS)
                }),
            owner = this,
            view = layout)

        sitePermissionsFeature.set(
            feature = SitePermissionsFeature(
                context = requireContext(),
                sessionId = sessionId,
                storage = components.permissionStorage,
                fragmentManager = parentFragmentManager,
                sitePermissionsRules = SitePermissionsRules(
                    autoplayAudible = AutoplayAction.BLOCKED,
                    autoplayInaudible = AutoplayAction.BLOCKED,
                    camera = SitePermissionsRules.Action.ASK_TO_ALLOW,
                    location = SitePermissionsRules.Action.ASK_TO_ALLOW,
                    notification = SitePermissionsRules.Action.ASK_TO_ALLOW,
                    microphone = SitePermissionsRules.Action.ASK_TO_ALLOW,
                    persistentStorage = SitePermissionsRules.Action.ASK_TO_ALLOW,
                    mediaKeySystemAccess = SitePermissionsRules.Action.ASK_TO_ALLOW
                ),
                onNeedToRequestPermissions = { permissions ->
                    requestPermissions(permissions, REQUEST_CODE_APP_PERMISSIONS)
                },
                onShouldShowRequestPermissionRationale = { shouldShowRequestPermissionRationale(it) },
                store = components.store
            ),
            owner = this,
            view = layout
        )

        findInPageIntegration.set(
            feature = FindInPageIntegration(components.store, layout.findInPage, layout.engineView),
            owner = this,
            view = layout)

        val secureWindowFeature = SecureWindowFeature(
            window = requireActivity().window,
            store = components.store,
            customTabId = sessionId
        )

        // Observe the lifecycle for supported features
        lifecycle.addObservers(
            scrollFeature,
            secureWindowFeature
        )

        return layout
    }

    fun onMenuItemPressed(item: BrowserMenu.Item){
        when(item){
            BrowserMenu.Item.Bookmarks -> {
                val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)

                val bookmarksDrawer = activity?.findViewById<FrameLayout>(R.id.right_drawer)

                if (bookmarksDrawer != null) {
                    drawerLayout?.openDrawer(bookmarksDrawer)
                }
            }
            BrowserMenu.Item.NewTab -> {
                requireActivity().findNavController(R.id.container).navigate(
                    R.id.homeFragment
                )
            }
        }
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumeFlow(components.store) { flow ->
            flow.mapNotNull { state -> state.findCustomTabOrSelectedTab(sessionId) }
                .ifAnyChanged { tab ->
                    arrayOf(
                        tab.content.loading,
                        tab.content.canGoBack,
                        tab.content.canGoForward
                    )
                }
                .collect {
                    view.toolbar.invalidateActions()
                }
        }
    }

    @CallSuper
    override fun onBackPressed(): Boolean =
        listOf(findInPageIntegration, toolbarFeature, sessionFeature).any { it.onBackPressed() }

    @CallSuper
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val feature: PermissionsFeature? = when (requestCode) {
            REQUEST_CODE_DOWNLOAD_PERMISSIONS -> downloadsFeature.get()
            REQUEST_CODE_PROMPT_PERMISSIONS -> promptFeature.get()
            REQUEST_CODE_APP_PERMISSIONS -> sitePermissionsFeature.get()
            else -> null
        }
        feature?.onPermissionsResult(permissions, grantResults)
    }

    @CallSuper
    override fun onActivityResult(requestCode: Int, data: Intent?, resultCode: Int): Boolean {
        return activityResultHandler.any { it.onActivityResult(requestCode, data, resultCode) }
    }

    companion object {
        private const val SESSION_ID_KEY = "session_id"

        private const val REQUEST_CODE_DOWNLOAD_PERMISSIONS = 1
        private const val REQUEST_CODE_PROMPT_PERMISSIONS = 2
        private const val REQUEST_CODE_APP_PERMISSIONS = 3

        @JvmStatic
        protected fun Bundle.putSessionId(sessionId: String?) {
            putString(SESSION_ID_KEY, sessionId)
        }
    }
}
