package com.daybreak.animelauncher.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RealTimeClock(
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var time by remember { mutableStateOf(formatter.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            // Dormir exactamente hasta el próximo cambio de minuto (+50ms margen)
            val now = System.currentTimeMillis()
            val millisToNextMinute = 60000L - (now % 60000L) + 50L
            delay(millisToNextMinute)
            time = formatter.format(Date())
        }
    }

    Text(
        text = time,
        fontWeight = FontWeight.Light,
        fontSize = fontSize,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
    )
}

