package com.daybreak.animelauncher.ui.screens.hiddenapps

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.daybreak.animelauncher.R
import com.daybreak.animelauncher.hiddenapps.*
import com.daybreak.animelauncher.theming.LocalLauncherThemeTokens
import com.daybreak.animelauncher.theming.accent
import com.daybreak.animelauncher.theming.border
import com.daybreak.animelauncher.theming.surface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Diálogo unificado de autenticación para Apps ocultas.
 * Gestiona Biometría, Credencial de dispositivo y credenciales propias (PIN/Patrón/Contraseña)
 * con control de intentos fallidos, cooldown temporal y flujo de restablecimiento seguro.
 */
@Composable
fun HiddenAppsAuthDialog(
    authMode: HiddenAppsAuthMode,
    authenticator: HiddenAppsAuthenticator,
    credentialStore: HiddenAppsCredentialStore,
    deviceCredentialLauncher: ActivityResultLauncher<Intent>,
    resetDeviceCredentialLauncher: ActivityResultLauncher<Intent>,
    credentialResultSink: MutableState<((ActivityResult) -> Unit)?>,
    resetResultSink: MutableState<((ActivityResult) -> Unit)?>,
    onAuthenticated: () -> Unit,
    onResetRequested: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // LocalContext aquí es el contexto de configuración del bloque de idioma de LauncherScreen,
    // nunca la Activity: el FragmentActivity que exige BiometricPrompt sale del owner fijado
    // en LauncherScreen (regresión #11 del informe de estabilización).
    val fragmentActivity = LocalActivityResultRegistryOwner.current as? FragmentActivity
        ?: context as? FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val themeTokens = LocalLauncherThemeTokens.current

    val strAuthFailed = stringResource(R.string.hidden_apps_auth_failed)
    val strPromptTitle = stringResource(R.string.hidden_apps_biometric_prompt_title)
    val strPromptSubtitle = stringResource(R.string.hidden_apps_biometric_prompt_subtitle)
    val strPromptCancel = stringResource(R.string.hidden_apps_biometric_prompt_cancel)
    val strResetTitle = stringResource(R.string.hidden_apps_reset_dialog_title)
    val strResetDesc = stringResource(R.string.hidden_apps_reset_dialog_desc)
    val strResetAuthTitle = stringResource(R.string.hidden_apps_reset_auth_title)
    val strResetAuthDesc = stringResource(R.string.hidden_apps_reset_auth_desc)

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cooldownRemaining by remember { mutableLongStateOf(authenticator.getRemainingCooldownSeconds()) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var isResetMode by remember { mutableStateOf(false) }

    var pinValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }

    // HALLAZGO 2 (revisado P0-B): el contenido sensible vive en la ventana del propio
    // Dialog, que ya es segura vía SecureFlagPolicy.SecureOn. Marcar la ventana de la
    // Activity HOME con FLAG_SECURE ennegrecía la superficie del launcher durante
    // transiciones del sistema (cambio de launcher predeterminado, recents) sin añadir
    // protección real, y es sospechoso del freeze percibido (superficie negra/pegada).
    // El FLAG_SECURE de pantalla completa permanece únicamente en HiddenAppsScreen.

    // Actualizador periódico del contador de cooldown si está activo
    LaunchedEffect(authenticator.isCooldownActive()) {
        while (authenticator.isCooldownActive()) {
            cooldownRemaining = authenticator.getRemainingCooldownSeconds()
            delay(1000L)
        }
        cooldownRemaining = 0L
    }

    // Los ActivityResultLauncher NO se crean aquí: dentro del contenido de un Compose
    // Dialog no resuelve LocalActivityResultRegistryOwner y rememberLauncherForActivityResult
    // lanza IllegalStateException (crash del launcher HOME observado en el incidente
    // original y reproducido el 02-oct 15:31/15:32). Los launchers viven en LauncherScreen
    // (scope de la Activity); este diálogo solo registra el manejador de resultado mientras
    // está compuesto.
    DisposableEffect(Unit) {
        credentialResultSink.value = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                authenticator.onAuthenticationSucceeded()
                onAuthenticated()
            } else {
                errorMessage = strAuthFailed
            }
        }
        resetResultSink.value = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onResetRequested()
            } else {
                errorMessage = strAuthFailed
            }
        }
        onDispose {
            credentialResultSink.value = null
            resetResultSink.value = null
        }
    }

    // Lanzamiento automático para modos de sistema (Biometría o Credencial del sistema)
    LaunchedEffect(authMode) {
        if (fragmentActivity != null) {
            when (authMode) {
                HiddenAppsAuthMode.BIOMETRIC -> {
                    val executor = ContextCompat.getMainExecutor(context)
                    val prompt = BiometricPrompt(fragmentActivity, executor, object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            authenticator.onAuthenticationSucceeded()
                            onAuthenticated()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                            ) {
                                onDismiss()
                            } else {
                                errorMessage = errString.toString()
                            }
                        }
                    })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(strPromptTitle)
                        .setSubtitle(strPromptSubtitle)
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .setNegativeButtonText(strPromptCancel)
                        .build()

                    try {
                        prompt.authenticate(promptInfo)
                    } catch (_: Exception) {
                        errorMessage = strAuthFailed
                    }
                }

                HiddenAppsAuthMode.DEVICE_CREDENTIAL -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val executor = ContextCompat.getMainExecutor(context)
                        val prompt = BiometricPrompt(fragmentActivity, executor, object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                authenticator.onAuthenticationSucceeded()
                                onAuthenticated()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                ) {
                                    onDismiss()
                                } else {
                                    errorMessage = errString.toString()
                                }
                            }
                        })

                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle(strPromptTitle)
                            .setSubtitle(strPromptSubtitle)
                            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            .build()

                        try {
                            prompt.authenticate(promptInfo)
                        } catch (_: Exception) {
                            errorMessage = strAuthFailed
                        }
                    } else {
                        // Fallback API 24-29 vía KeyguardManager
                        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                        @Suppress("DEPRECATION")
                        val intent = km?.createConfirmDeviceCredentialIntent(
                            strPromptTitle,
                            strPromptSubtitle
                        )
                        if (intent != null) {
                            deviceCredentialLauncher.launch(intent)
                        } else {
                            errorMessage = strAuthFailed
                        }
                    }
                }

                else -> {}
            }
        } else {
            // Sin Activity no hay BiometricPrompt ni UI propia: fail-visible para no dejar
            // el flujo atrapado en Authenticating sin interfaz (regresión 02-oct 15:57).
            errorMessage = strAuthFailed
        }
    }

    // Para modos biométricos o credencial del sistema que ya abrieron el diálogo de plataforma,
    // solo renderizamos UI propia si hubo un error o para permitir reintentar / cancelar
    if (authMode == HiddenAppsAuthMode.BIOMETRIC || authMode == HiddenAppsAuthMode.DEVICE_CREDENTIAL) {
        if (errorMessage != null) {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = themeTokens.surface.copy(alpha = 0.98f),
                    border = BorderStroke(1.dp, themeTokens.border.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = themeTokens.accent, modifier = Modifier.size(36.dp))
                        Text(
                            text = errorMessage ?: stringResource(R.string.hidden_apps_auth_failed),
                            color = Color(0xFFFF5252),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(stringResource(R.string.hidden_apps_cancel), color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // Modal interactivo para credenciales propias (PIN, PATRÓN, CONTRASEÑA)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            securePolicy = SecureFlagPolicy.SecureOn
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = themeTokens.surface.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, themeTokens.border.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera
                Icon(
                    imageVector = if (isResetMode) Icons.Outlined.LockReset else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (isResetMode) Color(0xFFFF8A80) else themeTokens.accent,
                    modifier = Modifier.size(36.dp)
                )

                Text(
                    text = if (isResetMode) {
                        strResetAuthTitle
                    } else {
                        when (authMode) {
                            HiddenAppsAuthMode.OWN_PIN -> stringResource(R.string.hidden_apps_enter_pin)
                            HiddenAppsAuthMode.OWN_PATTERN -> stringResource(R.string.hidden_apps_enter_pattern)
                            HiddenAppsAuthMode.OWN_PASSWORD -> stringResource(R.string.hidden_apps_enter_password)
                            else -> stringResource(R.string.hidden_apps_title)
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                if (isResetMode) {
                    Text(
                        text = strResetAuthDesc,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Mensajes de cooldown o error
                if (cooldownRemaining > 0L) {
                    Text(
                        text = stringResource(R.string.hidden_apps_cooldown_wait, cooldownRemaining),
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                val isLocked = (cooldownRemaining > 0L)

                // Contenido según credencial propia
                when (authMode) {
                    HiddenAppsAuthMode.OWN_PIN -> {

                        // Indicador visual de puntos de PIN
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            val dotCount = pinValue.length.coerceAtLeast(6)
                            for (i in 0 until dotCount) {
                                val filled = i < pinValue.length
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (filled) themeTokens.accent else Color.White.copy(alpha = 0.2f)
                                        )
                                )
                            }
                        }

                        // Teclado numérico táctil en pantalla
                        PinKeypadView(
                            enabled = !isLocked,
                            onDigit = { digit ->
                                if (pinValue.length < 12) {
                                    pinValue += digit
                                    errorMessage = null
                                }
                            },
                            onDelete = {
                                if (pinValue.isNotEmpty()) {
                                    pinValue = pinValue.dropLast(1)
                                    errorMessage = null
                                }
                            },
                            onSubmit = {
                                if (pinValue.length >= 6) {
                                    coroutineScope.launch {
                                        val chars = pinValue.toCharArray()
                                        val success = authenticator.verifyOwnCredential(chars, credentialStore)
                                        HiddenAppsCrypto.wipe(chars)
                                        if (success) {
                                            if (isResetMode) {
                                                onResetRequested()
                                            } else {
                                                onAuthenticated()
                                            }
                                        } else {
                                            pinValue = ""
                                            if (authenticator.isCooldownActive()) {
                                                cooldownRemaining = authenticator.getRemainingCooldownSeconds()
                                            } else {
                                                errorMessage = strAuthFailed
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    HiddenAppsAuthMode.OWN_PATTERN -> {
                        PatternLockView(
                            enabled = !isLocked,
                            isError = (errorMessage != null),
                            onPatternComplete = { patternList ->
                                errorMessage = null
                                val serialized = HiddenAppsCrypto.serializePattern(patternList)
                                coroutineScope.launch {
                                    val chars = serialized.toCharArray()
                                    val success = authenticator.verifyOwnCredential(chars, credentialStore)
                                    HiddenAppsCrypto.wipe(chars)
                                    if (success) {
                                        if (isResetMode) {
                                            onResetRequested()
                                        } else {
                                            onAuthenticated()
                                        }
                                    } else {
                                        if (authenticator.isCooldownActive()) {
                                            cooldownRemaining = authenticator.getRemainingCooldownSeconds()
                                        } else {
                                            errorMessage = strAuthFailed
                                        }
                                    }
                                }
                            }
                        )
                    }

                    HiddenAppsAuthMode.OWN_PASSWORD -> {
                        var passwordVisible by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = passwordValue,
                            onValueChange = {
                                passwordValue = it
                                errorMessage = null
                            },
                            enabled = !isLocked,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color.LightGray
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (passwordValue.length >= 8) {
                                        coroutineScope.launch {
                                            val chars = passwordValue.toCharArray()
                                            val success = authenticator.verifyOwnCredential(chars, credentialStore)
                                            HiddenAppsCrypto.wipe(chars)
                                            if (success) {
                                                if (isResetMode) {
                                                    onResetRequested()
                                                } else {
                                                    onAuthenticated()
                                                }
                                            } else {
                                                passwordValue = ""
                                                if (authenticator.isCooldownActive()) {
                                                    cooldownRemaining = authenticator.getRemainingCooldownSeconds()
                                                } else {
                                                    errorMessage = strAuthFailed
                                                }
                                            }
                                        }
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeTokens.accent,
                                unfocusedBorderColor = themeTokens.border.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (passwordValue.length >= 8) {
                                    coroutineScope.launch {
                                        val chars = passwordValue.toCharArray()
                                        val success = authenticator.verifyOwnCredential(chars, credentialStore)
                                        HiddenAppsCrypto.wipe(chars)
                                        if (success) {
                                            if (isResetMode) {
                                                onResetRequested()
                                            } else {
                                                onAuthenticated()
                                            }
                                        } else {
                                            passwordValue = ""
                                            if (authenticator.isCooldownActive()) {
                                                cooldownRemaining = authenticator.getRemainingCooldownSeconds()
                                            } else {
                                                errorMessage = strAuthFailed
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLocked && passwordValue.length >= 8,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeTokens.accent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.hidden_apps_confirm), fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {}
                }

                // Opciones inferiores: Restablecer protección y Cancelar
                if (isResetMode) {
                    TextButton(
                        onClick = {
                            isResetMode = false
                            pinValue = ""
                            passwordValue = ""
                            errorMessage = null
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.hidden_apps_cancel),
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showResetConfirmDialog = true }) {
                            Text(
                                text = stringResource(R.string.hidden_apps_reset_protection),
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp
                            )
                        }

                        TextButton(onClick = onDismiss) {
                            Text(
                                text = stringResource(R.string.hidden_apps_cancel),
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación destructiva de restablecimiento con prevención estricta de bypass
    if (showResetConfirmDialog) {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val hasDeviceLock = (km?.isDeviceSecure == true)

        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                securePolicy = SecureFlagPolicy.SecureOn
            ),
            title = {
                Text(strResetTitle, color = Color.White, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strResetDesc, color = Color.LightGray, fontSize = 13.sp)
                    if (!hasDeviceLock && authMode != HiddenAppsAuthMode.BIOMETRIC && authMode != HiddenAppsAuthMode.DEVICE_CREDENTIAL) {
                        Text(
                            stringResource(R.string.hidden_apps_reset_no_device_lock),
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (authMode == HiddenAppsAuthMode.BIOMETRIC) {
                        Button(
                            onClick = {
                                showResetConfirmDialog = false
                                if (fragmentActivity != null) {
                                    val executor = ContextCompat.getMainExecutor(context)
                                    val prompt = BiometricPrompt(fragmentActivity, executor, object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            authenticator.onAuthenticationSucceeded()
                                            onResetRequested()
                                        }
                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            errorMessage = strAuthFailed
                                        }
                                    })
                                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                        .setTitle(strResetAuthTitle)
                                        .setSubtitle(strResetAuthDesc)
                                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                        .setNegativeButtonText(strPromptCancel)
                                        .build()
                                    try {
                                        prompt.authenticate(promptInfo)
                                    } catch (_: Exception) {
                                        errorMessage = strAuthFailed
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) {
                            Text(stringResource(R.string.hidden_apps_reset_confirm), color = Color.White)
                        }
                    } else if (authMode == HiddenAppsAuthMode.DEVICE_CREDENTIAL) {
                        Button(
                            onClick = {
                                showResetConfirmDialog = false
                                if (hasDeviceLock) {
                                    @Suppress("DEPRECATION")
                                    val intent = km?.createConfirmDeviceCredentialIntent(strResetTitle, strResetDesc)
                                    if (intent != null) {
                                        resetDeviceCredentialLauncher.launch(intent)
                                    } else {
                                        errorMessage = strAuthFailed
                                    }
                                } else {
                                    errorMessage = strAuthFailed
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) {
                            Text(stringResource(R.string.hidden_apps_reset_confirm), color = Color.White)
                        }
                    } else {
                        // Credenciales propias (PIN, PATRÓN, CONTRASEÑA):
                        // Exige ingresar la credencial Nova configurada para autorizar el reseteo
                        Button(
                            onClick = {
                                showResetConfirmDialog = false
                                isResetMode = true
                                pinValue = ""
                                passwordValue = ""
                                errorMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) {
                            Text(stringResource(R.string.hidden_apps_reset_with_nova), color = Color.White)
                        }

                        // Si el dispositivo cuenta con bloqueo seguro del sistema, ofrecer recuperación
                        if (hasDeviceLock) {
                            TextButton(
                                onClick = {
                                    showResetConfirmDialog = false
                                    @Suppress("DEPRECATION")
                                    val intent = km?.createConfirmDeviceCredentialIntent(strResetTitle, strResetDesc)
                                    if (intent != null) {
                                        resetDeviceCredentialLauncher.launch(intent)
                                    } else {
                                        errorMessage = strAuthFailed
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.hidden_apps_reset_with_device), color = themeTokens.accent, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(R.string.hidden_apps_cancel), color = Color.LightGray)
                }
            },
            containerColor = themeTokens.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Teclado numérico táctil de alto rendimiento para PIN sin provocar recomposiciones masivas.
 */
@Composable
private fun PinKeypadView(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit
) {
    val themeTokens = LocalLauncherThemeTokens.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("del", "0", "ok")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                for (item in row) {
                    when (item) {
                        "del" -> {
                            IconButton(
                                onClick = onDelete,
                                enabled = enabled,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete", tint = Color.LightGray)
                            }
                        }
                        "ok" -> {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(if (enabled) themeTokens.accent else Color.DarkGray)
                                    .clickable(enabled = enabled, onClick = onSubmit),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("OK", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable(enabled = enabled) { onDigit(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
