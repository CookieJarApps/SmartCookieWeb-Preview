package com.cookiejarapps.android.smartcookieweb.browser.bookmark

import android.content.Context
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkSiteItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.repository.BookmarkManager
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.concept.storage.BookmarksStorage
import mozilla.components.concept.storage.bookmarks.InsertableBookmarkTreeRoot
import java.util.*

class CustomBookmarksStorage(context: Context): BookmarksStorage {

    private val manager = BookmarkManager.getInstance(context)

    override suspend fun addFolder(parentGuid: String, title: String, position: UInt?): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun insertTree(tree: InsertableBookmarkTreeRoot): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun addItem(
        parentGuid: String,
        url: String,
        title: String,
        position: UInt?
    ): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun addSeparator(parentGuid: String, position: UInt?): Result<String> {
        TODO("Not yet implemented")
    }

    override fun cleanup() {
        TODO("Not yet implemented")
    }

    override suspend fun countBookmarksInTrees(guids: List<String>): UInt {
        TODO("Not yet implemented")
    }

    override suspend fun deleteNode(guid: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun getBookmark(guid: String): Result<BookmarkNode?> {
        TODO("Not yet implemented")
    }

    override suspend fun getBookmarksWithUrl(url: String): Result<List<BookmarkNode>> {
        TODO("Not yet implemented")
    }

    override suspend fun getRecentBookmarks(
        limit: Int,
        maxAge: Long?,
        currentTime: Long
    ): Result<List<BookmarkNode>> {
        TODO("Not yet implemented")
    }

    override suspend fun getTree(guid: String, recursive: Boolean): Result<BookmarkNode?> {
        TODO("Not yet implemented")
    }

    override suspend fun runMaintenance(dbSizeLimit: UInt) {
        TODO("Not yet implemented")
    }

    override suspend fun searchBookmarks(query: String, limit: Int): Result<List<BookmarkNode>> {
        val bookmarks: MutableList<BookmarkNode> = emptyList<BookmarkNode>().toMutableList()
        for(i in manager.root.itemList){
            if(i is BookmarkSiteItem){
                bookmarks.add(BookmarkNode(BookmarkNodeType.ITEM, UUID.randomUUID().toString(), "",
                    0u, i.title, i.url, 0, 0, null))
            }
        }

        return Result.success(
            bookmarks.filter { s -> s.title?.contains(query) == true || s.url?.contains(query) == true }.take(limit)
        )
    }

    override suspend fun updateNode(guid: String, info: BookmarkInfo): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun warmUp() {
        TODO("Not yet implemented")
    }
}