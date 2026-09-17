package com.daybreak.animelauncher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.daybreak.animelauncher.AdvancedStyleConfig
import com.daybreak.animelauncher.AppShortcut
import com.daybreak.animelauncher.LauncherViewModel
import com.daybreak.animelauncher.ui.components.DynamicBackground
import com.daybreak.animelauncher.ui.components.ShortcutIcon

val LocalAdvancedStyleConfig = compositionLocalOf { AdvancedStyleConfig() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isEs = state.language == "es"

    var selectedViewIndexForMedia by remember { mutableStateOf<Int?>(null) }
    var showBackgroundSelection by remember { mutableStateOf(false) }
    var showAppPickerDialog by remember { mutableStateOf(false) }
    var targetViewIndexForApp by remember { mutableStateOf(0) }
    var isDiagonalTarget by remember { mutableStateOf(true) }
    var isGridTarget by remember { mutableStateOf(false) }
    var targetGridPageIndex by remember { mutableStateOf(0) }
    
    var installedApps by remember { mutableStateOf<List<AppShortcut>>(emptyList()) }
    var appSearchQuery by remember { mutableStateOf("") }

    // Load installed apps once
    LaunchedEffect(Unit) {
        installedApps = viewModel.getInstalledApps(context)
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            selectedViewIndexForMedia?.let { idx ->
                viewModel.updateBackgroundUri(idx, uri.toString())
            }
        }
    }

    var targetIconViewIndex by remember { mutableStateOf<Int?>(null) }
    var targetIconIsDiagonal by remember { mutableStateOf(true) }
    var targetIconIsGrid by remember { mutableStateOf(false) }
    var targetIconGridPage by remember { mutableStateOf(0) }
    var targetIconShortcutIndex by remember { mutableStateOf(0) }

    val pickIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            targetIconViewIndex?.let { idx ->
                if (targetIconIsGrid) {
                    viewModel.updateGridShortcutIcon(idx, targetIconGridPage, targetIconShortcutIndex, uri.toString())
                } else {
                    viewModel.updateShortcutIcon(idx, targetIconIsDiagonal, targetIconShortcutIndex, uri.toString())
                }
            }
        }
    }

    val firstScreenUri = remember(state.viewConfigs) {
        state.viewConfigs.firstOrNull()?.backgroundUri ?: ""
    }

    var showAdvancedSettings by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalAdvancedStyleConfig provides state.styleConfig) {
        if (showAdvancedSettings) {
            androidx.activity.compose.BackHandler(enabled = true) {
                showAdvancedSettings = false
            }
            AdvancedSettingsScreen(
                styleConfig = state.styleConfig,
                backgroundUri = firstScreenUri,
                isEs = isEs,
                onUpdateTransient = { viewModel.updateStyleConfigTransient(it) },
                onPersist = { viewModel.persistStyleConfig(it) },
                onReset = { viewModel.resetStyleConfig() },
                onBack = { showAdvancedSettings = false }
            )
        } else if (showBackgroundSelection && selectedViewIndexForMedia != null) {
            androidx.activity.compose.BackHandler(enabled = true) {
                showBackgroundSelection = false
            }
            BackgroundSelectionScreen(
                isEs = isEs,
                onThemeSelected = { theme ->
                    viewModel.updateBackgroundUri(selectedViewIndexForMedia!!, theme.backgroundUri)
                    viewModel.updateStyleConfig(theme.styleConfig)
                    showBackgroundSelection = false
                },
                onImageSelected = { uri ->
                    viewModel.updateBackgroundUri(selectedViewIndexForMedia!!, uri)
                    showBackgroundSelection = false
                },
                onCustomImageRequest = {
                    showBackgroundSelection = false
                    pickMediaLauncher.launch(arrayOf("image/*", "video/*"))
                },
                onBack = { showBackgroundSelection = false }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
        // 1. Fondo Dinámico de la Vista Principal (Pantalla 1)
        DynamicBackground(
            defaultVideoResId = com.daybreak.animelauncher.R.raw.bg_view_one,
            backgroundUri = firstScreenUri,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Capa Glass Overlay (Oscurecimiento translúcido ciberpunk oscuro puro)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030305).copy(alpha = 0.82f))
        )

        // 3. Contenido Principal con estilo Glassmorphism
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (isEs) "Configuración del Launcher" else "Launcher Settings", 
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF00F0FF), // Cian Neón fosforecente único
                            fontSize = 20.sp
                        ) 
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // 1. Fondo de Pantalla y Temas (UX-01)
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEs) "Fondo de Pantalla y Temas" else "Wallpaper & Themes",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF00F0FF)
                                    )
                                    Text(
                                        text = if (isEs) "Elige temas cyberpunk predefinidos o fotos y videos de tu galería" else "Choose preset cyberpunk themes or photos/videos from your gallery",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFF00F0FF).copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (i in 0 until state.viewCount) {
                                    Button(
                                        onClick = {
                                            selectedViewIndexForMedia = i
                                            showBackgroundSelection = true
                                        },
                                        colors = if (i == 0) ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)
                                                 else ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (i == 0) (if (isEs) "🎨 Vista 1 (Principal)" else "🎨 View 1 (Main)")
                                                   else (if (isEs) "Vista ${i + 1}" else "View ${i + 1}"),
                                            fontWeight = if (i == 0) FontWeight.Medium else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Widgets del Sistema (UX-06)
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Widgets, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEs) "Widgets del Sistema" else "System Widgets",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF00F0FF)
                                    )
                                    Text(
                                        text = if (isEs) "Añade reproductores, notas, clima o utilidades nativas a tus pantallas" else "Add native media players, notes, weather or utilities to screens",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFF00F0FF).copy(alpha = 0.3f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.requestOpenWidgetPicker(0)
                                        onBack()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isEs) "+ Añadir Widget" else "+ Add Widget", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.requestEnterWidgetEditMode()
                                        onBack()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isEs) "Gestionar / Eliminar" else "Manage / Remove", fontWeight = FontWeight.Normal, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // 3. Selector de Idioma / Language Selector (Glass Card)
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = if (isEs) "Idioma del Sistema" else "System Language",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isEs) "Cambia el idioma de toda la interfaz" else "Change global interface language",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                GlassPillButton(
                                    text = "Español",
                                    isSelected = isEs,
                                    onClick = { viewModel.setLanguage("es") }
                                )
                                GlassPillButton(
                                    text = "English",
                                    isSelected = !isEs,
                                    onClick = { viewModel.setLanguage("en") }
                                )
                            }
                        }
                    }
                }

                // 4. --- GESTOS Y NAVEGACIÓN (Solo gestos implementados: UX-02, UX-08) ---
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isEs) "Gestos y Navegación" else "Gestures & Navigation",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF00F0FF)
                                )
                            }
                            HorizontalDivider(color = Color(0xFF00F0FF).copy(alpha = 0.3f))

                            val gc = state.gesturesConfig
                            GestureToggle(
                                title = if (isEs) "Modo Inmersivo (Barra de Gestos)" else "Immersive Mode (Gesture Bar)",
                                subtitle = if (isEs) "Oculta la barra de gestos en navegación por gestos. En modo de 3 botones, los botones del sistema se mantienen visibles sobre transparencia." else "Hides gesture pill in gesture navigation. In 3-button mode, system buttons remain visible over transparency.",
                                checked = gc.immersiveMode,
                                onCheckedChange = { viewModel.updateGesturesConfig(gc.copy(immersiveMode = it)) }
                            )
                            GestureToggle(
                                title = if (isEs) "Doble Toque para Apagar" else "Double Tap to Sleep",
                                subtitle = if (isEs) "Toca 2 veces un espacio vacío para apagar pantalla" else "Tap 2 times on empty space to lock screen",
                                checked = gc.doubleTapToSleep,
                                onCheckedChange = { viewModel.updateGesturesConfig(gc.copy(doubleTapToSleep = it)) }
                            )
                            GestureToggle(
                                title = if (isEs) "Deslizar para Notificaciones" else "Swipe for Notifications",
                                subtitle = if (isEs) "Desliza hacia abajo en el escritorio para abrir notificaciones" else "Swipe down on desktop to open notifications",
                                checked = gc.swipeDownForNotifications,
                                onCheckedChange = { viewModel.updateGesturesConfig(gc.copy(swipeDownForNotifications = it)) }
                            )
                            GestureToggle(
                                title = if (isEs) "Pellizcar para Ajustes" else "Pinch In for Settings",
                                subtitle = if (isEs) "Usa 2 dedos para encoger la pantalla y abrir ajustes" else "Pinch screen with 2 fingers to open settings",
                                checked = gc.pinchInForSettings,
                                onCheckedChange = { viewModel.updateGesturesConfig(gc.copy(pinchInForSettings = it)) }
                            )
                        }
                    }
                }

                // 5. Acceso a Notificaciones (Fase 4C / T-19)
                item {
                    var hasNotifAccess by remember {
                        mutableStateOf(com.daybreak.animelauncher.NotificationMonitorService.isPermissionGranted(context))
                    }
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                hasNotifAccess = com.daybreak.animelauncher.NotificationMonitorService.isPermissionGranted(context)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        hasNotifAccess = com.daybreak.animelauncher.NotificationMonitorService.isPermissionGranted(context)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFF00F0FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isEs) "Acceso a Notificaciones" else "Notification Access",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF00F0FF)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (hasNotifAccess) {
                                        if (isEs) "Acceso activo: el contador de mensajes está funcionando."
                                        else "Access granted: pending message counter is active."
                                    } else {
                                        if (isEs) "Para mostrar el contador de mensajes pendientes, NovaLauncher necesita acceso a tus notificaciones."
                                        else "To show the pending message counter, NovaLauncher needs access to your notifications."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasNotifAccess) Color(0xFF39FF14) else Color.LightGray
                                )
                            }
                            Button(
                                onClick = {
                                    com.daybreak.animelauncher.NotificationMonitorService.openPermissionSettings(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hasNotifAccess) Color.White.copy(alpha = 0.15f) else Color(0xFF00F0FF),
                                    contentColor = if (hasNotifAccess) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (hasNotifAccess) {
                                        if (isEs) "Ajustes" else "Settings"
                                    } else {
                                        if (isEs) "Activar acceso" else "Enable Access"
                                    },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Número de Pantallas (Glass Card)
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (isEs) "Pantallas en tu Launcher: ${state.viewCount}" else "Screens in Launcher: ${state.viewCount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { viewModel.removeView() },
                                    enabled = state.viewCount > 2,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00F0FF).copy(alpha = 0.25f),
                                        contentColor = Color(0xFF00F0FF),
                                        disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(if (isEs) "Quitar Pantalla" else "Remove Screen", fontWeight = FontWeight.Normal)
                                }
                                Button(
                                    onClick = { viewModel.addView() },
                                    enabled = state.viewCount < 8,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00F0FF), // Cian Neón fosforecente único
                                        contentColor = Color.Black,
                                        disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(if (isEs) "+ Añadir Pantalla" else "+ Add Screen", fontWeight = FontWeight.Medium)
                                }
                            }
                            Text(
                                text = if (isEs) "Mínimo: 2 pantallas | Máximo: 8 pantallas" else "Minimum: 2 screens | Maximum: 8 screens",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                // Opciones Avanzadas de Diseño
                item {
                    GlassCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedSettings = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEs) "⚙️ Opciones Avanzadas de Diseño" else "⚙️ Advanced Design Options",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF00F0FF)
                                )
                                Text(
                                    text = if (isEs) "Personaliza colores, anchos y opacidades del launcher" else "Customize launcher colors, widths, and opacities",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Configuración de cada pantalla / View Configs
                itemsIndexed(state.viewConfigs) { index, config ->
                    GlassCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isEs) "✨ Pantalla ${index + 1}" else "✨ Screen ${index + 1}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF00F0FF) // Cian Neón fosforecente único
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00F0FF).copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (index == 0) (if (isEs) "Vista Principal" else "Main View") else (if (isEs) "Deslizable" else "Swipeable"),
                                        color = Color(0xFF00F0FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                            }



                            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                            // Accesos Rápidos Diagonal
                            GlassShortcutSection(
                                title = if (isEs) "⚡ Barra Diagonal Negra (4 Apps)" else "⚡ Black Diagonal Bar (4 Apps)",
                                shortcuts = config.diagonalShortcuts,
                                isEs = isEs,
                                onAddClick = {
                                    targetViewIndexForApp = index
                                    isDiagonalTarget = true
                                    isGridTarget = false
                                    showAppPickerDialog = true
                                },
                                onRemoveClick = { sIdx ->
                                    viewModel.removeShortcut(index, true, sIdx)
                                },
                                onEditIconClick = { sIdx ->
                                    targetIconViewIndex = index
                                    targetIconIsDiagonal = true
                                    targetIconIsGrid = false
                                    targetIconShortcutIndex = sIdx
                                    pickIconLauncher.launch(arrayOf("image/*"))
                                },
                                onResetIconClick = { sIdx ->
                                    viewModel.updateShortcutIcon(index, true, sIdx, null)
                                }
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                            // Accesos Rápidos Lateral
                            GlassShortcutSection(
                                title = if (isEs) "📱 Barra Lateral Blanca (4 Apps)" else "📱 White Sidebar (4 Apps)",
                                shortcuts = config.sidebarShortcuts,
                                isEs = isEs,
                                onAddClick = {
                                    targetViewIndexForApp = index
                                    isDiagonalTarget = false
                                    isGridTarget = false
                                    showAppPickerDialog = true
                                },
                                onRemoveClick = { sIdx ->
                                    viewModel.removeShortcut(index, false, sIdx)
                                },
                                onEditIconClick = { sIdx ->
                                    targetIconViewIndex = index
                                    targetIconIsDiagonal = false
                                    targetIconIsGrid = false
                                    targetIconShortcutIndex = sIdx
                                    pickIconLauncher.launch(arrayOf("image/*"))
                                },
                                onResetIconClick = { sIdx ->
                                    viewModel.updateShortcutIcon(index, false, sIdx, null)
                                }
                            )


                        }
                    }
                }
            }
        }
    }

    // Glass App Picker Dialog
    if (showAppPickerDialog) {
        Dialog(onDismissRequest = { showAppPickerDialog = false; appSearchQuery = "" }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF08080C).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f)),
                tonalElevation = 12.dp,
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = if (isEs) "Selecciona una aplicación" else "Select an application", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF00F0FF)
                    )

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text(if (isEs) "Buscar aplicación..." else "Search app...", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color(0xFF00F0FF)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    val filteredApps = remember(installedApps, appSearchQuery) {
                        if (appSearchQuery.isBlank()) installedApps
                        else installedApps.filter { it.name.contains(appSearchQuery, ignoreCase = true) }
                    }

                    if (filteredApps.isEmpty()) {
                        Text(
                            text = if (isEs) "No se encontraron aplicaciones." else "No apps found.", 
                            color = Color.LightGray, 
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            targetViewIndexForApp?.let { idx ->
                                                if (isGridTarget) {
                                                    viewModel.addShortcutToGridPage(idx, targetGridPageIndex, app)
                                                } else {
                                                    viewModel.addShortcut(idx, isDiagonalTarget, app)
                                                }
                                            }
                                            showAppPickerDialog = false
                                            appSearchQuery = ""
                                        }
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    ShortcutIcon(shortcut = app, modifier = Modifier.size(36.dp), defaultTint = Color(0xFF00F0FF))
                                    Text(app.name, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Light)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showAppPickerDialog = false; appSearchQuery = "" },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEs) "Cancelar" else "Cancel", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(LocalAdvancedStyleConfig.current.cornerRadius.dp),
    backgroundColor: Color = Color(0xFF08080C).copy(alpha = LocalAdvancedStyleConfig.current.panelTransparency),
    borderColor: Color = Color(0xFF00F0FF).copy(alpha = LocalAdvancedStyleConfig.current.glassBorderAlpha),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun GlassPillButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF00F0FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light,
            fontSize = 13.sp
        )
    }
}

@Composable
fun GlassShortcutSection(
    title: String,
    shortcuts: List<AppShortcut>,
    isEs: Boolean,
    onAddClick: () -> Unit,
    onRemoveClick: (Int) -> Unit,
    onEditIconClick: ((Int) -> Unit)? = null,
    onResetIconClick: ((Int) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Light, color = Color.White)
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (isEs) "+ Añadir App" else "+ Add App", fontSize = 12.sp, fontWeight = FontWeight.Light)
            }
        }

        if (shortcuts.isEmpty()) {
            Text(
                text = if (isEs) "Sin accesos rápidos en esta barra." else "No shortcuts in this bar.", 
                style = MaterialTheme.typography.bodySmall, 
                color = Color.LightGray
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                shortcuts.forEachIndexed { sIdx, shortcut ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ShortcutIcon(
                                shortcut = shortcut,
                                modifier = Modifier.size(32.dp),
                                defaultTint = Color.White
                            )
                            Text(shortcut.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Light)
                        }
                        Row {
                            if (shortcut.customIconUri != null && onResetIconClick != null) {
                                IconButton(onClick = { onResetIconClick(sIdx) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Restaurar Icono", tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                                }
                            }
                            if (onEditIconClick != null) {
                                IconButton(onClick = { onEditIconClick(sIdx) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = { onRemoveClick(sIdx) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar", tint = Color(0xFF00F0FF).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GestureToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.LightGray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00F0FF),
                checkedTrackColor = Color(0xFF00F0FF).copy(alpha = 0.3f)
            )
        )
    }
}
