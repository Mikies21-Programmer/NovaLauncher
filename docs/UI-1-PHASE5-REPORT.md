# UI-1-PHASE5-REPORT — Video adaptive sampling (MANUAL/AUTO parity extendida a video)

Fecha: 2026-10-04 · Base: Fases 1-4 cerradas · POCO X6 5G (Android 16/HyperOS, 120 Hz)

## 1. Arquitectura implementada

```text
Fondo de VIDEO en AUTO (solo con página visible + app en foreground):
  produceState(uri, styleConfig, isPageVisible, isAppForeground)
    → cada 8 s: VideoFrameExtractor.extractFrameAt(pos = tick*2000 % duration)
      (MediaMetadataRetriever en Dispatchers.IO, 1 frame ~400 px, recycle inmediato)
    → WallpaperAnalyzer.analyzeSync → ThemePalette por muestra
    → ventana de 3 muestras → ThemePaletteAggregator (medoid + lum media)
    → VideoPaletteGate.shouldUpdate (reutiliza colorDistanceSq del agregador
      + delta de luminancia; umbrales 3600 RGB² y 0.15)
    → si supera umbral: buildAutoPageTheme → value (lerp por el wrapper)
  → página invisible / app en background: productor CANCELADO (keys del
    produceState incluyen visibilidad y foreground) → cero extracciones.
```
- **No se tocó**: VideoWallpaperManager, VideoBackground, player pool, ExoPlayer lifecycle, TextureView, Pager/Gesture, DynamicBackground, MemoryManager, Hidden Apps, servicios.
- **No hay**: segundo player, segunda superficie, procesamiento por frame, bitmap persistente de tamaño completo (los frames de análisis se reciclan inmediatamente tras analyzeSync).

## 2. Frecuencia real (logcat, físico)
tick=0 a las 21:56:24 → tick=2 a las 21:56:40: **16 s / 2 ticks = 8.0 s por tick exacto**. El tick 1 no emitió actualización (gate amortiguó) — evidencia de que el sampling corre cada 8 s pero el tema solo cambia cuando la paleta supera el umbral.

## 3. Lifecycle (prueba crítica Minecraft)
- Minecraft 30 s en foreground: **0 líneas AdaptiveTheme** (0 muestras en background) ✓
- Proceso vivo (mismo pid) → video warm al volver ✓
- Retorno a Home: **566 ms** a foco ✓
- Al volver: sampler re-anclado (tick=0), theme re-aplicado de inmediato ✓
- 0 FATAL durante toda la secuencia ✓

## 4. Agregación
Ventana de 3 muestras con medoid mayoritario: test09 demuestra que una muestra azul
momentánea entre muestras rojas NO voltea el tema (updates ≤ 2 en 5 ticks con dominante
rojo persistente).

## 5. Umbral perceptual
- Δ RGB² dominante ≥ 3600 (≈60 por canal) O Δ lum ≥ 0.15 → actualiza.
- test06: paleta idéntica → no actualiza · test07: ΔR=8 → no actualiza ·
  test08: paletas distintas → actualiza (también por luminancia).

## 6. Lerp
`AdaptiveLerp.lerpPageTheme(a, b, f)` — puro y determinista (test12: f=0→a, f=1→b,
f=0.5→punto medio idéntico, glass.scrimAlpha dentro de rango). El wrapper Compose anima
el display hacia el target con tween 350 ms solo cuando el target cambia realmente;
el video no se interpola (solo tokens UI).

## 7-8. Performance y recursos (físico)
- Extracciones: exactamente 1 cada 8 s con página visible (contadas: extractionCount
  en tests; logcat timestamps en físico).
- Background: 0 extracciones en 30 s de Minecraft (grep logcat = 0).
- PSS en background con video AUTO: sin cambio atribuible al sampler (el sampling no
  retiene bitmaps — recycle inmediato tras analyzeSync).

## 9. Validación física completa
- Video neón del usuario (AUTO + video): textos blancos legibles sobre el video, glass
  oscuro en sidebar, "mi" con accent generado (#466A8D), screenshot `phase5_return.png`.
- Prueba crítica Minecraft: PASS (ver §3).
- Drawer OK (uiautomator), gestos OK, 0 FATAL, config original restaurada al final.

## 10. Video AUTO vs Image AUTO
| Aspecto | Image AUTO | Video AUTO |
|---|---|---|
| Análisis | 1 por URI (cacheado) | 1 cada 8 s con página visible |
| Paleta | única estable | ventana de 3 agregadas + gate |
| Cambios de theme | 1 (al analizar) | solo si el umbral perceptual supera |
| Coste por análisis | decode reducido ~10-30 ms | MediaMetadataRetriever ~50-200 ms |
| Cancelación | n/a (1-shot) | keys del produceState (visible/foreground) |

## 11. Riesgos
1. MediaMetadataRetriever en videos HDR/4K puede costar más de 200 ms por muestra — el
   intervalo de 8 s lo amortigua; monitorizar en videos grandes reales.
2. `videoDurationMs` null (streaming sin metadata) → sampling en posición 0 siempre →
   paleta estable → gate amortigua (sin thrashing).
3. La ventana de 3 con transiciones largas puede retrasar la adaptación hasta 24 s —
   comportamiento intencional (anti-flicker).

## 12. Blur dinámico
**NO implementado ni planeado a corto plazo**: el blur por muestra de video sería
procesamiento por muestra (prohibido) y el blur dinámico por frame está prohibido por
las reglas de la fase. El glass simulado (tint/gradiente/borde/highlight) cubre la
legibilidad. Re-evaluación: solo con evidencia de que el glass simulado es insuficiente
en fondos reales.

## Veredicto
**UI-1 PHASE 5 — READY** (físicamente validado; sin commit, orden del desarrollador).
