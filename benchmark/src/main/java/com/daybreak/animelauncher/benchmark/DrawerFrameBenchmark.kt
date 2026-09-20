package com.daybreak.animelauncher.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.daybreak.animelauncher"

@LargeTest
@RunWith(AndroidJUnit4::class)
class DrawerFrameBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private fun ensureDrawerOpen(device: UiDevice) {
        if (device.findObject(By.clazz("android.widget.EditText")) == null) {
            val width = device.displayWidth
            val height = device.displayHeight
            device.swipe(width / 2, (height * 0.70).toInt(), width / 2, (height * 0.20).toInt(), 10)
            device.waitForIdle()
            device.wait(Until.hasObject(By.clazz("android.widget.EditText")), 3000)
        }
    }

    @Test
    fun drawerScroll() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.None(),
        iterations = 5,
        setupBlock = {
            pressHome()
            device.waitForIdle()
            ensureDrawerOpen(device)
        }
    ) {
        val width = device.displayWidth
        val height = device.displayHeight
        // Fast scroll down across LazyColumn
        device.swipe(width / 2, (height * 0.75).toInt(), width / 2, (height * 0.25).toInt(), 8)
        // Fast scroll up
        device.swipe(width / 2, (height * 0.25).toInt(), width / 2, (height * 0.75).toInt(), 8)
        device.waitForIdle()
    }

    @Test
    fun drawerSearch() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.None(),
        iterations = 5,
        setupBlock = {
            pressHome()
            device.waitForIdle()
            ensureDrawerOpen(device)
            val searchField = device.findObject(By.clazz("android.widget.EditText"))
            searchField?.clear()
            device.waitForIdle()
        }
    ) {
        val searchField = device.findObject(By.clazz("android.widget.EditText"))
        searchField?.click()
        val query = "calculator"
        for (char in query) {
            device.executeShellCommand("input text $char")
            Thread.sleep(80)
        }
        device.waitForIdle()
    }
}
