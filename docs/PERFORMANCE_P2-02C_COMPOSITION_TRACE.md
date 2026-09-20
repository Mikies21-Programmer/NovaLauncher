# NovaLauncher — P2-02C: Composition Trace del Cold Start

**Fecha:** 2026-09-20  
**Dispositivo:** Xiaomi POCO X6 5G (Hardware Real — `2e81d6a9`)  
**Sistema Operativo:** Xiaomi HyperOS / Android 14 (API 34)  
**Tasa de refresco:** 120 Hz fija  
**Variante:** `benchmark` (R8 minificado, optimizaciones de release completas, Compose runtime-tracing activo)  
**Traces oficiales guardados:**
- `docs/performance_traces/P2-02C-official-run1.pftrace` (9.73 MB)
- `docs/performance_traces/P2-02C-official-run2.pftrace` (9.89 MB)
- `docs/performance_traces/P2-02C-official-run3.pftrace` (9.91 MB)

---

## 1. Configuración de Medición e Instrumentación

Para habilitar Composition Tracing en hardware real sobre la variante optimizada `benchmark` sin contaminar la variante `release` final de producción, se implementó la siguiente arquitectura de diagnóstico:

1. **Dependencias del Runtime de Compose Tracing:**
   - `:app`: `benchmarkImplementation("androidx.compose.runtime:runtime-tracing")`
   - `:app`: `benchmarkImplementation("androidx.tracing:tracing-perfetto:1.0.0")`
   - Proguard Benchmark Rules (`app/proguard-benchmark-rules.pro`): reglas `-keep` para `androidx.tracing.perfetto.**` y `androidx.compose.runtime.tracing.**`.
2. **Motor de Captura Nativo Perfetto:**
   - Binario de tracing `libtracing_perfetto.so` (arm64-v8a) desplegado en `/data/local/tmp/libtracing_perfetto.so` (con permisos 755).
   - Activación persistente de startup tracing mediante broadcast `androidx.tracing.perfetto.action.ENABLE_TRACING_COLD_START` con extra persistente (`-e persistent true`) hacia `StartupTracingReceiver`.
   - Inicialización automática en el fork del proceso vía `StartupTracingInitializer` de `androidx.startup`.
   - Configuración de captura Perfetto en `/data/misc/perfetto-configs/perfetto_cold.pbtxt` habilitando simultáneamente data source `linux.ftrace` (categorías `am`, `view`, `gfx`, `wm`, `res`, `dalvik`, `ftrace/print`, app `com.daybreak.animelauncher`) y data source `track_event` (Compose runtime tracing).
   - Trace Processor v45.0 (`trace_processor_shell_aarch64`) en `/data/local/tmp/trace_processor` para parsing SQLite instantáneo de eventos de bajo nivel.

---

## 2. Métricas Cuantitativas Oficiales (Cold Start)

Se ejecutaron 3 lanzamientos en frío oficiales tras forzar detención (`am force-stop`), pantalla encendida y desbloqueada, midiendo mediante `am start -W -n com.daybreak.animelauncher/.MainActivity` y correlacionando con los slices de Perfetto.

### 2.1 Resumen Global de Lanzamiento

| Métrica | Run 1 | Run 2 | Run 3 | Mediana | Media |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **TotalTime (`am start -W`)** | **713 ms** | **631 ms** | **602 ms** | **631.0 ms** | **648.7 ms** |
| **WaitTime (`am start -W`)** | **718 ms** | **640 ms** | **607 ms** | **640.0 ms** | **655.0 ms** |
| **Primer `Choreographer#doFrame`** | **214.83 ms** | **198.34 ms** | **201.22 ms** | **201.22 ms** | **204.80 ms** |
| **Primer `traversal`** | **214.65 ms** | **198.24 ms** | **201.12 ms** | **201.12 ms** | **204.67 ms** |
| **`AndroidOwner:onMeasure`** | **85.09 ms** | **80.23 ms** | **85.71 ms** | **85.09 ms** | **83.68 ms** |
| **`MAIN-ONCREATE-TOTAL`** | **39.37 ms** | **44.32 ms** | **32.44 ms** | **39.37 ms** | **38.71 ms** |
| **`VIEWMODEL-ACCESS`** | **18.18 ms** | **20.35 ms** | **18.06 ms** | **18.18 ms** | **18.86 ms** |
| **`LOAD_STATE_TOTAL`** | **5.71 ms** | **10.06 ms** | **11.88 ms** | **10.06 ms** | **9.22 ms** |

---

## 3. Desglose Detallado por Etapas

### 3.1 Etapa 1: Fork de Proceso y Application Startup
- **`bindApplication`:** Mediana **87.29 ms** (Rango: 81.91 — 88.78 ms). Carga inicial de librerías del sistema, inicializadores de Jetpack Startup y configuración de recursos.
- **`ActivityThreadMain`:** Mediana **31.07 ms** (Rango: 24.89 — 49.04 ms).
- **`activityStart`:** Mediana **56.16 ms** (Rango: 45.81 — 64.10 ms).

### 3.2 Etapa 2: MainActivity.onCreate y LauncherViewModel
- **`performCreate:MainActivity`:** Mediana **40.73 ms** (Rango: 33.80 — 45.47 ms).
- **`MAIN-ONCREATE-TOTAL`:** Mediana **39.37 ms** (Rango: 32.44 — 44.32 ms).
- **`VIEWMODEL-ACCESS` (instanciación e inicialización del ViewModel):** Mediana **18.18 ms** (Rango: 18.06 — 20.35 ms).
  - Dentro de `VIEWMODEL-ACCESS`, la función `loadState()` consume:
    - **`LOAD_STATE_TOTAL`:** Mediana **10.06 ms** (Rango: 5.71 — 11.88 ms).
      - `GSON_STATE`: Mediana **5.74 ms** (3.71 — 6.28 ms)
      - `WIDGET_VALIDATION`: Mediana **1.60 ms** (1.24 — 3.42 ms)
      - `STATE_READ`: Mediana **0.03 ms** (0.02 — 4.48 ms)
      - `GSON_STYLE`: Mediana **0.11 ms** (0.10 — 0.11 ms)
      - `STYLE_READ`: Mediana **0.01 ms** (0.01 — 0.03 ms)

### 3.3 Etapa 3: MainActivity.onResume
- **`activityResume`:** Mediana **96.31 ms** (Rango: 91.33 — 157.42 ms).
- **`performResume:MainActivity`:** Mediana **54.04 ms** (Rango: 52.63 — 87.99 ms).

### 3.4 Etapa 4: Primer Traversal de Frame (Choreographer)
- **Primer `Choreographer#doFrame`:** Mediana **201.22 ms** (Rango: 198.34 — 214.83 ms).
- **`traversal`:** Mediana **201.12 ms** (Rango: 198.24 — 214.65 ms).
- **`measure` / `AndroidOwner:onMeasure`:** Mediana **85.09 ms** (Rango: 80.23 ms — 85.84 ms).
- **`Record View#draw()`:** Mediana **18.00 ms** (Rango: 17.31 — 20.60 ms).

---

## 4. Ranking de Composición (Composition Tracing)

A través del data source `track_event` y Compose runtime tracing, se identificaron y cronometraron de forma unívoca todas las funciones `@Composable` ejecutadas durante el arranque en frío inicial.

A continuación se presenta el **ranking de impacto acumulado de composición**, ordenado de mayor a menor tiempo total consumido:

| Ranking | Función Composable | Archivo Fuente / Línea | Invocaciones | Duración Total (Mediana) | Duración Máx (Mediana) | Rol / Naturaleza |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **1** | `CompositionLocalProvider` | `CompositionLocal.kt:375` | 8 | **23.31 ms** | 5.26 ms | Inyección de contexto (Theme, Insets, Owners) |
| **2** | `BoxWithConstraints.<anonymous>` | `BoxWithConstraints.kt:66` | 2 | **20.83 ms** | **13.16 ms** | **Subcomposición de layout dinámico** (ViewOne/ViewTwo) |
| **3** | `ShortcutIcon` | `ShortcutIcon.kt:112` | 36 | **18.65 ms** | 2.07 ms | Renderizado masivo de iconos de aplicaciones |
| **4** | `ViewOne.<anonymous>` | `ViewOne.kt:78` | 1 | **13.11 ms** | **13.11 ms** | **Pantalla principal ViewOne** |
| **5** | `CompositionLocalProvider` | `CompositionLocal.kt:395` | 6 | **13.49 ms** | 6.65 ms | Inyección de estilos de texto y tipografía |
| **6** | `ScaffoldLayout.<anonymous>` | `Scaffold.kt:162` | 1 | **10.93 ms** | **10.93 ms** | **SubcomposeLayout de Material3 Scaffold** |
| **7** | `MainActivity.onCreate.<anonymous>` | `MainActivity.kt:189` | 1 | **10.86 ms** | 10.86 ms | Bloque contenedor de contenido de la actividad |
| **8** | `NavHost` | `NavHost.kt:210 / 490` | 1 | **10.34 ms** | 10.34 ms | Grafo de navegación de Jetpack Compose |
| **9** | `ViewTwo.<anonymous>` | `ViewTwo.kt:87` | 1 | **9.49 ms** | **9.49 ms** | Pantalla secundaria ViewTwo (precompuesta en Pager) |
| **10** | `SaveableStateHolderImpl.SaveableStateProvider` | `SaveableStateHolder.kt:70` | 3 | **6.72 ms** | 4.69 ms | Preservación de estado en Pager y NavHost |
| **11** | `AnimatedContent` | `AnimatedContent.kt:773` | 1 | **6.75 ms** | 6.75 ms | Transiciones y animaciones de navegación |
| **12** | `AnimatedEnterExitImpl` | `AnimatedVisibility.kt:715` | 3 | **6.14 ms** | 6.04 ms | Efectos de visibilidad animada en arranque |
| **13** | `LauncherScreen` | `LauncherScreen.kt:77` | 2 | **5.59 ms** | 4.35 ms | Composable raíz del launcher |
| **14** | `Icon` | `Icon.kt:69 / 142` | 21 | **4.96 ms** | 0.57 ms | Iconografía estática |
| **15** | `DynamicBackground` | `DynamicBackground.kt:21` | 2 | **3.35 ms** | 2.04 ms | Fondo dinámico / gradientes / wallpaper |
| **16** | `AnimeLauncherTheme` | `Theme.kt:28` | 1 | **3.41 ms** | 3.41 ms | Árbol de tema global (Adaptive Colors) |
| **17** | `MaterialTheme` | `MaterialTheme.kt:59` | 1 | **2.96 ms** | 2.96 ms | Configuración de Material 3 tokens |
| **18** | `Image` | `Image.kt:156` | 18 | **2.84 ms** | 0.55 ms | Componentes gráficos de imagen |
| **19** | `rememberAsyncImagePainter` | `SingletonAsyncImagePainter.kt:133`| 2 | **2.61 ms** | 1.53 ms | Pre-resolución de Coil image loaders |
| **20** | `RealTimeBattery` | `RealTimeBattery.kt:80` | 4 | **2.63 ms** | 1.10 ms | Indicador de batería en tiempo real |
| **21** | `RealTimeClock` | `RealTimeClock.kt:18` | 1 | **1.05 ms** | 1.05 ms | Reloj digital y fecha del launcher |

---

## 5. Correlación de Frame Traversal y Composición

El trace evidencia la conexión causal directa entre la arquitectura de Composición y la duración del primer frame:

1. **`AndroidOwner:onMeasure` (85.09 ms):**
   - El dominante absoluto del primer frame es la fase de medición (`measure`).
   - El motivo técnico demostrado por los slices de Composition Tracing es la presencia de **`SubcomposeLayout`**:
     - `BoxWithConstraints` (consumo de **20.83 ms** en Composición) fuerza la subcomposición diferida durante la fase de layout/medición para calcular restricciones de pantalla (`maxWidth`, `maxHeight`).
     - `ScaffoldLayout` (consumo de **10.93 ms** en Composición) utiliza a su vez `SubcomposeLayout` internamente para medir y sustraer insets y paddings.
     - En consecuencia, de los ~85 ms de `AndroidOwner:onMeasure`, al menos **~31.7 ms** corresponden directamente a subcomposición y composición sincronizada de hijos dentro del pase de medición.

2. **Precomposición de ViewTwo en HorizontalPager:**
   - A pesar de que la vista inicial al abrir el launcher es View 1 (`ViewOne`), `ViewTwo` se compone de inmediato en el arranque inicial consumiendo **9.49 ms**, debido al comportamiento por defecto de `HorizontalPager` / `PagerLazyLayoutItemProvider` que precarga la página adyacente para permitir scrolling continuo.

3. **Multiplicación de Overhead por Iconografía (`ShortcutIcon`):**
   - `ShortcutIcon` se ejecuta 36 veces durante el primer traversal, acumulando **18.65 ms**.
   - Cada icono realiza comprobaciones de tema, lookup de `customIconUri`, e inflado de `Icon` / `Image`.

---

## 6. Interpretación Técnica de Resultados

Las observaciones obtenidas se clasifican de acuerdo a su nivel de certeza empírica:

### 6.1 [MEDIDO] — Hallazgos Cuantitativos Confirmados
1. **Mediana del Cold Start en variante `benchmark`:** **631.0 ms** de TotalTime (`am start -W`). (Rango: 602 — 713 ms).
2. **Coste real de `loadState()`:** En la variante optimizada con R8, `loadState()` toma únicamente **10.06 ms** (GSON: 5.74 ms, Widget Validation: 1.60 ms, SharedPreferences read: 0.03 ms). Por lo tanto, `loadState()` explica únicamente el **~1.6%** del Cold Start total.
3. **Coste de `MainActivity.onCreate()`:** **39.37 ms** de mediana (con `VIEWMODEL-ACCESS` en 18.18 ms).
4. **Coste del primer Frame Traversal:** **201.22 ms** de mediana, donde `AndroidOwner:onMeasure` domina con **85.09 ms**.
5. **Top Hotspots Composable Identificados:**
   - Subcomposición de `BoxWithConstraints`: **20.83 ms**
   - Composición acumulada de `ShortcutIcon` (36 llamadas): **18.65 ms**
   - Composición de `ViewOne`: **13.11 ms**
   - Subcomposición de `ScaffoldLayout`: **10.93 ms**
   - `NavHost`: **10.34 ms**
   - Precomposición de `ViewTwo`: **9.49 ms**
   - `AnimatedContent`: **6.75 ms**
   - `LauncherScreen`: **5.59 ms**
   - `DynamicBackground`: **3.35 ms** (muy ligero, no es bottleneck)
   - `AnimeLauncherTheme`: **3.41 ms** (muy ligero, no es bottleneck)

### 6.2 [HIPÓTESIS] — Deducciones Técnicas Respaldadas por Datos
1. **Hipótesis de SubcomposeLayout en Cold Start:** El elevado tiempo de `AndroidOwner:onMeasure` (85.09 ms) se debe a que `BoxWithConstraints` fuerza que Compose detenga la medición para recomponer sus lambdas con las restricciones obtenidas. Eliminar `BoxWithConstraints` en favor de `Modifier.fillMaxSize()` o `LocalConfiguration.current` reduciría sustancialmente el coste de `onMeasure`.
2. **Hipótesis de Laziness de ViewTwo:** Si `ViewTwo` no se precusiera durante el primer frame de arranque (o si su inicialización se postergara al primer scroll), se liberarían ~9.5 ms de composición y ~15 ms de traversal en el arranque inicial.
3. **Hipótesis de optimización de ShortcutIcon:** La simplificación o memoización de nodos de `ShortcutIcon` tendría un impacto multiplicador notable al ejecutarse decenas de veces en cada frame.

### 6.3 [NO DETERMINABLE] — Factores Fuera del Alcance de Diagnóstico Actual
1. **Tiempo de IPC de SurfaceFlinger / WMS:** Los ~150-200 ms restantes del Cold Start entre el final de `performResume` y la presentación del buffer en pantalla involucran colas de IPC del sistema, sincronización con el compositor de hardware de Qualcomm y SurfaceFlinger, los cuales son propios del sistema operativo Android y no pertenecen al espacio de usuario de la app.

---

## 7. Candidatas a Optimización para Fases Futuras

> [!NOTE]
> Estas son únicamente hipótesis técnicas de optimización formuladas a partir de la evidencia de los traces. **En esta fase NO se modifica código de producción**.

1. **Sustitución de `BoxWithConstraints` en `ViewOne` y `ViewTwo`:**
   - Sustituir `BoxWithConstraints` por layout modifiers directos (`fillMaxSize()`, `weight()`, o cálculo simple de dimensiones) evitaría el salto de subcomposición durante `onMeasure`.
2. **Diferir la carga / composición de `ViewTwo`:**
   - Configurar `beyondBoundsPageCount = 0` o carga diferida de la página 2 para no competir con el primer frame de View 1.
3. **Optimización granular de `ShortcutIcon`:**
   - Estabilizar parámetros y minimizar lambdas anónimas para evitar recomposiciones innecesarias.
4. **Optimización de Navigation Graph:**
   - Evitar transiciones animadas pesadas en el nodo raíz de arranque.
