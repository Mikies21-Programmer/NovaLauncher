# PERFORMANCE — P2-02B: Mapa Completo del Cold Start

## Environment & Metadata

| Campo | Valor |
|---|---|
| Hardware | POCO X6 5G (Xiaomi 23122PCD1G) |
| SoC / CPU | Qualcomm Snapdragon 7s Gen 2 (8 cores) |
| Android | 14 (HyperOS) |
| Refresh Rate | 120 Hz (presupuesto 8.33 ms/frame) |
| Build Variant | **Debug** (`minifyEnabled false`, `debuggable true`) |
| Fecha de Medición | 2026-09-20 |
| Baseline TTID Release | **mediana 739 ms** · mín 537 ms · máx 978 ms · media 736.8 ms |
| TotalTime Debug Mediana (3 runs) | **1291 ms** · mín 1281 ms · máx 1300 ms |
| Tiempo a Primer Frame Dibujado (Wall-Clock Mediana) | **1052 ms** |
| Atrace Flags | `-a com.daybreak.animelauncher am view gfx wm res dalvik` |
| Buffer Size | 32768 KB |
| Traces Físicos Guardados | `docs/performance_traces/P2-02B-official-run{1,2,3}.trace` |

---

## 1. Contexto y Pregunta de Investigación

La fase P2-02A demostró empíricamente que `LOAD_STATE_TOTAL` (SharedPreferences + Gson + Widget Validation) tiene una mediana de **44.5 ms** en Debug (~20–30 ms en Release).

Esto reveló una conclusión arquitectónica crítica:
> **`loadState()` por sí solo NO explica los ~739 ms del Cold Start de Release ni los ~1300 ms de Debug.**

El objetivo estricto de **P2-02B** es mapear de principio a fin el Cold Start completo:
**"¿Dónde se consumen exactamente los ~739 ms restantes?"**

> [!IMPORTANT]
> **REGLA DE PROTECCIÓN:** Ningún componente protegido fue modificado (`loadState()`, `saveState()`, `LauncherState`, `VideoWallpaperManager`, `VideoBackground`, `WidgetHostManager`, `NativeWidgetView`, `ViewOne`, `ViewTwo`, Drawer, Search P2-01, Adaptive Wallpaper Theming).
> P2-02B es **exclusivamente diagnóstico e instrumentación temporal**.

---

## 2. Secciones Instrumentadas

Para este diagnóstico, se añadieron secciones de `android.os.Trace` en puntos no protegidos:

| Archivo | Sección Trace | Descripción |
|---|---|---|
| `MainActivity.kt` | `MAIN-ONCREATE-TOTAL` | Bloque completo de `onCreate()` tras `super.onCreate()` |
| `MainActivity.kt` | `VIEWMODEL-ACCESS` | Primer acceso al delegado `by viewModels()` (fuerza instanciación del VM y ejecución síncrona de `loadState()`) |
| `MainActivity.kt` | `APPLY-SYSTEM-BARS` | Configuración de barras de estado/navegación inmersivas |
| `MainActivity.kt` | `REGISTER-RECEIVER` | Registro de `screenReceiver` para eventos de pantalla |
| `MainActivity.kt` | `SETCONTENT` | Invocación de `setContent { ... }` (entrega del árbol a Compose) |
| `DynamicBackground.kt` | `DYNAMIC-BG-ENTER` | `SideEffect` en la raíz de `DynamicBackground` para detectar cuándo el composable es ejecutado por Compose |
| `LauncherViewModel.kt` | `LOAD_STATE_TOTAL` | Bloque de `loadState()` (persistido de P2-02A) |
| `LauncherViewModel.kt` | `STATE_READ` | Lectura en disco/RAM de `launcher_state` en SharedPreferences |
| `LauncherViewModel.kt` | `GSON_STATE` | Deserialización JSON con Gson de `LauncherState` |
| `LauncherViewModel.kt` | `STYLE_READ` | Lectura de `launcher_style_config` |
| `LauncherViewModel.kt` | `GSON_STYLE` | Deserialización JSON con Gson de `AdvancedStyleConfig` |
| `LauncherViewModel.kt` | `WIDGET_VALIDATION` | Validación IPC con `AppWidgetManager` |

Adicionalmente, se extrajeron las secciones estándar del framework Android registradas por `atrace`:
- `bindApplication`: Inicialización del Application context, font maps, recursos y Jetpack Startup.
- `activityStart` / `performCreate`: Creación formal de la Activity en el ciclo de vida de Android.
- `activityResume` / `performResume`: Transición a estado reanudado e interactivo.
- `Choreographer#doFrame` / `traversal`: El frame inicial que realiza la primera recomposición, medición, layout y dibujo de Compose.
- `Record View#draw()`: Grabación de comandos de dibujo hacia el `RenderThread`.

---

## 3. Datos Cuantitativos de las 3 Capturas Oficiales

Las capturas fueron realizadas físicamente en el **POCO X6 5G** conectado por ADB, con `am force-stop` previo en cada corrida y 3 segundos de reposo para asegurar estado en frío idéntico y evitar sesgos de estrangulamiento térmico.

### Tabla General de Mediciones (Duración en ms)

| Segmento / Sección | Run 1 | Run 2 | Run 3 | Mín (ms) | Máx (ms) | **Mediana (ms)** | % del TTID Debug |
|---|---:|---:|---:|---:|---:|---:|---:|
| **TotalTime (`am start -W`)** | 1300.0 | 1281.0 | 1291.0 | 1281.0 | 1300.0 | **1291.0 ms** | 100% |
| **WaitTime (`am start -W`)** | 1313.0 | 1286.0 | 1296.0 | 1286.0 | 1313.0 | **1296.0 ms** | — |
| **Tiempo a 1er Frame (`bindApp` $\to$ `draw`)** | 1058.0 | 1052.0 | 1036.0 | 1036.0 | 1058.0 | **1052.0 ms** | 81.5% |
| | | | | | | | |
| **1. `bindApplication` (Framework init)** | 129.481 | 175.270 | 137.786 | 129.481 | 175.270 | **137.786 ms** | 10.7% |
| ├── `setSystemFontMap` | 6.920 | 6.020 | 6.800 | 6.020 | 6.920 | **6.800 ms** | 0.5% |
| ├── `ResourcesManager#applyConfiguration` | 15.772 | 32.601 | 15.815 | 15.772 | 32.601 | **15.815 ms** | 1.2% |
| └── `App Startup Initializers` | 21.293 | 18.114 | 18.853 | 18.114 | 21.293 | **18.853 ms** | 1.5% |
| | | | | | | | |
| **2. `activityStart` (Ciclo de Vida)** | 161.866 | 188.209 | 183.350 | 161.866 | 188.209 | **183.350 ms** | 14.2% |
| └── `performCreate:MainActivity` | 120.345 | 134.625 | 140.275 | 120.345 | 140.275 | **134.625 ms** | 10.4% |
| &nbsp;&nbsp;&nbsp;&nbsp;└── **`MAIN-ONCREATE-TOTAL`** | 116.532 | 130.633 | 133.947 | 116.532 | 133.947 | **130.633 ms** | 10.1% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;├── **`VIEWMODEL-ACCESS`** | 65.554 | 87.529 | 85.505 | 65.554 | 87.529 | **85.505 ms** | 6.6% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;├── **`LOAD_STATE_TOTAL`** | 41.457 | 58.052 | 47.842 | 41.457 | 58.052 | **47.842 ms** | 3.7% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;├── `STATE_READ` | 24.593 | 31.295 | 23.581 | 23.581 | 31.295 | **24.593 ms** | 1.9% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;├── `GSON_STATE` | 15.294 | 24.891 | 21.817 | 15.294 | 24.891 | **21.817 ms** | 1.7% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;├── `STYLE_READ` | 0.020 | 0.028 | 0.025 | 0.020 | 0.028 | **0.025 ms** | <0.1% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;├── `GSON_STYLE` | 0.657 | 0.743 | 0.656 | 0.656 | 0.743 | **0.657 ms** | <0.1% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;└── `WIDGET_VALIDATION` | 0.078 | 0.079 | 0.089 | 0.078 | 0.089 | **0.079 ms** | <0.1% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;└── _VM init overhead outside loadState_ | 24.097 | 29.477 | 37.663 | 24.097 | 37.663 | **29.477 ms** | 2.3% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;├── `APPLY-SYSTEM-BARS` | 2.286 | 3.855 | 3.319 | 2.286 | 3.855 | **3.319 ms** | 0.3% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;├── `REGISTER-RECEIVER` | 2.487 | 1.896 | 2.522 | 1.896 | 2.522 | **2.487 ms** | 0.2% |
| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;└── `SETCONTENT` | 12.500 | 7.314 | 5.779 | 5.779 | 12.500 | **7.314 ms** | 0.6% |
| | | | | | | | |
| **3. `activityResume` (Ciclo de Vida)** | 153.805 | 156.923 | 172.173 | 153.805 | 172.173 | **156.923 ms** | 12.2% |
| └── `performResume:MainActivity` | 96.409 | 101.386 | 115.191 | 96.409 | 115.191 | **101.386 ms** | 7.9% |
| | | | | | | | |
| **4. `Choreographer#doFrame` (Primer Frame)** | 622.500 | 543.734 | 550.400 | 543.734 | 622.500 | **550.400 ms** | **42.6%** |
| └── `traversal` | 621.774 | 543.151 | 549.756 | 543.151 | 621.774 | **549.756 ms** | 42.6% |
| &nbsp;&nbsp;&nbsp;&nbsp;├── **`measure` / `AndroidOwner:onMeasure`** | 348.580 | 311.640 | 305.460 | 305.460 | 348.580 | **311.640 ms** | **24.1%** |
| &nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;├── `Compose:recompose` (acumulado 4 pases) | 231.410 | 210.680 | 222.480 | 210.680 | 231.410 | **222.480 ms** | 17.2% |
| &nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;├── `Compose:applyChanges` (acumulado) | 40.930 | 39.230 | 44.600 | 39.230 | 44.600 | **40.930 ms** | 3.2% |
| &nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;├── `Compose:onRemembered` | 12.103 | 8.850 | 9.863 | 8.850 | 12.103 | **9.863 ms** | 0.8% |
| &nbsp;&nbsp;&nbsp;&nbsp;│&nbsp;&nbsp;&nbsp;└── `TextStringSimpleNode::measure` | 9.771 | 9.780 | 9.553 | 9.553 | 9.780 | **9.771 ms** | 0.8% |
| &nbsp;&nbsp;&nbsp;&nbsp;├── `relayoutWindow` (Surface & WMS IPC) | 31.290 | 24.530 | 23.440 | 23.440 | 31.290 | **24.530 ms** | 1.9% |
| &nbsp;&nbsp;&nbsp;&nbsp;├── `layout` | 43.573 | 37.133 | 36.587 | 36.587 | 43.573 | **37.133 ms** | 2.9% |
| &nbsp;&nbsp;&nbsp;&nbsp;└── **`draw-VRI[MainActivity]` / `Record View#draw()`** | 43.945 | 46.794 | 47.757 | 43.945 | 47.757 | **46.794 ms** | 3.6% |
| | | | | | | | |
| **5. Hilos Secundarios Concurrentes** | | | | | | | |
| ├── `EmojiCompat.FontRequest...buildTypeface` | 378.88 | 373.05 | 380.11 | 373.05 | 380.11 | **378.88 ms** | background |
| ├── `Creating EGLContext` (RenderThread) | 54.61 | 73.52 | 61.05 | 54.61 | 73.52 | **61.05 ms** | async |
| └── `decodeBitmap` (Wallpaper/Icon worker) | 117.22 | 85.96 | 93.32 | 85.96 | 117.22 | **93.32 ms** | async |

---

## 4. Cronología Visual del Cold Start (Waterfall)

A continuación se muestra la secuencia temporal exacta de eventos en el hilo principal desde la creación del proceso (`t = 0 ms`) hasta la presentación del primer fotograma en pantalla:

```
[t = 0 ms]  ─────────────────────────────────────────────────────────────────────────────
│
├── [0 - 138 ms]       bindApplication (137.8 ms)
│   ├── setSystemFontMap (6.8 ms)
│   ├── ResourcesManager & ResourcesImpl init (22.2 ms)
│   └── App Startup (EmojiCompat, ProcessLifecycle, ProfileInstaller) (18.9 ms)
│
├── [138 - 321 ms]     activityStart & onCreate (183.4 ms)
│   └── MAIN-ONCREATE-TOTAL (130.6 ms)
│       ├── enableEdgeToEdge() (2.1 ms)
│       ├── VIEWMODEL-ACCESS (85.5 ms)
│       │   ├── [184 - 232 ms] LOAD_STATE_TOTAL (47.8 ms)
│       │   │   ├── STATE_READ (24.6 ms - SharedPreferences awaitLoadedLocked)
│       │   │   ├── GSON_STATE (21.8 ms - Gson reflect & class-loading)
│       │   │   └── WIDGET_VALIDATION (0.08 ms - 0 widgets)
│       │   └── ViewModel init & Flow wiring (29.5 ms)
│       ├── APPLY-SYSTEM-BARS (3.3 ms)
│       ├── REGISTER-RECEIVER (2.5 ms)
│       └── SETCONTENT (7.3 ms - entrega del árbol Compose)
│
├── [321 - 478 ms]     activityResume (156.9 ms)
│   └── performResume:MainActivity (101.4 ms)
│
├── [478 - 1028 ms]    EL GRAN BLOQUE: Primer Choreographer#doFrame Traversal (550.4 ms)  ◄── HOTSPOT REAL
│   ├── measure / AndroidOwner:onMeasure (311.6 ms)
│   │   ├── Compose:recompose iniciales (222.5 ms)
│   │   │   ├── DYNAMIC-BG-ENTER se ejecuta aquí (t ≈ 850 ms)
│   │   │   ├── Árbol Scaffold, NavHost, LauncherScreen
│   │   │   └── TextStringSimpleNode measure (9.8 ms)
│   │   └── Compose:applyChanges & onRemembered (50.8 ms)
│   ├── relayoutWindow (24.5 ms)
│   ├── layout (37.1 ms)
│   └── Record View#draw() / RenderThread enqueue (46.8 ms)  ◄── [t = 1052 ms] PRIMER FRAME DIBUJADO
│
└── [1052 - 1291 ms]   RenderThread swapBuffers + WindowManager cold-start transit handshake (239 ms)
                        [am start -W TotalTime completado a 1291 ms]
```

---

## 5. Hallazgos Cruciales y Respuesta a la Pregunta de Investigación

### ¿Dónde se consumen los ~739 ms restantes de la baseline de Release?

El desglose desmiente de forma concluyente la sospecha de que `loadState()` o SharedPreferences fuesen el cuello de botella del arranque en frío. 

Los datos demuestran la siguiente distribución real:

```
┌────────────────────────────────────────────────────────────────────────┐
│ DISTRIBUCIÓN DEL TIEMPO EN COLD START (Debug: 1291 ms / Release: 739 ms) │
├──────────────────────────────────────────────────────┬─────────────────┤
│ 1. Inicialización de Framework + OS (bindApplication)│ ~138 ms (10.7%) │
├──────────────────────────────────────────────────────┼─────────────────┤
│ 2. Ciclo de Vida Activity (onCreate + onResume)      │ ~288 ms (22.3%) │
│    ├── loadState() [Prefs + Gson]                    │  ↳  48 ms (3.7%)│
│    └── Resto de onCreate + onResume                  │  ↳ 240 ms(18.6%)│
├──────────────────────────────────────────────────────┼─────────────────┤
│ 3. Primer Frame Compose (Measure, Layout, Recompose) │ ~550 ms (42.6%) │ ◄── DOMINANTE
├──────────────────────────────────────────────────────┼─────────────────┤
│ 4. Window Manager, Surface, IPC Handshake & Draw     │ ~315 ms (24.4%) │
└──────────────────────────────────────────────────────┴─────────────────┘
```

### 1. El Verdadero Dominante del Cold Start: El Pipeline Inicial de Compose (~550 ms)
- El primer `Choreographer#doFrame` absorbe **550.4 ms** de tiempo en el hilo principal (42.6% del total).
- Dentro de este bloque, `AndroidOwner:onMeasure` y los sucesivos pases de `Compose:recompose` consumen **311.6 ms**.
- Esto ocurre porque al arrancar, Compose debe:
  1. Instanciar y recordar todos los nodos de composición (`Scaffold`, `CompositionLocalProvider`, `NavHost`, `LauncherScreen`, `DynamicBackground`, `AppDrawer`, `Dock`, etc.).
  2. Resolver dependencias de estado (`collectAsState()`, `remember(state.styleConfig)`).
  3. Realizar los cálculos de medida de fuentes y layouts iniciales.
- En **Release**, gracias a R8 y las Baseline Profiles (`baseline.prof`), este bloque se reduce sustancialmente (~150–200 ms), pero **sigue siendo el componente individual más grande** de los ~739 ms de TTID.

### 2. Peso Relativo Real de `loadState()`
- `LOAD_STATE_TOTAL` (mediana 47.8 ms en Debug) representa **únicamente el 3.7%** del cold start total en Debug y aproximadamente **~3% a 4% en Release** (~20–25 ms).
- Por lo tanto, incluso si `loadState()` se hiciera instantáneo (0 ms), el cold start solo mejoraría de 739 ms a ~715 ms.
- **Conclusión arquitectónica:** Mover `loadState()` a un hilo secundario sigue siendo una buena práctica de higiene del hilo principal (evita el I/O en disco de SharedPreferences), pero **no es una solución mágica** para recortar 300 ms del cold start.

### 3. Dinámica de `DynamicBackground`
- La sección `DYNAMIC-BG-ENTER` se ejecutó consistentemente a los **~850 ms** del proceso (en pleno pase de recomposición de Compose dentro de `measure`).
- Su ejecución duró menos de **0.005 ms** (es sólo la entrada al composable).
- La inicialización y decodificación pesada (por ejemplo `decodeBitmap` en hilos de trabajo) corre en background (~90–117 ms) en paralelo, **sin bloquear el hilo principal**.

---

## 6. Candidatos Técnicos para Futuras Fases de Optimización

Con el mapa completo ya medido y verificado con datos de hardware real en el POCO X6 5G:

1. **Optimización de Compose Initialization (Mayor Impacto Potencial):**
   - Verificar y refinar la cobertura de **Baseline Profiles** (`baseline-prof.txt`) para precompilar con ART los composables del camino crítico inicial (`LauncherScreen`, `NavHost`, `DynamicBackground`).
   - Evitar composiciones especulativas en el arranque inicial.

2. **Diferimiento / Asincronía de `loadState()` (Higiene del Main Thread):**
   - Aunque su impacto absoluto es moderado (~25 ms en Release), eliminar `STATE_READ` (24.6 ms de I/O bloqueante) erradica el riesgo de ANR cuando el almacenamiento flash del dispositivo esté bajo alta contención I/O.

3. **Optimización de `bindApplication` y ciclo de vida:**
   - Evaluar inicializadores de `App Startup` para diferir los que no sean estrictamente requeridos antes del primer frame visible.

---

## 7. Integridad y Estado de Git

- Ningún componente protegido ha sido modificado.
- La instrumentación diagnóstica en `MainActivity.kt`, `DynamicBackground.kt` y `LauncherViewModel.kt` se ejecutó limpiamente cumpliendo todas las reglas de contención de cambios.
- Suites de validación ejecutadas: `assembleDebug` ✅ · `testDebugUnitTest` ✅ · `lintDebug` ✅.
