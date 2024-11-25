package com.khalyl.android.kyubi.components.toolbar

import com.khalyl.android.kyubi.components.Components

 class BrowserInteractor(
    private val browserToolbarController: BrowserToolbarController,
    private val menuController: BrowserToolbarMenuController,
    private val components: Components,
) : BrowserToolbarViewInteractor {

    override fun onTabCounterClicked() {
        browserToolbarController.handleTabCounterClick()
    }

    override fun onBrowserToolbarPaste(text: String) {
        browserToolbarController.handleToolbarPaste(text)
    }

    override fun onBrowserToolbarPasteAndGo(text: String) {
        browserToolbarController.handleToolbarPasteAndGo(text)
    }

    override fun onBrowserToolbarClicked() {
        browserToolbarController.handleToolbarClick()
    }

    override fun onBrowserToolbarMenuItemTapped(item: ToolbarMenu.Item) {
        menuController.handleToolbarItemInteraction(item)
    }

    override fun onScrolled(offset: Int) {
        browserToolbarController.handleScroll(offset)
    }
    override fun onNewTabClicked() {
        components.tabsUseCases.addTab(
            url = "about:homepage",
            selectTab = true,
        )
    }
}
