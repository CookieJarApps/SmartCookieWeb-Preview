package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.view.View

interface BookmarkRecyclerViewClickInterface {
    fun onRecyclerItemClicked(v: View, position: Int)

    fun onRecyclerItemLongClicked(v: View, position: Int): Boolean
}