package com.daunpos.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daunpos.app.data.CartItem
import com.daunpos.app.data.OrderItem
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.PrinterSettings
import com.daunpos.app.data.Product
import com.daunpos.app.data.ReceiptSettings
import com.daunpos.app.data.ShiftRecord
import com.daunpos.app.data.Transaction
import com.daunpos.app.data.User
import com.daunpos.app.data.UserRole
import com.daunpos.app.printer.BluetoothPrinterManager
import com.daunpos.app.printer.BluetoothPrinterState
import com.daunpos.app.printer.ThermalPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DaunPOSViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DaunPOSApplication
    val repository = app.repository

    val products: StateFlow<List<Product>> =
        repository.products.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val transactions: StateFlow<List<Transaction>> =
        repository.transactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val receiptSettings: StateFlow<ReceiptSettings> = repository.receiptSettings

    val printerManager = BluetoothPrinterManager(application)
    val bluetoothState: StateFlow<BluetoothPrinterState> = printerManager.state

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _cashier = MutableStateFlow("Ayu · Kasir Pagi")
    val cashier: StateFlow<String> = _cashier.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _selectedTable = MutableStateFlow("Meja 04")
    val selectedTable: StateFlow<String> = _selectedTable.asStateFlow()

    private val _discount = MutableStateFlow(0)
    val discount: StateFlow<Int> = _discount.asStateFlow()

    private val _activeShift = MutableStateFlow<ShiftRecord?>(null)
    val activeShift: StateFlow<ShiftRecord?> = _activeShift.asStateFlow()

    private val _lastTransaction = MutableStateFlow<Transaction?>(null)
    val lastTransaction: StateFlow<Transaction?> = _lastTransaction.asStateFlow()

    val cartCount: Int get() = _cart.value.sumOf { it.quantity }
    val cartSubtotal: Int get() = _cart.value.sumOf { it.product.price * it.quantity }
    val cartTax: Int
        get() {
            val taxable = (cartSubtotal - _discount.value).coerceAtLeast(0)
            return (taxable * receiptSettings.value.taxPercentage) / 100
        }
    val cartTotal: Int get() = (cartSubtotal - _discount.value).coerceAtLeast(0) + cartTax

    init {
        // Sinkronkan status koneksi printer ke pengaturan tersimpan.
        viewModelScope.launch {
            printerManager.state.collect { s ->
                val current = repository.printerSettings.value
                if (current.deviceName != s.deviceName || current.connected != s.connected) {
                    repository.updatePrinterSettings(
                        PrinterSettings(
                            connected = s.connected,
                            deviceName = if (s.connected) s.deviceName else "",
                            deviceAddress = if (s.connected) current.deviceAddress else "",
                            scanType = current.scanType,
                            foundDevices = current.foundDevices
                        )
                    )
                }
            }
        }

        // Auto-reconnect ke printer terakhir jika tersimpan.
        viewModelScope.launch {
            val saved = repository.printerSettings.value
            if (saved.connected && saved.deviceAddress.isNotBlank()) {
                printerManager.connect(saved.deviceAddress)
            }
        }

        // Muat active shift
        viewModelScope.launch {
            _activeShift.value = repository.getActiveShift()
        }
    }

    suspend fun login(pin: String, role: UserRole): User? = withContext(Dispatchers.Default) {
        val username = if (role == UserRole.OWNER) "owner" else "kasir"
        val user = repository.authenticate(username, pin)
        if (user != null) {
            _currentUser.value = user
            _cashier.value = if (user.role == UserRole.OWNER) "Pemilik" else "Ayu · Kasir Pagi"
        }
        user
    }

    suspend fun verifyOwnerPin(pin: String): Boolean = withContext(Dispatchers.Default) {
        repository.authenticate("owner", pin) != null
    }

    fun logout() {
        _currentUser.value = null
        _cart.value = emptyList()
        _cashier.value = "Kasir"
    }

    fun setTable(table: String) {
        _selectedTable.value = table
    }

    fun setDiscount(amount: Int) {
        _discount.value = amount
    }

    // ---------- Cart ----------
    fun addToCart(product: Product) {
        if (!product.available || product.stock <= 0) return
        _cart.update { current ->
            val existing = current.find { it.product.id == product.id }
            if (existing != null) {
                if (existing.quantity >= product.stock) current
                else current.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                current + CartItem(product = product, quantity = 1)
            }
        }
    }

    fun increment(productId: String) {
        val product = products.value.find { it.id == productId }
        _cart.update { current ->
            current.map {
                if (it.product.id == productId) {
                    val max = product?.stock ?: 99
                    if (it.quantity < max) it.copy(quantity = it.quantity + 1) else it
                } else it
            }
        }
    }

    fun decrement(productId: String) {
        _cart.update { current ->
            current.mapNotNull {
                if (it.product.id == productId) {
                    if (it.quantity <= 1) null else it.copy(quantity = it.quantity - 1)
                } else it
            }
        }
    }

    fun removeFromCart(productId: String) {
        _cart.update { current -> current.filterNot { it.product.id == productId } }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _discount.value = 0
    }

    // ---------- Products ----------
    fun toggleProductAvailable(productId: String) {
        viewModelScope.launch { repository.toggleProductAvailable(productId) }
    }

    fun upsertProduct(product: Product) {
        viewModelScope.launch { repository.upsertProduct(product) }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch { repository.deleteProduct(productId) }
    }

    fun nextProductId(): String {
        val max = products.value.mapNotNull { it.id.removePrefix("P").toIntOrNull() }.maxOrNull() ?: 0
        return "P" + (max + 1).toString().padStart(3, '0')
    }

    // ---------- Transactions ----------
    fun checkout(paymentMethod: PaymentMethod, cashReceived: Int?, tableNumber: String = _selectedTable.value, note: String = ""): String {
        val items = _cart.value.map {
            OrderItem(
                productId = it.product.id,
                name = it.product.name,
                price = it.product.price,
                quantity = it.quantity,
                emoji = it.product.emoji
            )
        }
        val id = repository.nextInvoiceNumber()
        val tx = Transaction(
            id = id,
            dateTime = System.currentTimeMillis(),
            items = items,
            subtotal = cartSubtotal,
            discount = _discount.value,
            tax = cartTax,
            total = cartTotal,
            paymentMethod = paymentMethod,
            cashReceived = cashReceived,
            change = if (cashReceived != null) cashReceived - cartTotal else null,
            status = "SUKSES",
            note = note,
            tableNumber = tableNumber
        )
        _lastTransaction.value = tx
        viewModelScope.launch { repository.insertTransaction(tx) }

        // Cetak otomatis bila diaktifkan dan printer terhubung.
        if (receiptSettings.value.autoPrint && bluetoothState.value.connected) {
            printReceipt(tx)
        }

        clearCart()
        return tx.id
    }

    fun getTransaction(id: String): Transaction? =
        _lastTransaction.value?.takeIf { it.id == id } ?: transactions.value.find { it.id == id }

    fun cancelTransaction(id: String) {
        viewModelScope.launch { repository.cancelTransaction(id) }
    }

    // ---------- Shift Management ----------
    fun startShift(cashierName: String, startCash: Int) {
        viewModelScope.launch {
            _activeShift.value = repository.openShift(cashierName, startCash)
        }
    }

    fun endShift(endCash: Int) {
        val shift = _activeShift.value ?: return
        viewModelScope.launch {
            val closed = repository.closeShift(shift.id, endCash, transactions.value)
            _activeShift.value = null
        }
    }

    // ---------- Settings ----------
    fun updateReceiptSettings(settings: ReceiptSettings) = repository.updateReceiptSettings(settings)

    // ---------- Printer ----------
    fun startBluetoothDiscovery() = printerManager.startDiscovery()
    fun stopBluetoothDiscovery() = printerManager.stopDiscovery()
    fun connectPrinter(address: String) {
        val current = repository.printerSettings.value
        repository.updatePrinterSettings(current.copy(deviceAddress = address))
        printerManager.connect(address)
    }
    fun disconnectPrinter() = printerManager.disconnect()
    fun clearPrinterError() = printerManager.clearError()
    fun printReceipt(tx: Transaction) {
        printerManager.print(ThermalPrinter.buildReceiptBytes(tx, receiptSettings.value))
    }
    fun testPrint() {
        printerManager.print(ThermalPrinter.buildTestPrintBytes(receiptSettings.value))
    }

    override fun onCleared() {
        printerManager.close()
        super.onCleared()
    }
}
