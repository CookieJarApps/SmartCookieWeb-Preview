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
import mozilla.components.concept.engine.Engine

class PrivacyAndSecuritySettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_privacy_and_security)

        clickablePreference(
            preference = resources.getString(R.string.key_clear_tabs),
            onClick = { clearTabs() }
        )
        clickablePreference(
            preference = resources.getString(R.string.key_clear_history),
            onClick = { clearHistory() }
        )
        clickablePreference(
            preference = resources.getString(R.string.key_clear_cookies),
            onClick = { clearCookies() }
        )
        clickablePreference(
            preference = resources.getString(R.string.key_clear_cache),
            onClick = { clearCache() }
        )
        clickablePreference(
            preference = resources.getString(R.string.key_clear_permissions),
            onClick = { clearPermissions() }
        )

    }

    private fun clearTabs(){
        GlobalScope.launch{
            requireContext().components.tabsUseCases.removeAllTabs
        }
        Toast.makeText(context, R.string.tabs_cleared, Toast.LENGTH_LONG).show()
    }

    private fun clearHistory(){
        GlobalScope.launch{
            requireContext().components.historyStorage.deleteVisitsSince(0)
        }
        Toast.makeText(context, R.string.history_cleared, Toast.LENGTH_LONG).show()
    }

    private fun clearCookies(){
        GlobalScope.launch{
            requireContext().components.engine.clearData(
                Engine.BrowsingData.select(
                    Engine.BrowsingData.COOKIES,
                    Engine.BrowsingData.AUTH_SESSIONS
                )
            )
        }
        Toast.makeText(context, R.string.cookies_cleared, Toast.LENGTH_LONG).show()
    }

    private fun clearCache(){
        GlobalScope.launch{
            requireContext().components.engine.clearData(
                Engine.BrowsingData.select(Engine.BrowsingData.ALL_CACHES)
            )
        }
        Toast.makeText(context, R.string.cache_cleared, Toast.LENGTH_LONG).show()
    }

    private fun clearPermissions(){
        GlobalScope.launch{
            requireContext().components.engine.clearData(
                    Engine.BrowsingData.select(Engine.BrowsingData.ALL_SITE_SETTINGS)
                )
        }
        Toast.makeText(context, R.string.permissions_cleared, Toast.LENGTH_LONG).show()
    }
}