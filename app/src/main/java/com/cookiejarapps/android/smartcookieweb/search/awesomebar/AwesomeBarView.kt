package com.cookiejarapps.android.smartcookieweb.search.awesomebar

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.core.graphics.drawable.toBitmap
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.BrowsingMode
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.search.awesomebar.ShortcutsSuggestionProvider
import mozilla.components.browser.awesomebar.BrowserAwesomeBar
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.awesomebar.provider.HistoryStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SearchActionProvider
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SessionSuggestionProvider
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.android.content.getColorFromAttr
import com.cookiejarapps.android.smartcookieweb.search.SearchEngineSource
import com.cookiejarapps.android.smartcookieweb.search.SearchFragmentState

/**
 * View that contains and configures the BrowserAwesomeBar
 * TODO: suggestions based on bookmarks
 */
@Suppress("LargeClass")
class AwesomeBarView(
    private val activity: BrowserActivity,
    val interactor: AwesomeBarInteractor,
    val view: BrowserAwesomeBar
) {
    private val sessionProvider: SessionSuggestionProvider
    private val historyStorageProvider: HistoryStorageSuggestionProvider
    private val shortcutsEnginePickerProvider: ShortcutsSuggestionProvider
    private val defaultSearchSuggestionProvider: SearchSuggestionProvider
    private val defaultSearchActionProvider: SearchActionProvider
    private val searchSuggestionProviderMap: MutableMap<SearchEngine, List<AwesomeBar.SuggestionProvider>>
    private var providersInUse = mutableSetOf<AwesomeBar.SuggestionProvider>()

    private val loadUrlUseCase = object : SessionUseCases.LoadUrlUseCase {
        override fun invoke(
            url: String,
            flags: EngineSession.LoadUrlFlags,
            additionalHeaders: Map<String, String>?
        ) {
            interactor.onUrlTapped(url)
        }
    }

    private val searchUseCase = object : SearchUseCases.SearchUseCase {
        override fun invoke(
            searchTerms: String,
            searchEngine: SearchEngine?,
            parentSessionId: String?
        ) {
            interactor.onSearchTermsTapped(searchTerms)
        }
    }

    private val shortcutSearchUseCase = object : SearchUseCases.SearchUseCase {
        override fun invoke(
            searchTerms: String,
            searchEngine: SearchEngine?,
            parentSessionId: String?
        ) {
            interactor.onSearchTermsTapped(searchTerms)
        }
    }

    private val selectTabUseCase = object : TabsUseCases.SelectTabUseCase {
        override fun invoke(tabId: String) {
            interactor.onExistingSessionSelected(tabId)
        }
    }

    init {
        view.itemAnimator = null

        val components = activity.components
        val primaryTextColor = activity.getColorFromAttr(android.R.attr.textColorPrimary)

        val engineForSpeculativeConnects = when (activity.browsingModeManager.mode) {
            BrowsingMode.Normal -> components.engine
            BrowsingMode.Private -> null
        }
        sessionProvider =
            SessionSuggestionProvider(
                activity.resources,
                components.store,
                selectTabUseCase,
                components.icons,
                getDrawable(activity, R.drawable.ic_round_search),
                excludeSelectedSession = true
            )

        historyStorageProvider =
            HistoryStorageSuggestionProvider(
                components.historyStorage,
                loadUrlUseCase,
                components.icons,
                engineForSpeculativeConnects
            )

        val searchBitmap = getDrawable(activity, R.drawable.ic_round_search)!!.apply {
            colorFilter = createBlendModeColorFilterCompat(primaryTextColor, SRC_IN)
        }.toBitmap()

        defaultSearchSuggestionProvider =
            SearchSuggestionProvider(
                context = activity,
                store = components.store,
                searchUseCase = searchUseCase,
                fetchClient = components.client,
                mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                icon = searchBitmap,
                showDescription = false,
                engine = engineForSpeculativeConnects,
                filterExactMatch = true,
                private = when (activity.browsingModeManager.mode) {
                    BrowsingMode.Normal -> false
                    BrowsingMode.Private -> true
                }
            )

        defaultSearchActionProvider =
            SearchActionProvider(
                store = components.store,
                searchUseCase = searchUseCase,
                icon = searchBitmap,
                showDescription = false
            )

        shortcutsEnginePickerProvider =
            ShortcutsSuggestionProvider(
                store = components.store,
                context = activity,
                selectShortcutEngine = interactor::onSearchShortcutEngineSelected
            )

        searchSuggestionProviderMap = HashMap()
    }

    fun update(context: Context, state: SearchFragmentState) {
        updateSuggestionProvidersVisibility(context, state)

        if (state.query.isNotEmpty() && state.query == state.url && !state.showSearchShortcuts) {
            return
        }

        view.onInputChanged(state.query)
    }

    private fun updateSuggestionProvidersVisibility(context: Context, state: SearchFragmentState) {
        if (state.showSearchShortcuts) {
            handleDisplayShortcutsProviders()
            return
        }

        val providersToAdd = getProvidersToAdd(context, state)
        val providersToRemove = getProvidersToRemove(context, state)

        performProviderListChanges(providersToAdd, providersToRemove)
    }

    private fun performProviderListChanges(
        providersToAdd: MutableSet<AwesomeBar.SuggestionProvider>,
        providersToRemove: MutableSet<AwesomeBar.SuggestionProvider>
    ) {
        for (provider in providersToAdd) {
            if (providersInUse.find { it.id == provider.id } == null) {
                providersInUse.add(provider)
                view.addProviders(provider)
            }
        }

        for (provider in providersToRemove) {
            if (providersInUse.find { it.id == provider.id } != null) {
                providersInUse.remove(provider)
                view.removeProviders(provider)
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun getProvidersToAdd(context: Context, state: SearchFragmentState): MutableSet<AwesomeBar.SuggestionProvider> {
        val providersToAdd = mutableSetOf<AwesomeBar.SuggestionProvider>()

        if (state.showHistorySuggestions) {
            providersToAdd.add(historyStorageProvider)
        }

        if (state.showSearchSuggestions) {
            providersToAdd.addAll(getSelectedSearchSuggestionProvider(context, state))
        }

        if (activity.browsingModeManager.mode == BrowsingMode.Normal) {
            providersToAdd.add(sessionProvider)
        }

        return providersToAdd
    }

    private fun getProvidersToRemove(context: Context, state: SearchFragmentState): MutableSet<AwesomeBar.SuggestionProvider> {
        val providersToRemove = mutableSetOf<AwesomeBar.SuggestionProvider>()

        providersToRemove.add(shortcutsEnginePickerProvider)

        if (!state.showHistorySuggestions) {
            providersToRemove.add(historyStorageProvider)
        }

        if (!state.showSearchSuggestions) {
            providersToRemove.addAll(getSelectedSearchSuggestionProvider(context, state))
        }

        if (activity.browsingModeManager.mode == BrowsingMode.Private) {
            providersToRemove.add(sessionProvider)
        }

        return providersToRemove
    }

    private fun getSelectedSearchSuggestionProvider(context: Context, state: SearchFragmentState): List<AwesomeBar.SuggestionProvider> {
        //TODO: Clean this up when switching to search suggestion provider option
        return when (state.searchEngineSource) {
            is SearchEngineSource.Default -> {
                if(UserPreferences(context).searchSuggestionsEnabled){
                    listOf(
                        defaultSearchActionProvider,
                        defaultSearchSuggestionProvider
                    )
                }
                else{
                    emptyList()
                }
            }
            is SearchEngineSource.Shortcut -> getSuggestionProviderForEngine(
                state.searchEngineSource.searchEngine
            )
            is SearchEngineSource.None -> emptyList()
        }
    }

    private fun handleDisplayShortcutsProviders() {
        view.removeAllProviders()
        providersInUse.clear()
        providersInUse.add(shortcutsEnginePickerProvider)
        view.addProviders(shortcutsEnginePickerProvider)
    }

    private fun getSuggestionProviderForEngine(engine: SearchEngine): List<AwesomeBar.SuggestionProvider> {
        return searchSuggestionProviderMap.getOrPut(engine) {
            val components = activity.components
            val primaryTextColor = activity.getColorFromAttr(android.R.attr.textColorPrimary)

            val searchBitmap = getDrawable(activity, R.drawable.ic_round_search)!!.apply {
                colorFilter = createBlendModeColorFilterCompat(primaryTextColor, SRC_IN)
            }.toBitmap()

            val engineForSpeculativeConnects = when (activity.browsingModeManager.mode) {
                BrowsingMode.Normal -> components.engine
                BrowsingMode.Private -> null
            }

            listOf(
                SearchActionProvider(
                    searchEngine = engine,
                    store = components.store,
                    searchUseCase = shortcutSearchUseCase,
                    icon = searchBitmap
                ),
                SearchSuggestionProvider(
                    engine,
                    shortcutSearchUseCase,
                    components.client,
                    limit = 3,
                    mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                    icon = searchBitmap,
                    engine = engineForSpeculativeConnects,
                    filterExactMatch = true,
                    private = when (activity.browsingModeManager.mode) {
                        BrowsingMode.Normal -> false
                        BrowsingMode.Private -> true
                    }
                )
            )
        }
    }
}
