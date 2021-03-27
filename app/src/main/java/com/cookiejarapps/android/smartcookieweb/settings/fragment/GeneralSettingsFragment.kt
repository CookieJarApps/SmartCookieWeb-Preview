package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.SearchEngineList
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GeneralSettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_general)

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_javascript_enabled),
            isChecked = UserPreferences(requireContext()).javaScriptEnabled,
            onCheckChange = {
                UserPreferences(requireContext()).javaScriptEnabled = it
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_dark_mode),
            isChecked = UserPreferences(requireContext()).darkModeEnabled,
            onCheckChange = {
                UserPreferences(requireContext()).darkModeEnabled = it
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_follow_system),
            isChecked = UserPreferences(requireContext()).followSystem,
            onCheckChange = {
                UserPreferences(requireContext()).followSystem = it
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
        )

        clickablePreference(
            preference = resources.getString(R.string.key_search_engine),
            onClick = { pickSearchEngine() }
        )

    }

    fun pickSearchEngine(){
        val startingChoice = UserPreferences(requireContext()).searchEngineChoice
        val singleItems = emptyList<String>().toMutableList()
        val checkedItem = UserPreferences(requireContext()).searchEngineChoice

        for(i in SearchEngineList().engines){
            singleItems.add(i.name)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.search_engine))
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                UserPreferences(requireContext()).searchEngineChoice = startingChoice
            }
            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, which ->
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                UserPreferences(requireContext()).searchEngineChoice = which
            }
            .show()
    }
}