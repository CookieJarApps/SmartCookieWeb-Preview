package com.cookiejarapps.android.smartcookieweb.search.awesomebar

import mozilla.components.browser.state.search.SearchEngine

interface AwesomeBarInteractor {

    fun onUrlTapped(url: String)

    fun onSearchTermsTapped(searchTerms: String)

    fun onSearchShortcutEngineSelected(searchEngine: SearchEngine)

    fun onClickSearchEngineSettings()

    fun onExistingSessionSelected(tabId: String)

    fun onSearchShortcutsButtonClicked()

}
