package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.engine.Engine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext


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
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.clear_tabs))
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ ->
                GlobalScope.launch{
                    requireContext().components.tabsUseCases.removeAllTabs.invoke()
                }
                Toast.makeText(context, R.string.tabs_cleared, Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun clearHistory(){
        val historyDialog = AlertDialog.Builder(requireContext()).create()
        val inflater = requireContext().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout: View =
            inflater.inflate(R.layout.dialog_clear_history, null)

        val timeArray: Array<Long> = arrayOf(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2),
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7),
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30),
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(365),
            0)
        historyDialog.setView(layout)
        historyDialog.show()

        val spinner: Spinner = layout.findViewById(R.id.timeSpinner)
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.history_delete_times,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        layout.findViewById<Button>(R.id.clearButton).setOnClickListener {
            GlobalScope.launch{
                requireContext().components.historyStorage.deleteVisitsSince(timeArray[spinner.selectedItemPosition])
            }
            Toast.makeText(context, R.string.history_cleared, Toast.LENGTH_LONG).show()
            historyDialog.dismiss()
        }

        layout.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            historyDialog.dismiss()
        }
    }

    private fun clearCookies(){
         requireContext().components.engine.clearData(
                Engine.BrowsingData.select(
                    Engine.BrowsingData.COOKIES,
                    Engine.BrowsingData.AUTH_SESSIONS
                )
            )
        Toast.makeText(context, R.string.cookies_cleared, Toast.LENGTH_LONG).show()
    }

    private fun clearCache(){
        requireContext().components.engine.clearData(
            Engine.BrowsingData.select(Engine.BrowsingData.ALL_CACHES)
        )
        Toast.makeText(context, R.string.cache_cleared, Toast.LENGTH_LONG).show()
    }

    private fun clearPermissions(){
        requireContext().components.engine.clearData(
            Engine.BrowsingData.select(Engine.BrowsingData.PERMISSIONS)
        )
        GlobalScope.launch { components.permissionStorage.removeAll() }
        Toast.makeText(context, R.string.permissions_cleared, Toast.LENGTH_LONG).show()
    }
}