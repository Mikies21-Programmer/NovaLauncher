package com.daybreak.animelauncher.ui.screens

import com.daybreak.animelauncher.AppShortcut
import java.text.Normalizer

/**
 * Elementos de la lista plana para LazyColumn en Nova Drawer — Hybrid Stream.
 */
sealed interface DrawerListItem {
    val sectionChar: Char
    data class Header(val title: String, override val sectionChar: Char = title.firstOrNull() ?: '#') : DrawerListItem
    data class AppRow(val app: AppShortcut, override val sectionChar: Char) : DrawerListItem
}

/**
 * Contenedor de datos precalculados para el Drawer:
 * - [items]: lista plana para el LazyColumn (headers y app rows).
 * - [sectionIndexMap]: mapa de sección (Char) a índice real en [items].
 * - [availableSections]: lista ordenada de caracteres de sección disponibles en la vista actual.
 */
data class DrawerData(
    val items: List<DrawerListItem> = emptyList(),
    val sectionIndexMap: Map<Char, Int> = emptyMap(),
    val availableSections: List<Char> = emptyList()
)

/**
 * Normaliza la primera letra del nombre de una aplicación para agruparla en secciones alfabéticas.
 * - Trata caracteres acentuados como su letra base (Á -> A, É -> E, etc.).
 * - Si el primer carácter no es una letra alfabética A-Z, se agrupa bajo "#".
 */
fun normalizeSectionChar(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "#"
    val firstChar = trimmed[0].uppercaseChar()
    val baseChar = when (firstChar) {
        'Á', 'À', 'Â', 'Ã', 'Ä' -> 'A'
        'É', 'È', 'Ê', 'Ë' -> 'E'
        'Í', 'Ì', 'Î', 'Ï' -> 'I'
        'Ó', 'Ò', 'Ô', 'Õ', 'Ö' -> 'O'
        'Ú', 'Ù', 'Û', 'Ü' -> 'U'
        else -> {
            val normalized = Normalizer.normalize(firstChar.toString(), Normalizer.Form.NFD)
            normalized.firstOrNull()?.uppercaseChar() ?: firstChar
        }
    }
    return if (baseChar in 'A'..'Z') baseChar.toString() else "#"
}

/**
 * Transforma y agrupa la lista de aplicaciones precalculando:
 * - Lista plana ordenada alfabéticamente (case-insensitive) con secciones A-Z y '#' al final si existe.
 * - Mapa exacto de Char -> índice real en la lista.
 * - Lista de secciones presentes en la categoría.
 */
fun buildDrawerData(apps: List<AppShortcut>): DrawerData {
    if (apps.isEmpty()) return DrawerData()

    val grouped = apps.groupBy { normalizeSectionChar(it.name) }
    val sortedSections = grouped.keys.sortedWith { s1, s2 ->
        when {
            s1 == s2 -> 0
            s1 == "#" -> 1
            s2 == "#" -> -1
            else -> s1.compareTo(s2)
        }
    }

    val items = ArrayList<DrawerListItem>(apps.size + sortedSections.size)
    val indexMap = LinkedHashMap<Char, Int>(sortedSections.size)
    val sections = ArrayList<Char>(sortedSections.size)

    for (section in sortedSections) {
        val char = section[0]
        sections.add(char)
        indexMap[char] = items.size
        items.add(DrawerListItem.Header(section, char))
        val sectionApps = grouped[section]!!.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        for (app in sectionApps) {
            items.add(DrawerListItem.AppRow(app, char))
        }
    }
    return DrawerData(items = items, sectionIndexMap = indexMap, availableSections = sections)
}

/**
 * Función de compatibilidad para obtener únicamente la lista plana de DrawerListItem.
 */
fun buildDrawerListItems(apps: List<AppShortcut>): List<DrawerListItem> = buildDrawerData(apps).items
