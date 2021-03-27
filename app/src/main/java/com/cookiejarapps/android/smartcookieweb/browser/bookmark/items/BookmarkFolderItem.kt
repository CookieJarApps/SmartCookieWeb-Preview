package com.cookiejarapps.android.smartcookieweb.browser.bookmark.items

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.io.IOException
import java.io.Serializable
import java.util.*

class BookmarkFolderItem(title: String?, var parent: BookmarkFolderItem?, id: Long) : BookmarkItem(title, id), Serializable {

    internal val list: ArrayList<BookmarkItem> = ArrayList()

    val itemList: MutableList<BookmarkItem>
        get() = list

    override val type: Int
        get() = BOOKMARK_ITEM_ID

    fun add(item: BookmarkItem) {
        list.add(item)
    }

    fun add(folder: BookmarkFolderItem) {
        list.add(folder)
    }

    fun addAtStart(folder: BookmarkFolderItem) {
        list.add(0, folder)
    }

    operator fun get(index: Int): BookmarkItem {
        return list[index]
    }

    fun size(): Int {
        return list.size
    }

    fun clear() {
        list.clear()
    }

    @Throws(IOException::class)
    override fun writeMain(writer: JsonWriter): Boolean {
        writer.name(COLUMN_NAME_LIST)
        writer.beginArray()
        list.forEach {
            if (!it.write(writer)) return false
        }
        writer.endArray()
        return true
    }

    @Throws(IOException::class)
    override fun readMain(name: String?, reader: JsonReader): Boolean {
        if (COLUMN_NAME_LIST != name) return false
        if (reader.peek() != JsonReader.Token.BEGIN_ARRAY) return false
        reader.beginArray()
        while (reader.hasNext()) {
            list.add(read(reader, this) ?: return false)
        }
        reader.endArray()
        return true
    }

    @Throws(IOException::class)
    fun writeForRoot(writer: JsonWriter): Boolean {
        writer.beginArray()
        list.forEach {
            if (!it.write(writer)) return false
        }
        writer.endArray()
        return true
    }

    @Throws(IOException::class)
    fun readForRoot(reader: JsonReader): Boolean {
        if (reader.peek() != JsonReader.Token.BEGIN_ARRAY) return false
        reader.beginArray()
        while (reader.hasNext()) {
            list.add(read(reader, this) ?: return false)
        }
        reader.endArray()
        return true
    }

    companion object {
        const val COLUMN_NAME_LIST = "2"
        const val BOOKMARK_ITEM_ID = 1
    }
}