package com.example.clipboardkeyboard

import kotlinx.coroutines.flow.Flow

class ClipboardRepository(private val dao: ClipboardDao) {

    fun getAllItems(): Flow<List<ClipboardItem>> = dao.getAllItems()

    fun searchItems(query: String): Flow<List<ClipboardItem>> =
        dao.searchItems("%$query%")

    suspend fun addItem(text: String) {
        val existing = dao.getAllItems()
        val maxPos = dao.getMaxQueuePosition() ?: -1
        val item = ClipboardItem(
            text = text,
            queuePosition = maxPos + 1
        )
        dao.insert(item)
    }

    suspend fun deleteItem(item: ClipboardItem) {
        dao.shiftQueuePositionsDown(item.queuePosition)
        dao.delete(item)
    }

    suspend fun deleteItems(ids: List<Long>) {
        dao.deleteByIds(ids)
    }

    suspend fun clearAll() = dao.clearAll()

    suspend fun clearUnpinned() = dao.clearUnpinned()

    suspend fun togglePin(item: ClipboardItem) {
        dao.update(item.copy(isPinned = !item.isPinned))
    }

    suspend fun getNextQueueItem(): ClipboardItem? = dao.getNextQueueItem()

    suspend fun consumeNextQueueItem(): ClipboardItem? {
        val item = dao.getNextQueueItem() ?: return null
        dao.shiftQueuePositionsDown(item.queuePosition)
        dao.delete(item)
        return item
    }
}
