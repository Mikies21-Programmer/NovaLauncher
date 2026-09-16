package com.daybreak.animelauncher.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.daybreak.animelauncher.LauncherViewModel
import com.daybreak.animelauncher.ThemePresets
import com.daybreak.animelauncher.ui.components.ShortcutIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: LauncherViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isEs = state.language == "es"
    val pagerState = rememberPagerState(initialPage = state.onboardingStep, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    
    // Load apps just in case
    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030305))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomeStep(isEs) { 
                    viewModel.updateOnboardingStep(1)
                    coroutineScope.launch { pagerState.animateScrollToPage(1) } 
                }
                1 -> ThemeSelectionStep(isEs, viewModel) { 
                    viewModel.updateOnboardingStep(2)
                    coroutineScope.launch { pagerState.animateScrollToPage(2) } 
                }
                2 -> DefaultLauncherStep(isEs, context, onNext = { 
                    viewModel.updateOnboardingStep(3)
                    coroutineScope.launch { pagerState.animateScrollToPage(3) } 
                })
                3 -> ConfigureAppsStep(isEs, viewModel) { 
                    viewModel.updateOnboardingStep(4)
                    coroutineScope.launch { pagerState.animateScrollToPage(4) } 
                }
                4 -> TutorialStep(isEs) {
                    viewModel.updateOnboardingStep(0)
                    viewModel.updateHasCompletedOnboarding(true)
                    onFinish()
                }
            }
        }
        
        // Pager indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == i) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == i) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(isEs: Boolean, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isEs) "¡Bienvenido!" else "Welcome!",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isEs) {
                "Anime Launcher es tu nueva pantalla de inicio. Transformará por completo cómo usas tu teléfono con un diseño moderno, asimétrico y rápido.\n\nPrepárate para personalizar tu experiencia."
            } else {
                "Anime Launcher is your new home screen. It will completely transform how you use your phone with a modern, asymmetric, and fast design.\n\nGet ready to customize your experience."
            },
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isEs) "Comenzar Configuración" else "Start Setup", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ThemeSelectionStep(isEs: Boolean, viewModel: LauncherViewModel, onNext: () -> Unit) {
    val context = LocalContext.current
    var selectedThemeName by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isEs) "Elige tu Estilo Inicial" else "Choose your Initial Style",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isEs) "Selecciona uno de nuestros temas premium. Podrás cambiarlo más adelante." else "Select one of our premium themes. You can change it later.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            items(ThemePresets.defaultThemes) { theme ->
                val isSelected = selectedThemeName == theme.name
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { 
                            selectedThemeName = theme.name
                            viewModel.updateStyleConfig(theme.styleConfig)
                            viewModel.updateBackgroundUri(0, theme.backgroundUri)
                            viewModel.updateBackgroundUri(1, theme.backgroundUri)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp, 220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(theme.backgroundUri).crossfade(true).build(),
                            contentDescription = theme.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                    Text(theme.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isEs) "Continuar" else "Continue", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DefaultLauncherStep(isEs: Boolean, context: Context, onNext: () -> Unit) {
    var isAlreadyDefault by remember { mutableStateOf(false) }
    
    val checkDefault = {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        isAlreadyDefault = (resolveInfo?.activityInfo?.packageName == context.packageName)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkDefault()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        checkDefault()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isEs) "Establecer como Principal" else "Set as Default",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isEs) {
                "Para que Anime Launcher sea tu pantalla de inicio oficial cada vez que presiones el botón de inicio, debes establecerlo como predeterminado en el sistema."
            } else {
                "For Anime Launcher to be your official home screen every time you press the home button, you must set it as default in the system."
            },
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isAlreadyDefault) {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEs) "¡Ya eres el principal! (Continuar)" else "Already Default! (Continue)", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                                launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                                return@Button
                            }
                        }
                        // Fallback para versiones anteriores o si RoleManager falla
                        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                            context.startActivity(intent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, if(isEs) "Busca 'Aplicaciones predeterminadas' en tus ajustes" else "Search for 'Default apps' in settings", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEs) "Otorgar Permiso" else "Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEs) "Siguiente" else "Next", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConfigureAppsStep(isEs: Boolean, viewModel: LauncherViewModel, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isEs) "Atajos y Personalización" else "Shortcuts & Customization",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isEs) {
                "Tu pantalla principal cuenta con barras diagonales y laterales con accesos rápidos a tus aplicaciones favoritas.\n\nPodrás cambiarlas y personalizarlas en cualquier momento desde Configuración o dejando presionado sobre la pantalla."
            } else {
                "Your main screen features diagonal and side bars with quick shortcuts to your favorite apps.\n\nYou can change and customize them anytime from Settings or by long-pressing on the screen."
            },
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isEs) "Entendido" else "Got it", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TutorialStep(isEs: Boolean, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isEs) "Cómo usar NovaLauncher" else "How to use NovaLauncher",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TutorialItem(
                title = if (isEs) "Cajón de Aplicaciones" else "App Drawer",
                desc = if (isEs) "Desliza hacia arriba desde cualquier punto para ver todas tus aplicaciones instaladas." else "Swipe up from anywhere to see all your installed applications."
            )
            TutorialItem(
                title = if (isEs) "Cambio entre Páginas" else "Switch Between Pages",
                desc = if (isEs) "Desliza horizontalmente para alternar entre pantallas con diseños y accesos únicos." else "Swipe horizontally to switch between screens with unique layouts."
            )
            TutorialItem(
                title = if (isEs) "Menú de Personalización" else "Customization Menu",
                desc = if (isEs) "Mantén presionado cualquier espacio vacío en la pantalla para cambiar el fondo, colores o abrir Ajustes." else "Long press any empty space on the screen to change wallpaper, colors, or open Settings."
            )
            TutorialItem(
                title = if (isEs) "Widgets del Sistema" else "System Widgets",
                desc = if (isEs) "Mantén presionado en la pantalla y selecciona 'Widgets del sistema' para añadir y configurar widgets nativos." else "Long press on the screen and select 'System widgets' to add and configure native widgets."
            )
            TutorialItem(
                title = if (isEs) "Doble Toque para Apagar" else "Double Tap to Sleep",
                desc = if (isEs) "Toca dos veces sobre un espacio libre del escritorio para bloquear la pantalla (requiere activar el servicio de accesibilidad de NovaLauncher)." else "Double tap on empty desktop space to lock screen (requires enabling NovaLauncher accessibility service)."
            )
            TutorialItem(
                title = if (isEs) "Deslizar para Notificaciones" else "Swipe for Notifications",
                desc = if (isEs) "Desliza hacia abajo en el escritorio para desplegar el panel de notificaciones del sistema." else "Swipe down on desktop to expand system notifications panel."
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (isEs) "¡Comenzar!" else "Let's Go!", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun TutorialItem(title: String, desc: String) {
    Column {
        Text(title, color = Color(0xFF00F0FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
