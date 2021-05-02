package com.cookiejarapps.android.smartcookieweb.addons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.BuildConfig
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.ThemeChoice
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.feature.addons.Addon
import mozilla.components.feature.addons.AddonManagerException
import mozilla.components.feature.addons.ui.translateName

/**
 * An activity to show the details of a installed add-on.
 */
@Suppress("LargeClass")
class InstalledAddonDetailsActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_installed_add_on_details)
        val addon = requireNotNull(intent.getParcelableExtra<Addon>("add_on"))

        if(UserPreferences(this).appThemeChoice == ThemeChoice.SYSTEM.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else if(UserPreferences(this).appThemeChoice == ThemeChoice.LIGHT.ordinal) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        bindAddon(addon)
    }

    private fun bindAddon(addon: Addon) {
        scope.launch {
            try {
                val context = baseContext
                val addons = context.components.addonManager.getAddons()
                scope.launch(Dispatchers.Main) {
                    addons.find { addon.id == it.id }.let {
                        if (it == null) {
                            throw AddonManagerException(Exception("Addon ${addon.id} not found"))
                        } else {
                            bindUI(it)
                        }
                    }
                }
            } catch (e: AddonManagerException) {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(
                            baseContext,
                            R.string.mozac_feature_addons_failed_to_query_add_ons,
                            Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun bindUI(addon: Addon) {
        title = addon.translateName(this)

        bindEnableSwitch(addon)

        bindSettings(addon)

        bindDetails(addon)

        bindPermissions(addon)

        bindRate(addon)

        bindReport(addon)

        bindAllowInPrivateBrowsingSwitch(addon)

        bindRemoveButton(addon)
    }

    private fun bindEnableSwitch(addon: Addon) {
        val switch = findViewById<SwitchMaterial>(R.id.enable_switch)
        switch.setState(addon.isEnabled())
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                this.components.addonManager.enableAddon(
                        addon,
                        onSuccess = {
                            switch.setState(true)
                            Toast.makeText(
                                    this,
                                    getString(R.string.mozac_feature_addons_successfully_enabled, addon.translateName(this)),
                                    Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = {
                            Toast.makeText(
                                    this,
                                    getString(R.string.mozac_feature_addons_failed_to_enable, addon.translateName(this)),
                                    Toast.LENGTH_SHORT
                            ).show()
                        }
                )
            } else {
                this.components.addonManager.disableAddon(
                        addon,
                        onSuccess = {
                            switch.setState(false)
                            Toast.makeText(
                                    this,
                                    getString(R.string.mozac_feature_addons_successfully_disabled, addon.translateName(this)),
                                    Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = {
                            Toast.makeText(
                                    this,
                                    getString(R.string.mozac_feature_addons_failed_to_disable, addon.translateName(this)),
                                    Toast.LENGTH_SHORT
                            ).show()
                        }
                )
            }
        }
    }

    private fun bindSettings(addon: Addon) {
        val view = findViewById<View>(R.id.settings)
        val optionsPageUrl = addon.installedState?.optionsPageUrl
        view.isEnabled = optionsPageUrl != null
        view.setOnClickListener {
            if (addon.installedState?.openOptionsPageInTab == true) {
                components.tabsUseCases.addTab(optionsPageUrl as String)
                val intent = Intent(this, BrowserActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                this.startActivity(intent)
            } else {
                val intent = Intent(this, AddonSettingsActivity::class.java)
                intent.putExtra("add_on", addon)
                this.startActivity(intent)
            }
        }
    }

    private fun bindDetails(addon: Addon) {
        findViewById<View>(R.id.details).setOnClickListener {
            val intent = Intent(this, AddonDetailsActivity::class.java)
            intent.putExtra("add_on", addon)
            this.startActivity(intent)
        }
    }

    private fun bindPermissions(addon: Addon) {
        findViewById<View>(R.id.permissions).setOnClickListener {
            val intent = Intent(this, PermissionsDetailsActivity::class.java)
            intent.putExtra("add_on", addon)
            this.startActivity(intent)
        }
    }

    private fun bindRate(addon: Addon) {
        if(UserPreferences(applicationContext).customAddonCollection){
            findViewById<View>(R.id.rate).visibility = View.GONE
        }

        findViewById<View>(R.id.rate).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(
                    "https://addons.smartcookieweb.com/addon?id=${addon.id}&rate=true")
            intent.setPackage(BuildConfig.APPLICATION_ID)
            this.startActivity(intent)
        }
    }

    private fun bindReport(addon: Addon) {
        if(UserPreferences(applicationContext).customAddonCollection){
            findViewById<View>(R.id.report).visibility = View.GONE
        }

        findViewById<View>(R.id.report).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(
                "https://addons.smartcookieweb.com/addon?id=${addon.id}&report=true")
            intent.setPackage(BuildConfig.APPLICATION_ID)
            this.startActivity(intent)
        }
    }

    private fun bindAllowInPrivateBrowsingSwitch(addon: Addon) {
        val switch = findViewById<SwitchCompat>(R.id.allow_in_private_browsing_switch)
        switch.isChecked = addon.isAllowedInPrivateBrowsing()
        switch.setOnCheckedChangeListener { _, isChecked ->
            this.components.addonManager.setAddonAllowedInPrivateBrowsing(
                    addon,
                    isChecked,
                    onSuccess = {
                        switch.isChecked = isChecked
                    }
            )
        }
    }

    private fun bindRemoveButton(addon: Addon) {
        findViewById<View>(R.id.remove_add_on).setOnClickListener {
            this.components.addonManager.uninstallAddon(
                    addon,
                    onSuccess = {
                        Toast.makeText(
                                this,
                                getString(R.string.mozac_feature_addons_successfully_uninstalled, addon.translateName(this)),
                                Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    },
                    onError = { _, _ ->
                        Toast.makeText(
                                this,
                                getString(R.string.mozac_feature_addons_failed_to_uninstall, addon.translateName(this)),
                                Toast.LENGTH_SHORT
                        ).show()
                    }
            )
        }
    }

    private fun SwitchCompat.setState(checked: Boolean) {
        val text = if (checked) {
            R.string.mozac_feature_addons_enabled
        } else {
            R.string.mozac_feature_addons_disabled
        }
        setText(text)
        isChecked = checked
    }
}
