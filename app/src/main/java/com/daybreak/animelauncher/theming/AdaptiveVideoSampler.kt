package com.daybreak.animelauncher.theming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.ArrayDeque

/**
 * UI-1 Fase 5: umbral perceptual para actualizar el tema a partir de muestras de video.
 *
 * Métrica: reutiliza [ThemePaletteAggregator.colorDistanceSq] (distancia euclídea RGB²,
 * la métrica perceptual existente del proyecto) sobre los colores dominantes, más un
 * delta de luminancia para captar cambios globalmente oscuro↔claro que conserven tono.
 */
object VideoPaletteGate {

    /** Distancia RGB² mínima entre dominantes para considerar cambio real. */
    const val DOMINANT_DISTANCE_SQ_THRESHOLD = 3600f // ≈ 60 unidades RGB por canal

    /** Delta de luminancia media que fuerza actualización aunque el tono se mantenga. */
    const val LUMINANCE_DELTA_THRESHOLD = 0.15f

    fun shouldUpdate(previous: ThemePalette?, candidate: ThemePalette): Boolean {
        if (previous == null) return true
        val dominantDist = ThemePaletteAggregator.colorDistanceSq(
            previous.dominantColor, candidate.dominantColor
        ).toFloat()
        val lumDelta = kotlin.math.abs(previous.averageLuminance - candidate.averageLuminance)
        return dominantDist >= DOMINANT_DISTANCE_SQ_THRESHOLD ||
            lumDelta >= LUMINANCE_DELTA_THRESHOLD
    }
}

/**
 * UI-1 Fase 5: sampler periódico y amortiguado del tema para fondos de VIDEO en AUTO.
 *
 * - Frecuencia: [intervalMs] (default 8 s). NO procesa frames del player: extrae UN
 *   frame representativo por tick vía [frameAt] (producción: [VideoFrameExtractor.extractFrameAt]
 *   en posición rotatoria sobre la duración del video) y lo analiza con [analyze]
 *   (producción: WallpaperAnalyzer.analyzeSync + recycle).
 * - Amortiguación: ventana de [windowSize] muestras agregadas con
 *   [ThemePaletteAggregator] (una transición/momento ruidoso aislado no cambia el launcher).
 * - Umbral: [VideoPaletteGate] — solo si el cambio es perceptual se emite por [onPalette].
 * - Ciclo de vida: [setActive] combina página-visible y app-foreground; inactivo → no
 *   extrae ni analiza (espera barata). [stop] cancela el loop; [reset] limpia ventana
 *   al cambiar de URI.
 * - Genérico en [T] para tests puros sin Bitmap (fakes), con [delayFn] inyectable
 *   (tests sin reloj real).
 */
class AdaptiveVideoSampler<T>(
    private val intervalMs: Long = 8000L,
    private val windowSize: Int = 3,
    private val maxTicks: Int = Int.MAX_VALUE,
    private val delayFn: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
    private val frameAt: suspend (tick: Int) -> T?,
    private val analyze: (T?) -> ThemePalette,
    private val onPalette: (ThemePalette, tick: Int) -> Unit
) {
    private val window = ArrayDeque<ThemePalette>()
    private var applied: ThemePalette? = null
    private var tick = 0

    @Volatile
    var running: Boolean = true
        private set

    private var job: Job? = null

    /** Número de extracciones ejecutadas (telemetría/tests). */
    @Volatile
    var extractionCount: Int = 0
        private set

    fun setActive(active: Boolean) {
        running = active
    }

    /** Limpia ventana y estado aplicado (cambio de URI). */
    fun reset() {
        synchronized(window) { window.clear() }
        applied = null
        tick = 0
    }

    fun start(scope: CoroutineScope) {
        stop()
        job = scope.launch {
            // `iterations` avanza en AMBAS ramas: garantiza terminación del bucle
            // incluso con delayFn inmediatos (tests), sin afectar la frecuencia
            // de extracción real (solo cuenta la rama activa en `tick`).
            var iterations = 0
            while (iterations < maxTicks) {
                coroutineContext.ensureActive() // cancelación cooperativa siempre
                if (running) {
                    runTick(tick)
                    tick++
                    iterations++
                    delayFn(intervalMs)
                } else {
                    // Inactivo (página invisible / launcher background): espera barata,
                    // cero extracciones y cero análisis.
                    iterations++
                    delayFn(200)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    val isStarted: Boolean get() = job?.isActive == true

    private suspend fun runTick(tickIndex: Int) {
        val frame = frameAt(tickIndex)
        extractionCount++
        val palette = analyze(frame)
        val candidate = synchronized(window) {
            window.addLast(palette)
            while (window.size > windowSize) window.removeFirst()
            if (window.size == 1) palette else ThemePaletteAggregator.aggregate(window.toList())
        }
        if (VideoPaletteGate.shouldUpdate(applied, candidate)) {
            applied = candidate
            onPalette(candidate, tickIndex)
        }
    }
}

/** Política pura de activación del sampling (tests y wrapper). */
fun samplingEnabled(
    mode: ThemeMode,
    isVideoBackground: Boolean,
    isPageVisible: Boolean,
    isAppForeground: Boolean
): Boolean = mode != ThemeMode.MANUAL && isVideoBackground && isPageVisible && isAppForeground
