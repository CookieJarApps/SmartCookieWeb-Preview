package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.os.Bundle
import android.widget.Toast
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import javax.inject.Inject
import com.cookiejarapps.android.smartcookieweb.di.injector

class AboutSettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        injector.inject(this)

        addPreferencesFromResource(R.xml.preferences_about)
    }
}