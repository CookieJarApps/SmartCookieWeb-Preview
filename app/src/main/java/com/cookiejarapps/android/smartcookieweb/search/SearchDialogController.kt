package com.cookiejarapps.android.smartcookieweb.search

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.BrowserDirection
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.tabs.TabsUseCases

/**
 * An interface that handles the view manipulation of the Search, triggered by the Interactor
 */
@Suppress("TooManyFunctions")
interface SearchController {
    fun handleUrlCommitted(url: String)
    fun handleEditingCancelled()
    fun handleTextChanged(text: String)
    fun handleUrlTapped(url: String)
    fun handleSearchTermsTapped(searchTerms: String)
    fun handleSearchShortcutEngineSelected(searchEngine: SearchEngine)
    fun handleClickSearchEngineSettings()
    fun handleExistingSessionSelected(tabId: String)
    fun handleSearchShortcutsButtonClicked()
}

@Suppress("TooManyFunctions", "LongParameterList")
class SearchDialogController(
    private val activity: BrowserActivity,
    private val store: BrowserStore,
    private val tabsUseCases: TabsUseCases,
    private val fragmentStore: SearchFragmentStore,
    private val navController: NavController,
    private val dismissDialog: () -> Unit,
    private val clearToolbarFocus: () -> Unit,
    private val focusToolbar: () -> Unit
) : SearchController {

    override fun handleUrlCommitted(url: String) {
        when (url) {
            "moz://a" -> openSearchOrUrl("https://mozilla.org")
            else ->
                if (url.isNotBlank()) {
                    openSearchOrUrl(url)
                }
        }
        dismissDialog()
    }

    private fun openSearchOrUrl(url: String) {
        clearToolbarFocus()

        val searchEngine = fragmentStore.state.searchEngineSource.searchEngine

        activity.openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = false,
            from = BrowserDirection.FromSearchDialog,
            engine = searchEngine
        )
    }

    override fun handleEditingCancelled() {
        clearToolbarFocus()
    }

    override fun handleTextChanged(text: String) {
        val textMatchesCurrentUrl = fragmentStore.state.url == text
        val textMatchesCurrentSearch = fragmentStore.state.searchTerms == text

        fragmentStore.dispatch(SearchFragmentAction.UpdateQuery(text))
        fragmentStore.dispatch(
            SearchFragmentAction.ShowSearchShortcutEnginePicker(
                (textMatchesCurrentUrl || textMatchesCurrentSearch || text.isEmpty())
            )
        )
        fragmentStore.dispatch(
            SearchFragmentAction.AllowSearchSuggestionsInPrivateModePrompt(
                text.isNotEmpty() &&
                        activity.browsingModeManager.mode.isPrivate
            )
        )
    }

    override fun handleUrlTapped(url: String) {
        clearToolbarFocus()

        activity.openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = false,
            from = BrowserDirection.FromSearchDialog
        )
    }

    override fun handleSearchTermsTapped(searchTerms: String) {
        clearToolbarFocus()

        val searchEngine = fragmentStore.state.searchEngineSource.searchEngine

        activity.openToBrowserAndLoad(
            searchTermOrURL = searchTerms,
            newTab = false,
            from = BrowserDirection.FromSearchDialog,
            engine = searchEngine,
            forceSearch = true
        )
    }

    override fun handleSearchShortcutEngineSelected(searchEngine: SearchEngine) {
        focusToolbar()
        fragmentStore.dispatch(SearchFragmentAction.SearchShortcutEngineSelected(searchEngine))
    }

    override fun handleSearchShortcutsButtonClicked() {
        val isOpen = fragmentStore.state.showSearchShortcuts
        fragmentStore.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(!isOpen))
    }

    override fun handleClickSearchEngineSettings() {
        clearToolbarFocus()
    }

    override fun handleExistingSessionSelected(tabId: String) {
        clearToolbarFocus()

        tabsUseCases.selectTab(tabId)

        activity.openToBrowser(
            from = BrowserDirection.FromSearchDialog
        )
    }
}
