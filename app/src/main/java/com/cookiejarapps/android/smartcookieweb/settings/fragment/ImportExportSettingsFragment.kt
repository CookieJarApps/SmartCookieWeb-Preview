package com.cookiejarapps.android.smartcookieweb.settings.fragment

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toFile
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkFolderItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkSiteItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.repository.BookmarkManager
import com.cookiejarapps.android.smartcookieweb.utils.BookmarkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
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

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_clear_bookmarks),
            onClick = { clearBookmarks() }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_import_settings),
            onClick = { requestSettingsImport() }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_export_settings),
            onClick = { requestSettingsExport() }
        )

        clickablePreference(
            preference = requireContext().resources.getString(R.string.key_clear_settings),
            onClick = { clearSettings() }
        )
    }

    private fun clearBookmarks() {
        // TODO: notify bookmark manager to clear bookmarks
        val builder = MaterialAlertDialogBuilder(activity as Activity)
        builder.setTitle(getString(R.string.clear_bookmarks))
        builder.setMessage(getString(R.string.clear_bookmarks_confirm))

        builder.setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialogInterface, which ->
            Toast.makeText(
                activity,
                R.string.successful, Toast.LENGTH_LONG
            ).show()

            val manager = BookmarkManager.getInstance(requireActivity())
            manager.file.delete()
            manager.initialize()
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, which ->

        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(true)
        alertDialog.show()
    }

    private fun requestBookmarkImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
        startActivityForResult(intent, IMPORT_BOOKMARKS)
    }

    private fun requestBookmarkExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra(Intent.EXTRA_TITLE, "BookmarkExport.txt")
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
        startActivityForResult(intent, EXPORT_BOOKMARKS)
    }

    private fun requestSettingsImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
        startActivityForResult(intent, IMPORT_SETTINGS)
    }

    private fun requestSettingsExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra(Intent.EXTRA_TITLE, "SettingsExport.txt")
            putExtra("android.content.extra.SHOW_ADVANCED", true)
        }
        startActivityForResult(intent, EXPORT_SETTINGS)
    }

    private fun importBookmarks(uri: Uri) {
        val manager = BookmarkManager.getInstance(requireActivity())
        val bookmarkFile = manager.file

        val input: InputStream? = requireActivity().contentResolver.openInputStream(uri)
        val content = input?.bufferedReader().use { it?.readText() }

        if (context?.contentResolver?.getType(uri) == "text/html") {
            bookmarkFile.delete()
            manager.initialize()

            val doc = Jsoup.parse(content)

            val bookmarkElements = doc.select("A")
            val folderElements = doc.select("H3")

            val folderArray = mutableListOf<String>()
            val folderItemArray = mutableListOf<BookmarkFolderItem>()
            val folderMap = mutableMapOf<String, BookmarkFolderItem>()

            // First, identify all folders and create them
            for (folderElement in folderElements) {
                val folderName = folderElement.text()
                if (folderName.isNotEmpty()) {
                    folderArray.add(folderName)
                    val newFolder = BookmarkFolderItem(folderName, manager.root, BookmarkUtils.getNewId())
                    folderMap[folderName] = newFolder
                    manager.root.add(newFolder)
                }
            }

            // Iterate over each anchor element to extract bookmarks and add them to their respective folders
            for (element in bookmarkElements) {
                val url = element.attr("HREF")
                val title = element.text()
                var folderName = ""

                // Find the closest folder element before this link
                val parentElements = element.parents()
                for (parentElement in parentElements) {
                    if (parentElement.tagName().lowercase() == "dl") {
                        val previousElement = parentElement.previousElementSibling()
                        if (previousElement != null && previousElement.tagName().lowercase() == "h3") {
                            folderName = previousElement.text()
                            break
                        }
                    }
                }

                // Create a new BookmarkItem and add it to the correct folder
                val entry: BookmarkItem = BookmarkSiteItem(
                    title,
                    url,
                    BookmarkUtils.getNewId()
                )

                // If folderName is not empty, find the corresponding folder. Otherwise, add it to root
                val folder = if (folderName.isNotEmpty()) {
                    folderMap[folderName] ?: manager.root
                } else {
                    manager.root
                }

                manager.add(folder, entry)
            }

            // Save the manager state
            manager.save()

            Toast.makeText(context, R.string.successful, Toast.LENGTH_SHORT).show()
        } else {

            val itemArray = JSONTokener(content).nextValue()

            // If the imported file is JSON and is an array, assume it's an export from this browser, or if it's an object, assume it's a legacy export
            if (itemArray is JSONArray) {
                val bookmarks = FileOutputStream(bookmarkFile, false)
                val contents: ByteArray = content?.toByteArray() ?: byteArrayOf()
                bookmarks.write(contents)
                bookmarks.flush()
                bookmarks.close()

                manager.initialize()
            } else if (itemArray is JSONObject) {
                bookmarkFile.delete()
                manager.initialize()

                val folderScanner = Scanner(content)
                val folderArray = mutableListOf<String>()

                // Iterate over all folder items in the bookmark list and create them
                while (folderScanner.hasNextLine()) {
                    val line = folderScanner.nextLine()
                    val `object` = JSONObject(line)
                    val folderName: String = `object`.getString(KEY_FOLDER)

                    if (folderName != "") {
                        folderArray.add(folderName)
                    }
                }

                val uniqueFolderArray = folderArray.distinct()
                val folderItemArray = mutableListOf<BookmarkFolderItem>()

                for (i in uniqueFolderArray) {
                    val newFolder = BookmarkFolderItem(i, manager.root, BookmarkUtils.getNewId())
                    folderItemArray.add(newFolder)
                    manager.root.add(newFolder)
                }

                val scanner = Scanner(content)

                // Iterate over bookmarks and add them to relevant folders based on names
                while (scanner.hasNextLine()) {
                    val line = scanner.nextLine()
                    val `object` = JSONObject(line)
                    val folderName: String = `object`.getString(KEY_FOLDER)

                    var folder = manager.root

                    // Not the best way to do this, but it should be OK because names are unique in the old bookmark system
                    if (folderName != "") {
                        folder = folderItemArray[uniqueFolderArray.indexOf(folderName)]
                    }

                    val entry: BookmarkItem = BookmarkSiteItem(
                        `object`.getString(KEY_TITLE),
                        `object`.getString(KEY_URL),
                        BookmarkUtils.getNewId()
                    )

                    manager.add(folder, entry)
                    manager.save()
                }

                scanner.close()
            } else {
                Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
                return
            }
        }

        Toast.makeText(
            context,
            requireContext().resources.getText(R.string.app_restart),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun exportBookmarks(uri: Uri) {
        val manager = BookmarkManager.getInstance(requireActivity())
        val bookmarkFile = manager.file

        val output: OutputStream? = requireActivity().contentResolver.openOutputStream(uri)
        output?.write(bookmarkFile.readBytes())
        output?.flush()
        output?.close()
        Toast.makeText(context, R.string.successful, Toast.LENGTH_SHORT).show()
    }

    private fun clearSettings() {
        val builder = MaterialAlertDialogBuilder(activity as Activity)
        builder.setTitle(getString(R.string.clear_settings))

        builder.setPositiveButton(resources.getString(R.string.mozac_feature_prompts_ok)) { dialogInterface, which ->
            Toast.makeText(
                activity,
                R.string.successful, Toast.LENGTH_LONG
            ).show()

            requireContext().getSharedPreferences(SCW_PREFERENCES, 0).edit().clear().apply()
            Toast.makeText(
                context,
                requireContext().resources.getText(R.string.app_restart),
                Toast.LENGTH_LONG
            ).show()
        }
        builder.setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, which ->

        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(true)
        alertDialog.show()
    }

    private fun exportSettings(uri: Uri) {
        val userPref = requireActivity().getSharedPreferences(SCW_PREFERENCES, 0)
        val allEntries: Map<String, *> = userPref!!.all
        var string = "{"
        for (entry in allEntries.entries) {
            string += "\"${entry.key}\"=\"${entry.value}\","
        }

        string = string.substring(0, string.length - 1) + "}"

        try {
            val output: OutputStream? = requireActivity().contentResolver.openOutputStream(uri)

            output?.write(string.toByteArray())
            output?.flush()
            output?.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: " + e.toString())
        }
    }

    private fun importSettings(uri: Uri) {
        val input: InputStream? = requireActivity().contentResolver.openInputStream(uri)

        val bufferSize = 1024
        val buffer = CharArray(bufferSize)
        val out = StringBuilder()
        val `in`: Reader = InputStreamReader(input, "UTF-8")
        while (true) {
            val rsz = `in`.read(buffer, 0, buffer.size)
            if (rsz < 0) break
            out.appendRange(buffer, 0, rsz)
        }

        val content = out.toString()

        val answer = JSONObject(content)
        val keys: JSONArray = answer.names()
        val userPref = requireActivity().getSharedPreferences(SCW_PREFERENCES, 0)
        for (i in 0 until keys.length()) {
            val key: String = keys.getString(i) // Here's your key
            val value: String = answer.getString(key) // Here's your value
            with(userPref.edit()) {
                if (value.matches("-?\\d+".toRegex())) {
                    putInt(key, value.toInt())
                } else if (value == "true" || value == "false") {
                    putBoolean(key, value.toBoolean())
                } else {
                    putString(key, value)
                }
                apply()
            }

        }
        Toast.makeText(
            context,
            requireContext().resources.getText(R.string.app_restart),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri: Uri? = data?.data
        if (requestCode == EXPORT_BOOKMARKS && resultCode == Activity.RESULT_OK) {
            if (uri != null) {
                exportBookmarks(uri)
            }
        } else if (requestCode == IMPORT_BOOKMARKS && resultCode == Activity.RESULT_OK) {
            if (uri != null) {
                importBookmarks(uri)
            }
        } else if (requestCode == EXPORT_SETTINGS && resultCode == Activity.RESULT_OK) {
            if (uri != null) {
                exportSettings(uri)
            }
        } else if (requestCode == IMPORT_SETTINGS && resultCode == Activity.RESULT_OK) {
            if (uri != null) {
                importSettings(uri)
            }
        }
    }

    companion object {
        const val EXPORT_BOOKMARKS = 0
        const val IMPORT_BOOKMARKS = 1
        const val EXPORT_SETTINGS = 2
        const val IMPORT_SETTINGS = 3

        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_FOLDER = "folder"

        const val SCW_PREFERENCES = "scw_preferences"
    }
}