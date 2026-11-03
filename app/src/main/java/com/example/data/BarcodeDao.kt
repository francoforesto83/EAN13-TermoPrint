package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.BarcodeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {
  @Query("SELECT * FROM barcode_items ORDER BY timestamp DESC")
  fun getAll(): Flow<List<BarcodeItem>>

  @Query("SELECT * FROM barcode_items WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR ean13 LIKE '%' || :query || '%' ORDER BY timestamp DESC")
  fun search(query: String): Flow<List<BarcodeItem>>

  @Query("SELECT * FROM barcode_items WHERE ean13 = :ean LIMIT 1")
  suspend fun findByEan(ean: String): BarcodeItem?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: BarcodeItem): Long

  @Update
  suspend fun update(item: BarcodeItem)

  @Delete
  suspend fun delete(item: BarcodeItem)

  @Query("DELETE FROM barcode_items WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM barcode_items")
  suspend fun clearAll()
}
