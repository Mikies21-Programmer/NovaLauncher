package com.daybreak.animelauncher.hiddenapps

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Almacén criptográfico aislado para credenciales de Apps ocultas utilizando Android Keystore directamente.
 *
 * Arquitectura:
 * 1. Clave AES-GCM de 256 bits no exportable almacenada en AndroidKeyStore.
 * 2. Cifrado autenticado AES/GCM/NoPadding (tag de 128 bits, IV de 12 bytes).
 * 3. Persistencia de IV y ciphertext en SharedPreferences dedicada ("nova_hidden_apps_vault"),
 *    excluida expresamente de las copias de seguridad de Android.
 * 4. No utiliza librerías obsoletas como androidx.security:security-crypto ni MasterKey.
 */
class HiddenAppsCredentialStore(
    private val context: Context,
    private val keyAlias: String = KEY_ALIAS,
    private val prefsName: String = PREFS_NAME
) {

    companion object {
        const val KEY_ALIAS = "nova_hidden_apps_master_key"
        const val PREFS_NAME = "nova_hidden_apps_vault"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private const val KEY_CIPHERTEXT = "vault_ciphertext"
        private const val KEY_IV = "vault_iv"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    /**
     * Obtiene la clave secreta existente de Android Keystore o genera una nueva no exportable si no existe.
     */
    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(keyAlias)) {
                (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Cifra y almacena un [CredentialRecord].
     * Retorna true si la operación tuvo éxito.
     */
    fun saveCredential(record: CredentialRecord): Boolean {
        return try {
            val key = getOrCreateSecretKey() ?: return false
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            // Serialización del registro: "MODE:SALT_B64:HASH_B64:ITERATIONS"
            val payload = buildString {
                append(record.authMode.name)
                append(":")
                append(Base64.encodeToString(record.salt, Base64.NO_WRAP))
                append(":")
                append(Base64.encodeToString(record.hash, Base64.NO_WRAP))
                append(":")
                append(record.iterations)
            }.toByteArray(Charsets.UTF_8)

            val ciphertext = cipher.doFinal(payload)

            prefs.edit()
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Desencripta y recupera el [CredentialRecord] almacenado.
     * Retorna null si no existe, si el ciphertext está corrupto o si la clave del Keystore fue invalidada.
     */
    fun loadCredential(): CredentialRecord? {
        val ciphertextB64 = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val ivB64 = prefs.getString(KEY_IV, null) ?: return null

        return try {
            val ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)

            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: return null

            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            val payload = String(decryptedBytes, Charsets.UTF_8)

            val parts = payload.split(":")
            if (parts.size != 4) return null

            val mode = HiddenAppsAuthMode.valueOf(parts[0])
            val salt = Base64.decode(parts[1], Base64.NO_WRAP)
            val hash = Base64.decode(parts[2], Base64.NO_WRAP)
            val iterations = parts[3].toIntOrNull() ?: HiddenAppsCrypto.DEFAULT_ITERATIONS

            CredentialRecord(
                authMode = mode,
                salt = salt,
                hash = hash,
                iterations = iterations
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Elimina el registro cifrado de SharedPreferences y, opcionalmente, la clave de Android Keystore.
     */
    fun clearCredential(deleteKeystoreKey: Boolean = false): Boolean {
        prefs.edit().clear().apply()
        if (deleteKeystoreKey) {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (keyStore.containsAlias(keyAlias)) {
                    keyStore.deleteEntry(keyAlias)
                }
            } catch (_: Exception) {
                // Silencioso ante errores al eliminar clave
            }
        }
        return true
    }

    /**
     * Comprueba si existe un registro de credencial cifrado en disco.
     */
    fun hasStoredCredential(): Boolean {
        return prefs.contains(KEY_CIPHERTEXT) && prefs.contains(KEY_IV)
    }
}
