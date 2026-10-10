package com.daybreak.animelauncher.ram

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Intervalo del polling de RAM (ms). UI-1 Fase 7: 5 s — nunca por frame. */
const val RAM_POLL_INTERVAL_MS = 5_000L

/** 1024³: base binaria mostrada con la etiqueta "GB" del formato de usuario. */
const val BYTES_PER_GB = 1_073_741_824L

/**
 * UI-1 Fase 7: snapshot de RAM GLOBAL del teléfono (ActivityManager.MemoryInfo).
 * Puro: cálculo, validación y porcentaje sin dependencias de Compose.
 *
 * [usedBytesApprox] es APROXIMADA: availMem incluye memoria que el sistema puede
 * recuperar (cachés), por lo que "usada" no equivale a memoria ocupada estrictamente
 * ni "disponible" a memoria completamente libre.
 */
data class RamSnapshot(
    val totalBytes: Long,
    val availBytes: Long,
    val lowMemory: Boolean
) {
    /** usedApprox = max(total − avail, 0); el coerce protege contra avail > total. */
    val usedBytesApprox: Long get() = (totalBytes - availBytes).coerceAtLeast(0L)

    /** Fracción usada acotada [0,1]; snapshot inválido → 0. */
    val usedFraction: Float
        get() = if (!isValid) 0f
        else (usedBytesApprox.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)

    /** Porcentaje usado acotado [0,100]. */
    val usedPercent: Int get() = (usedFraction * 100f).toInt().coerceIn(0, 100)

    /** total > 0 y 0 ≤ avail ≤ total. */
    val isValid: Boolean get() = totalBytes > 0L && availBytes >= 0L && availBytes <= totalBytes

    companion object {
        fun from(mi: ActivityManager.MemoryInfo): RamSnapshot =
            RamSnapshot(totalBytes = mi.totalMem, availBytes = mi.availMem, lowMemory = mi.lowMemory)
    }
}

/** Fuente de snapshots. null = error transitorio del proveedor (no lanza). */
fun interface RamProvider {
    suspend fun snapshot(): RamSnapshot?
}

/**
 * Proveedor real vía ActivityManager.MemoryInfo (API estándar; /proc/meminfo queda
 * descartado como fuente). Contexto de APLICACIÓN (nunca retiene una Activity);
 * lectura en [Dispatchers.Default] porque getMemoryInfo es una llamada binder.
 */
class ActivityManagerRamProvider(
    applicationContext: Context
) : RamProvider {
    private val appContext = applicationContext.applicationContext

    override suspend fun snapshot(): RamSnapshot? = withContext(Dispatchers.Default) {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return@withContext null
        val info = ActivityManager.MemoryInfo()
        try {
            am.getMemoryInfo(info)
        } catch (_: Exception) {
            return@withContext null
        }
        RamSnapshot.from(info)
    }
}

/**
 * Formato UI compacto. Un decimal fijo con Locale.US (punto decimal estable: evita
 * saltos de anchura por locale; es-ES renderizaría coma). Sin animación de cifras.
 */
object RamFormat {

    fun bytesToGb(bytes: Long): Float = bytes.toDouble().div(BYTES_PER_GB.toDouble()).toFloat()

    fun gb(bytes: Long): String = String.format(java.util.Locale.US, "%.1f", bytesToGb(bytes))

    /** "6.8 / 11.2 GB" (usada/total). Snapshot nulo o inválido → "—". */
    fun usedOverTotal(snapshot: RamSnapshot?): String =
        if (snapshot == null || !snapshot.isValid) "—"
        else "${gb(snapshot.usedBytesApprox)} / ${gb(snapshot.totalBytes)} GB"

    /** "4.4 GB disponibles" / "4.4 GB available". Inválido → "". */
    fun available(snapshot: RamSnapshot?, isEs: Boolean): String =
        if (snapshot == null || !snapshot.isValid) ""
        else gb(snapshot.availBytes) + " GB" + (if (isEs) " disponibles" else " available")

    /** Advertencia discreta cuando el SISTEMA reporta lowMemory (no por % alto). */
    fun lowMemoryWarning(isEs: Boolean): String = if (isEs) "RAM baja" else "Low RAM"

    /**
     * Tercera línea del widget: vacía si no hay datos; advertencia de baja memoria
     * si el sistema la reporta; si no, memoria disponible.
     */
    fun detailLine(snapshot: RamSnapshot?, isEs: Boolean): String = when {
        snapshot == null || !snapshot.isValid -> ""
        snapshot.lowMemory -> lowMemoryWarning(isEs)
        else -> available(snapshot, isEs)
    }

    /**
     * Colores del widget a partir de los tokens de la página: la cifra usa
     * textPrimary y el resto textSecondary (nunca accent). Misma fuente de tokens
     * que el resto del sidebar (MANUAL: fromAdvancedStyleConfig; AUTO: tokens
     * adaptativos de la página).
     */
    fun widgetColors(tokens: com.daybreak.animelauncher.theming.LauncherThemeTokens): Pair<Int, Int> =
        tokens.textPrimaryColor to tokens.textSecondaryColor
}

/**
 * UI-1 Fase 7: loop de polling estructurado y cancelable.
 *
 * Primera lectura inmediata; después exactamente UNA lectura por [tick] completo:
 * cada update está precedido por un tick, por lo que la frecuencia máxima la fija
 * el tick (5000 ms en producción → imposible actualizar a 60/120 Hz). La
 * cancelación del scope (página invisible, Activity fuera de RESUMED) interrumpe
 * el tick en curso y no ejecuta ninguna lectura más: no queda trabajo residual.
 * Sin relojes reales en tests: el tick es inyectable y contable.
 */
suspend fun pollRam(
    provider: RamProvider,
    tick: suspend () -> Unit,
    onUpdate: (RamSnapshot?) -> Unit
) {
    onUpdate(provider.snapshot())
    while (currentCoroutineContext().isActive) {
        tick()
        if (!currentCoroutineContext().isActive) return
        onUpdate(provider.snapshot())
    }
}
