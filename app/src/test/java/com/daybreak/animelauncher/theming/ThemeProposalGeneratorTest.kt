package com.daybreak.animelauncher.theming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias exhaustiva para [ThemeProposalGenerator] (Fase 2C).
 *
 * Cubre los 12 requisitos estrictos definidos en la especificación de Adaptive Wallpaper Theming:
 * 1. Palette colorida -> múltiples propuestas válidas.
 * 2. Palette oscura -> texto claro y contraste válido.
 * 3. Palette clara -> texto oscuro y contraste válido.
 * 4. Palette monocromática -> no crashea y reduce cantidad si es necesario.
 * 5. Vibrant ausente -> fallback válido.
 * 6. Muted ausente -> fallback válido.
 * 7. ThemePalette.DEFAULT -> al menos una propuesta válida.
 * 8. Resultados deterministas.
 * 9. No existen propuestas duplicadas.
 * 10. Todas las propuestas cumplen contraste WCAG 2.1.
 * 11. Todos los tokens contienen colores ARGB válidos.
 * 12. Generador no depende de Compose.
 */
class ThemeProposalGeneratorTest {

    private val colorfulPalette = ThemePalette(
        dominantColor = 0xFF4A148C.toInt(), // Púrpura profundo
        vibrantColor = 0xFF00E5FF.toInt(),  // Cyan Neón
        mutedColor = 0xFFB39DDB.toInt(),    // Lavanda atenuado
        isDark = true,
        averageLuminance = 0.18f
    )

    private val darkPalette = ThemePalette(
        dominantColor = 0xFF0A0E17.toInt(), // Azul marino casi negro
        vibrantColor = 0xFF1F293D.toInt(),  // Azul noche
        mutedColor = 0xFF141A26.toInt(),    // Gris pizarra muy oscuro
        isDark = true,
        averageLuminance = 0.02f
    )

    private val lightPalette = ThemePalette(
        dominantColor = 0xFFF0F4F8.toInt(), // Blanco/hielo suave
        vibrantColor = 0xFFE1BEE7.toInt(),  // Rosa pastel suave
        mutedColor = 0xFFCFD8DC.toInt(),    // Gris perla
        isDark = false,
        averageLuminance = 0.90f
    )

    private val monochromePalette = ThemePalette(
        dominantColor = 0xFF555555.toInt(), // Gris neutro idéntico
        vibrantColor = 0xFF555555.toInt(),
        mutedColor = 0xFF555555.toInt(),
        isDark = true,
        averageLuminance = 0.22f
    )

    @Test
    fun `1 palette colorida produce multiples propuestas validas y distintas`() {
        val proposals = ThemeProposalGenerator.generateProposals(colorfulPalette)

        assertTrue("Debe generar entre 2 y 4 propuestas para paleta colorida", proposals.size in 2..4)

        val ids = proposals.map { it.id }
        assertEquals("Los IDs deben ser únicos", ids.distinct().size, ids.size)

        // Verificar que los órdenes sean correlativos 1..N
        proposals.forEachIndexed { index, proposal ->
            assertEquals("El orden debe ser consecutivo", index + 1, proposal.order)
            assertFalse("Label no debe estar vacío", proposal.label.isBlank())
        }
    }

    @Test
    fun `2 palette oscura asegura texto claro y contraste valido`() {
        val proposals = ThemeProposalGenerator.generateProposals(darkPalette)
        assertTrue("Debe producir al menos una propuesta", proposals.isNotEmpty())

        for (proposal in proposals) {
            assertTrue("La propuesta debe ser oscura", proposal.isDark)
            val tokens = proposal.proposedTokens

            // El texto primario debe ser claro (luminancia alta)
            val textPrimaryLum = ThemeProposalGenerator.calculateRelativeLuminance(tokens.textPrimaryColor)
            assertTrue("Texto primario debe ser claro en esquema oscuro (lum: $textPrimaryLum)", textPrimaryLum > 0.70f)

            // Validar umbrales mínimos de contraste
            val textPrimaryRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.textPrimaryColor, tokens.surfaceColor)
            val textSecondaryRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.textSecondaryColor, tokens.surfaceColor)
            val accentRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.accentColor, tokens.surfaceColor)

            assertTrue("TextPrimary contraste >= 4.5:1 (obtenido: $textPrimaryRatio)", textPrimaryRatio >= 4.5f)
            assertTrue("TextSecondary contraste >= 3:1 (obtenido: $textSecondaryRatio)", textSecondaryRatio >= 3.0f)
            assertTrue("Accent contraste >= 3:1 (obtenido: $accentRatio)", accentRatio >= 3.0f)
        }
    }

    @Test
    fun `3 palette clara asegura texto oscuro y contraste valido`() {
        val proposals = ThemeProposalGenerator.generateProposals(lightPalette)
        assertTrue("Debe producir al menos una propuesta", proposals.isNotEmpty())

        for (proposal in proposals) {
            assertFalse("La propuesta debe ser clara", proposal.isDark)
            val tokens = proposal.proposedTokens

            // El texto primario debe ser oscuro (luminancia baja)
            val textPrimaryLum = ThemeProposalGenerator.calculateRelativeLuminance(tokens.textPrimaryColor)
            assertTrue("Texto primario debe ser oscuro en esquema claro (lum: $textPrimaryLum)", textPrimaryLum < 0.25f)

            // Validar umbrales de contraste
            val textPrimaryRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.textPrimaryColor, tokens.surfaceColor)
            val textSecondaryRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.textSecondaryColor, tokens.surfaceColor)
            val accentRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.accentColor, tokens.surfaceColor)

            assertTrue("TextPrimary contraste >= 4.5:1 (obtenido: $textPrimaryRatio)", textPrimaryRatio >= 4.5f)
            assertTrue("TextSecondary contraste >= 3:1 (obtenido: $textSecondaryRatio)", textSecondaryRatio >= 3.0f)
            assertTrue("Accent contraste >= 3:1 (obtenido: $accentRatio)", accentRatio >= 3.0f)
        }
    }

    @Test
    fun `4 palette monocromatica no crashea y reduce cantidad sin duplicados`() {
        val proposals = ThemeProposalGenerator.generateProposals(monochromePalette)

        assertTrue("Debe generar al menos 1 propuesta", proposals.isNotEmpty())
        assertTrue("Debe reducir cantidad al no haber variedad (obtenido: ${proposals.size})", proposals.size < 3)

        // Verificar consistencia de orden
        proposals.forEachIndexed { index, proposal ->
            assertEquals(index + 1, proposal.order)
        }
    }

    @Test
    fun `5 vibrant ausente utiliza fallback valido sin crashear`() {
        val missingVibrant = ThemePalette(
            dominantColor = 0xFF283593.toInt(),
            vibrantColor = 0, // Ausente / transparente
            mutedColor = 0xFF5C6BC0.toInt(),
            isDark = true,
            averageLuminance = 0.20f
        )

        val proposals = ThemeProposalGenerator.generateProposals(missingVibrant)
        assertTrue("Debe generar propuestas válidas con vibrant ausente", proposals.isNotEmpty())

        for (p in proposals) {
            assertNotEquals("No debe tener color 0", 0, p.proposedTokens.accentColor)
            assertEquals("Alpha debe ser 0xFF", 0xFF, (p.proposedTokens.accentColor ushr 24) and 0xFF)
        }
    }

    @Test
    fun `6 muted ausente utiliza fallback valido sin crashear`() {
        val missingMuted = ThemePalette(
            dominantColor = 0xFF00695C.toInt(),
            vibrantColor = 0xFF64FFDA.toInt(),
            mutedColor = 0, // Ausente / transparente
            isDark = true,
            averageLuminance = 0.25f
        )

        val proposals = ThemeProposalGenerator.generateProposals(missingMuted)
        assertTrue("Debe generar propuestas válidas con muted ausente", proposals.isNotEmpty())

        for (p in proposals) {
            assertNotEquals("No debe tener color 0", 0, p.proposedTokens.accentColor)
            assertEquals("Alpha debe ser 0xFF", 0xFF, (p.proposedTokens.accentColor ushr 24) and 0xFF)
        }
    }

    @Test
    fun `7 ThemePalette DEFAULT produce al menos una propuesta valida`() {
        val proposals = ThemeProposalGenerator.generateProposals(ThemePalette.DEFAULT)

        assertTrue("ThemePalette.DEFAULT debe generar al menos 1 propuesta", proposals.isNotEmpty())
        assertTrue("Máximo 4 propuestas", proposals.size <= 4)

        for (proposal in proposals) {
            val tokens = proposal.proposedTokens
            val primaryRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.textPrimaryColor, tokens.surfaceColor)
            assertTrue("Contraste textPrimary >= 4.5:1", primaryRatio >= 4.5f)
        }
    }

    @Test
    fun `8 resultados son 100 por ciento deterministas`() {
        val run1 = ThemeProposalGenerator.generateProposals(colorfulPalette)
        val run2 = ThemeProposalGenerator.generateProposals(colorfulPalette)

        assertEquals("Misma cantidad de propuestas", run1.size, run2.size)
        for (i in run1.indices) {
            assertEquals("ID idéntico en iteración $i", run1[i].id, run2[i].id)
            assertEquals("Tokens idénticos en iteración $i", run1[i].proposedTokens, run2[i].proposedTokens)
            assertEquals("isDark idéntico en iteración $i", run1[i].isDark, run2[i].isDark)
            assertEquals("Label idéntico en iteración $i", run1[i].label, run2[i].label)
            assertEquals("Order idéntico en iteración $i", run1[i].order, run2[i].order)
        }
    }

    @Test
    fun `9 no existen propuestas duplicadas perceptualmente`() {
        val palettes = listOf(colorfulPalette, darkPalette, lightPalette, monochromePalette, ThemePalette.DEFAULT)

        for (palette in palettes) {
            val proposals = ThemeProposalGenerator.generateProposals(palette)
            for (i in proposals.indices) {
                for (j in (i + 1) until proposals.size) {
                    val t1 = proposals[i].proposedTokens
                    val t2 = proposals[j].proposedTokens

                    // Comprobar que no sean prácticamente idénticas en accent y surface
                    val accentDiff = rgbDistance(t1.accentColor, t2.accentColor)
                    val surfaceDiff = rgbDistance(t1.surfaceColor, t2.surfaceColor)
                    val borderDiff = rgbDistance(t1.borderColor, t2.borderColor)

                    val isDuplicate = accentDiff < 30 && surfaceDiff < 25 && borderDiff < 30
                    assertFalse(
                        "Las propuestas ${proposals[i].id} y ${proposals[j].id} son indistinguibles (accentDiff=$accentDiff, surfaceDiff=$surfaceDiff)",
                        isDuplicate
                    )
                }
            }
        }
    }

    @Test
    fun `10 todas las propuestas de cualquier paleta cumplen minimos estrictos de contraste`() {
        val extremePalettes = listOf(
            colorfulPalette,
            darkPalette,
            lightPalette,
            monochromePalette,
            ThemePalette.DEFAULT,
            // Extremo negro puro
            ThemePalette(0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), true, 0.0f),
            // Extremo blanco puro
            ThemePalette(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), false, 1.0f),
            // Extremo amarillo saturado (luminancia alta con color)
            ThemePalette(0xFFFFEB3B.toInt(), 0xFFFFD700.toInt(), 0xFFFBC02D.toInt(), false, 0.85f),
            // Extremo azul oscuro desaturado
            ThemePalette(0xFF0D1B2A.toInt(), 0xFF1B263B.toInt(), 0xFF415A77.toInt(), true, 0.04f)
        )

        for (palette in extremePalettes) {
            val proposals = ThemeProposalGenerator.generateProposals(palette)
            assertTrue("Debe existir al menos una propuesta incluso para extremos", proposals.isNotEmpty())

            for (proposal in proposals) {
                val tokens = proposal.proposedTokens
                val textPrimaryRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.textPrimaryColor, tokens.surfaceColor)
                val textSecondaryRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.textSecondaryColor, tokens.surfaceColor)
                val accentRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.accentColor, tokens.surfaceColor)
                val borderRatio = ThemeProposalGenerator.calculateContrastRatio(tokens.borderColor, tokens.surfaceColor)

                assertTrue(
                    "Propuesta '${proposal.id}': textPrimary contraste >= 4.5:1 (obtenido: $textPrimaryRatio)",
                    textPrimaryRatio >= 4.5f
                )
                assertTrue(
                    "Propuesta '${proposal.id}': textSecondary contraste >= 3.0:1 (obtenido: $textSecondaryRatio)",
                    textSecondaryRatio >= 3.0f
                )
                assertTrue(
                    "Propuesta '${proposal.id}': accent contraste >= 3.0:1 (obtenido: $accentRatio)",
                    accentRatio >= 3.0f
                )
                assertTrue(
                    "Propuesta '${proposal.id}': border contraste >= 1.5:1 (obtenido: $borderRatio)",
                    borderRatio >= 1.5f
                )
            }
        }
    }

    @Test
    fun `11 todos los tokens contienen colores ARGB opacos y validos de 32 bits`() {
        val palettes = listOf(colorfulPalette, darkPalette, lightPalette, ThemePalette.DEFAULT)

        for (palette in palettes) {
            val proposals = ThemeProposalGenerator.generateProposals(palette)
            for (proposal in proposals) {
                val tokens = proposal.proposedTokens
                val colors = listOf(
                    "accentColor" to tokens.accentColor,
                    "surfaceColor" to tokens.surfaceColor,
                    "textPrimaryColor" to tokens.textPrimaryColor,
                    "textSecondaryColor" to tokens.textSecondaryColor,
                    "borderColor" to tokens.borderColor
                )

                for ((name, color) in colors) {
                    val alpha = (color ushr 24) and 0xFF
                    assertEquals("El token '$name' de '${proposal.id}' debe ser 100% opaco (alpha 0xFF)", 0xFF, alpha)
                }
            }
        }
    }

    @Test
    fun `12 ThemeProposalGenerator no depende de Compose ni del framework de UI`() {
        val clazz = ThemeProposalGenerator::class.java

        for (field in clazz.declaredFields) {
            val typeName = field.type.name
            assertFalse(
                "El campo '${field.name}' tiene tipo '$typeName' de Compose",
                typeName.contains("androidx.compose")
            )
        }

        for (method in clazz.declaredMethods) {
            val returnTypeName = method.returnType.name
            assertFalse(
                "El método '${method.name}' retorna tipo '$returnTypeName' de Compose",
                returnTypeName.contains("androidx.compose")
            )
            for (paramType in method.parameterTypes) {
                assertFalse(
                    "El método '${method.name}' recibe parámetro '${paramType.name}' de Compose",
                    paramType.name.contains("androidx.compose")
                )
            }
        }
    }

    private fun rgbDistance(c1: Int, c2: Int): Int {
        val r1 = (c1 ushr 16) and 0xFF
        val g1 = (c1 ushr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 ushr 16) and 0xFF
        val g2 = (c2 ushr 8) and 0xFF
        val b2 = c2 and 0xFF

        return kotlin.math.abs(r1 - r2) + kotlin.math.abs(g1 - g2) + kotlin.math.abs(b1 - b2)
    }
}
