package com.daybreak.animelauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daybreak.animelauncher.ui.components.VideoWallpaperManager
import com.daybreak.animelauncher.ui.screens.LauncherScreen
import com.daybreak.animelauncher.ui.screens.OnboardingScreen
import com.daybreak.animelauncher.ui.screens.SettingsScreen
import com.daybreak.animelauncher.ui.theme.AnimeLauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    companion object {
        const val REQUEST_CONFIGURE_WIDGET = 6124
    }

    private var onWidgetConfigureResultCallback: ((resultCode: Int, data: Intent?) -> Unit)? = null

    fun startAppWidgetConfigure(appWidgetId: Int, callback: (resultCode: Int, data: Intent?) -> Unit): Boolean {
        onWidgetConfigureResultCallback = callback
        return try {
            viewModel.widgetHostManager.appWidgetHost.startAppWidgetConfigureActivityForResult(
                this,
                appWidgetId,
                0,
                REQUEST_CONFIGURE_WIDGET,
                null
            )
            true
        } catch (e: Exception) {
            onWidgetConfigureResultCallback = null
            false
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in ComponentActivity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            onWidgetConfigureResultCallback?.invoke(resultCode, data)
            onWidgetConfigureResultCallback = null
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> {
                    // El usuario acaba de desbloquear el dispositivo
                    viewModel.triggerUnlockAnimation()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    // Pantalla apagada: pausar reproductores de inmediato
                    VideoWallpaperManager.onPause()
                }
            }
        }
    }

    private var navControllerRef: androidx.navigation.NavController? = null

    fun isGestureNavigation(): Boolean {
        val rootInsets = androidx.core.view.ViewCompat.getRootWindowInsets(window.decorView)
        if (rootInsets != null) {
            val tappableBottom = rootInsets.getInsets(WindowInsetsCompat.Type.tappableElement()).bottom
            val navBottom = rootInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            if (tappableBottom > 0) return false // 3 botones físicos/virtuales detectados
            if (navBottom > 0 && tappableBottom == 0) return true // Barra de gestos
        }
        try {
            val navMode = android.provider.Settings.Secure.getInt(contentResolver, "navigation_mode", -1)
            if (navMode == 2) return true
            if (navMode == 0 || navMode == 1) return false
        } catch (_: Exception) {}
        try {
            val fsg = android.provider.Settings.Global.getInt(contentResolver, "force_fsg_nav_bar", -1)
            if (fsg == 1) return true
            if (fsg == 0) return false
        } catch (_: Exception) {}
        return false
    }

    fun applySystemBarsPolicy(immersiveMode: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
        val isGestures = isGestureNavigation()
        if (isGestures && immersiveMode) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val isHome = (intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) ||
                intent?.hasCategory(Intent.CATEGORY_HOME) == true
        if (isHome) {
            navControllerRef?.let { nav ->
                if (nav.currentDestination?.route != "launcher") {
                    nav.navigate("launcher") {
                        popUpTo("launcher") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            viewModel.onHomeIntentReceived()
        }
    }

    @android.annotation.SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        applySystemBarsPolicy(viewModel.state.value.gesturesConfig.immersiveMode)

        // Registrar receptor de desbloqueo de pantalla
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        setContent {
            AnimeLauncherTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.ui.graphics.Color.Black
                ) { _ ->
                    val navController = rememberNavController()
                    LaunchedEffect(navController) {
                        navControllerRef = navController
                    }
                    
                    val state by viewModel.state.collectAsState()
                    val startDest = if (state.hasCompletedOnboarding) "launcher" else "onboarding"
                    
                    LaunchedEffect(state.gesturesConfig.immersiveMode) {
                        applySystemBarsPolicy(state.gesturesConfig.immersiveMode)
                    }
                    
                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onFinish = {
                                    navController.navigate("launcher") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("launcher") {
                            LauncherScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = {
                                    navController.navigate("settings") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onActivityStarted()
    }

    override fun onResume() {
        super.onResume()
        applySystemBarsPolicy(viewModel.state.value.gesturesConfig.immersiveMode)
        VideoWallpaperManager.onResume(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applySystemBarsPolicy(viewModel.state.value.gesturesConfig.immersiveMode)
        }
        VideoWallpaperManager.onWindowFocusChanged(hasFocus, this)
    }

    override fun onPause() {
        VideoWallpaperManager.onPause()
        super.onPause()
    }

    override fun onStop() {
        VideoWallpaperManager.onPause()
        viewModel.onActivityStopped()
        super.onStop()
    }

    override fun onDestroy() {
        navControllerRef = null
        onWidgetConfigureResultCallback = null
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        VideoWallpaperManager.releaseAll()
        super.onDestroy()
    }
}