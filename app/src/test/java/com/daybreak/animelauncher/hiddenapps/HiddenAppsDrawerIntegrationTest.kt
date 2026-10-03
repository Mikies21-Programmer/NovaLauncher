package com.daybreak.animelauncher.hiddenapps

import com.daybreak.animelauncher.AppShortcut
import com.daybreak.animelauncher.ui.screens.DrawerListItem
import com.daybreak.animelauncher.ui.screens.filterDrawerData
import com.daybreak.animelauncher.ui.screens.prepareDrawerCategory
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias de integración de filtrado de Apps Ocultas con el App Drawer:
 * - Ausencia en categoría preparada
 * - Ausencia en búsqueda de Drawer
 * - Secciones vacías/ocultas sin crashes
 * - Recálculo exacto del Rail A-Z
 * - Coexistencia segura con CRASH-FIX-01
 */
class HiddenAppsDrawerIntegrationTest {

    private val sampleApps = listOf(
        AppShortcut(id = "com.apple/Main", name = "Apple Music", packageName = "com.apple"),
        AppShortcut(id = "com.brave/Main", name = "Brave Browser", packageName = "com.brave"),
        AppShortcut(id = "com.calculator/Main", name = "Calculator", packageName = "com.calculator"),
        AppShortcut(id = "com.secret/Main", name = "Secret Vault", packageName = "com.secret"),
        AppShortcut(id = "com.zebra/Main", name = "Zebra Notes", packageName = "com.zebra")
    )

    @Test
    fun testHiddenAppAbsentFromPreparedCategory() {
        val hiddenPackages = setOf("com.secret")
        val filteredApps = sampleApps.filter { it.packageName !in hiddenPackages }

        val prepared = prepareDrawerCategory(filteredApps)

        // Verificar que ninguna fila contenga el paquete oculto
        val appRows = prepared.baseData.items.filterIsInstance<DrawerListItem.AppRow>()
        assertFalse(
            "La app oculta no debe estar en los items de la categoría preparada",
            appRows.any { it.app.packageName == "com.secret" }
        )
        assertEquals(4, appRows.size)
    }

    @Test
    fun testHiddenAppAbsentFromSearch() {
        val hiddenPackages = setOf("com.secret")
        val filteredApps = sampleApps.filter { it.packageName !in hiddenPackages }

        val prepared = prepareDrawerCategory(filteredApps)
        val searchResult = filterDrawerData(prepared, "Secret")

        val appRows = searchResult.items.filterIsInstance<DrawerListItem.AppRow>()
        assertTrue("La búsqueda nunca debe retornar apps ocultas", appRows.isEmpty())
    }

    @Test
    fun testHiddenOnlySectionDoesNotCrash() {
        // 'Zebra Notes' es la única app con 'Z'. Si se oculta, no debe existir sección 'Z'
        val hiddenPackages = setOf("com.zebra")
        val filteredApps = sampleApps.filter { it.packageName !in hiddenPackages }

        val prepared = prepareDrawerCategory(filteredApps)

        assertFalse(
            "La sección 'Z' no debe aparecer si su única app está oculta",
            prepared.baseData.availableSections.contains('Z')
        )

        // Búsqueda en sección inexistente no causa excepciones
        val searchZ = filterDrawerData(prepared, "Zebra")
        assertTrue(searchZ.items.isEmpty())
    }

    @Test
    fun testAZRecalculatesCorrectly() {
        val hiddenPackages = setOf("com.apple", "com.zebra")
        val filteredApps = sampleApps.filter { it.packageName !in hiddenPackages }

        val prepared = prepareDrawerCategory(filteredApps)
        val sections = prepared.baseData.availableSections

        // Inicialmente había 'A', 'B', 'C', 'S', 'Z'.
        // Al ocultar 'A' (Apple) y 'Z' (Zebra), deben quedar 'B', 'C', 'S'.
        assertEquals(listOf('B', 'C', 'S'), sections)
    }

    @Test
    fun testCrashFix01RemainsValidWithEmptyOrSingleApp() {
        // Escenario: Todas las apps ocultas
        val allHiddenPackages = sampleApps.mapNotNull { it.packageName }.toSet()
        val emptyApps = sampleApps.filter { it.packageName !in allHiddenPackages }

        val preparedEmpty = prepareDrawerCategory(emptyApps)
        assertTrue(preparedEmpty.baseData.items.isEmpty())
        assertTrue(preparedEmpty.baseData.availableSections.isEmpty())

        // Simular lógica segura de CRASH-FIX-01
        val items = preparedEmpty.baseData.items
        val firstChar = items.firstOrNull()?.sectionChar
        assertNull("firstOrNull debe retornar null con seguridad sin lanzar NoSuchElementException", firstChar)

        // Escenario: 1 sola app visible
        val oneApp = sampleApps.take(1)
        val preparedOne = prepareDrawerCategory(oneApp)
        val oneItems = preparedOne.baseData.items
        assertEquals(2, oneItems.size) // 1 Header + 1 AppRow
        val safeChar = oneItems.getOrNull(0)?.sectionChar
        assertEquals('A', safeChar)
    }
}
