package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BarcodeViewModel
import com.example.ui.GeneratorScreen
import com.example.ui.HistoryScreen
import com.example.ui.PwaWebViewScreen
import com.example.ui.theme.MlBlue
import com.example.ui.theme.MlYellow
import com.example.ui.theme.MyApplicationTheme

enum class AppTab(val title: String) {
  GENERATOR("Generador"),
  HISTORY("Historial"),
  WEB_PWA("Web PWA")
}

class MainActivity : ComponentActivity() {
  private val viewModel: BarcodeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainApp(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: BarcodeViewModel) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }

  // Permite volver a la pestaña Generador con el botón Atrás
  BackHandler(enabled = selectedTab != 0) {
    selectedTab = 0
  }

  Scaffold(
    topBar = {
      Surface(
        color = MlYellow,
        shadowElevation = 3.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MlBlue,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.Print,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "EAN-13 TermoPrint",
              fontSize = 17.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF1E2348)
            )
            Text(
              text = "Mercado Libre • Mini Impresoras 58/80mm",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF475569)
            )
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MlBlue
          ) {
            Text(
              text = "POS 2\"/3\"",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("bottom_nav_bar")
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.Default.QrCode, contentDescription = "Generador") },
          label = { Text("Generador", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MlBlue,
            indicatorColor = MlYellow.copy(alpha = 0.5f)
          ),
          modifier = Modifier.testTag("nav_tab_generator")
        )

        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
          label = { Text("Historial", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MlBlue,
            indicatorColor = MlYellow.copy(alpha = 0.5f)
          ),
          modifier = Modifier.testTag("nav_tab_history")
        )

        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.Language, contentDescription = "Web PWA") },
          label = { Text("Web PWA", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MlBlue,
            indicatorColor = MlYellow.copy(alpha = 0.5f)
          ),
          modifier = Modifier.testTag("nav_tab_webpwa")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      when (selectedTab) {
        0 -> GeneratorScreen(viewModel = viewModel)
        1 -> HistoryScreen(
          viewModel = viewModel,
          onLoadItemToEditor = { selectedTab = 0 }
        )
        2 -> PwaWebViewScreen()
      }
    }
  }
}
