package com.khalyl.android.kyubi

import android.content.Intent
import androidx.navigation.NavController
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.toSafeIntent

class OpenBrowserIntentProcessor(
    private val activity: BrowserActivity,
    private val getIntentSessionId: (SafeIntent) -> String?
) : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        return if (intent.extras?.getBoolean(BrowserActivity.OPEN_TO_BROWSER) == true) {
            out.putExtra(BrowserActivity.OPEN_TO_BROWSER, false)

            activity.openToBrowser(BrowserDirection.FromGlobal, getIntentSessionId(intent.toSafeIntent()))
            true
        } else {
            false
        }
    }
}
