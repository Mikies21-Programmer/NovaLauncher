package com.daybreak.animelauncher.ui.screens.hiddenapps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.daybreak.animelauncher.R
import com.daybreak.animelauncher.hiddenapps.*
import com.daybreak.animelauncher.theming.LocalLauncherThemeTokens
import com.daybreak.animelauncher.theming.accent
import com.daybreak.animelauncher.theming.border
import com.daybreak.animelauncher.theming.surface
import kotlinx.coroutines.launch

private enum class SetupStep {
    SELECT_METHOD,
    INPUT_CREDENTIAL,
    CONFIRM_CREDENTIAL
}

/**
 * Diálogo de configuración inicial o cambio de método de protección para Apps ocultas.
 * Permite seleccionar entre Biometría, Credencial del sistema, PIN, Patrón o Contraseña
 * con confirmación en dos pasos para credenciales propias.
 */
@Composable
fun HiddenAppsSetupDialog(
    authenticator: HiddenAppsAuthenticator,
    repository: HiddenAppsRepository,
    isChangingMethod: Boolean = false,
    onConfigured: (HiddenAppsAuthMode) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themeTokens = LocalLauncherThemeTokens.current

    val canUseBiometric = remember { authenticator.canAuthenticateBiometric(context) }
    val isDeviceSecure = remember { authenticator.isDeviceSecure(context) }

    var currentStep by remember { mutableStateOf(SetupStep.SELECT_METHOD) }
    var selectedMode by remember { mutableStateOf<HiddenAppsAuthMode?>(null) }
    var primaryInput by remember { mutableStateOf("") }
    var confirmInput by remember { mutableStateOf("") }
    var primaryPattern by remember { mutableStateOf<List<Int>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val strPinInvalid = stringResource(R.string.hidden_apps_pin_invalid)
    val strPatternInvalid = stringResource(R.string.hidden_apps_pattern_invalid)
    val strPasswordInvalid = stringResource(R.string.hidden_apps_password_invalid)
    val strPinMismatch = stringResource(R.string.hidden_apps_pin_mismatch)
    val strPatternMismatch = stringResource(R.string.hidden_apps_pattern_mismatch)
    val strPasswordMismatch = stringResource(R.string.hidden_apps_password_mismatch)
    val strSaveFailed = stringResource(R.string.hidden_apps_setup_save_failed)

    // HALLAZGO 2 (revisado P0-B): el FLAG_SECURE sensible aplica a la ventana del Dialog
    // vía SecureFlagPolicy.SecureOn. No se marca la ventana de la Activity HOME para no
    // ennegrecer la superficie del launcher durante transiciones del sistema; el
    // FLAG_SECURE de pantalla completa permanece únicamente en HiddenAppsScreen.

    Dialog(
        onDismissRequest = {
            if (currentStep == SetupStep.SELECT_METHOD) {
                onDismiss()
            } else {
                currentStep = SetupStep.SELECT_METHOD
                primaryInput = ""
                confirmInput = ""
                primaryPattern = null
                errorMessage = null
            }
        },
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = themeTokens.accent,
                    modifier = Modifier.size(36.dp)
                )

                Text(
                    text = if (isChangingMethod) {
                        stringResource(R.string.hidden_apps_change_method)
                    } else {
                        stringResource(R.string.hidden_apps_setup_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                when (currentStep) {
                    SetupStep.SELECT_METHOD -> {
                        Text(
                            text = stringResource(R.string.hidden_apps_setup_subtitle),
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        // 1. Biometría segura (solo si está disponible)
                        if (canUseBiometric) {
                            SetupMethodCard(
                                icon = Icons.Outlined.Fingerprint,
                                title = stringResource(R.string.hidden_apps_auth_biometric),
                                description = stringResource(R.string.hidden_apps_auth_biometric_desc),
                                onClick = {
                                    repository.credentialVault.clearCredential(deleteKeystoreKey = true)
                                    repository.setAuthMode(HiddenAppsAuthMode.BIOMETRIC)
                                    onConfigured(HiddenAppsAuthMode.BIOMETRIC)
                                }
                            )
                        }

                        // 2. Credencial del dispositivo (solo si está configurada)
                        if (isDeviceSecure) {
                            SetupMethodCard(
                                icon = Icons.Outlined.ScreenLockPortrait,
                                title = stringResource(R.string.hidden_apps_auth_device),
                                description = stringResource(R.string.hidden_apps_auth_device_desc),
                                onClick = {
                                    repository.credentialVault.clearCredential(deleteKeystoreKey = true)
                                    repository.setAuthMode(HiddenAppsAuthMode.DEVICE_CREDENTIAL)
                                    onConfigured(HiddenAppsAuthMode.DEVICE_CREDENTIAL)
                                }
                            )
                        }

                        // 3. PIN Nova
                        SetupMethodCard(
                            icon = Icons.Outlined.Pin,
                            title = stringResource(R.string.hidden_apps_auth_pin),
                            description = stringResource(R.string.hidden_apps_auth_pin_desc),
                            onClick = {
                                selectedMode = HiddenAppsAuthMode.OWN_PIN
                                currentStep = SetupStep.INPUT_CREDENTIAL
                                errorMessage = null
                            }
                        )

                        // 4. Patrón Nova
                        SetupMethodCard(
                            icon = Icons.Outlined.Pattern,
                            title = stringResource(R.string.hidden_apps_auth_pattern),
                            description = stringResource(R.string.hidden_apps_auth_pattern_desc),
                            onClick = {
                                selectedMode = HiddenAppsAuthMode.OWN_PATTERN
                                currentStep = SetupStep.INPUT_CREDENTIAL
                                errorMessage = null
                            }
                        )

                        // 5. Contraseña Nova
                        SetupMethodCard(
                            icon = Icons.Outlined.Password,
                            title = stringResource(R.string.hidden_apps_auth_password),
                            description = stringResource(R.string.hidden_apps_auth_password_desc),
                            onClick = {
                                selectedMode = HiddenAppsAuthMode.OWN_PASSWORD
                                currentStep = SetupStep.INPUT_CREDENTIAL
                                errorMessage = null
                            }
                        )

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.hidden_apps_cancel), color = Color.LightGray)
                        }
                    }

                    SetupStep.INPUT_CREDENTIAL -> {
                        when (selectedMode) {
                            HiddenAppsAuthMode.OWN_PIN -> {
                                Text(stringResource(R.string.hidden_apps_enter_pin), color = Color.White, fontSize = 14.sp)

                                OutlinedTextField(
                                    value = primaryInput,
                                    onValueChange = {
                                        if (it.length <= 12 && it.all { ch -> ch.isDigit() }) {
                                            primaryInput = it
                                            errorMessage = null
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeTokens.accent,
                                        unfocusedBorderColor = themeTokens.border.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                SetupActionButtons(
                                    onBack = { currentStep = SetupStep.SELECT_METHOD; primaryInput = "" },
                                    onNext = {
                                        if (HiddenAppsCrypto.validatePin(primaryInput)) {
                                            currentStep = SetupStep.CONFIRM_CREDENTIAL
                                        } else {
                                            errorMessage = strPinInvalid
                                        }
                                    },
                                    isNextEnabled = HiddenAppsCrypto.validatePin(primaryInput)
                                )
                            }

                            HiddenAppsAuthMode.OWN_PATTERN -> {
                                Text(stringResource(R.string.hidden_apps_enter_pattern), color = Color.White, fontSize = 14.sp)

                                PatternLockView(
                                    onPatternComplete = { pattern ->
                                        if (HiddenAppsCrypto.validatePattern(pattern)) {
                                            primaryPattern = pattern
                                            currentStep = SetupStep.CONFIRM_CREDENTIAL
                                            errorMessage = null
                                        } else {
                                            errorMessage = strPatternInvalid
                                        }
                                    }
                                )

                                TextButton(onClick = { currentStep = SetupStep.SELECT_METHOD }) {
                                    Text(stringResource(R.string.hidden_apps_cancel), color = Color.LightGray)
                                }
                            }

                            HiddenAppsAuthMode.OWN_PASSWORD -> {
                                Text(stringResource(R.string.hidden_apps_enter_password), color = Color.White, fontSize = 14.sp)

                                OutlinedTextField(
                                    value = primaryInput,
                                    onValueChange = {
                                        if (it.length <= 64) {
                                            primaryInput = it
                                            errorMessage = null
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeTokens.accent,
                                        unfocusedBorderColor = themeTokens.border.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                SetupActionButtons(
                                    onBack = { currentStep = SetupStep.SELECT_METHOD; primaryInput = "" },
                                    onNext = {
                                        if (HiddenAppsCrypto.validatePassword(primaryInput)) {
                                            currentStep = SetupStep.CONFIRM_CREDENTIAL
                                        } else {
                                            errorMessage = strPasswordInvalid
                                        }
                                    },
                                    isNextEnabled = HiddenAppsCrypto.validatePassword(primaryInput)
                                )
                            }

                            else -> {}
                        }
                    }

                    SetupStep.CONFIRM_CREDENTIAL -> {
                        when (selectedMode) {
                            HiddenAppsAuthMode.OWN_PIN -> {
                                Text(stringResource(R.string.hidden_apps_confirm_pin), color = Color.White, fontSize = 14.sp)

                                OutlinedTextField(
                                    value = confirmInput,
                                    onValueChange = {
                                        if (it.length <= 12 && it.all { ch -> ch.isDigit() }) {
                                            confirmInput = it
                                            errorMessage = null
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeTokens.accent,
                                        unfocusedBorderColor = themeTokens.border.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                SetupActionButtons(
                                    onBack = { currentStep = SetupStep.INPUT_CREDENTIAL; confirmInput = "" },
                                    onNext = {
                                        if (primaryInput == confirmInput) {
                                            coroutineScope.launch {
                                                val chars = confirmInput.toCharArray()
                                                val saved = repository.saveOwnCredential(HiddenAppsAuthMode.OWN_PIN, chars)
                                                HiddenAppsCrypto.wipe(chars)
                                                if (saved) {
                                                    onConfigured(HiddenAppsAuthMode.OWN_PIN)
                                                } else {
                                                    errorMessage = strSaveFailed
                                                }
                                            }
                                        } else {
                                            errorMessage = strPinMismatch
                                        }
                                    },
                                    isNextEnabled = (confirmInput.length == primaryInput.length)
                                )
                            }

                            HiddenAppsAuthMode.OWN_PATTERN -> {
                                Text(stringResource(R.string.hidden_apps_confirm_pattern), color = Color.White, fontSize = 14.sp)

                                PatternLockView(
                                    onPatternComplete = { confirmPattern ->
                                        if (primaryPattern == confirmPattern) {
                                            coroutineScope.launch {
                                                val serialized = HiddenAppsCrypto.serializePattern(confirmPattern)
                                                val chars = serialized.toCharArray()
                                                val saved = repository.saveOwnCredential(HiddenAppsAuthMode.OWN_PATTERN, chars)
                                                HiddenAppsCrypto.wipe(chars)
                                                if (saved) {
                                                    onConfigured(HiddenAppsAuthMode.OWN_PATTERN)
                                                } else {
                                                    errorMessage = strSaveFailed
                                                }
                                            }
                                        } else {
                                            errorMessage = strPatternMismatch
                                        }
                                    }
                                )

                                TextButton(onClick = { currentStep = SetupStep.INPUT_CREDENTIAL }) {
                                    Text(stringResource(R.string.hidden_apps_cancel), color = Color.LightGray)
                                }
                            }

                            HiddenAppsAuthMode.OWN_PASSWORD -> {
                                Text(stringResource(R.string.hidden_apps_confirm_password), color = Color.White, fontSize = 14.sp)

                                OutlinedTextField(
                                    value = confirmInput,
                                    onValueChange = {
                                        if (it.length <= 64) {
                                            confirmInput = it
                                            errorMessage = null
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeTokens.accent,
                                        unfocusedBorderColor = themeTokens.border.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                SetupActionButtons(
                                    onBack = { currentStep = SetupStep.INPUT_CREDENTIAL; confirmInput = "" },
                                    onNext = {
                                        if (primaryInput == confirmInput) {
                                            coroutineScope.launch {
                                                val chars = confirmInput.toCharArray()
                                                val saved = repository.saveOwnCredential(HiddenAppsAuthMode.OWN_PASSWORD, chars)
                                                HiddenAppsCrypto.wipe(chars)
                                                if (saved) {
                                                    onConfigured(HiddenAppsAuthMode.OWN_PASSWORD)
                                                } else {
                                                    errorMessage = strSaveFailed
                                                }
                                            }
                                        } else {
                                            errorMessage = strPasswordMismatch
                                        }
                                    },
                                    isNextEnabled = (confirmInput.length == primaryInput.length)
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val themeTokens = LocalLauncherThemeTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(themeTokens.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = themeTokens.accent, modifier = Modifier.size(24.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(description, color = Color.LightGray, fontSize = 12.sp)
        }

        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SetupActionButtons(
    onBack: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean
) {
    val themeTokens = LocalLauncherThemeTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.hidden_apps_cancel), color = Color.LightGray)
        }

        Button(
            onClick = onNext,
            enabled = isNextEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = themeTokens.accent,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.hidden_apps_confirm), fontWeight = FontWeight.Bold)
        }
    }
}
