package com.cookiejarapps.android.smartcookieweb.browser.tabs

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.concept.base.images.ImageLoader
import mozilla.components.concept.tabstray.Tabs
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry

// Function responsible for creating a `TabViewHolder` in the `TabsAdapter`.

typealias ViewHolderProvider = (ViewGroup) -> TabViewHolder

// Shows tab list in drawer
open class TabListAdapter(
    private val context: Context,
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
    private val delegate: mozilla.components.browser.tabstray.TabsTray.Delegate
) : RecyclerView.Adapter<TabViewHolder>() {
    private var tabs: List<TabSessionState>? = null

    var styling: TabsTrayStyling = TabsTrayStyling()

    val selectedTabId = context.components.store.state.selectedTabId
    val selectedIndex = tabs?.indexOfFirst { it.id == selectedTabId }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        return viewHolderProvider.invoke(parent)
    }

    override fun getItemCount() = tabs?.size ?: 0

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tabs = tabs ?: return

        holder.bind(tabs[position], selectedIndex == position, styling, delegate)
    }

    override fun onBindViewHolder(
        holder: TabViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        tabs?.let { tabs ->
            if (tabs.isEmpty()) return

            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position)
                return
            }

            if (payloads.contains(HIGHLIGHT_SELECTED_ITEM) && position == selectedIndex) {
                holder.updateSelectedTabIndicator(true)
            } else if (payloads.contains(DONT_HIGHLIGHT_SELECTED_ITEM) && position == selectedIndex) {
                holder.updateSelectedTabIndicator(false)
            }
        }
    }

    companion object {
        val HIGHLIGHT_SELECTED_ITEM: Int = R.id.payload_highlight_selected_item
        val DONT_HIGHLIGHT_SELECTED_ITEM: Int = R.id.payload_dont_highlight_selected_item
    }
}