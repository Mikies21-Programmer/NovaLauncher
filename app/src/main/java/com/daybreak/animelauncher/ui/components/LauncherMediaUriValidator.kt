package com.daybreak.animelauncher.ui.components

/**
 * Validador estricto y puro de esquemas URI para medios locales (fondos, videos e iconos).
 *
 * Arquitectura de confinamiento local:
 * - El launcher opera de forma 100% confinada al almacenamiento local y a los activos del APK.
 * - Rechaza proactivamente esquemas remotos (HTTP, HTTPS, FTP) para evitar trÃ¡fico de red saliente
 *   a travÃ©s de Coil o ExoPlayer.
 * - Restringe el esquema `file://` exclusivamente al prefijo `/android_asset/` para impedir la lectura
 *   de archivos privados arbitrarios del sistema de archivos.
 * - Es una funciÃ³n pura y desacoplada del framework de Android, ejecutable y verificable en JVM estÃ¡ndar.
 */
object LauncherMediaUriValidator {

    private val ALLOWED_SCHEMES = setOf("content", "android.resource", "file")

    /**
     * Determina si la URI proporcionada corresponde a un medio local legÃ­timo y permitido.
     *
     * @param uriString Cadena de texto que representa el URI a evaluar.
     * @return `true` si el URI pertenece a un esquema local seguro (`content://`, `android.resource://`,
     *         o `file:///android_asset/`), `false` en caso contrario.
     */
    fun isAllowedMediaUri(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val trimmed = uriString.trim()
        val colonIdx = trimmed.indexOf(':')
        if (colonIdx <= 0) return false

        val scheme = trimmed.substring(0, colonIdx).lowercase()
        if (scheme !in ALLOWED_SCHEMES) return false

        return when (scheme) {
            "content" -> trimmed.startsWith("content://", ignoreCase = true) && trimmed.length > 10
            "android.resource" -> trimmed.startsWith("android.resource://", ignoreCase = true) && trimmed.length > 19
            "file" -> trimmed.startsWith("file:///android_asset/", ignoreCase = true) && trimmed.length > 22
            else -> false
        }
    }
}

