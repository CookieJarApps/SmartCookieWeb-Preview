package com.cookiejarapps.android.smartcookieweb.media

import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.media.service.AbstractMediaSessionService
import com.cookiejarapps.android.smartcookieweb.ext.components
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.support.base.android.NotificationsDelegate

class MediaSessionService : AbstractMediaSessionService() {
    override val crashReporter: CrashReporting? = null
    override val notificationsDelegate: NotificationsDelegate by lazy { components.notificationsDelegate }
    override val store: BrowserStore by lazy { components.store }
}
