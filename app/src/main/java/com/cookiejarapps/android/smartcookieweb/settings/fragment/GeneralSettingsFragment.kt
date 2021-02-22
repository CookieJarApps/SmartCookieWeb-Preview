package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.os.Bundle
import android.widget.Toast
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import javax.inject.Inject
import com.cookiejarapps.android.smartcookieweb.di.injector

class GeneralSettingsFragment : BaseSettingsFragment() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        injector.inject(this)

        addPreferencesFromResource(R.xml.preferences_general)

        switchPreference(
            preference = requireContext().resources.getString(R.string.key_javascript_enabled),
            isChecked = userPreferences.javaScriptEnabled,
            onCheckChange = {
                userPreferences.javaScriptEnabled = it
                Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
            }
        )
    }
}