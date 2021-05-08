package com.cookiejarapps.android.smartcookieweb.browser.tabs

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.annotation.Dimension.DP
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.cookiejarapps.android.smartcookieweb.R
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.browser.tabstray.thumbnail.TabThumbnailView
import mozilla.components.concept.base.images.ImageLoadRequest
import mozilla.components.concept.base.images.ImageLoader
import mozilla.components.concept.tabstray.Tab
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.util.dpToPx

class TabGridViewHolder(
        itemView: View,
        private val thumbnailLoader: ImageLoader? = null
) : TabViewHolder(itemView) {
    @VisibleForTesting
    internal val iconView: ImageView? = itemView.findViewById(R.id.mozac_browser_tabstray_icon)
    @VisibleForTesting
    internal val titleView: TextView = itemView.findViewById(R.id.mozac_browser_tabstray_title)
    @VisibleForTesting
    internal val tabBackground: ConstraintLayout = itemView.findViewById(R.id.tab_grid_item)
    @VisibleForTesting
    internal val closeView: AppCompatImageButton = itemView.findViewById(R.id.mozac_browser_tabstray_close)

    private val thumbnailView: TabThumbnailView = itemView.findViewById(R.id.mozac_browser_tabstray_thumbnail)

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

        if (thumbnailLoader != null && tab.thumbnail == null) {
            val thumbnailSize = THUMBNAIL_SIZE.dpToPx(thumbnailView.context.resources.displayMetrics)
            thumbnailLoader.loadIntoView(
                thumbnailView,
                ImageLoadRequest(id = tab.id, size = thumbnailSize)
            )
        } else if (tab.thumbnail != null) {
            thumbnailView.setImageBitmap(tab.thumbnail)
        }
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
        titleView.setTextColor(itemView.context.getColorFromAttr(android.R.attr.textColorPrimary))
        tabBackground.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.selected_tab))
        closeView.imageTintList = ColorStateList.valueOf(itemView.context.getColorFromAttr(android.R.attr.textColorPrimary))
    }

    @VisibleForTesting
    internal fun showItemAsNotSelected() {
        titleView.setTextColor(itemView.context.getColorFromAttr(android.R.attr.textColorPrimary))
        tabBackground.setBackgroundColor(itemView.context.getColorFromAttr(R.attr.colorSurface))
        closeView.imageTintList = ColorStateList.valueOf(itemView.context.getColorFromAttr(android.R.attr.textColorPrimary))
    }

    companion object {
        @Dimension(unit = DP) private const val THUMBNAIL_SIZE = 100
    }
}