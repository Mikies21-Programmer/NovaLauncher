# UI-1-PHASE4-REPORT — Glass Premium adaptativo + contraste contextual

Fecha: 2026-10-04 · Base: Fase 3 cerrada · POCO X6 5G (Android 16/HyperOS, 120 Hz)

## 1. Implementación

| Archivo | Cambio |
|---|---|
| `theming/AdaptiveThemeState.kt` | `GlassParams` extendido: `gradientAlpha`, `highlightAlpha`, `glowColor`, `glowAlpha`, `adaptive` (flag: AUTO=premium / MANUAL=legacy) |
| `theming/GlassPremium.kt` (nuevo) | `Modifier.glassPremium(glass, shape, topHighlightHeight)` — composición por capas |
| `theming/AdaptiveThemeEngine.kt` | Derivación determinista del scrim (luminancia + complejidad) + `paletteComplexity()` + construcción del glass premium en AUTO (`adaptive=true`) |
| `ui/screens/ViewOne.kt` / `ViewTwo.kt` | Parámetro `glass: GlassParams?`; sidebar consume `glassPremium` cuando `glass.adaptive`, si no el drawing legacy de `AdvancedStyleConfig` |
| `ui/screens/LauncherScreen.kt` | Pasa `pageTheme.value.glass` a ViewOne/ViewTwo |

Zonas protegidas intactas: VideoWallpaperManager, VideoBackground, DynamicBackground, Pager/Gesture
Engine, WidgetHostManager, servicios, Hidden Apps, MemoryManager, NovaApplication.

## 2. Composición del glass premium (capas, en orden de dibujo)

1. **Tint/superficie adaptativa**: `scrimColor @ scrimAlpha` (superficie de lectura generada).
2. **Gradiente vertical con highlight integrado**: stops `0 → highlight@highlightAlpha`,
   `lineFade → scrim@scrimAlpha`, `0.7 → scrim`, `1 → gradientBottom@gradientAlpha*0.4`.
3. **Gradiente inferior sutil**: `0.75 → transparent, 1 → gradientTop@gradientAlpha`.
4. **Borde fino**: `borderColor @ borderAlpha` (0.5 dp).
5. **Highlight superior**: línea de luz con fade de `topHighlightHeight` (1 dp).
6. **Glow externo**: deliberadamente NO dibujado (costo/riesgo de exageración); el highlight
   superior cumple el rol perceptual.

Sin trabajo por frame: los `Brush` son estáticos por composición (recomputan solo al cambiar
tokens/página).

## 3. Fórmula/derivación del scrim

```text
complejidad = (dist(dominant,vibrant) + dist(vibrant,muted)) / 2 / 765   // Manhattan normalizada 0..1
scrimAlpha  = (0.40 + 0.30*complejidad + 0.15*|lum_media - 0.5|).coerceIn(0.40, 0.65)
scrimColor  = surfaceColor de la propuesta generada
```
- Base 0.40: superficie de lectura estable.
- +0.30×complejidad: fondos multicolor exigen más scrim.
- +0.15×desvío de luminancia: extremos oscuros/claros necesitan más superficie para el par de contraste.
- Tope 0.65: nunca opaca el fondo.
- El contraste texto/superficie lo garantiza `ThemeProposalGenerator` (≥ 4.5:1 / ≥ 3:1); el scrim
  (surface @ alpha ≥ 0.40) preserva ese par sobre el fondo real.

## 4. Blur (decisión: DEFER)
El blur one-shot exigiría tocar `DynamicBackground` (zona protegida — es quien renderiza el fondo)
o añadir una segunda decodificación + bitmap residente por fondo. Evaluación: coste 10-30 ms una vez
+ bitmap residente a tamaño de pantalla; el beneficio visual sobre tint+gradiente en fondos sólidos/
suaves es marginal. **Decisión: glass simulado (tint+gradiente+borde+highlight) y blur one-shot
DEFER** — se re-evaluará en la fase de video sampling si el fondo lo justifica.

## 5. Contraste medido (logcat `AdaptiveTheme`, físico)

| Fondo | lum | accent generado | textPrimary | contrastTP | scrimAlpha |
|---|---:|---|---|---|---:|
| Clara | 0.87 | #8A8A8A | #111216 (oscuro) | **17.19:1** | 0.46 |
| Oscura | 0.01 | #466A8D | #FFFFFF | **19.79:1** | 0.47 |
| Saturada | 0.21 | #B55367 | #FFFFFF | **19.21:1** | 0.44 |
| Multicolor | 0.27 | #808880 | #FFFFFF | **18.26:1** | 0.47 |

## 6. Validación física (los 4 fondos de Fase 3, HOME-swap)

- Clara: fondo claro + **sidebar glass claro** con textos oscuros legibles; "mi" en accent muted.
- Oscura: fondo oscuro + **sidebar glass oscuro** con highlight superior sutil visible; textos blancos.
- Saturada/Multicolor: scrim elevado por complejidad; textos legibles.
- Sin clipping, sin flicker, sin pantalla negra, Drawer OK, gestos OK, widgets OK, 0 FATAL.
- Cold start con Fase 4: 1.1-1.2 s (rango normal de la serie; sin regresión atribuible — sin trabajo
  nuevo por frame ni por arranque más allá de construir 1 objeto GlassParams).

## 7. Paridad MANUAL (demostración matemática)

`glassPremium` con `adaptive=false` devuelve el modifier **intacto**; el drawing MANUAL usa el mismo
código legacy. Verificación física: captura MANUAL Fase 4 vs captura Fase 3 (misma config) con
comparación invariante a brillo (el sistema tenía `adjust_brightness_factor=0.5`):
`k=0.5000 exacto, residuo p50=0.00, p95=0.00` sobre 4000 píxeles de la región de fondo →
**misma imagen renderizada, solo difiere el brillo global del sistema**.

## 8. Tests
**178/178 PASS** (171 + 7 nuevos): rangos de todos los alphas · fórmula del scrim determinista
(luminancia+complejidad) · complejidad escala el scrim · textPrimary ≥ 4.5:1 y textSecondary ≥ 3:1
contra el scrim · glass determinista por paleta · MANUAL sin adaptación · cambio de URI no reutiliza
glass. Sin tests de blur (no implementado).

## 9. Regresiones
Ninguna detectada: MANUAL pixel-parity matemática, AUTO legible en los 4 fondos, 0 FATAL/ANR,
geometría/gestos/widgets/drawer intactos.

## 10. Decisión final
**UI-1 PHASE 4 — READY.** Glass Premium adaptativo operando en AUTO (scrim derivado por
luminancia+complejidad, capas tint/gradiente/borde/highlight), MANUAL 100 % paridad, blur real
DEFER (documentado; re-evaluar con video sampling en Fase 5). Sin commit (orden del desarrollador).
