package com.cookiejarapps.android.smartcookieweb.browser.shortcuts

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ShortcutEntity::class],
    version = 2
)
abstract class ShortcutDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao
}