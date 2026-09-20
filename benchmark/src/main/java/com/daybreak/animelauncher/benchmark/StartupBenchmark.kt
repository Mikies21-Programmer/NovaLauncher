package com.daybreak.animelauncher.benchmark

import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "StartupBenchmark"
private const val TARGET_PACKAGE = "com.daybreak.animelauncher"

@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun startupColdMacrobenchmark() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        iterations = 3,
        setupBlock = {
            device.executeShellCommand("am start -a android.settings.SETTINGS")
            device.executeShellCommand("am force-stop $TARGET_PACKAGE")
            Thread.sleep(500)
        }
    ) {
        startActivityAndWait(android.content.Intent().apply {
            setClassName(TARGET_PACKAGE, "$TARGET_PACKAGE.MainActivity")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
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
