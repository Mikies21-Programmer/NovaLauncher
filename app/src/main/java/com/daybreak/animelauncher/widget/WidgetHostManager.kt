package com.daybreak.animelauncher.widget

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gestor desacoplado de Widgets Nativos para NovaLauncher/AnimeLauncher.
 *
 * Utiliza exclusivamente el Application Context para evitar retenciones o fugas
 * de memoria de Activities, Views o PlayerViews.
 */
class WidgetHostManager(application: Application) {

    companion object {
        const val APPWIDGET_HOST_ID = 1024
        const val HYSTERESIS_THRESHOLD_DP = 4
    }

    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(application)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(application, APPWIDGET_HOST_ID)

    private var isListening: Boolean = false

    /**
     * Inicia la escucha de actualizaciones de widgets.
     * Protegido con [isListening] para evitar llamadas redundantes cuando onResume se encadena.
     */
    fun startListening() {
        if (!isListening) {
            try {
                appWidgetHost.startListening()
                isListening = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Pausa la escucha de widgets cuando la Activity entra en pausa o se detiene.
     * Protegido con [isListening] para evitar llamadas redundantes entre onPause y onStop.
     */
    fun stopListening() {
        if (isListening) {
            try {
                appWidgetHost.stopListening()
                isListening = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Libera la escucha del host al destruirse definitivamente el launcher.
     */
    fun clearViews() {
        try {
            stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Obtiene los proveedores de widgets compatibles exclusivamente con la pantalla de inicio.
     */
    fun getHomeScreenProviders(context: Context): List<AppWidgetProviderInfo> {
        val providers = try {
            appWidgetManager.installedProviders
        } catch (e: Exception) {
            emptyList()
        }
        val pm = context.packageManager
        return providers.filter { info ->
            val isHome = (info.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) != 0 || info.widgetCategory == 0
            isHome
        }.sortedBy { it.loadLabel(pm).lowercase() }
    }

    /**
     * Carga la preview real del widget si existe, con fallback a su icono de app del proveedor,
     * y último fallback a null (que activa el icono genérico en la UI).
     */
    fun loadWidgetPreview(info: AppWidgetProviderInfo, context: Context): Drawable? {
        try {
            val preview = info.loadPreviewImage(context, 0)
            if (preview != null) return preview
        } catch (e: Exception) {}
        try {
            val icon = info.loadIcon(context, 0)
            if (icon != null) return icon
        } catch (e: Exception) {}
        try {
            return context.packageManager.getApplicationIcon(info.provider.packageName)
        } catch (e: Exception) {}
        return null
    }

    /**
     * Calcula la dimensión en celdas (columnas × filas).
     */
    fun getCellSpan(info: AppWidgetProviderInfo, context: Context): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val targetW = info.targetCellWidth
            val targetH = info.targetCellHeight
            if (targetW > 0 && targetH > 0) {
                return Pair(targetW.coerceIn(1, 4), targetH.coerceIn(1, 6))
            }
        }
        val density = context.resources.displayMetrics.density
        val wDp = if (info.minWidth > 600) (info.minWidth / density).toInt() else info.minWidth
        val hDp = if (info.minHeight > 1200) (info.minHeight / density).toInt() else info.minHeight
        val spanX = kotlin.math.ceil((wDp - 30f) / 70f).toInt().coerceIn(1, 4)
        val spanY = kotlin.math.ceil((hDp - 30f) / 70f).toInt().coerceIn(1, 6)
        return Pair(spanX, spanY)
    }

    /**
     * Devuelve la representación legible del tamaño en celdas (ej. "2 × 1", "4 × 2").
     */
    fun getCellSpanString(info: AppWidgetProviderInfo, context: Context): String {
        val (x, y) = getCellSpan(info, context)
        return "$x × $y"
    }

    /**
     * Calcula la altura razonable inicial respetando el mínimo del proveedor.
     */
    fun getDesiredHeightDp(info: AppWidgetProviderInfo, context: Context): Dp {
        val density = context.resources.displayMetrics.density
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
            val cellH = (info.targetCellHeight * 70) + ((info.targetCellHeight - 1) * 8)
            return cellH.coerceIn(56, 420).dp
        }
        val hDp = if (info.minHeight > 1200) (info.minHeight / density).toInt() else info.minHeight
        return hDp.coerceIn(56, 420).dp
    }

    /**
     * Comunica el tamaño real del contenedor al proveedor.
     * Utiliza la sobrecarga adecuada según la versión de Android:
     * - API >= 31: updateAppWidgetSize(Bundle, List<SizeF>)
     * - API < 31: updateAppWidgetSize(Bundle, minWidth, minHeight, maxWidth, maxHeight)
     *
     * Retorna el nuevo par (widthDp, heightDp) si se envió actualización, o null si se descartó por histéresis.
     */
    fun updateWidgetSize(
        hostView: AppWidgetHostView,
        appWidgetId: Int,
        widthPx: Int,
        heightPx: Int,
        density: Float,
        lastReportedWidthDp: Int,
        lastReportedHeightDp: Int
    ): Pair<Int, Int>? {
        if (widthPx <= 0 || heightPx <= 0 || density <= 0f) return null
        val widthDp = (widthPx / density).toInt()
        val heightDp = (heightPx / density).toInt()

        // Control estricto de histéresis para evitar loops de recomposición
        if (kotlin.math.abs(widthDp - lastReportedWidthDp) < HYSTERESIS_THRESHOLD_DP &&
            kotlin.math.abs(heightDp - lastReportedHeightDp) < HYSTERESIS_THRESHOLD_DP
        ) {
            return null
        }

        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
        }

        try {
            appWidgetManager.updateAppWidgetOptions(appWidgetId, options)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sizeList = listOf(SizeF(widthDp.toFloat(), heightDp.toFloat()))
                hostView.updateAppWidgetSize(options, sizeList)
            } else {
                @Suppress("DEPRECATION")
                hostView.updateAppWidgetSize(options, widthDp, heightDp, widthDp, heightDp)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(widthDp, heightDp)
    }

    fun allocateWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun getAppWidgetInfo(widgetId: Int): AppWidgetProviderInfo? {
        return try {
            appWidgetManager.getAppWidgetInfo(widgetId)
        } catch (e: Exception) {
            null
        }
    }

    fun bindAppWidgetIdIfAllowed(widgetId: Int, provider: ComponentName): Boolean {
        return try {
            appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider)
        } catch (e: Exception) {
            false
        }
    }

    fun deleteWidgetId(widgetId: Int) {
        try {
            appWidgetHost.deleteAppWidgetId(widgetId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Valida una lista de IDs contra los proveedores actualmente instalados.
     * Si un widget ya no existe (app desinstalada), elimina su ID para evitar huérfanos.
     */
    fun validateAndCleanWidgets(existingWidgetIds: List<Int>): List<Int> {
        val validIds = mutableListOf<Int>()
        for (id in existingWidgetIds) {
            val info = getAppWidgetInfo(id)
            if (info != null) {
                validIds.add(id)
            } else {
                deleteWidgetId(id)
            }
        }
        return validIds
    }
}
