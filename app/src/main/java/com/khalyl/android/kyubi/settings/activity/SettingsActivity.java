package com.khalyl.android.kyubi.settings.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.khalyl.android.kyubi.R;
import com.khalyl.android.kyubi.settings.ThemeChoice;
import com.khalyl.android.kyubi.preferences.UserPreferences;
import com.khalyl.android.kyubi.settings.fragment.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if(new UserPreferences(this).getAppThemeChoice() == ThemeChoice.SYSTEM.ordinal()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if(new UserPreferences(this).getAppThemeChoice() == ThemeChoice.LIGHT.ordinal()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}