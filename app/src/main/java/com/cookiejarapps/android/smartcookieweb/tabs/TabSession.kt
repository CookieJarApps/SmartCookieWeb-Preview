package com.cookiejarapps.android.smartcookieweb.tabs

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebExtension

class TabSession : GeckoSession {
    private var mTitle: String? = null
    var uri: String? = null
        private set
    @JvmField
    var action: WebExtension.Action? = null

    constructor() : super() {}
    constructor(settings: GeckoSessionSettings?) : super(settings) {}

    var title: String?
        get() = if (mTitle == null || mTitle!!.length == 0) "about:blank" else mTitle
        set(title) {
            mTitle = title
        }

    override fun loadUri(uri: String) {
        super.loadUri(uri)
        this.uri = uri
    }

    fun onLocationChange(uri: String) {
        this.uri = uri
    }
}