package com.example.clipboardkeyboard

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, queuePosition ASC")
    fun getAllItems(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE text LIKE :query ORDER BY isPinned DESC, queuePosition ASC")
    fun searchItems(query: String): Flow<List<ClipboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem): Long

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun clearUnpinned()

    @Query("DELETE FROM clipboard_items")
    suspend fun clearAll()

    @Update
    suspend fun update(item: ClipboardItem)

    @Query("SELECT * FROM clipboard_items WHERE isPinned = 0 ORDER BY queuePosition ASC LIMIT 1")
    suspend fun getNextQueueItem(): ClipboardItem?

    @Query("SELECT MAX(queuePosition) FROM clipboard_items")
    suspend fun getMaxQueuePosition(): Int?

    @Query("UPDATE clipboard_items SET queuePosition = queuePosition - 1 WHERE queuePosition > :position AND isPinned = 0")
    suspend fun shiftQueuePositionsDown(position: Int)

    @Query("SELECT COUNT(*) FROM clipboard_items WHERE isPinned = 0")
    suspend fun getUnpinnedCount(): Int
}
