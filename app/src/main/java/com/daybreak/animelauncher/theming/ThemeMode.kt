package com.daybreak.animelauncher.theming

/**
 * UI-1: modo de tema del launcher.
 *
 * - [MANUAL]: los colores provienen exclusivamente de [AdvancedStyleConfig]
 *   (comportamiento histórico; default para todos los usuarios existentes).
 * - [AUTO]: la paleta del fondo controla los tokens (Fase 2).
 * - [AUTO_WITH_OVERRIDES]: el fondo genera la paleta base y los campos
 *   per-elemento de AdvancedStyleConfig actúan como overrides individuales (Fase 2).
 */
enum class ThemeMode {
    AUTO,
    MANUAL,
    AUTO_WITH_OVERRIDES
}
