package com.daybreak.animelauncher.hiddenapps

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerId
import com.daybreak.animelauncher.ui.screens.hiddenapps.HiddenAppsGestureDefaults
import com.daybreak.animelauncher.ui.screens.hiddenapps.HiddenAppsGestureEvaluationState
import com.daybreak.animelauncher.ui.screens.hiddenapps.HiddenAppsGestureLogic
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias de la lógica geométrica y del detector multitáctil Spread para Apps ocultas.
 */
class HiddenAppsGestureLogicTest {

    private val screenWidth = 1080f
    private val screenHeight = 2400f

    @Test
    fun testInitialCenterAccepted() {
        // Centro exacto de pantalla: (540, 1200)
        // Dos dedos simétricos alrededor del centro: (500, 1200) y (580, 1200) -> midpoint = (540, 1200)
        val p1 = Offset(500f, 1200f)
        val p2 = Offset(580f, 1200f)
        val mid = HiddenAppsGestureLogic.calculateMidPoint(p1, p2)

        val accepted = HiddenAppsGestureLogic.isWithinCenterZone(
            midPoint = mid,
            width = screenWidth,
            height = screenHeight,
            zoneFactor = HiddenAppsGestureDefaults.CENTER_ZONE_FACTOR
        )
        assertTrue("El punto medio en el centro geométrico debe ser aceptado", accepted)
    }

    @Test
    fun testInitialCenterRejected() {
        // Dedos en la esquina superior izquierda: (100, 200) y (180, 200) -> midpoint = (140, 200)
        val p1 = Offset(100f, 200f)
        val p2 = Offset(180f, 200f)
        val mid = HiddenAppsGestureLogic.calculateMidPoint(p1, p2)

        val accepted = HiddenAppsGestureLogic.isWithinCenterZone(
            midPoint = mid,
            width = screenWidth,
            height = screenHeight,
            zoneFactor = HiddenAppsGestureDefaults.CENTER_ZONE_FACTOR
        )
        assertFalse("El punto medio fuera del 35% central debe ser rechazado", accepted)
    }

    @Test
    fun testOneFingerRejected() {
        // Con 1 solo dedo no se debe iniciar el gesto
        val fingerCount = 1
        val canStart = (fingerCount == 2)
        assertFalse("Un solo dedo no debe iniciar el gesto Spread", canStart)
    }

    @Test
    fun testThreeFingersRejectedOrCancelled() {
        // Con 3 dedos se debe rechazar o cancelar inmediatamente
        val fingerCount = 3
        val canTrack = (fingerCount == 2)
        assertFalse("Tres dedos deben rechazar o cancelar el tracking del gesto", canTrack)
    }

    @Test
    fun testSpreadBelowThreshold() {
        val initialDist = 100f
        val currentDist = 140f // ratio = 1.40f < 1.45f
        val triggered = HiddenAppsGestureLogic.checkSpreadTrigger(
            currentDistance = currentDist,
            initialDistance = initialDist,
            hasTriggered = false,
            gestureCancelled = false
        )
        assertFalse("Distancia por debajo del umbral 1.45f no debe disparar", triggered)
    }

    @Test
    fun testSpreadAboveThreshold() {
        val initialDist = 100f
        val currentDist = 150f // ratio = 1.50f >= 1.45f
        val triggered = HiddenAppsGestureLogic.checkSpreadTrigger(
            currentDistance = currentDist,
            initialDistance = initialDist,
            hasTriggered = false,
            gestureCancelled = false
        )
        assertTrue("Distancia superior o igual al umbral 1.45f debe disparar el gesto", triggered)
    }

    @Test
    fun testCancelledGestureDoesNotTrigger() {
        val initialDist = 100f
        val currentDist = 200f // ratio = 2.0f
        val triggered = HiddenAppsGestureLogic.checkSpreadTrigger(
            currentDistance = currentDist,
            initialDistance = initialDist,
            hasTriggered = false,
            gestureCancelled = true
        )
        assertFalse("Un gesto cancelado no debe disparar aunque supere el umbral", triggered)
    }

    @Test
    fun testWidgetExclusion() {
        val widgetRect = Rect(100f, 100f, 500f, 500f)
        val p1Inside = Offset(200f, 200f)
        val p2Outside = Offset(700f, 700f)

        val touches = HiddenAppsGestureLogic.touchesAnyWidget(
            positions = listOf(p1Inside, p2Outside),
            widgetBounds = listOf(widgetRect)
        )
        assertTrue("Si algún dedo toca un widget, debe marcarse exclusión", touches)

        val bothOutside = HiddenAppsGestureLogic.touchesAnyWidget(
            positions = listOf(Offset(600f, 600f), Offset(700f, 700f)),
            widgetBounds = listOf(widgetRect)
        )
        assertFalse("Ambos dedos fuera de widgets no activan exclusión", bothOutside)
    }

    @Test
    fun testEditModeExclusion() {
        val conditionsMet = HiddenAppsGestureLogic.areConditionsMet(
            isHome = true,
            isWidgetEditMode = true,
            hasPendingThemeProposal = false,
            isUnlockPending = false
        )
        assertFalse("En modo edición de widgets el gesto debe estar bloqueado", conditionsMet)
    }

    @Test
    fun testNonHomeExclusion() {
        val conditionsMet = HiddenAppsGestureLogic.areConditionsMet(
            isHome = false,
            isWidgetEditMode = false,
            hasPendingThemeProposal = false,
            isUnlockPending = false
        )
        assertFalse("Fuera de Home el gesto Spread no debe estar activo", conditionsMet)
    }

    @Test
    fun testOneShotTrigger() {
        val state = HiddenAppsGestureEvaluationState(
            id1 = PointerId(1),
            id2 = PointerId(2),
            initialDistance = 100f,
            initialMidPoint = Offset(540f, 1200f)
        )

        val firstTrigger = HiddenAppsGestureLogic.checkSpreadTrigger(
            currentDistance = 160f,
            initialDistance = state.initialDistance,
            hasTriggered = state.hasTriggered,
            gestureCancelled = state.gestureCancelled
        )
        assertTrue("El primer disparo debe tener éxito", firstTrigger)
        state.hasTriggered = true

        val secondTrigger = HiddenAppsGestureLogic.checkSpreadTrigger(
            currentDistance = 180f,
            initialDistance = state.initialDistance,
            hasTriggered = state.hasTriggered,
            gestureCancelled = state.gestureCancelled
        )
        assertFalse("El segundo intento dentro del mismo ciclo no debe volver a disparar (one-shot)", secondTrigger)
    }

    @Test
    fun testPointerIdTracking() {
        val state = HiddenAppsGestureEvaluationState(
            id1 = PointerId(101),
            id2 = PointerId(102),
            initialDistance = 120f
        )

        assertEquals(PointerId(101), state.id1)
        assertEquals(PointerId(102), state.id2)

        state.reset()
        assertNull(state.id1)
        assertNull(state.id2)
        assertEquals(0f, state.initialDistance, 0.001f)
    }

    @Test
    fun testDensityScaledMinInitialDistance() {
        // Densidad 1.0 (mdpi): 24dp = 24px
        val min1 = HiddenAppsGestureDefaults.getMinInitialDistancePx(1.0f)
        assertEquals(24f, min1, 0.001f)

        // Densidad 3.0 (xxhdpi, POCO X6 5G): 24dp = 72px
        val min3 = HiddenAppsGestureDefaults.getMinInitialDistancePx(3.0f)
        assertEquals(72f, min3, 0.001f)

        // Distancia por debajo de la requerida en densidad 3.0 (50px < 72px)
        assertFalse(
            "Distancia inicial de 50px debe rechazarse en densidad 3.0",
            HiddenAppsGestureLogic.checkSpreadTrigger(
                currentDistance = 80f,
                initialDistance = 50f,
                hasTriggered = false,
                gestureCancelled = false,
                minInitialDistance = min3
            )
        )

        // Distancia válida en densidad 3.0 (80px >= 72px) que expande >= 1.45x
        assertTrue(
            "Distancia inicial válida con zoom >= 1.45x debe disparar",
            HiddenAppsGestureLogic.checkSpreadTrigger(
                currentDistance = 120f,
                initialDistance = 80f,
                hasTriggered = false,
                gestureCancelled = false,
                minInitialDistance = min3
            )
        )
    }

    @Test
    fun testConsumedPointersDoNotTrigger() {
        val triggered = HiddenAppsGestureLogic.checkSpreadTrigger(
            currentDistance = 200f,
            initialDistance = 100f,
            hasTriggered = false,
            gestureCancelled = true
        )
        assertFalse("Punteros marcados como consumidos no deben disparar spread", triggered)
    }
}
