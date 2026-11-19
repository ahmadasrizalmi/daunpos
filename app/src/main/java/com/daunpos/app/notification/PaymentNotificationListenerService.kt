package com.daunpos.app.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class PaymentNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "DaunPOS Payment Notification Listener Connected")
        PaymentNotificationBridge.setConnected(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "DaunPOS Payment Notification Listener Disconnected")
        PaymentNotificationBridge.setConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val packageName = sbn.packageName ?: return

        // Ekstrak Title dan Text dari notifikasi sistem
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
            Log.i(TAG, "Payment detected from ${event.bankName}: Rp ${event.amount} (Sender: '${event.senderName}')")
            PaymentNotificationBridge.postPayment(event)
        }
    }

    companion object {
        private const val TAG = "DaunPOSListener"
    }
}
