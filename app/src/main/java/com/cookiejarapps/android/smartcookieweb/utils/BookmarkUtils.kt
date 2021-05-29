package com.cookiejarapps.android.smartcookieweb.utils

import android.content.Context
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.storage.sync.PlacesBookmarksStorage
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarksStorage

class BookmarkUtils {
    private var lastId = time
    @Synchronized
    private fun createId(): Long {
        while (true) {
            val currentTime = time
            if (currentTime / 1000 == lastId / 1000) {
                if (lastId % 1000 < 999) {
                    return ++lastId
                }
                try {
                    Thread.sleep(1)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                continue
            } else if (currentTime < lastId) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                continue
            }
            return currentTime.also { lastId = it }
        }
    }

    private object InstanceHolder {
        val INSTANCE = BookmarkUtils()
    }

    suspend fun withOptionalDesktopFolders(context: Context, node: BookmarkNode, bookmarksStorage: Lazy<PlacesBookmarksStorage>): BookmarkNode {
        return when (node.guid) {
            BookmarkRoot.Mobile.id -> {
                // We're going to make a copy of the mobile node, and add-in a synthetic child folder to the top of the
                // children's list that contains all of the desktop roots.
                val childrenWithVirtualFolder =
                    listOfNotNull(bookmarksStorage.value.getTree(BookmarkRoot.Root.id, recursive = false)!!) + node.children.orEmpty()

                node.copy(children = childrenWithVirtualFolder)
            }
            BookmarkRoot.Root.id ->
                node.copy(
                    title = rootTitles(context, false)[node.title],
                    children = restructureDesktopRoots(context, node.children)
                )
            BookmarkRoot.Menu.id, BookmarkRoot.Toolbar.id, BookmarkRoot.Unfiled.id ->
                // If we're looking at one of the desktop roots, change their titles to friendly names.
                node.copy(title = rootTitles(context, false)[node.title])
            else ->
                // Otherwise, just return the node as-is.
                node
        }
    }

    private fun restructureDesktopRoots(context: Context, roots: List<BookmarkNode>?): List<BookmarkNode>? {
        roots ?: return null

        return roots.filter { rootTitles(context, false).containsKey(it.title) }
            .map { it.copy(title = rootTitles(context, false)[it.title]) }
    }

    fun rootTitles(context: Context, withMobileRoot: Boolean): Map<String, String> = if (withMobileRoot) {
        mapOf(
            "root" to "<h3>LIBRARY</h3>",
            "mobile" to "<b>LIBRARY</b>",
            "menu" to "<b>LIBRARY MENU</b>",
            "toolbar" to "<b>LIBRARY TOOLBAR</b>",
            "unfiled" to "<b>LIBRARY UNFILED</b>"
        )
    } else {
        mapOf(
            "root" to "<h3>LIBRARY</h3>",
            "menu" to "<b>LIBRARY MENU</b>",
            "toolbar" to "<b>LIBRARY TOOLBAR</b>",
            "unfiled" to "<b>LIBRARY UNFILED</b>"
        )
    }

    companion object {
        private val time: Long
            private get() {
                val time = System.currentTimeMillis() / 1000
                return time * 1000
            }
        val newId: Long
            get() = InstanceHolder.INSTANCE.createId()
    }
}