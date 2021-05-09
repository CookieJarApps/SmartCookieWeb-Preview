package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.preference.Preference
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.SearchEngineList
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class AdvancedSettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_advanced)

        preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_user))!!.isEnabled = UserPreferences(requireContext()).customAddonCollection
        preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_name))!!.isEnabled = UserPreferences(requireContext()).customAddonCollection

        clickablePreference(
                preference = resources.getString(R.string.key_sideload_xpi),
                onClick = { sideloadXpi() }
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
            } else {
                UserPreferences(requireContext()).customAddonCollection = it
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_user))!!.isEnabled = it
                preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_name))!!.isEnabled = it
                if(!it) Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
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

    private fun sideloadXpi() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.load_xpi))

        val input = EditText(requireContext())
        input.hint = getString(R.string.url)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, which ->

            components.engine.installWebExtension("", input.text.toString(), onSuccess = {
                Toast.makeText(requireContext(), "INSTALLED!", Toast.LENGTH_LONG).show()
            },
                    onError = { exception, e ->
                        Toast.makeText(requireContext(), "ERROR!", Toast.LENGTH_LONG).show()
                    })
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which -> dialog.cancel() }

        builder.show()

        /*val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setMessage(resources.getString(R.string.addon_not_available))
                .setCancelable(false)
                .setPositiveButton(R.string.mozac_feature_prompts_ok) { dialog, id ->
                    dialog.cancel()
                }*/
    }

    private fun pickCollectionUser() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.collection_user))

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        input.setText(UserPreferences(requireContext()).customAddonCollectionUser)

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

        input.setText(UserPreferences(requireContext()).customAddonCollectionName)

        builder.setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, which ->
            val text = input.text.toString()
            UserPreferences(requireContext()).customAddonCollectionName = text
            Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which -> dialog.cancel() }

        builder.show()
    }
}