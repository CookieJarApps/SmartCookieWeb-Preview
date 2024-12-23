/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.cookiejarapps.android.smartcookieweb.utils

import android.content.Context
import android.util.AttributeSet
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK
import androidx.appcompat.widget.AppCompatTextView
import com.cookiejarapps.android.smartcookieweb.R

/**
 * An [AppCompatTextView] that announces as link in screen readers for a11y purposes
 */
class LinkTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        val extras = info?.extras
        extras?.putCharSequence(
            "AccessibilityNodeInfo.roleDescription",
            "link",
        )
        // disable long click  announcement, as there is no action to be performed on long click
        info?.isLongClickable = false
        info?.removeAction(ACTION_LONG_CLICK)
    }
}
