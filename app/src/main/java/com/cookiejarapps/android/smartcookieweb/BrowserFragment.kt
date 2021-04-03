package com.cookiejarapps.android.smartcookieweb

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.drawerlayout.widget.DrawerLayout
import com.cookiejarapps.android.smartcookieweb.browser.SearchEngineList
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui.BookmarkFragment
import com.cookiejarapps.android.smartcookieweb.browser.tabs.TabsTrayFragment
import kotlinx.android.synthetic.main.fragment_browser.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.thumbnails.BrowserThumbnails
import mozilla.components.feature.awesomebar.AwesomeBarFeature
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.media.fullscreen.MediaSessionFullscreenFeature
import mozilla.components.feature.search.SearchFeature
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.tabs.WindowFeature
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.WebExtensionToolbarFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.ktx.android.view.enterToImmersiveMode
import mozilla.components.support.ktx.android.view.exitImmersiveModeIfNeeded
import com.cookiejarapps.android.smartcookieweb.ext.components
import mozilla.components.feature.search.ext.toDefaultSearchEngineProvider
import com.cookiejarapps.android.smartcookieweb.integration.ReaderModeIntegration
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import mozilla.components.browser.state.search.SearchEngine

// Fragment containing GeckoView
@ExperimentalCoroutinesApi
class BrowserFragment : BaseBrowserFragment(), UserInteractionHandler {
    private val thumbnailsFeature = ViewBoundFeatureWrapper<BrowserThumbnails>()
    private val readerViewFeature = ViewBoundFeatureWrapper<ReaderModeIntegration>()
    private val webExtToolbarFeature = ViewBoundFeatureWrapper<WebExtensionToolbarFeature>()
    private val searchFeature = ViewBoundFeatureWrapper<SearchFeature>()
    private val fullScreenFeature = ViewBoundFeatureWrapper<FullScreenFeature>()
    private val mediaSessionFullscreenFeature =
        ViewBoundFeatureWrapper<MediaSessionFullscreenFeature>()

    @Suppress("LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layout = super.onCreateView(inflater, container, savedInstanceState)

        ToolbarAutocompleteFeature(layout.toolbar, components.engine).apply {
            addHistoryStorageProvider(components.historyStorage)
            addDomainProvider(components.shippedDomainsProvider)
        }

        TabButtonFeature(
            toolbar = layout.toolbar,
            store = components.store,
            sessionId = sessionId,
            lifecycleOwner = viewLifecycleOwner,
            showTabs = ::showTabs,
            countBasedOnSelectedTabType = false
        )

        val engine = SearchEngineList().engines[UserPreferences(requireContext()).searchEngineChoice]

        requireContext().components.searchUseCases.selectSearchEngine(engine)

        AwesomeBarFeature(layout.awesomeBar, layout.toolbar, layout.engineView, components.icons)
            .addHistoryProvider(
                components.historyStorage,
                components.sessionUseCases.loadUrl,
                components.engine
            )
            .addSessionProvider(
                resources,
                components.store,
                components.tabsUseCases.selectTab
            )
            .addSearchActionProvider(
                components.store.toDefaultSearchEngineProvider(),
                searchUseCase = components.searchUseCases.defaultSearch
            )
            .addSearchProvider(
                requireContext(),
                components.store.toDefaultSearchEngineProvider(),
                components.searchUseCases.defaultSearch,
                fetchClient = components.client,
                mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                engine = components.engine,
                filterExactMatch = true
            )
            .addClipboardProvider(
                requireContext(),
                components.sessionUseCases.loadUrl,
                components.engine
            )

        readerViewFeature.set(
            feature = ReaderModeIntegration(
                requireContext(),
                components.engine,
                components.store,
                layout.toolbar,
                layout.readerViewBar,
                layout.readerViewAppearanceButton
            ),
            owner = this,
            view = layout
        )

        fullScreenFeature.set(
            feature = FullScreenFeature(
                components.store,
                components.sessionUseCases,
                sessionId
            ) { inFullScreen ->
                if (inFullScreen) {
                    activity?.enterToImmersiveMode()
                } else {
                    activity?.exitImmersiveModeIfNeeded()
                }
            },
            owner = this,
            view = layout
        )

        mediaSessionFullscreenFeature.set(
            feature = MediaSessionFullscreenFeature(
                requireActivity(),
                components.store
            ),
            owner = this,
            view = layout
        )

        thumbnailsFeature.set(
            feature = BrowserThumbnails(requireContext(), layout.engineView, components.store),
            owner = this,
            view = layout
        )

        if(UserPreferences(requireContext()).showAddonsInBar){
            webExtToolbarFeature.set(
                feature = WebExtensionToolbarFeature(
                    layout.toolbar,
                    components.store
                ),
                owner = this,
                view = layout
            )
        }

        searchFeature.set(
            feature = SearchFeature(components.store) { request, _ ->
                if (request.isPrivate) {
                    components.searchUseCases.newPrivateTabSearch.invoke(request.query)
                } else {
                    components.searchUseCases.newTabSearch.invoke(request.query)
                }
            },
            owner = this,
            view = layout
        )

        val windowFeature = WindowFeature(components.store, components.tabsUseCases)
        lifecycle.addObserver(windowFeature)

        return layout
    }

    private fun showTabs() {
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)

        val tabsDrawer = activity?.findViewById<FrameLayout>(R.id.left_drawer)

        if (tabsDrawer != null) {
            drawerLayout?.openDrawer(tabsDrawer)
        }
    }

    fun showBookmarks() {
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)

        val bookmarksDrawer = activity?.findViewById<FrameLayout>(R.id.right_drawer)

        if (bookmarksDrawer != null) {
            drawerLayout?.openDrawer(bookmarksDrawer)
        }
    }

    override fun onBackPressed(): Boolean {
        return when {
            fullScreenFeature.onBackPressed() -> true
            readerViewFeature.onBackPressed() -> true
            else -> super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    companion object {
        fun create(sessionId: String? = null) = BrowserFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }
    }
}
