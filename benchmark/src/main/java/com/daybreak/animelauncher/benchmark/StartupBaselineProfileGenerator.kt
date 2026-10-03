package com.daybreak.animelauncher.benchmark

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PERF-OPT-3: Generador de Baseline Profile para el camino crítico del launcher.
 *
 * Ejecutar con:
 *   ./gradlew :benchmark:connectedBenchmarkAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=com.daybreak.animelauncher.benchmark.StartupBaselineProfileGenerator
 *
 * El perfil generado (baseline-prof.txt en additionalTestOutput) se copia a
 * app/src/main/baseline-prof.txt para que AGP lo empaquete en el APK y ART lo
 * use en tiempo de instalación (dex precompilado del startup path).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupBaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.daybreak.animelauncher",
        stableIterations = 2,
        maxIterations = 5
    ) {
        // 1. Arranque en frío hasta el launcher (Application + MainActivity + LauncherScreen + video)
        startActivityAndWait(Intent().apply {
            setClassName("com.daybreak.animelauncher", "com.daybreak.animelauncher.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        device.waitForIdle()

        // 2. App Drawer (carga de lista + preparación de categorías)
        device.swipe(610, 2400, 610, 700, 20)
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()

        // 3. Segunda pasada de swipe para estabilizar el perfil del drawer
        device.swipe(610, 2400, 610, 700, 20)
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }
}
