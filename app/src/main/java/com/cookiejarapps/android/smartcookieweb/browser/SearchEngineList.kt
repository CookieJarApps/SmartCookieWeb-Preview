package com.cookiejarapps.android.smartcookieweb.browser

import android.graphics.Bitmap
import mozilla.components.browser.state.search.SearchEngine

open class SearchEngineList(){
    val engines =
        listOf(
            SearchEngine(
                id = "google",
                name = "Google",
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                type = SearchEngine.Type.BUNDLED,
                resultUrls = listOf("https://www.google.com/?q={searchTerms}"),
                suggestUrl = "https://www.google.com/"
            ),
            SearchEngine(
                id = "duckduckgo",
                name = "DuckDuckGo",
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                type = SearchEngine.Type.BUNDLED,
                resultUrls = listOf("https://www.duckduckgo.com/?q={searchTerms}"),
                suggestUrl = "https://www.duckduckgo.com/"
            )
        )
}