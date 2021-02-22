package com.cookiejarapps.android.smartcookieweb.settings.fragment

class SummaryUpdater(private val preference: androidx.preference.Preference) {

    fun updateSummary(text: String) {
        preference.summary = text
    }

}