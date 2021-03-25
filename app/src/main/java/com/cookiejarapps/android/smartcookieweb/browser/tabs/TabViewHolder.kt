package com.cookiejarapps.android.smartcookieweb.browser.tabs

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatImageButton
import com.cookiejarapps.android.smartcookieweb.R
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.browser.tabstray.thumbnail.TabThumbnailView
import mozilla.components.concept.tabstray.Tab
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable

class TabViewHolder(
        itemView: View
) : TabViewHolder(itemView) {
    @VisibleForTesting
    internal val iconView: ImageView? = itemView.findViewById(R.id.mozac_browser_tabstray_icon)
    @VisibleForTesting
    internal val titleView: TextView = itemView.findViewById(R.id.mozac_browser_tabstray_title)
    @VisibleForTesting
    internal val closeView: AppCompatImageButton = itemView.findViewById(R.id.mozac_browser_tabstray_close)

    override var tab: Tab? = null
    @VisibleForTesting
    internal var styling: TabsTrayStyling? = null

    override fun bind(
            tab: Tab,
            isSelected: Boolean,
            styling: TabsTrayStyling,
            observable: Observable<TabsTray.Observer>
    ) {
        this.tab = tab
        this.styling = styling

        val title = if (tab.title.isNotEmpty()) {
            tab.title
        } else {
            tab.url
        }

        titleView.text = title

        itemView.setOnClickListener {
            observable.notifyObservers { onTabSelected(tab) }
        }

        closeView.setOnClickListener {
            observable.notifyObservers { onTabClosed(tab) }
        }

        updateSelectedTabIndicator(isSelected)

        iconView?.setImageBitmap(tab.icon)
    }

    override fun updateSelectedTabIndicator(showAsSelected: Boolean) {
        if (showAsSelected) {
            showItemAsSelected()
        } else {
            showItemAsNotSelected()
        }
    }

    @VisibleForTesting
    internal fun showItemAsSelected() {
        styling?.let { styling ->
            titleView.setTextColor(styling.selectedItemTextColor)
            itemView.setBackgroundColor(Color.LTGRAY)
            closeView.imageTintList = ColorStateList.valueOf(styling.selectedItemTextColor)
        }
    }

    @VisibleForTesting
    internal fun showItemAsNotSelected() {
        styling?.let { styling ->
            titleView.setTextColor(styling.itemTextColor)
            itemView.setBackgroundColor(styling.itemBackgroundColor)
            closeView.imageTintList = ColorStateList.valueOf(styling.itemTextColor)
        }
    }
}