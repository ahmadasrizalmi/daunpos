package com.daunpos.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("daunpos_settings", Context.MODE_PRIVATE)

    init {
        // One-time v4 reset: the previous release shipped demonstration-only catalog,
        // invoices, account credentials and merchant identity.
        if (prefs.getBoolean("mock_data_reset_v4_pending", true)) {
            prefs.edit()
                .putBoolean("onboarding_completed", false)
                .putString("store_name", "Nama Toko")
                .putString("business_category", "FNB")
                .putString("store_address", "")
                .putString("store_phone", "")
                .putString("static_qris", "")
                .putString("bank_name", "")
                .putString("bank_account_number", "")
                .putString("bank_account_holder", "")
                .putBoolean("is_multi_user", false)
                .putInt("next_invoice", 1)
                .putBoolean("printer_connected", false)
                .putString("printer_name", "")
                .putString("printer_address", "")
                .putString("printer_found", "[]")
                .putInt("tax_percentage", 0)
                .putBoolean("auto_print", false)
                .putBoolean("show_logo", false)
                .putBoolean("show_qr", false)
                .putString("header_lines", JSONArray(listOf("Nama Toko")).toString())
                .putString("footer_text", "Terima kasih telah berbelanja.")
                .putBoolean("mock_data_reset_v4_pending", false)
                .commit()
        }
    }

    private val _receipt = MutableStateFlow(loadReceipt())
    val receipt: StateFlow<ReceiptSettings> = _receipt.asStateFlow()

    private val _printer = MutableStateFlow(loadPrinter())
    val printer: StateFlow<PrinterSettings> = _printer.asStateFlow()

    private val _staticQris = MutableStateFlow(prefs.getString("static_qris", "") ?: "")
    val staticQris: StateFlow<String> = _staticQris.asStateFlow()

    private val _bankAccount = MutableStateFlow(loadBankAccount())
    val bankAccount: StateFlow<BankAccountSettings> = _bankAccount.asStateFlow()

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
        val cleanQris = qris.trim().replace("\\s+".toRegex(), "")
        val tax = 0
        val newProfile = StoreProfile(
            storeName = name.trim(),
            businessCategory = category,
            address = address.trim(),
            phoneNumber = phone.trim(),
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
            .putString("store_name", newProfile.storeName)
            .putString("business_category", category.id)
            .putString("store_address", newProfile.address)
            .putString("store_phone", newProfile.phoneNumber)
            .putString("static_qris", cleanQris)
            .putBoolean("is_multi_user", isMultiUser)
            .putBoolean("mock_data_reset_v4_pending", false)
            .apply()

        updateReceipt(
            _receipt.value.copy(
                headerLines = listOf(newProfile.storeName, newProfile.address, newProfile.phoneNumber)
                    .filter { it.isNotBlank() },
                taxPercentage = tax
            )
        )
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _isOnboardingCompleted.value = completed
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    private fun loadStoreProfile(): StoreProfile {
        val name = prefs.getString("store_name", "Nama Toko") ?: "Nama Toko"
        val category = BusinessCategory.fromId(prefs.getString("business_category", "FNB") ?: "FNB")
        val address = prefs.getString("store_address", "") ?: ""
        val phone = prefs.getString("store_phone", "") ?: ""
        val qris = prefs.getString("static_qris", "") ?: ""
        val isMulti = prefs.getBoolean("is_multi_user", false)
        val completed = prefs.getBoolean("onboarding_completed", false)

        return StoreProfile(
            storeName = name,
            businessCategory = category,
            address = address,
            phoneNumber = phone,
            taxPercentage = prefs.getInt("tax_percentage", category.defaultTax),
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

    fun updateBankAccount(settings: BankAccountSettings) {
        val normalized = settings.copy(
            bankName = settings.bankName.trim().take(40),
            accountNumber = settings.accountNumber.filter(Char::isDigit).take(30),
            accountHolder = settings.accountHolder.trim().take(60)
        )
        require(normalized.validationError() == null) {
            normalized.validationError() ?: "Data rekening tidak valid."
        }
        _bankAccount.value = normalized
        prefs.edit()
            .putString("bank_name", normalized.bankName)
            .putString("bank_account_number", normalized.accountNumber)
            .putString("bank_account_holder", normalized.accountHolder)
            .apply()
    }

    private fun loadBankAccount(): BankAccountSettings = BankAccountSettings(
        bankName = prefs.getString("bank_name", "") ?: "",
        accountNumber = prefs.getString("bank_account_number", "") ?: "",
        accountHolder = prefs.getString("bank_account_holder", "") ?: ""
    )

    fun updateReceipt(settings: ReceiptSettings) {
        val normalized = settings.copy(
            paperWidth = if (settings.paperWidth == 80) 80 else 58,
            taxPercentage = settings.taxPercentage.coerceIn(0, 100),
            headerLines = settings.headerLines.map { it.trim() }.filter { it.isNotBlank() }.take(4),
            footerText = settings.footerText.trim().take(240)
        )
        _receipt.value = normalized
        prefs.edit()
            .putInt("paper_width", normalized.paperWidth)
            .putBoolean("auto_print", normalized.autoPrint)
            .putBoolean("show_logo", normalized.showLogo)
            .putBoolean("show_qr", false)
            .putInt("tax_percentage", normalized.taxPercentage)
            .putString("header_lines", JSONArray(normalized.headerLines).toString())
            .putString("footer_text", normalized.footerText)
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

    @Synchronized
    fun nextInvoiceNumber(): String {
        val next = prefs.getInt("next_invoice", 1).coerceAtLeast(1)
        prefs.edit().putInt("next_invoice", next + 1).commit()
        return "#INV-" + next.toString().padStart(4, '0')
    }

    private fun loadReceipt(): ReceiptSettings {
        val headers = runCatching {
            val raw = prefs.getString("header_lines", null) ?: return@runCatching null
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrNull() ?: listOf("Nama Toko")

        return ReceiptSettings(
            paperWidth = if (prefs.getInt("paper_width", 58) == 80) 80 else 58,
            autoPrint = prefs.getBoolean("auto_print", false),
            showLogo = prefs.getBoolean("show_logo", false),
            showQR = false,
            taxPercentage = prefs.getInt("tax_percentage", 0).coerceIn(0, 100),
            headerLines = headers,
            footerText = prefs.getString("footer_text", "Terima kasih telah berbelanja.")
                ?: "Terima kasih telah berbelanja."
        )
    }

    private fun loadPrinter(): PrinterSettings {
        val found = runCatching {
            val raw = prefs.getString("printer_found", null) ?: return@runCatching null
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrNull() ?: emptyList()

        return PrinterSettings(
            connected = prefs.getBoolean("printer_connected", false),
            deviceName = prefs.getString("printer_name", "") ?: "",
            deviceAddress = prefs.getString("printer_address", "") ?: "",
            scanType = prefs.getString("printer_scan_type", "Bluetooth") ?: "Bluetooth",
            foundDevices = found
        )
    }
}
