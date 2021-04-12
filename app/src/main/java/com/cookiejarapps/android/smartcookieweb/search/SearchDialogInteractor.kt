package com.cookiejarapps.android.smartcookieweb.search

import mozilla.components.browser.state.search.SearchEngine
import com.cookiejarapps.android.smartcookieweb.search.awesomebar.AwesomeBarInteractor
import com.cookiejarapps.android.smartcookieweb.search.toolbar.ToolbarInteractor

/**
 * Interactor for the search screen
 * Provides implementations for the AwesomeBarView and ToolbarView
 */
@Suppress("TooManyFunctions")
class SearchDialogInteractor(
    private val searchController: SearchDialogController
) : AwesomeBarInteractor, ToolbarInteractor {

    override fun onUrlCommitted(url: String) {
        searchController.handleUrlCommitted(url)
    }

    override fun onEditingCanceled() {
        searchController.handleEditingCancelled()
    }

    override fun onTextChanged(text: String) {
        searchController.handleTextChanged(text)
    }

    override fun onUrlTapped(url: String) {
        searchController.handleUrlTapped(url)
    }

    override fun onSearchTermsTapped(searchTerms: String) {
        searchController.handleSearchTermsTapped(searchTerms)
    }

    override fun onSearchShortcutEngineSelected(searchEngine: SearchEngine) {
        searchController.handleSearchShortcutEngineSelected(searchEngine)
    }

    override fun onSearchShortcutsButtonClicked() {
        searchController.handleSearchShortcutsButtonClicked()
    }

    override fun onClickSearchEngineSettings() {
        searchController.handleClickSearchEngineSettings()
    }

    override fun onExistingSessionSelected(tabId: String) {
        searchController.handleExistingSessionSelected(tabId)
    }
}
