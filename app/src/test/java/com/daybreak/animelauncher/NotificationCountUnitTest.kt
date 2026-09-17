package com.daybreak.animelauncher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias de aislamiento matemático para el contador de aplicaciones
 * con mensajes pendientes (Fase 4C - T-19).
 */
class NotificationCountUnitTest {

    @Test
    fun testTwoAppsWithMultipleNotificationsFromOneApp() {
        // [WhatsApp, WhatsApp, Telegram] => 2 aplicaciones
        val packages = listOf(
            "com.whatsapp",
            "com.whatsapp",
            "org.telegram.messenger"
        )
        val count = NotificationMonitorService.countUniquePackages(packages)
        assertEquals(2, count)
    }

    @Test
    fun testSingleAppWithMultipleNotifications() {
        // [WhatsApp, WhatsApp, WhatsApp] => 1 aplicación
        val packages = listOf(
            "com.whatsapp",
            "com.whatsapp",
            "com.whatsapp"
        )
        val count = NotificationMonitorService.countUniquePackages(packages)
        assertEquals(1, count)
    }

    @Test
    fun testThreeDistinctApps() {
        // [WhatsApp, Telegram, Instagram] => 3 aplicaciones
        val packages = listOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.instagram.android"
        )
        val count = NotificationMonitorService.countUniquePackages(packages)
        assertEquals(3, count)
    }

    @Test
    fun testEmptyNotificationList() {
        val packages = emptyList<String>()
        val count = NotificationMonitorService.countUniquePackages(packages)
        assertEquals(0, count)
    }

    @Test
    fun testDuplicateWithDifferentCasingAndNulls() {
        val packages = listOf(
            "com.whatsapp",
            "COM.WHATSAPP",
            "Com.WhatsApp",
            null,
            "org.telegram.messenger",
            "ORG.TELEGRAM.MESSENGER"
        )
        val count = NotificationMonitorService.countUniquePackages(packages)
        assertEquals(2, count)
    }
}
