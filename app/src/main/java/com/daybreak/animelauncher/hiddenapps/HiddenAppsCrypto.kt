package com.daybreak.animelauncher.hiddenapps

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Operaciones criptográficas de derivación de claves y hashing para Apps ocultas.
 * Implementa PBKDF2WithHmacSHA256 con salt aleatorio de 16 bytes e iteraciones robustas.
 *
 * Ningún secreto, salt ni hash se imprime en logs.
 */
object HiddenAppsCrypto {

    const val DEFAULT_ITERATIONS = 120_000
    private const val HASH_KEY_LENGTH = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

    // Fallback para API 24-25, donde PBKDF2WithHmacSHA256 no está registrada.
    // Los registros de credenciales son locales al dispositivo (ligados a la clave del
    // Keystore), por lo que el algoritmo elegido en el momento de guardar siempre
    // coincide con el usado al verificar en ese mismo dispositivo.
    private const val PBKDF2_FALLBACK_ALGORITHM = "PBKDF2WithHmacSHA1"

    private val secureRandom = SecureRandom()

    /**
     * Genera un salt criptográficamente seguro de [SALT_LENGTH_BYTES] bytes.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Deriva un hash seguro de 256 bits a partir de los caracteres del secreto y un salt dado.
     * La operación es intensiva en CPU y debe ejecutarse fuera del hilo principal.
     *
     * En API 24-25 (minSdk) PBKDF2WithHmacSHA256 no está disponible y se usa
     * PBKDF2WithHmacSHA1 como fallback; el algoritmo queda determinado al guardar y
     * nunca cruza dispositivos, por lo que la verificación es consistente.
     */
    fun deriveHash(
        secretChars: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): ByteArray {
        val spec = PBEKeySpec(secretChars, salt, iterations, HASH_KEY_LENGTH)
        return try {
            val factory = try {
                SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            } catch (_: NoSuchAlgorithmException) {
                SecretKeyFactory.getInstance(PBKDF2_FALLBACK_ALGORITHM)
            }
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Verifica en tiempo constante si el secreto candidato coincide con el hash almacenado.
     */
    fun verifySecret(
        candidateChars: CharArray,
        salt: ByteArray,
        expectedHash: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): Boolean {
        val computedHash = deriveHash(candidateChars, salt, iterations)
        return MessageDigest.isEqual(computedHash, expectedHash)
    }

    /**
     * Valida que un PIN cumpla con los requisitos mínimos:
     * - Solo dígitos numéricos
     * - Longitud entre 6 y 12
     */
    fun validatePin(pin: String): Boolean {
        return pin.length in 6..12 && pin.all { it.isDigit() }
    }

    /**
     * Valida que una contraseña cumpla con los requisitos:
     * - Longitud entre 8 y 64 caracteres
     */
    fun validatePassword(password: String): Boolean {
        return password.length in 8..64
    }

    /**
     * Valida que un patrón de cuadrícula 3x3 cumpla con los requisitos:
     * - Entre 4 y 9 puntos
     * - Todos los puntos en el rango 0..8
     * - Sin puntos duplicados en el recorrido
     */
    fun validatePattern(pattern: List<Int>): Boolean {
        if (pattern.size !in 4..9) return false
        if (pattern.any { it !in 0..8 }) return false
        return pattern.distinct().size == pattern.size
    }

    /**
     * Serializa un patrón de desbloqueo como una representación determinista.
     * Ejemplo: [0, 4, 8, 6] -> "0,4,8,6"
     */
    fun serializePattern(pattern: List<Int>): String {
        return pattern.joinToString(separator = ",")
    }

    /**
     * Sobrescribe un array de caracteres con ceros para reducir la exposición en memoria.
     */
    fun wipe(chars: CharArray) {
        chars.fill('0')
    }
}
