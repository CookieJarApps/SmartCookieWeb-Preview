package com.cookiejarapps.android.smartcookieweb.browser.bookmark.items

import com.cookiejarapps.android.smartcookieweb.utils.BookmarkUtils
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.io.IOException
import java.io.Serializable

abstract class BookmarkItem(var title: String?, val id: Long) : Serializable {

    protected abstract val type: Int

    @Throws(IOException::class)
    fun write(writer: JsonWriter): Boolean {
        writer.beginObject()
        writer.name(COLUMN_NAME_TYPE)
        writer.value(type)
        writer.name(COLUMN_NAME_TITLE)
        writer.value(title)
        writer.name(COLUMN_NAME_ID)
        writer.value(id)
        val result = writeMain(writer)
        writer.endObject()
        return result
    }

    @Throws(IOException::class)
    protected fun read(reader: JsonReader, parent: BookmarkFolderItem): BookmarkItem? {
        if (reader.peek() != JsonReader.Token.BEGIN_OBJECT){
            return null
        }
        reader.beginObject()
        var id = -1
        var itemId = -1L
        var title: String? = null
        var lastName: String? = null
        loop@ while (reader.hasNext()) {
            when (val name = reader.nextName()) {
                COLUMN_NAME_TYPE -> {
                    if (reader.peek() != JsonReader.Token.NUMBER){
                        return null
                    }
                    id = reader.nextInt()
                }
                COLUMN_NAME_TITLE -> {
                    if (reader.peek() == JsonReader.Token.STRING) {
                        title = reader.nextString()
                    } else {
                        reader.skipValue()
                    }
                }
                COLUMN_NAME_ID -> {
                    if (reader.peek() != JsonReader.Token.NUMBER) return null
                    itemId = reader.nextLong()
                }
                else -> {
                    lastName = name
                    break@loop
                }
            }
        }
        if (itemId < 0){
            itemId = BookmarkUtils.getNewId()
        }

        if(id < 0 || title == null){
            return null
        }

        val item = when (id) {
            BookmarkFolderItem.BOOKMARK_ITEM_ID -> BookmarkFolderItem(title, parent, itemId)
            BookmarkSiteItem.BOOKMARK_ITEM_ID -> BookmarkSiteItem(title, itemId)
            else -> return null
        }
        item.readMain(lastName, reader)
        while (reader.peek() != JsonReader.Token.END_OBJECT) {
            reader.skipValue()
        }
        reader.endObject()
        return item
    }

    @Throws(IOException::class)
    protected abstract fun writeMain(writer: JsonWriter): Boolean

    @Throws(IOException::class)
    protected abstract fun readMain(name: String?, reader: JsonReader): Boolean

    companion object {
        protected const val COLUMN_NAME_TYPE = "0"
        protected const val COLUMN_NAME_TITLE = "1"
        protected const val COLUMN_NAME_ID = "3"
    }
}