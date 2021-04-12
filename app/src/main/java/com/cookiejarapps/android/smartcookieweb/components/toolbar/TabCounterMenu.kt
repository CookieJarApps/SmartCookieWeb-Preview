package org.mozilla.fenix.components.toolbar

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.cookiejarapps.android.smartcookieweb.components.toolbar.ToolbarPosition
import mozilla.components.concept.menu.candidate.MenuCandidate
import mozilla.components.ui.tabcounter.TabCounterMenu

class TabCounterMenu(
    context: Context,
    onItemTapped: (Item) -> Unit,
    iconColor: Int? = null
) : TabCounterMenu(context, onItemTapped, iconColor) {

    @VisibleForTesting
    internal fun menuItems(toolbarPosition: ToolbarPosition): List<MenuCandidate> {
        val items = listOf(
            newTabItem,
            newPrivateTabItem
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
