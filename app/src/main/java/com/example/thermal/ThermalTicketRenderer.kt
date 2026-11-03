package com.example.thermal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.model.Ean13Generator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThermalTicketRenderer {

  /**
   * Generates a monochrome Bitmap tailored for thermal receipt printers (58mm or 80mm).
   * 58mm: 384px wide
   * 80mm: 576px wide
   */
  fun renderTicketBitmap(
    name: String,
    price: String,
    sku: String,
    ean13: String,
    paperSize: String = "58mm",
    showPrice: Boolean = true,
    showSku: Boolean = true,
    storeTag: String = "MERCADO LIBRE FULL"
  ): Bitmap {
    val width = if (paperSize == "80mm") 576 else 384
    val scale = if (paperSize == "80mm") 1.5f else 1.0f

    // Compute estimated height
    val baseHeight = (300 * scale).toInt()
    val bitmap = Bitmap.createBitmap(width, baseHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Clean white background
    canvas.drawColor(Color.WHITE)

    val paint = Paint().apply {
      isAntiAlias = false // High contrast sharp edges for thermal printing
      color = Color.BLACK
    }

    val textPaint = Paint().apply {
      isAntiAlias = true
      color = Color.BLACK
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    var currentY = 16f * scale

    // 1. Header border and text
    textPaint.textSize = 12f * scale
    textPaint.textAlign = Paint.Align.LEFT
    canvas.drawText("MERCADO LIBRE", 16f * scale, currentY, textPaint)

    textPaint.textSize = 10f * scale
    textPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(storeTag, width - 16f * scale, currentY, textPaint)

    currentY += 8f * scale
    paint.strokeWidth = 2f * scale
    canvas.drawLine(16f * scale, currentY, width - 16f * scale, currentY, paint)
    currentY += 16f * scale

    // 2. Product Name (Word wrap)
    textPaint.textSize = 14f * scale
    textPaint.textAlign = Paint.Align.CENTER
    val words = (name.ifBlank { "PRODUCTO MERCADO LIBRE" }).uppercase().split(" ")
    val lines = mutableListOf<String>()
    var currentLine = StringBuilder()
    val maxWidth = width - (32f * scale)

    for (word in words) {
      val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
      val textWidth = textPaint.measureText(testLine)
      if (textWidth <= maxWidth) {
        currentLine = StringBuilder(testLine)
      } else {
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        currentLine = StringBuilder(word)
      }
    }
    if (currentLine.isNotEmpty()) lines.add(currentLine.toString())

    // Draw up to 3 lines
    for (line in lines.take(3)) {
      canvas.drawText(line, width / 2f, currentY, textPaint)
      currentY += 18f * scale
    }

    currentY += 6f * scale

    // 3. EAN-13 Barcode
    val barcodeData = Ean13Generator.encodeToBinary(ean13)
    if (barcodeData != null) {
      val modules = barcodeData.modules
      val isGuard = barcodeData.isGuardBar
      val moduleCount = modules.size // 95
      val moduleWidth = ((width - (48f * scale)) / moduleCount).coerceAtLeast(2f)
      val barcodeTotalWidth = moduleCount * moduleWidth
      val startX = (width - barcodeTotalWidth) / 2f
      val barHeight = 48f * scale
      val guardExtra = 6f * scale

      for (i in modules.indices) {
        if (modules[i]) {
          val left = startX + (i * moduleWidth)
          val right = left + moduleWidth
          val top = currentY
          val bottom = top + barHeight + (if (isGuard[i]) guardExtra else 0f)
          canvas.drawRect(left, top, right, bottom, paint)
        }
      }

      currentY += barHeight + guardExtra + 14f * scale

      // Human-readable numbers below barcode
      textPaint.textSize = 12f * scale
      textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      textPaint.textAlign = Paint.Align.CENTER
      canvas.drawText(barcodeData.formattedDisplay, width / 2f, currentY, textPaint)
      currentY += 14f * scale
    } else {
      textPaint.textSize = 12f * scale
      textPaint.textAlign = Paint.Align.CENTER
      canvas.drawText("* $ean13 *", width / 2f, currentY, textPaint)
      currentY += 24f * scale
    }

    // 4. Separator Line
    paint.strokeWidth = 1.5f * scale
    canvas.drawLine(16f * scale, currentY, width - 16f * scale, currentY, paint)
    currentY += 16f * scale

    // 5. Price
    if (showPrice) {
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textPaint.textSize = 11f * scale
      textPaint.textAlign = Paint.Align.LEFT
      canvas.drawText("PRECIO:", 16f * scale, currentY, textPaint)

      textPaint.textSize = 18f * scale
      textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
      textPaint.textAlign = Paint.Align.RIGHT
      val formattedPrice = if (price.startsWith("$")) price else "$ $price"
      canvas.drawText(formattedPrice, width - 16f * scale, currentY, textPaint)
      currentY += 16f * scale
    }

    // 6. SKU and Date
    if (showSku) {
      textPaint.textSize = 10f * scale
      textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
      textPaint.textAlign = Paint.Align.LEFT
      canvas.drawText("SKU: ${sku.ifBlank { "N/A" }}", 16f * scale, currentY, textPaint)

      val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
      val dateStr = dateFormat.format(Date())
      textPaint.textAlign = Paint.Align.RIGHT
      canvas.drawText(dateStr, width - 16f * scale, currentY, textPaint)
      currentY += 14f * scale
    }

    // 7. Tear line (dashed)
    currentY += 8f * scale
    val finalHeight = (currentY + 12f * scale).toInt()
    
    // Crop or resize bitmap to exact content height
    val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, finalHeight)
    val finalCanvas = Canvas(finalBitmap)
    val dashPaint = Paint().apply {
      color = Color.DKGRAY
      strokeWidth = 2f
    }
    // Draw small cut mark
    var dashX = 10f
    while (dashX < width - 10f) {
      finalCanvas.drawLine(dashX, finalHeight - 4f, dashX + 8f, finalHeight - 4f, dashPaint)
      dashX += 16f
    }

    return finalBitmap
  }
}
