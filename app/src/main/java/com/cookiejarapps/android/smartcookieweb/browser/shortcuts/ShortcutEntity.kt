package com.cookiejarapps.android.smartcookieweb.browser.shortcuts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class
ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "url") var url: String?,
    //TODO: Remove add boolean: no longer needed
    @ColumnInfo(name = "boolean") val add: Boolean = false,
    @ColumnInfo(name = "title") var title: String?
)