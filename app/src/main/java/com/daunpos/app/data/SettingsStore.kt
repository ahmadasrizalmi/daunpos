package com.daunpos.app.data

import android.content.Context
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
        val next = prefs.getInt("next_invoice", 200)
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
            footerText = prefs.getString("footer_text", "Terima kasih sudah berkunjung!\nPassword Wi-Fi: kopi123")
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
