package com.daybreak.animelauncher.hiddenapps

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * Gestor de migración y resolución del estado de Hidden Apps aislado de Android Backup.
 *
 * Distingue de forma robusta:
 * - Caso A (Actualización local): usuario existente en el mismo dispositivo conservando contexto de seguridad.
 * - Caso B (Instalación nueva / restore huérfano): datos restaurados desde un backup antiguo sin contexto de seguridad local válido.
 */
object HiddenAppsMigrator {

    /**
     * Marcador de instalación local, persistido en [Context.getNoBackupFilesDir].
     *
     * Señal de generación local NO falsificable por backup:
     * - Se crea en cada resolución (idempotente) desde la era de aislamiento.
     * - Sobrevive a `adb install -r` (el directorio de datos se conserva en upgrades).
     * - Se destruye con `pm clear` y en instalación nueva.
     * - La plataforma NUNCA incluye `noBackupFilesDir` en payloads de backup
     *   (full-backup, key-value, cloud-backup ni device-to-device), por lo que un
     *   restore no puede crearlo ni restaurarlo.
     *
     * No se usa como condición de migración en la migración única legacy (la
     * generación v1.6.0-stable no lo creaba y exigirlo rompería upgrades legítimos
     * ya validados); queda como evidencia durable y gancho de endurecimiento futuro.
     */
    const val LOCAL_INSTALL_MARKER_NAME = "nova_local_install.marker"

    fun markerFile(noBackupDir: File?): File? = noBackupDir?.let { File(it, LOCAL_INSTALL_MARKER_NAME) }

    fun hasLocalInstallMarker(noBackupDir: File?): Boolean =
        markerFile(noBackupDir)?.exists() == true

    fun ensureLocalInstallMarker(noBackupDir: File?) {
        try {
            markerFile(noBackupDir)?.let { file ->
                if (!file.exists()) {
                    noBackupDir?.mkdirs()
                    file.createNewFile()
                }
            }
        } catch (_: Exception) {
            // La señal es best-effort: nunca debe impedir la resolución de configuración.
        }
    }

    /**
     * Determina si la ejecución actual corresponde a una actualización de la aplicación
     * en el mismo dispositivo físico basándose en los metadatos de instalación del sistema.
     */
    fun isLocalUpgrade(context: Context): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.lastUpdateTime > packageInfo.firstInstallTime
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Resuelve y migra la configuración de Hidden Apps asegurando aislamiento estricto de backup.
     *
     * @param context Contexto de la aplicación (opcional para entornos de test unitario).
     * @param prefsStore Almacén aislado dedicado a Hidden Apps.
     * @param credentialStore Bóveda criptográfica Keystore de Hidden Apps.
     * @param authenticator Servicio de autenticación para comprobar capacidades biométricas / bloqueo.
     * @param legacyConfig Configuración obtenida del JSON histórico de launcher_prefs (si existe).
     * @param isLocalUpgradeOverride Sobrescritura opcional para pruebas unitarias deterministas.
     * @param biometricAvailableOverride Sobrescritura de la comprobación biométrica (tests).
     * @param deviceSecureOverride Sobrescritura de la comprobación de bloqueo seguro (tests).
     * @param noBackupDirOverride Directorio no-respaldable para el marcador local (tests).
     * @param onPurgeLegacyConfig Callback para eliminar la configuración residual de launcher_prefs.
     */
    fun resolveConfig(
        context: Context?,
        prefsStore: HiddenAppsPreferencesStore,
        credentialStore: HiddenAppsCredentialStore?,
        authenticator: HiddenAppsAuthenticator,
        legacyConfig: HiddenAppsConfig?,
        isLocalUpgradeOverride: (() -> Boolean)? = null,
        biometricAvailableOverride: (() -> Boolean)? = null,
        deviceSecureOverride: (() -> Boolean)? = null,
        noBackupDirOverride: File? = null,
        onPurgeLegacyConfig: () -> Unit
    ): HiddenAppsConfig {
        // 1. Si el nuevo almacenamiento aislado ya existe en disco, es la única fuente de verdad.
        //    Un restore de backup NO puede traerlo (excluido) ni sobrescribirlo, por lo que un
        //    launcher_prefs legacy que aparezca con el store presente es necesariamente foráneo
        //    (restore sobre instalación existente) y se purga sin evaluarlo.
        if (prefsStore.hasPreferences()) {
            val config = prefsStore.loadConfig()
            if (legacyConfig != null && legacyConfig.isConfigured) {
                onPurgeLegacyConfig()
            }
            ensureLocalInstallMarker(noBackupDirOverride ?: context?.noBackupFilesDir)
            return config
        }

        // 2. Si no hay almacenamiento nuevo ni configuración antigua válida: estado inicial limpio
        if (legacyConfig == null || !legacyConfig.isConfigured || legacyConfig.authMode == HiddenAppsAuthMode.NONE) {
            val emptyConfig = HiddenAppsConfig()
            prefsStore.saveConfig(emptyConfig)
            ensureLocalInstallMarker(noBackupDirOverride ?: context?.noBackupFilesDir)
            return emptyConfig
        }

        // 3. Evaluación del contexto de seguridad local
        val isUpgrade = isLocalUpgradeOverride?.invoke()
            ?: (context?.let { isLocalUpgrade(it) } ?: false)

        val isValidContext = when (legacyConfig.authMode) {
            HiddenAppsAuthMode.OWN_PIN,
            HiddenAppsAuthMode.OWN_PATTERN,
            HiddenAppsAuthMode.OWN_PASSWORD -> {
                // Modos propios exigen que la clave en Keystore y la bóveda física existan
                // y puedan descifrarse correctamente en este dispositivo.
                // En un restore de backup, el vault no existe -> loadCredential() retorna null.
                credentialStore?.loadCredential() != null
            }
            HiddenAppsAuthMode.BIOMETRIC -> {
                // Biometría exige que sea actualización sobre la misma instalación
                // Y que el hardware biométrico esté disponible y enrolado en este dispositivo.
                isUpgrade && (biometricAvailableOverride?.invoke()
                    ?: (context?.let { authenticator.canAuthenticateBiometric(it) } ?: false))
            }
            HiddenAppsAuthMode.DEVICE_CREDENTIAL -> {
                // Credencial de dispositivo exige actualización sobre la misma instalación
                // Y que el dispositivo cuente con bloqueo de pantalla seguro configurado.
                isUpgrade && (deviceSecureOverride?.invoke()
                    ?: (context?.let { authenticator.isDeviceSecure(it) } ?: false))
            }
            HiddenAppsAuthMode.NONE -> false
        }

        val resolved = if (isValidContext) {
            // Caso A: Usuario existente actualizando la app con contexto local válido.
            // Migrar al almacenamiento aislado y purgar de launcher_prefs.
            prefsStore.saveConfig(legacyConfig)
            onPurgeLegacyConfig()
            legacyConfig
        } else {
            // Caso B: Restore antiguo / instalación nueva sin contexto válido.
            // NO reactivar para evitar lockout y apps atrapadas.
            val safeConfig = HiddenAppsConfig()
            prefsStore.saveConfig(safeConfig)
            onPurgeLegacyConfig()
            safeConfig
        }

        ensureLocalInstallMarker(noBackupDirOverride ?: context?.noBackupFilesDir)
        return resolved
    }
}
