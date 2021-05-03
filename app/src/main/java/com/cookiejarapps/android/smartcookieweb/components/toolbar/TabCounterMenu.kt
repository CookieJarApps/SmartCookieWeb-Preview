package com.cookiejarapps.android.smartcookieweb.components.toolbar

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat.getColor
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import mozilla.components.concept.menu.candidate.*
import mozilla.components.ui.tabcounter.TabCounterMenu

class TabCounterMenu(
    context: Context,
    onItemTapped: (Item) -> Unit,
    iconColor: Int? = null
) : TabCounterMenu(context, onItemTapped, iconColor) {

    open class ExtendedItem: TabCounterMenu.Item() {
        object DuplicateTab : Item()
    }

    var duplicateTabItem: TextMenuCandidate = TextMenuCandidate(
        text = context.getString(R.string.duplicate_tab),
        start = DrawableMenuIcon(
            context,
            R.drawable.mozac_ic_tab,
            tint = iconColor ?: getColor(context, R.color.mozac_ui_tabcounter_default_text)
        ),
        textStyle = TextStyle()
    ) {
        onItemTapped(ExtendedItem.DuplicateTab)
    }

    @VisibleForTesting
    internal fun menuItems(toolbarPosition: ToolbarPosition): List<MenuCandidate> {

        val items = listOf(
            newTabItem,
            newPrivateTabItem,
            DividerMenuCandidate(),
            duplicateTabItem,
            DividerMenuCandidate(),
            closeTabItem
        )

        return when (toolbarPosition) {
            ToolbarPosition.BOTTOM -> items.reversed()
            ToolbarPosition.TOP -> items
        }
    }

    fun updateMenu(toolbarPosition: ToolbarPosition) {
        menuController.submitList(menuItems(toolbarPosition))
    }
}
