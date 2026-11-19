package com.daunpos.app.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class BankNotificationEvent(
    val sourcePackage: String,
    val bankName: String,
    val amount: Int,
    val senderName: String = "",
    val rawTitle: String = "",
    val rawText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object PaymentNotificationBridge {

    private val _incomingPayments = MutableSharedFlow<BankNotificationEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingPayments: SharedFlow<BankNotificationEvent> = _incomingPayments.asSharedFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _recentNotifications = MutableStateFlow<List<BankNotificationEvent>>(emptyList())
    val recentNotifications: StateFlow<List<BankNotificationEvent>> = _recentNotifications.asStateFlow()

    fun postPayment(event: BankNotificationEvent) {
        _incomingPayments.tryEmit(event)
        val current = _recentNotifications.value.toMutableList()
        current.add(0, event)
        _recentNotifications.value = current.take(10)
    }

    fun setConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    /**
     * Memeriksa apakah aplikasi memiliki izin akses Notification Listener di level OS Android.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    /**
     * Membuka halaman pengaturan sistem Android untuk mengaktifkan izin Notification Listener.
     */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
