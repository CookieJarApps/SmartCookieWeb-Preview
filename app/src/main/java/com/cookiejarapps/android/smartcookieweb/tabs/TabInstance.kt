package com.cookiejarapps.android.smartcookieweb.tabs

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

class TabInstance(settings: GeckoSessionSettings?) : GeckoSession(settings) {
    private var mTitle: String? = null
    var uri: String? = null
        private set
    var title: String?
        get() = if (mTitle == null || mTitle!!.length == 0) "about:blank" else mTitle
        set(title) {
            mTitle = title
        }

    override fun loadUri(uri: String) {
        super.loadUri(uri)
        this.uri = uri
    }
}