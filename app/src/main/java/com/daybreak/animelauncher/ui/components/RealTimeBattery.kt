package com.daybreak.animelauncher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

object BatteryMonitor {
    var batteryLevel by mutableIntStateOf(100)
        private set
    var isCharging by mutableStateOf(false)
        private set
    private var listenerCount = 0
    private var receiver: BroadcastReceiver? = null

    fun register(context: Context) {
        if (listenerCount == 0) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    update(intent)
                }
            }
            val initial = context.applicationContext.registerReceiver(receiver, filter)
            update(initial)
        }
        listenerCount++
    }

    fun unregister(context: Context) {
        listenerCount--
        if (listenerCount <= 0) {
            listenerCount = 0
            receiver?.let {
                try {
                    context.applicationContext.unregisterReceiver(it)
                } catch (e: Exception) {}
            }
            receiver = null
        }
    }

    private fun update(intent: Intent?) {
        if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level != -1 && scale != -1) {
                batteryLevel = (level * 100 / scale.toFloat()).toInt()
            }
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
    }
}

@Composable
fun RealTimeBattery(
    color: Color,
    fontSize: TextUnit,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        BatteryMonitor.register(context)
        onDispose {
            BatteryMonitor.unregister(context)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        val icon = if (BatteryMonitor.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryFull
        Icon(icon, contentDescription = "Battery", tint = color, modifier = Modifier.size(iconSize))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "${BatteryMonitor.batteryLevel}%", fontWeight = FontWeight.Light, fontSize = fontSize, color = color, maxLines = 1, softWrap = false)
    }
}

