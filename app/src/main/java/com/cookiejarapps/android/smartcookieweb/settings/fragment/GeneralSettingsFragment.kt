package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.preference.Preference
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

        clickablePreference(
                preference = resources.getString(R.string.key_search_engine),
                onClick = { pickSearchEngine() }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_search_suggestions_enabled),
            isChecked = UserPreferences(requireContext()).searchSuggestionsEnabled,
            onCheckChange = {
                UserPreferences(requireContext()).searchSuggestionsEnabled = it
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_safe_browsing),
            isChecked = UserPreferences(requireContext()).safeBrowsing,
            onCheckChange = {
                UserPreferences(requireContext()).safeBrowsing = it
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
        )

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_tracking_protection),
            isChecked = UserPreferences(requireContext()).trackingProtection,
            onCheckChange = {
                UserPreferences(requireContext()).trackingProtection = it
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
        )

        clickablePreference(
                preference = resources.getString(R.string.key_homepage_type),
                onClick = { pickHomepage() }
        )

    }

    private fun pickHomepage(){
        val startingChoice = UserPreferences(requireContext()).homepageType
        val singleItems = resources.getStringArray(R.array.homepage_types).toMutableList()
        val checkedItem = UserPreferences(requireContext()).homepageType

        MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.homepage_type))
                .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                    UserPreferences(requireContext()).homepageType = startingChoice
                }
                .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ ->}
                .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                    UserPreferences(requireContext()).homepageType = which
                }
                .show()
    }

    private fun pickSearchEngine(){
        val startingChoice = UserPreferences(requireContext()).searchEngineChoice
        val singleItems = emptyList<String>().toMutableList()
        val checkedItem = UserPreferences(requireContext()).searchEngineChoice

        for(i in SearchEngineList().getEngines()){
            singleItems.add(i.name)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.search_engine))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                UserPreferences(requireContext()).searchEngineChoice = startingChoice
            }
            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ ->
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { _, which ->
                UserPreferences(requireContext()).searchEngineChoice = which
            }
            .show()
    }
}