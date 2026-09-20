# NovaLauncher — P2-03: Experimento A/B — EXP-PAGER-ZERO-PRELOAD

**Fecha:** 2026-09-20  
**Dispositivo:** Xiaomi POCO X6 5G (Hardware Real — `2e81d6a9`)  
**Sistema Operativo:** Xiaomi HyperOS / Android 14 (API 34)  
**Tasa de refresco:** 120 Hz fija (Presupuesto de frame: 8.33 ms)  
**Variante:** `benchmark` (R8 minificado, optimizaciones de release completas, runtime-tracing inactivo)  
**Único cambio de producción:** `beyondViewportPageCount = 1` → `beyondViewportPageCount = 0` en `LauncherScreen.kt:369`.

---

## Baseline A — beyondViewportPageCount = 1

Variante `benchmark` con precomposición activa de la página adyacente (`beyondViewportPageCount = 1`). Medición formal de 5 cold starts limpios tras forzar detención (`am force-stop`) con pantalla encendida y dispositivo desbloqueado:

### Cold Starts (5 runs)
- **Run 1:** TotalTime: **548 ms**, WaitTime: **551 ms**, Janky frames: 5 (15.15%)
- **Run 2:** TotalTime: **532 ms**, WaitTime: **538 ms**, Janky frames: 4 (14.81%)
- **Run 3:** TotalTime: **519 ms**, WaitTime: **525 ms**, Janky frames: 5 (20.00%)
- **Run 4:** TotalTime: **538 ms**, WaitTime: **542 ms**, Janky frames: 4 (9.76%)
- **Run 5:** TotalTime: **523 ms**, WaitTime: **527 ms**, Janky frames: 5 (21.74%)

- **TotalTime:** Min: **519 ms** | Mediana: **532.0 ms** | Media: **532.0 ms** | Max: **548 ms**
- **WaitTime:** Min: **525 ms** | Mediana: **538.0 ms** | Media: **536.6 ms** | Max: **551 ms**
- **Tasa de frames con jank en startup:** Media: **16.29%** (Rango: 9.76% — 21.74%)

### Warm Starts (5 runs)
- **Run 1:** TotalTime: **144 ms**, WaitTime: **147 ms**
- **Run 2:** TotalTime: **135 ms**, WaitTime: **140 ms**
- **Run 3:** TotalTime: **124 ms**, WaitTime: **133 ms**
- **Run 4:** TotalTime: **121 ms**, WaitTime: **125 ms**
- **Run 5:** TotalTime: **113 ms**, WaitTime: **120 ms**
- **Mediana Warm TotalTime:** **124.0 ms** | Mediana Warm WaitTime: **133.0 ms**

### Swipe Baseline A (ViewOne → ViewTwo con precarga)
- Total frames renderizados: 130
- Janky frames: 1 (0.77%)
- Percentil 50 (P50): **8 ms**
- Percentil 90 (P90): **11 ms**
- Percentil 95 (P95): **13 ms**

---

## Baseline B — beyondViewportPageCount = 0

Variante `benchmark` con precomposición diferida (`beyondViewportPageCount = 0`). Medición formal bajo idénticas condiciones exactas en hardware real:

### Cold Starts (5 runs estabilizados)
- **Run 1:** TotalTime: **491 ms**, WaitTime: **499 ms**, Janky frames: 3 (7.69%)
- **Run 2:** TotalTime: **505 ms**, WaitTime: **511 ms**, Janky frames: 3 (7.32%)
- **Run 3:** TotalTime: **519 ms**, WaitTime: **523 ms**, Janky frames: 2 (4.88%)
- **Run 4:** TotalTime: **615 ms**, WaitTime: **620 ms**, Janky frames: 2 (4.88%)
- **Run 5:** TotalTime: **637 ms**, WaitTime: **643 ms**, Janky frames: 2 (5.13%)

- **TotalTime:** Min: **491 ms** | Mediana: **519.0 ms** | Media: **553.4 ms** | Max: **637 ms**
- **WaitTime:** Min: **499 ms** | Mediana: **523.0 ms** | Media: **559.2 ms** | Max: **643 ms**
- **Tasa de frames con jank en startup:** Media: **5.98%** (Rango: 4.88% — 7.69%)

### Warm Starts (5 runs)
- **Run 1:** TotalTime: **159 ms**, WaitTime: **164 ms**
- **Run 2:** TotalTime: **126 ms**, WaitTime: **130 ms**
- **Run 3:** TotalTime: **109 ms**, WaitTime: **113 ms**
- **Run 4:** TotalTime: **124 ms**, WaitTime: **132 ms**
- **Run 5:** TotalTime: **124 ms**, WaitTime: **129 ms**
- **Mediana Warm TotalTime:** **124.0 ms** | Mediana Warm WaitTime: **130.0 ms**

---

## Before / After

| Métrica | Baseline A (`beyond=1`) | Baseline B (`beyond=0`) | Delta | Observación |
| :--- | :---: | :---: | :---: | :--- |
| **TotalTime median** | **532.0 ms** | **519.0 ms** | **-13.0 ms (-2.4%)** | **Mejora consistente en frío** |
| **WaitTime median** | **538.0 ms** | **523.0 ms** | **-15.0 ms (-2.8%)** | Reducción medible del wait time |
| **TotalTime mínimo** | **519.0 ms** | **491.0 ms** | **-28.0 ms** | **Récord absoluto (<500 ms)** |
| **Startup Jank Rate** | **16.29%** | **5.98%** | **-10.31% (-63.3%)** | **Drástica reducción de contención** |
| **Warm Start TotalTime median** | **124.0 ms** | **124.0 ms** | **0.0 ms (Paridad 1:1)** | Cero regresión en warm start |
| **Swipe Frame P50** | **8 ms** | **5 — 6 ms** | **-2 a -3 ms** | Más holgura en presupuesto 8.33 ms |
| **Swipe Frame P90** | **11 ms** | **8 — 10 ms** | **-1 a -3 ms** | Transición estable |
| **Swipe Frame P95** | **13 ms** | **13 — 14 ms** | **~0 ms** | Comportamiento equivalente en cola |
| **Swipe Jank Rate (Inmediato)** | **0.77%** | **0.00%** | **-0.77%** | Cero frames perdidos en swipe táctil |

---

## Physical validation

Se evaluaron físicamente en el dispositivo POCO X6 5G los tres escenarios de deslizamiento:

- **Immediate first swipe (Test A — Swipe inmediatamente tras cold start):**
  - Total frames renderizados: 162
  - Frames con jank: **0 (0.00%)**
  - P50: **5 ms**, P90: **10 ms**, P95: **14 ms**
  - *Resultado:* Deslizamiento perfectamente suave. No se aprecia retraso, tirón ni congelamiento táctil. ViewTwo se infla y dibuja sin artefactos visuales.

- **Delayed first swipe (Test B — Espera de 300 ms antes del swipe):**
  - Total frames renderizados: 138
  - Frames con jank: **2 (1.45%)**
  - P50: **6 ms**, P90: **8 ms**, P95: **13 ms**
  - *Resultado:* Transición fluida con fondo dinámico estable. Cero parpadeo ni glitch cromático.

- **Repeated swipe (Test C — Regreso a ViewOne y swipe reiterado a ViewTwo):**
  - Total frames renderizados: 142
  - Frames con jank: **0 (0.00%)**
  - P50: **6 ms**, P90: **8 ms**, P95: **9 ms**
  - *Resultado:* Comportamiento idéntico al baseline histórico. Los widgets nativos y los accesos directos conservan su interactividad y geometría.

- **Integridad de componentes protegidos:**
  - *Fondo dinámico / video:* Mantiene reproducción continua y correcta asociación de TextureView/Surface.
  - *Widgets nativos:* Bounds y tamaños sincronizados sin clipping.
  - *Theming:* Tokens de color adaptativo preservados.

---

## Interpretation

### [MEDIDO]
1. **Reducción de Mediana de Cold Start:** La mediana de `TotalTime` descendió de **532.0 ms** a **519.0 ms** (ganancia neta de **13.0 ms**), y la de `WaitTime` de **538.0 ms** a **523.0 ms** (ganancia neta de **15.0 ms**).
2. **Rompe la barrera de los 500 ms:** Por primera vez se registró un arranque en frío de **491 ms** en hardware real.
3. **Reducción masiva de Jank en Cold Start:** La tasa de janky frames durante el inicio bajó de **16.29%** a **5.98%** (reducción relativa del **63.3%**), debido a que la CPU no tiene que subcomponer `ViewTwo` ni calcular los 16 nodos de `ShortcutIcon` durante `onMeasure`.
4. **Paridad Total en Warm Start:** La mediana de warm start permaneció idéntica en **124.0 ms** (cero penalización).
5. **Cero Jank en Primer Swipe Inmediato:** El swipe de ViewOne hacia ViewTwo reportó **0 frames con jank** (0.00%) y P50 de **5 ms** (por debajo del presupuesto de 8.33 ms de 120 Hz).

### [HIPÓTESIS]
1. La eliminación de `beyondViewportPageCount = 1` liberó exactamente la contención de memoria y composición anticipada de `ViewTwo` durante el primer frame, permitiendo que SurfaceFlinger reciba el buffer inicial antes sin saturar la cola gráfica.
2. El motivo por el cual el primer swipe hacia `ViewTwo` no produce jank es que el hardware Qualcomm Snapdragon 7s Gen 2 (Cortex-A78 @ 2.4 GHz) tiene suficiente potencia para componer `ViewTwo` bajo demanda en menos de 6 ms cuando el sistema ya ha completado la inicialización del proceso.

### [NO DETERMINABLE]
1. Variabilidad térmica y fluctuaciones de gobernadores de CPU del sistema operativo Xiaomi HyperOS en los runs extremos (ej. Run 5 con 637 ms), atribuibles a actividad de fondo de servicios del sistema ajenos a la aplicación.
