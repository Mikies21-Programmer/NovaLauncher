package com.daybreak.animelauncher.ui.screens.hiddenapps

import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybreak.animelauncher.AppShortcut
import com.daybreak.animelauncher.R
import com.daybreak.animelauncher.hiddenapps.HiddenAppsConfig
import com.daybreak.animelauncher.theming.LocalLauncherThemeTokens
import com.daybreak.animelauncher.theming.accent
import com.daybreak.animelauncher.theming.border
import com.daybreak.animelauncher.theming.surface
import com.daybreak.animelauncher.ui.components.ShortcutIcon

/**
 * Pantalla dedicada y aislada para la administración de Apps ocultas en NovaLauncher.
 *
 * Características:
 * - Aplica FLAG_SECURE en la ventana mientras está visible y lo remueve inmediatamente al salir.
 * - Prioridad absoluta de navegación con botón Back hacia la pantalla de inicio.
 * - Listado de aplicaciones instaladas con indicación visual de estado oculto.
 * - Impide expresamente que NovaLauncher se oculte a sí mismo.
 * - Integrado visualmente con el sistema de temas y Glass UI ligero de NovaLauncher.
 */
@Composable
fun HiddenAppsScreen(
    installedApps: List<AppShortcut>,
    hiddenConfig: HiddenAppsConfig,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onChangeAuthMethod: () -> Unit,
    onResetProtection: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val themeTokens = LocalLauncherThemeTokens.current
    val ownPackageName = remember { context.packageName }

    // FLAG_SECURE: Activar mientras esta pantalla real esté visible y limpiar en onDispose
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Prioridad de Back: Salir de Hidden Apps inmediatamente
    BackHandler(enabled = true) {
        onClose()
    }

    // Temporizador de inactividad de 60 segundos (HALLAZGO 6).
    // El timestamp vive en un objeto no observable: escribir un estado Compose en cada
    // evento de puntero recomponía la pantalla completa en cada frame de scroll.
    val lastInteractionRef = remember { object { var value = System.currentTimeMillis() } }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000L)
            if (System.currentTimeMillis() - lastInteractionRef.value >= 60_000L) {
                onClose()
                break
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ocultas, 1: Todas
    var searchQuery by remember { mutableStateOf("") }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val hiddenPackages = hiddenConfig.hiddenPackages

    val filteredApps = remember(installedApps, hiddenPackages, selectedTab, searchQuery) {
        val baseList = if (selectedTab == 0) {
            installedApps.filter { it.packageName != null && hiddenPackages.contains(it.packageName) }
        } else {
            installedApps
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter { it.name.lowercase().contains(q) }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInteractionRef.value = System.currentTimeMillis()
                    }
                }
            },
        color = themeTokens.surface.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Barra superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = themeTokens.accent
                    )
                }

                Text(
                    text = stringResource(R.string.hidden_apps_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                // Menú de opciones de configuración
                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = "Options",
                            tint = themeTokens.accent
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(themeTokens.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hidden_apps_change_method), color = Color.White, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.LockReset, contentDescription = null, tint = themeTokens.accent) },
                            onClick = {
                                showOptionsMenu = false
                                onChangeAuthMethod()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hidden_apps_reset_protection), color = Color(0xFFFF8A80), fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = Color(0xFFFF5252)) },
                            onClick = {
                                showOptionsMenu = false
                                showResetConfirmDialog = true
                            }
                        )
                    }
                }
            }

            // Pestañas (Ocultas / Todas)
            @Suppress("DEPRECATION")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = themeTokens.accent,
                divider = { HorizontalDivider(color = themeTokens.border.copy(alpha = 0.25f)) },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = (selectedTab == 0),
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.hidden_apps_tab_hidden, hiddenPackages.size),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) themeTokens.accent else Color.LightGray
                        )
                    }
                )
                Tab(
                    selected = (selectedTab == 1),
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(R.string.hidden_apps_tab_all, installedApps.size),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) themeTokens.accent else Color.LightGray
                        )
                    }
                )
            }

            // Barra de búsqueda rápida
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.hidden_apps_search_hint), color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeTokens.accent,
                    unfocusedBorderColor = themeTokens.border.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // Contenido principal
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(themeTokens.accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = themeTokens.accent,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.hidden_apps_empty_state_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = stringResource(R.string.hidden_apps_empty_state_desc),
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.id }
                    ) { app ->
                        val isHidden = app.packageName != null && hiddenPackages.contains(app.packageName)
                        val isOwnLauncher = (app.packageName == ownPackageName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShortcutIcon(
                                shortcut = app,
                                modifier = Modifier.size(44.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isHidden) {
                                    Text(
                                        text = stringResource(R.string.hidden_apps_hidden_badge),
                                        color = themeTokens.accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Botón de alternar visibilidad (NovaLauncher no puede ser ocultado)
                            if (isOwnLauncher) {
                                Text(
                                    text = "Launcher",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Button(
                                    onClick = {
                                        val pkg = app.packageName ?: return@Button
                                        if (isHidden) {
                                            onUnhideApp(pkg)
                                        } else {
                                            onHideApp(pkg)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isHidden) Color.White.copy(alpha = 0.1f) else themeTokens.accent,
                                        contentColor = if (isHidden) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = if (isHidden) stringResource(R.string.hidden_apps_show) else stringResource(R.string.hidden_apps_hide),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación destructiva al solicitar restablecer desde el menú de opciones
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                securePolicy = SecureFlagPolicy.SecureOn
            ),
            title = {
                Text(stringResource(R.string.hidden_apps_reset_dialog_title), color = Color.White, fontSize = 16.sp)
            },
            text = {
                Text(stringResource(R.string.hidden_apps_reset_dialog_desc), color = Color.LightGray, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetProtection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text(stringResource(R.string.hidden_apps_reset_confirm), color = Color.White)
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
