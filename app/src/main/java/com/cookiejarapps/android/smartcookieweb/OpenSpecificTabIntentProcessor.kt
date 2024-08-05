package com.cookiejarapps.android.smartcookieweb

import android.content.Intent
import androidx.navigation.NavController
import com.cookiejarapps.android.smartcookieweb.ext.components
import mozilla.components.browser.state.selector.findTab
import mozilla.components.feature.media.service.AbstractMediaSessionService


class OpenSpecificTabIntentProcessor(
    private val activity: BrowserActivity
) : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        if (intent.action == AbstractMediaSessionService.Companion.ACTION_SWITCH_TAB) {
            activity.components.store
            val tabId = intent.extras?.getString(AbstractMediaSessionService.Companion.EXTRA_TAB_ID)

            if (tabId != null) {
                activity.components.tabsUseCases.selectTab(tabId)
                activity.openToBrowser(BrowserDirection.FromGlobal)
                return true
            }
        }

        return false
    }
}