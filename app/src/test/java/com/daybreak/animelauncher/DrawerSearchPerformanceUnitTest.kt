package com.daybreak.animelauncher

import com.daybreak.animelauncher.ui.screens.DrawerData
import com.daybreak.animelauncher.ui.screens.DrawerListItem
import com.daybreak.animelauncher.ui.screens.buildDrawerData
import com.daybreak.animelauncher.ui.screens.filterDrawerData
import com.daybreak.animelauncher.ui.screens.prepareDrawerCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de validación funcional estricta para la optimización de Search en el App Drawer (P2-01).
 * Verifica que [prepareDrawerCategory] y [filterDrawerData] produzcan exactamente el mismo resultado
 * (mismos elementos, mismo orden, mismos encabezados, mismo sectionIndexMap y mismas availableSections)
 * que el algoritmo canónico original para todos los escenarios de búsqueda.
 */
class DrawerSearchPerformanceUnitTest {

    private val sampleApps = listOf(
        AppShortcut("1", "Calculadora", "com.example.calc"),
        AppShortcut("2", "Cámara", "com.example.camera"),
        AppShortcut("3", "Chrome", "com.android.chrome"),
        AppShortcut("4", "Álbum de Fotos", "com.example.album"),
        AppShortcut("5", "Éxito Mobile", "com.example.exito"),
        AppShortcut("6", "Índice", "com.example.indice"),
        AppShortcut("7", "Ópera Mini", "com.opera.mini"),
        AppShortcut("8", "Útil Tools", "com.example.util"),
        AppShortcut("9", "WhatsApp", "com.whatsapp"),
        AppShortcut("10", "YouTube", "com.google.android.youtube"),
        AppShortcut("11", "7-Zip", "org.sevenzip"),
        AppShortcut("12", "@Voice Aloud Reader", "com.hyperionics.avar"),
        AppShortcut("13", "Reloj", "com.google.android.deskclock"),
        AppShortcut("14", "Radio FM", "com.example.radio"),
        AppShortcut("15", "Gmail", "com.google.android.gm"),
        AppShortcut("16", "Google Maps", "com.google.android.apps.maps")
    )

    private fun assertDrawerDataEquals(expected: DrawerData, actual: DrawerData) {
        assertEquals("Available sections mismatch", expected.availableSections, actual.availableSections)
        assertEquals("Section index map mismatch", expected.sectionIndexMap, actual.sectionIndexMap)
        assertEquals("Items count mismatch", expected.items.size, actual.items.size)

        for (i in expected.items.indices) {
            val expItem = expected.items[i]
            val actItem = actual.items[i]
            when {
                expItem is DrawerListItem.Header && actItem is DrawerListItem.Header -> {
                    assertEquals("Header title mismatch at $i", expItem.title, actItem.title)
                    assertEquals("Header sectionChar mismatch at $i", expItem.sectionChar, actItem.sectionChar)
                }
                expItem is DrawerListItem.AppRow && actItem is DrawerListItem.AppRow -> {
                    assertEquals("AppRow app mismatch at $i", expItem.app, actItem.app)
                    assertEquals("AppRow sectionChar mismatch at $i", expItem.sectionChar, actItem.sectionChar)
                }
                else -> {
                    throw AssertionError("Item type mismatch at index $i: expected $expItem, got $actItem")
                }
            }
        }
    }

    @Test
    fun testEmptySearchQueryReturnsBaseData() {
        val prepared = prepareDrawerCategory(sampleApps)
        val expected = buildDrawerData(sampleApps)

        val actualEmpty = filterDrawerData(prepared, "")
        assertDrawerDataEquals(expected, actualEmpty)

        val actualBlank = filterDrawerData(prepared, "   ")
        assertDrawerDataEquals(expected, actualBlank)
    }

    @Test
    fun testSingleLetterSearch() {
        val prepared = prepareDrawerCategory(sampleApps)
        val query = "c"
        val expectedFilteredApps = sampleApps.filter { it.name.contains(query, ignoreCase = true) }
        val expected = buildDrawerData(expectedFilteredApps)

        val actual = filterDrawerData(prepared, query)
        assertDrawerDataEquals(expected, actual)
    }

    @Test
    fun testMultiLetterSearch() {
        val prepared = prepareDrawerCategory(sampleApps)
        val query = "calc"
        val expectedFilteredApps = sampleApps.filter { it.name.contains(query, ignoreCase = true) }
        val expected = buildDrawerData(expectedFilteredApps)

        val actual = filterDrawerData(prepared, query)
        assertDrawerDataEquals(expected, actual)
    }

    @Test
    fun testExactMatchSearch() {
        val prepared = prepareDrawerCategory(sampleApps)
        val query = "Calculadora"
        val expectedFilteredApps = sampleApps.filter { it.name.contains(query, ignoreCase = true) }
        val expected = buildDrawerData(expectedFilteredApps)

        val actual = filterDrawerData(prepared, query)
        assertDrawerDataEquals(expected, actual)
    }

    @Test
    fun testCaseInsensitiveMatching() {
        val prepared = prepareDrawerCategory(sampleApps)

        val lowerResult = filterDrawerData(prepared, "chrome")
        val upperResult = filterDrawerData(prepared, "CHROME")
        val mixedResult = filterDrawerData(prepared, "ChRoMe")

        assertDrawerDataEquals(lowerResult, upperResult)
        assertDrawerDataEquals(lowerResult, mixedResult)
    }

    @Test
    fun testNoResultsSearch() {
        val prepared = prepareDrawerCategory(sampleApps)
        val actual = filterDrawerData(prepared, "xyz999nonexistent")

        assertTrue("Items should be empty", actual.items.isEmpty())
        assertTrue("Section index map should be empty", actual.sectionIndexMap.isEmpty())
        assertTrue("Available sections should be empty", actual.availableSections.isEmpty())
    }

    @Test
    fun testAccentedCharactersAndSymbols() {
        val prepared = prepareDrawerCategory(sampleApps)

        // Búsqueda de apps con acento
        val query = "álbum"
        val expectedFilteredApps = sampleApps.filter { it.name.contains(query, ignoreCase = true) }
        val expected = buildDrawerData(expectedFilteredApps)
        val actual = filterDrawerData(prepared, query)
        assertDrawerDataEquals(expected, actual)

        // Búsqueda de apps con símbolos
        val querySymbol = "7-zip"
        val expectedSymbolApps = sampleApps.filter { it.name.contains(querySymbol, ignoreCase = true) }
        val expectedSymbol = buildDrawerData(expectedSymbolApps)
        val actualSymbol = filterDrawerData(prepared, querySymbol)
        assertDrawerDataEquals(expectedSymbol, actualSymbol)
    }

    @Test
    fun testMultipleMatchesAcrossDifferentSections() {
        val prepared = prepareDrawerCategory(sampleApps)
        // La letra 'a' aparece en Calculadora (C), Cámara (C), Álbum (A), WhatsApp (W), Radio (R), Gmail (G), Google Maps (G), @Voice (#)
        val query = "a"
        val expectedFilteredApps = sampleApps.filter { it.name.contains(query, ignoreCase = true) }
        val expected = buildDrawerData(expectedFilteredApps)

        val actual = filterDrawerData(prepared, query)
        assertDrawerDataEquals(expected, actual)
    }

    @Test
    fun testEmptyAppsList() {
        val emptyPrepared = prepareDrawerCategory(emptyList())
        val actual = filterDrawerData(emptyPrepared, "calc")

        assertTrue(actual.items.isEmpty())
        assertTrue(actual.sectionIndexMap.isEmpty())
        assertTrue(actual.availableSections.isEmpty())
    }

    @Test
    fun testCategorySubset() {
        val subset = sampleApps.filter { it.packageName?.startsWith("com.google") == true }
        val prepared = prepareDrawerCategory(subset)
        val expected = buildDrawerData(subset)

        val actual = filterDrawerData(prepared, "")
        assertDrawerDataEquals(expected, actual)

        val query = "map"
        val expectedFiltered = buildDrawerData(subset.filter { it.name.contains(query, ignoreCase = true) })
        val actualFiltered = filterDrawerData(prepared, query)
        assertDrawerDataEquals(expectedFiltered, actualFiltered)
    }
}
