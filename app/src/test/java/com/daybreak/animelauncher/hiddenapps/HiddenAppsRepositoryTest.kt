package com.daybreak.animelauncher.hiddenapps

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para las operaciones de dominio sobre Apps Ocultas
 * (ocultar, desocultar, deduplicación, desinstalación, regla propia del launcher).
 */
class HiddenAppsRepositoryTest {

    private var config = HiddenAppsConfig()
    private val ownPackage = "com.daybreak.animelauncher"

    // Repositorio en memoria puro para tests
    private class TestCredentialStore {
        var cleared = false
        fun clearCredential(): Boolean {
            cleared = true
            return true
        }
    }

    private val testStore = TestCredentialStore()

    private fun hidePackage(pkg: String): Boolean {
        if (pkg.isBlank()) return false
        if (pkg == ownPackage) return false
        if (config.hiddenPackages.contains(pkg)) return true
        config = config.copy(hiddenPackages = config.hiddenPackages + pkg)
        return true
    }

    private fun unhidePackage(pkg: String) {
        if (!config.hiddenPackages.contains(pkg)) return
        config = config.copy(hiddenPackages = config.hiddenPackages - pkg)
    }

    private fun pruneUninstalled(installed: Set<String>) {
        val valid = config.hiddenPackages.filter { it in installed }.toSet()
        config = config.copy(hiddenPackages = valid)
    }

    @Before
    fun setUp() {
        config = HiddenAppsConfig()
        testStore.cleared = false
    }

    @Test
    fun testHideApp() {
        val result = hidePackage("com.whatsapp")
        assertTrue(result)
        assertTrue(config.hiddenPackages.contains("com.whatsapp"))
        assertEquals(1, config.hiddenPackages.size)
    }

    @Test
    fun testUnhideApp() {
        hidePackage("com.whatsapp")
        hidePackage("com.telegram")
        assertEquals(2, config.hiddenPackages.size)

        unhidePackage("com.whatsapp")
        assertFalse(config.hiddenPackages.contains("com.whatsapp"))
        assertTrue(config.hiddenPackages.contains("com.telegram"))
        assertEquals(1, config.hiddenPackages.size)
    }

    @Test
    fun testDuplicateHideHarmless() {
        hidePackage("com.whatsapp")
        hidePackage("com.whatsapp")
        hidePackage("com.whatsapp")

        assertEquals(1, config.hiddenPackages.size)
        assertTrue(config.hiddenPackages.contains("com.whatsapp"))
    }

    @Test
    fun testUninstallHiddenAppCleanup() {
        hidePackage("com.whatsapp")
        hidePackage("com.uninstalled.app")

        val installedPackages = setOf("com.whatsapp", "com.spotify")
        pruneUninstalled(installedPackages)

        assertTrue(config.hiddenPackages.contains("com.whatsapp"))
        assertFalse("App desinstalada debe ser purgada", config.hiddenPackages.contains("com.uninstalled.app"))
        assertEquals(1, config.hiddenPackages.size)
    }

    @Test
    fun testOwnLauncherCannotBeHidden() {
        val result = hidePackage(ownPackage)
        assertFalse("El paquete propio de NovaLauncher no debe poder ocultarse", result)
        assertFalse(config.hiddenPackages.contains(ownPackage))
        assertTrue(config.hiddenPackages.isEmpty())
    }

    @Test
    fun testEmptyHiddenList() {
        assertTrue(config.hiddenPackages.isEmpty())
    }

    @Test
    fun testAllAppsHiddenFromDrawer() {
        val installedApps = listOf(
            "com.app1",
            "com.app2",
            "com.app3"
        )

        for (pkg in installedApps) {
            hidePackage(pkg)
        }

        assertEquals(3, config.hiddenPackages.size)

        // Simulación del filtro del App Drawer:
        val visibleInDrawer = installedApps.filter { it !in config.hiddenPackages }
        assertTrue("Si todas las apps están ocultas, la lista del drawer queda vacía", visibleInDrawer.isEmpty())
    }
}
