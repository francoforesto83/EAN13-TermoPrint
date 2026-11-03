package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcode_items")
data class BarcodeItem(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val name: String,
  val price: String,
  val sku: String,
  val ean13: String,
  val paperSize: String = "58mm", // "58mm" or "80mm"
  val showPrice: Boolean = true,
  val showSku: Boolean = true,
  val storeTag: String = "MERCADO LIBRE FULL",
  val timestamp: Long = System.currentTimeMillis()
)
