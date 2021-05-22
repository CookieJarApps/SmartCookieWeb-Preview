package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.R.attr.data
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkFolderItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkSiteItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.repository.BookmarkManager
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.utils.BookmarkUtils
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.*
import java.util.*


class ImportExportSettingsFragment : BaseSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences_import_export)

        clickablePreference(
                preference = requireContext().resources.getString(R.string.key_import_bookmarks),
                onClick = { requestBookmarkImport() }
        )

        clickablePreference(
                preference = requireContext().resources.getString(R.string.key_export_bookmarks),
                onClick = { requestBookmarkExport() }
        )
    }

    private fun requestBookmarkImport(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
        startActivityForResult(intent, IMPORT_BOOKMARKS)
    }

    private fun requestBookmarkExport(){
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra(Intent.EXTRA_TITLE, "BookmarkExport.txt")
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
        startActivityForResult(intent, EXPORT_BOOKMARKS)
    }

    private fun importBookmarks(uri: Uri){
        val manager = BookmarkManager.getInstance(requireActivity())
        val bookmarkFile = manager.file

        val input: InputStream? = requireActivity().contentResolver.openInputStream(uri)

        val bufferSize = 1024
        val buffer = CharArray(bufferSize)
        val out = StringBuilder()
        val `in`: Reader = InputStreamReader(input, "UTF-8")
        while (true) {
            val rsz = `in`.read(buffer, 0, buffer.size)
            if (rsz < 0) break
            out.append(buffer, 0, rsz)
        }

        val content = out.toString()

        val itemArray = JSONTokener(content).nextValue()

        // If the imported file is JSON and is an array, assume it's an export from this browser, or if it's an object, assume it's a legacy export
        if (itemArray is JSONArray) {
            val bookmarks = FileOutputStream(bookmarkFile, false)
            val contents: ByteArray = content.toByteArray()
            bookmarks.write(contents)
            bookmarks.flush()
            bookmarks.close()

            manager.initialize()
        }
        else if(itemArray is JSONObject) {
            bookmarkFile.delete()
            manager.initialize()

            val scanner = Scanner(content)
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val `object` = JSONObject(line)
                val folderName: String = `object`.getString(KEY_FOLDER)

                var folder = manager.root
                if(folderName != "") {
                    val importedFolder = BookmarkFolderItem(folderName, manager.root, BookmarkUtils.getNewId())
                    manager.root.add(importedFolder)
                    folder = importedFolder
                }

                val entry: BookmarkItem = BookmarkSiteItem(
                        `object`.getString(KEY_TITLE),
                        `object`.getString(KEY_URL),
                        BookmarkUtils.getNewId()
                )

                manager.add(folder, entry)
            }

            scanner.close()
            manager.save()
        }
        else{
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, requireContext().resources.getText(R.string.app_restart), Toast.LENGTH_LONG).show()
    }

    private fun exportBookmarks(uri: Uri){
        val manager = BookmarkManager.getInstance(requireActivity())
        val bookmarkFile = manager.file

        val output: OutputStream? = requireActivity().contentResolver.openOutputStream(uri)
        output?.write(bookmarkFile.readBytes())
        output?.flush()
        output?.close()
        Toast.makeText(context, R.string.successful, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri: Uri? = data?.data
        if (requestCode == EXPORT_BOOKMARKS && resultCode == Activity.RESULT_OK) {
           if(uri != null){
               exportBookmarks(uri)
           }
        }
        else if(requestCode == IMPORT_BOOKMARKS) {
            if(uri != null){
                importBookmarks(uri)
            }
        }
    }

    companion object{
        const val EXPORT_BOOKMARKS = 0
        const val IMPORT_BOOKMARKS = 1

        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_FOLDER = "folder"
        const val KEY_ORDER = "order"
    }
}