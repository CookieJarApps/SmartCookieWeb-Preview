package com.cookiejarapps.android.smartcookieweb

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.ui.tabcounter.TabCounter
import com.cookiejarapps.android.smartcookieweb.integration.ContextMenuIntegration
import com.cookiejarapps.android.smartcookieweb.integration.FindInPageIntegration
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

    @CallSuper
    @Suppress("LongMethod")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        layout.toolbar.display.menuBuilder = components.menuBuilder

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

        layout.toolbar.setBackgroundColor(0xFFFFFFFF.toInt())

        layout.toolbar.display.colors = layout.toolbar.display.colors.copy(
            securityIconInsecure = 0xFFd9534f.toInt(),
            securityIconSecure = 0xFF5cb85c.toInt(),
            text = 0xFF0c0c0d.toInt(),
            menu = 0xFF20123a.toInt(),
            separator = 0x1E15141a,
            trackingProtection = 0xFF20123a.toInt(),
            emptyIcon = 0xFF20123a.toInt(),
            hint = 0x1E15141a
        )

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
