package com.cookiejarapps.android.smartcookieweb.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.textclassifier.TextClassifier
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import mozilla.components.support.utils.SafeUrl
import mozilla.components.support.utils.WebURLFinder
import mozilla.components.support.ktx.kotlin.MAX_URI_LENGTH

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

    internal fun containsURL(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val description = clipboard.primaryClipDescription
            // An IllegalStateException is thrown if the url is too long.
            val score =
                try {
                    description?.getConfidenceScore(TextClassifier.TYPE_URL) ?: 0F
                } catch (e: IllegalStateException) {
                    0F
                }
            score >= 0.7F
        } else {
            !extractURL().isNullOrEmpty()
        }
    }

    fun extractURL(): String? {
        return text?.let {
            if (it.length > MAX_URI_LENGTH) {
                return null
            }

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
