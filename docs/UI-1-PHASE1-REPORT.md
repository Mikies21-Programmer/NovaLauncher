# UI-1-PHASE1-REPORT — Núcleo runtime del tema adaptativo (MANUAL parity)

Fecha: 2026-10-04 · Base: commit post-Memory-L2 · Auditoría: `UI-1-ADAPTIVE-PREMIUM-AUDIT.md` (READY)

## 1. Archivos creados/modificados

| Archivo | Tipo | Cambio |
|---|---|---|
| `theming/LauncherThemeTokens.kt` | modificado | +`textOnAccentColor` (default `DEFAULT_TEXT_ON_ACCENT` = negro) y `+statusColor` (default = accentColor, paridad con status widgets actuales); `fromAdvancedStyleConfig` los popula (miTextColor / accent) |
| `theming/ThemeMode.kt` (nuevo) | creado | enum `AUTO / MANUAL / AUTO_WITH_OVERRIDES` |
| `theming/AdaptiveThemeState.kt` (nuevo) | creado | `AdaptiveThemeState` + `PageTheme` + `GlassParams` (modelos puros inmutables) |
| `theming/AdaptiveThemeEngine.kt` (nuevo) | creado | `resolve(pageIndex, backgroundUri, mode, styleConfig)` + `resolveState(...)`; Fase 1 = solo MANUAL (AUTO/AUTO_WITH_OVERRIDES fail-safe → camino MANUAL) |
| `LauncherViewModel.kt` | modificado | `AdvancedStyleConfig.themeMode: ThemeMode = MANUAL` + normalización defensiva Gson (JSON antiguo/corrupto → MANUAL) |
| `theming/AdaptiveThemeEngineTest.kt` (nuevo) | creado | 13 tests (A-J del gate + fail-safe AUTO) |
| `docs/UI-1-PHASE1-REPORT.md` (nuevo) | creado | este informe |

**No modificados (verificados):** ViewOne, ViewTwo, AppDrawerScreen, AdvancedSettingsScreen, DynamicBackground, VideoWallpaperManager, VideoBackground, Pager/Gestures, WidgetHostManager, Hidden Apps, servicios, MemoryManager, NovaApplication. **La UI visible no cambió** (los tokens extendidos no tienen consumidores nuevos aún).

## 2. Modelo final

- `LauncherThemeTokens` (+2 campos con fallback determinista; `DEFAULT` intacto; constructores nombrados existentes compilan sin cambios).
- `PageTheme(pageIndex, tokens, glass, palette, isDark)` — inmutable, data class estable.
- `GlassParams(scrimColor, scrimAlpha, gradientTop/Bottom, borderColor, borderAlpha, highlightColor, cornerRadius)` — defaults equivalentes al diseño actual.
- `AdaptiveThemeState(perPage: Map<Int, PageTheme>, activePage)` + `themeFor(pageIndex)` con fallback seguro.

## 3. API de AdaptiveThemeEngine

```kotlin
AdaptiveThemeEngine.resolve(pageIndex, backgroundUri, mode, styleConfig): PageTheme
AdaptiveThemeEngine.resolveState(pageIndices, backgroundUris, activePage, mode, styleConfig): AdaptiveThemeState
```
- MANUAL: puro, sin IO, sin MediaMetadataRetriever, `backgroundUri` ignorado, paleta = `ThemePalette.DEFAULT` estructural.
- AUTO / AUTO_WITH_OVERRIDES: fail-safe (resuelven igual que MANUAL en Fase 1) — puntos de conexión documentados para `WallpaperAnalyzer`/`VideoFrameExtractor`/`ThemeProposalGenerator` en Fase 2.

## 4. Comportamiento MANUAL (paridad)
- tokens = `fromAdvancedStyleConfig(config)` + `textOnAccentColor = parseHex(config.miTextColor)` + `statusColor = accent` (reflejo exacto del tratamiento actual).
- glass = mapeo directo: sidebarColor/sidebarOpacity → scrim; diagonalBarColor → gradientTop; accent/glassBorderAlpha → border; cornerRadius.
- **Sin reinterpretación de campos** (la eliminación del anti-patrón accent→texto ocurre en fases posteriores, cuando ViewOne/ViewTwo consuman tokens).

## 5. Tests
13 nuevos (`AdaptiveThemeEngineTest`): default MANUAL · mapeo config→tokens/glass · colores personalizados · backgroundUri ignorado en MANUAL · DEFAULT determinista · inválidos→fallback · GlassParams en rango · PageTheme estable · multi-página independiente + fallback seguro de `themeFor` · Gson JSON antiguo sin themeMode → MANUAL (constructor no-arg de AdvancedStyleConfig) · round-trip Gson · contraste textOnAccent vs accent ≥ 4.5:1 usando `ThemeProposalGenerator.calculateContrastRatio` (autoridad existente, sin duplicar) · AUTO/AUTO_WITH_OVERRIDES fail-safe.
**Total: 148/148 PASS** (135 + 13).

## 6. Riesgos
1. Gson enum: JSON con nombre de enum desconocido/corrupto → null; mitigado con normalización defensiva en `loadState` (→ MANUAL).
2. `statusColor`/`textOnAccentColor` sin consumidores reales hasta que la UI migre — sin efecto visible (por diseño de Fase 1).
3. El fail-safe AUTO==MANUAL debe eliminarse explícitamente en Fase 2 (test11 lo documenta como comportamiento temporal).

## 7. Deliberadamente sin implementar
Análisis de wallpaper/video · sampling periódico · umbral perceptual · caché por uri (solo API) · glass visual (modelos listos, sin dibujo) · lerp entre páginas · logo · RAM widget · Settings de modo · migración de ViewOne/ViewTwo/drawer a tokens.

## Gates
**148/148 tests PASS** · assembleDebug PASS · lintDebug PASS · bundleRelease PASS · git diff --check PASS.
