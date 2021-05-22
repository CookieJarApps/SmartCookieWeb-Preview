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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.session.ext.toSecurityInfoState
import mozilla.components.feature.sitepermissions.SitePermissions
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.mozilla.gecko.util.ThreadUtils.runOnUiThread

/**
 * Shows an informative dialog with the provided [SslCertificate] information.
 */
fun Context.showSslDialog() {
    val securityInfo = this.components.sessionManager.selectedSession?.securityInfo
    val host = this.components.sessionManager.selectedSession?.url?.tryGetHostFromUrl() ?: ""

    val icon = if(securityInfo?.secure == true) R.drawable.ic_baseline_lock else R.drawable.ic_baseline_lock_open
    val certAuthority = securityInfo?.issuer
    val certHost = securityInfo?.host

    val contentView = LayoutInflater.from(this).inflate(R.layout.dialog_ssl_info, null, false).apply {
        findViewById<TextView>(R.id.ssl_layout_issue_by).text = certAuthority
        findViewById<TextView>(R.id.ssl_layout_issue_to).text = certHost
    }

    MaterialAlertDialogBuilder(this)
        .setIcon(icon)
        .setTitle(host ?: this.resources.getString(R.string.blank_page))
        .setPositiveButton(R.string.mozac_feature_prompts_ok, null)
        .setView(contentView)
        .show()
}
