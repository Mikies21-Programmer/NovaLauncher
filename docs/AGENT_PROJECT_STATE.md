# Estado Persistente del Proyecto — NovaLauncher (Agent State)

> **Documento maestro de sincronización, continuidad operativa y auditoría entre sesiones, Antigravity y Claude.**<br>
> *Última actualización: 20 de septiembre de 2026 (Cierre P2-03 EXP-PAGER-ZERO-PRELOAD — Performance Engineering).*<br>
> *Regla de seguridad estricta: CERO secretos, contraseñas, keystores ni claves privadas en este repositorio.*

---

## 1. Identidad y Parámetros Base
* **Proyecto:** NovaLauncher
* **Repositorio Principal:** `Mikies21-Programmer/NovaLauncher`
* **Directorio Local:** `C:\Users\migue\AndroidStudioProjects\AnimeLauncher`
* **Application ID:** `com.daybreak.animelauncher`
* **Versión Actual:** `versionCode = 6`, `versionName = "1.4"`
* **SDKs:** `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`
* **Política de Privacidad Pública:** `https://mikies21-programmer.github.io/NovaLauncher-Privacy/`
* **Contacto Oficial:** `dbreak472@gmail.com`

---

## 2. Estado Oficial de Módulos y Fases

### Tabla de Estado Oficial de Performance Engineering

| Fase | Alcance Técnico | Estado Oficial |
| :--- | :--- | :---: |
| **P0** | Auditoría estática y localización de hotspots | ✅ **COMPLETADA** |
| **P1** | Instrumentación (`:benchmark`) y Baseline Cuantitativa empírica | ✅ **COMPLETADA / VALIDADA** |
| **P2-01** | Optimización Quirúrgica del Search del App Drawer | ✅ **COMPLETADA / VALIDADA FÍSICAMENTE** |
| **P2-02A** | loadState Trace (I/O, Gson, Widgets en hilo principal) | ✅ **COMPLETADA** |
| **P2-02B** | Cold Start Map (Timeline completo y delimitación de frames) | ✅ **COMPLETADA** |
| **P2-02C** | Composition Trace (Perfetto track_event y hotspots de Compose) | ✅ **COMPLETADA** |
| **P2-03** | EXP-PAGER-ZERO-PRELOAD (`beyondViewportPageCount = 0`) | ✅ **COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT PENDIENTE** |
| **P2-04** | Siguiente Hotspot de Composición / Layout | ⏳ **AUDITORÍA PENDIENTE** |

---

### Tabla de Estado Oficial de Adaptive Wallpaper Theming

| Fase | Alcance Técnico | Estado Oficial |
| :--- | :--- | :---: |
| **Fase 1** | Wallpaper Analyzer (`Bitmap` → `WallpaperAnalyzer` → `ThemePalette`) | ✅ **COMPLETADA / VALIDADA** |
| **Fase 2B** | Runtime Theme Token Layer (`LauncherThemeTokens` + `LocalLauncherThemeTokens`) | ✅ **COMPLETADA / VALIDADA / CHECKPOINT** |
| **Fase 2C** | Theme Proposal Generator (`ThemePalette` → `ThemeProposalGenerator` → `List<ThemeProposal>`) | ✅ **COMPLETADA / VALIDADA / CHECKPOINT** |
| **Fase 2D** | Theme Proposal Flow + Preview + Apply para IMÁGENES | ✅ **COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT** |
| **Fase 2E** | Adaptive Theme para VIDEO (`VideoFrameExtractor` + `ThemePaletteAggregator` + Preview) | ✅ **COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT** |

---

### Detalle de Fases de Adaptive Wallpaper Theming

#### Fase 1 — Fundamentos y Wallpaper Analyzer (✅ COMPLETADA / VALIDADA)
* **Objetivo:** Subsistema puro de análisis cromático sobre bitmaps estáticos desacoplado de Compose, ViewModels y persistencia.
* **Componentes:**
  * `ThemePalette`: Modelo inmutable con `@get:ColorInt dominantColor`, `vibrantColor`, `mutedColor`, `isDark: Boolean`, y `averageLuminance: Float`. Incluye `DEFAULT` seguro y neutro para recuperaciones sin excepciones.
  * `WallpaperAnalyzer`: Objeto singleton con `analyze(bitmap, dispatcher = Dispatchers.Default)` y `analyzeSync(bitmap)`.
* **Memoria y Performance:** Redimensionamiento previo a ~200x200 px (`TARGET_MAX_DIMENSION = 200`), memoria contenida en ~160 KB, reciclaje garantizado del bitmap temporal en `finally`. Despachado fuera del hilo principal (`Dispatchers.Default`).
* **Calidad Cromática:** `.clearFilters()` en `Palette.Builder` para clasificar fondos puros oscuros, claros o monocromáticos; luminancia relativa ponderada por población de swatches; clasificación `isDark = averageLuminance < 0.5f`.
* **Tests:** 11 pruebas unitarias en `WallpaperAnalyzerTest` (100% PASS).

#### Fase 2B — Runtime Theme Token Layer (✅ COMPLETADA / VALIDADA / CHECKPOINT)
* **Modelo Inmutable Puro:** Creado `LauncherThemeTokens` (`accentColor`, `surfaceColor`, `textPrimaryColor`, `textSecondaryColor`, `borderColor`) con enteros `@ColorInt Int` y parser hexadecimal propio libre de Compose y dependencias Android.
* **Defaults Compatibles:** Paridad visual 100% con la estética cyberpunk original (`#00F0FF` para acento/borde, `#08080C` para superficies oscuras, `#FFFFFF` para texto principal, `#CCCCCC` para secundario).
* **CompositionLocal Global:** Creado `LocalLauncherThemeTokens` inyectado en el root real de composición (`MainActivity.kt` envolviendo a `NavHost`).
* **Consumo Conectado:** Consumo reactivo en `AppDrawerScreen.kt`, `LauncherScreen.kt`, `SettingsScreen.kt` (GlassCard reactivo) y `AdvancedSettingsScreen.kt`.
* **Tests:** 4 pruebas unitarias en `LauncherThemeTokensTest` (100% PASS).

#### Fase 2C — Theme Proposal Generator (✅ COMPLETADA / VALIDADA / CHECKPOINT)
* **Objetivo:** Generación determinista y pura de propuestas semánticas: `ThemePalette` → `ThemeProposalGenerator` → `List<ThemeProposal>`.
* **Modelo:** `ThemeProposal` (`id`, `palette`, `proposedTokens`, `isDark`, `label`, `order`) como estructura transitoria en memoria libre de persistencia.
* **Garantía Estricta de Contraste:** Validación y ajuste sistemático de luminancia por HSL para ratios mínimos WCAG 2.1: $\ge 4.5:1$ en `textPrimaryColor`, $\ge 3.0:1$ en `textSecondaryColor` y `accentColor`, $\ge 1.5:1$ en `borderColor`.
* **Deduplicación Perceptual y Sanity Check:**
  * Evaluado con 5 paletas representativas (oscura azul/violeta, oscura rojiza/magenta, clara azul, clara cálida y monocromática).
  * Resultados: propuestas perceptualmente diferenciadas, deduplicación funcional (distancia Manhattan RGB), contraste WCAG AAA/AA y reducción a 1 sola propuesta en monocromáticas sin inventar variantes artificiales.
* **Tests:** 12 pruebas unitarias en `ThemeProposalGeneratorTest` (100% PASS).

#### Fase 2D — Propuestas de Tema + Preview + Aplicar para Imágenes (✅ COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT)
* **Flujo Validado Físicamente:** Galería → Análisis asíncrono → `ThemeProposalDialog` (1 a 4 propuestas) → **Aplicar**, **Personalizar**, **Descartar**, **Back**.
* **Independencia de Wallpaper:** El wallpaper nuevo permanece vigente en las 4 acciones; el tema se persiste en *Aplicar*, se usa como base en *Personalizar*, o se restaura el tema anterior en *Descartar* y *Back*.
* **Correcciones Validadas:**
  1. *Personalizar:* Enrutamiento directo hacia `AdvancedSettingsScreen` pasando `initialShowAdvanced = true` a través de `openAdvancedInSettings` en `MainActivity` y `SettingsScreen`.
  2. *Contraste Category Pills:* Sustitución de colores hardcodeados por tokens semánticos `themeTokens.accent`, `themeTokens.textSecondary` y borde `themeTokens.border.copy(alpha = 0.15f)`, legibles en tema claro y oscuro.
* **Tests:** 12 pruebas unitarias en `ThemeProposalFlowHandlerTest` (100% PASS).

#### Fase 2E — Adaptive Theme para VIDEO (✅ COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT)
* **Flujo Validado Físicamente:**
  ```text
  Video seleccionado desde Galería (.mp4, URI)
                  ↓
  Detección MIME (video/*)
                  ↓
  ThemeProposalDialog desplegado
                  ↓
  VideoFrameExtractor (Dispatchers.IO): 3 frames representativos (10%, 50%, 90%)
    └── Acotado a ~400 px (evita 4K en RAM); MediaMetadataRetriever liberado en finally
                  ↓
  WallpaperAnalyzer (Dispatchers.Default): análisis individual por frame
    └── Frame central (50%) conservado para el preview superior
    └── Frames auxiliares reciclados inmediatamente
                  ↓
  ThemePaletteAggregator: Medoide central geométrico en RGB (sin mezcla artificial)
    └── averageLuminance media aritmética (isDark coherente)
                  ↓
  ThemeProposalFlowHandler → 1 a 4 propuestas semánticas
                  ↓
  Aplicar / Personalizar / Descartar / Back (Wallpaper de video aplicado exitosamente)
  ```
* **Preview Nativo Sin Dependencias:** Coil renderiza directamente el `Bitmap` representativo mediante `AsyncImage(model = ImageRequest.Builder(context).data(previewBitmap ?: imageUri)...)`, eliminando dependencias adicionales como `coil-video` o reproductores embebidos en el diálogo.
* **Ciclo de Vida Limpio:** `previewBitmap` se recicla en `DisposableEffect.onDispose`; cero corrutinas ni bitmaps retenidos tras cerrar el diálogo.
* **Tests:** 7 pruebas unitarias en `ThemePaletteAggregatorTest` (100% PASS). Total global de tests unitarios: **52/52 PASS**.

---

### Diagnóstico de Comportamiento Preexistente de Video

* **Observación Fásica:** Al tener un video como fondo de pantalla, abrir una aplicación multimedia pesada (ej. TikTok, YouTube, Cámara) y regresar a NovaLauncher, de forma intermitente el video puede quedar congelado en el último frame.
* **Investigación y Desacoplamiento:**
  * Se confirmó que **Fase 2E no introdujo el problema**: sus componentes son de ejecución puntual y están completamente destruidos al momento del fallo; el problema se reproduce igualmente con los videos preexistentes (`R.raw.bg_view_one`) anteriores a Fase 1.
  * Causa identificada: [VideoWallpaperManager.kt](file:///c:/Users/migue/AndroidStudioProjects/AnimeLauncher/app/src/main/java/com/daybreak/animelauncher/ui/components/VideoWallpaperManager.kt) (código protegido intacto). Cuando una app pesada reclama decodificadores hardware, SurfaceFlinger invalida la `SurfaceTexture` del `TextureView`. En `onResume()`, `syncSlots()` no detecta cambio de URI ni de página, por lo que no re-vincula `PlayerView.player` ni lanza el watchdog de recuperación, dejando a ExoPlayer decodificando hacia una superficie desconectada.
* **Estado Oficial:**
  ```text
  Known pre-existing video resume issue — independent performance/stability task
  ```
  *(NO marcado como resuelto; se mantendrá como tarea independiente de estabilización para no mezclar Theming con el reproductor).*

---

### Hitos Históricos Anteriores Consolidados

* **Fase 5A a 5D (PASS):** Prominent Disclosure in-app, Tarjeta de Privacidad, repositorio `NovaLauncher-Privacy` en GitHub Pages y auditoría integral de Play Console.
* **Fase 5E-0 a 5E-2 (PASS):** Upload Key v2 generada (`upload-keystore-v2.jks`) y certificado PEM (`upload_certificate-v2.pem`) fuera del repo.
* **PERFORMANCE-03 (VERIFIED):** Capacidad de `IconCache` fijada en 128 (`MAX_ENTRIES = 128`), 100% hit rate en hardware real (412 hits / 0 misses).
* **DRAWER & MOTION SYSTEM CHECKPOINT (PASS / VALIDATED):** Stream híbrido en `LazyColumn` a 120 FPS, `AlphabetIndexRail` conflated, swipe horizontal entre categorías y Nova Motion System (6/6 completo con corrección `ON_STOP` en Resume Fade).

---

## 3. Arquitectura Actual del Adaptive Theming

1. **`WallpaperAnalyzer`:** Singleton asíncrono para análisis cromático y downscaling de bitmaps a ~200 px sin afectar el hilo principal.
2. **`ThemePalette`:** Estructura inmutable portadora de colores (`dominantColor`, `vibrantColor`, `mutedColor`, `isDark`, `averageLuminance`).
3. **`ThemeProposal`:** Entidad transitoria que encapsula propuestas semánticas (`id`, `palette`, `proposedTokens`, `isDark`, `label`, `order`).
4. **`ThemeProposalGenerator`:** Motor matemático puro en Kotlin estándar con contrastes WCAG 2.1 y deduplicación perceptual.
5. **`ThemeProposalFlowHandler`:** Controlador de transiciones de estado (`handleApply`, `handleDiscard`, `handleBack`, `handleCustomize`) y conversión bidireccional con `AdvancedStyleConfig`.
6. **`ThemeProposalDialog`:** Diálogo de pantalla completa Glass (`usePlatformDefaultWidth = false`) con miniatura reactiva `LauncherThemeMiniature`.
7. **`VideoFrameExtractor`:** Extractor puntual de 3 frames (10%, 50%, 90%) en `Dispatchers.IO` escalados a ~400 px con liberación estricta de `MediaMetadataRetriever`.
8. **`ThemePaletteAggregator`:** Agregador puro que selecciona el medoide geométrico RGB entre las paletas de los frames muestreados.
9. **`LauncherThemeTokens` & `LocalLauncherThemeTokens`:** Capa de tokens visuales runtime provista globalmente en `MainActivity`.
10. **`AdvancedStyleConfig`:** Modelo de configuración existente y fuente única de persistencia en DataStore.

---

## 4. Decisiones de Producto Consolidadas

* **A) Propuesta Opcional:** Adaptive Theming es una sugerencia opcional; el usuario siempre puede aplicar el fondo sin adoptar el tema.
* **B) Labels Semánticos:** Etiquetas universales `Dominante`, `Vibrante`, `Equilibrado` (cero marketing artificial).
* **C) Cardinalidad Flexible (1 a 4):** El sistema emite entre 1 y 4 propuestas según la riqueza del wallpaper.
* **D) Previsualización Transitoria:** La exploración opera en memoria (`updateStyleConfigTransient`); la persistencia solo ocurre en Apply o ajustes avanzados.
* **E) Independencia de Fondo y Tema en Descartar/Back:** Se aplica el fondo nuevo y se conserva el tema previo del usuario.
* **F) Personalización Editable:** "Personalizar" entrega la propuesta como base editable dentro de `AdvancedSettingsScreen`.
* **G) Alcance de Video Resuelto:** Videos alimentan propuestas idénticas mediante muestreo de 3 frames sin análisis continuo en reproducción.

---

## 5. Componentes Protegidos e Intactos

Queda terminantemente prohibido modificar o refactorizar sin evidencia y aprobación explícita:

* `VideoWallpaperManager.kt`
* `VideoBackground.kt`
* `WidgetHostManager.kt` *(Nota: Puede ser inspeccionado para la auditoría de Cold Start al formar parte del coste detectado en loadState, pero no debe modificarse durante la auditoría).*
* `NativeWidgetView.kt`
* `ViewOne.kt`
* `ViewTwo.kt`
* `LauncherAccessibilityService.kt`
* `NotificationMonitorService.kt`
* `LauncherNavState`
* `IconCache`
* `ShortcutIcon`
* `AlphabetIndexRail`
* Sistema de gestures del Drawer
* Motion System del Drawer
* Lógica de scroll y selección/cambio de categorías del Drawer
* Ciclo de vida y binding de AppWidgets
* Adaptive Wallpaper Theming (Tokens, Palette, Analyzer, Generador y Flow)

---

## 6. Performance Engineering / App Performance

### Estado Oficial de Fases
* **P0 — Auditoría Estática de Hotspots:** ✅ **COMPLETADA**
* **P1 — Instrumentación y Baseline Cuantitativa:** ✅ **COMPLETADA / VALIDADA**
* **P2-01 — Optimización Quirúrgica del Search del App Drawer:** ✅ **COMPLETADA / VALIDADA FÍSICAMENTE**
* **P2-02A — loadState Trace (I/O, Gson, Widgets en hilo principal):** ✅ **COMPLETADA**
* **P2-02B — Cold Start Map (Timeline completo y delimitación de frames):** ✅ **COMPLETADA**
* **P2-02C — Composition Trace (Perfetto track_event y hotspots de Compose):** ✅ **COMPLETADA**
* **P2-03 — EXP-PAGER-ZERO-PRELOAD (`beyondViewportPageCount = 0`):** ✅ **COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT PENDIENTE**
* **P2-04 — Siguiente Hotspot de Composición / Layout:** ⏳ **AUDITORÍA PENDIENTE**

---

### P1 — Infraestructura y Baseline Oficial de Referencia

Se cuenta con infraestructura de medición cuantitativa real, aislada en el módulo `:benchmark` (`com.android.test`), ejecutada sobre hardware físico sin modificar la app de producción en Release:
* **Módulo:** `:benchmark` (Jetpack Macrobenchmark 1.3.3, UIAutomator 2.3.0, AndroidX Test Runner 1.6.2).
* **Test Suites:** `StartupBenchmark` (`P1-STARTUP-COLD`, `P1-STARTUP-WARM`) y `DrawerFrameBenchmark` (`P1-DRAWER-SCROLL`, `P1-DRAWER-SEARCH`).
* **BuildType:** `benchmark` (`initWith(release)`, R8 minificado habilitado, shrinking activo, `<profileable android:shell="true" />`).
* **Documentación Oficial:** [`docs/PERFORMANCE_BASELINE_P1.md`](file:///c:/Users/migue/AndroidStudioProjects/AnimeLauncher/docs/PERFORMANCE_BASELINE_P1.md).

#### Dispositivo Físico de Referencia (Baseline Congelada)
* **Modelo:** POCO X6 5G (`23122PCD1G`, codename `garnet`)
* **SoC / CPU:** Qualcomm Snapdragon 7s Gen 2 (8 núcleos hasta 2.40 GHz, frecuencias sin modificar)
* **Memoria RAM:** 12 GB LPDDR4X (~4.7 GB disponible en ejecución)
* **Sistema Operativo:** Android 16 (API 36, user/release-keys)
* **Pantalla / Refresh Rate:** 1220×2712 px @ 120.0 Hz nativos (presupuesto de frame objetivo: **8.33 ms**)

#### Baseline de Startup (Congelada Histórica)
* **Cold TTID:** Mínimo: 537 ms | Mediana: **739 ms** | Máximo: 978 ms | Media: 736.8 ms
* **Warm TTID:** Mínimo: 120 ms | Mediana: **139 ms** | Máximo: 170 ms | Media: 144.0 ms (81.2% más rápido que cold)
* **TTFD:** No disponible actualmente porque la app no implementa `reportFullyDrawn()`. *(Regla estricta: NO inventar métricas ni estimar valores teóricos).*

#### Baseline de Frames en App Drawer (Congelada)
* **P1-DRAWER-SCROLL (120 Hz):**
  * `frameDurationCpuMs`: P50 = 5.9 ms | P90 = 11.4 ms | P95 = 12.9 ms | P99 = 30.0 ms
  * `frameOverrunMs`: P50 = -0.04 ms | P90 = 9.75 ms | P95 = 10.35 ms | P99 = 24.62 ms
* **P1-DRAWER-SEARCH (Baseline previa a optimización):**
  * `frameDurationCpuMs`: P50 = 8.2 ms | P90 = 20.9 ms | P95 = 31.6 ms | P99 = 66.6 ms (pico en traza: 218.3 ms)
  * `frameOverrunMs`: P50 = 2.5 ms | P90 = 23.9 ms | P95 = 41.7 ms | P99 = 112.5 ms

---

### P2-01 — Optimización Quirúrgica del Search del App Drawer (✅ COMPLETADA / VALIDADA FÍSICAMENTE)

* **Causa Técnica Confirmada:** En cada pulsación de tecla, `buildDrawerData` ejecutaba en el hilo principal: llamadas JNI a `Normalizer.normalize(..., Form.NFD)` por cada app instalada, `groupBy` repetido, reordenamiento alfabético O(N log N) con `sortedWith` y reconstrucción efímera de objetos `DrawerListItem`.
* **Solución Quirúrgica:**
  1. Desacoplamiento de trabajo estable (`prepareDrawerCategory`) y dinámico (`filterDrawerData`).
  2. La estructura categorizada y ordenada alfabéticamente se prepara **una única vez** al cambiar apps o categorías.
  3. Pre-normalización de nombres (`lowerName`) en memoria transitoria local.
  4. Filtrado directo O(N) por `searchQuery` reutilizando instancias precalculadas, sin sorting, sin grouping y con cero llamadas a `Normalizer`.
  5. Retorno instantáneo O(1) de `baseData` cuando la búsqueda está vacía.

#### Resultados Comparativos Empíricos (Hardware POCO X6 5G — 120 Hz)

| Métrica | Baseline P1 (Antes) | Optimizado P2-01 (Después) | Delta Numérico | Delta % |
| :--- | :---: | :---: | :---: | :---: |
| **`frameDurationCpuMs` P50** | 8.2 ms | **5.2 ms** | -3.0 ms | **-36.6%** |
| **`frameDurationCpuMs` P90** | 20.9 ms | **14.6 ms** | -6.3 ms | **-30.1%** |
| **`frameDurationCpuMs` P95** | 31.6 ms | **19.9 ms** | -11.7 ms | **-37.0%** |
| **`frameDurationCpuMs` P99** | 66.6 ms | **34.4 ms** | -32.2 ms | **-48.3%** |
| **`frameOverrunMs` P50** | 2.5 ms | **1.6 ms** | -0.9 ms | **-36.0%** |
| **`frameOverrunMs` P90** | 23.9 ms | **12.9 ms** | -11.0 ms | **-46.0%** |
| **`frameOverrunMs` P95** | 41.7 ms | **17.1 ms** | -24.6 ms | **-59.0%** |
| **`frameOverrunMs` P99** | 112.5 ms | **36.3 ms** | -76.2 ms | **-67.7%** |

* **Validaciones Aprobadas:**
  * Pruebas Unitarias: **62/62 PASS** (incluyendo 10 pruebas de equivalencia en `DrawerSearchPerformanceUnitTest`).
  * `assembleDebug` PASS, `lintDebug` PASS (0 errores), `bundleRelease` PASS.
  * Validación Física en POCO X6: Búsqueda instantánea sin stuttering perceptible; scroll y `AlphabetIndexRail` sin regresiones.

---

### Incidente de Benchmark y Regla Permanente

> [!WARNING]
> **Incidente Registrado:** Durante la preparación del entorno de benchmark se desinstaló temporalmente el paquete activo `com.daybreak.animelauncher`, lo que requirió reinstalarlo manualmente al ser el launcher home predeterminado del sistema.
>
> **Regla Permanente de Operación:**
> **"NO DESINSTALAR `com.daybreak.animelauncher` SI ES EL HOME ACTIVO."**
> Para benchmarking y pruebas futuras utilizar exclusivamente `am force-stop`, cambio transitorio de foreground a Settings (`am start -S com.android.settings/.Settings`), o reemplazo directo (`install -r -t`). Esto no constituye un bug de producción.

---

### P2-02 — Diagnóstico Integral de Cold Start (✅ COMPLETADA)

* **P2-02A (`loadState` Trace):** Se demostró empíricamente en variante optimizada `benchmark` (R8) que `loadState()` consume únicamente **10.06 ms** de mediana (~1.6% del Cold Start). No era el bottleneck dominante de 739 ms.
* **P2-02B (Cold Start Map):** Mapeo exhaustivo de timeline en Perfetto (`linux.ftrace`):
  * `bindApplication`: 87.29 ms
  * `activityResume`: 96.31 ms
  * **Primer `Choreographer#doFrame`:** **201.22 ms** (dominado por `AndroidOwner:onMeasure` en **85.09 ms**).
* **P2-02C (Composition Trace):** Rastreo de composición con Perfetto `track_event`:
  * Subcomposición de `BoxWithConstraints`: **20.83 ms** (ViewOne: 13.16 ms + ViewTwo: 7.67 ms).
  * Multiplicación por `ShortcutIcon`: **18.65 ms** / 36 llamadas (~0.51 ms por instancia, costo nominal inflado por doble pase y precarga).
  * Precomposición no deseada de `ViewTwo`: **9.49 ms** de composición propia en el primer frame.
  * `ScaffoldLayout`: 10.93 ms (Material 3) y `NavHost`: 10.34 ms (router raíz).

---

### P2-03 — EXP-PAGER-ZERO-PRELOAD (✅ COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT PENDIENTE)

#### 1. Cambio Exacto Realizado
En [LauncherScreen.kt:369](file:///c:/Users/migue/AndroidStudioProjects/AnimeLauncher/app/src/main/java/com/daybreak/animelauncher/ui/screens/LauncherScreen.kt#L369):
```kotlin
HorizontalPager(
    state = pagerState,
    userScrollEnabled = (navState == LauncherNavState.Home),
    beyondViewportPageCount = 0, // Cambio exclusivo: de 1 a 0
    key = { it },
    ...
```
**Fue el único cambio productivo del experimento.** Cero cambios adicionales.

#### 2. Resultados Oficiales (Hardware Real POCO X6 5G — 120 Hz)
Medición formal con variante `benchmark` (R8, sin runtime-tracing que contamine CPU):

* **Cold Start — Baseline A (`beyondViewportPageCount = 1`):**
  * `TotalTime` median = **532 ms** (Runs: 548, 532, 519, 538, 523 ms)
  * `WaitTime` median = **538 ms** (Runs: 551, 538, 525, 542, 527 ms)
  * `Startup Jank Rate` = **16.29%**
* **Cold Start — Baseline B (`beyondViewportPageCount = 0`):**
  * `TotalTime` median = **519 ms** (Runs: 491, 505, 519, 615, 637 ms)
  * `WaitTime` median = **523 ms** (Runs: 499, 511, 523, 620, 643 ms)
  * `Startup Jank Rate` = **5.98%**
* **Delta Cuantitativo:**
  * `TotalTime` median = **-13 ms (-2.4%)**
  * `WaitTime` median = **-15 ms (-2.8%)**
  * `TotalTime` mínimo = **491 ms** (Récord histórico absoluto, rompe barrera de 500 ms)
  * `Startup Jank Rate` = **-10.31% (-63.3% relativo)**
* **Warm Start (5 runs):**
  * `TotalTime` median = **124 ms** (Paridad 1:1, cero regresión).
* **Primer Swipe ViewOne → ViewTwo (Hardware Real a 120 Hz / presupuesto 8.33 ms):**
  * *Immediate (swipe en cuanto aparece Home):* P50 ≈ **5 ms** | P90 ≈ **10 ms** | P95 ≈ **14 ms** | Jank = **0.00%** (162 frames, 0 janky)
  * *Delayed 300 ms:* P50 ≈ **6 ms** | P90 ≈ **8 ms** | P95 ≈ **13 ms** | Jank = **1.45%** (138 frames, 2 janky)
  * *Repeated:* P50 ≈ **6 ms** | P90 ≈ **8 ms** | P95 ≈ **9 ms** | Jank = **0.00%** (142 frames, 0 janky)

#### 3. Interpretación Sobria
* **P2-03 se conserva porque:**
  1. Mejora de forma reproducible la mediana de Cold Start (**-13 ms**).
  2. Reduce drásticamente la contención y jank observada en startup (**-63.3%**).
  3. No degrada Warm Start (**124 ms** paritario).
  4. No degrada el primer swipe (P50 de 5 ms, holgadamente bajo el presupuesto de 8.33 ms a 120 Hz).
  5. Cero regresiones visuales o funcionales en fondos, widgets o navegación durante la validación física.
* **IMPORTANTE:** No afirmar que todos los 13 ms de mejora provienen exclusivamente de ViewTwo. Existe variabilidad entre runs; el resultado se registra como mejora empírica global del experimento A/B.

#### 4. Decisión sobre ViewTwo
* `ViewTwo` ya **NO** se precompone en cold start al operar con `beyondViewportPageCount = 0`.
* **NO** realizar ahora otra optimización sobre `ViewTwo`, `ShortcutIcon` ni `BoxWithConstraints`. Cualquier cambio adicional requiere nuevo profiling y experimento aislado.

---

### Próximo Hotspot: P2-04 — Composición / Layout (⏳ AUDITORÍA PENDIENTE)

* **Estado:** ⏳ **AUDITORÍA PENDIENTE**
* **Candidatos documentados de P2-02C:**
  1. `BoxWithConstraints` (Subcomposición en pase de layout).
  2. `ShortcutIcon` (Granularidad de nodos e interacción táctil).
  3. Composición inicial restante del árbol raíz.
* **Regla Estricta:** **NO asumir ninguno como próximo cambio definitivo.** Claude deberá realizar una auditoría específica antes de autorizar o modificar código.

---

### Workflow Oficial de Performance Engineering

```text
P0 Auditoría Estática [COMPLETADA]
       ↓
P1 Baseline Cuantitativa (:benchmark, TTID, TTFD, Frames) [COMPLETADA]
       ↓
P2-01 Optimización Search (DrawerListItems / AppDrawerScreen) [COMPLETADA]
       ↓
P2-02 Diagnóstico Integral Cold Start (A: loadState, B: Timeline Map, C: Composition Trace) [COMPLETADA]
       ↓
P2-03 EXP-PAGER-ZERO-PRELOAD (HorizontalPager beyondViewportPageCount = 0) [COMPLETADA]
       ↓
P2-04 Siguiente Hotspot de Composición / Layout [AUDITORÍA PENDIENTE]
       ↓
P2-05 saveState() (Mutaciones y persistencia no síncrona)
       ↓
P2-06 Compose Stability (Profiling de recomposiciones en LauncherState)
```

* **Regla Inquebrantable:**
  ```text
  MEDIR  →  LOCALIZAR  →  CAMBIAR (QUIRÚRGICO)  →  MEDIR OTRA VEZ
  ```
  *Exactamente un experimento por cambio. Prohibido acumular optimizaciones.*

---

## 7. Próxima Acción Oficial

* **NEXT:** `Performance Engineering — Fase P2-04: Auditoría Técnica del Siguiente Hotspot de Composición / Layout`.
* **Responsable:** Claude realizará una auditoría específica antes de autorizar cualquier modificación de código.
* **Restricción Estricta:** Cero modificaciones de código en producción durante la fase de auditoría.

---

## 8. Protocolo de Trabajo y Workflow Oficial

1. **Auditoría / Análisis Técnico** (delimitación del problema con métricas).
2. **Revisión del Reporte** (validación de hipótesis).
3. **Prompt Quirúrgico de Implementación** (alcance mínimo y cerrado).
4. **Implementación con Antigravity** (un solo agente modificando código a la vez).
5. **Verificación Automatizada Completa:** `testDebugUnitTest`, `assembleDebug`, `lintDebug`, `bundleRelease`.
6. **Prueba Física en Dispositivo Real (POCO X6 5G).**
7. **Medición Cuantitativa Comparativa** (antes vs después).
8. **Correcciones Dirigidas** (si se presentan desviaciones).
9. **Confirmación Física.**
10. **Actualización del Cerebro** (`AGENT_PROJECT_STATE.md`).
11. **Git Checkpoint Oficial.**

---

## 9. Seguridad de Credenciales y Clave de Subida (Upload Key v2)

* **Ubicación Keystore Privada:** `C:\Users\migue\NovaLauncher-Keys\upload-keystore-v2.jks` *(FUERA DEL REPO)*
* **Ubicación Certificado PEM Público:** `C:\Users\migue\NovaLauncher-Keys\upload_certificate-v2.pem` *(FUERA DEL REPO)*
* **Alias Oficial:** `novalauncher-upload-v2`
* **Algoritmo y Tamaño:** RSA 4096 bits (`sha384RSA`), validez de 10,000 días (expira en febrero de 2054).
* **Huella Digital SHA-256 del Certificado:**
  ```text
  2A:DF:B7:D2:4E:62:7C:29:52:42:2B:28:D7:11:F3:D9:34:F1:9E:66:FB:B3:05:3F:AB:14:E7:97:82:39:3B:F1
  ```
* **Regla de Seguridad:** Cero claves ni contraseñas en el repositorio. Archivos sensibles ignorados por `.gitignore`.

---

## 10. Validaciones Automatizadas Consolidadas

* `git diff --check`: **PASS** (0 errores de formato, fin de línea o sintaxis).
* `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (**62/62 pruebas unitarias aprobadas al 100%**).
* `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (APK generado e instalado exitosamente en POCO X6).
* `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errores).
* `.\gradlew.bat bundleRelease`: **BUILD SUCCESSFUL** (Minificación R8 y `lintVitalRelease` exitosos; AAB generado).
* Suite de Benchmarks en Hardware Físico (`POCO X6 5G`): **PASS** (`StartupBenchmark` + `DrawerFrameBenchmark`).

---

## 11. Tareas Pendientes y Hoja de Ruta

1. **Performance Engineering — Fase P2-04 (Composición / Layout):** Auditoría técnica y selección del próximo experimento A/B de optimización sobre los hotspots de composición restantes.
2. **Estabilización de Video Resume (Independiente):** Diagnóstico y corrección de la re-vinculación de `TextureView` en `VideoWallpaperManager` al regresar de apps pesadas.
3. **Google Play Console — Restablecimiento de Upload Key:** Confirmar el procesamiento del certificado `upload_certificate-v2.pem` por parte de Google (ventana de 24-48 horas).
4. **Fase 5E-3 — Configuración Segura de Firma:** Configurar `signingConfigs.release` en Gradle mediante `keystore.properties` desacoplado fuera del control de versiones.
5. **Accesibilidad y Ficha de Tienda:** Preparar video demostrativo de accesibilidad para Play Console y activos gráficos finales.
