package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.repository.BookmarkManager


class AddBookmarkOptionDialog : DialogFragment() {

    companion object {
        private const val ARG_URL = "url"
        private const val ARG_TITLE = "title"

        @JvmStatic
        fun newInstance(title: String, url: String): AddBookmarkOptionDialog {
            return AddBookmarkOptionDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_URL, url)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = arguments ?: throw IllegalArgumentException()
        return AlertDialog.Builder(requireActivity()).apply {
            setTitle(R.string.bookmark)
            setItems(R.array.add_bookmark_option) { _, i ->
                when (i) {
                    0 -> AddBookmarkSiteDialog(requireActivity(), arguments.getString(ARG_TITLE)!!, arguments.getString(ARG_URL)!!).show()
                    1 -> {
                        BookmarkManager.getInstance(requireActivity()).run {
                            removeAll(arguments.getString(ARG_URL)!!)
                            save()
                        }
                    }
                }
            }
            setNegativeButton(android.R.string.cancel, null)
        }.create()
    }
}