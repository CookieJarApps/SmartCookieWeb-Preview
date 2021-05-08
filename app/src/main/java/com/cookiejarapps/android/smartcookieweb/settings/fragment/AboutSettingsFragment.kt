package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.pm.PackageInfoCompat
import com.cookiejarapps.android.smartcookieweb.R
import org.mozilla.geckoview.BuildConfig


class AboutSettingsFragment : BaseSettingsFragment() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_about)

        val packageInfo =
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)

        clickablePreference(
            preference = "pref_version",
            summary = "${packageInfo.versionName} (${PackageInfoCompat.getLongVersionCode(packageInfo)})",
            onClick = { }
        )

        clickablePreference(
            preference = "pref_version_geckoview",
            summary = BuildConfig.MOZ_APP_VERSION + "-" + BuildConfig.MOZ_APP_BUILDID,
            onClick = { }
        )

        clickablePreference(
            preference = "pref_version_mozac",
            summary = mozilla.components.Build.version + ", " + mozilla.components.Build.gitHash,
            onClick = { }
        )

    }

}