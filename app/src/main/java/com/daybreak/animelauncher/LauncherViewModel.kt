package com.daybreak.animelauncher

import android.app.Application
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppShortcut(
    val id: String,
    val name: String,
    val packageName: String? = null,
    val customIconUri: String? = null
)

data class ViewConfig(
    val quote: String = "Dueño de mi propio destino",
    val backgroundUri: String = "",
    val showDateWidget: Boolean = true,
    val showStatusWidget: Boolean = true,
    val nativeWidgetIds: List<Int> = emptyList(), // Android System AppWidget IDs
    val diagonalShortcuts: List<AppShortcut> = listOf(
        AppShortcut("default_search", "Buscar"),
        AppShortcut("default_camera", "Cámara"),
        AppShortcut("default_chat", "Chat"),
        AppShortcut("default_phone", "Teléfono")
    ),
    val sidebarShortcuts: List<AppShortcut> = listOf(
        AppShortcut("default_home", "Inicio"),
        AppShortcut("default_help", "Ayuda"),
        AppShortcut("default_headphones", "Audio"),
        AppShortcut("default_calendar", "Calendario")
    ),
    val gridPages: List<List<AppShortcut>> = listOf(
        listOf(
            AppShortcut("default_calc", "Calculadora"),
            AppShortcut("default_cloud", "Nube"),
            AppShortcut("default_car", "Auto"),
            AppShortcut("default_eco", "Eco"),
            AppShortcut("default_fav", "Favoritos"),
            AppShortcut("default_game", "Juegos"),
            AppShortcut("default_audio", "Music"),
            AppShortcut("default_image", "Fotos"),
            AppShortcut("default_key", "Ajustes")
        )
    )
)

data class AdvancedStyleConfig(
    // Colores de elementos individuales (en HEX)
    val miButtonColor: String = "#00F0FF",
    val miTextColor: String = "#000000",
    val clockColor: String = "#00F0FF",
    val batteryColor: String = "#00F0FF",
    val messagesColor: String = "#00F0FF",
    val dateColor: String = "#00F0FF",
    
    // Opacidad y color para contenedores y formas
    val diagonalBarOpacity: Float = 0.96f,
    val diagonalBarColor: String = "#050508",
    
    val sidebarOpacity: Float = 0.88f,
    val sidebarColor: String = "#08080C",
    
    val triangleOpacity: Float = 0.85f,
    val triangleColor: String = "#00F0FF",
    val triangleWidth: Float = 0.88f,
    
    val appDrawerBgColor: String = "#030305",
    val appDrawerBgOpacity: Float = 0.75f,
    val appDrawerTextColor: String = "#FFFFFF",
    val customIconColor: String = "#FFFFFF"
)

data class DrawerCategory(
    val id: String,
    val name: String,
    val packageNames: List<String> = emptyList()
)

data class DrawerConfig(
    val categories: List<DrawerCategory> = listOf(
        DrawerCategory("all", "Todas"),
        DrawerCategory("entertainment", "Entretenimiento"),
        DrawerCategory("finance", "Finanzas"),
        DrawerCategory("social", "Redes Sociales")
    )
)

data class GesturesConfig(
    val doubleTapToSleep: Boolean = true,
    val swipeDownForNotifications: Boolean = true,
    val edgeSwipeToBack: Boolean = true,
    val bottomSwipeToRecents: Boolean = true,
    val pinchInForSettings: Boolean = true,
    val immersiveMode: Boolean = true
)

data class LauncherState(
    val viewCount: Int = 2,
    val language: String = "es",
    val styleConfig: AdvancedStyleConfig = AdvancedStyleConfig(),
    val viewConfigs: List<ViewConfig> = listOf(
        ViewConfig(quote = "Dueño de mi propio destino"), // View 1
        ViewConfig(quote = "Explora el horizonte") // View 2 default
    ),
    val drawerConfig: DrawerConfig = DrawerConfig(),
    val gesturesConfig: GesturesConfig = GesturesConfig(),
    val customAppIcons: Map<String, String> = emptyMap(),
    val hasCompletedOnboarding: Boolean = false,
    val onboardingStep: Int = 0
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()
    
    val appWidgetHost: AppWidgetHost = AppWidgetHost(application, 1024).apply {
        startListening()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadState(): LauncherState {
        val json = prefs.getString("launcher_state", null)
        if (json != null) {
            try {
                val loaded = gson.fromJson(json, LauncherState::class.java)
                if (loaded != null) {
                    return loaded.copy(
                        styleConfig = loaded.styleConfig ?: AdvancedStyleConfig(),
                        viewConfigs = loaded.viewConfigs ?: listOf(
                            ViewConfig(quote = "Dueño de mi propio destino"),
                            ViewConfig(quote = "Explora el horizonte")
                        ),
                        hasCompletedOnboarding = loaded.hasCompletedOnboarding,
                        onboardingStep = loaded.onboardingStep
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return LauncherState()
    }

    private fun saveState(newState: LauncherState) {
        try {
            val json = gson.toJson(newState)
            prefs.edit().putString("launcher_state", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateState(transform: (LauncherState) -> LauncherState) {
        _state.update { currentState ->
            val newState = transform(currentState)
            saveState(newState)
            newState
        }
    }

    fun updateStyleConfig(newConfig: AdvancedStyleConfig) {
        updateState { it.copy(styleConfig = newConfig) }
    }

    fun resetStyleConfig() {
        updateState { it.copy(styleConfig = AdvancedStyleConfig()) }
    }

    fun setLanguage(lang: String) {
        updateState { it.copy(language = lang) }
    }

    fun updateHasCompletedOnboarding(completed: Boolean) {
        updateState { it.copy(hasCompletedOnboarding = completed) }
    }

    fun updateOnboardingStep(step: Int) {
        updateState { it.copy(onboardingStep = step) }
    }

    fun updateGesturesConfig(config: GesturesConfig) {
        updateState { it.copy(gesturesConfig = config) }
    }

    private val _installedApps = MutableStateFlow<List<AppShortcut>>(emptyList())
    val installedApps: StateFlow<List<AppShortcut>> = _installedApps.asStateFlow()

    fun loadInstalledApps(context: Context) {
        if (_installedApps.value.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val apps = getInstalledApps(context)
                val stateIcons = _state.value.customAppIcons
                val mappedApps = apps.map { app ->
                    if (stateIcons.containsKey(app.packageName)) {
                        app.copy(customIconUri = stateIcons[app.packageName])
                    } else app
                }
                _installedApps.value = mappedApps
                autoCategorizeApps(context, mappedApps)
            }
        }
    }

    private fun autoCategorizeApps(context: Context, apps: List<AppShortcut>) {
        val pm = context.packageManager
        val state = _state.value
        val currentCategories = state.drawerConfig.categories.toMutableList()
        
        // Obtenemos todas las aplicaciones que ya están en alguna categoría manual (excepto "all")
        val assignedPackages = currentCategories.flatMap { 
            if (it.id != "all") it.packageNames else emptyList() 
        }.toSet()
        
        var modified = false
        
        for (app in apps) {
            if (app.packageName == null || assignedPackages.contains(app.packageName)) continue
            
            var targetCategoryId: String? = null
            
            // 1. Detección oficial nativa de Android
            try {
                val appInfo = pm.getApplicationInfo(app.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (appInfo.category) {
                        ApplicationInfo.CATEGORY_GAME,
                        ApplicationInfo.CATEGORY_AUDIO,
                        ApplicationInfo.CATEGORY_VIDEO -> targetCategoryId = "entertainment"
                        ApplicationInfo.CATEGORY_SOCIAL -> targetCategoryId = "social"
                        ApplicationInfo.CATEGORY_PRODUCTIVITY -> targetCategoryId = "finance"
                    }
                }
            } catch (e: Exception) { }
            
            // 2. Filtro de palabras clave en el nombre del paquete o nombre visible
            if (targetCategoryId == null) {
                val pkg = app.packageName.lowercase()
                val name = app.name.lowercase()
                
                if (pkg.contains("bank") || pkg.contains("pago") || pkg.contains("pay") || 
                    pkg.contains("finan") || pkg.contains("wallet") || pkg.contains("banco") ||
                    pkg.contains("bbva") || pkg.contains("santander") || pkg.contains("banamex") ||
                    name.contains("banco") || name.contains("bank") || name.contains("finanzas") ||
                    name.contains("prestamo") || name.contains("loan") || name.contains("inversion")) {
                    targetCategoryId = "finance"
                } else if (pkg.contains("game") || pkg.contains("juego") || pkg.contains("netflix") || 
                           pkg.contains("spotify") || pkg.contains("tiktok") || pkg.contains("youtube") ||
                           name.contains("juego") || name.contains("game")) {
                    targetCategoryId = "entertainment"
                } else if (pkg.contains("social") || pkg.contains("facebook") || pkg.contains("instagram") ||
                           pkg.contains("twitter") || pkg.contains("whatsapp") || pkg.contains("telegram") ||
                           pkg.contains("messenger") || pkg.contains("snapchat") || pkg.contains("discord")) {
                    targetCategoryId = "social"
                }
            }
            
            // Si detectamos categoría, añadimos el app y marcamos modificado
            if (targetCategoryId != null) {
                val index = currentCategories.indexOfFirst { it.id == targetCategoryId }
                if (index != -1) {
                    val cat = currentCategories[index]
                    currentCategories[index] = cat.copy(packageNames = cat.packageNames + app.packageName)
                    modified = true
                }
            }
        }
        
        if (modified) {
            updateState { s ->
                s.copy(drawerConfig = s.drawerConfig.copy(categories = currentCategories))
            }
        }
    }
    fun updateDrawerAppIcon(packageName: String, uri: String?) {
        updateState { currentState ->
            val newIcons = currentState.customAppIcons.toMutableMap()
            if (uri != null) {
                newIcons[packageName] = uri
            } else {
                newIcons.remove(packageName)
            }
            currentState.copy(customAppIcons = newIcons)
        }
        
        _installedApps.value = _installedApps.value.map { app ->
            if (app.packageName == packageName) {
                app.copy(customIconUri = uri)
            } else {
                app
            }
        }
    }


    fun updateShortcutIcon(viewIndex: Int, isDiagonal: Boolean, shortcutIndex: Int, uri: String?) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                if (isDiagonal) {
                    val list = currentConfig.diagonalShortcuts.toMutableList()
                    if (shortcutIndex in list.indices) {
                        list[shortcutIndex] = list[shortcutIndex].copy(customIconUri = uri)
                        newConfigs[viewIndex] = currentConfig.copy(diagonalShortcuts = list)
                    }
                } else {
                    val list = currentConfig.sidebarShortcuts.toMutableList()
                    if (shortcutIndex in list.indices) {
                        list[shortcutIndex] = list[shortcutIndex].copy(customIconUri = uri)
                        newConfigs[viewIndex] = currentConfig.copy(sidebarShortcuts = list)
                    }
                }
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun updateGridShortcutIcon(viewIndex: Int, pageIndex: Int, shortcutIndex: Int, uri: String?) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                val pages = currentConfig.gridPages.toMutableList()
                if (pageIndex in pages.indices) {
                    val list = pages[pageIndex].toMutableList()
                    if (shortcutIndex in list.indices) {
                        list[shortcutIndex] = list[shortcutIndex].copy(customIconUri = uri)
                        pages[pageIndex] = list
                        newConfigs[viewIndex] = currentConfig.copy(gridPages = pages)
                    }
                }
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun addView() {
        updateState { currentState ->
            if (currentState.viewCount < 8) {
                currentState.copy(
                    viewCount = currentState.viewCount + 1,
                    viewConfigs = currentState.viewConfigs + ViewConfig(quote = "Nueva Vista ${currentState.viewCount + 1}")
                )
            } else {
                currentState
            }
        }
    }

    fun removeView() {
        updateState { currentState ->
            if (currentState.viewCount > 2) {
                currentState.copy(
                    viewCount = currentState.viewCount - 1,
                    viewConfigs = currentState.viewConfigs.dropLast(1)
                )
            } else {
                currentState
            }
        }
    }

    fun updateQuote(index: Int, newQuote: String) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (index in newConfigs.indices) {
                newConfigs[index] = newConfigs[index].copy(quote = newQuote)
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun updateBackground(index: Int, newUrl: String) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (index in newConfigs.indices) {
                newConfigs[index] = newConfigs[index].copy(backgroundUri = newUrl)
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun updateBackgroundUri(index: Int, newUri: String) {
        updateBackground(index, newUri)
        
        // Sincronizar con el fondo del sistema para la multitarea (cuadrado)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val wallpaperManager = WallpaperManager.getInstance(context)
                val uri = android.net.Uri.parse(newUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    wallpaperManager.setStream(inputStream)
                    inputStream.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleDateWidget(index: Int) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (index in newConfigs.indices) {
                newConfigs[index] = newConfigs[index].copy(showDateWidget = !newConfigs[index].showDateWidget)
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun toggleStatusWidget(index: Int) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (index in newConfigs.indices) {
                newConfigs[index] = newConfigs[index].copy(showStatusWidget = !newConfigs[index].showStatusWidget)
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun addNativeWidget(viewIndex: Int, widgetId: Int) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                if (!currentConfig.nativeWidgetIds.contains(widgetId)) {
                    newConfigs[viewIndex] = currentConfig.copy(
                        nativeWidgetIds = currentConfig.nativeWidgetIds + widgetId
                    )
                }
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun removeNativeWidget(viewIndex: Int, widgetId: Int) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                newConfigs[viewIndex] = currentConfig.copy(
                    nativeWidgetIds = currentConfig.nativeWidgetIds - widgetId
                )
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun addShortcut(viewIndex: Int, isDiagonal: Boolean, shortcut: AppShortcut) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                if (isDiagonal) {
                    newConfigs[viewIndex] = currentConfig.copy(
                        diagonalShortcuts = currentConfig.diagonalShortcuts + shortcut
                    )
                } else {
                    newConfigs[viewIndex] = currentConfig.copy(
                        sidebarShortcuts = currentConfig.sidebarShortcuts + shortcut
                    )
                }
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun removeShortcut(viewIndex: Int, isDiagonal: Boolean, shortcutIndex: Int) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                if (isDiagonal) {
                    val list = currentConfig.diagonalShortcuts.toMutableList()
                    if (shortcutIndex in list.indices) {
                        list.removeAt(shortcutIndex)
                        newConfigs[viewIndex] = currentConfig.copy(diagonalShortcuts = list)
                    }
                } else {
                    val list = currentConfig.sidebarShortcuts.toMutableList()
                    if (shortcutIndex in list.indices) {
                        list.removeAt(shortcutIndex)
                        newConfigs[viewIndex] = currentConfig.copy(sidebarShortcuts = list)
                    }
                }
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun addGridPage(viewIndex: Int) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                newConfigs[viewIndex] = currentConfig.copy(
                    gridPages = currentConfig.gridPages + listOf(emptyList())
                )
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun removeGridPage(viewIndex: Int, pageIndex: Int) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                val pages = currentConfig.gridPages.toMutableList()
                if (pageIndex in pages.indices && pages.size > 1) {
                    pages.removeAt(pageIndex)
                    newConfigs[viewIndex] = currentConfig.copy(gridPages = pages)
                }
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun addShortcutToGridPage(viewIndex: Int, pageIndex: Int, shortcut: AppShortcut) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                val pages = currentConfig.gridPages.toMutableList()
                if (pageIndex in pages.indices) {
                    if (pages[pageIndex].size < 9) { // Máximo 3x3 = 9 apps por página
                        pages[pageIndex] = pages[pageIndex] + shortcut
                        newConfigs[viewIndex] = currentConfig.copy(gridPages = pages)
                    }
                }
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun removeShortcutFromGridPage(viewIndex: Int, pageIndex: Int, shortcutIndex: Int) {
        updateState { currentState ->
            val newConfigs = currentState.viewConfigs.toMutableList()
            if (viewIndex in newConfigs.indices) {
                val currentConfig = newConfigs[viewIndex]
                val pages = currentConfig.gridPages.toMutableList()
                if (pageIndex in pages.indices) {
                    val list = pages[pageIndex].toMutableList()
                    if (shortcutIndex in list.indices) {
                        list.removeAt(shortcutIndex)
                        pages[pageIndex] = list
                        newConfigs[viewIndex] = currentConfig.copy(gridPages = pages)
                    }
                }
            }
            currentState.copy(viewConfigs = newConfigs)
        }
    }

    fun getInstalledApps(context: Context): List<AppShortcut> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        return resolveInfoList.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName == context.packageName) null
            else {
                val appName = resolveInfo.loadLabel(packageManager).toString()
                AppShortcut(
                    id = packageName + "/" + resolveInfo.activityInfo.name,
                    name = appName,
                    packageName = packageName
                )
            }
        }.sortedBy { it.name.lowercase() }
    }

    // --- Drawer Management ---

    fun addDrawerCategory(name: String) {
        val newId = name.lowercase().replace(" ", "_") + "_" + System.currentTimeMillis()
        val currentCategories = _state.value.drawerConfig.categories.toMutableList()
        currentCategories.add(DrawerCategory(newId, name))
        updateState { state ->
            state.copy(
                drawerConfig = state.drawerConfig.copy(categories = currentCategories)
            )
        }
    }

    fun removeDrawerCategory(id: String) {
        if (id == "all") return
        val currentCategories = _state.value.drawerConfig.categories.filter { it.id != id }
        updateState { state ->
            state.copy(
                drawerConfig = state.drawerConfig.copy(categories = currentCategories)
            )
        }
    }

    fun moveAppToCategory(packageName: String, categoryId: String) {
        val currentCategories = _state.value.drawerConfig.categories.map { category ->
            val newPackageNames = if (category.id == categoryId) {
                if (!category.packageNames.contains(packageName)) category.packageNames + packageName else category.packageNames
            } else if (category.id != "all") {
                category.packageNames - packageName
            } else {
                category.packageNames
            }
            category.copy(packageNames = newPackageNames)
        }
        updateState { state ->
            state.copy(
                drawerConfig = state.drawerConfig.copy(categories = currentCategories)
            )
        }
    }
}
