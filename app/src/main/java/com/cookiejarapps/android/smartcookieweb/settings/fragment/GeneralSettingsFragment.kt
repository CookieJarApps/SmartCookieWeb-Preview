package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.settings.HomepageChoice
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
                    if(UserPreferences(requireContext()).homepageType == HomepageChoice.CUSTOM_PAGE.ordinal){
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle(R.string.custom_page)

                        val input = EditText(context)
                        input.inputType = InputType.TYPE_CLASS_TEXT
                        builder.setView(input)

                        input.setText(UserPreferences(requireContext()).customHomepageUrl)

                        builder.setPositiveButton(
                            "OK"
                        ) { dialog, which ->
                            UserPreferences(requireContext()).customHomepageUrl = input.text.toString()
                            Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
                        }
                        builder.setNegativeButton(
                            "Cancel"
                        ) { dialog, which ->

                        }

                        builder.show()
                    }
                }
                .show()
    }

    private fun pickSearchEngine(){
        val startingChoice = UserPreferences(requireContext()).searchEngineChoice
        val singleItems = emptyList<String>().toMutableList()

        for(i in SearchEngineList().getEngines()){
            singleItems.add(i.name)
        }

        singleItems.add(resources.getString(R.string.custom))

        val checkedItem = if(!UserPreferences(requireContext()).customSearchEngine) UserPreferences(requireContext()).searchEngineChoice else singleItems.size - 1

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.search_engine))
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ ->
                UserPreferences(requireContext()).searchEngineChoice = startingChoice
            }
            .setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { _, _ ->
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
            .setSingleChoiceItems(singleItems.toTypedArray(), checkedItem) { dialog, which ->
                if(which == singleItems.size - 1){
                    customSearchEngineDialog()
                    dialog.cancel()
                }
                else{
                    UserPreferences(requireContext()).customSearchEngine = false
                    UserPreferences(requireContext()).searchEngineChoice = which
                }
            }
            .show()
    }

    fun customSearchEngineDialog(){
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.custom_search_engine)
        builder.setMessage(R.string.custom_search_engine_details)

        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        input.setText(UserPreferences(requireContext()).customSearchEngineURL)

        builder.setPositiveButton(
            "OK"
        ) { dialog, which ->
            if(input.text.toString().contains("{searchTerms}")){
                UserPreferences(requireContext()).customSearchEngine = true
                UserPreferences(requireContext()).customSearchEngineURL = input.text.toString()
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(context, R.string.custom_search_engine_error, Toast.LENGTH_LONG).show()
                customSearchEngineDialog()
            }
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which ->
            UserPreferences(requireContext()).customSearchEngine = false
        }

        builder.show()
    }
}