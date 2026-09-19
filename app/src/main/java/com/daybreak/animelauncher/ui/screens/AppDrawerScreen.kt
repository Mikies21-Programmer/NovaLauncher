package com.daybreak.animelauncher.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.daybreak.animelauncher.AppShortcut
import com.daybreak.animelauncher.DrawerCategory
import com.daybreak.animelauncher.LauncherViewModel
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

    // Letra activa derivada eficientemente de listState.firstVisibleItemIndex
    val activeLetter by remember(drawerData.items) {
        derivedStateOf {
            drawerData.items.getOrNull(listState.firstVisibleItemIndex)?.sectionChar
        }
    }

    var isIndexVisible by remember { mutableStateOf(false) }
    var isTouchingIndex by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var hideJob by remember { mutableStateOf<Job?>(null) }

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
        scrollJob?.cancel()
        hideJob?.cancel()
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
                placeholder = { Text(if (isEs) "Buscar aplicaciones..." else "Search apps...", color = Color.LightGray) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF00F0FF)) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00F0FF),
                    unfocusedBorderColor = Color(0xFF00F0FF).copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00F0FF)
                ),
                singleLine = true
            )

            // Tabs for categories
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF00F0FF),
                edgePadding = 16.dp,
                divider = {}
            ) {

                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = { Text(category.name, fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
                // Add category button
                IconButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Category", tint = Color(0xFF00F0FF))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Apps List (Nova Drawer — Hybrid Stream)
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 8.dp,
                    end = 20.dp,
                    bottom = 16.dp + navBarBottom
                ),
                modifier = Modifier.fillMaxSize()
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
                                    .padding(top = 16.dp, bottom = 6.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    color = Color(0xFF00F0FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color(0xFF00F0FF).copy(alpha = 0.25f))
                                )
                            }
                        }
                        is DrawerListItem.AppRow -> {
                            val app = item.app
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 58.dp)
                                    .padding(vertical = 4.dp)
                                    .pointerInput(app.id) {
                                        detectTapGestures(
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
                            ) {
                                ShortcutIcon(
                                    shortcut = app,
                                    modifier = Modifier.size(48.dp),
                                    customIconTint = state.styleConfig?.customIconColor?.parseColorSafe()
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = app.name,
                                    color = if (style.appDrawerTextColor.equals("#000000", ignoreCase = true)) Color.White else style.appDrawerTextColor.parseColorSafe(),
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Normal
                                )
                            }
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
                onLetterSelected = { char ->
                    selectedLetter = char
                    val targetIndex = drawerData.sectionIndexMap[char]
                    if (targetIndex != null && targetIndex in 0 until drawerData.items.size) {
                        scrollJob?.cancel()
                        scrollJob = coroutineScope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    }
                },
                onInteractionStateChange = { interacting ->
                    isTouchingIndex = interacting
                    if (interacting) {
                        isIndexVisible = true
                    } else {
                        hideJob?.cancel()
                        hideJob = coroutineScope.launch {
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
            containerColor = Color(0xFF08080C),
            titleContentColor = Color(0xFF00F0FF),
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)
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
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00F0FF))
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
                    Text(if (isEs) "Cerrar" else "Close", color = Color(0xFF00F0FF))
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
            containerColor = Color(0xFF08080C),
            titleContentColor = Color(0xFF00F0FF),
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black)
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
                    Text(if (isEs) "Cerrar" else "Close", color = Color(0xFF00F0FF))
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = Color(0xFF08080C),
            titleContentColor = Color(0xFF00F0FF),
            textContentColor = Color.White,
            title = { Text(if (isEs) "Nueva Categoría" else "New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(if (isEs) "Nombre" else "Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00F0FF),
                        focusedLabelColor = Color(0xFF00F0FF),
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
                    Text(if (isEs) "Añadir" else "Add", color = Color(0xFF00F0FF))
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
 * - Precalcula la altura por elemento para mantener un tamaño proporcional y óptimo de área de contacto.
 */
@Composable
private fun AlphabetIndexRail(
    sections: List<Char>,
    activeLetter: Char?,
    selectedLetter: Char?,
    onLetterSelected: (Char) -> Unit,
    onInteractionStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sections.isEmpty()) return

    var railHeightPx by remember { mutableFloatStateOf(0f) }
    val itemHeightDp = (480.dp / sections.size.coerceAtLeast(1)).coerceIn(14.dp, 26.dp)

    Box(
        modifier = modifier
            .width(38.dp)
            .background(
                color = Color(0xCC08080C),
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
                    if (railHeight > 0 && sections.isNotEmpty()) {
                        val itemHeight = railHeight / sections.size
                        val index = (down.position.y / itemHeight).toInt().coerceIn(0, sections.size - 1)
                        lastIndex = index
                        onLetterSelected(sections[index])
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        change.consume()
                        if (!change.pressed) {
                            break
                        }
                        if (railHeight > 0 && sections.isNotEmpty()) {
                            val itemHeight = railHeight / sections.size
                            val index = (change.position.y / itemHeight).toInt().coerceIn(0, sections.size - 1)
                            if (index != lastIndex) {
                                lastIndex = index
                                onLetterSelected(sections[index])
                            }
                        }
                    }
                    onInteractionStateChange(false)
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
                val isActive = char == (selectedLetter ?: activeLetter)
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.toString(),
                        color = if (isActive) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.55f),
                        fontSize = if (isActive) 12.sp else 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
