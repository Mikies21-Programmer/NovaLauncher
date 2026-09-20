# PERFORMANCE — P2-02A: Cold Start Trace (android.os.Trace / atrace)

## Environment

| Campo | Valor |
|---|---|
| Hardware | POCO X6 5G |
| Android | 14 |
| Refresh Rate | 120 Hz |
| Build Variant | **Debug** (minifyEnabled false, debuggable true) |
| Fecha | 2026-09-20 |
| Baseline TTID Release | mediana 739 ms · mín 537 ms · máx 978 ms |
| TotalTime Debug (3 runs) | 1338 ms · 1301 ms · 1358 ms |
| Atrace flags | `-a com.daybreak.animelauncher am view gfx wm res dalvik` |
| Traces descargados | `docs/performance_traces/P2-02A-official-run{1,2,3}.trace` |

> [!IMPORTANT]
> Los tiempos de Debug **NO son comparables** directamente contra la baseline release de 739 ms.
> El overhead de debug (JDWP, desoptimización JIT, class-loading diferido, dalvik trace) incrementa
> el cold start absoluto en ~500–600 ms en este dispositivo.
> Lo que sí es comparable entre los 3 runs: **la distribución proporcional** de cada sub-sección
> dentro de `LOAD_STATE_TOTAL`.

---

## Widget State

| Campo | Valor |
|---|---|
| Vistas configuradas | 2 |
| `nativeWidgetIds` Vista 1 | `[]` — 0 widgets |
| `nativeWidgetIds` Vista 2 | `[]` — 0 widgets |
| Total widgets a validar | **0** |

---

## Secciones Trace Instrumentadas

Las secciones de `android.os.Trace` insertadas en `LauncherViewModel.loadState()`:

| Sección | Descripción |
|---|---|
| `LOAD_STATE_TOTAL` | Bloque completo de `loadState()` de inicio a fin |
| `STATE_READ` | `prefs.getString("launcher_state", null)` — lectura de SharedPreferences |
| `GSON_STATE` | `gson.fromJson(json, LauncherState::class.java)` — deserialización del estado principal |
| `STYLE_READ` | `prefs.getString("launcher_style_config", null)` — lectura de estilo dedicado |
| `GSON_STYLE` | `gson.fromJson(styleJson, AdvancedStyleConfig::class.java)` — deserialización del estilo |
| `WIDGET_VALIDATION` | `widgetHostManager.validateAndCleanWidgets(...)` — IPC con AppWidgetManager |

---

## Run 1

**TotalTime (am start -W):** 1338 ms · PID: 17405

| Sección | B| (s) | E| (s) | Duración (ms) | % de LOAD_STATE_TOTAL |
|---|---|---|---|---|
| `LOAD_STATE_TOTAL` | 218805.614725 | 218805.666443 | **51.718** | 100% |
| `STATE_READ` | 218805.614736 | 218805.642439 | **27.703** | 53.6% |
| `GSON_STATE` | 218805.642537 | 218805.665535 | **22.998** | 44.5% |
| `STYLE_READ` | 218805.665564 | 218805.665597 | **0.033** | 0.1% |
| `GSON_STYLE` | 218805.665597 | 218805.666320 | **0.723** | 1.4% |
| `WIDGET_VALIDATION` | 218805.666339 | 218805.666439 | **0.100** | 0.2% |
| Gap no instrumentado | — | — | **0.161** | 0.3% |

**Observaciones Run 1:**
- `SharedPreferencesImpl awaitLoadedLocked` visible dentro de STATE_READ → prefs no estaban cargadas en RAM
- `GSON_STATE` incluye class-loading de LauncherState, AdvancedStyleConfig, ViewConfig, AppShortcut, DrawerConfig, GesturesConfig, etc.
- 0 IPC cross-process en WIDGET_VALIDATION (0 widgets configurados)

---

## Run 2

**TotalTime (am start -W):** 1301 ms · PID: 17620

| Sección | B| (s) | E| (s) | Duración (ms) | % de LOAD_STATE_TOTAL |
|---|---|---|---|---|
| `LOAD_STATE_TOTAL` | 218873.668330 | 218873.703814 | **35.484** | 100% |
| `STATE_READ` | 218873.668340 | 218873.687044 | **18.704** | 52.7% |
| `GSON_STATE` | 218873.687091 | 218873.703078 | **15.987** | 45.1% |
| `STYLE_READ` | 218873.703096 | 218873.703122 | **0.026** | 0.1% |
| `GSON_STYLE` | 218873.703122 | 218873.703733 | **0.611** | 1.7% |
| `WIDGET_VALIDATION` | 218873.703745 | 218873.703811 | **0.066** | 0.2% |
| Gap no instrumentado | — | — | **0.090** | 0.3% |

---

## Run 3

**TotalTime (am start -W):** 1358 ms · PID: 18079

| Sección | B| (s) | E| (s) | Duración (ms) | % de LOAD_STATE_TOTAL |
|---|---|---|---|---|
| `LOAD_STATE_TOTAL` | 218919.113884 | 218919.158371 | **44.487** | 100% |
| `STATE_READ` | 218919.113916 | 218919.131473 | **17.557** | 39.5% |
| `GSON_STATE` | 218919.131587 | 218919.156534 | **24.947** | 56.1% |
| `STYLE_READ` | 218919.156572 | 218919.156611 | **0.039** | 0.1% |
| `GSON_STYLE` | 218919.156611 | 218919.157402 | **0.791** | 1.8% |
| `WIDGET_VALIDATION` | 218919.158201 | 218919.158363 | **0.162** | 0.4% |
| Gap no instrumentado | — | — | **0.179** | 0.4% |

---

## Median Breakdown (3 runs)

| Segmento | Run 1 (ms) | Run 2 (ms) | Run 3 (ms) | Mín | Mediana | Máx |
|---|---:|---:|---:|---:|---:|---:|
| `STATE_READ` | 27.703 | 18.704 | 17.557 | 17.557 | **18.704** | 27.703 |
| `GSON_STATE` | 22.998 | 15.987 | 24.947 | 15.987 | **22.998** | 24.947 |
| `STYLE_READ` | 0.033 | 0.026 | 0.039 | 0.026 | **0.033** | 0.039 |
| `GSON_STYLE` | 0.723 | 0.611 | 0.791 | 0.611 | **0.723** | 0.791 |
| `WIDGET_VALIDATION` | 0.100 | 0.066 | 0.162 | 0.066 | **0.100** | 0.162 |
| `LOAD_STATE_TOTAL` | 51.718 | 35.484 | 44.487 | 35.484 | **44.487** | 51.718 |

**Mediana `LOAD_STATE_TOTAL`: 44.5 ms**

---

## Correlation with TTID

| Campo | Run 1 | Run 2 | Run 3 |
|---|---|---|---|
| `am start -W TotalTime` | 1338 ms | 1301 ms | 1358 ms |
| `LOAD_STATE_TOTAL` | 51.7 ms | 35.5 ms | 44.5 ms |
| `loadState` como % de TotalTime | **3.9%** | **2.7%** | **3.3%** |

**[MEDIDO]** `LOAD_STATE_TOTAL` representa entre el 2.7% y el 3.9% del TotalTime en build Debug.

**[HIPÓTESIS]** En el build Release (TotalTime ~739 ms), `LOAD_STATE_TOTAL` estimado sería proporcional al ratio de JIT/class-loading overhead de debug vs. release. Dado que los 44 ms se deben principalmente a I/O de SharedPreferences y deserialización Gson — operaciones independientes del modo de compilación — la estimación conservadora en Release sería: **20–40 ms de `LOAD_STATE_TOTAL`**.

**[NO DETERMINABLE]** El timestamp exacto de `setContent` y el primer frame renderizado no fueron capturados en este trace. Se necesitaría habilitación adicional de `view` trace con Compose slots para correlacionar.

---

## Interpretation

### Hotspot dominante: `STATE_READ` + `GSON_STATE`

La suma de ambos representa el **97.6–98.2% de `LOAD_STATE_TOTAL`** en los 3 runs.

**[MEDIDO]** `STATE_READ` (mediana 18.7 ms):
- El trace muestra explícitamente `SharedPreferencesImpl awaitLoadedLocked` como sub-slice dentro de STATE_READ.
- Esto confirma que SharedPreferences **no había completado su carga en background** cuando `loadState()` lo accede en el hilo principal.
- El `getString()` bloquea hasta que el `XmlBlock` del archivo XML es parseado desde disco.
- Esto es **I/O real de filesystem** en el hilo principal durante cold start.

**[MEDIDO]** `GSON_STATE` (mediana 23.0 ms):
- El trace muestra decenas de `B|…|Lcom/google/gson/…` y `B|…|Lcom/daybreak/animelauncher/…` class-loading slices.
- Esto confirma que Gson realiza class-loading diferido de sus TypeAdapters **durante la primera deserialización**.
- Una parte del coste (≈5-10 ms estimados) es class-loading de JVM que solo ocurre en el primer cold start real.
- En warm starts subsecuentes, `GSON_STATE` se reduciría significativamente.

**[MEDIDO]** `STYLE_READ` (mediana 0.033 ms):
- **No es un hotspot.** La segunda llamada a SharedPreferences es ~600x más rápida que la primera porque el mapa ya está cargado en RAM.

**[MEDIDO]** `GSON_STYLE` (mediana 0.723 ms):
- **No es un hotspot.** `AdvancedStyleConfig` es una clase mucho más pequeña que `LauncherState`. El class-loading ya ocurrió durante GSON_STATE.

**[MEDIDO]** `WIDGET_VALIDATION` (mediana 0.100 ms):
- **No es un hotspot.** Con 0 widgets configurados, no hay IPC cross-process con `AppWidgetManager`. El tiempo es overhead de Kotlin iteration sobre una lista vacía.

---

## Analysis of I/O

**[MEDIDO]** Dentro de `STATE_READ`:
- `SharedPreferencesImpl awaitLoadedLocked` es una señal explícita de que el SharedPreferences XML estaba siendo cargado desde disco cuando `loadState()` lo necesitó.
- El intervalo de 18-27 ms incluye la latencia de lectura del archivo XML + parseo DOM + conversión a Java Map.
- **Nota:** SharedPreferences en Android no usa `mmap`, hace `FileInputStream` sincrónico en el primer acceso.
- El coste **es real I/O + parsing**, no overhead de acceso de memoria.

---

## Analysis of Widget IPC

**[MEDIDO]** `WIDGET_VALIDATION` con 0 widgets:
- Duración: 0.066–0.162 ms
- Sin llamadas Binder observadas en el trace para este PID en esta ventana temporal
- **Conclusión:** WIDGET_VALIDATION es O(n_widgets) con overhead casi nulo cuando n=0
- Con widgets reales, el coste crecería con IPC cross-process a `AppWidgetManager`

**[HIPÓTESIS]** Si el usuario añadiera ≥1 widget, se esperaría un pico de 10–50 ms por widget en IPC binder durante WIDGET_VALIDATION. Esto requeriría validación separada.

---

## Limitations

1. **Build Debug vs. Release:** Los TotalTime (1301–1358 ms debug vs. 739 ms release) no son comparables directamente. Los tiempos dentro de `LOAD_STATE_TOTAL` tienen overhead de debug (~30% estimado por JIT deoptimization + class-loading diferido).

2. **Primer cold start real:** Los 3 runs aquí son cold starts con `force-stop` pero no cold starts de boot frío (sin page-cache caliente). El page-cache puede reducir el I/O visible de SharedPreferences.

3. **`setContent` y primer frame:** No capturados. Se necesitaría un trace separado con Compose tracing para correlacionar `LOAD_STATE_TOTAL` con el pipeline de composición.

4. **Varianza alta en STATE_READ:** 17.5–27.7 ms (rango de 10 ms). Esto sugiere que la latencia de SharedPreferences `awaitLoadedLocked` varía con la carga del sistema en el momento del cold start.

5. **WIDGET_VALIDATION sin widgets:** El análisis asume 0 widgets. Con widgets reales, WIDGET_VALIDATION podría convertirse en un hotspot significativo.

---

## Candidate for P2-02B

Basado en los datos medidos:

### Candidato #1 — [MEDIDO] `STATE_READ` → SharedPreferences
**Problema:** `getString("launcher_state")` bloquea el hilo principal ~18-27 ms esperando el cargador de SharedPreferences.
**Solución candidata P2-02B:** Mover `loadState()` a un coroutine en `Dispatchers.IO` y exponer el estado como `StateFlow` inicialmente vacío, llenándolo async antes del primer frame.

### Candidato #2 — [MEDIDO] `GSON_STATE` → deserialización síncrona
**Problema:** `gson.fromJson(LauncherState)` tarda ~16-25 ms con class-loading.
**Solución candidata P2-02C:** Migrar a `kotlinx.serialization` (codegen sin reflection) o diferir a IO si se adopta la estrategia async de P2-02B.

### Candidato #3 — [HIPÓTESIS] `WIDGET_VALIDATION` con widgets reales
**Hipótesis:** Con ≥1 widget, el IPC podría dominar `LOAD_STATE_TOTAL`.
**Solución candidata:** Diferir `validateAndCleanWidgets()` a un coroutine post-composición.

---

## Próxima Fase: P2-02B

**Decisión de dirección:** `STATE_READ` es el hotspot confirmado de I/O bloqueante en el hilo principal.

**P2-02B propuesta:** Instrumentar y planear la migración de `loadState()` a coroutine en `Dispatchers.IO`, con estado inicial por defecto, rellenado asíncronamente antes del primer frame interactivo.

---

## Condiciones de Medición

- 3 cold starts reales con `am force-stop` previo
- Pantalla encendida, dispositivo desbloqueado
- Sin otras apps pesadas activas durante la captura
- Build: Debug (`minifyEnabled false`)
- `atrace -a com.daybreak.animelauncher am view gfx wm res dalvik`
- Buffer: 32768 KB
- Timestamps extraídos directamente de `tracing_mark_write` en formato ftrace


