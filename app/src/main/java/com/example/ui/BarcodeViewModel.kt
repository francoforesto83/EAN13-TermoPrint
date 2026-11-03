package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BarcodeRepository
import com.example.model.BarcodeItem
import com.example.model.Ean13Generator
import com.example.thermal.ThermalTicketRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LabelUiState(
  val productName: String = "Auriculares Inalámbricos Bluetooth Pro",
  val price: String = "14999",
  val sku: String = "MLB-PRO-BLK",
  val ean13: String = "7791234567897",
  val paperSize: String = "58mm", // "58mm" or "80mm"
  val showPrice: Boolean = true,
  val showSku: Boolean = true,
  val storeTag: String = "FULL",
  val copies: Int = 1
)

class BarcodeViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: BarcodeRepository

  init {
    val dao = AppDatabase.getDatabase(application).barcodeDao()
    repository = BarcodeRepository(dao)
  }

  private val _uiState = MutableStateFlow(LabelUiState())
  val uiState: StateFlow<LabelUiState> = _uiState.asStateFlow()

  // Search in History
  var searchQuery = MutableStateFlow("")
    private set

  val historyList: StateFlow<List<BarcodeItem>> = searchQuery
    .flatMapLatest { query ->
      if (query.isBlank()) {
        repository.allItems
      } else {
        repository.searchItems(query)
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Reactive ticket bitmap derived from state
  val currentBitmap: StateFlow<Bitmap> = _uiState.map { state ->
    val cleanCode = state.ean13.filter { it.isDigit() }
    val code13 = if (cleanCode.length == 12) {
      val check = Ean13Generator.calculateCheckDigit(cleanCode) ?: 0
      cleanCode + check
    } else {
      cleanCode
    }
    ThermalTicketRenderer.renderTicketBitmap(
      name = state.productName,
      price = state.price,
      sku = state.sku,
      ean13 = code13,
      paperSize = state.paperSize,
      showPrice = state.showPrice,
      showSku = state.showSku,
      storeTag = "MERCADO LIBRE ${state.storeTag}"
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    ThermalTicketRenderer.renderTicketBitmap(
      name = _uiState.value.productName,
      price = _uiState.value.price,
      sku = _uiState.value.sku,
      ean13 = _uiState.value.ean13,
      paperSize = _uiState.value.paperSize
    )
  )

  fun updateProductName(newName: String) {
    _uiState.value = _uiState.value.copy(productName = newName)
  }

  fun updatePrice(newPrice: String) {
    _uiState.value = _uiState.value.copy(price = newPrice)
  }

  fun updateSku(newSku: String) {
    _uiState.value = _uiState.value.copy(sku = newSku.uppercase())
  }

  fun updateEan13(newEan: String) {
    val digits = newEan.filter { it.isDigit() }.take(13)
    _uiState.value = _uiState.value.copy(ean13 = digits)
  }

  fun updatePaperSize(newSize: String) {
    _uiState.value = _uiState.value.copy(paperSize = newSize)
  }

  fun toggleShowPrice(show: Boolean) {
    _uiState.value = _uiState.value.copy(showPrice = show)
  }

  fun toggleShowSku(show: Boolean) {
    _uiState.value = _uiState.value.copy(showSku = show)
  }

  fun updateCopies(count: Int) {
    _uiState.value = _uiState.value.copy(copies = count.coerceIn(1, 100))
  }

  fun updateSearchQuery(query: String) {
    searchQuery.value = query
  }

  /**
   * If user has typed 12 digits, calculates and appends the 13th check digit.
   */
  fun calculateAndAppendCheckDigit() {
    val current = _uiState.value.ean13.filter { it.isDigit() }
    if (current.length >= 12) {
      val d12 = current.take(12)
      val check = Ean13Generator.calculateCheckDigit(d12)
      if (check != null) {
        _uiState.value = _uiState.value.copy(ean13 = d12 + check)
      }
    }
  }

  /**
   * Generates a random valid EAN-13 code with Argentine/Mercado Libre 779 prefix
   */
  fun generateRandomEan() {
    val code = Ean13Generator.generateRandomEan13("779")
    _uiState.value = _uiState.value.copy(ean13 = code)
  }

  /**
   * Fills form with a typical Mercado Libre bestseller
   */
  fun quickFillSample() {
    val samples = listOf(
      Triple("Auriculares Bluetooth TWS i12 Pro", "12499", "MLB-TWS-BLK"),
      Triple("Termo de Acero Inoxidable 1L Doble Pared", "29900", "TER-INOX-1000"),
      Triple("Mate Térmico con Bombilla Acero", "8900", "MAT-TERM-AZUL"),
      Triple("Funda Silicona Líquida Antigolpes", "4999", "FND-SIL-NEGRA"),
      Triple("Cable USB-C Carga Rápida 2m Reforzado", "3499", "CBL-USBC-2M")
    )
    val randomSample = samples.random()
    val randomCode = Ean13Generator.generateRandomEan13("779")
    _uiState.value = _uiState.value.copy(
      productName = randomSample.first,
      price = randomSample.second,
      sku = randomSample.third,
      ean13 = randomCode
    )
  }

  fun saveCurrentLabel(onSaved: () -> Unit) {
    viewModelScope.launch {
      val state = _uiState.value
      var code = state.ean13.filter { it.isDigit() }
      if (code.length == 12) {
        val check = Ean13Generator.calculateCheckDigit(code) ?: 0
        code += check
        _uiState.value = _uiState.value.copy(ean13 = code)
      }

      val item = BarcodeItem(
        name = state.productName.trim().ifBlank { "Producto Mercado Libre" },
        price = state.price.trim().ifBlank { "0" },
        sku = state.sku.trim().ifBlank { "SKU" },
        ean13 = code,
        paperSize = state.paperSize,
        showPrice = state.showPrice,
        showSku = state.showSku,
        storeTag = state.storeTag
      )
      repository.insertItem(item)
      onSaved()
    }
  }

  fun loadFromItem(item: BarcodeItem) {
    _uiState.value = _uiState.value.copy(
      productName = item.name,
      price = item.price,
      sku = item.sku,
      ean13 = item.ean13,
      paperSize = item.paperSize,
      showPrice = item.showPrice,
      showSku = item.showSku
    )
  }

  fun deleteHistoryItem(item: BarcodeItem) {
    viewModelScope.launch {
      repository.deleteItem(item)
    }
  }

  fun clearAllHistory() {
    viewModelScope.launch {
      repository.clearAll()
    }
  }
}
