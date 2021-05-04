package com.cookiejarapps.android.smartcookieweb.browser

import android.content.Context
import android.graphics.Bitmap
import com.cookiejarapps.android.smartcookieweb.ext.components
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.availableSearchEngines
import mozilla.components.browser.state.state.searchEngines

class SearchEngineList {
    fun getEngines(): List<SearchEngine> {
        return listOf(
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
            ),
            SearchEngine(
                id = "bing",
                name = "Bing",
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                type = SearchEngine.Type.BUNDLED,
                resultUrls = listOf("https://www.bing.com/?q={searchTerms}"),
                suggestUrl = "https://www.bing.com/"
            ),
            SearchEngine(
                id = "baidu",
                name = "Baidu",
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                type = SearchEngine.Type.CUSTOM,
                resultUrls = listOf("https://www.baidu.com/s?wd={searchTerms}"),
                suggestUrl = "https://www.baidu.com/"
            ),
            SearchEngine(
                id = "yandex",
                name = "Yandex",
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                type = SearchEngine.Type.CUSTOM,
                resultUrls = listOf("https://yandex.com/search/?text={searchTerms}"),
                suggestUrl = "https://www.yandex.com/"
            ),
            SearchEngine(
                id = "naver",
                name = "Naver",
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                type = SearchEngine.Type.CUSTOM,
                resultUrls = listOf("https://www.naver.com/search.naver?query={searchTerms}"),
                suggestUrl = "https://www.naver.com/"
            ),
            SearchEngine(
                id = "qwant",
                name = "Qwant",
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                type = SearchEngine.Type.CUSTOM,
                resultUrls = listOf("https://qwant.com/?q={searchTerms}")
            )
        )
    }
}