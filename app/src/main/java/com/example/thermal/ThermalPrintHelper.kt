package com.example.thermal

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ThermalPrintHelper {

  /**
   * Dispatches the thermal label directly to Android PrintManager.
   * Compatible with all Android thermal print services (Bluetooth/WiFi/USB) and PDF export.
   */
  fun printTicket(context: Context, bitmap: Bitmap, jobName: String = "EAN13_Etiqueta") {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager == null) {
      Toast.makeText(context, "Servicio de impresión no disponible", Toast.LENGTH_SHORT).show()
      return
    }

    val printAdapter = object : PrintDocumentAdapter() {
      private var pdfDocument: PdfDocument? = null

      override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
      ) {
        if (cancellationSignal?.isCanceled == true) {
          callback?.onLayoutCancelled()
          return
        }

        val info = PrintDocumentInfo.Builder("$jobName.pdf")
          .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
          .setPageCount(1)
          .build()

        callback?.onLayoutFinished(info, true)
      }

      override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
      ) {
        if (destination == null) {
          callback?.onWriteFailed("Destino inválido")
          return
        }

        try {
          pdfDocument = PdfDocument()
          val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
          val page = pdfDocument!!.startPage(pageInfo)

          page.canvas.drawBitmap(bitmap, 0f, 0f, null)
          pdfDocument!!.finishPage(page)

          FileOutputStream(destination.fileDescriptor).use { output ->
            pdfDocument!!.writeTo(output)
          }

          callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
          callback?.onWriteFailed(e.message)
        } finally {
          pdfDocument?.close()
          pdfDocument = null
        }
      }
    }

    // Default attributes for thermal label (dense 203 DPI)
    val attributes = PrintAttributes.Builder()
      .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
      .build()

    printManager.print(jobName, printAdapter, attributes)
  }

  /**
   * Shares the generated ticket bitmap to external thermal printing apps (RawBT, POS-Printer, Bluetooth Print) or WhatsApp.
   */
  fun shareTicketImage(context: Context, bitmap: Bitmap, productName: String) {
    try {
      val cachePath = File(context.cacheDir, "images")
      cachePath.mkdirs()
      val file = File(cachePath, "etiqueta_ean13.png")
      FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
      }

      val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_SUBJECT, "Etiqueta Térmica: $productName")
        putExtra(Intent.EXTRA_TEXT, "Etiqueta EAN-13 para $productName generada para mini impresora térmica.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      context.startActivity(Intent.createChooser(shareIntent, "Enviar a Impresora Térmica o Compartir"))
    } catch (e: IOException) {
      Toast.makeText(context, "Error al compartir etiqueta: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }
}
