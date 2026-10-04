# MEMORY-L1 — TRIM_MEMORY_UI_HIDDEN (informe de implementación)

Fecha: 2026-10-04 · Baseline: `32db2beb` · Dispositivo: POCO X6 5G (Android 16/HyperOS, 120 Hz)
Auditoría previa: batería A-G con métricas reales (ver chat de auditoría; juego pesado = Minecraft).

## Cambios (diff completo)

| Archivo | Cambio |
|---|---|
| `memory/MemoryManager.kt` (nuevo) | Singleton a nivel de proceso, implementa `ComponentCallbacks2`, attach idempotente vía `registerComponentCallbacks`, registro de acciones nombradas por nivel (L1 UI_HIDDEN implementado; L2/L3 estructura lista sin acciones), cada acción envuelta en try/catch (nunca propaga), thread-safe. |
| `MainActivity.kt` | 1 línea en `onCreate`: `MemoryManager.attach(this)` (idempotente). |
| `memory/MemoryManagerTest.kt` (nuevo) | 8 tests: dispatch por nivel, otros niveles no disparan L1, unregister, reemplazo por nombre, idempotencia ×5, registro vacío, acción que lanza no propaga ni bloquea, acciones L2/L3 solo con niveles de presión. |

Audiencia previa (§2): sin ImageLoader custom → Coil 2.6 default, memory cache = 25 % del memoryClass; app sin `largeHeap`, growth limit 256 MB → techo de cache 64 MB (lento de llenar; hoy casi vacío con fondo de video). Acción registrada: `imageLoader.memoryCache?.clear()` (no toca disk cache ni video).

IconCache (LRU 128, ≤4.6 MB): sin trim en L1 — candidato L2 (regla §3).

## Resultados físicos (misma batería A→G, mismo pid 21958 de punta a punta)

| Estado | Auditoría (día 1) | L1 (día 2) |
|---|---:|---:|
| A | 203 MB | 140.8 MB |
| B | 230.6 | 157.3 |
| C | 227.9 | 168.5 |
| D | 217.2 | 146.5 |
| E (juego bg) | 162.6 | **94.1** |
| F (+4 min) | 163.7 | **93.8** |
| G (retorno) | 221.6 | **153.1** |

Comparación absoluta entre días contaminada (estado de datos del app más ligero hoy tras los pm clear de Q3/Q6). Dentro de cada batería:
- UI-hidden: Graphics **68.4→13.1 MB** (auto por el sistema), Native 23.8→8.0, Code evict 172→150 → background −52.4 MB.
- Aporte L1 específico hoy: ~0 MB (cache Coil vacía con fondo de video); **techo 64 MB** para usuarios de fondo-imagen (configuración default de Coil, ahora liberada en cada UI_HIDDEN).
- **Retorno a Home: 649 ms** (auditoría: 776 ms) — sin regresión.
- Video: en movimiento inmediato, 0 black frames (2 hashes en 6×1 s; el clip tiene segmentos cuasi-estáticos largos).
- Drawer/gestos/widgets: operativos tras retorno y estrés.
- **Stress juego↔Home ×20 + 4 ciclos**: pid vivo todo el tiempo, **0 FATAL, 0 ANR**, post-stress 159.1 MB.

## Gates
135/135 tests (127+8) · assembleDebug PASS · lintDebug PASS · bundleRelease PASS · git diff --check PASS.

## Notas
- L2/L3 (presión de background, IconCache, decisiones sobre el pool) NO implementados: la estructura `registerBackgroundAction/registerCriticalAction` está lista y sin acciones.
- Verificación del despacho físico: los tests unitarios cubren el dispatch (nivel y orden); con config de video la cache Coil está vacía, por lo que no hay delta PSS observable que atribuir — el beneficio crece con fondos-imagen.
