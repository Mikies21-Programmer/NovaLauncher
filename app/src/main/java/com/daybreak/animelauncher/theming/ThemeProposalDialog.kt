package com.daybreak.animelauncher.theming

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Diálogo de pantalla completa para la selección y previsualización de temas generados
 * a partir de la imagen de wallpaper seleccionada por el usuario (Adaptive Wallpaper Theming - Fase 2D).
 *
 * Sigue el patrón exacto de [BackgroundSelectionScreen]:
 * - usePlatformDefaultWidth = false
 * - Lenguaje visual Glass oscuro (#030305 con alpha 0.95f)
 * - Motion System: Fade + Scale (160 ms)
 * - Miniatura visual ligera desacoplada por cada propuesta vía [LocalLauncherThemeTokens]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeProposalDialog(
    imageUri: Uri,
    isEs: Boolean,
    onApply: (ThemeProposal) -> Unit,
    onCustomize: (ThemeProposal) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit,
    onPreviewTransient: (ThemeProposal) -> Unit
) {
    val context = LocalContext.current
    var isAnalyzing by remember { mutableStateOf(true) }
    var proposals by remember { mutableStateOf<List<ThemeProposal>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            val bitmap = loadSampledBitmap(context, imageUri, targetSize = 250)
            val palette = WallpaperAnalyzer.analyze(bitmap, Dispatchers.Default)
            val generated = ThemeProposalFlowHandler.resolveProposals(palette)
            withContext(Dispatchers.Main) {
                proposals = generated
                isAnalyzing = false
                if (generated.isNotEmpty()) {
                    selectedIndex = 0
                    onPreviewTransient(generated[0])
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030305).copy(alpha = 0.95f))
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.96f, animationSpec = tween(160, easing = FastOutSlowInEasing))
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (isEs) "Elige un tema" else "Choose a Theme",
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF00F0FF),
                                    fontSize = 20.sp
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = if (isEs) "Volver" else "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                ) { innerPadding ->
                    if (isAnalyzing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00F0FF),
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = if (isEs) "Analizando fondo..." else "Analyzing wallpaper...",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // 1. Preview superior del wallpaper seleccionado
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0E0E14))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Wallpaper Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // 2. Fila dinámica de 1 a 4 propuestas de tema
                            Text(
                                text = if (isEs) "Propuestas de color" else "Color Proposals",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                proposals.forEachIndexed { index, proposal ->
                                    val isSelected = index == selectedIndex
                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.0f else 0.95f,
                                        animationSpec = tween(80, easing = FastOutSlowInEasing),
                                        label = "selectionScale"
                                    )

                                    val activeAccent = Color(proposal.proposedTokens.accentColor)

                                    Column(
                                        modifier = Modifier
                                            .width(114.dp)
                                            .scale(scale)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) activeAccent.copy(alpha = 0.12f)
                                                else Color.White.copy(alpha = 0.04f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) activeAccent else Color.White.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                selectedIndex = index
                                                onPreviewTransient(proposal)
                                            }
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Miniatura con los tokens inyectados de esta propuesta
                                        CompositionLocalProvider(LocalLauncherThemeTokens provides proposal.proposedTokens) {
                                            LauncherThemeMiniature(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                            )
                                        }

                                        // Label y estado seleccionado
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = activeAccent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = proposal.label,
                                                color = if (isSelected) activeAccent else Color.LightGray,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f, fill = false))

                            // 3. Botones de acción inferiores (Aplicar, Personalizar, Descartar)
                            val currentProposal = proposals.getOrNull(selectedIndex)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Botón principal: Aplicar
                                Button(
                                    onClick = {
                                        currentProposal?.let { onApply(it) }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = currentProposal?.let { Color(it.proposedTokens.accentColor) } ?: Color(0xFF00F0FF),
                                        contentColor = if (currentProposal?.isDark == true) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "Aplicar tema y fondo" else "Apply Theme & Wallpaper",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }

                                // Botón secundario: Personalizar
                                OutlinedButton(
                                    onClick = {
                                        currentProposal?.let { onCustomize(it) }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isEs) "Personalizar estilo..." else "Customize Style...",
                                        fontSize = 14.sp
                                    )
                                }

                                // Botón terciario: Descartar
                                TextButton(
                                    onClick = onDiscard,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isEs) "Descartar propuesta (conservar tema actual)" else "Discard Proposal (Keep Current Theme)",
                                        color = Color.LightGray.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Representación visual sintética y ultraligera que consume [LocalLauncherThemeTokens]
 * para mostrar cómo lucirá el launcher con los tokens de una propuesta.
 */
@Composable
fun LauncherThemeMiniature(
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherThemeTokens.current
    val surfaceColor = tokens.surface
    val accentColor = tokens.accent
    val textPrimary = tokens.textPrimary
    val textSecondary = tokens.textSecondary
    val borderColor = tokens.border

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Mini barra de búsqueda
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(borderColor.copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(textSecondary.copy(alpha = 0.5f))
                    )
                }
            }

            // Mini Category Pill
            Box(
                modifier = Modifier
                    .height(13.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(surfaceColor)
                )
            }

            // 3 filas sintéticas de apps
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(borderColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(textPrimary)
                        )
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(textSecondary.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }
    }
}

private fun loadSampledBitmap(context: Context, uri: Uri, targetSize: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        val maxDim = maxOf(options.outWidth, options.outHeight)
        var inSampleSize = 1
        while (maxDim / (inSampleSize * 2) >= targetSize) {
            inSampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (_: Exception) {
        null
    }
}
