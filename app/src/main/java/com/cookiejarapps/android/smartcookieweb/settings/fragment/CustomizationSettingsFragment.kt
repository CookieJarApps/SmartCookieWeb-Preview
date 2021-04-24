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


class CustomizationSettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_customization)

        switchPreference(
                preference = requireContext().resources.getString(R.string.key_move_navbar),
                isChecked = UserPreferences(requireContext()).shouldUseBottomToolbar,
                onCheckChange = {
                    UserPreferences(requireContext()).shouldUseBottomToolbar = it
                    Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
                }
        )

        switchPreference(
                preference = requireContext().resources.getString(R.string.key_show_addons_in_bar),
                isChecked = UserPreferences(requireContext()).showAddonsInBar,
                onCheckChange = {
                    UserPreferences(requireContext()).showAddonsInBar = it
                    Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
                }
        )

        clickablePreference(
                preference = requireContext().resources.getString(R.string.key_app_theme_type),
                onClick = { pickAppTheme() }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_web_theme_type),
            onClick = { pickWebTheme() }
        )

    }

    private fun pickAppTheme(){
        val startingChoice = UserPreferences(requireContext()).appThemeChoice
        val singleItems = resources.getStringArray(R.array.theme_types).toMutableList()
        val checkedItem = UserPreferences(requireContext()).appThemeChoice

        MaterialAlertDialogBuilder(requireContext())
                .setTitle(resources.getString(R.string.theme))
                .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                    UserPreferences(requireContext()).appThemeChoice = startingChoice
                }
                .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ ->}
                .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                    UserPreferences(requireContext()).appThemeChoice = which
                }
                .show()
    }

    private fun pickWebTheme(){
        val startingChoice = UserPreferences(requireContext()).webThemeChoice
        val singleItems = resources.getStringArray(R.array.theme_types).toMutableList()
        val checkedItem = UserPreferences(requireContext()).webThemeChoice

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.theme))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                UserPreferences(requireContext()).webThemeChoice = startingChoice
            }
            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ ->}
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                UserPreferences(requireContext()).webThemeChoice = which
            }
            .show()
    }

}