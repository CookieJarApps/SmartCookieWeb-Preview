package com.cookiejarapps.android.smartcookieweb.browser.home

import androidx.lifecycle.ViewModel

class HomeScreenViewModel : ViewModel() {
    /**
     * Used to delete a specific session once the home screen is resumed
     */
    var sessionToDelete: String? = null

    /**
     * Used to remember if we need to scroll to top of the homeFragment's recycleView (top sites) see #8561
     * */
    var shouldScrollToTopSites: Boolean = true
}
