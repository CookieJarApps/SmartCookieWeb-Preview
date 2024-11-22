package com.khalyl.android.kyubi.settings.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.khalyl.android.kyubi.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_headers)
    }
}