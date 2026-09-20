package com.daybreak.animelauncher.theming

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Extractor quirúrgico y seguro de frames representativos de video para el análisis
 * adaptativo de color (Adaptive Wallpaper Theming - Fase 2E).
 *
 * Responsabilidad estricta:
 * - Extraer hasta 3 frames representativos en los puntos temporales 10%, 50% y 90%
 *   (omitiendo 0% y 100% para descartar fundidos o transiciones de entrada/salida).
 * - Ejecutarse de forma no bloqueante en [Dispatchers.IO].
 * - Decodificación acotada en resolución (~400 px) para evitar la carga de frames 4K completos en memoria.
 * - Liberación sistemática de [MediaMetadataRetriever] mediante bloque finally.
 * - Soporte para cancelación cooperativa de corrutinas entre extracciones.
 * - Resiliencia absoluta ante fallos individuales de frames, formatos no soportados o URIs inválidos.
 */
object VideoFrameExtractor {

    /** Dimensión máxima para los bitmaps de análisis y preview (contención de memoria). */
    private const val TARGET_MAX_DIMENSION = 400

    /**
     * Extrae de forma asíncrona hasta 3 frames representativos del video indicado por [videoUri].
     *
     * @param context Contexto de la aplicación para resolver el [videoUri].
     * @param videoUri URI del archivo o stream de video.
     * @param dispatcher Despachador de I/O donde se ejecutará la extracción (por defecto [Dispatchers.IO]).
     * @return Lista de [Bitmap] válidos y listos para análisis cromático (o lista vacía en caso de error total).
     */
    suspend fun extractFrames(
        context: Context,
        videoUri: Uri,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): List<Bitmap> = withContext(dispatcher) {
        val frames = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, videoUri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val durationUs = durationMs * 1000L

            val timestampsUs = if (durationUs > 0) {
                listOf(
                    (durationUs * 0.10).toLong(),
                    (durationUs * 0.50).toLong(),
                    (durationUs * 0.90).toLong()
                )
            } else {
                // Fallback si la metadata de duración es nula o inválida: intentar un único frame en 0 µs
                listOf(0L)
            }

            for (timeUs in timestampsUs) {
                // Cancelación cooperativa si el diálogo fue descartado por el usuario
                coroutineContext.ensureActive()

                try {
                    val frame = extractScaledFrame(retriever, timeUs, TARGET_MAX_DIMENSION)
                    if (frame != null && !frame.isRecycled && frame.width > 0 && frame.height > 0) {
                        frames.add(frame)
                    }
                } catch (e: Exception) {
                    // Tolerar fallo individual de frame sin abortar la extracción de los demás
                }
            }
        } catch (e: Exception) {
            // Error en setDataSource (URI inválido, permiso denegado, archivo corrupto)
            // Se garantiza retorno de lista vacía sin propagar excepciones al hilo de UI
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
                // Silenciar posibles excepciones en la liberación del recurso nativo
            }
        }

        frames
    }

    /**
     * Extrae un frame en el tiempo [timeUs] escalado directamente si la versión de Android lo permite (API 27+),
     * o escala inmediatamente el bitmap resultante para evitar retener frames en resolución nativa (4K / 1080p).
     */
    internal fun extractScaledFrame(
        retriever: MediaMetadataRetriever,
        timeUs: Long,
        maxDimension: Int
    ): Bitmap? {
        val rawBitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                retriever.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    maxDimension,
                    maxDimension
                )
            } catch (_: Throwable) {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } else {
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }

        if (rawBitmap == null || rawBitmap.isRecycled) {
            return null
        }

        // Si la API devolvió un frame mayor a la dimensión máxima, downscale inmediato y reciclado del original
        if (rawBitmap.width > maxDimension || rawBitmap.height > maxDimension) {
            val maxDim = maxOf(rawBitmap.width, rawBitmap.height)
            val scale = maxDimension.toFloat() / maxDim
            val targetW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
            val targetH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)

            val scaled = Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
            if (scaled != rawBitmap) {
                rawBitmap.recycle()
            }
            return scaled
        }

        return rawBitmap
    }
}
