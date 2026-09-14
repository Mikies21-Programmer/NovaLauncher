package com.daybreak.animelauncher.ui.screens

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.daybreak.animelauncher.LauncherViewModel
import com.daybreak.animelauncher.ui.components.VideoWallpaperManager
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // Native Android AppWidgetHost (HOST_ID = 1024) managed by ViewModel
    val appWidgetHost = viewModel.appWidgetHost

    var navState by remember { mutableStateOf<LauncherNavState>(LauncherNavState.Home) }
    var longPressedPageIndex by remember { mutableStateOf(0) }
    var allocatedWidgetId by remember { mutableStateOf<Int?>(null) }
    val installedApps by viewModel.installedApps.collectAsState()

    val pagerState = rememberPagerState(pageCount = { state.viewCount })
    val coroutineScope = rememberCoroutineScope()

    fun handleBackNavigation(): Boolean {
        return when (navState) {
            is LauncherNavState.InDrawer -> {
                navState = LauncherNavState.Home
                true
            }
            is LauncherNavState.WidgetPicker -> {
                navState = LauncherNavState.Home
                true
            }
            is LauncherNavState.BackgroundSelection -> {
                navState = LauncherNavState.Home
                true
            }
            is LauncherNavState.LongPressMenu -> {
                navState = LauncherNavState.Home
                true
            }
            LauncherNavState.Home -> {
                if (pagerState.currentPage != 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    // Gestión estricta del botón Back según prioridad:
    // 1. Cerrar diálogo / menú
    // 2. Cerrar App Drawer
    // 3. Regresar a pantalla principal (página 0)
    // 4. Permanecer en Home consumiendo el evento sin cerrar el launcher
    androidx.activity.compose.BackHandler(enabled = true) {
        handleBackNavigation()
    }

    // Sincronizar el gestor de fondos de video con la página activa
    LaunchedEffect(pagerState.currentPage) {
        VideoWallpaperManager.onPageSelected(pagerState.currentPage, context)
    }

    // Señal explícita y determinista de cambio de topología de páginas (añadir/quitar pantalla)
    LaunchedEffect(state.viewCount) {
        VideoWallpaperManager.onPageSelected(pagerState.currentPage, context)
    }

    // Control estricto de animación de desbloqueo: SOLO se ejecuta al desbloquear/iniciar sesión
    val isUnlockPending by viewModel.isUnlockPending.collectAsState()
    val unlockAlpha = remember { Animatable(1f) }
    val unlockScale = remember { Animatable(1f) }
    val unlockOffsetY = remember { Animatable(0f) }

    LaunchedEffect(isUnlockPending) {
        if (isUnlockPending) {
            try {
                unlockAlpha.snapTo(0f)
                unlockScale.snapTo(0.96f)
                unlockOffsetY.snapTo(24f)
                
                val j1 = launch {
                    unlockAlpha.animateTo(1f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                }
                val j2 = launch {
                    unlockScale.animateTo(1f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                }
                val j3 = launch {
                    unlockOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                }
                kotlinx.coroutines.joinAll(j1, j2, j3)
            } finally {
                unlockAlpha.snapTo(1f)
                unlockScale.snapTo(1f)
                unlockOffsetY.snapTo(0f)
                viewModel.consumeUnlockAnimation()
            }
        }
    }

    // Launcher for Widget Configuration screen (e.g. city picker for weather widget)
    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && allocatedWidgetId != null) {
            viewModel.addNativeWidget(longPressedPageIndex, allocatedWidgetId!!)
        } else if (allocatedWidgetId != null) {
            try { appWidgetHost.deleteAppWidgetId(allocatedWidgetId!!) } catch (e: Exception) {}
        }
    }

    // Launcher for ACTION_APPWIDGET_BIND (asks user permission to bind widget if not already granted)
    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && allocatedWidgetId != null) {
            val widgetId = allocatedWidgetId!!
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val info = appWidgetManager.getAppWidgetInfo(widgetId)
            if (info?.configure != null) {
                val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                try {
                    configureLauncher.launch(configureIntent)
                } catch (e: Exception) {
                    viewModel.addNativeWidget(longPressedPageIndex, widgetId)
                }
            } else {
                viewModel.addNativeWidget(longPressedPageIndex, widgetId)
            }
        } else if (allocatedWidgetId != null) {
            try { appWidgetHost.deleteAppWidgetId(allocatedWidgetId!!) } catch (e: Exception) {}
        }
    }

    // Launcher for Gallery Wallpaper picker
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
            viewModel.updateBackgroundUri(longPressedPageIndex, uri.toString())
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = unlockAlpha.value
                scaleX = unlockScale.value
                scaleY = unlockScale.value
                translationY = unlockOffsetY.value * density
            }
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = (navState == LauncherNavState.Home),
            beyondViewportPageCount = 1,
            key = { it },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            var event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            while (!event.changes.any { it.pressed }) {
                                event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            }
                            var totalX = 0f
                            var totalY = 0f
                            var isTracking = true
                            while (isTracking) {
                                event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                val change = event.changes.firstOrNull()
                                if (change == null || !change.pressed) {
                                    isTracking = false
                                } else {
                                    val deltaX = change.position.x - change.previousPosition.x
                                    val deltaY = change.position.y - change.previousPosition.y
                                    totalX += deltaX
                                    totalY += deltaY

                                    // Si el movimiento es predominantemente horizontal, ignorar para permitir que el pager deslice sin interferencia
                                    if (kotlin.math.abs(totalX) > kotlin.math.abs(totalY) * 1.5f && kotlin.math.abs(totalX) > 40f) {
                                        isTracking = false
                                    } else if (kotlin.math.abs(totalY) > kotlin.math.abs(totalX) * 1.2f) {
                                        if (totalY < -80f) {
                                            navState = LauncherNavState.InDrawer.Main
                                            isTracking = false
                                        } else if (totalY > 80f && state.gesturesConfig.swipeDownForNotifications) {
                                            expandStatusBar(context)
                                            isTracking = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (state.gesturesConfig.pinchInForSettings && zoom < 0.9f) {
                            onNavigateToSettings()
                        }
                    }
                }
        ) { page ->
            // Transición suave por barrido horizontal sin parpadeos negros (calculada en draw phase, sin recomposiciones)
            val modifier = Modifier.graphicsLayer {
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                alpha = (1f - pageOffset * 0.35f).coerceIn(0.65f, 1f)
            }

        val viewConfig = state.viewConfigs.getOrNull(page) ?: state.viewConfigs.last()
        
        val onLongPressAction = {
            longPressedPageIndex = page
            navState = LauncherNavState.LongPressMenu(page)
        }

        val onDoubleTapAction = {
            if (state.gesturesConfig.doubleTapToSleep) {
                val locked = com.daybreak.animelauncher.LauncherAccessibilityService.lockScreen()
                if (!locked) {
                    Toast.makeText(
                        context,
                        if (state.language == "es") "Activa el servicio de accesibilidad de NovaLauncher para apagar la pantalla" else "Enable NovaLauncher accessibility service to lock screen",
                        Toast.LENGTH_LONG
                    ).show()
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        val isEs = state.language == "es"
        if (page % 2 == 0) {
            ViewOne(
                config = viewConfig,
                onSettingsClick = onNavigateToSettings,
                onLongPress = onLongPressAction,
                onDoubleTap = onDoubleTapAction,
                appWidgetHost = appWidgetHost,
                viewModel = viewModel,
                viewIndex = page,
                language = state.language,
                showUI = (navState !is LauncherNavState.InDrawer),
                modifier = modifier
            )
        } else {
            ViewTwo(
                config = viewConfig,
                onSettingsClick = onNavigateToSettings,
                onLongPress = onLongPressAction,
                onDoubleTap = onDoubleTapAction,
                appWidgetHost = appWidgetHost,
                viewModel = viewModel,
                viewIndex = page,
                language = state.language,
                showUI = (navState !is LauncherNavState.InDrawer),
                modifier = modifier
            )
        }
        }

        // 1. Left Edge -> Back (Lógica interna independiente de accesibilidad)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(24.dp)
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f }
                    ) { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag > 50f && state.gesturesConfig.edgeSwipeToBack) {
                            handleBackNavigation()
                            totalDrag = 0f
                        }
                    }
                }
        )

        // 2. Right Edge -> Back (Lógica interna independiente de accesibilidad)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(24.dp)
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f }
                    ) { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag < -50f && state.gesturesConfig.edgeSwipeToBack) {
                            handleBackNavigation()
                            totalDrag = 0f
                        }
                    }
                }
        )

        // 3. Bottom Edge -> Recents (Acción global del sistema con validación de accesibilidad)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { totalDrag = 0f }
                    ) { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag < -50f && state.gesturesConfig.bottomSwipeToRecents) {
                            val opened = com.daybreak.animelauncher.LauncherAccessibilityService.openRecents()
                            if (!opened) {
                                Toast.makeText(
                                    context,
                                    if (state.language == "es") "Activa el servicio de accesibilidad de NovaLauncher para ver Recientes" else "Enable NovaLauncher accessibility service to view Recents",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            totalDrag = 0f
                        }
                    }
                }
        )
    }

    // Long Press Classic Launcher Menu (Fondos, Widgets, Configuración)
    if (navState is LauncherNavState.LongPressMenu) {
        val isEs = state.language == "es"
        Dialog(onDismissRequest = { navState = LauncherNavState.Home }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF08080C).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f)),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isEs) "Opciones de Pantalla ${longPressedPageIndex + 1}" else "Screen ${longPressedPageIndex + 1} Options",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF00F0FF), // Cian Neón fosforecente único
                        fontWeight = FontWeight.Normal
                    )

                    HorizontalDivider(color = Color(0xFF00F0FF).copy(alpha = 0.3f))

                    // 1. Fondos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                navState = LauncherNavState.BackgroundSelection(longPressedPageIndex)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = "Fondos", tint = Color(0xFF00F0FF), modifier = Modifier.size(28.dp))
                        Column {
                            Text(if (isEs) "Fondos de pantalla" else "Wallpapers & Videos", color = Color.White, fontWeight = FontWeight.Normal, fontSize = 16.sp)
                            Text(if (isEs) "Elige video o foto de tu galería" else "Choose video or photo from gallery", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }

                    // 2. Widgets (Abre nuestro selector propio integrado)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                navState = LauncherNavState.WidgetPicker(longPressedPageIndex)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.Apps, contentDescription = "Widgets", tint = Color(0xFF00F0FF), modifier = Modifier.size(28.dp))
                        Column {
                            Text(if (isEs) "Widgets del sistema" else "System Widgets", color = Color.White, fontWeight = FontWeight.Normal, fontSize = 16.sp)
                            Text(if (isEs) "WhatsApp, Spotify, Fotos, Google Maps..." else "WhatsApp, Spotify, Photos, Google Maps...", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }

                    // 3. Configuración
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                navState = LauncherNavState.Home
                                onNavigateToSettings()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = Color(0xFF00F0FF), modifier = Modifier.size(28.dp))
                        Column {
                            Text(if (isEs) "Configuración de pantalla" else "Screen Settings", color = Color.White, fontWeight = FontWeight.Normal, fontSize = 16.sp)
                            Text(if (isEs) "Administrar pantallas y accesos rápidos" else "Manage screens and shortcuts", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }

                    TextButton(
                        onClick = { navState = LauncherNavState.Home },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (isEs) "Cancelar" else "Cancel", color = Color(0xFF00F0FF), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Custom In-Launcher Widget Picker Dialog (Sin salir al launcher antiguo)
    if (navState is LauncherNavState.WidgetPicker) {
        val isEs = state.language == "es"
        Dialog(onDismissRequest = { navState = LauncherNavState.Home }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF08080C).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f)),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
                val installedProviders = remember {
                    try {
                        appWidgetManager.installedProviders.sortedBy { it.loadLabel(context.packageManager) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                var widgetSearchQuery by remember { mutableStateOf("") }
                val filteredProviders = remember(installedProviders, widgetSearchQuery) {
                    if (widgetSearchQuery.isBlank()) installedProviders
                    else installedProviders.filter { 
                        it.loadLabel(context.packageManager).contains(widgetSearchQuery, ignoreCase = true) 
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isEs) "Selecciona un Widget" else "Select a Widget",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF00F0FF), // Cian Neón fosforecente único
                        fontWeight = FontWeight.Normal
                    )

                    OutlinedTextField(
                        value = widgetSearchQuery,
                        onValueChange = { widgetSearchQuery = it },
                        label = { Text(if (isEs) "Buscar widget (ej. WhatsApp, Clima...)" else "Search widget (e.g. WhatsApp, Weather...)", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00F0FF), // Cian Neón fosforecente único
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color(0xFF00F0FF)
                        ),
                        singleLine = true
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProviders) { providerInfo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        navState = LauncherNavState.Home
                                        try {
                                            val widgetId = appWidgetHost.allocateAppWidgetId()
                                            allocatedWidgetId = widgetId
                                            val bound = try {
                                                appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, providerInfo.provider)
                                            } catch (e: Exception) {
                                                false
                                            }

                                            if (bound) {
                                                if (providerInfo.configure != null) {
                                                    val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                                                        component = providerInfo.configure
                                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                                    }
                                                    try {
                                                        configureLauncher.launch(configureIntent)
                                                    } catch (e: Exception) {
                                                        viewModel.addNativeWidget(longPressedPageIndex, widgetId)
                                                    }
                                                } else {
                                                    viewModel.addNativeWidget(longPressedPageIndex, widgetId)
                                                }
                                            } else {
                                                val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                                                }
                                                try {
                                                    bindWidgetLauncher.launch(bindIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, if (isEs) "Permiso requerido por el sistema" else "System permission required", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                val pm = context.packageManager
                                val label = remember(providerInfo) { providerInfo.loadLabel(pm) }
                                val appName = remember(providerInfo) {
                                    try { pm.getApplicationLabel(pm.getApplicationInfo(providerInfo.provider.packageName, 0)).toString() } catch (e: Exception) { "" }
                                }

                                Icon(
                                    imageVector = Icons.Outlined.Widgets,
                                    contentDescription = null,
                                    tint = Color(0xFF00F0FF),
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = label, color = Color.White, fontWeight = FontWeight.Normal, fontSize = 15.sp)
                                    if (appName.isNotEmpty() && appName != label) {
                                        Text(text = appName, color = Color.LightGray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { navState = LauncherNavState.Home },
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

    if (navState is LauncherNavState.BackgroundSelection) {
        val isEs = state.language == "es"
        BackgroundSelectionScreen(
            isEs = isEs,
            onThemeSelected = { theme ->
                viewModel.updateBackgroundUri(longPressedPageIndex, theme.backgroundUri)
                viewModel.updateStyleConfig(theme.styleConfig)
                navState = LauncherNavState.Home
            },
            onImageSelected = { uri ->
                viewModel.updateBackgroundUri(longPressedPageIndex, uri)
                navState = LauncherNavState.Home
            },
            onCustomImageRequest = {
                navState = LauncherNavState.Home
                pickMediaLauncher.launch(arrayOf("image/*", "video/*"))
            },
            onBack = { navState = LauncherNavState.Home }
        )
    }

    AnimatedVisibility(
        visible = navState is LauncherNavState.InDrawer,
        enter = slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { it / 3 },
            animationSpec = tween(180, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(180))
    ) {
        AppDrawerScreen(
            viewModel = viewModel,
            installedApps = installedApps,
            categories = state.drawerConfig.categories,
            isEs = state.language == "es",
            onClose = { navState = LauncherNavState.Home }
        )
    }
}

fun expandStatusBar(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expand = statusBarManager.getMethod("expandNotificationsPanel")
        expand.invoke(statusBarService)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
