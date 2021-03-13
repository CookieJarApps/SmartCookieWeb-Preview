package com.cookiejarapps.android.smartcookieweb.browser.tabs

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import mozilla.components.browser.tabstray.DefaultTabViewHolder
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.concept.tabstray.Tabs
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import mozilla.components.concept.base.images.ImageLoader

// Function responsible for creating a `TabViewHolder` in the `TabsAdapter`.

typealias ViewHolderProvider = (ViewGroup) -> TabViewHolder

// Shows tab list in drawer
open class TabListAdapter(
    private val viewHolderProvider: ViewHolderProvider = { parent ->
        com.cookiejarapps.android.smartcookieweb.browser.tabs.TabViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.tab_list_item, parent, false)
        )
    },
    delegate: Observable<TabsTray.Observer> = ObserverRegistry()
) : RecyclerView.Adapter<TabViewHolder>(), TabsTray, Observable<TabsTray.Observer> by delegate {
    private var tabs: Tabs? = null

    var styling: TabsTrayStyling = TabsTrayStyling()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        return viewHolderProvider.invoke(parent)
    }

    override fun getItemCount() = tabs?.list?.size ?: 0

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tabs = tabs ?: return

        holder.bind(tabs.list[position], isTabSelected(tabs, position), styling, this)
    }

    override fun onBindViewHolder(
        holder: TabViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        tabs?.let { tabs ->
            if (tabs.list.isEmpty()) return

            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position)
                return
            }

            if (payloads.contains(HIGHLIGHT_SELECTED_ITEM) && position == tabs.selectedIndex) {
                holder.updateSelectedTabIndicator(true)
            } else if (payloads.contains(DONT_HIGHLIGHT_SELECTED_ITEM) && position == tabs.selectedIndex) {
                holder.updateSelectedTabIndicator(false)
            }
        }
    }

    override fun updateTabs(tabs: Tabs) {
        this.tabs = tabs

        notifyObservers { onTabsUpdated() }
    }

    override fun onTabsInserted(position: Int, count: Int) =
        notifyItemRangeInserted(position, count)

    override fun onTabsRemoved(position: Int, count: Int) = notifyItemRangeRemoved(position, count)

    override fun onTabsMoved(fromPosition: Int, toPosition: Int) =
        notifyItemMoved(fromPosition, toPosition)

    override fun onTabsChanged(position: Int, count: Int) = notifyItemRangeChanged(position, count)

    override fun isTabSelected(tabs: Tabs, position: Int) = tabs.selectedIndex == position

    companion object {
        val HIGHLIGHT_SELECTED_ITEM: Int = R.id.payload_highlight_selected_item
        val DONT_HIGHLIGHT_SELECTED_ITEM: Int = R.id.payload_dont_highlight_selected_item
    }
}