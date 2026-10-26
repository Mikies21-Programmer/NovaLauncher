package com.daybreak.animelauncher.ui.screens

import androidx.lifecycle.Lifecycle

/**
 * Política de ciclo de vida para la gestión segura de la sesión de Apps Ocultas.
 *
 * Devuelve `true` exclusivamente cuando el evento del ciclo de vida exige
 * la revocación inmediata de la sesión autenticada (fail-closed al pasar a segundo plano / detenerse la Activity).
 */
internal fun shouldRevokeHiddenAppsSession(event: Lifecycle.Event): Boolean {
    return event == Lifecycle.Event.ON_STOP
}
