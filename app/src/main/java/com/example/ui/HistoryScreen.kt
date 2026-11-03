package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BarcodeItem
import com.example.thermal.ThermalPrintHelper
import com.example.thermal.ThermalTicketRenderer
import com.example.ui.theme.MlBlue

@Composable
fun HistoryScreen(
  viewModel: BarcodeViewModel,
  onLoadItemToEditor: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val historyList by viewModel.historyList.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  var showClearConfirmDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {

    // Barra de Búsqueda
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { viewModel.updateSearchQuery(it) },
      placeholder = { Text("Buscar por producto, SKU o código EAN...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { viewModel.updateSearchQuery("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("input_search_history")
    )

    // Fila de encabezado: Cantidad y Limpiar todo
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Historial Guardado (${historyList.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )

      if (historyList.isNotEmpty()) {
        TextButton(
          onClick = { showClearConfirmDialog = true },
          modifier = Modifier.testTag("btn_clear_all_history")
        ) {
          Text("Borrar Todo", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
      }
    }

    if (historyList.isEmpty()) {
      // Estado vacío
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(56.dp)
          )
          Text(
            text = if (searchQuery.isEmpty()) "No hay etiquetas en el historial." else "Sin resultados para '$searchQuery'",
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
          )
          if (searchQuery.isEmpty()) {
            TextButton(onClick = {
              viewModel.quickFillSample()
              viewModel.saveCurrentLabel {
                Toast.makeText(context, "Ejemplo guardado", Toast.LENGTH_SHORT).show()
              }
            }) {
              Text("Cargar etiqueta de prueba")
            }
          }
        }
      }
    } else {
      // Lista de etiquetas guardadas
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(historyList, key = { it.id }) { item ->
          HistoryItemCard(
            item = item,
            onLoad = {
              viewModel.loadFromItem(item)
              onLoadItemToEditor()
            },
            onReprint = {
              val bmp = ThermalTicketRenderer.renderTicketBitmap(
                name = item.name,
                price = item.price,
                sku = item.sku,
                ean13 = item.ean13,
                paperSize = item.paperSize,
                showPrice = item.showPrice,
                showSku = item.showSku
              )
              ThermalPrintHelper.printTicket(context, bmp, "ML_${item.sku}")
            },
            onDelete = {
              viewModel.deleteHistoryItem(item)
              Toast.makeText(context, "Eliminado del historial", Toast.LENGTH_SHORT).show()
            },
            onCopyCode = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("EAN-13", item.ean13)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Código copiado: ${item.ean13}", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }
    }
  }

  // Diálogo de confirmación para borrar todo
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text("¿Vaciar todo el historial?") },
      text = { Text("Se eliminarán permanentemente todas las etiquetas guardadas del almacenamiento local.") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.clearAllHistory()
            showClearConfirmDialog = false
            Toast.makeText(context, "Historial vaciado", Toast.LENGTH_SHORT).show()
          }
        ) {
          Text("Borrar Todo", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Cancelar")
        }
      }
    )
  }
}

@Composable
fun HistoryItemCard(
  item: BarcodeItem,
  onLoad: () -> Unit,
  onReprint: () -> Unit,
  onDelete: () -> Unit,
  onCopyCode: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    modifier = Modifier.fillMaxWidth().testTag("history_item_${item.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "$ ${item.price}",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 13.sp,
              color = Color(0xFF047857)
            )

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFE2E8F0)
            ) {
              Text(
                text = item.sku,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFEFF6FF)
            ) {
              Text(
                text = item.paperSize,
                fontSize = 10.sp,
                color = MlBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // EAN-13 code with copy button
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("code_row_${item.id}")
          ) {
            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = item.ean13,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color(0xFF1E293B)
            )
            IconButton(
              onClick = onCopyCode,
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(12.dp))
            }
          }
        }

        // Action buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onLoad,
            modifier = Modifier.size(36.dp).testTag("btn_load_${item.id}")
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Cargar en editor", tint = MlBlue)
          }

          IconButton(
            onClick = onReprint,
            modifier = Modifier.size(36.dp).testTag("btn_reprint_${item.id}")
          ) {
            Icon(Icons.Default.Print, contentDescription = "Reimprimir", tint = Color(0xFF059669))
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp).testTag("btn_delete_${item.id}")
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444))
          }
        }
      }
    }
  }
}
