package com.daybreak.animelauncher.ui.screens.hiddenapps

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot

/**
 * Constantes y lógica de detección multitáctil para el gesto Spread de Apps ocultas.
 */
object HiddenAppsGestureDefaults {
    /**
     * Factor de zona central: 35% central de las dimensiones de la pantalla.
     */
    const val CENTER_ZONE_FACTOR = 0.35f

    /**
     * Distancia inicial mínima en DP para independizar el umbral de la densidad de pantalla (DPI).
     */
    const val MIN_INITIAL_DISTANCE_DP = 24f

    /**
     * Calcula la distancia inicial mínima en píxeles según la densidad de la pantalla.
     * En densidad 1.0 (mdpi) = 24px; en densidad 3.0 (xxhdpi, POCO X6) = 72px.
     */
    fun getMinInitialDistancePx(density: Float): Float = MIN_INITIAL_DISTANCE_DP * density

    /**
     * Distancia inicial mínima baseline en píxeles (densidad 1.0).
     */
    const val MIN_INITIAL_DISTANCE = 24f

    /**
     * Umbral de separación para disparar la apertura de Apps ocultas (45% de incremento relativo).
     */
    const val SPREAD_TRIGGER_ZOOM = 1.45f

    /**
     * Umbral de contracción para considerar que el gesto es un Pinch-in hacia adentro,
     * invalidando la posibilidad de que sea un Spread en el mismo ciclo.
     */
    const val PINCH_DIRECTION_THRESHOLD = 0.85f
}

/**
 * Estado lógico evaluable para pruebas unitarias y aislamiento de decisiones del gesto.
 */
data class HiddenAppsGestureEvaluationState(
    var id1: PointerId? = null,
    var id2: PointerId? = null,
    var initialDistance: Float = 0f,
    var initialMidPoint: Offset = Offset.Zero,
    var hasTriggered: Boolean = false,
    var gestureCancelled: Boolean = false
) {
    fun reset() {
        id1 = null
        id2 = null
        initialDistance = 0f
        initialMidPoint = Offset.Zero
        hasTriggered = false
        gestureCancelled = false
    }
}

/**
 * Funciones puras de cálculo geométrico y validación para el gesto Spread.
 */
object HiddenAppsGestureLogic {

    /**
     * Calcula la distancia euclidiana entre dos puntos.
     */
    fun calculateDistance(p1: Offset, p2: Offset): Float {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }

    /**
     * Calcula el punto medio geométrico entre dos posiciones.
     */
    fun calculateMidPoint(p1: Offset, p2: Offset): Offset {
        return Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
    }

    /**
     * Comprueba si el punto medio se encuentra dentro del 35% central de la pantalla.
     */
    fun isWithinCenterZone(
        midPoint: Offset,
        width: Float,
        height: Float,
        zoneFactor: Float = HiddenAppsGestureDefaults.CENTER_ZONE_FACTOR
    ): Boolean {
        if (width <= 0f || height <= 0f) return false
        val halfZoneW = (width * zoneFactor) / 2f
        val halfZoneH = (height * zoneFactor) / 2f
        val centerX = width / 2f
        val centerY = height / 2f

        return midPoint.x in (centerX - halfZoneW)..(centerX + halfZoneW) &&
                midPoint.y in (centerY - halfZoneH)..(centerY + halfZoneH)
    }

    /**
     * Evalúa si alguna de las posiciones interseca con rectángulos de widgets excluidos.
     */
    fun touchesAnyWidget(positions: List<Offset>, widgetBounds: List<Rect>): Boolean {
        if (widgetBounds.isEmpty()) return false
        return positions.any { pos -> widgetBounds.any { rect -> rect.contains(pos) } }
    }

    /**
     * Determina si las condiciones globales permiten iniciar o procesar el gesto.
     */
    fun areConditionsMet(
        isHome: Boolean,
        isWidgetEditMode: Boolean,
        hasPendingThemeProposal: Boolean,
        isUnlockPending: Boolean
    ): Boolean {
        return isHome && !isWidgetEditMode && !hasPendingThemeProposal && !isUnlockPending
    }

    /**
     * Procesa una actualización de distancia y decide si debe disparar el gesto Spread.
     */
    fun checkSpreadTrigger(
        currentDistance: Float,
        initialDistance: Float,
        hasTriggered: Boolean,
        gestureCancelled: Boolean,
        triggerZoom: Float = HiddenAppsGestureDefaults.SPREAD_TRIGGER_ZOOM,
        minInitialDistance: Float = HiddenAppsGestureDefaults.MIN_INITIAL_DISTANCE
    ): Boolean {
        if (hasTriggered || gestureCancelled) return false
        if (initialDistance < minInitialDistance) return false
        return (currentDistance / initialDistance) >= triggerZoom
    }
}

/**
 * Modificador Compose para detectar el gesto Spread de 2 dedos en la pantalla de inicio.
 *
 * Sigue el patrón estricto del proyecto:
 * - PointerEventPass.Initial
 * - Tracking por PointerId
 * - Dos dedos comenzando en la zona central fuera de widgets
 * - One-shot trigger
 * - Consumo únicamente al superar el umbral
 *
 * Los bounds de widgets se leen mediante [currentWidgetBoundsList] dentro del ciclo de
 * eventos y NO forman parte de las keys de pointerInput: las listas que ViewOne/ViewTwo
 * re-emiten en cada pase de layout cancelarían y relanzarían el detector continuamente.
 */
fun Modifier.hiddenAppsSpreadGesture(
    isHome: Boolean,
    isWidgetEditMode: Boolean,
    hasPendingThemeProposal: Boolean,
    isUnlockPending: Boolean,
    currentWidgetBoundsList: () -> List<Rect>,
    onSpreadTriggered: () -> Unit
): Modifier = this.pointerInput(isHome, isWidgetEditMode, hasPendingThemeProposal, isUnlockPending) {
    awaitPointerEventScope {
        while (true) {
            var event = awaitPointerEvent(PointerEventPass.Initial)
            val pressedPointers = event.changes.filter { it.pressed }

            // El gesto requiere exactamente 2 dedos
            if (pressedPointers.size == 2) {
                val p1 = pressedPointers[0]
                val p2 = pressedPointers[1]
                val id1 = p1.id
                val id2 = p2.id

                val screenWidth = size.width.toFloat()
                val screenHeight = size.height.toFloat()

                val initialDistance = HiddenAppsGestureLogic.calculateDistance(p1.position, p2.position)
                val initialMidPoint = HiddenAppsGestureLogic.calculateMidPoint(p1.position, p2.position)

                // 1. Verificación de condiciones globales
                val conditionsMet = HiddenAppsGestureLogic.areConditionsMet(
                    isHome = isHome,
                    isWidgetEditMode = isWidgetEditMode,
                    hasPendingThemeProposal = hasPendingThemeProposal,
                    isUnlockPending = isUnlockPending
                )

                // 2. Verificación de zona central
                val inCenter = HiddenAppsGestureLogic.isWithinCenterZone(
                    midPoint = initialMidPoint,
                    width = screenWidth,
                    height = screenHeight
                )

                // 3. Verificación de exclusión de widgets (lectura fresca, sin key)
                val touchWidget = HiddenAppsGestureLogic.touchesAnyWidget(
                    positions = listOf(p1.position, p2.position),
                    widgetBounds = currentWidgetBoundsList()
                )

                val minInitialDistancePx = HiddenAppsGestureDefaults.getMinInitialDistancePx(density)

                val canTrack = conditionsMet && inCenter && !touchWidget &&
                        (initialDistance >= minInitialDistancePx)

                var hasTriggered = false
                var gestureCancelled = !canTrack

                while (true) {
                    event = awaitPointerEvent(PointerEventPass.Initial)
                    val activeChanges = event.changes.filter { it.pressed }

                    // Cancelar si desaparece algún dedo o si aparece un tercer dedo
                    if (activeChanges.size != 2) {
                        gestureCancelled = true
                        break
                    }

                    val change1 = activeChanges.find { it.id == id1 }
                    val change2 = activeChanges.find { it.id == id2 }

                    // Cancelar si desaparece algún dedo o si los eventos ya fueron consumidos (ej. por pinch-in)
                    if (change1 == null || change2 == null || change1.isConsumed || change2.isConsumed) {
                        gestureCancelled = true
                        break
                    }

                    if (!gestureCancelled && !hasTriggered) {
                        val currentDistance = HiddenAppsGestureLogic.calculateDistance(change1.position, change2.position)
                        val ratio = currentDistance / initialDistance

                        // Si la dirección es hacia adentro (Pinch), cancelar Spread para evitar conflictos
                        if (ratio < HiddenAppsGestureDefaults.PINCH_DIRECTION_THRESHOLD) {
                            gestureCancelled = true
                        } else if (ratio >= HiddenAppsGestureDefaults.SPREAD_TRIGGER_ZOOM) {
                            hasTriggered = true
                            change1.consume()
                            change2.consume()
                            onSpreadTriggered()
                        }
                    }
                }

                // Esperar a que se liberen todos los punteros antes de un nuevo ciclo
                while (event.changes.any { it.pressed }) {
                    event = awaitPointerEvent(PointerEventPass.Initial)
                }
            }
        }
    }
}
