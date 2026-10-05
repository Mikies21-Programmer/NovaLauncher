# UI-1-PHASE3-REPORT — AUTO activo para fondos de imagen + migración de consumidores

Fecha: 2026-10-04 · Base: Fase 2 (provider por página, paridad pixel-perfect) · POCO X6 5G

## 1. Pipeline AUTO (implementado y validado en físico)
```
backgroundUri (imagen)
  → decode reducido (inSampleSize hacia ~200px, Files/ContentResolver, fuera de Main)
  → WallpaperAnalyzer.analyzeSync → ThemePalette
  → caché LRU por URI (máx 12; mismo URI nunca re-analiza)
  → ThemeProposalGenerator.generateProposals (autoridad matemática, sin duplicar)
  → selección determinista: propuesta "Equilibrado" (fallback: primera; si vacío: DEFAULT)
  → tokens con texto neutro forzado (ver §anti-patrón) + elementColors por jerarquía
  → PageTheme → LocalLauncherThemeTokens por página
```
- Compose: `rememberAdaptivePageTheme(pageIndex, backgroundUri, styleConfig, appContext)` — MANUAL sync; AUTO async con fail-safe MANUAL inicial (sin flash) y actualización al terminar el análisis.
- `resolveAuto` auto-gate: si `themeMode == MANUAL` resuelve MANUAL sin IO (aunque se llame por error).

## 2. Caché
LRU en memoria `uri → ThemePalette` (máx 12, `synchronized`), **solo paletas** (no bitmaps). Mismo URI → 1 análisis (test01 + Robolectric end-to-end). URI nuevo → análisis nuevo (test02). Invalidación: por capacidad LRU; un cambio de fondo introduce clave nueva.

## 3. Política de overrides
- Modelo `AdaptiveColorOverrides(clock/battery/messages/date: String? = null)` — **null = sin override**. Los defaults históricos de AdvancedStyleConfig NUNCA se interpretan como overrides.
- Aplican **solo en AUTO_WITH_OVERRIDES**; AUTO puro los ignora (test08b); MANUAL los ignora (test12).
- Aplicación: solo el elemento con override no-null cambia; el resto conserva el valor generado (test08).

## 4. Cambios en ViewOne/ViewTwo (solo colores; geometría/posiciones/gestos intactos)
Nuevo parámetro `elementColors: AdaptiveElementColors?` (null = MANUAL parity). Mapeo AUTO:
| Elemento | Antes (MANUAL/legacy) | AUTO |
|---|---|---|
| Reloj | clockColor (accent) | **textPrimary** |
| Batería | batteryColor (accent) | **textPrimary** |
| Mensajes | messagesColor (accent) | **textSecondary** |
| Día (grande) | dateColor (accent) | **textPrimary** |
| Fecha (0.85 alpha) | dateColor | **textSecondary** |
| Iconos (tint) | customIconColor | **textPrimary** |
| Botón "mi" | miButtonColor | **accent** (highlight/activo) |
| Texto "mi" | miTextColor | **textOnAccent** |
Otros campos (sidebar, diagonal, triangle, drawer) permanecen desde AdvancedStyleConfig.

## 5-6. Ejemplos de paletas y ratios (logcat `AdaptiveTheme`, físico)
| Fondo | lum | isDark | accent generado | textPrimary | contrastTP |
|---|---:|---|---|---|---:|
| Clara (#F2F2F0) | 0.87 | false | #8A8A8A | **#111216 (oscuro)** | **17.19:1** |
| Oscura (#101820) | 0.01 | true | #466A8D | **#FFFFFF (claro)** | **19.79:1** |
| Saturada (#FF1744) | 0.21 | true | #B55367 | **#FFFFFF** | **19.21:1** |
| Multicolor (gradiente) | 0.27 | true | #808880 | **#FFFFFF** | **18.26:1** |

Todos ≥ 4.5:1 (≥ 17:1 real). Screenshots: `auto_clara/oscura/saturada/multicolor.png` (texto legible, sin cambios geométricos).

## 7. Tests
**171/171 PASS** (155 + 16 nuevos): misma uri → 1 análisis · uri distinto → análisis nuevo · MANUAL no analiza (auto-gate) · AUTO oscuro/claro contrastados · fondo claro→texto oscuro / oscuro→claro · accent ≠ textPrimary · override solo cambia el elemento · AUTO puro ignora overrides · MANUAL paridad style · uri inválido→DEFAULT · Robolectric: análisis real de bitmaps oscuro/claro + uri nulo.

## 8. Validación física
4 escenarios con imágenes generadas (clara/oscura/saturada/multicolor) inyectadas como `file://` en el directorio privado: modo AUTO activo, tokens y ratios capturados por logcat (tabla §5-6), reloj/batería/mensajes/fecha/iconos legibles en las 4, sin cambios de geometría, 0 FATAL, 0 black frames, Drawer OK, gestos OK. **Paridad MANUAL re-verificada tras restaurar la config original** (sin themeMode → MANUAL; sin línea AdaptiveTheme en ese modo).

## 9. Diferencias MANUAL/AUTO
- MANUAL: colores por-elemento exactos de AdvancedStyleConfig; sin IO; paleta DEFAULT estructural.
- AUTO: paleta del fondo → propuesta Equilibrada → texto adaptativo (jerarquía) + acento solo highlights; overrides aplicables solo en AUTO_WITH_OVERRIDES.

## 10. Riesgos y próximos pasos
1. Imágenes de altísimo contraste local (multi-región): la paleta global puede no representar la zona del texto — el scrim del glass (Fase siguiente) cubre ese caso.
2. Sampling de video (Fase posterior): reutilizar este mismo pipeline con trigger periódico + umbral.
3. Overrides de sidebar/accent/logo: estructura `AdaptiveColorOverrides` extensible, aún sin UI.
4. El fail-safe AUTO==MANUAL de Fase 1 fue sustituido por AUTO real; el test11 de Fase 1 sigue fijando la paridad estructural de `resolve` sync.

## Gates
**171/171 tests PASS** · assembleDebug PASS · lintDebug PASS · bundleRelease PASS · git diff --check PASS · Working tree: solo cambios de UI-1 Fases 1-3 + documentación.
