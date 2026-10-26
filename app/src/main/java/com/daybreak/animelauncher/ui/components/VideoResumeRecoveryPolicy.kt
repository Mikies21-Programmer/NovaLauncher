package com.daybreak.animelauncher.ui.components

import androidx.media3.common.Player

/**
 * Acciones deterministas de reanudación para un slot de video de fondo al volver a foreground.
 */
enum class VideoResumeAction {
    /**
     * El slot no tiene página asignada; no se debe realizar ninguna acción.
     */
    NO_OP,

    /**
     * El reproductor se encuentra en un estado nominal válido (STATE_READY o STATE_BUFFERING sin errores).
     * Solo debe reafirmarse `playWhenReady = true` sin reinicios, seeks espurios ni llamadas a `prepare()`.
     */
    RESUME_PLAYBACK,

    /**
     * El reproductor se encuentra en un estado objetivamente inconsistente o detenido
     * (STATE_IDLE, STATE_ENDED o playerError != null).
     * Requiere re-preparación determinista (`seekTo` a posición guardada y `prepare()`).
     */
    REPREPARE_AND_PLAY
}

/**
 * Evalúa de forma pura y determinista la acción de reanudación requerida para un slot de video
 * al producirse un evento de reanudación (onResume / onWindowFocusChanged / re-bind).
 *
 * Reglas de estabilidad visual y ciclo de vida:
 * 1. Si no hay página asignada (slot libre), no se ejecuta acción (NO_OP).
 * 2. Si existe error activo en el reproductor (playerError != null), requiere re-preparación (REPREPARE_AND_PLAY).
 * 3. Si el reproductor está en STATE_IDLE o STATE_ENDED, requiere re-preparación (REPREPARE_AND_PLAY).
 * 4. Si el reproductor está en STATE_READY o STATE_BUFFERING sin errores, el estado es legítimo:
 *    NO se debe detener, ni re-preparar, ni reiniciar la superficie; únicamente reafirmar reproducción (RESUME_PLAYBACK).
 *
 * @param assignedPage Página asociada al slot, o null si está libre.
 * @param playbackState Estado actual de ExoPlayer (Player.STATE_*).
 * @param hasPlayerError Indica si ExoPlayer tiene un error activo reportado.
 */
fun evaluateVideoResumeAction(
    assignedPage: Int?,
    playbackState: Int,
    hasPlayerError: Boolean
): VideoResumeAction {
    if (assignedPage == null) {
        return VideoResumeAction.NO_OP
    }
    if (hasPlayerError || playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
        return VideoResumeAction.REPREPARE_AND_PLAY
    }
    return VideoResumeAction.RESUME_PLAYBACK
}
