package com.khalyl.android.kyubi.settings.fragment

import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat

abstract class BaseSettingsFragment : PreferenceFragmentCompat() {

    protected fun switchPreference(
        preference: String,
        isChecked: Boolean,
        isEnabled: Boolean = true,
        summary: String? = null,
        onCheckChange: (Boolean) -> Unit
    ) = findPreference<androidx.preference.SwitchPreferenceCompat>(preference)?.apply {
        this.isChecked = isChecked
        this.isEnabled = isEnabled
        summary?.let {
            this.summary = summary
        }
        onPreferenceChangeListener = OnPreferenceChangeListener { _, any: Any ->
            onCheckChange(any as Boolean)
            true
        }
    }

    protected fun clickablePreference(
        preference: String,
        isEnabled: Boolean = true,
        summary: String? = null,
        onClick: () -> Unit
    ) = clickableDynamicPreference(
        preference = preference,
        isEnabled = isEnabled,
        summary = summary,
        onClick = { onClick() }
    )

    protected fun clickableDynamicPreference(
        preference: String,
        isEnabled: Boolean = true,
        summary: String? = null,
        onClick: (SummaryUpdater) -> Unit
    ) = findPreference<Preference>(preference)?.apply {
        this.isEnabled = isEnabled
        summary?.let {
            this.summary = summary
        }
        val summaryUpdate = SummaryUpdater(this)
        onPreferenceClickListener = OnPreferenceClickListener {
            onClick(summaryUpdate)
            true
        }
    }

    protected fun seekbarPreference(
        preference: String,
        isEnabled: Boolean = true,
        summary: String? = null,
        onStateChanged: (Int) -> Unit
    ) = findPreference<androidx.preference.SeekBarPreference>(preference)?.apply {
        this.isEnabled = isEnabled
        summary?.let {
            this.summary = summary
        }
        val summaryUpdate = SummaryUpdater(this)
        onPreferenceChangeListener = OnPreferenceChangeListener { preference: Preference, newValue: Any ->
            onStateChanged(newValue as Int)
            true
        }
    }

}