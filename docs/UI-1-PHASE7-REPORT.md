# UI-1 FASE 7 — Indicador de RAM global en el sidebar

**Fecha:** 2026-10-09 / 2026-10-10 · **Dispositivo:** POCO X6 5G (HyperOS, Android 14+) · **Build validada:** app-debug.apk 2026-10-09 22:30 (misma fuente que la build actual, `compileDebugKotlin UP-TO-DATE` en gates)

**Veredicto: NOT READY (pendiente de confirmación física del desarrollador — ver §7.3).** Todos los gates automatizados en verde y batería objetiva en dispositivo completada; el bloqueo es exclusivamente el protocolo de validación física (los PASS manuales solo puede confirmarlos el desarrollador).

---

## 1. API y modelo de datos

Fuente: **`ActivityManager.MemoryInfo`** (API estándar). `/proc/meminfo` queda **descartado** como fuente. Campos usados: `totalMem`, `availMem`, `lowMemory`. No se lee PSS de NovaLauncher: la cifra es la RAM **global del teléfono**.

Nueva capa desacoplada de Compose en `app/src/main/java/com/daybreak/animelauncher/ram/RamMonitor.kt`:

| Pieza | Rol |
|---|---|
| `RamSnapshot(totalBytes, availBytes, lowMemory)` | data class puro: cálculo, validación y porcentaje sin Android/Compose. `from(MemoryInfo)` como único punto de entrada del framework. |
| `RamProvider` | `fun interface` (fuente de snapshots). `null` = error transitorio del proveedor (no lanza). |
| `ActivityManagerRamProvider` | Implementación real. Recibe **Application context** (`.applicationContext`, nunca retiene Activity). Lectura en `Dispatchers.Default` explícito (`getMemoryInfo` es llamada binder), `try/catch` → `null`. |
| `RamFormat` | Formateo puro (GB, líneas es/en, advertencia lowMemory, mapeo de tokens → colores). |
| `pollRam(provider, tick, onUpdate)` | Loop de polling suspendible, estructurado y cancelable, tick inyectable (testeable sin relojes). |

## 2. Fórmula del cálculo

```
usedApprox = max(totalMem − availMem, 0)          // coerceAtLeast(0): nunca negativa
usedFraction = usedApprox / total                 // acotada [0,1]; snapshot inválido → 0
usedPercent  = floor(usedFraction × 100)          // acotada [0,100]
valid        = total > 0 && 0 ≤ avail ≤ total     // protege contra basura del proveedor
```

La cifra es **aproximada por diseño**: `availMem` incluye memoria recuperable por el sistema; el widget nunca etiqueta "disponible" como memoria completamente libre ni "usada" como ocupación estricta (documentado en el KDoc de `RamSnapshot`).

## 3. Formato visual

Componente reutilizable: `app/src/main/java/com/daybreak/animelauncher/ui/components/RealTimeRam.kt`.

- **Recibe:** snapshot (estado), `LauncherThemeTokens` (tokens de color), `language`, `pollActive` (visibilidad), `pollIntervalMs`. Presentación pura separada (`RealTimeRamContent`) para composición sin polling.
- **Tres líneas compactas centradas, sin contenedor nuevo:**
  - `RAM` — 8 sp, `textSecondary`, tracking 2 sp
  - `6.8 / 11.2 GB` — 10 sp (piso 7 sp), `textPrimary`, Medium
  - `4.4 GB disponibles` / `4.4 GB available` / `RAM baja` (`Low RAM`) — 8 sp (piso 6 sp), `textSecondary`, Thin. Con `lowMemory` del sistema activo la tercera línea pasa a la advertencia discreta; **un % alto por sí solo nunca se representa como peligro**.
- **Colores:** solo `textPrimaryColor`/`textSecondaryColor` de los tokens existentes de la página (AUTO: tokens adaptativos; MANUAL: `fromAdvancedStyleConfig`, blanco/`#CCCCCC` históricos). **No se creó ningún motor de color nuevo** y no se tocó el Glass Engine (solo consumo de tokens).
- **Anti-salto de anchura:** `Locale.US` fijo (punto decimal), 1 decimal siempre, sin animación de cifras; `maxLines=1`, `softWrap=false` y encogimiento progresivo (`AutoShrinkRamLine`, mismo patrón que `LogoAutoText` de Fase 6) con piso de legibilidad — nunca wrap ni desborde del sidebar.
- Unidades: 1 GB = 1024³ bytes (`BYTES_PER_GB`), etiqueta "GB".

## 4. Ciclo de vida del polling

- Intervalo: **5000 ms** (`RAM_POLL_INTERVAL_MS`) — nunca por frame.
- Gate de visibilidad cableado en `LauncherScreen.kt`: `ramPollActive = isAppForeground && pagerState.currentPage == page` → el polling **solo corre** con el launcher en foreground Y la página siendo la visible.
- Implementación: `LaunchedEffect(pollActive)` → key change = cancelación inmediata de la corutina en curso (sin ticks residuales) y relanzamiento con **lectura inmediata** al volver. Una sola instancia visible ⇒ un solo loop (el gate `currentPage == page` lo garantiza).
- Primera lectura inmediata; después exactamente **una lectura por tick** (cada update está precedido por un tick → imposible actualizar a 60/120 Hz; probado en test N).
- Cancelación al salir de RESUMED / página oculta: la corutina muere dentro del tick en curso (test R: cancelación a mitad de intervalo → 0 lecturas nuevas). Sin servicio persistente, sin CoroutineScope global, sin trabajo periódico en background.
- No se modificó `MemoryManager` ni `NovaApplication`.

## 5. Configuración

- `AdvancedStyleConfig.showRamWidget: Boolean = false` (**default retrocompatible**: la UI de usuarios existentes no cambia hasta activarlo).
- Normalización defensiva: `showRamWidget` es primitivo → Gson no puede dejarlo `null` (JSON antiguo/corrupto sin el campo → `false` del no-arg ctor); la ruta de carga (`LauncherViewModel`) mantiene el patrón de campos nullable→default de Fases 5-6.
- Control mostrar/ocultar añadido a `AdvancedSettingsScreen` (GlassCard + Switch, sección mínima; el rediseño de la sección Tema Adaptativo queda para la fase posterior de integración de Advanced Settings).
- Verificación Gson con pruebas reales: JSON antiguo sin campo → `false` y el resto de campos sobreviven (test I); round-trip idempotente (test J).

## 6. Tests

`app/src/test/java/com/daybreak/animelauncher/ram/RamMonitorTest.kt` — **16 tests, gates A–R** (Q incluido como loops independientes; puros JVM, sin Android framework ni relojes reales: proveedor falso contable + ticks por `Channel`):

| Gate | Test | Cubre |
|---|---|---|
| A | test01 | `usedApprox` (y avail=total → 0) |
| B | test02 | total/available válidos |
| C | test03 | límites: total 0/negativo, avail negativo, avail>total (usada nunca negativa, fracción 0) |
| D | test04 | porcentaje acotado 0–100 (inválido → 0) |
| E | test05 | formato es/en (`6.4 / 10.8 GB`, `disponibles`/`available`) |
| F | test06 | conversión/redondeo (1 decimal fijo, punto US, HALF_UP) |
| G | test07 | `lowMemory` → advertencia; % alto normal → línea de disponibles |
| H | test08 | snapshot nulo/inválido → `—` / `""` |
| I | test09 | config antigua sin `showRamWidget` → false, campos Fase 6 intactos |
| J | test10 | estado inicial show/hide + round-trip Gson |
| K/L | test11 | cancelación (página oculta / salida de RESUMED) → 0 lecturas nuevas |
| M | test12 | regreso → relanzamiento con lectura inmediata |
| N | test13 | 1 update por tick (imposible 60/120 Hz) + intervalo 5 s |
| O/P | test14 | tokens adaptativos del widget; MANUAL conserva blanco/`#CCCCCC` |
| Q | test15 | loops independientes: cancelar A no afecta a B |
| R | test16 | cancelación a mitad de tick → cero trabajo residual |

**Gate 1 — `:app:testDebugUnitTest`: PASS. 226/226 (0 fallos, 0 errores, 0 saltados)** = 210 previos + 16 nuevos. Log completo: `docs/assets/p7-gate1-testDebugUnitTest.log`.

## 7. Validación física

### 7.1 Gates (todos individuales, sin batería monolítica; logs completos en `docs/assets/`)

| Gate | Resultado |
|---|---|
| `:app:testDebugUnitTest` | **PASS** — 226/226 |
| `:app:assembleDebug` | **PASS** — APK debug 87 MB (fuente actual) |
| `:app:lintDebug` | **PASS** — 80 warnings + 6 hints **preexistentes** (baseline), **0 errores, 0 avisos en archivos nuevos `ram/` o `RealTimeRam`** |
| `:app:bundleRelease` | **PASS** — AAB firmado 64 MB |
| `git diff --check` | **Limpio** (solo avisos de normalización CRLF, sin errores de whitespace) |

### 7.2 Evidencia objetiva en dispositivo (agent-facilitated, 2026-10-09 por la sesión previa + 2026-10-10 en esta sesión)

Ejecutado hoy (2026-10-10) sobre el POCO X6 conectado, con la build actual ya instalada (`lastUpdateTime 2026-10-09 22:30:26` = APK actual; config respaldada antes y **restaurada y verificada después**: MANUAL, `showRamWidget=false`, ICON_ONLY, fondos 248/249):

1. **Widget OFF (default)** → sidebar sin RAM (captura `phase7-00-default-off.png`, ayer 22:13).
2. **AUTO + imagen, ViewOne:** RAM `6.3 / 11.0 GB` / `4.7 GB disponibles` (`phase7-01`, ayer; `p7-live-1.png`, hoy 10:10).
3. **Polling objetivo (hoy):** dos capturas separadas 6 s → el valor cambió de `6.3 / 11.0 GB` a `6.2 / 11.0 GB` (`p7-live-1` vs `p7-live-2`) — actualización periódica real, no estática.
4. **AUTO + video, ViewTwo:** RAM sobre el video de tinta en reproducción: `6.8 / 11.0 GB` / `4.2 GB disponibles` (`p7-vid-4.png`, hoy 10:17; coincide con `phase7-04` de ayer 22:52). Video activo verificado por diferencia de frames (73 % de píxeles centrales cambian en 3 s).
5. **Fondo claro + RAM oculta (toggle):** `phase7-05-light-ram-hidden.png` (ayer 23:15) y `phase7-05-hidden-textlogo-light.png` (TEXT_ONLY logo + video + RAM oculta, 23:08).
6. **Reloj/batería/mensajes/fecha/logo:** visibles y sin solape en todas las capturas con widget ON (la RAM queda anclada al separador, entre el spacer flexible y la línea — ver §11).
7. **Minecraft → Home (2 ejecuciones hoy, ≥35 s en primer plano cada una):** ver §9.
8. **Sin FATAL/ANR** en buffers `crash` y `main` durante toda la sesión (dos barridos post-Minecraft).

### 7.3 Pendiente de confirmación del desarrollador (protocolo: el agente no asigna PASS físicos)

- [ ] Aceptación visual del widget en los estados capturados (tamaño, legibilidad, colores en claro/oscuro).
- [ ] Drawer: apertura/cierre con widget ON.
- [ ] Gestos entre páginas con widget ON (fluidez, sin conflictos).
- [ ] Widgets nativos interactivos con widget ON.
- [ ] Logo ICON_PLUS_TEXT con RAM visible (hoy quedó incidentalmente capturado icono+NOVA en `p7-vid-postmc-1.png` en ViewOne; falta dedicado).
- [ ] Doble toque para apagar pantalla y deslizamiento de notificaciones (no regresión).
- [ ] Confirmar de vista que el video no genera análisis en background (el retorno warm del video SÍ quedó objetivo hoy: §9).
- [ ] `phase7-02b.png` (variante intermedia de la prueba light-bg de ayer) fue eliminada por error durante la limpieza de esta sesión; las capturas 02 y 03 del mismo estado están intactas. Si esa variante era necesaria, repetir el punto de fondo claro.

## 8. Rendimiento

- 1 lectura binder (`getMemoryInfo`) cada 5 s por página visible, en `Dispatchers.Default` — coste por tick despreciable (~µs-ms), 0 trabajo cuando el launcher está oculto o fuera de RESUMED.
- Sin recomposición periódica extra: solo 3 `Text` por update de snapshot (máx 1 recomposición/5 s por página visible); cifras sin animación.
- RSS del proceso durante la sesión (HyperSentinel): 494 MB → 337 MB tras recorte del sistema, sin fugas atribuibles al widget. **El widget es un monitor informativo: no se declara ninguna mejora de memoria atribuible a esta fase.**
- Sin logs nuevos en hot paths; sin instrumentación residual.

## 9. Resultados Minecraft → Home

Método: `am start` Minecraft → 35 s en primer plano → `KEYCODE_HOME` → espera activa de `topResumedActivity` → capturas + barrido logcat.

| Métrica | Ejecución 1 (config imagen) | Ejecución 2 (config video) |
|---|---|---|
| Tiempo en primer plano | 35 s | 35 s |
| Retorno HOME → Nova top-resumed | **847 ms** | **511 ms** |
| RAM al volver | `7.0 / 11.0 GB` fresca inmediata (antes: 6.2) | `7.1 / 11.0 GB` fresca inmediata (página 1) |
| Video tras retorno | n/a (imagen) | **reanudado warm** en página 2: 66 % píxeles centrales cambian en 3 s (`p7-vid-postmc-3`) |
| Proceso Nova tras retorno | vivo (pid 14521) | vivo (pid 16113) |
| FATAL / ANR | ninguno | ninguno |
| Polling Nova durante Minecraft | estructuralmente imposible (corutina cancelada al salir de RESUMED — test K/L/R); sin instrumentación añadida para no tocar zonas protegidas | ídem |

Capturas: `p7-live-3-postminecraft.png` (post-MC ejecución 1), `p7-vid-postmc-1.png` (post-MC ejecución 2, página 1), `p7-vid-postmc-3.png` (video reanudado en página 2).

## 10. Capturas (`docs/assets/`)

**Sesión 2026-10-09 (previa):** `phase7-00-default-off.png` (default OFF, 22:13) · `phase7-01-ram-image-auto.png` (ViewOne imagen AUTO, 22:30) · `phase7-02-ram-light-bg.png` / `phase7-03-ram-light-bg.png` (variantes fondo claro, 22:33/22:48) · `phase7-04-ram-video.png` (ViewTwo video AUTO, 22:52) · `phase7-05-hidden-textlogo-light.png` (TEXT logo + oculto, 23:08) · `phase7-05-light-ram-hidden.png` (oculto tras toggle, 23:15) · `phase7-picker*.png` / `phase7-pagecheck.png` (navegación de Settings/picker) · `p7-light.xml`, `p7-video.xml`, `p7-a.xml` (snapshots de config de prueba).

**Sesión 2026-10-10 (esta sesión):** `p7-live-1/2.png` (polling objetivo 6 s) · `p7-live-3-postminecraft.png` · `p7-vid-1.png` (estado transitorio documentado en §11) · `p7-vid-4.png` (RAM sobre video activo) · `p7-vid-postmc-1/3.png` · `p7-restored.png` (config restaurada) · `p7-gate1..4.log` (outputs completos de gates).

## 11. Riesgos y decisiones

1. **Ubicación (decisión de diseño):** el status rotado del sidebar usa `wrapContentSize(unbounded)+rotationZ` y sobresale visualmente ~2× su placeholder de layout (defecto latente preexistente, inocuo antes porque solo le seguía un spacer flexible). Colocar la RAM inmediatamente después del status **solapaba el texto** (validado físicamente). Solución: widget anclado **entre el spacer flexible y el separador**, en ViewOne y ViewTwo, sin mover logo/status/separador/accesos ni tocar la geometría global del sidebar.
2. **Estado transitorio observado (no bloqueante):** tras el primer arranque con config de video inyectada vía prefs (app force-stopped), la página 2 mostró el poster estático ~2 min (video no arrancó). Tras un reinicio limpio del proceso el video arrancó normal y el comportamiento posterior (incluido el retorno warm desde Minecraft) fue correcto. No reproducible con la app definitiva; probable interacción HyperOS+player tras el swap de prefs bajo el proceso. Zona de video intacta en esta fase — vigilar en validación interactiva.
3. **Cifra aproximada:** `availMem` incluye recuperable — el widget lo documenta y nunca representa % alto como peligro.
4. **Locale:** formato con punto decimal fijo (Locale.US) para evitar saltos de anchura; etiquetas es/en vía `language`.
5. **Eliminación accidental:** `phase7-02b.png` borrada durante limpieza (variante de light-bg; 02/03 intactas). Declarado en §7.3.

## 12. Archivos

**Nuevos (fase 7):**
- `app/src/main/java/com/daybreak/animelauncher/ram/RamMonitor.kt`
- `app/src/main/java/com/daybreak/animelauncher/ui/components/RealTimeRam.kt`
- `app/src/test/java/com/daybreak/animelauncher/ram/RamMonitorTest.kt`

**Modificados (cableado mínimo):**
- `LauncherViewModel.kt` — campo `showRamWidget` + normalización Gson en la carga
- `LauncherScreen.kt` — `ramPollActive = isAppForeground && currentPage == page` (2 llamadas)
- `ViewOne.kt` / `ViewTwo.kt` — parámetro `ramPollActive` + widget condicionado a `style.showRamWidget` entre spacer y separador
- `AdvancedSettingsScreen.kt` — switch mostrar/ocultar

**Protegidos — sin modificar:** `VideoWallpaperManager`, `VideoBackground`, `DynamicBackground`, player pool/ExoPlayer, Pager/Gestures (más allá del gate de visibilidad), `WidgetHostManager`, servicios de notificaciones/accesibilidad, Hidden Apps, `MemoryManager`, `NovaApplication`, `AdaptiveVideoSampler`, `VideoPaletteGate`, Glass Engine.

**Sin commit, sin push, sin tag** (según instrucción de la fase).
