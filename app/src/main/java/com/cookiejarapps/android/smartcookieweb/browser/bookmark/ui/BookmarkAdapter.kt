package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.BookmarkSortType
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkFolderItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkSiteItem
import com.cookiejarapps.android.smartcookieweb.ext.components
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.icons.IconRequest
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import java.net.IDN

open class BookmarkAdapter(
    protected val context: Context,
    list: MutableList<BookmarkItem>,
    private val bookmarkItemListener: OnBookmarkRecyclerListener
) : ArrayRecyclerAdapter<BookmarkItem, BookmarkAdapter.BookmarkItemHolder>(context, list, null) {

    init {
        setRecyclerListener(object : BookmarkRecyclerViewClickInterface {
            override fun onRecyclerItemClicked(v: View, position: Int) {
                bookmarkItemListener.onRecyclerItemClicked(v, position)
            }

            override fun onRecyclerItemLongClicked(v: View, position: Int): Boolean {
                return bookmarkItemListener.onRecyclerItemLongClicked(v, position)
            }
        })
    }

    override fun onBindViewHolder(holder: BookmarkItemHolder, item: BookmarkItem, position: Int) {
        super.onBindViewHolder(holder, item, position)

        if (item is BookmarkSiteItem && holder is BookmarkSiteHolder) {
            holder.url.text = item.url

            CoroutineScope(Dispatchers.Main).launch{
                val bitmap: Bitmap
                withContext(Dispatchers.IO) {
                    bitmap = context.components.icons.loadIcon(IconRequest(item.url)).await().bitmap
                }

                withContext(Dispatchers.Main){
                    holder.icon.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup?, viewType: Int): BookmarkItemHolder {
        return when (viewType) {
            TYPE_SITE -> BookmarkSiteHolder(inflater.inflate(R.layout.bookmark_item_site, parent, false), this)
            TYPE_FOLDER -> BookmarkItemHolder(inflater.inflate(R.layout.bookmark_item_folder, parent, false), this)
            else -> throw IllegalStateException()
        }
    }

    protected fun onOverflowButtonClick(v: View, position: Int, item: BookmarkItem) {
        val calPosition = searchPosition(position, item)
        if (calPosition < 0){
            return
        }
        bookmarkItemListener.onShowMenu(v, calPosition)
    }

    override fun getItemViewType(position: Int): Int {
        return when (get(position)) {
            is BookmarkSiteItem -> TYPE_SITE
            is BookmarkFolderItem -> TYPE_FOLDER
            else -> throw IllegalStateException()
        }
    }

    class BookmarkSiteHolder(itemView: View, adapter: BookmarkAdapter) : BookmarkItemHolder(itemView, adapter) {
        // TODO: setting text size here, when customization settings are added
        val url: TextView = itemView.findViewById(R.id.urlTextView)
    }

    open class BookmarkItemHolder(itemView: View, adapter: BookmarkAdapter) : ArrayViewHolder<BookmarkItem>(itemView, adapter) {
        val title: TextView = itemView.findViewById(R.id.titleTextView)
        val icon: ImageButton = itemView.findViewById(R.id.imageButton)
        val more: ImageButton = itemView.findViewById(R.id.dropdownBookmark)

        init {
            more.setOnClickListener {
                adapter.onOverflowButtonClick(more, adapterPosition, item)
            }
        }

        override fun setUp(item: BookmarkItem) {
            super.setUp(item)
            title.text = item.title
        }
    }

    fun sortBookmarks(sortType: BookmarkSortType) {
        when (sortType) {
            BookmarkSortType.A_Z -> items.sortBy { it.title?.toLowerCase() }
            BookmarkSortType.Z_A -> items.sortByDescending { it.title?.toLowerCase() }
            BookmarkSortType.MANUAL -> {} // Do nothing, keep the manual order
        }
        notifyDataSetChanged()
    }

    interface OnBookmarkRecyclerListener : BookmarkRecyclerViewClickInterface {
        fun onIconClick(v: View, position: Int)

        fun onShowMenu(v: View, position: Int)

        fun onSelectionStateChange(items: Int)
    }

    companion object {
        const val TYPE_SITE = 1
        const val TYPE_FOLDER = 2
    }
}