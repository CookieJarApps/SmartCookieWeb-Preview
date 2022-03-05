package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkFolderItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.repository.BookmarkManager
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import java.net.IDN

abstract class AddBookmarkDialog<S : BookmarkItem, T>(
    protected val context: Context,
    manager: BookmarkManager?,
    protected val mItem: S?,
    title: String?,
    url: T
) : BookmarkFoldersDialog.OnFolderSelectedListener {
    protected val mDialog: AlertDialog
    protected val titleEditText: EditText
    protected val urlEditText: EditText
    protected val folderTextView: TextView
    protected val folderButton: AppCompatTextView
    protected val addToTopCheckBox: CheckBox
    protected var mOnClickListener: DialogInterface.OnClickListener? = null
    protected val mManager: BookmarkManager = manager ?: BookmarkManager.getInstance(context)
    protected lateinit var mParent: BookmarkFolderItem

    init {
        val view = inflateView()
        titleEditText = view.findViewById(R.id.titleEditText)
        urlEditText = view.findViewById(R.id.urlEditText)
        folderTextView = view.findViewById(R.id.folderTextView)
        folderButton = view.findViewById(R.id.folderButton)
        addToTopCheckBox = view.findViewById(R.id.addToTopCheckBox)

        initView(view, title, url)

        mDialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (mItem == null) R.string.add_bookmark else R.string.edit_bookmark)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    protected fun inflateView(): View {
        return LayoutInflater.from(context).inflate(R.layout.add_bookmark_dialog, null)
    }

    protected open fun initView(view: View, title: String?, url: T) {
        if (mItem == null) {
            val root = getRootPosition()
            mParent = root
            folderButton.text = root.title
            folderButton.setOnClickListener { v ->
                BookmarkFoldersDialog(context, mManager)
                    .setTitle(R.string.folder)
                    .setCurrentFolder(root)
                    .setOnFolderSelectedListener(this@AddBookmarkDialog)
                    .show()
            }
        } else {
            folderTextView.visibility = View.GONE
            folderButton.visibility = View.GONE
            addToTopCheckBox.visibility = View.GONE
        }
    }

    private fun getRootPosition(): BookmarkFolderItem {
        if (UserPreferences(context).bookmarkFolder) {
            val id = UserPreferences(context).bookmarkFolderId
            val item = mManager[id]
            if (item is BookmarkFolderItem) {
                return item
            }
        }
        return mManager.root
    }

    fun show() {
        mDialog.show()

        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { v ->
            val title = titleEditText.text
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(mDialog.context, R.string.title_empty_mes, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val url = urlEditText.text
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(mDialog.context, R.string.url_empty_mes, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val item = makeItem(mItem, title.toString(), url.toString())
            if (item != null) {
                if (addToTopCheckBox.isChecked)
                    mManager.addFirst(mParent, item)
                else
                    mManager.add(mParent, item)
            }
            if (mItem != null && addToTopCheckBox.isChecked) {
                mManager.moveToFirst(mParent, mItem)
            }

            if (mManager.save()) {
                Toast.makeText(mDialog.context, R.string.successful, Toast.LENGTH_SHORT).show()
                mOnClickListener?.onClick(mDialog, DialogInterface.BUTTON_POSITIVE)
                mDialog.dismiss()
            } else {
                Toast.makeText(mDialog.context, R.string.failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    protected abstract fun makeItem(item: S?, title: String, url: String): S?

    fun setOnClickListener(l: DialogInterface.OnClickListener): AddBookmarkDialog<S, T> {
        mOnClickListener = l
        return this
    }

    inline fun setOnClickListener(crossinline listener: (DialogInterface, Int) -> Unit): AddBookmarkDialog<S, T> {
        setOnClickListener(DialogInterface.OnClickListener { dialog, which -> listener(dialog, which) })
        return this
    }

    override fun onFolderSelected(dialog: DialogInterface, folder: BookmarkFolderItem): Boolean {
        folderButton.text = folder.title
        mParent = folder
        return false
    }
}