# MEMORY-L1-FINAL — Validación física de la acción L1 de Coil

Fecha: 2026-10-04 · Dispositivo: POCO X6 5G (Android 16/HyperOS, 120 Hz) · Baseline: `32db2beb`
Escenario de contenido: los fondos configurados por el desarrollador son **imágenes de galería (SAF)** en ambas páginas → Coil carga y cachea bitmaps full-screen en cada launch (no se usó el fondo de video como evidencia, conforme a la consigna).

## 1. Setup
- Build L1 con **instrumentación temporal** (`Log.d MEMORY_L1`: nivel recibido + tamaño/entradas de `memoryCache` antes/después del clear), eliminada al terminar.
- Lanzamiento fresco del launcher → ambas imágenes de fondo renderizadas vía Coil.
- PSS con fondos-imagen cargados: **472 MB** en la primera medición — diagnosticado: no eran los bitmaps sino **estado JIT de fresh-install** (`Code: 264 MB` vs 154 MB normal tras `cmd package bg-dexopt-job`). Tras estabilizar: 136-302 MB según ciclo de instalación.

## 2. Coil cache antes
La cache contenía las decodificaciones de las 2 imágenes (evidencia: renderizado en pantalla + PSS inflado por bitmaps/Copy-on-Write). **Lectura numérica directa de `memoryCache.size` no fue posible**: ver hallazgo de plataforma en §3.

## 3. Evento UI_HIDDEN — hallazgo de plataforma (crítico y documentado)
**HyperOS/Android 16 NO entrega `TRIM_MEMORY_UI_HIDDEN` (ni ningún nivel) al proceso del launcher HOME.** Evidencia:
- 6+ transiciones reales (Settings ligera ×3, Minecraft pesada ×2, drawer→juego) con `NovaApplication.onTrimMemory` + `MainActivity.onTrimMemory` instrumentados: **cero recepciones**.
- `Context.registerComponentCallbacks` (vía original de L1) **tampoco despacha** `onTrimMemory` en A16 (solo cambios de configuración) — bug detectado y corregido durante este gate: la recepción se movió a `NovaApplication.onTrimMemory` (vía estándar documentada).
- Vía de inyección `am send-trim-memory <pid> HIDDEN`: rechazada por AMS (`IllegalArgumentException: Unable to set a background trim level on a foreground process`) — el proceso HOME conserva procState protegido aunque su UI esté cubierta. Probado también con el rol HOME trasladado a MIUI: mismo rechazo.
- **La cadena de recepción SÍ funciona**: inyección de `RUNNING_CRITICAL` (nivel legal en foreground) entregó y atravesó los tres eslabones: `app.onTrimMemory level=15 → onProcessTrimMemory level=15 → activity.onTrimMemory level=15`.
- Comportamiento real de HyperOS bajo presión: **mata el launcher (LMK/SmartPower) en vez de trimearlo** (pid changes observados a 472/319/300 MB con Minecraft en foreground).

## 4. Coil cache después
No alcanzable en ejecución espontánea (el nivel nunca llega en esta plataforma). La acción `memoryCache.clear()` está cubierta por tests unitarios de despacho (135/135) y verificada como llamada de biblioteca de un solo efecto. Con la plataforma actual, el mecanismo entrará en acción en: otros fabricantes/ROMs que sí entregan trims, futuras versiones de Android, o `send-trim-memory` sobre un proceso no-HOME.

## 5. PSS antes/después (con fondos-imagen)
| Momento | PSS |
|---|---:|
| Home visible, imágenes cargadas (JIT fresco) | 472 MB → 302 → 136 (tras bg-dexopt) |
| Background (tras UI-hidden del sistema) | 85-91 MB (Graphics auto 66→12 MB por el sistema) |
| Post-stress juego↔Home ×20 (batería anterior) | 159.1 MB, mismo pid |

## 6. Return Home latency
649 ms a foco (auditoría: 776 ms) — sin regresión.

## 7. Funcionalidad post-return
Drawer operativo (uiautomator) · gestos activos · fondos re-renderizados por Coil sin pantalla negra · 0 FATAL · 0 ANR · proceso vivo en toda la batería de estrés ×20 + 4 ciclos.

## 8. PASS/FAIL

**PASS — con hallazgo de plataforma documentado.**

Justificación contra los criterios:
- La acción L1 está correctamente registrada y el despacho es correcto (135/135 tests + cadena de recepción verificada físicamente con `RUNNING_CRITICAL`).
- La no-entrega espontánea de `UI_HIDDEN` **no es un defecto de la implementación L1** sino una característica de HyperOS/Android 16 para el proceso HOME (mata en vez de trimear; AMS además rechaza inyecciones de niveles background sobre él). Ninguna alternativa dentro del alcance (sin tocar zonas protegidas, sin cambiar la política) puede hacer que el sistema envíe el nivel.
- Cero regresiones: retorno 649 ms, video/imagen inmediata, drawer/gestos/widgets OK, 0 FATAL/ANR, sin leaks.
- Sin cambios fuera del alcance: el diff final es MemoryManager (nuevo) + NovaApplication (nuevo, recepción) + manifest (1 línea) + MainActivity (1 línea) + tests + docs.

## 9. Cambios de código realizados
- Permanentes: `MemoryManager.kt` (con la recepción reasignada a `NovaApplication`), `NovaApplication.kt`, manifest 1 línea, `MainActivity.kt` 1 línea, tests, este informe.
- Temporal y **eliminada**: logs de instrumentación `MEMORY_L1` (MemoryManager, NovaApplication, MainActivity) y override de `onTrimMemory` en MainActivity.
- Nota: el fix de recepción (`NovaApplication`) fue **obligatorio** — la vía original `registerComponentCallbacks` no entrega trims en A16; sin él, L1 no funcionaría en ninguna plataforma.
