package com.daybreak.animelauncher.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pruebas unitarias para IconCache (NOVA-APP-DRAWER-ICONS-01).
 *
 * Valida:
 * 1. Comportamiento de caché fría vs caliente.
 * 2. Deduplicación y coalescencia de solicitudes concurrentes del mismo paquete.
 * 3. Desacople de cancelación: la cancelación de la vista no aborta la carga en segundo plano.
 * 4. Distinción explícita de fallos reales (isFailed) frente a estados de carga en progreso.
 * 5. Invalidación selectiva por ciclo de vida de paquetes (instalación, actualización, desinstalación).
 * 6. Precalentamiento acotado al presupuesto de memoria (MAX_ENTRIES = 128).
 * 7. Aislamiento estricto de resultados entre diferentes paquetes.
 */
@RunWith(RobolectricTestRunner::class)
class IconCacheTest {

    private lateinit var context: Context
    private val originalDecoder = IconCache.iconDecoder

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        IconCache.clear()
    }

    @After
    fun tearDown() {
        IconCache.clear()
        IconCache.iconDecoder = originalDecoder
    }

    private fun createTestBitmap(color: Int = 0xFF00FF00.toInt()): ImageBitmap {
        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }
        return bmp.asImageBitmap()
    }

    @Test
    fun coldCache_returnsNullFromGetCached() {
        assertNull(
            "En arranque en frío un paquete no cargado debe retornar null sincrónicamente",
            IconCache.getCached("com.example.unloaded")
        )
        assertFalse(
            "Un paquete no cargado no debe estar marcado como fallido",
            IconCache.isFailed("com.example.unloaded")
        )
    }

    @Test
    fun hotCache_returnsCachedBitmapImmediately() {
        val testBitmap = createTestBitmap()
        IconCache.put("com.example.app", testBitmap)

        val cached = IconCache.getCached("com.example.app")
        assertNotNull("La caché caliente debe devolver el ImageBitmap almacenado", cached)
        assertEquals("Debe coincidir exactamente la referencia del bitmap", testBitmap, cached)
    }

    @Test
    fun loadIcon_populatesCacheAndReturnsBitmap() = runBlocking {
        val testBitmap = createTestBitmap()
        IconCache.iconDecoder = { _, pkg ->
            if (pkg == "com.example.app") testBitmap else null
        }

        val result = IconCache.loadIcon(context, "com.example.app")
        assertNotNull("loadIcon debe retornar el bitmap decodificado", result)
        assertEquals(testBitmap, result)
        assertEquals(
            "loadIcon debe haber persistido el bitmap en la caché en memoria",
            testBitmap,
            IconCache.getCached("com.example.app")
        )
    }

    @Test
    fun concurrentRequests_coalesceToSingleDecoderInvocation() = runBlocking {
        val decodeCount = AtomicInteger(0)
        val testBitmap = createTestBitmap()

        IconCache.iconDecoder = { _, _ ->
            decodeCount.incrementAndGet()
            Thread.sleep(50) // Simular IPC/decodificación
            testBitmap
        }

        // Lanzar 10 solicitudes concurrentes para el mismo paquete
        val jobs = (1..10).map {
            async(Dispatchers.IO) {
                IconCache.loadIcon(context, "com.example.concurrent")
            }
        }
        val results = jobs.awaitAll()

        assertEquals("La decodificación debe invocarse exactamente una vez para solicitudes concurrentes", 1, decodeCount.get())
        for (res in results) {
            assertEquals("Todas las llamadas concurrentes deben recibir el bitmap esperado", testBitmap, res)
        }
    }

    @Test
    fun cancellationOfCallingCoroutine_doesNotCancelUnderlyingCachePopulation() = runBlocking {
        val testBitmap = createTestBitmap()
        val decoderStarted = CompletableDeferred<Unit>()

        IconCache.iconDecoder = { _, _ ->
            decoderStarted.complete(Unit)
            Thread.sleep(80)
            testBitmap
        }

        // Simular que el usuario hace scroll rápido: el composable lanza la corrutina y se cancela
        val callerJob = launch(Dispatchers.IO) {
            IconCache.loadIcon(context, "com.example.scrolled_away")
        }

        decoderStarted.await()
        callerJob.cancelAndJoin()

        // Esperar a que el cacheScope complete la decodificación en segundo plano
        delay(150)

        val cached = IconCache.getCached("com.example.scrolled_away")
        assertNotNull(
            "El bitmap debe haberse guardado en IconCache a pesar de la cancelación del llamante de UI",
            cached
        )
        assertEquals(testBitmap, cached)
    }

    @Test
    fun realFailure_recordsFailedPackageAndDistinguishesFromLoading() = runBlocking {
        IconCache.iconDecoder = { _, _ ->
            throw RuntimeException("Package not found")
        }

        val result = IconCache.loadIcon(context, "com.example.broken")
        assertNull("Un fallo real debe retornar null", result)
        assertTrue(
            "El paquete debe quedar registrado como fallido en failedPackages",
            IconCache.isFailed("com.example.broken")
        )
        assertNull(
            "El paquete fallido no debe estar en caché de imágenes",
            IconCache.getCached("com.example.broken")
        )
    }

    @Test
    fun invalidate_removesFromCacheAndClearsFailedState() {
        val testBitmap = createTestBitmap()
        IconCache.put("com.example.updated", testBitmap)
        assertNotNull(IconCache.getCached("com.example.updated"))

        IconCache.invalidate("com.example.updated")
        assertNull("Tras invalidate, el paquete debe haber sido eliminado del LRU", IconCache.getCached("com.example.updated"))
        assertFalse("Tras invalidate, el paquete no debe figurar como fallido", IconCache.isFailed("com.example.updated"))
    }

    @Test
    fun clear_purgesAllCacheAndState() {
        IconCache.put("com.example.app1", createTestBitmap())
        IconCache.put("com.example.app2", createTestBitmap())

        IconCache.clear()
        assertNull(IconCache.getCached("com.example.app1"))
        assertNull(IconCache.getCached("com.example.app2"))
    }

    @Test
    fun preloadIcons_respectsMaxEntriesLimit() = runBlocking {
        val loadedCount = AtomicInteger(0)
        IconCache.iconDecoder = { _, _ ->
            loadedCount.incrementAndGet()
            createTestBitmap()
        }

        // Enviar 160 paquetes para precalentamiento (mayor al límite de 128)
        val packageNames = (1..160).map { "com.example.app$it" }
        IconCache.preloadIcons(context, packageNames)

        // Esperar a que el precalentamiento complete
        delay(300)

        assertTrue(
            "El precalentador no debe solicitar más de MAX_ENTRIES (${IconCache.MAX_ENTRIES}) paquetes",
            loadedCount.get() <= IconCache.MAX_ENTRIES
        )
    }

    @Test
    fun packageIsolation_jobForPackageADoesNotOverwritePackageB() = runBlocking {
        val bitmapA = createTestBitmap(0xFFFF0000.toInt())
        val bitmapB = createTestBitmap(0xFF0000FF.toInt())

        IconCache.iconDecoder = { _, pkg ->
            if (pkg == "com.pkg.a") bitmapA else bitmapB
        }

        val jobA = async(Dispatchers.IO) { IconCache.loadIcon(context, "com.pkg.a") }
        val jobB = async(Dispatchers.IO) { IconCache.loadIcon(context, "com.pkg.b") }

        assertEquals(bitmapA, jobA.await())
        assertEquals(bitmapB, jobB.await())

        assertEquals(bitmapA, IconCache.getCached("com.pkg.a"))
        assertEquals(bitmapB, IconCache.getCached("com.pkg.b"))
    }
}
