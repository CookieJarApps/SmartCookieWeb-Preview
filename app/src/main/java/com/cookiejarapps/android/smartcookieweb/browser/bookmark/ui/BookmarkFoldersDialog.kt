package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkFolderItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.repository.BookmarkManager

class BookmarkFoldersDialog(private val context: Context, private val manager: BookmarkManager) {
    private lateinit var mDialog: AlertDialog
    private val mListView: ListView = ListView(context)
    private lateinit var mCurrentFolder: BookmarkFolderItem
    private val mFolderList = ArrayList<BookmarkFolderItem?>()
    private var mExcludeList: Collection<BookmarkItem>? = null
    private var mOnFolderSelectedListener: OnFolderSelectedListener? = null

    private val titleText: TextView

    interface OnFolderSelectedListener {
        fun onFolderSelected(dialog: DialogInterface, folder: BookmarkFolderItem): Boolean
    }

    init {
        val top = View.inflate(context, R.layout.dialog_title, null)
        val button: ImageButton = top.findViewById(R.id.addButton)

        titleText = top.findViewById(R.id.titleText)

        button.setOnClickListener {
            AddBookmarkFolderDialog(context, manager, context.getString(R.string.new_folder_name), mCurrentFolder)
                .setOnClickListener { _, _ -> setFolder(mCurrentFolder) }
                    .show()
        }
        button.setOnLongClickListener {
            Toast.makeText(context, R.string.new_folder_name, Toast.LENGTH_SHORT).show()
            true
        }

        mDialog = AlertDialog.Builder(context)
            .setView(mListView)
            .setCustomTitle(top)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (mOnFolderSelectedListener != null){
                    mOnFolderSelectedListener!!.onFolderSelected(mDialog, mCurrentFolder)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()


        mListView.adapter = object : ArrayAdapter<BookmarkFolderItem>(context.applicationContext, 0, mFolderList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView
                    ?: View.inflate(context, android.R.layout.simple_list_item_1, null)
                val item = getItem(position)
                view.findViewById<TextView>(android.R.id.text1).text = if (item != null){
                    item.title
                } else{
                    ".."
                }
                return view
            }
        }

        mListView.setOnItemClickListener { _, _, position, _ ->
            val folder = mFolderList[position] ?: mCurrentFolder.parent!!
            setFolder(folder)
        }

        mListView.setOnItemLongClickListener { _, _, position, _ ->
            val folder = mFolderList[position] ?: mCurrentFolder.parent!!
            mOnFolderSelectedListener?.onFolderSelected(mDialog, folder) ?: false
        }
    }

    fun setTitle(title: Int): BookmarkFoldersDialog {
        titleText.setText(title)
        return this
    }

    fun setCurrentFolder(folder: BookmarkFolderItem): BookmarkFoldersDialog {
        mExcludeList = null
        setFolder(folder)
        return this
    }

    fun setCurrentFolder(folder: BookmarkFolderItem, excludeItem: BookmarkItem?): BookmarkFoldersDialog {
        mExcludeList = hashSetOf<BookmarkItem>().apply { if (excludeItem != null){ add(excludeItem) } }
        setFolder(folder)
        return this
    }

    fun setOnFolderSelectedListener(l: OnFolderSelectedListener): BookmarkFoldersDialog {
        mOnFolderSelectedListener = l
        return this
    }

    inline fun setOnFolderSelectedListener(crossinline l: (BookmarkFolderItem) -> Boolean): BookmarkFoldersDialog {
        return setOnFolderSelectedListener(object : OnFolderSelectedListener {
            override fun onFolderSelected(dialog: DialogInterface, folder: BookmarkFolderItem): Boolean {
                return l(folder)
            }
        })
    }

    private fun setFolder(folder: BookmarkFolderItem) {
        mFolderList.clear()
        mCurrentFolder = folder
        if (folder.parent != null){
            mFolderList.add(null)
        }
        for (i in folder.itemList){
            if (i is BookmarkFolderItem && (mExcludeList == null || !mExcludeList!!.contains(i))){
                mFolderList.add(i)
            }
        }

        (mListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    fun show() {
        mDialog.show()
    }
}