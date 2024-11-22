package com.khalyl.android.kyubi.addons

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentManager
import com.khalyl.android.kyubi.R
import com.khalyl.android.kyubi.settings.ThemeChoice
import com.khalyl.android.kyubi.preferences.UserPreferences


// An activity to manage add-ons.

class AddonsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        val addonId = intent.getStringExtra("ADDON_ID")
        val addonUrl = intent.getStringExtra("ADDON_URL")

        when (UserPreferences(this).appThemeChoice) {
            ThemeChoice.SYSTEM.ordinal -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            ThemeChoice.LIGHT.ordinal -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        if (savedInstanceState == null) {
            val fm: FragmentManager = supportFragmentManager
            val arguments = Bundle()
            if(addonId != null) arguments.putString("ADDON_ID", addonId)
            if(addonUrl != null) arguments.putString("ADDON_URL", addonUrl)

            val addonFragment = AddonsFragment()
            addonFragment.arguments = arguments

            fm.beginTransaction().replace(R.id.container, addonFragment).commit()
        }
    }
}
