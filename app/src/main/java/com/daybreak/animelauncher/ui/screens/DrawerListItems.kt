package com.daybreak.animelauncher.ui.screens

import com.daybreak.animelauncher.AppShortcut
import java.text.Normalizer

/**
 * Elementos de la lista plana para LazyColumn en Nova Drawer — Hybrid Stream.
 */
sealed interface DrawerListItem {
    data class Header(val title: String) : DrawerListItem
    data class AppRow(val app: AppShortcut) : DrawerListItem
}

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
 * Transforma y agrupa la lista de aplicaciones en una lista plana de [DrawerListItem]
 * (Header y AppRow) ordenadas alfabéticamente de forma case-insensitive.
 * Secciones alfabéticas A-Z primero, seguidas de '#' si existen aplicaciones con caracteres especiales/números.
 */
fun buildDrawerListItems(apps: List<AppShortcut>): List<DrawerListItem> {
    if (apps.isEmpty()) return emptyList()

    val grouped = apps.groupBy { normalizeSectionChar(it.name) }
    val sortedSections = grouped.keys.sortedWith { s1, s2 ->
        when {
            s1 == s2 -> 0
            s1 == "#" -> 1
            s2 == "#" -> -1
            else -> s1.compareTo(s2)
        }
    }

    val result = ArrayList<DrawerListItem>(apps.size + sortedSections.size)
    for (section in sortedSections) {
        result.add(DrawerListItem.Header(section))
        val sectionApps = grouped[section]!!.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        for (app in sectionApps) {
            result.add(DrawerListItem.AppRow(app))
        }
    }
    return result
}
