package com.cookiejarapps.android.smartcookieweb.media

import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.media.service.AbstractMediaSessionService
import com.cookiejarapps.android.smartcookieweb.ext.components

class MediaSessionService : AbstractMediaSessionService() {
    override val store: BrowserStore by lazy { components.store }
}
