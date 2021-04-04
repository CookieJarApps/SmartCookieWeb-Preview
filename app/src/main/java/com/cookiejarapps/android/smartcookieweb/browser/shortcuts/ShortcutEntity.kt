package com.cookiejarapps.android.smartcookieweb.browser.shortcuts

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class
ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "url") var url: String?,
    @ColumnInfo(name = "boolean") val add: Boolean = false
)