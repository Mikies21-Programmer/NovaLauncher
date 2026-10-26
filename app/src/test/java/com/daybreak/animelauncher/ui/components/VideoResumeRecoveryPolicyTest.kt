package com.daybreak.animelauncher.ui.components

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias para la política pura de elegibilidad de recuperación de video al reanudar.
 *
 * Verifica que solo los estados objetivamente inconsistentes (error, IDLE, ENDED) disparen
 * la re-preparación del reproductor, protegiendo los estados válidos (READY, BUFFERING)
 * contra reinicios arbitrarios o parpadeos negros.
 */
class VideoResumeRecoveryPolicyTest {

    @Test
    fun evaluateVideoResumeAction_whenAssignedPageIsNull_returnsNoOp() {
        val states = listOf(
            Player.STATE_IDLE,
            Player.STATE_BUFFERING,
            Player.STATE_READY,
            Player.STATE_ENDED
        )
        for (state in states) {
            assertEquals(
                "Un slot sin página asignada siempre debe resultar en NO_OP sin error",
                VideoResumeAction.NO_OP,
                evaluateVideoResumeAction(assignedPage = null, playbackState = state, hasPlayerError = false)
            )
            assertEquals(
                "Un slot sin página asignada siempre debe resultar en NO_OP incluso con error",
                VideoResumeAction.NO_OP,
                evaluateVideoResumeAction(assignedPage = null, playbackState = state, hasPlayerError = true)
            )
        }
    }

    @Test
    fun evaluateVideoResumeAction_whenHasPlayerError_returnsReprepareAndPlay() {
        val states = listOf(
            Player.STATE_IDLE,
            Player.STATE_BUFFERING,
            Player.STATE_READY,
            Player.STATE_ENDED
        )
        for (state in states) {
            assertEquals(
                "Cualquier estado con playerError debe desencadenar REPREPARE_AND_PLAY",
                VideoResumeAction.REPREPARE_AND_PLAY,
                evaluateVideoResumeAction(assignedPage = 0, playbackState = state, hasPlayerError = true)
            )
        }
    }

    @Test
    fun evaluateVideoResumeAction_whenStateIdleWithoutError_returnsReprepareAndPlay() {
        val action = evaluateVideoResumeAction(
            assignedPage = 0,
            playbackState = Player.STATE_IDLE,
            hasPlayerError = false
        )
        assertEquals(
            "STATE_IDLE indica reproductor no inicializado o detenido, debe re-preparar",
            VideoResumeAction.REPREPARE_AND_PLAY,
            action
        )
    }

    @Test
    fun evaluateVideoResumeAction_whenStateEndedWithoutError_returnsReprepareAndPlay() {
        val action = evaluateVideoResumeAction(
            assignedPage = 1,
            playbackState = Player.STATE_ENDED,
            hasPlayerError = false
        )
        assertEquals(
            "STATE_ENDED en un fondo en bucle indica finalización no reanudable directamente, debe re-preparar",
            VideoResumeAction.REPREPARE_AND_PLAY,
            action
        )
    }

    @Test
    fun evaluateVideoResumeAction_whenStateReadyWithoutError_returnsResumePlayback() {
        val action = evaluateVideoResumeAction(
            assignedPage = 0,
            playbackState = Player.STATE_READY,
            hasPlayerError = false
        )
        assertEquals(
            "STATE_READY sin errores es un estado nominal válido: NO debe re-preparar ni reiniciar",
            VideoResumeAction.RESUME_PLAYBACK,
            action
        )
    }

    @Test
    fun evaluateVideoResumeAction_whenStateBufferingWithoutError_returnsResumePlayback() {
        val action = evaluateVideoResumeAction(
            assignedPage = 0,
            playbackState = Player.STATE_BUFFERING,
            hasPlayerError = false
        )
        assertEquals(
            "STATE_BUFFERING sin errores está cargando legítimamente: NO debe interrumpirse",
            VideoResumeAction.RESUME_PLAYBACK,
            action
        )
    }
}
