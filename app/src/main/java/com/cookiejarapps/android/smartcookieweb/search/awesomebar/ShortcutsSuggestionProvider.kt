package com.cookiejarapps.android.smartcookieweb.search.awesomebar

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.cookiejarapps.android.smartcookieweb.R
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.searchEngines
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.awesomebar.AwesomeBar
import java.util.UUID

class ShortcutsSuggestionProvider(
    private val store: BrowserStore,
    private val context: Context,
    private val selectShortcutEngine: (engine: SearchEngine) -> Unit
) : AwesomeBar.SuggestionProvider {
    override val id: String = UUID.randomUUID().toString()

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        val suggestions = mutableListOf<AwesomeBar.Suggestion>()

        store.state.search.searchEngines.mapTo(suggestions) {
            AwesomeBar.Suggestion(
                provider = this,
                id = it.id,
                icon = it.icon,
                title = it.name,
                onSuggestionClicked = {
                    selectShortcutEngine(it)
                }
            )
        }
        
        return suggestions
    }
}
