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
import androidx.compose.material.icons.outlined.Edit
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.daybreak.animelauncher.LauncherViewModel
import com.daybreak.animelauncher.ui.components.AccessibilityDisclosureDialog
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
    
    val widgetHostManager = viewModel.widgetHostManager
    val appWidgetHost = widgetHostManager.appWidgetHost

    var navState by remember { mutableStateOf<LauncherNavState>(LauncherNavState.Home) }
    var isWidgetEditMode by remember { mutableStateOf(false) }
    var longPressedPageIndex by remember { mutableStateOf(0) }
    var allocatedWidgetId by remember { mutableStateOf<Int?>(null) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onAccessibilityDisclosureRequested = {
            showAccessibilityDisclosure = true
        }
        onDispose {
            onAccessibilityDisclosureRequested = null
        }
    }
    val pageWidgetBoundsMap = remember { mutableStateMapOf<Int, List<Rect>>() }
    val installedApps by viewModel.installedApps.collectAsState()

    val pagerState = rememberPagerState(pageCount = { state.viewCount })
    val coroutineScope = rememberCoroutineScope()

    val currentNavState by rememberUpdatedState(navState)
    val currentWidgetBoundsList by rememberUpdatedState(pageWidgetBoundsMap[pagerState.currentPage] ?: emptyList())
    val currentWidgetEditMode by rememberUpdatedState(isWidgetEditMode)
    val currentPinchEnabled by rememberUpdatedState(state.gesturesConfig.pinchInForSettings)
    val currentSwipeDownEnabled by rememberUpdatedState(state.gesturesConfig.swipeDownForNotifications)
    val currentLanguage by rememberUpdatedState(state.language)

    fun handleBackNavigation(): Boolean {
        if (isWidgetEditMode) {
            isWidgetEditMode = false
            return true
        }
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
    // 1. Cerrar Modo Edición de widgets
    // 2. Cerrar diálogo / menú
    // 3. Cerrar App Drawer
    // 4. Regresar a pantalla principal (página 0)
    // 5. Si ya está en Home Página 0, permanecer en Home consumiendo el evento sin disparar transiciones de salida
    androidx.activity.compose.BackHandler(enabled = true) {
        handleBackNavigation()
    }

    // Escuchar evento HOME del sistema (onNewIntent) para volver a Página 0 y cerrar overlays
    LaunchedEffect(Unit) {
        viewModel.homeActionTrigger.collect {
            isWidgetEditMode = false
            navState = LauncherNavState.Home
            if (pagerState.currentPage != 0) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
        }
    }

    // Recolector de acciones de widgets solicitadas desde Settings (UX-06)
    LaunchedEffect(Unit) {
        viewModel.widgetActionFlow.collect { action ->
            when (action) {
                is LauncherViewModel.WidgetAction.OpenPicker -> {
                    longPressedPageIndex = action.pageIndex
                    navState = LauncherNavState.WidgetPicker(action.pageIndex)
                }
                LauncherViewModel.WidgetAction.EnterEditMode -> {
                    isWidgetEditMode = true
                    navState = LauncherNavState.Home
                }
            }
        }
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
            widgetHostManager.deleteWidgetId(allocatedWidgetId!!)
            Toast.makeText(context, if (state.language == "es") "Configuración cancelada" else "Configuration cancelled", Toast.LENGTH_SHORT).show()
        }
        allocatedWidgetId = null
    }

    // Launcher for ACTION_APPWIDGET_BIND (asks user permission to bind widget if not already granted)
    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && allocatedWidgetId != null) {
            val widgetId = allocatedWidgetId!!
            val info = widgetHostManager.getAppWidgetInfo(widgetId)
            if (info?.configure != null) {
                val activity = context as? com.daybreak.animelauncher.MainActivity
                val started = activity?.startAppWidgetConfigure(widgetId) { cfgResult, _ ->
                    if (cfgResult == Activity.RESULT_OK) {
                        viewModel.addNativeWidget(longPressedPageIndex, widgetId)
                    } else {
                        widgetHostManager.deleteWidgetId(widgetId)
                        Toast.makeText(context, if (state.language == "es") "Configuración cancelada" else "Configuration cancelled", Toast.LENGTH_SHORT).show()
                    }
                    allocatedWidgetId = null
                } ?: false
                if (!started) {
                    val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = info.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    try {
                        configureLauncher.launch(configureIntent)
                    } catch (e: Exception) {
                        widgetHostManager.deleteWidgetId(widgetId)
                        allocatedWidgetId = null
                        Toast.makeText(context, if (state.language == "es") "Error al abrir configuración" else "Error opening configuration", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                viewModel.addNativeWidget(longPressedPageIndex, widgetId)
                allocatedWidgetId = null
            }
        } else if (allocatedWidgetId != null) {
            widgetHostManager.deleteWidgetId(allocatedWidgetId!!)
            allocatedWidgetId = null
            Toast.makeText(context, if (state.language == "es") "Permiso denegado por el usuario" else "Permission denied by user", Toast.LENGTH_SHORT).show()
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

                            // Si al inicio hay 2 o más dedos pulsados, cancelar inmediatamente el swipe vertical
                            // para permitir que el detector de pinch o gestos multitáctiles procese el evento sin interferencia.
                            if (event.changes.count { it.pressed } >= 2) {
                                while (event.changes.any { it.pressed }) {
                                    event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                }
                                continue
                            }

                            val downChange = event.changes.firstOrNull { it.pressed }
                            var isTracking = true

                            // Mientras isWidgetEditMode sea true, ignorar gestos verticales del launcher (Drawer / Notificaciones)
                            if (currentWidgetEditMode) {
                                isTracking = false
                            }

                            // PR-01 / 4B-01: Si el toque inicial ocurre sobre la región física real de un widget nativo,
                            // no secuestrar el gesto vertical para permitir el scroll natural o interacción del widget.
                            if (downChange != null && currentWidgetBoundsList.any { it.contains(downChange.position) }) {
                                isTracking = false
                            }

                            var totalX = 0f
                            var totalY = 0f
                            while (isTracking) {
                                event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)

                                // Cancelar tracking vertical si se añade un segundo dedo (inicio diferido de pinch)
                                if (event.changes.count { it.pressed } >= 2) {
                                    isTracking = false
                                    break
                                }

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
                                            change.consume()
                                            navState = LauncherNavState.InDrawer.Main
                                            isTracking = false
                                        } else if (totalY > 80f && currentSwipeDownEnabled) {
                                            change.consume()
                                            openNotificationsWithFallback(context, currentLanguage) {
                                                showAccessibilityDisclosure = true
                                            }
                                            isTracking = false
                                        }
                                    }
                                }
                            }
                            // Esperar a que se liberen todos los punteros antes del siguiente ciclo de detección
                            while (event.changes.any { it.pressed }) {
                                event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            }
                        }
                    }
                }
                // 4B-01: Gesto de pellizco hacia adentro (Pinch-in) para abrir Settings.
                // Detector aislado de dos dedos con cálculo de zoom acumulado, umbral de 25% (0.75f),
                // exclusión de regiones físicas de widgets (PR-01/4B-01), verificación de estado raíz (LauncherNavState.Home),
                // disparo único por gesto y reinicio al liberar todos los punteros.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            var event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            val pressedPointers = event.changes.filter { it.pressed }
                            if (pressedPointers.size >= 2) {
                                val p1 = pressedPointers[0]
                                val p2 = pressedPointers[1]
                                val id1 = p1.id
                                val id2 = p2.id

                                val boundsList = currentWidgetBoundsList
                                // Si ambos dedos comenzaron dentro de regiones físicas de widgets,
                                // no capturar pinch (el usuario está interactuando con el contenido del widget).
                                val bothStartedInWidget = boundsList.isNotEmpty() &&
                                    boundsList.any { it.contains(p1.position) } &&
                                    boundsList.any { it.contains(p2.position) }

                                val isRoot = (currentNavState == LauncherNavState.Home)
                                val initialDistance = kotlin.math.hypot(
                                    p1.position.x - p2.position.x,
                                    p1.position.y - p2.position.y
                                )
                                val minInitialSpan = 60f // Evitar ruidos con dedos pegados

                                var hasTriggered = false

                                while (true) {
                                    event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    val activeChanges = event.changes.filter { it.pressed }
                                    if (activeChanges.size < 2) {
                                        break
                                    }

                                    val change1 = activeChanges.find { it.id == id1 }
                                    val change2 = activeChanges.find { it.id == id2 }

                                    if (change1 != null && change2 != null && initialDistance > minInitialSpan) {
                                        val currentDistance = kotlin.math.hypot(
                                            change1.position.x - change2.position.x,
                                            change1.position.y - change2.position.y
                                        )
                                        val cumulativeZoom = currentDistance / initialDistance

                                        // Umbral: reducción de al menos 25% respecto a la distancia inicial (zoom < 0.75f).
                                        // Mientras isWidgetEditMode sea true, no capturar pinch ni abrir Settings.
                                        if (!hasTriggered && !bothStartedInWidget && !currentWidgetEditMode && isRoot && currentPinchEnabled && cumulativeZoom < 0.75f) {
                                            hasTriggered = true
                                            change1.consume()
                                            change2.consume()
                                            onNavigateToSettings()
                                        }
                                    }
                                }

                                // Esperar a que se liberen todos los punteros antes de permitir un nuevo gesto
                                while (event.changes.any { it.pressed }) {
                                    event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                }
                            }
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
                    showAccessibilityDisclosure = true
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
                isEditMode = isWidgetEditMode,
                onEnterEditMode = { isWidgetEditMode = true },
                onWidgetBoundsChanged = { pageWidgetBoundsMap[page] = it },
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
                isEditMode = isWidgetEditMode,
                onEnterEditMode = { isWidgetEditMode = true },
                onWidgetBoundsChanged = { pageWidgetBoundsMap[page] = it },
                modifier = modifier
            )
        }
        }


        // 4. Banner flotante durante Modo Edición de Widgets
        AnimatedVisibility(
            visible = isWidgetEditMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF08080C).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.6f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (state.language == "es") "Modo Edición de Widgets" else "Widget Edit Mode",
                        color = Color(0xFF00F0FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = { isWidgetEditMode = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F0FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (state.language == "es") "Listo" else "Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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

                    // 2.5 Modo Edición de Widgets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                navState = LauncherNavState.Home
                                isWidgetEditMode = true
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar Widgets", tint = Color(0xFF00F0FF), modifier = Modifier.size(28.dp))
                        Column {
                            Text(if (isEs) "Editar widgets" else "Edit widgets", color = Color.White, fontWeight = FontWeight.Normal, fontSize = 16.sp)
                            Text(if (isEs) "Eliminar widgets colocados" else "Manage or remove placed widgets", color = Color.LightGray, fontSize = 12.sp)
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
                val installedProviders = remember {
                    widgetHostManager.getHomeScreenProviders(context)
                }
                var widgetSearchQuery by remember { mutableStateOf("") }
                val filteredProviders = remember(installedProviders, widgetSearchQuery) {
                    if (widgetSearchQuery.isBlank()) installedProviders
                    else installedProviders.filter { provider ->
                        val label = provider.loadLabel(context.packageManager)
                        val appLabel = try {
                            context.packageManager.getApplicationLabel(
                                context.packageManager.getApplicationInfo(provider.provider.packageName, 0)
                            ).toString()
                        } catch (e: Exception) { "" }
                        label.contains(widgetSearchQuery, ignoreCase = true) ||
                                appLabel.contains(widgetSearchQuery, ignoreCase = true)
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isEs) "Selecciona un Widget" else "Select a Widget",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF00F0FF),
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
                            focusedBorderColor = Color(0xFF00F0FF),
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
                            val pm = context.packageManager
                            val label = remember(providerInfo) { providerInfo.loadLabel(pm) }
                            val appName = remember(providerInfo) {
                                try { pm.getApplicationLabel(pm.getApplicationInfo(providerInfo.provider.packageName, 0)).toString() } catch (e: Exception) { "" }
                            }
                            val previewDrawable = remember(providerInfo) {
                                widgetHostManager.loadWidgetPreview(providerInfo, context)
                            }
                            val cellSpanText = remember(providerInfo) {
                                widgetHostManager.getCellSpanString(providerInfo, context)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        navState = LauncherNavState.Home
                                        val targetPage = longPressedPageIndex
                                        try {
                                            val widgetId = widgetHostManager.allocateWidgetId()
                                            allocatedWidgetId = widgetId
                                            val bound = widgetHostManager.bindAppWidgetIdIfAllowed(widgetId, providerInfo.provider)

                                            fun launchConfigureOrAdd() {
                                                if (providerInfo.configure != null) {
                                                    val activity = context as? com.daybreak.animelauncher.MainActivity
                                                    val started = activity?.startAppWidgetConfigure(widgetId) { resultCode, _ ->
                                                        if (resultCode == Activity.RESULT_OK) {
                                                            viewModel.addNativeWidget(targetPage, widgetId)
                                                        } else {
                                                            widgetHostManager.deleteWidgetId(widgetId)
                                                            Toast.makeText(context, if (isEs) "Configuración cancelada" else "Configuration cancelled", Toast.LENGTH_SHORT).show()
                                                        }
                                                        allocatedWidgetId = null
                                                    } ?: false

                                                    if (!started) {
                                                        val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                                                            component = providerInfo.configure
                                                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                                        }
                                                        try {
                                                            configureLauncher.launch(configureIntent)
                                                        } catch (e: Exception) {
                                                            widgetHostManager.deleteWidgetId(widgetId)
                                                            allocatedWidgetId = null
                                                            Toast.makeText(context, if (isEs) "No se pudo abrir la configuración" else "Could not open configuration", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    viewModel.addNativeWidget(targetPage, widgetId)
                                                    allocatedWidgetId = null
                                                }
                                            }

                                            if (bound) {
                                                launchConfigureOrAdd()
                                            } else {
                                                val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
                                                }
                                                try {
                                                    bindWidgetLauncher.launch(bindIntent)
                                                } catch (e: Exception) {
                                                    widgetHostManager.deleteWidgetId(widgetId)
                                                    allocatedWidgetId = null
                                                    Toast.makeText(context, if (isEs) "Permiso para vincular widget requerido" else "Permission to bind widget required", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            if (allocatedWidgetId != null) {
                                                widgetHostManager.deleteWidgetId(allocatedWidgetId!!)
                                                allocatedWidgetId = null
                                            }
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Preview / Icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (previewDrawable != null) {
                                        AsyncImage(
                                            model = previewDrawable,
                                            contentDescription = label,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Widgets,
                                            contentDescription = null,
                                            tint = Color(0xFF00F0FF),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = label, color = Color.White, fontWeight = FontWeight.Normal, fontSize = 15.sp)
                                    if (appName.isNotEmpty() && appName != label) {
                                        Text(text = appName, color = Color.LightGray, fontSize = 12.sp)
                                    }
                                }

                                // Tamaño en celdas (ej. 2 × 1, 4 × 2)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF00F0FF).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = cellSpanText,
                                        color = Color(0xFF00F0FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
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

    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            isEs = state.language == "es",
            onDismiss = { showAccessibilityDisclosure = false },
            onAccept = {
                showAccessibilityDisclosure = false
                try {
                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }
}

private var onAccessibilityDisclosureRequested: (() -> Unit)? = null

fun openNotificationsWithFallback(
    context: Context,
    language: String = "es",
    onRequestDisclosure: (() -> Unit)? = null
) {
    val opened = com.daybreak.animelauncher.LauncherAccessibilityService.openNotifications()
    if (!opened) {
        val disclosureAction = onRequestDisclosure ?: onAccessibilityDisclosureRequested
        if (disclosureAction != null) {
            disclosureAction()
        } else {
            Toast.makeText(
                context,
                if (language == "es") "Activa el Servicio de Accesibilidad para abrir notificaciones" else "Enable Accessibility Service to open notifications",
                Toast.LENGTH_LONG
            ).show()
            try {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun expandStatusBar(context: Context, onRequestDisclosure: (() -> Unit)? = null) {
    openNotificationsWithFallback(context, onRequestDisclosure = onRequestDisclosure)
}
