package com.daybreak.animelauncher.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    val currentCategory = categories.getOrNull(selectedCategoryIndex) ?: categories.first()
    
    val state by viewModel.state.collectAsState()
    val style = state.styleConfig

    val filteredApps = remember(searchQuery, selectedCategoryIndex, installedApps, categories) {
        val appsInCat = if (currentCategory.id == "all") {
            installedApps
        } else {
            installedApps.filter { it.packageName != null && currentCategory.packageNames.contains(it.packageName) }
        }
        
        if (searchQuery.isBlank()) {
            appsInCat
        } else {
            appsInCat.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(style.appDrawerBgColor.parseColorSafe().copy(alpha = style.appDrawerBgOpacity))
            .clickable(onClick = onClose) // Click outside to close (if not full screen, but it is)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp) // SafeArea
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
            ScrollableTabRow(
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

            // Apps Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = filteredApps, key = { it.id }) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if (app.packageName != null) {
                                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                            if (launchIntent != null) {
                                                if (app.id.contains("/")) {
                                                    launchIntent.setClassName(app.packageName, app.id.substringAfter("/"))
                                                }
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
                            modifier = Modifier.size(56.dp),
                            customIconTint = state.styleConfig?.customIconColor?.parseColorSafe()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = app.name,
                            color = if (style.appDrawerTextColor.equals("#000000", ignoreCase = true)) Color.White else style.appDrawerTextColor.parseColorSafe(),
                            fontSize = 12.sp,
                            maxLines = 1,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
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
