package com.daybreak.animelauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio oficial para el conteo de notificaciones en tiempo real.
 * 
 * Privacidad y eficiencia:
 * - NO almacena ni procesa contenido, remitente ni texto privado de las notificaciones.
 * - Solo cuenta la cantidad de notificaciones activas no continuas (excluye descargas en curso, llamadas activas, etc.).
 * - Deja preparada la arquitectura para filtrado futuro por paquetes de mensajería (WhatsApp, Telegram, SMS, etc.).
 */
class NotificationMonitorService : NotificationListenerService() {

    companion object {
        private val _notificationCount = MutableStateFlow(0)
        val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

        var isConnected = false
            private set

        // Lista base extensible de paquetes de mensajería para filtrado estricto
        val KNOWN_MESSAGING_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.plus",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.facebook.orca",
            "com.instagram.android",
            "com.discord",
            "com.slack",
            "com.google.android.gm",
            "com.microsoft.teams",
            "com.viber.voip",
            "jp.naver.line.android",
            "com.tencent.mm",
            "com.skype.raider",
            "org.thoughtcrime.securesms"
        )

        fun isMessagingNotification(sbn: StatusBarNotification): Boolean {
            if (sbn.isOngoing) return false
            val pkg = sbn.packageName?.lowercase() ?: return false
            if (KNOWN_MESSAGING_PACKAGES.contains(pkg)) return true
            val cat = sbn.notification?.category
            if (cat == android.app.Notification.CATEGORY_MESSAGE || cat == android.app.Notification.CATEGORY_EMAIL) return true
            return false
        }

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun openPermissionSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        updateCount()
    }

    override fun onListenerDisconnected() {
        isConnected = false
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateCount()
    }

    private fun updateCount() {
        try {
            val active = activeNotifications
            if (active != null) {
                // Cuenta exclusivamente notificaciones activas de mensajería para no presentar otras alertas como mensajes
                val count = active.count { isMessagingNotification(it) }
                _notificationCount.value = count
            } else {
                _notificationCount.value = 0
            }
        } catch (e: Exception) {
            _notificationCount.value = 0
        }
    }
}
