package com.cookiejarapps.android.smartcookieweb.ssl

import android.content.Context
import android.net.http.SslCertificate
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mozilla.components.browser.state.selector.findCustomTabOrSelectedTab
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl

/**
 * Shows an informative dialog with the provided [SslCertificate] information.
 */
fun Context.showSslDialog() {
    val securityInfo = this.components.store.state.selectedTab?.content?.securityInfo
    val host = this.components.store.state.selectedTab?.content?.url?.tryGetHostFromUrl() ?: ""

    val icon = if(securityInfo?.secure == true) R.drawable.ic_baseline_lock else R.drawable.ic_baseline_lock_open
    val certAuthority = securityInfo?.issuer
    val certHost = securityInfo?.host

    val contentView = LayoutInflater.from(this).inflate(R.layout.dialog_ssl_info, null, false).apply {
        findViewById<TextView>(R.id.ssl_layout_issue_by).text = certAuthority
        findViewById<TextView>(R.id.ssl_layout_issue_to).text = certHost
    }

    MaterialAlertDialogBuilder(this)
        .setIcon(icon)
        .setTitle(host)
        .setPositiveButton(R.string.mozac_feature_prompts_ok, null)
        .setView(contentView)
        .show()
}
