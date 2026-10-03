package com.daybreak.animelauncher.hiddenapps

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio aislado para la gestión de autenticación, control de intentos fallidos
 * y cooldown temporal para Apps ocultas.
 */
class HiddenAppsAuthenticator(
    private val maxFailedAttempts: Int = MAX_FAILED_ATTEMPTS,
    private val cooldownDurationMs: Long = COOLDOWN_DURATION_MS
) {

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val COOLDOWN_DURATION_MS = 30_000L
    }

    private var consecutiveFailedAttempts = 0
    private var cooldownUntilMillis = 0L

    /**
     * Retorna si el bloqueo temporal (cooldown) por demasiados intentos fallidos está activo.
     */
    fun isCooldownActive(): Boolean {
        val now = System.currentTimeMillis()
        if (now < cooldownUntilMillis) {
            return true
        }
        if (cooldownUntilMillis > 0L) {
            // Cooldown expiró
            cooldownUntilMillis = 0L
        }
        return false
    }

    /**
     * Retorna los segundos restantes del cooldown activo (0 si no está activo).
     */
    fun getRemainingCooldownSeconds(): Long {
        val now = System.currentTimeMillis()
        val remaining = (cooldownUntilMillis - now + 999L) / 1000L
        return remaining.coerceAtLeast(0L)
    }

    /**
     * Registra un intento de autenticación fallido.
     * Si supera [maxFailedAttempts], activa el periodo de cooldown.
     * Retorna true si se activó el cooldown con este fallo.
     */
    fun onAuthenticationFailed(): Boolean {
        consecutiveFailedAttempts++
        if (consecutiveFailedAttempts >= maxFailedAttempts) {
            cooldownUntilMillis = System.currentTimeMillis() + cooldownDurationMs
            return true
        }
        return false
    }

    /**
     * Restablece el contador de intentos fallidos tras una autenticación exitosa.
     */
    fun onAuthenticationSucceeded() {
        consecutiveFailedAttempts = 0
        cooldownUntilMillis = 0L
    }

    /**
     * Cantidad actual de intentos fallidos consecutivos en memoria.
     */
    fun getFailedAttempts(): Int = consecutiveFailedAttempts

    /**
     * Verifica una credencial propia (PIN, patrón o contraseña) fuera del hilo principal.
     */
    suspend fun verifyOwnCredential(
        secretChars: CharArray,
        credentialStore: HiddenAppsCredentialStore
    ): Boolean = withContext(Dispatchers.Default) {
        if (isCooldownActive()) {
            return@withContext false
        }

        val record = credentialStore.loadCredential() ?: run {
            onAuthenticationFailed()
            return@withContext false
        }

        val verified = HiddenAppsCrypto.verifySecret(
            candidateChars = secretChars,
            salt = record.salt,
            expectedHash = record.hash,
            iterations = record.iterations
        )

        if (verified) {
            onAuthenticationSucceeded()
        } else {
            onAuthenticationFailed()
        }

        verified
    }

    /**
     * Comprueba si la biometría fuerte del sistema está disponible y registrada en el dispositivo.
     */
    fun canAuthenticateBiometric(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                    BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Comprueba si el dispositivo tiene configurada una credencial segura de bloqueo (PIN/patrón/contraseña).
     */
    fun isDeviceSecure(context: Context): Boolean {
        return try {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.isDeviceSecure ?: false
        } catch (_: Exception) {
            false
        }
    }
}
