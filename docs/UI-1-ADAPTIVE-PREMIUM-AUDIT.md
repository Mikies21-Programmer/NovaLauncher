# UI-1-ADAPTIVE-PREMIUM-AUDIT — Tema adaptativo dinámico, Glass Premium y contraste real

Fecha: 2026-10-04 · Base: commit post-Memory-L2 · Alcance: AUDITORÍA + DISEÑO (sin cambios de código)
Zonas protegidas intocadas en el diseño: VideoWallpaperManager, VideoBackground, Pager/Gesture Engine, WidgetHostManager, servicios, Hidden Apps, MemoryManager, NovaApplication.

## 1. Arquitectura actual (cómo se colorea el launcher hoy)

```
AdvancedStyleConfig (12+ campos HEX por elemento, persistido en launcher_style_config)
        │  LauncherThemeTokens.fromAdvancedStyleConfig()  ← SOLO toma accentColor; resto = DEFAULT
        ▼
LocalLauncherThemeTokens (CompositionLocal global en MainActivity)
        │                                  │
        ▼                                  ▼
Hidden Apps dialogs (único consumidor real   ViewOne/ViewTwo/Drawer
de textPrimary/textSecondary/border)         consumen AdvancedStyleConfig DIRECTAMENTE
                                             (0 usos de themeTokens — verificado)
```

- **ViewOne/ViewTwo consumen `style.<elemento>`** (miButtonColor, clockColor, batteryColor, messagesColor, dateColor, triangleColor, sidebarColor, diagonalBarColor…) — no consumen tokens.
- **El flujo de propuestas existente aplana tokens → elementos** (`ThemeProposalFlowHandler.proposalToAdvancedStyleConfig`): asigna `accentColor` a clock/battery/messages/date — **el anti-patrón que UI-1 debe eliminar** (acento como texto principal).
- Pipeline adaptativo existente (usuario-disparado, desde `a3aef60`):
  `VideoFrameExtractor.extractFrames(uri)` (MediaMetadataRetriever, frames escalados, retriever liberado en finally) → `WallpaperAnalyzer.analyze(bitmap)` → `ThemePalette` (dominant/vibrant/muted/isDark/avgLuminance) → `ThemePaletteAggregator.aggregate` (medoid + luminancia media) → `ThemeProposalGenerator.generateProposals` (Dominante/Vibrante/Muted, **texto forzado a blanco/negro con ensureContrast ≥ 4.5:1, WCAG 2.1**) → `ThemeProposalDialog` (usuario elige) → `ThemeProposalFlowHandler.proposalToAdvancedStyleConfig` → persiste.

## 2. Qué ya existe y puede reutilizarse (NO duplicar)

| Pieza | Estado | Reuso en UI-1 |
|---|---|---|
| `ThemePalette` | completo + DEFAULT | motor de paleta del tema adaptativo |
| `WallpaperAnalyzer.analyze` | completo (IO dispatcher) | paleta para fondos-imagen |
| `VideoFrameExtractor.extractFrames` | completo (MediaMetadataRetriever, scaled) | sampling periódico de video (base) |
| `ThemePaletteAggregator.aggregate` | medoid + luminancia | fusionar muestras periódicas del video |
| `ThemeProposalGenerator.generateProposals` | 3 propuestas + ensureContrast + WCAG 4.5:1 | generación de tokens adaptativos |
| `LauncherThemeTokens` + `LocalLauncherThemeTokens` | 5 tokens, inmutable, testable | base del nuevo token set (extender) |
| `AdvancedStyleConfig` per-elemento HEX | persistido, Gson-safe | capa de OVERRIDES en AUTO_WITH_OVERRIDES |
| `DynamicBackground` | selector imagen/video | fuente de la señal (uri) para el engine |
| Glass actual (sidebar/diagonal/triangle con alpha) | en ViewOne/ViewTwo | punto de enganche del glass adaptativo |

## 3. Qué falta

1. **Modo de tema** (AUTO/MANUAL/AUTO_WITH_OVERRIDES) — no existe; hoy todo es "manual con propuesta".
2. **Motor runtime** que resuelva tokens por fondo automáticamente (sin diálogo) y con caché por `backgroundUri`.
3. **Tokens por página** (theme A ↔ theme B con transición) — hoy es un CompositionLocal global único.
4. **Tokens de status adaptativos** (reloj/batería/mensajes/día/fecha) derivados de textPrimary — hoy cada uno apunta al acento.
5. **Logo widget** (icon/text/ambos) — no existe.
6. **RAM widget** (memoria global del teléfono) — no existe.
7. **Glass adaptativo** formalizado (tint/alpha/gradiente/borde/highlight derivados de paleta) — hoy hay alphas fijos.
8. **Animación de transición de tokens** (lerp) — hoy el cambio es instantáneo.
9. **Umbral perceptual** para actualizar paleta solo si el fondo cambió de verdad.

## 4. Flujo propuesto

```
DynamicBackground resuelve (pageIndex → uri, tipo)
        │ (cambio de uri O tick de sampling)
        ▼
AdaptiveThemeEngine.resolve(pageIndex, uri, mode, overrides)
        │  imagen: WallpaperAnalyzer.analyze (1 vez por uri, cacheada)
        │  video:  VideoFrameExtractor cada N s (amortiguado, umbral perceptual)
        ▼
ThemePalette → ThemeProposalGenerator.generateProposals → selección determinista (AUTO: "Equilibrado")
        ▼
AdaptiveThemeState { pageIndex → LauncherThemeTokens extendidos + glassParams }
        │ (animación de lerp si cambió sobre umbral)
        ▼
LocalLauncherThemeTokens proveído POR PÁGINA (dentro del contenido del Pager)
```

- `MANUAL`: el engine no corre; tokens = `fromAdvancedStyleConfig` (comportamiento actual exacto).
- `AUTO_WITH_OVERRIDES`: tokens generados; cualquier elemento con override del usuario gana.
- Disparo: cambio de `backgroundUri` (inmediato, cacheada) + timer de sampling solo para video.

## 5. Modelo de AdaptiveThemeState

```kotlin
data class AdaptiveThemeState(
    val perPage: Map<Int, PageTheme>,          // tema resuelto por página
    val activePage: Int,
) {
    data class PageTheme(
        val tokens: LauncherThemeTokens,       // extendido (ver abajo)
        val glass: GlassParams,                // tint/alpha/gradiente/borde/highlight
        val palette: ThemePalette,             // origen (para debug y transiciones)
        val isDark: Boolean,
    )
}

data class GlassParams(
    val scrimColor: Int,        // surface adaptativo tras el texto
    val scrimAlpha: Float,      // función de avgLuminance y densidad de texto
    val gradientTopColor: Int, val gradientBottomColor: Int,
    val borderColor: Int, val borderAlpha: Float,
    val highlightColor: Int,    // línea superior 1px (glass premium)
    val cornerRadius: Float
)

enum class ThemeMode { AUTO, MANUAL, AUTO_WITH_OVERRIDES }
```

`LauncherThemeTokens` se extiende con: `textOnAccentColor` (para botones "mi"), `scrimColor/Alpha` (o en GlassParams) y `statusColor` — manteniendo DEFAULT retrocompatible. Overrides: `AdvancedStyleConfig` gana `themeMode: ThemeMode = MANUAL` (retrocompatible: hoy todos los usuarios están en MANUAL) + el significado de sus campos por-elemento cambia a "override" solo cuando mode != MANUAL.

## 6. Integración por página
- El `CompositionLocalProvider(LocalLauncherThemeTokens provides …)` se **mueve de MainActivity al contenido de cada página** del Pager (ViewOne/ViewTwo ya reciben `viewIndex`).
- `background A → theme A; background B → theme B`: cada página resuelve su propio uri; el cambio de página conmuta el set de tokens con **lerp de ~350 ms** (animación por canal ARGB, derivedStateOf del pager offset para anticipar).
- Drawers/diálogos que viven FUERA de las páginas (App Drawer global) usan los tokens de la página activa (activePage).

## 7. Reglas de contraste
- Texto normal ≥ 4.5:1 (reutilizar `MIN_CONTRAST_TEXT_PRIMARY` del generador); secundario ≥ 3:1 recomendado (hoy el generador ya lo enforcement).
- Elementos principales (logo, texto personalizado, reloj, batería, mensajes, fecha, RAM): SIEMPRE `textPrimary` adaptativo (blanco/negro garantizado por ensureContrast sobre el scrim, no sobre el fondo crudo — el scrim garantiza el contraste base).
- **El acento solo para**: highlights, active states, bordes, iconos decorativos. Nunca texto principal por defecto.
- Fondo complejo → scrim/glass adaptativo (GlassParams.scrimAlpha sube con avgLuminance y con la varianza de la paleta) y el texto se contrasta contra el SCRIM (par (texto, scrim) ≥ 4.5:1), no contra el video.

## 8. Política de glass
- **Video: glass simulado premium** — tint adaptativo (surface con alpha de GlassParams) + gradiente vertical (más opaco donde hay texto) + borde 1px + highlight superior + sombra/glow mínima. **Prohibido**: duplicar el video, segundo player, blur por frame.
- **Imágenes estáticas**: blur real PERMITIDO solo como **blur one-shot** (bitmap desenfocado una vez al cargar y cacheado junto al uri; `RenderEffect`/`BlurMaskFilter` — nunca por frame). Evaluación de costo: decodifica+blur ≈ 10-30 ms una vez por cambio de fondo — aceptable; si el fondo cambia por sampling, no re-blurear.
- El scrim adaptativo se dibuja detrás de las zonas de texto (status zone, logo, dock) — no a pantalla completa salvo que avgLuminance sea extremo.

## 9. Política de video
- **NO recalcular paleta por frame.** Sampling: `VideoFrameExtractor.extractFrames` sobre el mismo `videoUri` cada **8 s** (configurable), frame escalado ~64 px, en `Dispatchers.Default`.
- Amortiguación: `ThemePaletteAggregator` sobre las últimas 2-3 muestras; **actualizar tokens solo si** la distancia perceptual (medoid color + luminancia, umbral ΔE-equivalente ya existente en `colorDistanceSq`) supera umbral → transición lerp.
- El sampling corre en un `LaunchedEffect(uri)` por página visible únicamente (la página con prewarm sin mostrarse no samplea), cancelado al salir de la página/background.

## 10. Logo
- Modos: `ICON_ONLY` (default: icono `Icons.Outlined.AutoAwesome`/estrella minimalista), `TEXT_ONLY`, `ICON_PLUS_TEXT`.
- Texto: **máximo 10 caracteres, una línea, sin wrap**; validación en Settings (rechazo explícito con mensaje si excede) y truncado defensivo en runtime (`take(10)` + `maxLines=1` + `softWrap=false`).
- Tipografía: peso medio, letterSpacing 1.5-2 sp, **autoSize** (BasicText autoSize o escala por longitud) y color = `textPrimary` adaptativo (nunca accent).

## 11. Texto (personalizado)
- Usa el mismo token/texto del logo; misma regla de 10 chars/línea/auto-size; contraste contra scrim.

## 12. Status widgets
- Reloj, batería, mensajes, día, fecha: color = `textPrimary`/`textSecondary` adaptativo (jerarquía: hora > fecha/mensajes).
- Batería: color dinámico permitido solo como ESTADO (≤15 % → acento/warning), no como identidad.
- Persistencia: los campos existentes de AdvancedStyleConfig (clockColor, batteryColor…) se reinterpretan como overrides en AUTO_WITH_OVERRIDES y se respetan tal cual en MANUAL.

## 13. RAM
- Widget RAM: `ActivityManager.MemoryInfo` → `totalMem`, `availMem`, `used ≈ total − avail` (+ `%` usado).
- Refresco: cada 2-5 s o en callbacks de componente; sin listeners de alto costo.
- **No confundir con PSS de Nova** — es RAM global del teléfono; documento explícito en el widget/Settings.

## 14. Advanced Settings
- Sección "Tema adaptativo": selector de modo (AUTO/MANUAL/AUTO_WITH_OVERRIDES), toggles por elemento de override (visibles solo en AUTO_WITH_OVERRIDES), configuración de logo (modo/texto/validación), RAM widget on/off, intervalo de sampling de video (avanzado).
- MANUAL conserva el editor actual tal cual (cero regresión).

## 15. Archivos que deberán modificarse
| Archivo | Cambio |
|---|---|
| `LauncherThemeTokens.kt` | extender tokens (textOnAccent, statusColor) retrocompatible |
| `AdaptiveThemeEngine.kt` (nuevo) | resolución por página, caché por uri, sampling video, umbral perceptual |
| `AdaptiveThemeState.kt` + `GlassParams` (nuevo) | modelo §5 |
| `MainActivity.kt` | mover provider global → per-page; proveer AdaptiveThemeState |
| `ViewOne.kt` / `ViewTwo.kt` | consumir tokens por página; glass adaptativo; logo; status; RAM |
| `AdvancedStyleConfig` (LauncherViewModel) | añadir themeMode/logo/RAM (Gson-safe, defaults retro) |
| `AdvancedSettingsScreen.kt` | sección Tema adaptativo + logo + RAM |
| `AppDrawerScreen.kt` | consumir tokens de la página activa |

## 16. Archivos que NO deberán modificarse
`VideoWallpaperManager` · `VideoBackground` · Pager/gesture engine · `WidgetHostManager` · servicios protegidos · Hidden Apps · `MemoryManager` · `NovaApplication` · `ThemeProposalGenerator` (reuso tal cual; si falta un helper se añade aparte, sin duplicar motor).

## 17. Estrategia de pruebas
- Unitarios (puros, sin Compose): contraste de todas las combinaciones de paleta (fuzz con paletas luminancia 0-1) ≥ 4.5:1; resolución AUTO/MANUAL/AUTO_WITH_OVERRIDES; overrides ganan solo cuando corresponde; umbral perceptual (no actualiza bajo ruido); caché por uri; logo truncado a 10; RAM global ≠ PSS.
- Compose/UI: smoke por modo; transición de página con lerp (no salto); contraste visual en paleta extrema (blanco puro / negro puro).
- Física (POCO X6, 120 Hz): batería A-G reutilizada + escenario "video con escenas de luminancia contrastada" (verificar lerp + umbral) + Drawer + retorno 649 ms sin regresión + 0 FATAL.

## 18. Riesgos de rendimiento
1. **Sampling de video**: MediaMetadataRetriever re-abierto por muestra — coste ~50-200 ms en Default dispatcher; mitigación: intervalo ≥ 8 s + solo página visible + cancelación.
2. **Recomposición por tokens**: animar SOLO los tokens que cambian (lerp por canal); derivedStateOf para evitar recomposición de toda la página por 1 token.
3. **Caché de paleta**: clave `uri` (no por frame) → paletas repetidas no re-analizan.
4. **Bitmaps de blur**: one-shot + cache (Memory-L2 mostró que los hardware bitmaps cuestan ~0 en background — mismo principio).
5. **Cold start**: resolución inicial = 1 análisis de imagen (~30-80 ms IO) — hacer lazy tras primer frame (patrón dynamicBeyondViewport) y cacheado.

## 19. Orden exacto de implementación
1. Extender `LauncherThemeTokens` (retrocompatible, DEFAULT intacto) + tests de contraste.
2. `AdaptiveThemeState`/`GlassParams` + `AdaptiveThemeEngine` (modo MANUAL primero = paridad total) + tests.
3. Mover provider por página (MainActivity → Pager) verificando paridad visual pixel-a-pixel en MANUAL.
4. AUTO para fondos-imagen (WallpaperAnalyzer + cache por uri) + umbral.
5. AUTO_WITH_OVERRIDES (reinterpretar campos existentes como overrides) + Settings.
6. Glass adaptativo (scrim/gradiente/borde/highlight) + blur one-shot para imágenes.
7. AUTO para video (sampling 8 s + agregador + lerp).
8. Logo widget (icon/text/mixto + validación 10 chars).
9. Status widgets adaptativos + RAM (ActivityManager.MemoryInfo).
10. Validación física completa (matriz §17) y batería de regresión A-G de memoria.

---

## ESTADO DE IMPLEMENTACIÓN (actualización)

- **Fase 1 IMPLEMENTADA** (ver `UI-1-PHASE1-REPORT.md`): `LauncherThemeTokens` extendido (textOnAccentColor/statusColor con fallbacks), `ThemeMode`, `AdaptiveThemeState`/`PageTheme`/`GlassParams`, `AdaptiveThemeEngine` con MANUAL parity exacta (AUTO/AUTO_WITH_OVERRIDES fail-safe) + normalización Gson de themeMode en loadState.
- **MANUAL parity disponible**: 148/148 tests (13 nuevos), assembleDebug/lintDebug/bundleRelease PASS.
- **AUTO aún NO activado** (fail-safe = MANUAL en Fase 1; conexión del pipeline en Fase 2).
- **UI aún NO migrada**: ViewOne/ViewTwo/Drawer siguen consumiendo AdvancedStyleConfig directamente; cero cambio visual.
