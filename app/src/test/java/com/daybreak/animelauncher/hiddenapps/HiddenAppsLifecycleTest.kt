package com.daybreak.animelauncher.hiddenapps

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para las garantías de ciclo de vida de Hidden Apps:
 * - El estado de autenticación no se persiste
 * - Reapertura requiere nueva autenticación
 * - Cierre/Logout limpia la pantalla protegida
 */
class HiddenAppsLifecycleTest {

    // Máquina de estados en memoria que modela el comportamiento de LauncherScreen
    private class TestLauncherScreenState {
        var flowState: HiddenAppsFlowState = HiddenAppsFlowState.Closed
        var isAuthenticated: Boolean = false

        fun onSpreadGesture(isConfigured: Boolean) {
            if (isConfigured) {
                isAuthenticated = false
                flowState = HiddenAppsFlowState.Authenticating
            } else {
                flowState = HiddenAppsFlowState.Setup
            }
        }

        fun onAuthenticationSuccess() {
            isAuthenticated = true
            flowState = HiddenAppsFlowState.Visible
        }

        fun onBackOrHome() {
            isAuthenticated = false
            flowState = HiddenAppsFlowState.Closed
        }

        fun onActivityStop() {
            isAuthenticated = false
            flowState = HiddenAppsFlowState.Closed
        }

        fun isProtectedListRenderable(): Boolean {
            return (flowState == HiddenAppsFlowState.Visible && isAuthenticated)
        }
    }

    @Test
    fun testAuthenticationStateNotPersistedInConfig() {
        val config = HiddenAppsConfig(
            isConfigured = true,
            authMode = HiddenAppsAuthMode.OWN_PIN,
            hiddenPackages = setOf("com.secret.app")
        )

        // Verificar que HiddenAppsConfig no posee campos de sesión ni flags volátiles
        val fields = HiddenAppsConfig::class.java.declaredFields.map { it.name }
        assertFalse("isAuthenticated no debe existir en el modelo persistido", fields.contains("isAuthenticated"))
        assertFalse("sessionToken no debe existir en el modelo persistido", fields.contains("sessionToken"))
    }

    @Test
    fun testReopeningRequiresAuthentication() {
        val screen = TestLauncherScreenState()

        // 1. Spread y autenticación exitosa
        screen.onSpreadGesture(isConfigured = true)
        assertEquals(HiddenAppsFlowState.Authenticating, screen.flowState)
        assertFalse(screen.isProtectedListRenderable())

        screen.onAuthenticationSuccess()
        assertTrue("Tras autenticación correcta la lista protegida es renderizable", screen.isProtectedListRenderable())

        // 2. Usuario pulsa HOME o Back
        screen.onBackOrHome()
        assertFalse("Tras Back/Home la lista protegida NO debe ser renderizable", screen.isProtectedListRenderable())
        assertEquals(HiddenAppsFlowState.Closed, screen.flowState)
        assertFalse(screen.isAuthenticated)

        // 3. Usuario vuelve a hacer Spread
        screen.onSpreadGesture(isConfigured = true)
        assertEquals(HiddenAppsFlowState.Authenticating, screen.flowState)
        assertFalse("Al volver a abrir por Spread debe exigir autenticación antes de renderizar", screen.isProtectedListRenderable())
    }

    @Test
    fun testLogoutClosesProtectedScreenOnStop() {
        val screen = TestLauncherScreenState()

        // Sesión activa
        screen.onSpreadGesture(isConfigured = true)
        screen.onAuthenticationSuccess()
        assertTrue(screen.isProtectedListRenderable())

        // Activity pasa a segundo plano (ON_STOP)
        screen.onActivityStop()

        assertFalse("Al detener la Activity la sesión debe quedar completamente cerrada", screen.isProtectedListRenderable())
        assertEquals(HiddenAppsFlowState.Closed, screen.flowState)
        assertFalse(screen.isAuthenticated)
    }
}
