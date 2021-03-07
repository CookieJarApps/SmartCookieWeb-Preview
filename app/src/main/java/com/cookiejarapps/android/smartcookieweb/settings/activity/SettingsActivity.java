package com.cookiejarapps.android.smartcookieweb.settings.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;
import androidx.appcompat.widget.Toolbar;

import com.cookiejarapps.android.smartcookieweb.R;
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences;
import com.cookiejarapps.android.smartcookieweb.settings.fragment.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if(!new UserPreferences(this).getFollowSystem()){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
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