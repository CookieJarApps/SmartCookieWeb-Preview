package com.cookiejarapps.android.smartcookieweb

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.cookiejarapps.android.smartcookieweb.ext.components
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.feature.intent.processing.TabIntentProcessor

class IntentReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainScope().launch {
            val intent = intent?.let { Intent(it) } ?: Intent()

            intent.flags = intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
            intent.flags = intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK.inv()

            intent.flags = intent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS.inv()

            val activityClass = BrowserActivity::class
            val processor = TabIntentProcessor(components.tabsUseCases, components.searchUseCases.newTabSearch, isPrivate = false)
            processor.process(intent)

            intent.setClassName(applicationContext, activityClass.java.name)

            startActivity(intent)
            finish()
        }
    }
}