package com.khalyl.android.kyubi

import android.content.Intent
import androidx.navigation.NavController

/**
 * Processor for Android intents received in [com.khalyl.android.kyubi.BrowserActivity].
 */
interface HomeIntentProcessor {
    fun process(intent: Intent, navController: NavController, out: Intent): Boolean
}
