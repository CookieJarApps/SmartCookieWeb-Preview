package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.os.Bundle
import android.widget.Toast
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import javax.inject.Inject

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

    }
}