package com.daybreak.animelauncher.theming

import com.daybreak.animelauncher.AdvancedStyleConfig
import com.daybreak.animelauncher.normalizeLoadedStyle
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI-1 Subfase 8A: normalización defensiva y persistencia de la configuración de
 * estilo (adaptiveOverrides). Puros JVM, sin Android framework.
 *
 * Contrato verificado:
 * - El constructor no-arg de AdvancedStyleConfig preserva defaults de campos AUSENTES
 *   del JSON (JSON antiguo sin adaptiveOverrides → contenedor vacío, NO null).
 * - Gson SÍ puede dejar un campo no-nulo en null con un null EXPLÍCITO en el JSON o
 *   un nombre de enum corrupto → normalizeLoadedStyle debe recuperar estado seguro.
 * - null en una excepción individual = color automático; sobrevive a la persistencia.
 * - Un color explícito sobrevive a guardar y volver a cargar.
 * - La normalización no altera themeMode ni los demás valores preexistentes.
 */
class AdaptiveThemeSettingsTest {

    private val gson = Gson()

    // ── 1. JSON antiguo SIN adaptiveOverrides → estado seguro equivalente ──
    // La omisión del campo NO lo deja null (no-arg ctor); la normalización debe
    // mantener el contenedor vacío (todas las excepciones null = automático).
    @Test
    fun test01_JsonAntiguoSinAdaptiveOverrides_ContenedorVacioYValoresIntactos() {
        val oldJson = """{"themeMode":"AUTO","sidebarColor":"#07080D","clockColor":"#FF0000"}"""
        val loaded = gson.fromJson(oldJson, AdvancedStyleConfig::class.java)
        val safe = normalizeLoadedStyle(loaded)

        assertNotNull("contenedor de excepciones nunca null", safe.adaptiveOverrides)
        assertNull("clockColor auto", safe.adaptiveOverrides.clockColor)
        assertNull("batteryColor auto", safe.adaptiveOverrides.batteryColor)
        assertNull("messagesColor auto", safe.adaptiveOverrides.messagesColor)
        assertNull("dateColor auto", safe.adaptiveOverrides.dateColor)
        // los demás valores antiguos sobreviven intactos
        assertEquals(ThemeMode.AUTO, safe.themeMode)
        assertEquals("#07080D", safe.sidebarColor)
        assertEquals("#FF0000", safe.clockColor)
    }

    // ── 2. null EXPLÍCITO en el JSON → normalización recupera estado seguro ──
    // Vía real de exposición: prefs restauradas de backup / editadas a mano
    // (la Gson propia de la app nunca serializa nulls). Sin la línea defensiva
    // el contenedor queda null y el engine (applyOverrides) lanzaría NPE en
    // AUTO / AUTO_WITH_OVERRIDES.
    @Test
    fun test02_NullExplicitoEnJson_ContenedorNormalizado() {
        val hostileJson = """{"themeMode":"AUTO_WITH_OVERRIDES","adaptiveOverrides":null}"""
        val loaded = gson.fromJson(hostileJson, AdvancedStyleConfig::class.java)
        val safe = normalizeLoadedStyle(loaded)

        assertNotNull("null explícito debe normalizarse a contenedor vacío", safe.adaptiveOverrides)
        assertNull(safe.adaptiveOverrides.clockColor)
        assertNull(safe.adaptiveOverrides.batteryColor)
        assertNull(safe.adaptiveOverrides.messagesColor)
        assertNull(safe.adaptiveOverrides.dateColor)
        assertEquals(ThemeMode.AUTO_WITH_OVERRIDES, safe.themeMode)
    }

    // ── 3. Guardar y volver a cargar excepciones personalizadas ──
    @Test
    fun test03_RoundTripExcepcionesPersonalizadas() {
        val config = AdvancedStyleConfig().copy(
            themeMode = ThemeMode.AUTO_WITH_OVERRIDES,
            adaptiveOverrides = AdaptiveColorOverrides(
                clockColor = "#FF007F",
                batteryColor = "#00FF7F"
            )
        )
        val restored = normalizeLoadedStyle(
            gson.fromJson(gson.toJson(config), AdvancedStyleConfig::class.java)
        )

        assertEquals("#FF007F", restored.adaptiveOverrides.clockColor)
        assertEquals("#00FF7F", restored.adaptiveOverrides.batteryColor)
        assertNull("mensajes sigue automático", restored.adaptiveOverrides.messagesColor)
        assertNull("fecha sigue automática", restored.adaptiveOverrides.dateColor)
        assertEquals(ThemeMode.AUTO_WITH_OVERRIDES, restored.themeMode)
    }

    // ── 4. Resetear UNA excepción a null persiste como null (automático) ──
    @Test
    fun test04_ResetIndividualANull_SobrevivePersistencia() {
        val conExcepciones = AdvancedStyleConfig().copy(
            adaptiveOverrides = AdaptiveColorOverrides(
                clockColor = "#FF007F",
                batteryColor = "#00FF7F"
            )
        )
        // "Restablecer" el reloj: null = vuelve al color automático
        val reset = conExcepciones.copy(
            adaptiveOverrides = conExcepciones.adaptiveOverrides.copy(clockColor = null)
        )
        val restored = normalizeLoadedStyle(
            gson.fromJson(gson.toJson(reset), AdvancedStyleConfig::class.java)
        )

        assertNull("reloj vuelve a automático tras persistir", restored.adaptiveOverrides.clockColor)
        assertEquals("batería conserva su excepción", "#00FF7F", restored.adaptiveOverrides.batteryColor)
    }

    // ── 5. La normalización no altera el modo ni otros valores preexistentes ──
    @Test
    fun test05_NormalizacionPreservaModoLogoRamYDemasValores() {
        val json = """{"themeMode":"AUTO_WITH_OVERRIDES","logoMode":"TEXT_ONLY",
            "logoText":"NOVA","showRamWidget":true,"sidebarOpacity":0.5,
            "adaptiveOverrides":{"dateColor":"#123456"}}"""
        val safe = normalizeLoadedStyle(gson.fromJson(json, AdvancedStyleConfig::class.java))

        assertEquals(ThemeMode.AUTO_WITH_OVERRIDES, safe.themeMode)
        assertEquals(LogoMode.TEXT_ONLY, safe.logoMode)
        assertEquals("NOVA", safe.logoText)
        assertTrue(safe.showRamWidget)
        assertEquals(0.5f, safe.sidebarOpacity, 1e-6f)
        assertEquals("#123456", safe.adaptiveOverrides.dateColor)
        assertNull(safe.adaptiveOverrides.clockColor)

        // enum corrupto (nombre desconocido) → MANUAL, sin excepción
        val corrupto = normalizeLoadedStyle(
            gson.fromJson("""{"themeMode":"BASURA","sidebarColor":"#08080C"}""", AdvancedStyleConfig::class.java)
        )
        assertEquals(ThemeMode.MANUAL, corrupto.themeMode)
        assertEquals("#08080C", corrupto.sidebarColor)
    }

    // ── 6. Selección de los tres modos preserva configuraciones y excepciones ──
    @Test
    fun test06_SeleccionDeLosTresModos_PreservaConfiguracionesManualesYExcepciones() {
        val baseConfig = AdvancedStyleConfig().copy(
            themeMode = ThemeMode.MANUAL,
            clockColor = "#FF0000",
            sidebarColor = "#112233",
            adaptiveOverrides = AdaptiveColorOverrides(
                clockColor = "#FFAA00",
                messagesColor = "#00FFAA"
            )
        )

        // MANUAL -> AUTO
        val autoConfig = baseConfig.copy(themeMode = ThemeMode.AUTO)
        assertEquals(ThemeMode.AUTO, autoConfig.themeMode)
        assertEquals("#FF0000", autoConfig.clockColor)
        assertEquals("#112233", autoConfig.sidebarColor)
        assertEquals("#FFAA00", autoConfig.adaptiveOverrides.clockColor)
        assertEquals("#00FFAA", autoConfig.adaptiveOverrides.messagesColor)

        // AUTO -> AUTO_WITH_OVERRIDES
        val overridesConfig = autoConfig.copy(themeMode = ThemeMode.AUTO_WITH_OVERRIDES)
        assertEquals(ThemeMode.AUTO_WITH_OVERRIDES, overridesConfig.themeMode)
        assertEquals("#FF0000", overridesConfig.clockColor)
        assertEquals("#FFAA00", overridesConfig.adaptiveOverrides.clockColor)
        assertEquals("#00FFAA", overridesConfig.adaptiveOverrides.messagesColor)

        // AUTO_WITH_OVERRIDES -> MANUAL
        val backToManual = overridesConfig.copy(themeMode = ThemeMode.MANUAL)
        assertEquals(ThemeMode.MANUAL, backToManual.themeMode)
        assertEquals("#FF0000", backToManual.clockColor)
        assertEquals("#FFAA00", backToManual.adaptiveOverrides.clockColor)
        assertEquals("#00FFAA", backToManual.adaptiveOverrides.messagesColor)
    }

    // ── 7. Asignación de los cuatro campos de excepciones ──
    @Test
    fun test07_AsignacionDeLasCuatroExcepciones_InmutableYCompleta() {
        val base = AdvancedStyleConfig()
        val allFour = base.copy(
            adaptiveOverrides = AdaptiveColorOverrides(
                clockColor = "#FF1111",
                batteryColor = "#22FF22",
                messagesColor = "#3333FF",
                dateColor = "#FFFF44"
            )
        )

        assertEquals("#FF1111", allFour.adaptiveOverrides.clockColor)
        assertEquals("#22FF22", allFour.adaptiveOverrides.batteryColor)
        assertEquals("#3333FF", allFour.adaptiveOverrides.messagesColor)
        assertEquals("#FFFF44", allFour.adaptiveOverrides.dateColor)

        // Asignación individual inmutable
        val oneByOne = base.copy(
            adaptiveOverrides = base.adaptiveOverrides.copy(clockColor = "#FF1111")
        ).copy(
            adaptiveOverrides = base.adaptiveOverrides.copy(clockColor = "#FF1111", batteryColor = "#22FF22")
        )
        assertEquals("#FF1111", oneByOne.adaptiveOverrides.clockColor)
        assertEquals("#22FF22", oneByOne.adaptiveOverrides.batteryColor)
        assertNull(oneByOne.adaptiveOverrides.messagesColor)
        assertNull(oneByOne.adaptiveOverrides.dateColor)
    }

    // ── 8. Restablecimiento independiente de cada campo sin alterar los demás ──
    @Test
    fun test08_RestablecimientoIndependiente_CadaCampoVuelveANullSinAfectarLosDemas() {
        val full = AdvancedStyleConfig().copy(
            adaptiveOverrides = AdaptiveColorOverrides(
                clockColor = "#AAAAAA",
                batteryColor = "#BBBBBB",
                messagesColor = "#CCCCCC",
                dateColor = "#DDDDDD"
            )
        )

        // Reset messagesColor
        val resetMessages = full.copy(
            adaptiveOverrides = full.adaptiveOverrides.copy(messagesColor = null)
        )
        assertNull(resetMessages.adaptiveOverrides.messagesColor)
        assertEquals("#AAAAAA", resetMessages.adaptiveOverrides.clockColor)
        assertEquals("#BBBBBB", resetMessages.adaptiveOverrides.batteryColor)
        assertEquals("#DDDDDD", resetMessages.adaptiveOverrides.dateColor)

        // Reset batteryColor
        val resetBattery = resetMessages.copy(
            adaptiveOverrides = resetMessages.adaptiveOverrides.copy(batteryColor = null)
        )
        assertNull(resetBattery.adaptiveOverrides.batteryColor)
        assertNull(resetBattery.adaptiveOverrides.messagesColor)
        assertEquals("#AAAAAA", resetBattery.adaptiveOverrides.clockColor)
        assertEquals("#DDDDDD", resetBattery.adaptiveOverrides.dateColor)

        // Reset clockColor
        val resetClock = resetBattery.copy(
            adaptiveOverrides = resetBattery.adaptiveOverrides.copy(clockColor = null)
        )
        assertNull(resetClock.adaptiveOverrides.clockColor)
        assertNull(resetClock.adaptiveOverrides.batteryColor)
        assertNull(resetClock.adaptiveOverrides.messagesColor)
        assertEquals("#DDDDDD", resetClock.adaptiveOverrides.dateColor)

        // Reset dateColor -> todas quedan en null
        val resetAll = resetClock.copy(
            adaptiveOverrides = resetClock.adaptiveOverrides.copy(dateColor = null)
        )
        assertNull(resetAll.adaptiveOverrides.clockColor)
        assertNull(resetAll.adaptiveOverrides.batteryColor)
        assertNull(resetAll.adaptiveOverrides.messagesColor)
        assertNull(resetAll.adaptiveOverrides.dateColor)
    }

    // ── 9. Excepciones inicialmente vacías (semántica null por defecto) ──
    @Test
    fun test09_ExcepcionesInicialmenteVacias_SemanticaNullPorDefecto() {
        val defaultOverrides = AdaptiveColorOverrides()
        assertNull("clockColor debe ser null por defecto", defaultOverrides.clockColor)
        assertNull("batteryColor debe ser null por defecto", defaultOverrides.batteryColor)
        assertNull("messagesColor debe ser null por defecto", defaultOverrides.messagesColor)
        assertNull("dateColor debe ser null por defecto", defaultOverrides.dateColor)

        val defaultConfig = AdvancedStyleConfig()
        assertNotNull(defaultConfig.adaptiveOverrides)
        assertNull(defaultConfig.adaptiveOverrides.clockColor)
        assertNull(defaultConfig.adaptiveOverrides.batteryColor)
        assertNull(defaultConfig.adaptiveOverrides.messagesColor)
        assertNull(defaultConfig.adaptiveOverrides.dateColor)
    }

    // ── 10. Persistencia round-trip con AUTO_WITH_OVERRIDES y excepciones mixtas ──
    @Test
    fun test10_PersistenciaRoundTrip_ModoAutoWithOverridesYMultiplesExcepciones() {
        val original = AdvancedStyleConfig().copy(
            themeMode = ThemeMode.AUTO_WITH_OVERRIDES,
            clockColor = "#990000", // color manual preexistente
            sidebarColor = "#001122",
            adaptiveOverrides = AdaptiveColorOverrides(
                clockColor = "#FFCC00",
                dateColor = "#00CCFF"
                // batteryColor y messagesColor son null
            )
        )

        val serialized = gson.toJson(original)
        val deserialized = gson.fromJson(serialized, AdvancedStyleConfig::class.java)
        val normalized = normalizeLoadedStyle(deserialized)

        assertEquals(ThemeMode.AUTO_WITH_OVERRIDES, normalized.themeMode)
        assertEquals("#990000", normalized.clockColor)
        assertEquals("#001122", normalized.sidebarColor)
        assertEquals("#FFCC00", normalized.adaptiveOverrides.clockColor)
        assertEquals("#00CCFF", normalized.adaptiveOverrides.dateColor)
        assertNull("batteryColor debe ser null tras deserializar", normalized.adaptiveOverrides.batteryColor)
        assertNull("messagesColor debe ser null tras deserializar", normalized.adaptiveOverrides.messagesColor)
    }
}
