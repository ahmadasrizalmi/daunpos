package com.daunpos.app.data

import android.content.Context
import com.daunpos.app.data.qris.QrisConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("daunpos_settings", Context.MODE_PRIVATE)

    private val _receipt = MutableStateFlow(loadReceipt())
    val receipt: StateFlow<ReceiptSettings> = _receipt.asStateFlow()

    private val _printer = MutableStateFlow(loadPrinter())
    val printer: StateFlow<PrinterSettings> = _printer.asStateFlow()

    private val _staticQris = MutableStateFlow(
        prefs.getString("static_qris", "") ?: ""
    )
    val staticQris: StateFlow<String> = _staticQris.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(
        prefs.getBoolean("onboarding_completed", false)
    )
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _storeProfile = MutableStateFlow(loadStoreProfile())
    val storeProfile: StateFlow<StoreProfile> = _storeProfile.asStateFlow()

    fun completeOnboarding(
        name: String,
        category: BusinessCategory,
        address: String,
        phone: String,
        qris: String,
        isMultiUser: Boolean
    ) {
        val cleanQris = qris.trim().replace("\\s+".toRegex(), "").ifBlank { _staticQris.value }
        val tax = category.defaultTax

        val newProfile = StoreProfile(
            storeName = name,
            businessCategory = category,
            address = address,
            phoneNumber = phone,
            taxPercentage = tax,
            staticQris = cleanQris,
            isMultiUserEnabled = isMultiUser,
            isOnboardingCompleted = true
        )

        _storeProfile.value = newProfile
        _isOnboardingCompleted.value = true
        _staticQris.value = cleanQris

        prefs.edit()
            .putBoolean("onboarding_completed", true)
            .putString("store_name", name)
            .putString("business_category", category.id)
            .putString("store_address", address)
            .putString("store_phone", phone)
            .putString("static_qris", cleanQris)
            .putBoolean("is_multi_user", isMultiUser)
            .apply()

        // Sync store name and address to receipt headers
        updateReceipt(
            _receipt.value.copy(
                headerLines = listOf(name, address, phone),
                taxPercentage = tax
            )
        )
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _isOnboardingCompleted.value = completed
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    private fun loadStoreProfile(): StoreProfile {
        val name = prefs.getString("store_name", "DaunPOS") ?: "DaunPOS"
        val categoryStr = prefs.getString("business_category", "FNB") ?: "FNB"
        val category = BusinessCategory.fromId(categoryStr)
        val address = prefs.getString("store_address", "Jl. Daun Hijau No. 8, Bandung") ?: "Jl. Daun Hijau No. 8, Bandung"
        val phone = prefs.getString("store_phone", "0812-3456-7890") ?: "0812-3456-7890"
        val qris = prefs.getString("static_qris", "") ?: ""
        val isMulti = prefs.getBoolean("is_multi_user", false)
        val completed = prefs.getBoolean("onboarding_completed", false)

        return StoreProfile(
            storeName = name,
            businessCategory = category,
            address = address,
            phoneNumber = phone,
            taxPercentage = category.defaultTax,
            staticQris = qris,
            isMultiUserEnabled = isMulti,
            isOnboardingCompleted = completed
        )
    }

    fun updateStaticQris(raw: String) {
        val clean = raw.trim().replace("\\s+".toRegex(), "")
        _staticQris.value = clean
        prefs.edit().putString("static_qris", clean).apply()
    }

    fun updateReceipt(settings: ReceiptSettings) {
        _receipt.value = settings
        prefs.edit()
            .putInt("paper_width", settings.paperWidth)
            .putBoolean("auto_print", settings.autoPrint)
            .putBoolean("cash_drawer", settings.cashDrawer)
            .putBoolean("show_logo", settings.showLogo)
            .putBoolean("show_qr", settings.showQR)
            .putString("header_lines", JSONArray(settings.headerLines).toString())
            .putString("footer_text", settings.footerText)
            .apply()
    }

    fun updatePrinter(settings: PrinterSettings) {
        _printer.value = settings
        prefs.edit()
            .putBoolean("printer_connected", settings.connected)
            .putString("printer_name", settings.deviceName)
            .putString("printer_address", settings.deviceAddress)
            .putString("printer_scan_type", settings.scanType)
            .putString("printer_found", JSONArray(settings.foundDevices).toString())
            .apply()
    }

    fun nextInvoiceNumber(): String {
        val next = prefs.getInt("next_invoice", 184)
        prefs.edit().putInt("next_invoice", next + 1).apply()
        return "#INV-" + next.toString().padStart(4, '0')
    }

    private fun loadReceipt(): ReceiptSettings {
        val headers = runCatching {
            val arr = JSONArray(prefs.getString("header_lines", null) ?: return@runCatching null)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrNull() ?: listOf("DaunPOS Coffee & Eatery", "Jl. Daun Hijau No. 8, Bandung", "0812-3456-7890")

        return ReceiptSettings(
            paperWidth = prefs.getInt("paper_width", 58),
            autoPrint = prefs.getBoolean("auto_print", true),
            cashDrawer = prefs.getBoolean("cash_drawer", false),
            showLogo = prefs.getBoolean("show_logo", true),
            showQR = prefs.getBoolean("show_qr", true),
            headerLines = headers,
            footerText = prefs.getString("footer_text", "Terima kasih sudah berkunjung!\nSilakan datang kembali.")
                ?: "Terima kasih sudah berkunjung!"
        )
    }

    private fun loadPrinter(): PrinterSettings {
        val found = runCatching {
            val arr = JSONArray(prefs.getString("printer_found", null) ?: return@runCatching null)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrNull() ?: listOf("RPP02N Bluetooth Printer", "DAUNPOS-KITCHEN", "58BT-Thermal-01")

        return PrinterSettings(
            connected = prefs.getBoolean("printer_connected", false),
            deviceName = prefs.getString("printer_name", "") ?: "",
            deviceAddress = prefs.getString("printer_address", "") ?: "",
            scanType = prefs.getString("printer_scan_type", "Bluetooth") ?: "Bluetooth",
            foundDevices = found
        )
    }
}
