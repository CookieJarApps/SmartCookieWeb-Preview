package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.cookiejarapps.android.smartcookieweb.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_headers)
    }
}