package com.daybreak.animelauncher.theming

import com.daybreak.animelauncher.AdvancedStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de aislamiento para [LauncherThemeTokens].
 *
 * Valida:
 * 1. Los valores por defecto reproducen con exactitud los colores actuales de NovaLauncher.
 * 2. La conversión desde [AdvancedStyleConfig] es determinista y consistente.
 * 3. Los tokens resisten valores inválidos o corruptos sin excepciones.
 * 4. El modelo puro está completamente desacoplado de Compose.
 */
class LauncherThemeTokensTest {

    @Test
    fun `1 defaults reproducen con exactitud los colores visuales de NovaLauncher`() {
        val defaultTokens = LauncherThemeTokens.DEFAULT

        assertEquals(0xFF00F0FF.toInt(), defaultTokens.accentColor)
        assertEquals(0xFF08080C.toInt(), defaultTokens.surfaceColor)
        assertEquals(0xFFFFFFFF.toInt(), defaultTokens.textPrimaryColor)
        assertEquals(0xFFCCCCCC.toInt(), defaultTokens.textSecondaryColor)
        assertEquals(0xFF00F0FF.toInt(), defaultTokens.borderColor)
    }

    @Test
    fun `2 conversion desde AdvancedStyleConfig es determinista`() {
        val config = AdvancedStyleConfig(accentColor = "#FF5722")

        val tokens1 = LauncherThemeTokens.fromAdvancedStyleConfig(config)
        val tokens2 = LauncherThemeTokens.fromAdvancedStyleConfig(config)

        assertEquals(0xFFFF5722.toInt(), tokens1.accentColor)
        assertEquals(0xFFFF5722.toInt(), tokens1.borderColor)
        assertEquals(tokens1, tokens2)
        assertEquals(tokens1.hashCode(), tokens2.hashCode())
    }

    @Test
    fun `3 tokens manejan valores corruptos o invalidos con fallback robusto`() {
        val testCases = listOf(
            "",
            "   ",
            "#",
            "not-a-hex",
            "#GG1122",
            "#12345",
            "#1234567"
        )

        for (invalid in testCases) {
            val config = AdvancedStyleConfig(accentColor = invalid)
            val tokens = LauncherThemeTokens.fromAdvancedStyleConfig(config)
            assertEquals("Fallback esperado para '$invalid'", LauncherThemeTokens.DEFAULT.accentColor, tokens.accentColor)
            assertEquals("Fallback esperado para '$invalid'", LauncherThemeTokens.DEFAULT.borderColor, tokens.borderColor)
        }

        // Casos válidos con y sin almohadilla
        val parsedNoHash = LauncherThemeTokens.parseHexColor("00FF88", LauncherThemeTokens.DEFAULT.accentColor)
        assertEquals(0xFF00FF88.toInt(), parsedNoHash)

        val parsed8Chars = LauncherThemeTokens.parseHexColor("#80112233", LauncherThemeTokens.DEFAULT.accentColor)
        assertEquals(0x80112233.toInt(), parsed8Chars)
    }

    @Test
    fun `4 el modelo puro LauncherThemeTokens no tiene dependencias de Compose`() {
        val clazz = LauncherThemeTokens::class.java

        // Verificar que ningún campo pertenece al paquete androidx.compose
        for (field in clazz.declaredFields) {
            val typeName = field.type.name
            assertFalse(
                "El campo '${field.name}' tiene tipo '$typeName' de Compose",
                typeName.contains("androidx.compose")
            )
        }

        // Verificar que ningún método público tiene parámetros ni retorno de Compose
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
}
