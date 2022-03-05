package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkFolderItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.repository.BookmarkManager
import com.cookiejarapps.android.smartcookieweb.utils.BookmarkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddBookmarkFolderDialog @JvmOverloads constructor(context: Context, private var mManager: BookmarkManager?, title: String?, private var mParent: BookmarkFolderItem?, private val item: BookmarkFolderItem? = null) {
    private val mDialog: androidx.appcompat.app.AlertDialog
    private val titleEditText: EditText
    private val addToTopCheckBox: CheckBox
    private var mOnClickListener: DialogInterface.OnClickListener? = null

    constructor(context: Context, manager: BookmarkManager, item: BookmarkFolderItem) : this(context, manager, item.title, item.parent, item)

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.add_bookmark_folder_dialog, null)
        titleEditText = view.findViewById(R.id.titleEditText)
        addToTopCheckBox = view.findViewById(R.id.addToTopCheckBox)

        if (item != null) {
            addToTopCheckBox.visibility = View.GONE
        }

        titleEditText.setText(title)

        mDialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (item == null) R.string.add_folder else R.string.edit_bookmark)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    fun show() {
        mDialog.show()

        mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { _ ->
            val title = titleEditText.text
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(mDialog.context, R.string.title_empty_mes, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mManager == null)
                mManager = BookmarkManager.getInstance(mDialog.context)

            if (mParent == null)
                mParent = mManager!!.root

            if (item == null) {
                val item = BookmarkFolderItem(title.toString(), mParent, BookmarkUtils.getNewId())
                if (addToTopCheckBox.isChecked)
                    mParent!!.addAtStart(item)
                else
                    mParent!!.add(item)
            } else {
                if (item.parent == null) {
                    item.parent = mParent
                    mParent!!.add(item)
                }
                item.title = title.toString()
            }

            if (mManager!!.save()) {
                Toast.makeText(mDialog.context, R.string.successful, Toast.LENGTH_SHORT).show()
                if (mOnClickListener != null)
                    mOnClickListener!!.onClick(mDialog, DialogInterface.BUTTON_POSITIVE)
                mDialog.dismiss()
            } else {
                Toast.makeText(mDialog.context, R.string.failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun setOnClickListener(l: DialogInterface.OnClickListener): AddBookmarkFolderDialog {
        mOnClickListener = l
        return this
    }
}