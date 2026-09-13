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

    @android.annotation.SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        // Iconos claros en la barra de estado para máxima visibilidad sobre wallpapers oscuros/neón
        windowInsetsController.isAppearanceLightStatusBars = false

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
                    
                    val state by viewModel.state.collectAsState()
                    val startDest = if (state.hasCompletedOnboarding) "launcher" else "onboarding"
                    
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    LaunchedEffect(state.gesturesConfig.immersiveMode) {
                        if (state.gesturesConfig.immersiveMode) {
                            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                        } else {
                            insetsController.show(WindowInsetsCompat.Type.navigationBars())
                        }
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
                                onNavigateToSettings = { navController.navigate("settings") }
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

    override fun onResume() {
        super.onResume()
        VideoWallpaperManager.onResume(this)
        viewModel.onActivityResumed()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        VideoWallpaperManager.onWindowFocusChanged(hasFocus, this)
    }

    override fun onPause() {
        VideoWallpaperManager.onPause()
        viewModel.onActivityPaused()
        super.onPause()
    }

    override fun onStop() {
        VideoWallpaperManager.onPause()
        viewModel.onActivityPaused()
        super.onStop()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        VideoWallpaperManager.releaseAll()
        super.onDestroy()
    }
}