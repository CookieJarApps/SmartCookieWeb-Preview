package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.preference.Preference
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.mozilla.gecko.util.ThreadUtils.runOnUiThread


class AdvancedSettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_advanced)

        preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_user))!!.isEnabled = UserPreferences(
            requireContext()
        ).customAddonCollection
        preferenceScreen.findPreference<Preference>(resources.getString(R.string.key_collection_name))!!.isEnabled = UserPreferences(
            requireContext()
        ).customAddonCollection

        clickablePreference(
            preference = resources.getString(R.string.key_sideload_xpi),
            onClick = { sideloadXpi() }
        )

        switchPreference(
                preference = requireContext().resources.getString(R.string.key_remote_debugging),
                isChecked = UserPreferences(requireContext()).remoteDebugging
        ) {
            UserPreferences(requireContext()).remoteDebugging = it
               Toast.makeText(
                        context,
                        requireContext().resources.getText(R.string.app_restart),
                        Toast.LENGTH_LONG
                ).show()
        }

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_trust_third_party_certs),
            isChecked = UserPreferences(requireContext()).trustThirdPartyCerts
        ) {
            UserPreferences(requireContext()).trustThirdPartyCerts = it
            Toast.makeText(
                context,
                requireContext().resources.getText(R.string.app_restart),
                Toast.LENGTH_LONG
            ).show()
        }

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_prompt_external_downloader),
            isChecked = UserPreferences(requireContext()).promptExternalDownloader
        ) {
            UserPreferences(requireContext()).promptExternalDownloader = it
            Toast.makeText(
                context,
                requireContext().resources.getText(R.string.app_restart),
                Toast.LENGTH_LONG
            ).show()
        }

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
                if(!it) Toast.makeText(
                    context,
                    requireContext().resources.getText(R.string.app_restart),
                    Toast.LENGTH_LONG
                ).show()
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
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(resources.getString(R.string.load_xpi))

        val input = EditText(requireContext())
        input.hint = getString(R.string.url)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, which ->
            val loadingDialog = ProgressDialog.show(
                activity, "",
                requireContext().resources.getString(R.string.loading), true
            )

            components.engine.installWebExtension("", input.text.toString(), onSuccess = {
                CoroutineScope(Dispatchers.IO).launch {
                    val addons = requireContext().components.addonCollectionProvider.getAvailableAddons()
                    for(i in addons){
                        if(i.id == it.id){
                            runOnUiThread {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(resources.getString(R.string.error))
                                    .setMessage(resources.getString(R.string.already_available))
                                    .setNeutralButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, which ->
                                        dialog.dismiss()
                                    }
                                    .show()
                                components.engine.uninstallWebExtension(it)
                            }
                            loadingDialog.dismiss()
                            return@launch
                        }
                    }
                    runOnUiThread {
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), requireContext().resources.getString(R.string.installed), Toast.LENGTH_LONG).show()
                    }
                }
            },
                onError = { exception, e ->
                    Toast.makeText(requireContext(), requireContext().resources.getString(R.string.error), Toast.LENGTH_LONG).show()
                })
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which -> dialog.cancel() }

        builder.show()
    }

    private fun pickCollectionUser() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(resources.getString(R.string.collection_user))

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        input.setText(UserPreferences(requireContext()).customAddonCollectionUser)

        builder.setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, which ->
            val text = input.text.toString()
            UserPreferences(requireContext()).customAddonCollectionUser = text
            Toast.makeText(
                context,
                requireContext().resources.getText(R.string.app_restart),
                Toast.LENGTH_LONG
            ).show()
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which -> dialog.cancel() }

        builder.show()
    }

    private fun pickCollectionName() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(resources.getString(R.string.collection_name))

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        input.setText(UserPreferences(requireContext()).customAddonCollectionName)

        builder.setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialog, which ->
            val text = input.text.toString()
            UserPreferences(requireContext()).customAddonCollectionName = text
            Toast.makeText(
                context,
                requireContext().resources.getText(R.string.app_restart),
                Toast.LENGTH_LONG
            ).show()
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which -> dialog.cancel() }

        builder.show()
    }
}