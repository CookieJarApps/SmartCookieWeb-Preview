package com.khalyl.android.kyubi.browser.home

import androidx.lifecycle.ViewModel
import mozilla.components.browser.state.state.content.DownloadState

class SharedViewModel : ViewModel() {
    /**
     * Stores data needed for [DynamicDownloadDialog]. See #9044
     * Format: HashMap<sessionId, Pair<DownloadState, didFail>
     * */
    var downloadDialogState: HashMap<String?, Pair<DownloadState?, Boolean>> = HashMap()
}
