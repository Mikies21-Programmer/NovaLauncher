package com.daybreak.animelauncher.ui.screens

import com.daybreak.animelauncher.AppShortcut

/**
 * Fuente única de verdad para la navegación interna del Launcher.
 * Reemplaza banderas booleanas sueltas y garantiza una jerarquía de retroceso
 * estricta, predecible y desacoplada del servicio de accesibilidad.
 */
sealed interface LauncherNavState {
    // Pantalla de inicio normal (Home)
    data object Home : LauncherNavState

    // App Drawer y sus diálogos/submenús internos
    sealed interface InDrawer : LauncherNavState {
        data object Main : InDrawer
        data class AppMenu(val app: AppShortcut) : InDrawer
        data class IconPicker(val app: AppShortcut) : InDrawer
        data object AddCategory : InDrawer
    }

    // Menús contextuales y selectores del Launcher
    data class LongPressMenu(val pageIndex: Int) : LauncherNavState
    data class BackgroundSelection(val pageIndex: Int) : LauncherNavState
    data class WidgetPicker(val pageIndex: Int) : LauncherNavState
}
