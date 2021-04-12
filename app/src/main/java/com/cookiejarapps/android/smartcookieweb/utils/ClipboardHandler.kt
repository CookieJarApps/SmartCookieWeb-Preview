package com.cookiejarapps.android.smartcookieweb.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import mozilla.components.support.utils.SafeUrl
import mozilla.components.support.utils.WebURLFinder

private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
private const val MIME_TYPE_TEXT_HTML = "text/html"

class ClipboardHandler(val context: Context) {
    private val clipboard = context.getSystemService<ClipboardManager>()!!

    var text: String?
        get() {
            if (!clipboard.isPrimaryClipEmpty() &&
                (clipboard.isPrimaryClipPlainText() ||
                        clipboard.isPrimaryClipHtmlText())
            ) {
                return firstSafePrimaryClipItemText
            }
            return null
        }
        set(value) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Text", value))
        }

    val url: String?
        get() {
            return text?.let {
                val finder = WebURLFinder(it)
                finder.bestWebURL()
            }
        }

    private fun ClipboardManager.isPrimaryClipPlainText() =
        primaryClipDescription?.hasMimeType(MIME_TYPE_TEXT_PLAIN) ?: false

    private fun ClipboardManager.isPrimaryClipHtmlText() =
        primaryClipDescription?.hasMimeType(MIME_TYPE_TEXT_HTML) ?: false

    private fun ClipboardManager.isPrimaryClipEmpty() = primaryClip?.itemCount == 0

    private val ClipboardManager.firstPrimaryClipItem: ClipData.Item?
        get() = primaryClip?.getItemAt(0)

    @VisibleForTesting
    internal val firstSafePrimaryClipItemText: String?
        get() = SafeUrl.stripUnsafeUrlSchemes(context, clipboard.firstPrimaryClipItem?.text)
}
