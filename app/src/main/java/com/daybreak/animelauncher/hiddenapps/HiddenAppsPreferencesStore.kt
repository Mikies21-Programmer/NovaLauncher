package com.daybreak.animelauncher.hiddenapps

import android.content.Context
import android.content.SharedPreferences

/**
 * Almacén de configuración funcional de Apps Ocultas aislado del sistema de backup de Android.
 * Persiste exclusivamente en [PREFS_NAME] ("nova_hidden_apps_prefs.xml"),
 * archivo expresamente excluido de cloud-backup, device-transfer y legacy backups.
 */
class HiddenAppsPreferencesStore(
    private val prefs: SharedPreferences
) {
    constructor(context: Context, prefsName: String = PREFS_NAME) : this(
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    )

    companion object {
        const val PREFS_NAME = "nova_hidden_apps_prefs"
        const val KEY_IS_CONFIGURED = "is_configured"
        const val KEY_AUTH_MODE = "auth_mode"
        const val KEY_HIDDEN_PACKAGES = "hidden_packages"
    }

    /**
     * Comprueba si el almacenamiento dedicado ya existe y contiene configuración inicializada.
     */
    fun hasPreferences(): Boolean {
        return prefs.contains(KEY_IS_CONFIGURED) || prefs.contains(KEY_AUTH_MODE)
    }

    /**
     * Carga la configuración funcional de Apps Ocultas desde el almacenamiento aislado.
     */
    fun loadConfig(): HiddenAppsConfig {
        if (!hasPreferences()) return HiddenAppsConfig()
        val isConfigured = prefs.getBoolean(KEY_IS_CONFIGURED, false)
        val modeStr = prefs.getString(KEY_AUTH_MODE, HiddenAppsAuthMode.NONE.name)
        val authMode = try {
            HiddenAppsAuthMode.valueOf(modeStr ?: HiddenAppsAuthMode.NONE.name)
        } catch (_: Exception) {
            HiddenAppsAuthMode.NONE
        }
        val hiddenPackages = prefs.getStringSet(KEY_HIDDEN_PACKAGES, emptySet())?.toSet() ?: emptySet()
        return HiddenAppsConfig(
            isConfigured = isConfigured,
            authMode = authMode,
            hiddenPackages = hiddenPackages
        )
    }

    /**
     * Guarda atómicamente la configuración funcional en el almacenamiento aislado.
     */
    fun saveConfig(config: HiddenAppsConfig) {
        prefs.edit()
            .putBoolean(KEY_IS_CONFIGURED, config.isConfigured)
            .putString(KEY_AUTH_MODE, config.authMode.name)
            .putStringSet(KEY_HIDDEN_PACKAGES, HashSet(config.hiddenPackages))
            .apply()
    }

    /**
     * Limpia completamente el archivo de preferencias de Apps Ocultas.
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
