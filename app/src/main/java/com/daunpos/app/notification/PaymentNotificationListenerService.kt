package com.daunpos.app.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class PaymentNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        PaymentNotificationBridge.setConnected(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        PaymentNotificationBridge.setConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // Privacy Whitelist: Early-return jika package bukan aplikasi perbankan/e-wallet yang dikenali.
        // Mencegah ekstraksi extras atau pembacaan notifikasi pribadi (WhatsApp, Telegram, SMS, OTP, dll).
        if (!BankNotificationParser.isKnownBankPackage(packageName)) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Ekstrak Title dan Text dari notifikasi sistem perbankan
        val title = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getString(Notification.EXTRA_TEXT)

        // Parse melalui parser perbankan
        val event = BankNotificationParser.parseNotification(
            packageName = packageName,
            title = title,
            text = text
        )

        if (event != null) {
            // Data pembayaran diproses murni di memori tanpa mencetak logcat untuk menjaga keamanan data finansial
            PaymentNotificationBridge.postPayment(event)
        }
    }
}
