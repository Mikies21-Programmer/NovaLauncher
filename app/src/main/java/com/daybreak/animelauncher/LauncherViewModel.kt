package com.daybreak.animelauncher

import android.app.Application
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import com.daybreak.animelauncher.widget.WidgetHostManager
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow

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
    val customIconColor: String = "#FFFFFF",

    // Tokens Glass / Dark Premium
    val accentColor: String = "#00F0FF",
    val panelTransparency: Float = 0.88f,
    val glassBorderAlpha: Float = 0.25f,
    val cornerRadius: Float = 16f,
    val widgetOpacity: Float = 0.90f
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
    @Deprecated("Legacy field preserved for SharedPreferences/Gson backwards compatibility")
    val edgeSwipeToBack: Boolean = true,
    @Deprecated("Legacy field preserved for SharedPreferences/Gson backwards compatibility")
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
    val widgetHostManager = WidgetHostManager(application)
    val appWidgetHost: AppWidgetHost get() = widgetHostManager.appWidgetHost

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()
    
    // Estado para ejecutar la animación de desbloqueo SOLAMENTE al desbloquear/iniciar sesión
    private val _isUnlockPending = MutableStateFlow(false)
    val isUnlockPending: StateFlow<Boolean> = _isUnlockPending.asStateFlow()

    fun triggerUnlockAnimation() {
        _isUnlockPending.value = true
    }

    fun consumeUnlockAnimation() {
        _isUnlockPending.value = false
    }

    // Trigger para eventos de navegación HOME del sistema (onNewIntent)
    private val _homeActionTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homeActionTrigger: SharedFlow<Unit> = _homeActionTrigger.asSharedFlow()

    fun onHomeIntentReceived() {
        _homeActionTrigger.tryEmit(Unit)
    }

    // Trigger para acciones de widgets iniciadas desde Settings (UX-06)
    sealed interface WidgetAction {
        data class OpenPicker(val pageIndex: Int = 0) : WidgetAction
        data object EnterEditMode : WidgetAction
    }

    private val _widgetActionChannel = kotlinx.coroutines.channels.Channel<WidgetAction>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val widgetActionFlow: kotlinx.coroutines.flow.Flow<WidgetAction> = _widgetActionChannel.receiveAsFlow()

    fun requestOpenWidgetPicker(pageIndex: Int = 0) {
        _widgetActionChannel.trySend(WidgetAction.OpenPicker(pageIndex))
    }

    fun requestEnterWidgetEditMode() {
        _widgetActionChannel.trySend(WidgetAction.EnterEditMode)
    }

    // Notificaciones no leídas en tiempo real (NotificationListenerService)
    val notificationCount: StateFlow<Int> = NotificationMonitorService.notificationCount

    private val packageReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshInstalledApps()
        }
    }

    init {
        // Precarga de aplicaciones instaladas en hilo secundario y escucha de cambios de paquetes
        refreshInstalledApps()
        try {
            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            application.registerReceiver(packageReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = getInstalledApps(getApplication())
            val stateIcons = _state.value.customAppIcons
            val mappedApps = apps.map { app ->
                if (stateIcons.containsKey(app.packageName)) {
                    app.copy(customIconUri = stateIcons[app.packageName])
                } else app
            }
            _installedApps.value = mappedApps
            autoCategorizeApps(getApplication(), mappedApps)
        }
    }

    fun onActivityStarted() {
        widgetHostManager.startListening()
    }

    fun onActivityStopped() {
        widgetHostManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        widgetHostManager.clearViews()
        try {
            getApplication<Application>().unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun loadState(): LauncherState {
        android.os.Trace.beginSection("LOAD_STATE_TOTAL")
        try {
            // ── BLOQUE 1: Lectura de SharedPreferences (STATE_READ) ──────────────
            android.os.Trace.beginSection("STATE_READ")
            val json = prefs.getString("launcher_state", null)
            android.os.Trace.endSection() // STATE_READ

            var state = LauncherState()

            if (json != null) {
                // ── BLOQUE 2: Deserialización Gson del estado principal (GSON_STATE) ──
                android.os.Trace.beginSection("GSON_STATE")
                try {
                    val loaded = gson.fromJson(json, LauncherState::class.java)
                    if (loaded != null) {
                        state = loaded.copy(
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
                } finally {
                    android.os.Trace.endSection() // GSON_STATE
                }
            }

            // ── BLOQUE 3: Lectura de SharedPreferences para el estilo (STYLE_READ) ──
            android.os.Trace.beginSection("STYLE_READ")
            // Carga desacoplada de estilo si existe configuración dedicada
            val styleJson = prefs.getString("launcher_style_config", null)
            android.os.Trace.endSection() // STYLE_READ

            if (styleJson != null) {
                // ── BLOQUE 4: Deserialización Gson del estilo (GSON_STYLE) ──────────
                android.os.Trace.beginSection("GSON_STYLE")
                try {
                    val loadedStyle = gson.fromJson(styleJson, AdvancedStyleConfig::class.java)
                    if (loadedStyle != null) {
                        state = state.copy(styleConfig = loadedStyle)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    android.os.Trace.endSection() // GSON_STYLE
                }
            }

            // ── BLOQUE 5: Validación IPC de widgets (WIDGET_VALIDATION) ──────────
            android.os.Trace.beginSection("WIDGET_VALIDATION")
            try {
                // Validación de widgets contra providers instalados para purgar huérfanos
                val cleanedViewConfigs = state.viewConfigs.map { config ->
                    config.copy(nativeWidgetIds = widgetHostManager.validateAndCleanWidgets(config.nativeWidgetIds))
                }
                state = state.copy(viewConfigs = cleanedViewConfigs)
            } finally {
                android.os.Trace.endSection() // WIDGET_VALIDATION
            }

            return state
        } finally {
            android.os.Trace.endSection() // LOAD_STATE_TOTAL
        }
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

    /**
     * Actualización visual inmediata en memoria sin I/O en disco durante el arrastre de sliders.
     */
    fun updateStyleConfigTransient(newConfig: AdvancedStyleConfig) {
        _state.update { it.copy(styleConfig = newConfig) }
    }

    /**
     * Persistencia desacoplada: solo serializa y guarda el estilo al terminar el gesto o cambiar valores discretos.
     */
    fun persistStyleConfig(newConfig: AdvancedStyleConfig) {
        _state.update { it.copy(styleConfig = newConfig) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = gson.toJson(newConfig)
                prefs.edit().putString("launcher_style_config", json).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateStyleConfig(newConfig: AdvancedStyleConfig) {
        persistStyleConfig(newConfig)
    }

    fun resetStyleConfig() {
        val defaultStyle = AdvancedStyleConfig()
        persistStyleConfig(defaultStyle)
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
        widgetHostManager.deleteWidgetId(widgetId)
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
