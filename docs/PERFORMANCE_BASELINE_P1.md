# NovaLauncher — Performance Engineering
## Baseline Cuantitativa — Fase P1

Documento oficial de baseline de rendimiento obtenido mediante instrumentación empírica con Jetpack Macrobenchmark sobre hardware físico real.

---

### 1. Environment

- **Dispositivo Físico:** POCO X6 5G (`23122PCD1G`, codename `garnet` / `garnetp_eea`)
- **Sistema Operativo:** Android 16 (API 36, build `BP2A.250605.031.A3 / OS3.0.302.0.WNREUXM`, user/release-keys)
- **SoC / CPU:** Qualcomm Snapdragon 7s Gen 2 (8 cores: 4x 2.40 GHz Cortex-A78 + 4x 1.95 GHz Cortex-A55, `cpuLocked: false`)
- **Memoria RAM:** 12 GB LPDDR4X (11,827,490,816 bytes detectados por Benchmark Context, ~4.7 GB disponible durante la prueba)
- **Pantalla / Refresh Rate:** 1220 x 2712 px @ 120.0 Hz (periodo de frame: 8.33 ms)
- **Modo de Compilación / Build:** `app-benchmark.apk` (derivada de `release`: R8 minification habilitada, resource shrinking habilitado, firma debug para instrumentación, `<profileable android:shell="true" />`, `compilationMode: run-from-apk` / sin Baseline Profile preinstalado)
- **Framework de Medición:**
  - `androidx.benchmark:benchmark-macro-junit4:1.3.3`
  - `androidx.test.uiautomator:uiautomator:2.3.0`
  - `androidx.test:runner:1.6.2`
  - `androidx.profileinstaller:profileinstaller:1.4.1`
- **Condiciones de Ejecución:**
  - Dispositivo conectado vía ADB por cable USB
  - Batería > 70%, sin carga rápida activa para mitigar estrangulamiento térmico
  - Frecuencias térmicas normales (sin modificación de gobernadores, `sustainedPerformanceModeEnabled: false`)
  - Configuración del launcher: Launcher por defecto del sistema
  - Target package: `com.daybreak.animelauncher` (Activity: `com.daybreak.animelauncher.MainActivity`)

---

### 2. Startup Benchmark

Métricas de tiempo de arranque capturadas mediante `StartupTimingMetric()` con 5 iteraciones por modo.

#### P1-STARTUP-COLD
- **Definición:** `StartupMode.COLD` (el proceso de la app se destruye por completo entre iteraciones; navegación forzada a background/settings previa para garantizar ciclo completo de creación de proceso y Activity).
- **Iteraciones individuales (TTID):**
  - Iteración 0: 978 ms
  - Iteración 1: 537 ms
  - Iteración 2: 745 ms
  - Iteración 3: 685 ms
  - Iteración 4: 739 ms
- **Resumen Estadístico (TTID):**
  - **Mínimo:** 537.0 ms
  - **Mediana:** 739.0 ms
  - **Máximo:** 978.0 ms
  - **Media:** 736.8 ms
- **TTFD (Time To Full Display):**
  - *Estado:* No señalizado por la aplicación.
  - *Causa técnica:* El código de producción actual no invoca `reportFullyDrawn()` ni utiliza la API `androidx.activity.compose.ReportDrawn`. En consecuencia, el framework de Macrobenchmark reporta el mismo valor que TTID o marca TTFD como no implementado. Conforme a las reglas de P1, no se sustituye por estimación ni se altera el código de producción.

#### P1-STARTUP-WARM
- **Definición:** `StartupMode.WARM` (el proceso de la app permanece en memoria en background; se destruye y recrea la Activity de entrada).
- **Iteraciones individuales (TTID):**
  - Iteración 0: 170 ms
  - Iteración 1: 120 ms
  - Iteración 2: 133 ms
  - Iteración 3: 158 ms
  - Iteración 4: 139 ms
- **Resumen Estadístico (TTID):**
  - **Mínimo:** 120.0 ms
  - **Mediana:** 139.0 ms
  - **Máximo:** 170.0 ms
  - **Media:** 144.0 ms
- **TTFD:** No señalizado en código actual (equivalente a TTID).

---

### 3. Frame Benchmark — App Drawer (P1-DRAWER-SCROLL)

Medición de fluidez durante el desplazamiento rápido del App Drawer con dataset real de aplicaciones instaladas, capturado con `FrameTimingMetric()` a lo largo de 5 iteraciones consecutivas. En pantalla de 120 Hz, el presupuesto objetivo de frame es de **8.33 ms**.

- **Total de frames analizados por iteración:**
  - Iteración 0: 43 frames
  - Iteración 1: 56 frames
  - Iteración 2: 45 frames
  - Iteración 3: 55 frames
  - Iteración 4: 41 frames
  - *Resumen frameCount:* Mínimo: 41.0 | Mediana: 45.0 | Máximo: 56.0

- **Métricas de CPU por Frame (`frameDurationCpuMs`):**
  - **P50 (Percentil 50):** 5.9 ms
  - **P90 (Percentil 90):** 11.4 ms
  - **P95 (Percentil 95):** 12.9 ms
  - **P99 (Percentil 99):** 30.0 ms

- **Métricas de Overrun por Frame (`frameOverrunMs`):**
  *(Valores positivos indican tiempo excedido respecto al deadline de refresco de 8.33 ms de la pantalla).*
  - **P50 (Percentil 50):** -0.04 ms
  - **P90 (Percentil 90):** 9.75 ms
  - **P95 (Percentil 95):** 10.35 ms
  - **P99 (Percentil 99):** 24.62 ms

- **Jank y Frames Perdidos:**
  - En P50, el tiempo de CPU (5.9 ms) permanece dentro de la ventana de refresco de 120 Hz (< 8.33 ms), reflejando un overrun mediano neutro (-0.04 ms).
  - A partir del P90 (11.4 ms) y P95 (12.9 ms), la duración supera el umbral de 8.33 ms por un margen de ~9.7 a 10.4 ms, lo que equivale a la pérdida de al menos 1 refresco completo de 120 Hz (jank de 1 frame).
  - En el extremo P99 (30.0 ms CPU, 24.6 ms overrun), la duración excede el presupuesto de varios ciclos de refresco consecutivos (saltos de 2 a 3 frames).

---

### 4. Frame Benchmark — Search en Drawer (P1-DRAWER-SEARCH)

Medición de fluidez interactiva durante la introducción de texto secuencial (cadena de 10 caracteres a velocidad humana tipificada) en el campo de búsqueda del Drawer, evaluando la carga de frames mientras se actualiza el estado y se recalculan los resultados. Capturado con `FrameTimingMetric()` en 5 iteraciones consecutivas.

- **Total de frames analizados por iteración:**
  - Iteración 0: 73 frames
  - Iteración 1: 83 frames
  - Iteración 2: 90 frames
  - Iteración 3: 86 frames
  - Iteración 4: 86 frames
  - *Resumen frameCount:* Mínimo: 73.0 | Mediana: 86.0 | Máximo: 90.0

- **Métricas de CPU por Frame (`frameDurationCpuMs`):**
  - **P50 (Percentil 50):** 8.2 ms
  - **P90 (Percentil 90):** 20.9 ms
  - **P95 (Percentil 95):** 31.6 ms
  - **P99 (Percentil 99):** 66.6 ms
  - *(Pico máximo individual observado en traza: 218.3 ms)*

- **Métricas de Overrun por Frame (`frameOverrunMs`):**
  - **P50 (Percentil 50):** 2.5 ms
  - **P90 (Percentil 90):** 23.9 ms
  - **P95 (Percentil 95):** 41.7 ms
  - **P99 (Percentil 99):** 112.5 ms

- **Jank y Fluidez Interactiva:**
  - En P50 (8.2 ms de CPU y +2.5 ms de overrun), la interacción ya sobrepasa ligeramente el presupuesto ideal de 8.33 ms para 120 Hz.
  - En P90 (20.9 ms CPU, 23.9 ms overrun) y P95 (31.6 ms CPU, 41.7 ms overrun), la acumulación de overrun genera demoras visibles en la renderización de los resultados de filtrado.
  - En P99 (66.6 ms CPU, 112.5 ms overrun) y con picos de más de 200 ms en trazas aisladas, se registran bloqueos notables de múltiples frames durante la escritura continua.

---

### 5. Technical Interpretation & Observations

*Nota metodológica: Conforme al protocolo de la Fase P1, no se declaran causas definitivas ni se ejecutan optimizaciones anticipadas. Se registran exclusivamente las mediciones empíricas, su variabilidad y el comportamiento observado.*

1. **Variabilidad en Cold Startup:**
   - La primera ejecución en frío registró 978 ms, mientras que la subsiguiente descendió a 537 ms, estabilizándose la mediana en 739 ms.
   - Esta diferencia entre primera ejecución y subsecuentes es típica del comportamiento de carga de I/O en disco (lectura de SharedPreferences y parseo JSON) y del estado de cachés de clases en ART sin optimización AOT/Baseline Profile previa.
2. **Comparativa Cold vs Warm:**
   - El arranque tibio/caliente (mediana de 139 ms) es un 81.2% más rápido que el arranque en frío (mediana de 739 ms), confirmando que el costo principal de inicio reside en las etapas de inicialización de proceso, carga de datos iniciales y primer inflado de Compose.
3. **Comportamiento en Scroll del Drawer:**
   - La mayor parte de los frames durante el scroll sostenido opera cerca de los 5.9 ms de CPU (satisfaciendo la tasa de refresco). La degradación a 11-13 ms (P90-P95) y picos de 30 ms (P99) coincide puntualmente con el reciclaje y primera carga de iconos de aplicaciones que entran al viewport.
4. **Comportamiento en Search del Drawer:**
   - El proceso de búsqueda exhibe un costo computacional sensiblemente mayor que el scroll pasivo, elevando el P90 de CPU a 20.9 ms y el P99 a 66.6 ms.
   - Se observa una correlación directa entre cada pulsación de tecla y un incremento transitorio en la duración de frame en el hilo principal.
5. **Estado de Baseline Profiles:**
   - Al momento de esta baseline cuantitativa, la aplicación no cuenta con reglas de compilación Baseline Profile preempaquetadas (`compilationMode: run-from-apk`). Todo el código de Compose, serialización y layout opera bajo modo JIT/interpretado inicial en las primeras iteraciones.

---

### 6. P2-01 SEARCH — BEFORE / AFTER

Resultados de la optimización quirúrgica del Search del App Drawer (separación de preparación de datos de categoría estables y filtrado lineal O(N) sin re-agrupamiento, re-ordenamiento ni re-normalización NFD por tecla).

#### Entorno y Condiciones de Ejecución
- **Dispositivo:** POCO X6 5G (`23122PCD1G`, garnet)
- **Sistema Operativo:** Android 16 (API 36)
- **Tasa de Refresco:** 1220 x 2712 @ 120.0 Hz (deadline de frame: 8.33 ms)
- **Modo de Compilación:** `app-benchmark.apk` (R8 minificado, shrinking habilitado, `run-from-apk`)
- **Benchmark:** `DrawerFrameBenchmark#drawerSearch` (5 iteraciones, 10 caracteres secuenciales `"calculator"`)

#### Tabla Comparativa de Rendimiento Empírico

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
| **`frameCount` (mediana)** | 86.0 | **81.0** | -5.0 | -5.8% |

#### Interpretación Técnica de los Resultados
1. **Presupuesto de 120 Hz en P50:** La mediana de duración de CPU por frame descendió a **5.2 ms**, situándose holgadamente por debajo del umbral crítico de 8.33 ms necesario para 120 Hz ininterrumpidos en interacción continua.
2. **Reducción de Picos Críticos (P99):** El P99 de tiempo de CPU se redujo prácticamente a la mitad (**-48.3%**, de 66.6 ms a 34.4 ms), eliminando el cuello de botella severo provocado por la reconstrucción de estructuras y llamadas a `Normalizer.normalize` por cada carácter.
3. **Desplome del Frame Overrun:** El overrun de frame en percentiles altos se redujo en más de un **59% en P95** y un **67.7% en P99** (de 112.5 ms a 36.3 ms), lo que mitiga drásticamente la congelación perceptible durante la escritura continua.
4. **Integridad de Comportamiento:** Las 62 pruebas unitarias automatizadas (`DrawerSearchPerformanceUnitTest`) y la validación en pantalla confirman que el orden alfabético, los headers de sección, la indexación del `AlphabetIndexRail` y el filtrado por categoría son 100% idénticos y equivalentes al comportamiento previo.

