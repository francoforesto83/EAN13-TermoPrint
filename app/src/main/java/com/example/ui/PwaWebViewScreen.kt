package com.example.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.ui.theme.MlBlue
import com.example.ui.theme.MlYellow
import java.io.File
import java.io.FileOutputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PwaWebViewScreen(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var webViewRef: WebView? = remember { null }

  Column(
    modifier = modifier.fillMaxSize()
  ) {
    // Header informativo con acciones para el archivo HTML
    Surface(
      color = Color(0xFF1E2348),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, contentDescription = null, tint = MlYellow, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "PWA HTML Único Archivo",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
          Text(
            text = "Tailwind CSS + JsBarcode + LocalStorage",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          // Copiar código HTML completo al portapapeles
          IconButton(
            onClick = {
              try {
                val htmlContent = context.assets.open("index.html").bufferedReader().use { it.readText() }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("index.html", htmlContent)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "✓ Código HTML copiado al portapapeles", Toast.LENGTH_LONG).show()
              } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier.testTag("btn_copy_html")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar HTML", tint = Color.White)
          }

          // Compartir archivo index.html
          IconButton(
            onClick = {
              try {
                val htmlContent = context.assets.open("index.html").bufferedReader().use { it.readText() }
                val cacheFile = File(context.cacheDir, "index.html")
                FileOutputStream(cacheFile).use { it.write(htmlContent.toByteArray()) }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/html"
                  putExtra(Intent.EXTRA_STREAM, uri)
                  putExtra(Intent.EXTRA_SUBJECT, "index.html - Generador EAN-13 Mercado Libre")
                  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir o Exportar index.html"))
              } catch (e: Exception) {
                Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier.testTag("btn_share_html")
          ) {
            Icon(Icons.Default.Share, contentDescription = "Compartir HTML", tint = MlYellow)
          }

          // Recargar WebView
          IconButton(
            onClick = { webViewRef?.reload() },
            modifier = Modifier.testTag("btn_reload_webview")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = Color.White)
          }
        }
      }
    }

    // Android WebView cargando file:///android_asset/index.html
    AndroidView(
      factory = { ctx ->
        WebView(ctx).apply {
          webViewRef = this
          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            displayZoomControls = false
            builtInZoomControls = false
          }

          // Interfaz para disparar Android PrintManager cuando el JS llame a window.print()
          addJavascriptInterface(object {
            @JavascriptInterface
            fun printFromJs() {
              post {
                val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = createPrintDocumentAdapter("EAN13_PWA_Etiqueta")
                printManager?.print("EAN13_PWA_Etiqueta", printAdapter, null)
              }
            }
          }, "AndroidNativePrint")

          webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              // Inyectar enlace para que window.print() invoque la impresión nativa si está disponible
              view?.evaluateJavascript(
                """
                (function() {
                  const origPrint = window.print;
                  window.print = function() {
                    if (window.AndroidNativePrint && typeof window.AndroidNativePrint.printFromJs === 'function') {
                      window.AndroidNativePrint.printFromJs();
                    } else {
                      origPrint.apply(window);
                    }
                  };
                })();
                """.trimIndent(), null
              )
            }
          }

          webChromeClient = WebChromeClient()
          loadUrl("file:///android_asset/index.html")
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .testTag("pwa_webview")
    )
  }
}
