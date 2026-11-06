package com.daybreak.animelauncher.ram

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Intervalo del polling de RAM (ms). UI-1 Fase 7: 5 s — nunca por frame. */
const val RAM_POLL_INTERVAL_MS = 5_000L

/** SI decimal y base binaria IEC: las etiquetas nunca son intercambiables. */
const val BYTES_PER_GB = 1_000_000_000L
const val BYTES_PER_GIB = 1_073_741_824L

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
    val lowMemory: Boolean,
    /** Capacidad anunciada reportada por Android, sin inferir ni sumar RAM virtual. */
    val advertisedBytes: Long? = null
) {
    /** Validar ANTES de restar evita valores negativos y overflow de Long. */
    val usedBytesApprox: Long get() = if (isValid) totalBytes - availBytes else 0L

    /** Un anuncio menor que la memoria accesible es incoherente, no se inventa otro. */
    val advertisedBytesOrNull: Long?
        get() = advertisedBytes?.takeIf { it > 0L && (totalBytes <= 0L || it >= totalBytes) }

    /** Fracción usada acotada [0,1]; snapshot inválido → 0. */
    val usedFraction: Float
        get() = if (!isValid) 0f
        else (usedBytesApprox.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)

    /** Porcentaje usado acotado [0,100]. */
    val usedPercent: Int
        get() = if (!isValid) 0 else
            (usedBytesApprox.toDouble() / totalBytes.toDouble() * 100.0).roundToInt().coerceIn(0, 100)

    /** total > 0 y 0 ≤ avail ≤ total. */
    val isValid: Boolean get() = totalBytes > 0L && availBytes >= 0L && availBytes <= totalBytes

    companion object {
        fun from(mi: ActivityManager.MemoryInfo): RamSnapshot {
            // Nunca acceder al campo añadido en API 34 en versiones anteriores.
            val advertised = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mi.advertisedMem.takeIf { it > 0L && (mi.totalMem <= 0L || it >= mi.totalMem) }
            } else null
            return RamSnapshot(mi.totalMem, mi.availMem, mi.lowMemory, advertised)
        }
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
 * Formato centralizado SI/IEC. Un decimal fijo y locale de la UI.
 * Las métricas son del sistema Android, no del proceso de NovaLauncher.
 */
object RamFormat {

    fun bytesToGb(bytes: Long): Double = bytes.toDouble() / BYTES_PER_GB
    fun bytesToGib(bytes: Long): Double = bytes.toDouble() / BYTES_PER_GIB

    fun gb(bytes: Long, locale: Locale = Locale.US): String =
        String.format(locale, "%.1f", bytesToGb(bytes))

    fun gib(bytes: Long, locale: Locale = Locale.US): String =
        String.format(locale, "%.1f", bytesToGib(bytes))

    /** Conserva la región del dispositivo si coincide con el idioma elegido. */
    fun localeForLanguage(language: String, systemLocale: Locale): Locale {
        val requested = Locale.forLanguageTag(language)
        return if (requested.language == systemLocale.language) systemLocale else requested
    }

    /** GB decimales: usada estimada / accesible al sistema, nunca RAM anunciada. */
    fun usedOverTotal(snapshot: RamSnapshot?, locale: Locale = Locale.US): String =
        if (snapshot == null || !snapshot.isValid) "—"
        else "${gb(snapshot.usedBytesApprox, locale)} / ${gb(snapshot.totalBytes, locale)} GB"

    /** "4.4 GB disponibles" / "4.4 GB available". Inválido → "". */
    fun available(
        snapshot: RamSnapshot?, isEs: Boolean,
        locale: Locale = if (isEs) Locale.forLanguageTag("es") else Locale.US
    ): String =
        if (snapshot == null || !snapshot.isValid) ""
        else gb(snapshot.availBytes, locale) + " GB" + (if (isEs) " disponibles" else " available")

    /** Advertencia discreta cuando el SISTEMA reporta lowMemory (no por % alto). */
    fun lowMemoryWarning(isEs: Boolean): String = if (isEs) "RAM baja" else "Low RAM"

    /**
     * Tercera línea del widget: vacía si no hay datos; advertencia de baja memoria
     * si el sistema la reporta; si no, memoria disponible.
     */
    fun detailLine(
        snapshot: RamSnapshot?, isEs: Boolean,
        locale: Locale = if (isEs) Locale.forLanguageTag("es") else Locale.US
    ): String = when {
        snapshot == null || !snapshot.isValid -> ""
        snapshot.lowMemory -> lowMemoryWarning(isEs)
        else -> available(snapshot, isEs, locale)
    }

    data class DetailRow(val label: String, val value: String)

    /** Solo se construye al abrir el detalle; todas las cantidades comparten fuente/unidades. */
    fun detailRows(snapshot: RamSnapshot?, isEs: Boolean, locale: Locale): List<DetailRow> {
        val valid = snapshot?.takeIf { it.isValid }
        fun quantity(bytes: Long?): String =
            bytes?.let { "${gb(it, locale)} GB (${gib(it, locale)} GiB)" }
                ?: if (isEs) "No disponible" else "Unavailable"
        return listOf(
            DetailRow(if (isEs) "RAM anunciada" else "Advertised RAM",
                quantity(snapshot?.advertisedBytesOrNull)),
            DetailRow(if (isEs) "Accesible a Android" else "Accessible to Android",
                quantity(valid?.totalBytes)),
            DetailRow(if (isEs) "Disponible" else "Available",
                quantity(valid?.availBytes)),
            DetailRow(if (isEs) "Uso estimado" else "Estimated use",
                quantity(valid?.usedBytesApprox)),
            DetailRow(if (isEs) "Utilización del sistema" else "System utilization",
                valid?.let { "${it.usedPercent}%" } ?: if (isEs) "No disponible" else "Unavailable")
        )
    }

    fun accessibilitySummary(snapshot: RamSnapshot?, isEs: Boolean, locale: Locale): String {
        val system = if (isEs) "RAM del sistema Android" else "Android system RAM"
        if (snapshot?.isValid != true) return system + if (isEs) ": no disponible" else ": unavailable"
        val used = if (isEs) "uso estimado" else "estimated use"
        return "$system, $used: ${usedOverTotal(snapshot, locale)}, ${snapshot.usedPercent}%. " +
            detailLine(snapshot, isEs, locale)
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
