package com.cookiejarapps.android.smartcookieweb.browser.tabs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import mozilla.components.browser.state.state.TabPartition
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.DefaultTabViewHolder
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.concept.base.images.ImageLoader

// Function responsible for creating a `TabViewHolder` in the `TabsAdapter`.

typealias ViewHolderProvider = (ViewGroup) -> TabViewHolder

// Shows tab list in drawer
open class TabListAdapter(
    thumbnailLoader: ImageLoader? = null,
    private val viewHolderProvider: ViewHolderProvider = { parent ->
        if(UserPreferences(parent.context).showTabsInGrid)
            TabGridViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.tab_grid_item, parent, false),
                thumbnailLoader
            )
        else
            TabListViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.tab_list_item, parent, false)
            )
    },
    private val styling: TabsTrayStyling = TabsTrayStyling(),
    private val delegate: TabsTray.Delegate,
) : ListAdapter<TabSessionState, TabViewHolder>(DiffCallback), TabsTray {

    private var selectedTabId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        return viewHolderProvider.invoke(parent)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = getItem(position)

        holder.bind(tab, tab.id == selectedTabId, styling, delegate)
    }

    override fun onBindViewHolder(
        holder: TabViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        val tabs = currentList
        if (tabs.isEmpty()) return

        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        val tab = getItem(position)
        if (tab.id == selectedTabId) {
            if (payloads.contains(PAYLOAD_HIGHLIGHT_SELECTED_ITEM)) {
                holder.updateSelectedTabIndicator(true)
            } else if (payloads.contains(PAYLOAD_DONT_HIGHLIGHT_SELECTED_ITEM)) {
                holder.updateSelectedTabIndicator(false)
            }
        }
    }

    override fun updateTabs(
        tabs: List<TabSessionState>,
        tabPartition: TabPartition?,
        selectedTabId: String?
    ) {
        this.selectedTabId = selectedTabId

        submitList(tabs) {
            notifyDataSetChanged()
        }
    }

    companion object {
        /**
         * Payload used in onBindViewHolder for a partial update of the current view.
         *
         * Signals that the currently selected tab should be highlighted. This is the default behavior.
         */
        val PAYLOAD_HIGHLIGHT_SELECTED_ITEM: Int = R.id.payload_highlight_selected_item

        /**
         * Payload used in onBindViewHolder for a partial update of the current view.
         *
         * Signals that the currently selected tab should NOT be highlighted. No tabs would appear as highlighted.
         */
        val PAYLOAD_DONT_HIGHLIGHT_SELECTED_ITEM: Int = R.id.payload_dont_highlight_selected_item
    }

    private object DiffCallback : DiffUtil.ItemCallback<TabSessionState>() {
        override fun areItemsTheSame(oldItem: TabSessionState, newItem: TabSessionState): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TabSessionState, newItem: TabSessionState): Boolean {
            return oldItem == newItem
        }
    }
}