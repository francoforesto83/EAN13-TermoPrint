package com.example.data

import com.example.model.BarcodeItem
import kotlinx.coroutines.flow.Flow

class BarcodeRepository(private val barcodeDao: BarcodeDao) {
  val allItems: Flow<List<BarcodeItem>> = barcodeDao.getAll()

  fun searchItems(query: String): Flow<List<BarcodeItem>> = barcodeDao.search(query)

  suspend fun insertItem(item: BarcodeItem): Long = barcodeDao.insert(item)

  suspend fun deleteItem(item: BarcodeItem) = barcodeDao.delete(item)

  suspend fun deleteById(id: Long) = barcodeDao.deleteById(id)

  suspend fun clearAll() = barcodeDao.clearAll()
}
