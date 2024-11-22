package com.khalyl.android.kyubi.settings.fragment

import android.os.Bundle
import androidx.core.content.pm.PackageInfoCompat
import com.khalyl.android.kyubi.R
import com.khalyl.android.kyubi.settings.fragment.BaseSettingsFragment
import mozilla.components.Build
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
            summary = Build.version + ", " + Build.gitHash,
            onClick = { }
        )

    }

}