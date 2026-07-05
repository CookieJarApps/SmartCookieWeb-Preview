package com.cookiejarapps.android.smartcookieweb.browser

import android.content.Context
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.feature.search.ext.createSearchEngine

/**
 * Applies the search engine currently stored in [UserPreferences] to the browser store.
 *
 * @param scope Coroutine scope used to load the custom engine's icon.
 */
fun Context.applySelectedSearchEngine(scope: CoroutineScope) {
    val components = components
    val prefs = UserPreferences(this)

    // We don't support keeping a list of custom engines, only setting one as default,
    // so clear any previously added custom engine first.
    for (customEngine in components.store.state.search.customSearchEngines) {
        components.searchUseCases.removeSearchEngine(customEngine)
    }

    if (prefs.customSearchEngine) {
        scope.launch {
            val customSearch = createSearchEngine(
                name = "Custom Search",
                url = prefs.customSearchEngineURL,
                icon = components.icons.loadIcon(IconRequest(prefs.customSearchEngineURL))
                    .await().bitmap
            )

            withContext(Dispatchers.Main) {
                components.searchUseCases.addSearchEngine(customSearch)
                components.searchUseCases.selectSearchEngine(customSearch)
            }
        }
    } else {
        val engine = SearchEngineList().getEngines()[prefs.searchEngineChoice]
        if (engine.type != SearchEngine.Type.BUNDLED) {
            components.searchUseCases.addSearchEngine(engine)
        }
        components.searchUseCases.selectSearchEngine(engine)
    }
}
