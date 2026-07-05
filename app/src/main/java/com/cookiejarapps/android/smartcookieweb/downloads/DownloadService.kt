package com.cookiejarapps.android.smartcookieweb.downloads

import com.cookiejarapps.android.smartcookieweb.R
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.feature.downloads.DefaultPackageNameProvider
import mozilla.components.feature.downloads.DownloadEstimator
import mozilla.components.feature.downloads.filewriter.DefaultDownloadFileWriter
import com.cookiejarapps.android.smartcookieweb.ext.components
import mozilla.components.support.base.android.NotificationsDelegate

class DownloadService : AbstractFetchDownloadService() {
    override val httpClient by lazy { components.client }
    override val store: BrowserStore by lazy { components.store }
    override val style: Style by lazy { Style(R.color.photonBlue40) }
    override val fileSizeFormatter by lazy { components.fileSizeFormatter }
    override val notificationsDelegate: NotificationsDelegate by lazy { components.notificationsDelegate }
    override val packageNameProvider by lazy { DefaultPackageNameProvider(applicationContext) }
    override val downloadEstimator by lazy { DownloadEstimator(components.dateTimeProvider) }
    override val downloadFileUtils by lazy { components.downloadFileUtils }
    override val downloadFileWriter by lazy {
        DefaultDownloadFileWriter(applicationContext, components.downloadFileUtils)
    }
}
