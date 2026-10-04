# MEMORY-L2-AUDIT — Auditoría de memoria recuperable en background

Fecha: 2026-10-04 · Base: `3e312fc` (L1 cerrado) · Dispositivo: POCO X6 5G (Android 16/HyperOS, 120 Hz)
Alcance: SOLO auditoría. Sin cambios de código. Working tree limpio salvo este informe.

## 1. Baseline

Batería física A-G (mismo pid 32419 de punta a punta; Minecraft como app pesada):

| Estado | PSS | Java | Native | Code | Graphics |
|---|---:|---:|---:|---:|---:|
| A — recién iniciado¹ | 330.6 MB | 21.0 | 57.5 | 352.9 | 66.8 |
| B — 5 min de uso | 161.2 MB | 20.5 | 24.8 | 158.6 | 68.3 |
| C — Drawer+Settings+páginas | 168.5 MB | 26.9 | 23.7 | 160.3 | 68.8 |
| D — 4 min de video/imagen | 154.5 MB | 15.6 | 22.3 | 156.6 | 68.5 |
| E — Minecraft 90 s (bg) | **99.0 MB** | 10.8 | 19.7 | 157.0 | **13.0** |
| F — juego +4 min (bg) | **98.1 MB** | 10.5 | 11.0 | 155.1 | 13.0 |
| G — retorno a Home | **153.6 MB** | 11.0 | 11.4 | 156.0 | 68.0 |

¹ **A está inflado por JIT de fresh-install** (Code 353 MB vs ~155 estable): artefacto de cada `install -r`, se disipa solo tras minutos de uso. El estado estable real de "recién iniciado" es B (161 MB). Foco de retorno: **628 ms**; fondo en movimiento al volver; 0 FATAL; 0 ANR.

## 2. Arquitectura/caches encontradas

| Cache/estructura | Dueño | Vida | Contenido |
|---|---|---|---|
| `IconCache.lruCache` | singleton `IconCache` | proceso | LRU 128 × ImageBitmap 96×96 ARGB (36 KB c/u) |
| `VideoWallpaperManager.pageBindings/savedPositions` | singleton (protegido) | proceso | 2-8 entradas, bytes |
| `MemoryManager.uiHiddenActions` etc. | singleton | proceso | 1 acción (Coil), bytes |
| Coil `memoryCache` | ImageLoader default (25 % de 256 MB = techo 64 MB) | proceso | bitmaps de fondos-imagen (2 con la config actual) |
| Coil `diskCache` | default de Coil | disco (no RAM) | no configurado |
| `LauncherViewModel._state/_installedApps` | VM | proceso | LauncherState (~12 KB JSON) + ~110 AppShortcut (4 strings c/u) |
| `LauncherScreen` companion (`onAccessibilityDisclosureRequested`) | clase | proceso | 1 closure, anulada en dispose |
| `BatteryMonitor.receiver` | singleton | dinámico | 1 receiver con contador register/unregister pareado |
| `WallpaperAnalyzer` / `VideoFrameExtractor` / `ThemeProposal*` | singletons stateless | — | sin estado retenido |

## 3. Tabla de consumidores de memoria (estado estable: D foreground / E-F background)
Ver §1. Reparto background (98 MB): Code 155 (file-backed, compartido — el OS lo evicta bajo presión sin morir el proceso), Graphics 13 (auto), Native 11-20 (pool de players + runtime), Java 10.5, resto System/Other.

## 4. Mediciones A-G
Tabla §1. Notas: la caída D→E (−55 MB) es **Graphics liberado automáticamente por el sistema** al destruirse las superficies — no atribuible a código propio; Native cae además 22→11-20 por trim nativo del runtime; pid sobrevivió a todo el estrés.

## 5. IconCache
- **Estructura:** `object` con `android.util.LruCache<String, ImageBitmap>(128)`, acceso `synchronized`.
- **Entradas reales:** ≈ nº de apps mostradas (110-140 instaladas; LRU cap 128) → **1-4.6 MB** (36 KB/icono por downsample 96×96).
- **Referencias fuertes:** solo dentro del LRU acotado; **sin retención de Context/Activity** (contexto usado transitoriamente para `getApplicationIcon`).
- **Costo en background:** ≤4.6 MB techo (≈2-4 MB real).
- **Reconstrucción al volver:** re-decode por icono (1-5 ms, en IO) → re-pintado del drawer con parpadeo breve posible.
- **Mecanismo LRU existente:** sí (el propio LruCache evicta por acceso).
- Veredicto: el ahorro (~2-4 MB sobre 98 MB) no compensa el costo/riesgo de retorno.

## 6. Cachés auxiliares
Ninguna con retención material: statics de VideoWallpaperManager (protegido, bytes), MemoryManager (1 acción), ViewModel (listas livianas), companion de LauncherScreen (anulado en dispose), BatteryMonitor (register/unregister pareados, application context). **Sin bitmaps fuera de Coil/IconCache. Sin resultados de queries retenidos. Sin Contexts de Activity retenidos por singletons.**

## 7. Coil
- L1 ya limpia `memoryCache` en `UI_HIDDEN` (en HyperOS el nivel no se entrega espontáneamente al proceso HOME — ver MEMORY-L1-FINAL.md; el clear actuará en plataformas que sí entregan, o si L2 elige un disparador propio).
- Tamaño máximo teórico: 64 MB (25 % de 256 MB); **observado en background: ~0 MB neto** — los hardware bitmaps cacheados liberan sus texturas al destruirse la superficie (E/F: Graphics 13 MB, Java 10.5 MB) → el cache "lleno" de fondos-imagen **no aparece como RAM residente en background**.
- No hay otros cache layers en uso (disk cache default, en disco).

## 8. Video pool (solo medición — zona protegida)
- Pool de 2 players vivo en background, `playWhenReady=false` (pausado), surfaces desmontadas por el sistema.
- Aporte marginal en background: dentro de Native 11-20 MB junto al resto del runtime; **no hay firma de los players por separado**, pero la caída D→E lo sitúa en el rango de pocos MB.
- Recuperación al volver: primera frame inmediata (0 black frames, 628 ms a foco, fondo en movimiento).
- **`DO NOT TOUCH — ROI insuficiente / riesgo de warm-return`**: liberar players ahorraría pocos MB a cambio de re-prepare (0.5-1 s) + riesgo de black frame, contradiciendo el criterio de retorno inmediato ya validado.

## 9. Posibles retenciones/leaks
**Ninguno detectado**: receiver de pantalla unregister en onDestroy ✓; closure estático de LauncherScreen anulada en dispose ✓; singletons stateless ✓; BatteryMonitor pareado ✓; SwapPss ≈ 0 (91-157 KB); sin crecimiento B→D ni entre ciclos de estrés de L1 (×20, pid estable).

Clasificación (§5 del mandato): todo lo retenido es **A) deliberadamente residente** (pool, VM, IconCache) o **B) cache** (Coil, IconCache); **C) recuperable adicional: ~2-4 MB (IconCache)**; **D) leaks: 0**.

## 10. Matriz costo/beneficio

| Objeto | MB est. | Recuperable | Costo reconstrucción | Riesgo | Recomendación |
|---|---:|---|---|---|---|
| Graphics (auto) | 55 | ya lo hace el sistema | n/a | — | DO NOT TOUCH |
| Code (dex/oat/so) | 155 | file-backed; el OS evicta | n/a | — | DO NOT TOUCH |
| Video pool (2 players) | ~5-10 | sí, técnico | 0.5-1 s + riesgo black | warm-return | **DO NOT TOUCH** |
| IconCache | 2-4 | sí | re-decode por icono | flicker en drawer | **DEFER** |
| Coil memoryCache | ~0 bg (techo 64 MB fg) | L1 ya lo cubre | re-decode on-demand | bajo | **KEEP (L1 vigente)** |
| ViewModel/estado/listas | <2 | no deseable | re-load disco | warm-return | **KEEP** |
| WidgetHost views | variable | no deseable | re-bind usuario | widgets | **DO NOT TOUCH** |
| Statics varios | <0.1 | trivial | trivial | — | **KEEP** |

## 11. Candidatos reales para L2
**Ninguno cumple las reglas de decisión.** El único candidato medible (IconCache, 2-4 MB) falla el criterio "el comportamiento actual realmente se beneficia": el proceso sobrevivió a todo el estrés a 94-160 MB, y el ahorro es <5 % del PSS background con costo visible de retorno.

## 12. Elementos explícitamente descartados
- Liberar el pool de video (protegido + ROI insuficiente + riesgo warm-return) — `DO NOT TOUCH`.
- Trim de IconCache — `DEFER` (revisar solo si aparece evidencia de process death por presión real de usuario).
- Liberar widgets/views — prohibido (zona protegida).
- Objetivo arbitrario `<100 MB` — rechazado: el background ya mide 98 MB y la cifra no representa memoria recuperable por la app (Code compartido domina).

## 13. Recomendación final
**No implementar optimización alguna en L2.** La política actual (L1 + comportamiento por defecto del sistema + arquitectura de pool) ya entrega: **98 MB en background, 628 ms de retorno, cero black frames, cero leaks, proceso vivo bajo estrés ×20**. La única acción con sentido futuro: si en uso real aparece process death del launcher con juegos, reabrir esta auditoría priorizando (en orden) un disparador propio de UI-hidden (onStop explícito) para Coil, y el trim de IconCache — ambos con la matriz de validación de este documento.

## Veredicto
`L2 AUDIT — READY` (auditoría completa; conclusión: **no hay candidato que justifique implementación L2** — mantener arquitectura actual y reserva IconCache como único candidato condicional).
