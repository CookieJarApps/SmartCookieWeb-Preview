package com.khalyl.android.kyubi.browser.bookmark.ui

import android.content.Context
import android.view.View
import com.khalyl.android.kyubi.browser.bookmark.items.BookmarkSiteItem
import com.khalyl.android.kyubi.browser.bookmark.repository.BookmarkManager
import com.khalyl.android.kyubi.utils.BookmarkUtils

class AddBookmarkSiteDialog : AddBookmarkDialog<BookmarkSiteItem, String> {
    constructor(context: Context, manager: BookmarkManager, item: BookmarkSiteItem) : super(context, manager, item, item.title, item.url)

    constructor(context: Context, title: String, url: String) : super(context, null, null, title, url)

    override fun initView(view: View, title: String?, url: String) {
        super.initView(view, title, url)
        titleEditText.setText(title ?: url)
        urlEditText.setText(url)
    }

    override fun makeItem(item: BookmarkSiteItem?, title: String, url: String): BookmarkSiteItem? {
        return if (item == null) {
            BookmarkSiteItem(title, url.trim { it <= ' ' }, BookmarkUtils.getNewId())
        } else {
            item.title = title
            item.url = url.trim { it <= ' ' }
            null
        }
    }
}