package com.cookiejarapps.android.smartcookieweb.utils

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.browser_toolbar_popup_window.view.*
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import java.lang.ref.WeakReference

object ToolbarPopupWindow {
    fun show(
        view: WeakReference<View>,
        customTabId: String? = null,
        handlePasteAndGo: (String) -> Unit,
        handlePaste: (String) -> Unit,
        copyVisible: Boolean = true
    ) {
        val context = view.get()?.context ?: return
        val clipboard = context.components.clipboardHandler
        if (!copyVisible && clipboard.text.isNullOrEmpty()) return

        val isCustomTabSession = customTabId != null

        val customView = LayoutInflater.from(context)
            .inflate(R.layout.browser_toolbar_popup_window, null)
        val popupWindow = PopupWindow(
            customView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            context.resources.getDimensionPixelSize(R.dimen.context_menu_height),
            true
        )
        popupWindow.elevation =
            context.resources.getDimension(R.dimen.mozac_browser_menu_elevation)

        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        customView.copy.isVisible = copyVisible

        customView.paste.isVisible = !clipboard.text.isNullOrEmpty() && !isCustomTabSession
        customView.paste_and_go.isVisible =
            !clipboard.text.isNullOrEmpty() && !isCustomTabSession

        customView.copy.setOnClickListener {
            popupWindow.dismiss()
            clipboard.text = getUrlForClipboard(
                it.context.components.store,
                customTabId
            )

            view.get()?.let {
                Snackbar.make(
                    it,
                    context.resources.getString(R.string.copied),
                    Snackbar.LENGTH_SHORT
                )
            }
        }

        customView.paste.setOnClickListener {
            popupWindow.dismiss()
            handlePaste(clipboard.text!!)
        }

        customView.paste_and_go.setOnClickListener {
            popupWindow.dismiss()
            handlePasteAndGo(clipboard.text!!)
        }

        view.get()?.let {
            popupWindow.showAsDropDown(
                it,
                context.resources.getDimensionPixelSize(R.dimen.context_menu_x_offset),
                0,
                Gravity.START
            )
        }
    }

    @VisibleForTesting
    internal fun getUrlForClipboard(
        store: BrowserStore,
        customTabId: String? = null
    ): String? {
        return if (customTabId != null) {
            val customTab = store.state.findCustomTab(customTabId)
            customTab?.content?.url
        } else {
            val selectedTab = store.state.selectedTab
            selectedTab?.readerState?.activeUrl ?: selectedTab?.content?.url
        }
    }
}
