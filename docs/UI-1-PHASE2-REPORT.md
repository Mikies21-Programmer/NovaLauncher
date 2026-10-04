# UI-1-PHASE2-REPORT — Provider de tema por página (paridad MANUAL pixel-perfect)

Fecha: 2026-10-04 · Base: Fase 1 cerrada · Dispositivo: POCO X6 5G (120 Hz)

## 1. Provider anterior
- `MainActivity.onCreate` proveía `LocalLauncherThemeTokens` **global** para todo el árbol (NavHost + Pager + Drawer + diálogos), con tokens derivados de `AdvancedStyleConfig` vía `fromAdvancedStyleConfig`.
- ViewOne/ViewTwo/Drawer no consumían tokens (usaban `AdvancedStyleConfig` directo); el único consumidor real era Hidden Apps (diálogos).

## 2. Provider nuevo (capas, de fuera hacia dentro)
1. **Global (MainActivity, sin cambios)** — fallback/base para rutas no-página (Settings, Onboarding). Se conserva para paridad total de Hidden Apps y cualquier consumidor no migrado.
2. **Página activa (LauncherScreen)** — `CompositionLocalProvider(LocalLauncherThemeTokens provides activePageTheme.tokens)` alrededor del **AppDrawerScreen**: el Drawer global consume el tema de la página activa (`pagerState.currentPage`).
3. **Por página (dentro del lambda del Pager)** — `CompositionLocalProvider(LocalLauncherThemeTokens provides pageTheme.tokens)` alrededor de ViewOne/ViewTwo, con:
```kotlin
val pageTheme = remember(viewConfig.backgroundUri, state.styleConfig) {
    AdaptiveThemeEngine.resolve(page, viewConfig.backgroundUri,
        state.styleConfig.themeMode, state.styleConfig)
}
```

## 3. Flujo por página
```
pageIndex + ViewConfig.backgroundUri + AdvancedStyleConfig (+ themeMode)
    → AdaptiveThemeEngine.resolve → PageTheme → LocalLauncherThemeTokens (por página)
Drawer: AdaptiveThemeEngine.resolve(página activa) → tokens de página activa
```
Fase 2 = MANUAL: sin análisis de fondo, sin IO, sin sampling, sin AUTO.

## 4. Resultados de tests
**155/155 PASS** (148 + 7 nuevos de Fase 2):
- A: multi-página permite PageThemes distintos (estructura).
- B: cambiar `activePage` cambia el theme expuesto (`themeForActive`).
- C: `resolveState` en MANUAL ignora los backgroundUris.
- D: fallback seguro con pageIndex inexistente (cae a activePage).
- E: páginas no comparten instancia de PageTheme (`assertNotSame`), tokens iguales en MANUAL pero identidad por página (`pageIndex`).
- F: Drawer obtiene el theme de activePage.
- G: JSON antiguo de config → resolve MANUAL funciona (compatibilidad).

## 5. Validación física (paridad)
Protocolo: captura BEFORE (build pre-Fase 2 instalada) → install -r Fase 2 → captura AFTER (mismo minuto, misma página, misma configuración de fondos-imagen del dev).

- Dos capturas consecutivas de la misma build → **md5 idéntico**: el encoder PNG es determinista, el md5 es métrica válida.
- BEFORE vs AFTER: 2 hashes → análisis píxel a píxel (decode zlib propio):
  - **120 de 2712 filas difieren, en 2 bandas: filas 33-82 (barra de estado del SISTEMA) y 2607-2676 (barra de navegación del SISTEMA)**.
  - **El área de contenido de Nova (filas 122-2606) es 100% píxel-idéntica**: mismos colores, geometrías, sidebar, iconos, reloj, batería, mensajes, fecha, widgets.
- Smoke post-validación: Drawer abre (uiautomator), gestos de página OK, 0 FATAL, proceso vivo.

## 6. Prueba de paridad MANUAL
- `PageTheme.tokens` en MANUAL == `LauncherThemeTokens.fromAdvancedStyleConfig(config)` == lo que el provider global entregaba antes (test de paridad en AdaptiveThemeEngineTest).
- Diferencias de captura confinadas a bandas de SystemUI ⇒ paridad pixel-perfect del launcher.

## 7. Archivos tocados
- `ui/screens/LauncherScreen.kt`: provider por página en el lambda del Pager + provider de página activa alrededor de AppDrawerScreen + import de `AdaptiveThemeEngine`.
- `theming/AdaptiveThemeState.kt`: +`themeForActive()`.
- `theming/AdaptiveThemeEngineTest.kt`: +7 tests (Fase 2).
- `docs/UI-1-PHASE2-REPORT.md` (este documento).

## 8. Archivos protegidos intactos
VideoWallpaperManager · VideoBackground · Pager/Gesture Engine (solo el wrapper de provider dentro del lambda, sin tocar detectores ni keys) · WidgetHostManager · NotificationMonitorService · LauncherAccessibilityService · Hidden Apps · MemoryManager · NovaApplication · ThemeProposalGenerator · DynamicBackground · ViewOne · ViewTwo · AppDrawerScreen · AdvancedSettingsScreen.
