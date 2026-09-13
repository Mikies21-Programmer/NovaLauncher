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
    var time by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
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
