package com.daybreak.animelauncher.theming

import androidx.annotation.ColorInt

/**
 * Agregador puro, determinista y matemáticamente robusto de paletas de color
 * extraídas de múltiples frames de video (Adaptive Wallpaper Theming - Fase 2E).
 *
 * Responsabilidad estricta:
 * - Recibir una lista de [ThemePalette] (hasta 3 frames representativos).
 * - Seleccionar colores representativos centrales (medoide) sin promediar canales RGB
 *   ni inventar colores artificiales.
 * - Manejar listas vacías, paletas únicas, 2 o 3 paletas de forma determinista.
 * - Combinar la luminancia relativa promedio de manera coherente.
 * - Cero dependencias con Compose, vistas, ViewModels o almacenamiento persistente.
 */
object ThemePaletteAggregator {

    /**
     * Agrega una colección de [palettes] correspondientes a frames de video en una única [ThemePalette].
     *
     * @param palettes Lista de paletas extraídas de los frames analizados.
     * @return [ThemePalette] consolidada y representativa de la escena general del video.
     */
    fun aggregate(palettes: List<ThemePalette>): ThemePalette {
        if (palettes.isEmpty()) {
            return ThemePalette.DEFAULT
        }

        if (palettes.size == 1) {
            return palettes[0]
        }

        // 1. Selección de colores medoides para cada campo cromático
        val dominantMedoid = selectMedoidColor(palettes.map { it.dominantColor })
        val vibrantMedoid = selectMedoidColor(palettes.map { it.vibrantColor })
        val mutedMedoid = selectMedoidColor(palettes.map { it.mutedColor })

        // 2. Cálculo determinista de luminancia promedio entre los frames analizados
        val avgLuminance = palettes.map { it.averageLuminance }
            .average()
            .toFloat()
            .coerceIn(0.0f, 1.0f)

        // 3. Clasificación coherente según la luminancia agregada
        val isDark = avgLuminance < 0.5f

        return ThemePalette(
            dominantColor = dominantMedoid,
            vibrantColor = vibrantMedoid,
            mutedColor = mutedMedoid,
            isDark = isDark,
            averageLuminance = avgLuminance
        )
    }

    /**
     * Selecciona el color medoide (representante central geométrico) de una lista de [candidates].
     *
     * El medoide es el candidato que minimiza la suma total de distancias cuadráticas Euclidianas
     * respecto a todos los demás candidatos en el espacio RGB:
     *   min_i \sum_{j != i} distanceSq(c_i, c_j)
     *
     * Esto asegura que:
     * - Ningún outlier (ej. frame de transición o flash blanco/negro) sea seleccionado.
     * - El color elegido sea un color real extraído directamente de la escena, sin mezcla artificial.
     * - En caso de empate de distancias, se desambigua de forma determinista por el índice más temprano.
     */
    internal fun selectMedoidColor(@ColorInt candidates: List<Int>): Int {
        if (candidates.isEmpty()) return ThemePalette.DEFAULT.dominantColor
        if (candidates.size == 1) return candidates[0]
        if (candidates.size == 2) {
            // En una lista de 2 candidatos (dist(c0, c1) == dist(c1, c0)),
            // se selecciona deterministamente el primer elemento.
            return candidates[0]
        }

        var bestCandidate = candidates[0]
        var minTotalDistance = Long.MAX_VALUE

        for (i in candidates.indices) {
            val c1 = candidates[i]
            var totalDistance = 0L
            for (j in candidates.indices) {
                if (i != j) {
                    totalDistance += colorDistanceSq(c1, candidates[j])
                }
            }
            if (totalDistance < minTotalDistance) {
                minTotalDistance = totalDistance
                bestCandidate = c1
            }
        }

        return bestCandidate
    }

    /**
     * Distancia Euclidiana cuadrática entre dos colores en el espacio RGB (sin raíz cuadrada para mayor rendimiento).
     */
    internal fun colorDistanceSq(@ColorInt c1: Int, @ColorInt c2: Int): Long {
        val r1 = (c1 ushr 16) and 0xFF
        val g1 = (c1 ushr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 ushr 16) and 0xFF
        val g2 = (c2 ushr 8) and 0xFF
        val b2 = c2 and 0xFF

        val dr = (r1 - r2).toLong()
        val dg = (g1 - g2).toLong()
        val db = (b1 - b2).toLong()

        return (dr * dr) + (dg * dg) + (db * db)
    }
}
