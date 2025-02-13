package com.cookiejarapps.android.smartcookieweb.settings.activity

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.settings.ThemeChoice
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom
            )
            val insetsController = WindowCompat.getInsetsController(window, v)
            insetsController.isAppearanceLightStatusBars = UserPreferences(this).appThemeChoice != ThemeChoice.LIGHT.ordinal
            WindowInsetsCompat.CONSUMED
        }
        supportActionBar?.elevation = 0f
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}