package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.SearchEngineList
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PrivacyAndSecuritySettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_privacy_and_security)

        clickablePreference(
            preference = resources.getString(R.string.key_clear_history),
            onClick = { pickSearchEngine() }
        )

    }

    fun pickSearchEngine(){
        GlobalScope.launch{
            requireContext().components.historyStorage.deleteVisitsSince(0)
        }
        Toast.makeText(context, R.string.history_cleared, Toast.LENGTH_LONG).show()
    }
}