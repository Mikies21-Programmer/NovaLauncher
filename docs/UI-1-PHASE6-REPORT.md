# UI-1-PHASE6-REPORT — Launcher Logo configurable (elemento superior "mi")

Fecha: 2026-10-08 · Base: Fases 1-5 cerradas (7539e09) · POCO X6 5G (Android 16/HyperOS, 120 Hz)

## 1. Objetivo

Convertir el elemento superior del sidebar ("mi") en un **Logo Launcher configurable**
(modo / icono / texto) respetando el sistema de contraste adaptativo, sin tocar los
componentes protegidos.

## 2. Modelo (AdvancedStyleConfig)

```kotlin
val logoMode: LogoMode = LogoMode.ICON_ONLY      // ICON_ONLY | TEXT_ONLY | ICON_PLUS_TEXT
val logoIcon: String  = LogoCatalog.DEFAULT_ICON // "AutoAwesome"
val logoText: String  = ""                        // máx 10 chars, una línea
```

- Persistencia Gson segura: claves String/enum normalizadas defensivamente en
  `loadState` (JSON antiguo sin campos → defaults; enum corrupto → default) —
  mismo patrón que `themeMode` en Fase 1.
- Usuario que nunca configura logo → default correcto sin entrar a Settings
  (ICON_ONLY + AutoAwesome; verificado físicamente en phase6-01).

## 3. Catálogo de iconos (pequeño y estable)

`AutoAwesome` (default), `Star`, `RocketLaunch`, `Bolt`, `Home`, `Apps`, `Palette`,
`Settings` — 8 Material outlined. Claves persistidas como String; clave desconocida
(Gson de futuro/corrupción) degrada al default (test17). Icono personalizado:
**preparado pero no implementado** (la clave String lo admite sin migración).

## 4. Reglas del texto

| Regla | Mecanismo |
|---|---|
| máx 10 caracteres | `logoTextError()` en Settings: error visible "Máximo 10 caracteres", NO guarda |
| una línea / sin wrap | `maxLines=1, softWrap=false` en `LogoAutoText` |
| contador `n/10` | supportingText del campo; rojo si >10 |
| no solo espacios | `logoTextError()` rechaza `"   "`; error visible |
| runtime take(10) | `sanitizeLogoText()` = trim + `take(10)` + blank→"" |
| fallback vacío | TEXT_ONLY con texto vacío/blanco degrada a ICON_ONLY default (test13) |

## 5. Tipografía

- `FontWeight.Medium`, `letterSpacing 1.5 sp`, color = **textPrimary**.
- Auto-size: tamaño inicial por longitud (`≤4→14 sp, 5-7→12 sp, 8-10→10 sp`) +
  reducción progresiva por overflow real (`onTextLayout`, piso 8 sp).
- Prioridad aplicada: legibilidad → una línea → no deformar sidebar. "GABRIELA10"
  (10 chars) cabe en una línea a 10 sp sin tocar la geometría (phase6-08).

## 6. Color — regla absoluta

```
texto principal      → textPrimary           (NUNCA accent)
glifo sobre círculo  → textOnAccentColor     (icono con "texto interno")
círculo (identidad)  → accent
```

- **MANUAL**: círculo = color histórico del usuario (`miButtonColor` vía tokens,
  mismo mapeo que el engine aplica a `elementColors.miButton`); texto/glyph =
  tokens. `miButtonColor`/`miTextColor` NO se interpretan como overrides implícitos
  en AUTO/AUTO_WITH_OVERRIDES (la Fase 3 ya estableció que los defaults históricos
  nunca son overrides).
- **AUTO**: círculo = accent adaptativo muestreado; texto = textPrimary adaptativo.
- **AUTO_WITH_OVERRIDES**: identidad accent como AUTO; override explícito del color
  del logo = trabajo posterior (no existe aún el campo `logoColor` — por diseño,
  no se ha prometido UI).

## 7. Composable `LauncherLogo` (reusable)

`LauncherLogo.kt`: `LauncherLogoSpec` (pura, testeable) + `LauncherLogoResolver`
(puro) + composable `LauncherLogo(spec, onClick)` con 3 layouts:
ICON_ONLY (círculo 45 dp, geometría histórica exacta), TEXT_ONLY (solo texto),
ICON_PLUS_TEXT (círculo 36 dp + texto debajo). Parámetros según diseño: mode+icon
+text+tokens empaquetados en `spec` (resolver inyecta tokens) + onClick.

## 8. UI (ViewOne/ViewTwo — solo zona del logo)

- El Box "mi" 45 dp → `LauncherLogo(...)`; mismo `onSettingsClick`.
- `effMiText` eliminado (ya sin consumidor); resto del archivo intacto.
- `LauncherScreen` pasa `pageTokens = pageTheme.value.tokens` a ambas vistas
  (nuevo parámetro nullable con fallback a tokens de config — paridad legacy).
- NO cambió: ancho sidebar, posiciones, spacing, rotación, status widget, iconos
  inferiores, pager/gestos, glass, video.

## 9. Settings (sección mínima)

Card "Logo del Launcher" en AdvancedSettingsScreen (antes de Colores de Elementos):
chips de modo (Icono/Texto/Ambos), campo de texto con contador+error (solo visible
si modo ≠ ICON_ONLY), selector de 8 iconos. La sección histórica "Botón Logo 'mi'"
se conserva intacta debajo (MANUAL).

## 10. Tests — 17 nuevos (LauncherLogoTest, puros)

Cobertura del gate A-N: A default ICON_ONLY · B default AutoAwesome · C 10 ok /
11 rechazados · D contador/validación · E whitespace-only · F take(10) runtime ·
G MANUAL (círculo histórico) vs AUTO vs AUTO_WITH_OVERRIDES (accent) · H texto =
textPrimary · I accent solo como identidad (glifo y texto ≠ accent) · J ICON_ONLY ·
K TEXT_ONLY · L ICON_PLUS_TEXT · M fallback vacío · N determinismo + extras
(catálogo estable, auto-size por longitud, clave desconocida→default).

**Suite completa: 210/210 PASS** (0 failures, 0 errors, 0 skipped).

## 11. Validación física (POCO X6)

| Caso | Evidencia | Resultado |
|---|---|---|
| ICON_ONLY default (arranque limpio) | `phase6-01` (sesión previa, APK vigente) | círculo + glifo, contraste ok |
| Settings sección logo | `phase6-02` | chips/campo/selector renderizados |
| Campo texto NOVA + contador 4/10 | `phase6-03` | sin error |
| TEXT_ONLY "NOVA" | `phase6-04` | una línea, sin clipping, sidebar intacto |
| ICON_PLUS_TEXT + fondo oscuro imagen | `phase6-05` | accent adaptativo (mauve muestreado), texto blanco |
| 10 chars "GABRIELA10" + contador 10/10 | `phase6-06`, `phase6-08` | 10 sp una línea, 180 px ancho, sin romper geometría |
| 11º char "X" rechazado | `phase6-07` + prefs | error "Máximo 10 caracteres", 11/10 rojo, **persistido = GABRIELA10 (sin X)** |
| Video AUTO | `phase6-09` + logcat | `source=video page=0 tick=0 lum=0.08 accent=#ff757575 textPrimary=#ffffffff` — sampler de Fase 5 intacto, logo correcto sobre video |
| Fondo claro | `phase6-01` | contraste correcto |
| Drawer | `phase6-dump-drawer.xml` | swipe-up abre lista A-Z |
| Gestos página | swipes 0↔1 en sesión | OK (2 páginas) |
| FATAL / ANR | logcat sesión completa | **0 / 0**, pid estable (14951) |
| Restauración | `phase6-10` + prefs | config original restaurada (ICON_PLUS_TEXT+NOVA+AUTO, imagen page 0) |

Nota de sesión: la validación continuó una sesión previa interrumpida (15:18-15:22);
el APK instalado (15:18:22) es posterior al último cambio de código (15:12), por lo
que las capturas 01-04 de aquella sesión son válidas para el código actual.

## 12. Gates

| Gate | Resultado |
|---|---|
| `./gradlew test` | PASS — 210/210 |
| `./gradlew assembleDebug` | PASS |
| `./gradlew lintDebug` | PASS |
| `./gradlew bundleRelease` | PASS |
| `git diff --check` | PASS |

Sin commit (orden del desarrollador). Diff = 5 archivos modificados
(LauncherViewModel, AdvancedSettingsScreen, LauncherScreen, ViewOne, ViewTwo) +
2 nuevos (theming/LauncherLogo.kt, test/LauncherLogoTest.kt) + docs/assets.

## 13. Riesgos

1. **Auto-size por `onTextLayout`**: la reducción progresiva converge (piso 8 sp),
   pero con fuentes del sistema escaladas >1.3× un texto de 10 chars podría quedar
   al piso — aceptable (prioridad: no romper geometría del sidebar).
2. **TEXT_ONLY sin círculo sobre fondos de luminancia media**: textPrimary adaptativo
   ya resuelve contra el scrim del glass (Fase 2-4); sin evidencia de fallo en las
   pruebas, pero es el caso con menor superficie de contraste garantizado.
3. **Icono personalizado** (futuro): la clave String lo admite; el riesgo real será
   el lifecycle de un bitmap/URI persistido — diseñar con cache por clave.
4. **Catálogo**: añadir iconos solo al final de `KEYS` (persistencia por índice no,
   por clave String sí — pero el orden mostrado en Settings es el contrato visual).

## Veredicto

**UI-1 PHASE 6 — READY**
