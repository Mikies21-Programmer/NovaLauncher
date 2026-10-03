package com.daybreak.animelauncher.hiddenapps

/**
 * Modos de autenticación soportados para Apps ocultas en NovaLauncher.
 * Únicamente un modo puede estar activo a la vez.
 */
enum class HiddenAppsAuthMode {
    NONE,
    BIOMETRIC,
    DEVICE_CREDENTIAL,
    OWN_PIN,
    OWN_PATTERN,
    OWN_PASSWORD
}

/**
 * Configuración funcional de Apps ocultas persistida en [com.daybreak.animelauncher.LauncherState].
 * Cumple con serialización Gson y retrocompatibilidad con valores por defecto.
 *
 * @param isConfigured Indica si el usuario ya configuró un método de protección.
 * @param authMode Método de autenticación activo seleccionado.
 * @param hiddenPackages Conjunto de packageNames que deben ocultarse del App Drawer y búsqueda.
 */
data class HiddenAppsConfig(
    val isConfigured: Boolean = false,
    val authMode: HiddenAppsAuthMode = HiddenAppsAuthMode.NONE,
    val hiddenPackages: Set<String> = emptySet()
)

/**
 * Estados del flujo de interacción de Apps ocultas.
 * Gestionado de forma local en la jerarquía de UI y nunca persistido en disco.
 */
sealed interface HiddenAppsFlowState {
    data object Closed : HiddenAppsFlowState
    data object Setup : HiddenAppsFlowState
    data object Authenticating : HiddenAppsFlowState
    data object Visible : HiddenAppsFlowState
}

/**
 * Registro de credencial cifrado en Android Keystore para credenciales propias (PIN/Patrón/Contraseña).
 * Nunca contiene el secreto original en claro.
 */
data class CredentialRecord(
    val authMode: HiddenAppsAuthMode,
    val salt: ByteArray,
    val hash: ByteArray,
    val iterations: Int = 120_000
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CredentialRecord

        if (authMode != other.authMode) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!hash.contentEquals(other.hash)) return false
        if (iterations != other.iterations) return false

        return true
    }

    override fun hashCode(): Int {
        var result = authMode.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + hash.contentHashCode()
        result = 31 * result + iterations
        return result
    }
}
