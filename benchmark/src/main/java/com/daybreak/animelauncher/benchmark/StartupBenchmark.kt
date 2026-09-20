package com.daybreak.animelauncher.benchmark

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "StartupBenchmark"
private const val TARGET_PACKAGE = "com.daybreak.animelauncher"

@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun startupCold() {
        val runs = mutableListOf<Long>()
        val waitTimes = mutableListOf<Long>()
        repeat(5) { iteration ->
            device.executeShellCommand("am start -S -a android.settings.SETTINGS")
            device.executeShellCommand("am force-stop $TARGET_PACKAGE")
            Thread.sleep(1000)
            val output = device.executeShellCommand("am start -W -n $TARGET_PACKAGE/.MainActivity")
            val totalTime = output.lines()
                .firstOrNull { it.startsWith("TotalTime:") }
                ?.substringAfter(":")?.trim()?.toLongOrNull() ?: 0L
            val waitTime = output.lines()
                .firstOrNull { it.startsWith("WaitTime:") }
                ?.substringAfter(":")?.trim()?.toLongOrNull() ?: 0L
            if (totalTime > 0) {
                runs.add(totalTime)
                waitTimes.add(waitTime)
                Log.i(TAG, "startupCold iteration $iteration: TotalTime=$totalTime ms, WaitTime=$waitTime ms")
            }
            Thread.sleep(500)
        }
        assertTrue("Cold startup runs must not be empty", runs.isNotEmpty())
        Log.i(TAG, "startupCold results: min=${runs.minOrNull()}, median=${runs.sorted()[runs.size / 2]}, max=${runs.maxOrNull()} ms")
    }

    @Test
    fun startupWarm() {
        val runs = mutableListOf<Long>()
        val waitTimes = mutableListOf<Long>()
        repeat(5) { iteration ->
            device.executeShellCommand("am start -S -a android.settings.SETTINGS")
            Thread.sleep(1000)
            val output = device.executeShellCommand("am start -W -n $TARGET_PACKAGE/.MainActivity")
            val totalTime = output.lines()
                .firstOrNull { it.startsWith("TotalTime:") }
                ?.substringAfter(":")?.trim()?.toLongOrNull() ?: 0L
            val waitTime = output.lines()
                .firstOrNull { it.startsWith("WaitTime:") }
                ?.substringAfter(":")?.trim()?.toLongOrNull() ?: 0L
            if (totalTime > 0) {
                runs.add(totalTime)
                waitTimes.add(waitTime)
                Log.i(TAG, "startupWarm iteration $iteration: TotalTime=$totalTime ms, WaitTime=$waitTime ms")
            }
            Thread.sleep(500)
        }
        assertTrue("Warm startup runs must not be empty", runs.isNotEmpty())
        Log.i(TAG, "startupWarm results: min=${runs.minOrNull()}, median=${runs.sorted()[runs.size / 2]}, max=${runs.maxOrNull()} ms")
    }
}
