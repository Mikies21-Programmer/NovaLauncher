package com.daybreak.animelauncher.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlin.math.absoluteValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.daybreak.animelauncher.AppShortcut
import com.daybreak.animelauncher.DrawerCategory
import com.daybreak.animelauncher.LauncherViewModel
import com.daybreak.animelauncher.theming.LocalLauncherThemeTokens
import com.daybreak.animelauncher.theming.accent
import com.daybreak.animelauncher.theming.border
import com.daybreak.animelauncher.theming.surface
import com.daybreak.animelauncher.theming.textPrimary
import com.daybreak.animelauncher.theming.textSecondary
import com.daybreak.animelauncher.ui.components.ShortcutIcon
import com.daybreak.animelauncher.ui.theme.parseColorSafe
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.daybreak.animelauncher.getAssetImages
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    viewModel: LauncherViewModel,
    installedApps: List<AppShortcut>,
    categories: List<DrawerCategory>,
    isEs: Boolean,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val selectCategory: (Int) -> Unit = { index ->
        val clamped = index.coerceIn(0, (categories.size - 1).coerceAtLeast(0))
        if (clamped != selectedCategoryIndex) {
            selectedCategoryIndex = clamped
        }
    }
    
    // States for context menu
    var showAppMenu by remember { mutableStateOf<AppShortcut?>(null) }
    var showIconPicker by remember { mutableStateOf<AppShortcut?>(null) }
    
    // States for adding category
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    // Prioridad de botón Back dentro del App Drawer:
    // Si hay un diálogo o menú contextual abierto, lo cierra; si no, cierra el drawer.
    androidx.activity.compose.BackHandler(enabled = true) {
        if (showIconPicker != null) {
            showIconPicker = null
        } else if (showAppMenu != null) {
            showAppMenu = null
        } else if (showAddCategoryDialog) {
            showAddCategoryDialog = false
        } else {
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    val currentCategory = categories.getOrNull(selectedCategoryIndex) ?: categories.first()
    
    val state by viewModel.state.collectAsState()
    val style = state.styleConfig
    val themeTokens = LocalLauncherThemeTokens.current

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val drawerData = remember(searchQuery, selectedCategoryIndex, installedApps, categories) {
        val appsInCat = if (currentCategory.id == "all") {
            installedApps
        } else {
            installedApps.filter { it.packageName != null && currentCategory.packageNames.contains(it.packageName) }
        }
        
        val searchFiltered = if (searchQuery.isBlank()) {
            appsInCat
        } else {
            appsInCat.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        buildDrawerData(searchFiltered)
    }

    // Letra activa derivada de la posición real de la lista, considerando tanto el inicio
    // como la progresión hacia el final y el tope inferior (W -> Z).
    val activeLetter by remember(drawerData.items) {
        derivedStateOf {
            val items = drawerData.items
            if (items.isEmpty()) return@derivedStateOf null

            // Si estamos al inicio absoluto o la lista no tiene scroll hacia atrás
            if (!listState.canScrollBackward) {
                return@derivedStateOf items.first().sectionChar
            }

            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                return@derivedStateOf items.getOrNull(listState.firstVisibleItemIndex)?.sectionChar
            }

            val firstVisible = visibleItems.first()
            val lastVisible = visibleItems.last()

            // Si el último elemento de la lista está en pantalla, transicionamos hacia el final
            if (lastVisible.index >= items.lastIndex) {
                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                val bottomEdge = lastVisible.offset + lastVisible.size
                val remainingScroll = bottomEdge - layoutInfo.viewportEndOffset + layoutInfo.afterContentPadding

                if (remainingScroll <= 0 || !listState.canScrollForward) {
                    items.last().sectionChar
                } else if (viewportHeight > 0) {
                    val progress = (1f - (remainingScroll.toFloat() / viewportHeight)).coerceIn(0f, 1f)
                    val targetIndex = (firstVisible.index + ((items.lastIndex - firstVisible.index) * progress).toInt())
                        .coerceIn(firstVisible.index, items.lastIndex)
                    items[targetIndex].sectionChar
                } else {
                    items[firstVisible.index].sectionChar
                }
            } else {
                items.getOrNull(firstVisible.index)?.sectionChar
            }
        }
    }

    var isIndexVisible by remember { mutableStateOf(false) }
    var isTouchingIndex by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    val scrollChannel = remember { Channel<Int>(Channel.CONFLATED) }
    val hideJobHolder = remember { object { var job: Job? = null } }

    // Desplazamiento instantáneo sin acumulación ni animación ("latest event wins")
    LaunchedEffect(listState) {
        for (targetIndex in scrollChannel) {
            if (listState.firstVisibleItemIndex != targetIndex) {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    // Visibilidad del índice alfabético según scrollInProgress y searchQuery
    LaunchedEffect(listState, searchQuery) {
        if (searchQuery.isNotBlank()) {
            isIndexVisible = false
            selectedLetter = null
            return@LaunchedEffect
        }
        snapshotFlow { listState.isScrollInProgress }
            .collectLatest { isScrolling ->
                if (isScrolling) {
                    isIndexVisible = true
                } else {
                    delay(700)
                    if (!isTouchingIndex) {
                        isIndexVisible = false
                        selectedLetter = null
                    }
                }
            }
    }

    // Reseteo de scroll y cancelación de saltos al cambiar categoría o búsqueda
    LaunchedEffect(selectedCategoryIndex, searchQuery) {
        hideJobHolder.job?.cancel()
        selectedLetter = null
        if (drawerData.items.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(style.appDrawerBgColor.parseColorSafe().copy(alpha = style.appDrawerBgOpacity))
            .clickable(onClick = onClose) // Click outside to close
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .clickable(enabled = false, onClick = {}) // Consume clicks inside
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(if (isEs) "Buscar aplicaciones..." else "Search apps...", color = Color.LightGray.copy(alpha = 0.6f)) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = themeTokens.accent.copy(alpha = 0.7f)
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeTokens.border,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = themeTokens.accent.copy(alpha = 0.04f),
                    unfocusedContainerColor = themeTokens.surface.copy(alpha = 0.20f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = themeTokens.accent,
                    focusedLeadingIconColor = themeTokens.accent,
                    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            // Tabs for categories (Motion System: single sliding glass pill)
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = Color.Transparent,
                contentColor = themeTokens.accent,
                edgePadding = 16.dp,
                indicator = {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(selectedCategoryIndex)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .border(
                                width = 1.dp,
                                color = themeTokens.border.copy(alpha = 0.50f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(
                                color = themeTokens.accent.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                },
                divider = {}
            ) {
                categories.forEachIndexed { index, category ->
                    val isSelected = selectedCategoryIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectCategory(index) },
                        selectedContentColor = themeTokens.accent,
                        unselectedContentColor = themeTokens.textSecondary
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .border(
                                    width = 1.dp,
                                    color = themeTokens.border.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.name,
                                color = if (isSelected) themeTokens.accent else themeTokens.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                // Add category button
                IconButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Category", tint = themeTokens.accent)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Apps List (Nova Drawer — Hybrid Stream)
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val currentCategoryIndex by rememberUpdatedState(selectedCategoryIndex)

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 8.dp,
                    end = 20.dp,
                    bottom = 16.dp + navBarBottom
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        val touchSlop = viewConfiguration.touchSlop
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                var totalX = 0f
                                var totalY = 0f
                                var isSlopDecided = false
                                var isHorizontalSwipe = false
                                var hasSwiped = false

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break

                                    val dragX = change.position.x - change.previousPosition.x
                                    val dragY = change.position.y - change.previousPosition.y
                                    totalX += dragX
                                    totalY += dragY

                                    if (!isSlopDecided) {
                                        val absX = totalX.absoluteValue
                                        val absY = totalY.absoluteValue
                                        if (absY > touchSlop && absY >= absX) {
                                            isSlopDecided = true
                                            isHorizontalSwipe = false
                                        } else if (absX > touchSlop && absX > absY * 1.25f) {
                                            isSlopDecided = true
                                            isHorizontalSwipe = true
                                        }
                                    }

                                    if (isHorizontalSwipe) {
                                        change.consume()
                                        if (!hasSwiped) {
                                            hasSwiped = true
                                            if (totalX < 0) {
                                                // Swipe izquierda: actualIndex + 1
                                                selectCategory(currentCategoryIndex + 1)
                                            } else {
                                                // Swipe derecha: actualIndex - 1
                                                selectCategory(currentCategoryIndex - 1)
                                            }
                                        }
                                    } else if (isSlopDecided) {
                                        // Gesto vertical identificado: ceder control al scroll de LazyColumn
                                        break
                                    }
                                }
                            }
                        }
                    }
            ) {
                items(
                    items = drawerData.items,
                    key = { item ->
                        when (item) {
                            is DrawerListItem.Header -> "header_${item.title}"
                            is DrawerListItem.AppRow -> "app_${item.app.id}"
                        }
                    },
                    contentType = { item ->
                        when (item) {
                            is DrawerListItem.Header -> "header"
                            is DrawerListItem.AppRow -> "app_row"
                        }
                    }
                ) { item ->
                    when (item) {
                        is DrawerListItem.Header -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 18.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    color = themeTokens.accent,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(themeTokens.accent.copy(alpha = 0.22f))
                                )
                            }
                        }
                        is DrawerListItem.AppRow -> {
                            val app = item.app
                            DrawerAppRowItem(
                                app = app,
                                textColor = if (style.appDrawerTextColor.equals("#000000", ignoreCase = true)) Color.White else style.appDrawerTextColor.parseColorSafe(),
                                customIconTint = state.styleConfig?.customIconColor?.parseColorSafe(),
                                onTap = {
                                    if (app.packageName != null) {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                        if (launchIntent != null) {
                                            if (app.id.contains("/")) {
                                                launchIntent.setClassName(app.packageName, app.id.substringAfter("/"))
                                            }
                                            onClose()
                                            context.startActivity(launchIntent)
                                        }
                                    }
                                },
                                onLongPress = {
                                    showAppMenu = app
                                }
                            )
                        }
                    }
                }
            }
        }

        // Alphabet Index Rail (Bloque 2)
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        AnimatedVisibility(
            visible = isIndexVisible && searchQuery.isBlank() && drawerData.availableSections.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    top = 100.dp,
                    bottom = 16.dp + navBarBottom,
                    end = 4.dp
                )
        ) {
            AlphabetIndexRail(
                sections = drawerData.availableSections,
                activeLetter = activeLetter,
                selectedLetter = selectedLetter,
                isTouchingIndex = isTouchingIndex,
                onLetterSelected = { char ->
                    selectedLetter = char
                    val targetIndex = drawerData.sectionIndexMap[char]
                    if (targetIndex != null && targetIndex in 0 until drawerData.items.size) {
                        scrollChannel.trySend(targetIndex)
                    }
                },
                onInteractionStateChange = { interacting ->
                    isTouchingIndex = interacting
                    if (interacting) {
                        isIndexVisible = true
                    } else {
                        hideJobHolder.job?.cancel()
                        hideJobHolder.job = coroutineScope.launch {
                            delay(700)
                            if (!listState.isScrollInProgress && !isTouchingIndex) {
                                isIndexVisible = false
                                selectedLetter = null
                            }
                        }
                    }
                }
            )
        }
    }

    // App Context Menu Dialog
    if (showAppMenu != null) {
        val app = showAppMenu!!
        AlertDialog(
            onDismissRequest = { showAppMenu = null },
            containerColor = themeTokens.surface,
            titleContentColor = themeTokens.accent,
            textContentColor = Color.White,
            title = { Text(app.name) },
            text = {
                Column {
                    Button(
                        onClick = {
                            showIconPicker = app
                            showAppMenu = null
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeTokens.accent, contentColor = Color.Black)
                    ) {
                        Text(if (isEs) "🎨 Cambiar Icono" else "🎨 Change Icon", fontWeight = FontWeight.Bold)
                    }

                    Text(if (isEs) "Mover a categoría:" else "Move to category:")
                    Spacer(modifier = Modifier.height(8.dp))
                    categories.forEach { category ->
                        if (category.id != "all") {
                            val inCategory = category.packageNames.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (app.packageName != null) {
                                            viewModel.moveAppToCategory(app.packageName, category.id)
                                        }
                                        showAppMenu = null
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = inCategory,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = themeTokens.accent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(category.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppMenu = null }) {
                    Text(if (isEs) "Cerrar" else "Close", color = themeTokens.accent)
                }
            }
        )
    }

    if (showIconPicker != null) {
        val app = showIconPicker!!
        val iconPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) { }
                if (app.packageName != null) {
                    viewModel.updateDrawerAppIcon(app.packageName, uri.toString())
                }
                showIconPicker = null
            }
        }
        
        val presetIcons = remember { getAssetImages(context, "iconos") }

        AlertDialog(
            onDismissRequest = { showIconPicker = null },
            containerColor = themeTokens.surface,
            titleContentColor = themeTokens.accent,
            textContentColor = Color.White,
            title = { Text(if (isEs) "Cambiar Icono" else "Change Icon") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    Button(
                        onClick = {
                            iconPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeTokens.accent, contentColor = Color.Black)
                    ) {
                        Text(if (isEs) "Elegir desde Galería" else "Choose from Gallery", fontWeight = FontWeight.Bold)
                    }
                    
                    Text(if (isEs) "Iconos predeterminados:" else "Default icons:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(50.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(presetIcons) { iconUri ->
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray.copy(alpha=0.3f))
                                    .clickable {
                                        if (app.packageName != null) {
                                            viewModel.updateDrawerAppIcon(app.packageName, iconUri)
                                        }
                                        showIconPicker = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(LocalContext.current)
                                            .data(iconUri)
                                            .build()
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPicker = null }) {
                    Text(if (isEs) "Cerrar" else "Close", color = themeTokens.accent)
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = themeTokens.surface,
            titleContentColor = themeTokens.accent,
            textContentColor = Color.White,
            title = { Text(if (isEs) "Nueva Categoría" else "New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(if (isEs) "Nombre" else "Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeTokens.border,
                        focusedLabelColor = themeTokens.accent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addDrawerCategory(newCategoryName)
                            newCategoryName = ""
                        }
                        showAddCategoryDialog = false
                    }
                ) {
                    Text(if (isEs) "Añadir" else "Add", color = themeTokens.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(if (isEs) "Cancelar" else "Cancel", color = Color.Gray)
                }
            }
        )
    }
}

/**
 * Rail vertical de índice alfabético A-Z (Bloque 2).
 * - Estética minimalista/glass acorde a NovaLauncher.
 * - Soporta tanto tap directo sobre una letra como desplazamiento continuo (drag) táctil.
 * - Tap feedback: pulse calibrado (1.0f -> 1.12f -> 1.0f, ~125ms), halo perceptible y tarjeta flotante glass (42dp, fade+scale).
 * - Scrubbing: seguimiento directo 1:1, actualización instantánea sin tarjeta flotante ni animaciones intermedias ("latest event wins").
 */
@Composable
private fun AlphabetIndexRail(
    sections: List<Char>,
    activeLetter: Char?,
    selectedLetter: Char?,
    isTouchingIndex: Boolean,
    onLetterSelected: (Char) -> Unit,
    onInteractionStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sections.isEmpty()) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val verticalPaddingPx = with(density) { 6.dp.toPx() }
    var railHeightPx by remember { mutableFloatStateOf(0f) }
    val itemHeightDp = (480.dp / sections.size.coerceAtLeast(1)).coerceIn(14.dp, 26.dp)

    var scrubbedLetter by remember { mutableStateOf<Char?>(null) }
    var tappedLetter by remember { mutableStateOf<Char?>(null) }
    var lastTappedLetter by remember { mutableStateOf<Char?>(null) }
    var tappedLetterCenterY by remember { mutableFloatStateOf(0f) }

    val tapHaloScale = remember { Animatable(1f) }
    val cardAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.85f) }

    val tapHaloJobHolder = remember { object { var job: Job? = null } }
    val cardAnimJobHolder = remember { object { var job: Job? = null } }
    val tapDismissJobHolder = remember { object { var job: Job? = null } }
    val themeTokens = LocalLauncherThemeTokens.current

    val currentActiveChar = if (isTouchingIndex) {
        scrubbedLetter ?: selectedLetter ?: activeLetter
    } else {
        selectedLetter ?: activeLetter
    }

    Box(
        modifier = modifier
            .width(38.dp)
            .border(
                width = 0.5.dp,
                color = themeTokens.border.copy(alpha = if (isTouchingIndex) 0.35f else 0.15f),
                shape = RoundedCornerShape(19.dp)
            )
            .background(
                color = themeTokens.surface.copy(alpha = 0.80f),
                shape = RoundedCornerShape(19.dp)
            )
            .onGloballyPositioned { coordinates ->
                railHeightPx = coordinates.size.height.toFloat()
            }
            .pointerInput(sections) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onInteractionStateChange(true)
                    val railHeight = if (this.size.height > 0) this.size.height.toFloat() else railHeightPx
                    var lastIndex = -1
                    var hasScrubbed = false

                    if (railHeight > 0 && sections.isNotEmpty()) {
                        val contentHeight = (railHeight - 2 * verticalPaddingPx).coerceAtLeast(1f)
                        val relativeY = (down.position.y - verticalPaddingPx).coerceIn(0f, contentHeight - 1f)
                        val index = ((relativeY / contentHeight) * sections.size).toInt().coerceIn(0, sections.size - 1)
                        lastIndex = index
                        val char = sections[index]
                        scrubbedLetter = char
                        onLetterSelected(char)

                        // Feedback táctil inmediato en TAP directo
                        tappedLetter = char
                        lastTappedLetter = char
                        val itemHeightPx = contentHeight / sections.size
                        tappedLetterCenterY = verticalPaddingPx + (index + 0.5f) * itemHeightPx

                        tapDismissJobHolder.job?.cancel()
                        tapHaloJobHolder.job?.cancel()
                        tapHaloJobHolder.job = coroutineScope.launch {
                            tapHaloScale.snapTo(1f)
                            tapHaloScale.animateTo(1.12f, tween(60, easing = FastOutSlowInEasing))
                            tapHaloScale.animateTo(1.0f, tween(65, easing = FastOutSlowInEasing))
                        }

                        cardAnimJobHolder.job?.cancel()
                        cardAnimJobHolder.job = coroutineScope.launch {
                            cardAlpha.snapTo(0f)
                            cardScale.snapTo(0.85f)
                            cardAlpha.animateTo(1f, tween(40, easing = LinearEasing))
                            cardScale.animateTo(1f, tween(50, easing = FastOutSlowInEasing))
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        change.consume()
                        if (!change.pressed) {
                            break
                        }
                        if (railHeight > 0 && sections.isNotEmpty()) {
                            val contentHeight = (railHeight - 2 * verticalPaddingPx).coerceAtLeast(1f)
                            val relativeY = (change.position.y - verticalPaddingPx).coerceIn(0f, contentHeight - 1f)
                            val index = ((relativeY / contentHeight) * sections.size).toInt().coerceIn(0, sections.size - 1)
                            if (index != lastIndex) {
                                lastIndex = index
                                hasScrubbed = true
                                // En scrubbing continuo: sin tarjeta, sin pulse acumulado, seguimiento 1:1 directo
                                tappedLetter = null
                                cardAnimJobHolder.job?.cancel()
                                coroutineScope.launch {
                                    cardAlpha.snapTo(0f)
                                }
                                tapHaloJobHolder.job?.cancel()
                                coroutineScope.launch {
                                    tapHaloScale.snapTo(1f)
                                }
                                scrubbedLetter = sections[index]
                                onLetterSelected(sections[index])
                            }
                        }
                    }

                    onInteractionStateChange(false)
                    if (!hasScrubbed) {
                        // TAP confirmado: mantener tarjeta y halo el tiempo calibrado (~125ms)
                        tapDismissJobHolder.job?.cancel()
                        tapDismissJobHolder.job = coroutineScope.launch {
                            delay(125)
                            cardAnimJobHolder.job?.cancel()
                            cardAlpha.animateTo(0f, tween(50, easing = LinearEasing))
                            cardScale.animateTo(0.85f, tween(50, easing = FastOutSlowInEasing))
                            tappedLetter = null
                            scrubbedLetter = null
                        }
                    } else {
                        tappedLetter = null
                        scrubbedLetter = null
                        cardAnimJobHolder.job?.cancel()
                        coroutineScope.launch {
                            cardAlpha.snapTo(0f)
                        }
                    }
                }
            }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.wrapContentHeight()
        ) {
            sections.forEach { char ->
                val isActive = char == currentActiveChar
                val isNovaChar = char in "NOVA"
                val isTapped = char == tappedLetter
                val currentHaloScale = if (isTapped) tapHaloScale.value else 1f

                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .graphicsLayer {
                                    scaleX = currentHaloScale
                                    scaleY = currentHaloScale
                                }
                                .background(
                                    color = themeTokens.accent.copy(alpha = if (isTapped) 0.30f else 0.22f),
                                    shape = CircleShape
                                )
                        )
                    }
                    Text(
                        text = char.toString(),
                        color = if (isActive) themeTokens.accent else Color.White.copy(alpha = if (isNovaChar) 0.75f else 0.50f),
                        fontSize = if (isActive) 12.sp else 10.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
        }

        // Tarjeta Glass temporal a la izquierda en TAP directo (~125ms)
        if (cardAlpha.value > 0f || tappedLetter != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = -52.dp.roundToPx(),
                            y = (tappedLetterCenterY - (railHeightPx / 2f)).toInt()
                        )
                    }
                    .graphicsLayer {
                        alpha = cardAlpha.value
                        scaleX = cardScale.value
                        scaleY = cardScale.value
                    }
                    .size(42.dp)
                    .background(
                        color = themeTokens.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = themeTokens.border.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (tappedLetter ?: lastTappedLetter)?.toString() ?: "",
                    color = themeTokens.accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Fila individual de aplicación con microinteracción táctil ultra-rápida (70ms).
 * El feedback visual (alpha 1.0 -> 0.85) opera en graphicsLayer sin recomposición,
 * y el lanzamiento de la actividad se despacha inmediatamente en onTap sin bloquear.
 */
@Composable
private fun DrawerAppRowItem(
    app: AppShortcut,
    textColor: Color,
    customIconTint: Color?,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val rowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1.0f,
        animationSpec = tween(durationMillis = 70, easing = LinearEasing),
        label = "row_tap_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(vertical = 4.dp)
            .graphicsLayer {
                alpha = rowAlpha
            }
            .pointerInput(app.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        onTap()
                    },
                    onLongPress = {
                        onLongPress()
                    }
                )
            }
    ) {
        ShortcutIcon(
            shortcut = app,
            modifier = Modifier.size(48.dp),
            customIconTint = customIconTint
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.name,
            color = textColor,
            fontSize = 15.sp,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}
