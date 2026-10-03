package com.daybreak.animelauncher.hiddenapps

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de dominio para la gestión del estado, persistencia y configuración de Apps ocultas.
 * Coordina la configuración en LauncherViewModel/LauncherState con el almacén criptográfico [HiddenAppsCredentialStore].
 */
class HiddenAppsRepository(
    private val credentialStore: HiddenAppsCredentialStore,
    private val getCurrentConfig: () -> HiddenAppsConfig,
    private val onUpdateConfig: (HiddenAppsConfig) -> Unit
) {

    constructor(
        context: Context,
        getCurrentConfig: () -> HiddenAppsConfig,
        onUpdateConfig: (HiddenAppsConfig) -> Unit
    ) : this(
        credentialStore = HiddenAppsCredentialStore(context),
        getCurrentConfig = getCurrentConfig,
        onUpdateConfig = onUpdateConfig
    )

    val credentialVault: HiddenAppsCredentialStore get() = credentialStore

    /**
     * Obtiene la configuración actual de Apps ocultas.
     */
    fun getConfig(): HiddenAppsConfig = getCurrentConfig()

    /**
     * Retorna el conjunto actual de paquetes ocultos.
     */
    fun getHiddenPackages(): Set<String> = getCurrentConfig().hiddenPackages

    /**
     * Oculta una aplicación agregándola a [HiddenAppsConfig.hiddenPackages].
     * Se protege contra ocultar el paquete propio de NovaLauncher.
     */
    fun hidePackage(packageName: String, ownPackageName: String? = null): Boolean {
        if (packageName.isBlank()) return false
        if (ownPackageName != null && packageName == ownPackageName) return false

        val current = getCurrentConfig()
        if (current.hiddenPackages.contains(packageName)) return true

        val updated = current.copy(hiddenPackages = current.hiddenPackages + packageName)
        onUpdateConfig(updated)
        return true
    }

    /**
     * Desoculta una aplicación removiéndola de [HiddenAppsConfig.hiddenPackages].
     */
    fun unhidePackage(packageName: String) {
        val current = getCurrentConfig()
        if (!current.hiddenPackages.contains(packageName)) return

        val updated = current.copy(hiddenPackages = current.hiddenPackages - packageName)
        onUpdateConfig(updated)
    }

    /**
     * Actualiza el método de autenticación activo.
     */
    fun setAuthMode(mode: HiddenAppsAuthMode) {
        val current = getCurrentConfig()
        val updated = current.copy(
            isConfigured = (mode != HiddenAppsAuthMode.NONE),
            authMode = mode
        )
        onUpdateConfig(updated)
    }

    /**
     * Guarda una credencial propia (PIN, patrón, contraseña) y activa el modo correspondiente.
     * El KDF (PBKDF2, intensivo en CPU) y las operaciones de bóveda se ejecutan fuera del
     * hilo principal: el llamador es una corrutina de UI y bloquear Main provocaría jank/ANR.
     */
    suspend fun saveOwnCredential(
        mode: HiddenAppsAuthMode,
        secretChars: CharArray
    ): Boolean = withContext(Dispatchers.Default) {
        try {
            val salt = HiddenAppsCrypto.generateSalt()
            val hash = HiddenAppsCrypto.deriveHash(secretChars, salt)
            val record = CredentialRecord(
                authMode = mode,
                salt = salt,
                hash = hash,
                iterations = HiddenAppsCrypto.DEFAULT_ITERATIONS
            )

            val savedInVault = credentialStore.saveCredential(record)
            if (savedInVault) {
                setAuthMode(mode)
            }
            savedInVault
        } catch (_: Exception) {
            // KDF o bóveda no disponibles (p. ej. Keystore invalidada): fail-closed sin crash.
            false
        }
    }

    /**
     * Intenta verificar la credencial propia antes de ejecutar un restablecimiento destructivo.
     * Retorna true y limpia la configuración si la credencial es correcta.
     * Retorna false y NO modifica la configuración si la credencial es incorrecta.
     */
    suspend fun verifyAndReset(
        authenticator: HiddenAppsAuthenticator,
        secretChars: CharArray
    ): Boolean {
        val verified = authenticator.verifyOwnCredential(secretChars, credentialStore)
        if (verified) {
            resetProtection()
            return true
        }
        return false
    }

    /**
     * Restablecimiento destructivo de protección:
     * - Borra el secreto criptográfico del almacén.
     * - Restablece authMode a NONE e isConfigured a false.
     * - Borra todos los hiddenPackages (reaparecen en el Drawer).
     */
    fun resetProtection() {
        credentialStore.clearCredential(deleteKeystoreKey = true)
        val resetConfig = HiddenAppsConfig(
            isConfigured = false,
            authMode = HiddenAppsAuthMode.NONE,
            hiddenPackages = emptySet()
        )
        onUpdateConfig(resetConfig)
    }

    /**
     * Limpia del conjunto de apps ocultas aquellas que ya no se encuentren instaladas en el sistema.
     */
    fun pruneUninstalledPackages(installedPackageNames: Set<String>) {
        val current = getCurrentConfig()
        val validPackages = current.hiddenPackages.filter { it in installedPackageNames }.toSet()
        if (validPackages.size != current.hiddenPackages.size) {
            onUpdateConfig(current.copy(hiddenPackages = validPackages))
        }
    }
}
