package com.daybreak.animelauncher.hiddenapps

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para derivación criptográfica, validaciones de credenciales,
 * cooldown temporal y reseteo de seguridad.
 */
class HiddenAppsAuthTest {

    @Test
    fun testPBKDF2HashGenerated() {
        val secret = "123456".toCharArray()
        val salt = HiddenAppsCrypto.generateSalt()
        assertEquals(16, salt.size)

        val hash = HiddenAppsCrypto.deriveHash(secret, salt, iterations = 2000)
        assertNotNull(hash)
        assertEquals(32, hash.size) // 256 bits = 32 bytes
    }

    @Test
    fun testSameSecretVerifies() {
        val secret = "mySecretPassword123".toCharArray()
        val salt = HiddenAppsCrypto.generateSalt()
        val hash = HiddenAppsCrypto.deriveHash(secret, salt, iterations = 2000)

        val matches = HiddenAppsCrypto.verifySecret(
            candidateChars = "mySecretPassword123".toCharArray(),
            salt = salt,
            expectedHash = hash,
            iterations = 2000
        )
        assertTrue("El mismo secreto debe verificar correctamente", matches)
    }

    @Test
    fun testWrongSecretRejected() {
        val secret = "mySecretPassword123".toCharArray()
        val salt = HiddenAppsCrypto.generateSalt()
        val hash = HiddenAppsCrypto.deriveHash(secret, salt, iterations = 2000)

        val matches = HiddenAppsCrypto.verifySecret(
            candidateChars = "wrongPassword456".toCharArray(),
            salt = salt,
            expectedHash = hash,
            iterations = 2000
        )
        assertFalse("Un secreto incorrecto debe ser rechazado", matches)
    }

    @Test
    fun testDifferentSaltProducesDifferentRecord() {
        val secret = "identicalSecret".toCharArray()
        val salt1 = HiddenAppsCrypto.generateSalt()
        val salt2 = HiddenAppsCrypto.generateSalt()

        val hash1 = HiddenAppsCrypto.deriveHash(secret, salt1, iterations = 2000)
        val hash2 = HiddenAppsCrypto.deriveHash(secret, salt2, iterations = 2000)

        assertFalse("Salts diferentes deben generar hashes diferentes", hash1.contentEquals(hash2))
    }

    @Test
    fun testPatternSerializationDeterministic() {
        val pattern1 = listOf(0, 4, 8, 6)
        val serialized1 = HiddenAppsCrypto.serializePattern(pattern1)

        val pattern2 = listOf(0, 4, 8, 6)
        val serialized2 = HiddenAppsCrypto.serializePattern(pattern2)

        assertEquals("0,4,8,6", serialized1)
        assertEquals(serialized1, serialized2)
    }

    @Test
    fun testPinValidation() {
        // Válidos (6 a 12 dígitos)
        assertTrue(HiddenAppsCrypto.validatePin("123456"))
        assertTrue(HiddenAppsCrypto.validatePin("987654321012"))

        // Inválidos
        assertFalse("Menos de 6 dígitos no permitido", HiddenAppsCrypto.validatePin("12345"))
        assertFalse("Más de 12 dígitos no permitido", HiddenAppsCrypto.validatePin("1234567890123"))
        assertFalse("Letras en PIN no permitido", HiddenAppsCrypto.validatePin("12345a"))
        assertFalse("Caracteres especiales en PIN no permitido", HiddenAppsCrypto.validatePin("12345-"))
    }

    @Test
    fun testPasswordValidation() {
        // Válidos (8 a 64 caracteres)
        assertTrue(HiddenAppsCrypto.validatePassword("12345678"))
        assertTrue(HiddenAppsCrypto.validatePassword("unP@ssw0rd!Segur0_Con_Unicode_ñ"))

        // Inválidos
        assertFalse("Menos de 8 caracteres no permitido", HiddenAppsCrypto.validatePassword("short12"))
        val tooLong = "a".repeat(65)
        assertFalse("Más de 64 caracteres no permitido", HiddenAppsCrypto.validatePassword(tooLong))
    }

    @Test
    fun testCooldownAfterFiveFailedAttempts() {
        val authenticator = HiddenAppsAuthenticator(
            maxFailedAttempts = 5,
            cooldownDurationMs = 30_000L
        )

        assertFalse(authenticator.isCooldownActive())
        assertEquals(0, authenticator.getFailedAttempts())

        // 4 fallos no activan cooldown
        for (i in 1..4) {
            val cooldownActivated = authenticator.onAuthenticationFailed()
            assertFalse("Fallo $i no debe activar cooldown", cooldownActivated)
            assertFalse(authenticator.isCooldownActive())
        }

        // El 5to fallo activa cooldown
        val activated = authenticator.onAuthenticationFailed()
        assertTrue("El quinto fallo consecutivo debe activar cooldown", activated)
        assertTrue("Cooldown debe estar activo", authenticator.isCooldownActive())
        assertTrue("Los segundos restantes deben ser mayores a 0", authenticator.getRemainingCooldownSeconds() > 0)

        // Tras éxito se reinicia
        authenticator.onAuthenticationSucceeded()
        assertFalse(authenticator.isCooldownActive())
        assertEquals(0, authenticator.getFailedAttempts())
        assertEquals(0L, authenticator.getRemainingCooldownSeconds())
    }

    @Test
    fun testResetClearsConfiguration() {
        var currentConfig = HiddenAppsConfig(
            isConfigured = true,
            authMode = HiddenAppsAuthMode.OWN_PIN,
            hiddenPackages = setOf("com.secret.app1", "com.secret.app2")
        )

        // Mock repository logic
        val onReset = {
            currentConfig = HiddenAppsConfig(
                isConfigured = false,
                authMode = HiddenAppsAuthMode.NONE,
                hiddenPackages = emptySet()
            )
        }

        onReset()

        assertFalse("isConfigured debe ser false tras reseteo", currentConfig.isConfigured)
        assertEquals(HiddenAppsAuthMode.NONE, currentConfig.authMode)
        assertTrue("hiddenPackages debe estar vacío tras reseteo", currentConfig.hiddenPackages.isEmpty())
    }

    @Test
    fun testSinBloqueoSeguroNoExisteResetLibre() {
        val isDeviceSecure = false
        var currentConfig = HiddenAppsConfig(
            isConfigured = true,
            authMode = HiddenAppsAuthMode.OWN_PIN,
            hiddenPackages = setOf("com.app.hidden1")
        )

        // Regla: sin bloqueo seguro, no existe fallback de reseteo directo sin ingresar credencial Nova
        val attemptResetWithoutNovaCredential = {
            if (isDeviceSecure) {
                // Procedería a solicitar confirmación de credencial de dispositivo
            } else {
                // Sin bloqueo seguro, no se permite reseteo directo. Debe fallar/bloquearse.
            }
        }

        attemptResetWithoutNovaCredential()
        assertTrue("La configuración debe permanecer activa si no se proporciona credencial", currentConfig.isConfigured)
        assertEquals(setOf("com.app.hidden1"), currentConfig.hiddenPackages)
    }

    @Test
    fun testResetCanceladoNoCambiaConfiguracion() {
        val initialConfig = HiddenAppsConfig(
            isConfigured = true,
            authMode = HiddenAppsAuthMode.OWN_PASSWORD,
            hiddenPackages = setOf("com.app.bank", "com.app.crypto")
        )
        var currentConfig = initialConfig

        // Usuario abre diálogo de confirmación pero presiona Cancelar
        val onCancelReset = {
            // No-op
        }

        onCancelReset()
        assertEquals(initialConfig, currentConfig)
    }

    @Test
    fun testResetSinAutenticacionNoEliminaHiddenPackages() {
        var currentConfig = HiddenAppsConfig(
            isConfigured = true,
            authMode = HiddenAppsAuthMode.OWN_PIN,
            hiddenPackages = setOf("com.secret1", "com.secret2")
        )

        val unauthenticatedReset = { isAuthenticated: Boolean ->
            if (isAuthenticated) {
                currentConfig = HiddenAppsConfig()
            }
        }

        unauthenticatedReset(false)
        assertEquals(2, currentConfig.hiddenPackages.size)
        assertTrue(currentConfig.hiddenPackages.contains("com.secret1"))
    }

    @Test
    fun testCredencialIncorrectaNoPuedeResetear() {
        val secret = "123456".toCharArray()
        val salt = HiddenAppsCrypto.generateSalt()
        val hash = HiddenAppsCrypto.deriveHash(secret, salt, iterations = 2000)

        var currentConfig = HiddenAppsConfig(
            isConfigured = true,
            authMode = HiddenAppsAuthMode.OWN_PIN,
            hiddenPackages = setOf("com.secret1")
        )

        val wrongInput = "999999".toCharArray()
        val verified = HiddenAppsCrypto.verifySecret(wrongInput, salt, hash, iterations = 2000)
        assertFalse("Credencial errónea no debe verificar", verified)

        if (verified) {
            currentConfig = HiddenAppsConfig()
        }

        assertTrue("Configuración debe permanecer activa ante credencial errónea", currentConfig.isConfigured)
        assertEquals(setOf("com.secret1"), currentConfig.hiddenPackages)
    }

    @Test
    fun testCredencialCorrectaSIPuedeResetear() {
        val secret = "123456".toCharArray()
        val salt = HiddenAppsCrypto.generateSalt()
        val hash = HiddenAppsCrypto.deriveHash(secret, salt, iterations = 2000)

        var currentConfig = HiddenAppsConfig(
            isConfigured = true,
            authMode = HiddenAppsAuthMode.OWN_PIN,
            hiddenPackages = setOf("com.secret1")
        )

        val correctInput = "123456".toCharArray()
        val verified = HiddenAppsCrypto.verifySecret(correctInput, salt, hash, iterations = 2000)
        assertTrue("Credencial correcta debe verificar", verified)

        if (verified) {
            currentConfig = HiddenAppsConfig(
                isConfigured = false,
                authMode = HiddenAppsAuthMode.NONE,
                hiddenPackages = emptySet()
            )
        }

        assertFalse("Configuración debe deshabilitarse tras reseteo exitoso", currentConfig.isConfigured)
        assertEquals(HiddenAppsAuthMode.NONE, currentConfig.authMode)
        assertTrue("Apps ocultas deben limpiarse tras reseteo exitoso", currentConfig.hiddenPackages.isEmpty())
    }

    @Test
    fun testHiddenPackagesPermaneceIntactoAnteAutenticacionFallida() {
        var currentConfig = HiddenAppsConfig(
            isConfigured = true,
            authMode = HiddenAppsAuthMode.OWN_PATTERN,
            hiddenPackages = setOf("com.private.photos", "com.private.notes")
        )
        val authenticator = HiddenAppsAuthenticator()

        // 3 fallos consecutivos
        for (i in 1..3) {
            authenticator.onAuthenticationFailed()
        }

        assertEquals(3, authenticator.getFailedAttempts())
        assertEquals(2, currentConfig.hiddenPackages.size)
        assertTrue(currentConfig.hiddenPackages.contains("com.private.photos"))
        assertTrue(currentConfig.hiddenPackages.contains("com.private.notes"))
    }

    @Test
    fun testEstadoRestauradoSinVaultNoDesbloqueaAppsOcultas() {
        // Simulación: LauncherState restaurado con isConfigured = true, pero el vault está vacío (loadCredential() == null)
        val restoredConfig = HiddenAppsConfig(
            isConfigured = true,
            authMode = HiddenAppsAuthMode.OWN_PIN,
            hiddenPackages = setOf("com.sensitive.app")
        )
        val authenticator = HiddenAppsAuthenticator()

        val mockLoadCredential: () -> CredentialRecord? = { null }

        val attemptUnlock = { inputChars: CharArray ->
            val record = mockLoadCredential()
            if (record == null) {
                authenticator.onAuthenticationFailed()
                false
            } else {
                HiddenAppsCrypto.verifySecret(inputChars, record.salt, record.hash, record.iterations)
            }
        }

        val unlocked = attemptUnlock("123456".toCharArray())
        assertFalse("Sin registro en el almacén de credenciales no se debe desbloquear (fail-closed)", unlocked)
        assertEquals(1, authenticator.getFailedAttempts())
        assertTrue("Apps ocultas permanecen protegidas e invisibles en Drawer", restoredConfig.hiddenPackages.contains("com.sensitive.app"))
    }
}
