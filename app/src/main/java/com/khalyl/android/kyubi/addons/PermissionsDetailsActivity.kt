package com.khalyl.android.kyubi.addons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.khalyl.android.kyubi.R
import com.khalyl.android.kyubi.settings.ThemeChoice
import com.khalyl.android.kyubi.preferences.UserPreferences
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.ui.AddonPermissionsAdapter
import mozilla.components.feature.addons.ui.translateName

private const val LEARN_MORE_URL =
    "https://smartcookieweb.com/help-biscuit/extensions/#permission-requests"


// An activity to show the permissions of an add-on.
class PermissionsDetailsActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_on_permissions)
        val addon = requireNotNull(intent.getParcelableExtra<Addon>("add_on"))
        title = addon.translateName(this)

        if(UserPreferences(this).appThemeChoice == ThemeChoice.SYSTEM.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else if(UserPreferences(this).appThemeChoice == ThemeChoice.LIGHT.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.add_ons_permissions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val sortedPermissions = addon.translatePermissions(this).sorted()
        recyclerView.adapter = AddonPermissionsAdapter(sortedPermissions)

        findViewById<View>(R.id.learn_more_label).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val intent =
            Intent(Intent.ACTION_VIEW).setData(Uri.parse(LEARN_MORE_URL))
        startActivity(intent)
    }
}
