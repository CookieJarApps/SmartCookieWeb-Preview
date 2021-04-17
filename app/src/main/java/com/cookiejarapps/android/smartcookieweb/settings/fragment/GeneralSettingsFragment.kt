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

        if(UserPreferences(requireContext()).customAddonCollection){
            preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_user))!!.isEnabled = true
            preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_name))!!.isEnabled = true
        }

        switchPreference(
                preference = requireContext().resources.getString(R.string.key_javascript_enabled),
                isChecked = UserPreferences(requireContext()).javaScriptEnabled,
                onCheckChange = {
                    UserPreferences(requireContext()).javaScriptEnabled = it
                    Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
                }
        )

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

        clickablePreference(
                preference = resources.getString(R.string.key_search_engine),
                onClick = { pickSearchEngine() }
        )

        clickablePreference(
                preference = resources.getString(R.string.key_homepage_type),
                onClick = { pickHomepage() }
        )

        switchPreference(
                preference = requireContext().resources.getString(R.string.key_use_custom_collection),
                isChecked = UserPreferences(requireContext()).customAddonCollection
        ) {
            if (it && !UserPreferences(requireContext()).shownCollectionDisclaimer) {
                AlertDialog.Builder(context)
                        .setTitle(resources.getString(R.string.custom_collection))
                        .setMessage(resources.getString(R.string.use_custom_collection_disclaimer))
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            UserPreferences(requireContext()).customAddonCollection = it
                            preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_user))!!.isEnabled = it
                            preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_name))!!.isEnabled = it
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
                UserPreferences(requireContext()).shownCollectionDisclaimer = true
            } else if(it) {
                UserPreferences(requireContext()).customAddonCollection = it
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_user))!!.isEnabled = it
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_name))!!.isEnabled = it
            }
            else{
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
        }

        clickablePreference(
                preference = resources.getString(R.string.key_collection_user),
                onClick = { pickCollectionUser() }
        )

        clickablePreference(
                preference = resources.getString(R.string.key_collection_name),
                onClick = { pickCollectionName() }
        )

    }

    private fun pickCollectionUser() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.collection_user))

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, which ->
            val text = input.text.toString()
            UserPreferences(requireContext()).customAddonCollectionUser = text
            Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which -> dialog.cancel() }

        builder.show()
    }

    private fun pickCollectionName() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.collection_name))

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, which ->
            val text = input.text.toString()
            UserPreferences(requireContext()).customAddonCollectionName = text
            Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which -> dialog.cancel() }

        builder.show()
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

        for(i in SearchEngineList().engines){
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