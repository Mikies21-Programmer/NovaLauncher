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
 * Fila de aplicación precalculada para evitar re-normalizaciones y re-instanciaciones durante búsquedas.
 */
class PreparedAppRow(
    val row: DrawerListItem.AppRow,
    val lowerName: String
)

/**
 * Sección precalculada del Drawer con su cabecera y lista de aplicaciones ya ordenadas alfabéticamente.
 */
class PreparedDrawerSection(
    val char: Char,
    val header: DrawerListItem.Header,
    val apps: List<PreparedAppRow>
)

/**
 * Contenedor de datos estables para una categoría del Drawer.
 * Contiene los datos completos [baseData] listos para cuando no hay búsqueda,
 * y las secciones preordenadas [sections] listas para un filtrado O(N) directo sin sorting ni grouping.
 */
class PreparedCategoryDrawer(
    val baseData: DrawerData,
    val sections: List<PreparedDrawerSection>
)

/**
 * Prepara y agrupa de forma estable la lista de aplicaciones de una categoría:
 * - Agrupa por sección (A-Z, #) ejecutando [normalizeSectionChar] una sola vez por app.
 * - Ordena secciones y aplicaciones alfabéticamente una sola vez.
 * - Prepara [PreparedCategoryDrawer] para permitir entrega instantánea sin búsqueda (O(1))
 *   y filtrado directo O(N) durante la escritura sin sorting ni normalizaciones adicionales.
 */
fun prepareDrawerCategory(apps: List<AppShortcut>): PreparedCategoryDrawer {
    if (apps.isEmpty()) {
        return PreparedCategoryDrawer(baseData = DrawerData(), sections = emptyList())
    }

    val grouped = apps.groupBy { normalizeSectionChar(it.name) }
    val sortedSections = grouped.keys.sortedWith { s1, s2 ->
        when {
            s1 == s2 -> 0
            s1 == "#" -> 1
            s2 == "#" -> -1
            else -> s1.compareTo(s2)
        }
    }

    val totalItemsCount = apps.size + sortedSections.size
    val items = ArrayList<DrawerListItem>(totalItemsCount)
    val indexMap = LinkedHashMap<Char, Int>(sortedSections.size)
    val sections = ArrayList<Char>(sortedSections.size)
    val preparedSections = ArrayList<PreparedDrawerSection>(sortedSections.size)

    for (section in sortedSections) {
        val char = section[0]
        sections.add(char)
        indexMap[char] = items.size
        val header = DrawerListItem.Header(section, char)
        items.add(header)

        val sectionApps = grouped[section]!!.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        val preparedRows = ArrayList<PreparedAppRow>(sectionApps.size)

        for (app in sectionApps) {
            val row = DrawerListItem.AppRow(app, char)
            items.add(row)
            preparedRows.add(PreparedAppRow(row = row, lowerName = app.name.lowercase()))
        }

        preparedSections.add(
            PreparedDrawerSection(
                char = char,
                header = header,
                apps = preparedRows
            )
        )
    }

    val baseData = DrawerData(items = items, sectionIndexMap = indexMap, availableSections = sections)
    return PreparedCategoryDrawer(baseData = baseData, sections = preparedSections)
}

/**
 * Filtra los datos precalculados de la categoría según la consulta de búsqueda.
 * - Si [searchQuery] está en blanco, retorna [prepared.baseData] inmediatamente en O(1).
 * - Si [searchQuery] contiene texto, realiza un filtrado lineal directo sobre las secciones
 *   y filas ya ordenadas, preservando el orden alfabético estricto sin ejecutar sorting,
 *   groupBy ni normalizaciones NFD.
 */
fun filterDrawerData(prepared: PreparedCategoryDrawer, searchQuery: String): DrawerData {
    if (searchQuery.isBlank()) {
        return prepared.baseData
    }

    val query = searchQuery.lowercase()
    val resultItems = ArrayList<DrawerListItem>()
    val resultIndexMap = LinkedHashMap<Char, Int>()
    val resultSections = ArrayList<Char>()

    val sections = prepared.sections
    for (s in 0 until sections.size) {
        val section = sections[s]
        val apps = section.apps
        var hasMatches = false
        val sectionStartIndex = resultItems.size

        for (a in 0 until apps.size) {
            val appRow = apps[a]
            if (appRow.lowerName.contains(query)) {
                if (!hasMatches) {
                    hasMatches = true
                    resultSections.add(section.char)
                    resultIndexMap[section.char] = sectionStartIndex
                    resultItems.add(section.header)
                }
                resultItems.add(appRow.row)
            }
        }
    }

    return DrawerData(
        items = resultItems,
        sectionIndexMap = resultIndexMap,
        availableSections = resultSections
    )
}

/**
 * Transforma y agrupa la lista de aplicaciones precalculando:
 * - Lista plana ordenada alfabéticamente (case-insensitive) con secciones A-Z y '#' al final si existe.
 * - Mapa exacto de Char -> índice real en la lista.
 * - Lista de secciones presentes en la categoría.
 */
fun buildDrawerData(apps: List<AppShortcut>): DrawerData = prepareDrawerCategory(apps).baseData

/**
 * Función de compatibilidad para obtener únicamente la lista plana de DrawerListItem.
 */
fun buildDrawerListItems(apps: List<AppShortcut>): List<DrawerListItem> = buildDrawerData(apps).items
