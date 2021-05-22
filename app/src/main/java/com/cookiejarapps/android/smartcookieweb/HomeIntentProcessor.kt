package com.cookiejarapps.android.smartcookieweb

import android.content.Intent
import androidx.navigation.NavController

/**
 * Processor for Android intents received in [com.cookiejarapps.android.smartcookieweb.BrowserActivity].
 */
interface HomeIntentProcessor {
    fun process(intent: Intent, navController: NavController, out: Intent): Boolean
}
