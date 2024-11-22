package com.khalyl.android.kyubi.browser.bookmark.ui

import android.view.View

interface BookmarkRecyclerViewClickInterface {
    fun onRecyclerItemClicked(v: View, position: Int)

    fun onRecyclerItemLongClicked(v: View, position: Int): Boolean
}