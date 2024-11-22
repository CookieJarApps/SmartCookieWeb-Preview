package com.khalyl.android.kyubi.browser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import mozilla.components.browser.state.search.SearchEngine

class SearchEngineList {

    private var startPageLogo = Base64.decode(
        "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",
        Base64.DEFAULT)

    fun getEngines(): List<SearchEngine> {
        return listOf(
            SearchEngine(
                id = "google-b-m",
                name = "Google",
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                type = SearchEngine.Type.BUNDLED,
                resultUrls = listOf("https://www.google.com/?q={searchTerms}"),
                suggestUrl = "https://www.google.com/"
            ),
            SearchEngine(
                id = "ddg",
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
                resultUrls = listOf("https://m.search.naver.com/search.naver?query={searchTerms}"),
                suggestUrl = "https://www.naver.com/"
            ),
            SearchEngine(
                id = "qwant",
                name = "Qwant",
                icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                type = SearchEngine.Type.CUSTOM,
                resultUrls = listOf("https://qwant.com/?q={searchTerms}")
            ),
            SearchEngine(
                id = "startpage",
                name = "StartPage",
                icon = BitmapFactory.decodeByteArray(startPageLogo, 0, startPageLogo.size),
                type = SearchEngine.Type.CUSTOM,
                resultUrls = listOf("https://startpage.com/sp/search?query={searchTerms}")
            )
        )
    }
}