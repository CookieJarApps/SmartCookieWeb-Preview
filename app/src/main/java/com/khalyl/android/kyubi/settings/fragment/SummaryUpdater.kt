package com.khalyl.android.kyubi.settings.fragment

class SummaryUpdater(private val preference: androidx.preference.Preference) {

    fun updateSummary(text: String) {
        preference.summary = text
    }

}