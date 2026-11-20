package com.daunpos.app.ui.receipt

import android.content.Context
import android.content.Intent

object ReceiptSharing {
    private val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun isWhatsAppAvailable(context: Context): Boolean =
        whatsappPackages.any { packageName ->
            runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
        }

    fun share(context: Context, receiptText: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        val targetPackage = whatsappPackages.firstOrNull { packageName ->
            runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess
        }

        if (targetPackage != null) {
            val directIntent = Intent(shareIntent).setPackage(targetPackage)
            if (runCatching { context.startActivity(directIntent) }.isSuccess) return
        }

        context.startActivity(Intent.createChooser(shareIntent, "Bagikan struk"))
    }
}
