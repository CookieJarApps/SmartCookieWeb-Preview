package com.cookiejarapps.android.smartcookieweb.settings.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.browser.ThemeChoice
import androidx.appcompat.app.AppCompatDelegate
import com.cookiejarapps.android.smartcookieweb.settings.fragment.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (UserPreferences(this).appThemeChoice == ThemeChoice.SYSTEM.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else if (UserPreferences(this).appThemeChoice == ThemeChoice.LIGHT.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, SettingsFragment())
            .commit()

        // add back arrow to toolbar
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}