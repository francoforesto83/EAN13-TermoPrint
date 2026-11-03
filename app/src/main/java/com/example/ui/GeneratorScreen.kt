package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Ean13Generator
import com.example.thermal.ThermalPrintHelper
import com.example.ui.theme.MlBlue
import com.example.ui.theme.MlYellow

@Composable
fun GeneratorScreen(
  viewModel: BarcodeViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  val uiState by viewModel.uiState.collectAsState()
  val bitmap by viewModel.currentBitmap.collectAsState()

  val isValid = Ean13Generator.isValid(uiState.ean13)
  val is12Digits = uiState.ean13.length == 12

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {

    // Card: Formulario de Datos
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth().testTag("form_card")
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Título de sección y botón de ejemplo
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "DATOS DE LA ETIQUETA",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          TextButton(
            onClick = { viewModel.quickFillSample() },
            modifier = Modifier.testTag("btn_sample_fill")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Cargar Ejemplo", fontSize = 12.sp)
          }
        }

        // Nombre del Producto
        OutlinedTextField(
          value = uiState.productName,
          onValueChange = { viewModel.updateProductName(it) },
          label = { Text("Nombre del Producto / Publicación ML") },
          placeholder = { Text("Ej: Auriculares Bluetooth Pro") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_product_name")
        )

        // Precio y SKU en fila
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = uiState.price,
            onValueChange = { viewModel.updatePrice(it) },
            label = { Text("Precio ($)") },
            placeholder = { Text("14999") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("input_price")
          )

          OutlinedTextField(
            value = uiState.sku,
            onValueChange = { viewModel.updateSku(it) },
            label = { Text("SKU / Código") },
            placeholder = { Text("MLB-PRO-BLK") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("input_sku")
          )
        }

        // Campo Código EAN-13
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          OutlinedTextField(
            value = uiState.ean13,
            onValueChange = { viewModel.updateEan13(it) },
            label = { Text("Código EAN-13 (12 ó 13 dígitos)") },
            placeholder = { Text("779123456789") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (is12Digits) {
                  TextButton(
                    onClick = { viewModel.calculateAndAppendCheckDigit() },
                    modifier = Modifier.testTag("btn_calc_check")
                  ) {
                    Text("+ Dígito", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_ean13")
          )

          // Indicador de estado del código EAN
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (isValid) {
                Icon(
                  Icons.Default.CheckCircle,
                  contentDescription = "Válido",
                  tint = Color(0xFF10B981),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("✓ EAN-13 Válido (GS1)", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              } else if (is12Digits) {
                Icon(
                  Icons.Default.Info,
                  contentDescription = "12 dígitos",
                  tint = Color(0xFF3B82F6),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("12 dígitos: pulsa '+ Dígito' para el 13°", color = Color(0xFF3B82F6), fontSize = 11.sp)
              } else {
                Icon(
                  Icons.Default.Warning,
                  contentDescription = "Incompleto",
                  tint = Color(0xFFF59E0B),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ingresa 12 dígitos o usa Auto ML", color = Color(0xFFF59E0B), fontSize = 11.sp)
              }
            }

            TextButton(
              onClick = { viewModel.generateRandomEan() },
              modifier = Modifier.testTag("btn_auto_ean")
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Auto ML", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Selección de Formato Térmico (58mm vs 80mm)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Ancho de Impresora Térmica:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FormatSelectorChip(
              title = "58 mm (2\")",
              subtitle = "Mini POS Bluetooth",
              isSelected = uiState.paperSize == "58mm",
              onClick = { viewModel.updatePaperSize("58mm") },
              modifier = Modifier.weight(1f)
            )

            FormatSelectorChip(
              title = "80 mm (3\")",
              subtitle = "Impresora Estándar",
              isSelected = uiState.paperSize == "80mm",
              onClick = { viewModel.updatePaperSize("80mm") },
              modifier = Modifier.weight(1f)
            )
          }

          // Checkboxes de opciones
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(
                checked = uiState.showPrice,
                onCheckedChange = { viewModel.toggleShowPrice(it) },
                colors = CheckboxDefaults.colors(checkedColor = MlBlue)
              )
              Text("Mostrar Precio", fontSize = 12.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(
                checked = uiState.showSku,
                onCheckedChange = { viewModel.toggleShowSku(it) },
                colors = CheckboxDefaults.colors(checkedColor = MlBlue)
              )
              Text("Mostrar SKU / ML", fontSize = 12.sp)
            }
          }
        }

        // Botón Guardar en Historial
        OutlinedButton(
          onClick = {
            viewModel.saveCurrentLabel {
              Toast.makeText(context, "✓ Etiqueta guardada en historial", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_save_history")
        ) {
          Icon(Icons.Default.Save, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Guardar en Historial Local", fontWeight = FontWeight.SemiBold)
        }
      }
    }

    // Card: Vista Previa de Etiqueta Térmica
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "VISTA PREVIA TÉRMICA",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(2.dp)
          ) {
            Text(
              text = uiState.paperSize,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }

        // Contenedor visual del ticket de papel
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          // Papel blanco con sombra
          Box(
            modifier = Modifier
              .shadow(6.dp, RoundedCornerShape(4.dp))
              .background(Color.White, RoundedCornerShape(4.dp))
              .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(4.dp))
              .padding(8.dp)
          ) {
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = "Vista previa de etiqueta térmica EAN-13",
              modifier = Modifier
                .width(if (uiState.paperSize == "80mm") 280.dp else 220.dp)
                .testTag("thermal_preview_image")
            )
          }
        }

        // Botones de Impresión Mobile-First
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              ThermalPrintHelper.printTicket(context, bitmap, "ML_${uiState.sku}")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("btn_print_thermal")
          ) {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Imprimir Térmica", fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = {
              ThermalPrintHelper.shareTicketImage(context, bitmap, uiState.productName)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("btn_share_rawbt")
          ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = MlYellow)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bluetooth / RawBT", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }

        // Tip de impresión térmica
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
            .padding(10.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFFD97706),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Conecta tu mini impresora Bluetooth en Ajustes. Pulsa 'Imprimir Térmica' para el diálogo del sistema, o 'Bluetooth / RawBT' para enviar la imagen a apps como RawBT o POS-Printer.",
            fontSize = 11.sp,
            color = Color(0xFF92400E),
            lineHeight = 15.sp
          )
        }
      }
    }
  }
}

@Composable
fun FormatSelectorChip(
  title: String,
  subtitle: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .border(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) MlBlue else Color(0xFFCBD5E1),
        shape = RoundedCornerShape(10.dp)
      )
      .background(if (isSelected) Color(0xFFEFF6FF) else Color.White)
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp, horizontal = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = if (isSelected) MlBlue else Color(0xFF1E293B)
      )
      Text(
        text = subtitle,
        fontSize = 10.sp,
        color = Color(0xFF64748B)
      )
    }
  }
}
