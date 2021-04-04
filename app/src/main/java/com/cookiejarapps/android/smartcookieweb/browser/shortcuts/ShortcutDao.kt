package com.cookiejarapps.android.smartcookieweb.browser.shortcuts

import androidx.room.*

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcutentity")
    fun getAll(): List<ShortcutEntity>

    @Query("SELECT * FROM shortcutentity WHERE uid IN (:shortcutIds)")
    fun loadAllByIds(shortcutIds: IntArray): List<ShortcutEntity>

    @Query("SELECT * FROM shortcutentity WHERE url LIKE :urlFind LIMIT 1")
    fun findByUrl(urlFind: String): ShortcutEntity

    @Update
    fun update(item: ShortcutEntity)

    @Insert
    fun insertAll(vararg item: ShortcutEntity)

    @Delete
    fun delete(item: ShortcutEntity)
}