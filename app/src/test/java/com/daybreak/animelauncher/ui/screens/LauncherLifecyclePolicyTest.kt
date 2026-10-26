package com.daybreak.animelauncher.ui.screens

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la política de ciclo de vida de LauncherScreen.
 *
 * Verifica que la revocación de la sesión de Apps Ocultas responda exclusivamente
 * a Lifecycle.Event.ON_STOP (fail-closed al pasar a segundo plano) y a ningún otro evento.
 */
class LauncherLifecyclePolicyTest {

    @Test
    fun shouldRevokeHiddenAppsSession_returnsTrueOnlyForOnStop() {
        assertTrue(
            "ON_STOP debe revocar la sesión de Apps Ocultas",
            shouldRevokeHiddenAppsSession(Lifecycle.Event.ON_STOP)
        )
    }

    @Test
    fun shouldRevokeHiddenAppsSession_returnsFalseForOnCreate() {
        assertFalse(
            "ON_CREATE no debe revocar la sesión de Apps Ocultas",
            shouldRevokeHiddenAppsSession(Lifecycle.Event.ON_CREATE)
        )
    }

    @Test
    fun shouldRevokeHiddenAppsSession_returnsFalseForOnStart() {
        assertFalse(
            "ON_START no debe revocar la sesión de Apps Ocultas",
            shouldRevokeHiddenAppsSession(Lifecycle.Event.ON_START)
        )
    }

    @Test
    fun shouldRevokeHiddenAppsSession_returnsFalseForOnResume() {
        assertFalse(
            "ON_RESUME no debe revocar la sesión de Apps Ocultas",
            shouldRevokeHiddenAppsSession(Lifecycle.Event.ON_RESUME)
        )
    }

    @Test
    fun shouldRevokeHiddenAppsSession_returnsFalseForOnPause() {
        assertFalse(
            "ON_PAUSE no debe revocar la sesión de Apps Ocultas",
            shouldRevokeHiddenAppsSession(Lifecycle.Event.ON_PAUSE)
        )
    }

    @Test
    fun shouldRevokeHiddenAppsSession_returnsFalseForOnDestroy() {
        assertFalse(
            "ON_DESTROY no debe revocar la sesión de Apps Ocultas",
            shouldRevokeHiddenAppsSession(Lifecycle.Event.ON_DESTROY)
        )
    }

    @Test
    fun shouldRevokeHiddenAppsSession_exhaustiveNonStopEventsReturnFalse() {
        val nonStopEvents = listOf(
            Lifecycle.Event.ON_CREATE,
            Lifecycle.Event.ON_START,
            Lifecycle.Event.ON_RESUME,
            Lifecycle.Event.ON_PAUSE,
            Lifecycle.Event.ON_DESTROY,
            Lifecycle.Event.ON_ANY
        )
        for (event in nonStopEvents) {
            assertFalse(
                "El evento $event no debe revocar la sesión",
                shouldRevokeHiddenAppsSession(event)
            )
        }
    }
}
