# NOVALAUNCHER — INFORME CONSOLIDADO DE CIERRE UI-1 FASE 8
## Advanced Settings: Integración del Tema Adaptativo en Pantalla de Ajustes

- **Estado final:** **UI-1 Fase 8 — IMPLEMENTADA, PRUEBAS AUTOMATIZADAS APROBADAS, VALIDACIÓN FÍSICA ACEPTADA POR EL DESARROLLADOR**
- **Fecha de auditoría inicial:** 2026-10-10
- **Implementación (8A, 8B, 8C):** 2026-10-10 / 2026-10-11
- **Ejecución física en hardware (8D):** 2026-10-15
- **Aceptación formal del desarrollador:** 2026-10-17
- **Gates y cierre formal de fase:** 2026-10-17
- **Rol:** Ingeniero Android Sénior, Arquitecto de Software y Auditor Técnico
- **Proyecto Android:** `C:\Users\migue\AndroidStudioProjects\AnimeLauncher`
- **Obsidian Vault:** `C:\Users\migue\AndroidStudioProjects\NovaLauncher Knowledge\NovaLauncher Knowledge`
- **Dispositivo canónico de validación:** POCO X6 5G (`23122PCD1G`, Snapdragon 7s Gen 2, 1220×2712 @ 120 Hz, HyperOS / Android 16)
- **Estado de intervención:** **FASE CERRADA — IMPLEMENTACIÓN COMPLETA, 236/236 PRUEBAS APROBADAS, HARDWARE VALIDADO Y RESTAURADO**

---

## 1. Resumen Ejecutivo

El subsistema de theming de NovaLauncher ha completado exitosamente la **Fase 8 de UI-1 (Advanced Settings: Integración del Tema Adaptativo)**, cerrando la brecha histórica entre el motor de theming en tiempo de ejecución (Fases 1–7) y la interfaz de usuario de configuración (`AdvancedSettingsScreen.kt` y `LauncherViewModel.kt`).

La implementación y validación técnica se articuló en cuatro subfases rigurosas:
1. **8A (Persistencia y defensiva):** Normalización estricta de `AdaptiveColorOverrides` y `ThemeMode` en `LauncherViewModel.kt` (`normalizeLoadedStyle()`) ante cargas con JSON corrupto, nulos explícitos o ausencia de claves legacy, garantizando fail-safe determinista a `MANUAL` y excepciones vacías sin corromper preferencias.
2. **8B (Selector de modo y excepciones):** Selector de `ThemeMode` de 3 chips (`MANUAL`, `AUTO`, `AUTO_WITH_OVERRIDES`) con tarjetas explicativas reactivas y renderizado condicional estricto. Gestión de excepciones de color (`OverrideColorItem`) sobre `clockColor`, `batteryColor`, `messagesColor` y `dateColor`, permitiendo la asignación de presets y el restablecimiento inequívoco a `null` (Automático).
3. **8C (Jerarquía visual de 5 secciones y limpieza):** Reorganización limpia de `AdvancedSettingsScreen.kt` en 5 grupos visuales estructurados: Tema y Modo, Excepciones/Colores, Personalización Glass UI, Logo del Launcher y Widget RAM / Elementos de Vista. Eliminación del control visible `widgetOpacity` (campo muerto auditado en 4C-3B, conservado únicamente en la data class por compatibilidad de serialización).
4. **8D (Validación física y restauración en POCO X6 5G):** Batería P-01 a P-09 ejecutada en hardware real, cubriendo paridad MANUAL, AUTO sobre vídeo vivo, excepciones de color, cold start, reseteo a defaults, estrés con Minecraft (retorno +379 ms, vídeo warm), y restauración íntegra del dispositivo a su estado original verificado.

**Resultados de verificación técnica (2026-10-17):**
- **236/236 pruebas unitarias PASS** (100% de la suite de pruebas unitarias, 0 fallos, 0 errores, 0 omitidos).
- **5 gates de calidad aprobados al 100%:** `testDebugUnitTest`, `assembleDebug`, `lintDebug` (0 errores, 80 advertencias baseline preexistentes), `bundleRelease` (AAB firmado y optimizado con R8) y `git diff --check` limpio.
- **Validación física P-01 a P-09 formalmente aceptada por el desarrollador (2026-10-17)** con registro riguroso de limitaciones metodológicas y del entorno HyperOS.
- **Dispositivo POCO X6 5G restaurado y verificado:** Preferencias semánticamente idénticas al respaldo original (`MANUAL`, widget RAM apagado, logo `ICON_ONLY`, fondos originales 248/249, bóveda de Apps Ocultas intacta).

---

## 2. Estado Real de Git y Commits

Verificación ejecutada mediante herramientas Git en `C:\Users\migue\AndroidStudioProjects\AnimeLauncher`:

### A. Historial de commits reciente (`git log -10 --oneline --decorate`)
```text
16eea76 (HEAD -> main) feat: add global device RAM widget             [LOCAL - Fase 7 UI-1]
f4c72a0 feat: add configurable launcher logo                           [LOCAL - Fase 6 UI-1]
7539e09 feat: add adaptive video theme sampling                        [LOCAL - Fase 5 UI-1]
08340c6 feat: add adaptive glass premium                               [LOCAL - Fase 4 UI-1]
35669b5 feat: enable adaptive colors for image backgrounds             [LOCAL - Fase 3 UI-1]
e6f97ea feat: scope adaptive theme per launcher page                   [LOCAL - Fase 2 UI-1]
ec49dc8 feat: add adaptive theme runtime core                          [LOCAL - Fase 1 UI-1]
05ef434 (origin/main, origin/HEAD) docs: close memory l2 audit         [REMOTO - Último push]
3e312fc perf: add process memory trim handling                         [REMOTO - Memory L1]
32db2be security: isolate Hidden Apps from Android backup              [REMOTO - Seguridad]
```

### B. Relación entre HEAD y origin/main
- **Rama activa:** `main`.
- **HEAD local:** `16eea76da84fead3ce5f60b3be07bcd6f3c246fe`.
- **origin/main:** `05ef43445582f3ef2930777e487fb69ff46c827c`.
- **Divergencia:** El repositorio local está exactamente **7 commits por delante de origin/main** (`ec49dc8` .. `16eea76`).
- **Directiva:** NO ejecutar `git push`, NO crear tags, NO alterar el historial sin orden explícita del desarrollador.

### C. Estado del Working Tree (`git status`)
- Archivos fuente (`app/src/`, `core/`, `domain/`, etc.): **Completamente limpios (0 modificaciones)**.
- Archivos en `benchmark/build/`: Existen diferencias no comiteadas y archivos no rastreados exclusivamente dentro del directorio de compilación de benchmarks (`benchmark/build/`), generado en compilaciones históricas donde parte de la carpeta quedó rastreada por Git. `git status --short -- ':!benchmark/build'` produce salida vacía.
- `git diff --check`: 0 errores en código fuente del proyecto.

> **⚠️ ACTUALIZACIÓN 2026-10-11:** este punto 2.C describe el estado al cierre de la auditoría original y **ya no está vigente**. Tras la autorización de implementación, el working tree contiene ahora el trabajo de las subfases 8A-8C **sin commit** (2 archivos modificados + 1 test nuevo sin rastrear) y la 8D quedó interrumpida a mitad. Estado verificado y desglosado en **§13**.

---

## 3. Inventario de Controles Actuales

### Pantalla: `AdvancedSettingsScreen.kt`

| # | Elemento UI | Control Compose | Configuración leída/escrita | Valores / Rango | Consumo en Renderizado |
|---|---|---|---|---|---|
| 1 | TopBar: Volver | `IconButton` (ArrowBack) | N/A (`onBack()`) | Acción | Navegación hacia Settings |
| 2 | TopBar: Reset | `IconButton` (Refresh) | `handleReset` | Acción | Invoca `resetStyleConfig()` |
| 3 | Banner Reset | `Button("Restaurar")` | `handleReset` | Acción | Restaura `AdvancedStyleConfig()` por defecto |
| 4 | Color Acento Neón | `ColorSettingItem` | `accentColor` | Hex (10 presets) | MANUAL: alimenta `tokens.accentColor` y Glass |
| 5 | Transp. Paneles Glass | `SliderSettingItem` | `panelTransparency` | 0.20f .. 1.00f (def 0.88f) | Alimenta `GlassCard` en ajustes |
| 6 | Brillo Borde Glass | `SliderSettingItem` | `glassBorderAlpha` | 0.05f .. 0.60f (def 0.25f) | `GlassCard` y `GlassParams.borderAlpha` |
| 7 | Redondeo de Esquinas | `SliderSettingItem` | `cornerRadius` | 8f .. 32f (def 16f) | `GlassCard` y `GlassParams.cornerRadius` |
| 8 | Opacidad de Widgets | `SliderSettingItem` | `widgetOpacity` | 0.20f .. 1.00f (def 0.90f) | **GHOST / DEAD CODE** (0 consumidores UI) |
| 9 | Modo del Logo | 3 Chips (Icon/Text/Both) | `logoMode` | `LogoMode` enum | `LauncherLogoResolver` en ViewOne/ViewTwo |
| 10| Texto del Logo | `OutlinedTextField` | `logoText` | String (máx 10 chars) | `LauncherLogo` en ViewOne/ViewTwo |
| 11| Icono del Logo | Fila de 8 cajas | `logoIcon` | Claves `LogoCatalog.KEYS` | `LauncherLogo` en ViewOne/ViewTwo |
| 12| Indicador RAM Global | `Switch` | `showRamWidget` | Boolean (def false) | Monta `RealTimeRam` en ViewOne/ViewTwo |
| 13| Botón 'mi' (Fondo) | `ColorSettingItem` | `miButtonColor` | Hex (10 presets) | MANUAL: fondo círculo en `LauncherLogo` |
| 14| Texto 'mi' | `ColorSettingItem` | `miTextColor` | Hex (10 presets) | MANUAL: glifo en `LauncherLogo` |
| 15| Reloj / Hora | `ColorSettingItem` | `clockColor` | Hex (10 presets) | MANUAL: `RealTimeClock` |
| 16| Batería | `ColorSettingItem` | `batteryColor` | Hex (10 presets) | MANUAL: `RealTimeBattery` |
| 17| Texto Mensajes | `ColorSettingItem` | `messagesColor` | Hex (10 presets) | MANUAL: Contador de notificaciones |
| 18| Fecha y Día | `ColorSettingItem` | `dateColor` | Hex (10 presets) | MANUAL: Triángulo View 1 |
| 19| Opacidad Barra Lateral | `SliderSettingItem` | `sidebarOpacity` | 0.25f .. 1.00f (def 0.88f) | MANUAL: Sidebar alpha |
| 20| Color Barra Lateral | `ColorSettingItem` | `sidebarColor` | Hex (10 presets) | MANUAL: Sidebar fondo |
| 21| Opacidad Barra Diag. | `SliderSettingItem` | `diagonalBarOpacity` | 0.25f .. 1.00f (def 0.96f) | MANUAL: Barra diagonal alpha |
| 22| Color Barra Diag. | `ColorSettingItem` | `diagonalBarColor` | Hex (10 presets) | MANUAL: Barra diagonal fondo |
| 23| Tamaño Triángulo | `SliderSettingItem` | `triangleWidth` | 0.65f .. 1.00f (def 0.88f) | ViewTwo: Escala geométrica triángulo |
| 24| Opacidad Triángulo | `SliderSettingItem` | `triangleOpacity` | 0.25f .. 1.00f (def 0.85f) | ViewTwo: Triángulo alpha |
| 25| Color Acento Triángulo | `ColorSettingItem` | `triangleColor` | Hex (10 presets) | ViewTwo: Triángulo color |
| 26| Opacidad Fondo Cajón | `SliderSettingItem` | `appDrawerBgOpacity` | 0.30f .. 1.00f (def 0.75f) | `AppDrawerScreen`: fondo alpha |
| 27| Color Fondo Cajón | `ColorSettingItem` | `appDrawerBgColor` | Hex (10 presets) | `AppDrawerScreen`: fondo color |
| 28| Color Texto Cajón | `ColorSettingItem` | `appDrawerTextColor` | Hex (10 presets) | `AppDrawerScreen`: títulos apps |
| 29| Iconos Personalizados | `ColorSettingItem` | `customIconColor` | Hex (10 presets) | `AppDrawerScreen`: iconos por defecto |

---

## 4. Matriz de Trazabilidad Técnica

| Funcionalidad | Clase / Archivo Fuente | Persistencia / Key | Control Visible | Cobertura de Tests | Estado Arquitectónico |
|---|---|---|---|---|---|
| **ThemeMode** | `theming/ThemeMode.kt`, `LauncherViewModel.kt` | `launcher_style_config` (`themeMode`) | **NO** (Sin control UI) | `AdaptiveThemeEngineTest` (test01, test09), `AdaptiveVideoSamplerTest` | Motor listo, UI ausente |
| **Adaptive Overrides** | `theming/AdaptiveThemeState.kt` (`AdaptiveColorOverrides`) | `launcher_style_config` (`adaptiveOverrides`) | **NO** (Sin control UI) | `AdaptiveThemeAutoTest` (L193, L208, L373), `AdaptiveVideoSamplerTest` (L160) | Motor listo, UI ausente |
| **Pipeline Imagen AUTO** | `theming/AdaptiveThemeEngine.kt`, `WallpaperAnalyzer.kt` | En memoria + LRU por URI (12 entradas) | Indirecto (vía fondos) | `AdaptiveThemeAutoTest` (13 tests) | Completo y verificado |
| **Pipeline Vídeo AUTO** | `theming/AdaptiveVideoSampler.kt`, `VideoPaletteGate.kt` | En memoria (bucle 8 s, ventana 3 muestras) | Indirecto (vía fondos) | `AdaptiveVideoSamplerTest` (8 tests) | Completo y verificado |
| **Glass Premium** | `theming/GlassPremium.kt`, `theming/GlassParams` | En memoria (`PageTheme.glass`) | Parcial (solo alphas manuales) | `AdaptiveThemeEngineTest`, validación física POCO X6 | Completo y verificado |
| **Launcher Logo** | `theming/LauncherLogo.kt` | `launcher_style_config` (`logoMode`, `logoIcon`, `logoText`) | **SÍ** (`AdvancedSettingsScreen.kt`) | `LauncherLogoTest` (17 tests) | Completo y verificado |
| **Widget RAM Global** | `ram/RamMonitor.kt`, `ui/components/RealTimeRam.kt` | `launcher_style_config` (`showRamWidget`) | **SÍ** (Switch en `AdvancedSettingsScreen.kt`) | `RamMonitorTest` (16 tests gates A-R) | Completo y verificado |
| **Tokens por Página** | `theming/AdaptivePageTheme.kt`, `LauncherScreen.kt` | En memoria (`LocalLauncherThemeTokens`) | N/A (Arquitectura interna) | `AdaptiveThemeEngineTest` (test07, test08) | Completo y verificado |
| **Contraste WCAG** | `theming/ThemeProposalGenerator.kt` | N/A (Algoritmo puro) | N/A (Autoridad matemática) | `ThemeProposalGeneratorTest` (WCAG 4.5:1) | Autoridad intocable |
| **Persistencia Gson** | `LauncherViewModel.kt` (`loadState`, `persistStyleConfig`) | `SharedPreferences` (`launcher_style_config`) | N/A (Capa de datos) | `AdaptiveThemeEngineTest` (test09, roundTrip) | Estable con salvedad menor |

---

## 5. Brechas Verificadas para la Fase 8

1. **Ausencia total de control para `ThemeMode`:**
   - En disco: `themeMode: ThemeMode = ThemeMode.MANUAL` en `AdvancedStyleConfig`.
   - En UI: Ni `SettingsScreen.kt` ni `AdvancedSettingsScreen.kt` exponen un selector de modo. El usuario no puede alternar entre `MANUAL`, `AUTO` y `AUTO_WITH_OVERRIDES`.
2. **Desconexión de `AdaptiveColorOverrides`:**
   - La estructura `AdaptiveColorOverrides` solo admite 4 campos anulables: `clockColor`, `batteryColor`, `messagesColor`, `dateColor`.
   - La UI actual sólo ofrece selectores que modifican `currentStyle.clockColor`, `currentStyle.batteryColor`, etc.
   - Si la app operara en `AUTO_WITH_OVERRIDES`, modificar estos controles manuales **no tiene ningún efecto** porque el motor busca `styleConfig.adaptiveOverrides.<field>`.
   - No existe un mecanismo para restablecer una excepción (pasar a `null` para que vuelva a adoptar el color automático).
3. **Contradicción conceptual de controles de color en modo `AUTO`:**
   - Si el usuario está en `AUTO`, los colores de texto y acento se derivan matemáticamente del fondo. Mostrar una lista de selectores de color manuales fijos en este modo resulta confuso porque dichos selectores son ignorados por el motor.
4. **Falta de normalización defensiva en `loadState()` para `adaptiveOverrides`:**
   - En `LauncherViewModel.kt` (líneas 344–352), se normalizan defensivamente `themeMode`, `logoMode`, `logoIcon`, `logoText` y `showRamWidget`. Sin embargo, `adaptiveOverrides` no tiene un fallback explícito `loadedStyle.adaptiveOverrides ?: AdaptiveColorOverrides()`.
   - **CORRECCIÓN 8A (verificada experimentalmente):** la omisión del campo en un JSON antiguo NO produce null (el constructor no-arg de `AdvancedStyleConfig` preserva los defaults de los campos ausentes — comprobado por `AdaptiveThemeEngineTest.test09`). La vía real de exposición es un **null EXPLÍCITO** en el JSON (prefs restauradas de backup / editadas a mano; la Gson propia de la app nunca serializa nulls). En ese caso la NPE no llega al engine: revienta **dentro de `copy()` mismo** (`copy$default` pasa el contenedor null al parámetro no-nulo), el `catch` genérico de `loadState()` la traga y **se descarta silenciosamente TODA la configuración de estilo del usuario en cada arranque en frío**. Impacto mayor al estimado en esta auditoría.
5. **Control fantasma no funcional (`widgetOpacity`):**
   - El slider de opacidad de widgets (líneas 294–305 de `AdvancedSettingsScreen.kt`) sigue presente en la UI pero tiene 0 consumidores en el renderizado (auditado y diferido en Fase 4C-3B). Ocupa espacio visual innecesario en la sección Glass.
6. **Desconexión de `LocalLauncherThemeTokens` dentro de `AdvancedSettingsScreen`:**
   - En `AdvancedSettingsScreen.kt` (líneas 148–155), `LocalLauncherThemeTokens` se provee mediante `LauncherThemeTokens.fromAdvancedStyleConfig(currentStyle)`. Esto siempre sintetiza tokens de estilo MANUAL, impidiendo que la pantalla de ajustes previsualice los tokens adaptativos en tiempo real si el usuario selecciona `AUTO`.

---

## 6. Riesgos y Componentes Protegidos

En estricta observancia de `00_CORE/Protected_Functionality.md`, `00_CORE/Risks.md` y los contratos arquitectónicos del proyecto:

### 🔴 ZONAS ROJAS — PROHIBIDO ALTERAR
1. **Pipeline de Vídeo (`VideoWallpaperManager`, `VideoBackground`):**
   - Pool fijo de 2 `ExoPlayer` (`slotA`/`slotB`).
   - Pausa obligatoria antes de `clearVideoSurface()`.
   - Prohibido `onReset` en `AndroidView` (eliminado en V-04).
   - El widget de RAM o los ajustes de tema **bajo ninguna circunstancia** deben invocar liberaciones o recreaciones de players.
2. **Ciclo de Vida y Detección de Gestos (`LauncherScreen.kt`):**
   - Cadena de detectores `pointerInput` en `PointerEventPass.Initial` (Swipe vertical, Pinch-in, Spread).
   - Bounds de widgets no reactivos vía `ConcurrentHashMap` con getter `() -> List<Rect>`.
   - `BackHandler(enabled = true)` permanente en Home.
3. **Hidden Apps (Seguridad):**
   - Entrada exclusiva por gesto Spread central.
   - Bóveda Keystore AES-GCM-256 aislada en `nova_hidden_apps_prefs.xml`.
   - Prohibido exponer accesos directos o configuraciones de Hidden Apps en la pantalla de ajustes avanzados.
4. **Autoridad Matemática de Contraste (`ThemeProposalGenerator`):**
   - Prohibido duplicar la lógica de contraste WCAG 2.1 o recalcular ratios ad-hoc en Compose.
   - La regla central se mantiene inviolable: **EL COLOR DE ACENTO JAMÁS ES EL COLOR DE TEXTO PRINCIPAL EN AUTO**.
5. **Monitor de Memoria del Sistema (`MemoryManager`, `NovaApplication.onTrimMemory`):**
   - No alterar el canal de recepción de eventos de memoria ni vincular la pantalla de ajustes a liberaciones forzadas.

### 🟡 ZONAS AMARILLAS — IMPACTO COLATERAL CONTROLADO
1. **`LauncherViewModel.persistStyleConfig`:**
   - La serialización debe mantenerse fuera del hilo principal (`Dispatchers.IO`).
   - La reactividad durante el arrastre de sliders o cambios continuos debe usar `updateStyleConfigTransient` en memoria sin I/O en disco.
2. **Compatibilidad Gson hacia atrás:**
   - No renombrar campos de `AdvancedStyleConfig`.
   - Respetar los valores por defecto del constructor sin argumentos.

---

## 7. Propuesta de Organización de la Interfaz (Fase 8)

Se propone reestructurar `AdvancedSettingsScreen.kt` en **5 secciones lógicas y jerárquicas**, eliminando la duplicación y ofreciendo una experiencia clara y cyberpunk:

```
┌────────────────────────────────────────────────────────┐
│ ⚙️ Opciones Avanzadas de Diseño                         │
│ [Restaurar Diseño Original] (Banner superior de reset) │
├────────────────────────────────────────────────────────┤
│ 1. MODO DE TEMA                                        │
│    [ MANUAL ]  [ AUTO ]  [ AUTO CON EXCEPCIONES ]      │
│    (Selector tipo chips con descripción contextual)    │
├────────────────────────────────────────────────────────┤
│ 2. COLORES Y PERSONALIZACIÓN                           │
│    - Si MANUAL: Paleta completa tradicional            │
│    - Si AUTO: Banner informativo de contraste adaptativo│
│    - Si AUTO_WITH_OVERRIDES: Panel de excepciones      │
│      • Reloj: [Auto] o [#Color] [x Restablecer]        │
│      • Batería: [Auto] o [#Color] [x Restablecer]      │
│      • Mensajes: [Auto] o [#Color] [x Restablecer]     │
│      • Fecha: [Auto] o [#Color] [x Restablecer]        │
├────────────────────────────────────────────────────────┤
│ 3. EFECTO CRISTAL (GLASS PREMIUM)                      │
│    - Redondeo de esquinas (8 - 32 dp)                  │
│    - Brillo de contorno cristalino (5% - 60%)          │
│    - Transparencia de paneles Glass (20% - 100%)       │
│    - Opacidad de barra lateral (en MANUAL)             │
│    (Se retira o etiqueta como diferido widgetOpacity)  │
├────────────────────────────────────────────────────────┤
│ 4. LOGO DEL LAUNCHER (BARRA LATERAL)                   │
│    - Modo: [ Icono ]  [ Texto ]  [ Ambos ]             │
│    - Texto personalizado (máx 10 chars, validación)    │
│    - Selector de icono (8 glifos de LogoCatalog)       │
├────────────────────────────────────────────────────────┤
│ 5. INDICADORES Y BARRAS DEL LAUNCHER                   │
│    - Switch: Monitor global de memoria RAM (sidebar)   │
│    - Ajustes específicos de barra diagonal y cajón     │
└────────────────────────────────────────────────────────┘
```

### Detalle por Grupo de Configuración:

#### Grupo 1: Modo de Tema (`ThemeMode`)
- **Controles:** Selector horizontal de 3 opciones (chips/pills): `MANUAL` ("Manual"), `AUTO` ("Automático"), `AUTO_WITH_OVERRIDES` ("Auto + Excepciones").
- **Estado leído/modificado:** `currentStyle.themeMode`.
- **Comportamiento:**
  * Al pulsar un chip, actualiza `currentStyle.themeMode` y persiste de forma inmediata.
  * Muestra una breve tarjeta explicativa debajo del modo seleccionado:
    - *Manual:* "Los colores provienen estrictamente de tu configuración manual fija."
    - *Automático:* "Los colores se adaptan en tiempo real al fondo de cada pantalla garantizando contraste WCAG."
    - *Auto + Excepciones:* "Colores automáticos según el fondo, con personalizaciones explícitas para elementos clave."
- **Casos límite:** Selección de modo sin fondo configurado (el motor ya degrada a `ThemePalette.DEFAULT`).

#### Grupo 2: Colores y Excepciones
- **Controles según el modo activo:**
  * **Si `themeMode == MANUAL`:** Se muestran los `ColorSettingItem` tradicionales (Reloj, Batería, Mensajes, Fecha, Botón Logo, Barra Lateral, etc.). Modifican `currentStyle.<elemento>`.
  * **Si `themeMode == AUTO`:** Se ocultan los selectores individuales de texto y se muestra una `GlassCard` informativa indicando que los colores se calculan dinámicamente según el wallpaper de cada página (17:1+ de contraste verificado), evitando el anti-patrón de acento como texto.
  * **Si `themeMode == AUTO_WITH_OVERRIDES`:** Se presenta la lista de los 4 elementos compatibles: **Reloj**, **Batería**, **Mensajes** y **Fecha**.
    - Cada fila muestra: Título del elemento, estado actual (badge "Automático" en cian tenue si es `null`, o swatch del color personalizado si tiene valor), selector de color (10 presets) y botón discreto "Restablecer" (`IconButton` con icono `Close` o texto "Auto") que vuelve a fijar la propiedad en `null`.
- **Estado leído/modificado:** `currentStyle.adaptiveOverrides` (`clockColor`, `batteryColor`, `messagesColor`, `dateColor`).
- **Validaciones:** Solo se aceptan cadenas Hex válidas (#RRGGBB); el valor por defecto/reset siempre es `null`.

#### Grupo 3: Apariencia del Cristal (`GlassParams`)
- **Controles:**
  * Slider "Redondeo de Esquinas" (`cornerRadius`, 8..32 dp).
  * Slider "Brillo de Borde Glass" (`glassBorderAlpha`, 5%..60%).
  * Slider "Transparencia de Paneles Glass" (`panelTransparency`, 20%..100%).
  * En modo `MANUAL`: Slider "Opacidad de Barra Lateral" (`sidebarOpacity`, 25%..100%). (En `AUTO`, el scrim se calcula determinísticamente por complejidad de fondo).
  * **Depuración de `widgetOpacity`:** Ocultar o marcar como "Próximamente" sin generar falsas expectativas al usuario.
- **Componentes reutilizados:** `SliderSettingItem`.

#### Grupo 4: Logo del Launcher
- **Controles:** Mantener la implementación validada de la Fase 6 (`LogoMode` chips, `OutlinedTextField` con contador y validación `logoTextError`, y cuadrícula de 8 iconos de `LogoCatalog`).
- **Estado leído/modificado:** `currentStyle.logoMode`, `currentStyle.logoText`, `currentStyle.logoIcon`.

#### Grupo 5: Indicadores del Launcher (RAM y Elementos)
- **Controles:**
  * Tarjeta con `Switch` para "Indicador de RAM (global)" (`showRamWidget`).
  * Subsección con sliders y colores de la barra diagonal, triángulo inferior y cajón de aplicaciones (estos últimos preservados para garantizar compatibilidad con View 2).

---

## 8. Lista de Archivos que Requerirían Cambios en la Implementación

| Archivo | Motivo del Cambio | Tipo de Cambio |
|---|---|---|
| `app/src/main/java/com/daybreak/animelauncher/ui/screens/AdvancedSettingsScreen.kt` | Reestructuración de la pantalla en los 5 grupos; integración del selector de `ThemeMode`; integración del panel de excepciones para `AdaptiveColorOverrides`; condicionalidad de controles según el modo. | UI / Compose |
| `app/src/main/java/com/daybreak/animelauncher/LauncherViewModel.kt` | Añadir normalización defensiva en `loadState()` para `adaptiveOverrides`: `loadedStyle.adaptiveOverrides ?: AdaptiveColorOverrides()`. | Capa de datos / Deserialización |
| `app/src/main/res/values/strings.xml` | Strings localizados en español para los nuevos títulos, descripciones de modos de tema y botones de reseteo de excepciones. | Recursos |
| `app/src/main/res/values-en/strings.xml` | Strings localizados en inglés equivalentes para soporte bilingüe completo. | Recursos |
| `app/src/test/java/com/daybreak/animelauncher/theming/AdaptiveThemeSettingsTest.kt` | Nueva suite de pruebas unitarias cubriendo la lógica de selección de modo, creación, actualización y reseteo de excepciones, y persistencia Gson. | Tests Unitarios |

*Nota:* `AdaptiveThemeEngine.kt`, `AdaptivePageTheme.kt`, `RamMonitor.kt`, `LauncherLogo.kt`, `VideoWallpaperManager.kt` y `LauncherScreen.kt` **NO requieren modificaciones**, ya que sus contratos y APIs están completamente preparados y validados.

---

## 9. Plan de Implementación por Subfases Pequeñas

Para mantener el control estricto de regresiones y minimizar riesgos:

- **Subfase 8A: Robustecimiento de Modelo y Deserialización Defensiva — ✅ IMPLEMENTADA (2026-10-10)**
  - Normalización defensiva de `adaptiveOverrides` aplicada vía nueva función pura `normalizeLoadedStyle()` (extraída de `loadState()` para testeabilidad; sin cambio de contrato de datos).
  - `AdaptiveThemeSettingsTest` creada: 5 tests (JSON antiguo sin campo, null explícito, round-trip de excepciones, reset individual a `null`, preservación de modo/valores + enum corrupto → MANUAL).
  - Evidencia pre-fix: 1 fallo (NPE en `copy()` con null explícito) → post-fix **231/231 PASS**; `assembleDebug` PASS; `git diff --check` limpio.
  - *Gate:* 226+ tests PASS — **CUMPLIDO (231/231)**. UI intacta.

- **Subfase 8B: Componentes de UI para ThemeMode y Excepciones — ✅ IMPLEMENTADA (2026-10-10)**
  - Selector accesible de `ThemeMode` implementado en `AdvancedSettingsScreen.kt` con 3 chips (`MANUAL`, `AUTO`, `AUTO_WITH_OVERRIDES`), descripciones contextuales reactivas y persistencia inmediata vía `onPersist(currentStyle.copy(themeMode = ...))`.
  - Componente composable reutilizable `OverrideColorItem` implementado con indicador visual de estado (badge "Automático" / swatch HEX), selector de 10 colores preset, y acción explícita de reseteo a `null` ("Restablecer" / "Auto").
  - Panel de excepciones integrado para los cuatro campos existentes: `clockColor`, `batteryColor`, `messagesColor` y `dateColor`, actualizados de forma inmutable sin perder las demás excepciones.
  - `AdaptiveThemeSettingsTest` ampliada de 5 a 10 pruebas unitarias cubriendo: transición entre los 3 modos con preservación de colores manuales y excepciones, asignación individual/conjunta de las 4 excepciones, restablecimiento independiente a `null` sin mutar los otros 3 campos, semántica de excepciones vacías por defecto y serialización/deserialización round-trip completa.
  - *Resultados:* **236/236 unit tests PASS** (10/10 en `AdaptiveThemeSettingsTest`), `assembleDebug` exitoso en 6s, `lintDebug` 0 errores (80 warnings preexistentes de baseline), `git diff --check` limpio (0 errores de espacios en blanco). UI previa no reorganizada (pendiente para 8C).

- **Subfase 8C: Reorganización y Condicionalidad de AdvancedSettingsScreen — ✅ IMPLEMENTADA (2026-10-10)**
  - Reestructuración completa del árbol `LazyColumn` en los cinco grupos visuales jerárquicos:
    1. **1. Modo de Tema:** Selector por chips accesible (`MANUAL`, `AUTO`, `AUTO_WITH_OVERRIDES`) con tarjeta explicativa dinámica.
    2. **2. Colores y Personalización:** Condicional estricto por `currentStyle.themeMode`:
       - En `MANUAL`: Se presentan los selectores tradicionales (`miButtonColor`, `miTextColor`, `clockColor`, `batteryColor`, `messagesColor`, `dateColor`) preservando el contrato histórico y persistencia.
       - En `AUTO`: Se ocultan los selectores manuales de texto/iconos; se muestra tarjeta explicativa de cálculo adaptativo desde wallpaper con contraste WCAG (17:1+) y se aclara que las excepciones se conservan para cuando se use el modo correspondiente.
       - En `AUTO_WITH_OVERRIDES`: Se muestran los 4 `OverrideColorItem` (`clockColor`, `batteryColor`, `messagesColor`, `dateColor`) con badge visual `[Automático]` vs swatch HEX, presets y botón directo de restablecimiento a `null`, advirtiendo con precisión que colores manuales pueden reducir la legibilidad sobre ciertos fondos.
    3. **3. Apariencia del Cristal (Glass):** `accentColor`, `panelTransparency` (con alcance delimitado a paneles de ajustes), `glassBorderAlpha` (intensidad del contorno cristalino) y `cornerRadius` (8..32 dp). El slider fantasma `widgetOpacity` fue retirado de la UI sin alterar el campo persistido en `AdvancedStyleConfig`.
    4. **4. Logo del Launcher:** Conserva los modos `ICON_ONLY`, `TEXT_ONLY`, `ICON_PLUS_TEXT`, caja de texto con validación y contador `$count/10`, y catálogo de 8 iconos.
    5. **5. Indicadores y Barras:** Switch de RAM global (delimitado a la memoria RAM total/usada por todo el dispositivo), subsección de barras (lateral y diagonal) y subsección de Vista 2 (triángulo y cajón de apps).
  - Decisión sobre el tema visual de la propia pantalla de ajustes ratificada: Se conserva `LauncherThemeTokens.fromAdvancedStyleConfig(currentStyle)` en `AdvancedSettingsScreen` garantizando máxima legibilidad técnica en Obsidian/Cian sin duplicar el motor adaptativo ni sobrecargar análisis en menús de configuración.
  - *Resultados de Gates:*
    - `testDebugUnitTest`: **236/236 PASS** (100% éxito, 0 fallos, 0 omitidos).
    - `assembleDebug`: **BUILD SUCCESSFUL** en 6s.
    - `lintDebug`: **BUILD SUCCESSFUL** en 1m 40s (0 errores, 80 warnings baseline, 6 hints).
    - `bundleRelease`: **BUILD SUCCESSFUL** en 2m 3s (R8 minify, Proguard, signReleaseBundle).
    - `git diff --check`: **Limpio (Código 0)**.

- **Subfase 8D: Validación Física en POCO X6 5G y Cierre**
  - Ejecución de la batería de pruebas en hardware real (cambio de modos, persistencia cold start tras `am force-stop`, creación/eliminación de excepciones, verificación de no-regresión en MANUAL y retorno desde juegos).
  - Confirmación explícita del desarrollador y registro en el Ledger.
  - **PROGRESO 2026-10-10/11 (INTERRUMPIDA A MITAD — ver §13):** intento del 10-10 (evidencia en `docs/evidence_p8/`) y reanudación el 11-10 19:48-20:23 (respaldo `docs/assets/phase8-original-prefs.xml` + capturas `phase8-00-estado-inicial.png`, `phase8-p01-*` ×7, `phase8-p02-*` ×2). Ejecutadas con evidencia capturada pero **SIN aceptación del desarrollador**: P-01 (recorrido completo de la nueva pantalla de ajustes + retorno a Home) y P-02 (selección de AUTO + Home tras AUTO). **Pendientes: P-03..P-09 y aceptación física explícita.** El dispositivo quedó con `launcher_style_config.themeMode = "AUTO"` (verificado por `run-as` el 11-10): la restauración final (P-09) NO se ha ejecutado; el respaldo original (MANUAL) está intacto en `docs/assets/phase8-original-prefs.xml`.
  - **✅ COMPLETADA 2026-10-15 (ver §14):** P-02(vídeo)..P-09 ejecutadas con evidencia objetiva; dispositivo restaurado y verificado (MANUAL, RAM off, logo original, excepciones limpias, fondos 248/249, Hidden Apps intacto). Aceptación visual del desarrollador PENDIENTE (regla de oro: el agente no declara PASS físico por su cuenta).

---

## 10. Matriz de Regresión Automatizada y Física

### A. Pruebas Automatizadas Unitarias (JVM)
| ID | Descripción de la Prueba | Tipo | Archivo / Suite | Estado |
|---|---|---|---|---|
| U-01 | Suite base completa (Startup, Drawer, Memory, Notificaciones, etc.) | Regresión | Múltiples suites (107 tests) | **EXISTENTE (PASS)** |
| U-02 | Integración y contratos de Hidden Apps | Regresión | `hiddenapps/` (6 suites) | **EXISTENTE (PASS)** |
| U-03 | Memory L1 trim handling | Regresión | `MemoryManagerTest` (6 tests) | **EXISTENTE (PASS)** |
| U-04 | Core y resolución de Adaptive Theme (Fases 1 y 2) | Regresión | `AdaptiveThemeEngineTest` (10 tests) | **EXISTENTE (PASS)** |
| U-05 | Pipeline AUTO con imágenes y contraste WCAG (Fase 3) | Regresión | `AdaptiveThemeAutoTest` (13 tests) | **EXISTENTE (PASS)** |
| U-06 | Sampling adaptativo de vídeo amortiguado (Fase 5) | Regresión | `AdaptiveVideoSamplerTest` (8 tests) | **EXISTENTE (PASS)** |
| U-07 | Logo configurable y validación de texto (Fase 6) | Regresión | `LauncherLogoTest` (17 tests) | **EXISTENTE (PASS)** |
| U-08 | Monitor global de RAM y ciclo de vida de polling (Fase 7) | Regresión | `RamMonitorTest` (16 tests gates A-R) | **EXISTENTE (PASS)** |
| U-09 | Normalización defensiva de `adaptiveOverrides` en Gson | Nueva | `AdaptiveThemeSettingsTest` (test01..test05) | **8A: 5 tests PASS** |
| U-10 | Mutación atómica de excepciones, ThemeMode, reset individual y round-trip | Nueva | `AdaptiveThemeSettingsTest` (test06..test10) | **8B: 5 tests PASS (236/236 total)** |

### B. Pruebas Físicas en Hardware Real (POCO X6 5G)
| ID | Escenario de Prueba Física | Procedimiento en Hardware Real | Aceptación Requerida |
|---|---|---|---|
| P-01 | **Paridad de MANUAL intacta** | Configurar modo `MANUAL`; verificar que todos los elementos conservan exactamente la apariencia previa. | Desarrollador — *EJECUTADA 2026-10-11 con evidencia (`phase8-p01-*` ×7); aceptación PENDIENTE* |
| P-02 | **Conmutación a AUTO** | Cambiar a `AUTO` en Ajustes; volver al Home; verificar que reloj/batería/mensajes/fecha adaptan contraste (≥ 17:1) sobre imágenes y vídeo sin parpadeos. | Desarrollador — *EJECUTADA parcialmente 2026-10-11 (solo imagen; `phase8-p02-*` ×2); aceptación PENDIENTE; vídeo y contraste logcat sin verificar* |
| P-03 | **Conmutación a AUTO_WITH_OVERRIDES** | Cambiar a `AUTO_WITH_OVERRIDES`; fijar excepción en Reloj (`#FF007F`); verificar que el reloj toma el color manual y batería/mensajes permanecen automáticos. | Desarrollador |
| P-04 | **Eliminación / Reseteo de excepción** | Pulsar "Restablecer" en la excepción del Reloj; verificar retorno instantáneo al color automático del fondo. | Desarrollador |
| P-05 | **Persistencia tras Cold Start** | Configurar `AUTO_WITH_OVERRIDES` con 1 excepción; ejecutar `am force-stop com.daybreak.animelauncher`; reabrir; comprobar que el modo y la excepción persisten. | Desarrollador |
| P-06 | **Restauración de fábrica ("Restaurar Diseño")** | Pulsar "Restaurar" en Ajustes; verificar que vuelve a `MANUAL`, con excepciones limpias (`null`), logo por defecto y RAM oculta. | Desarrollador |
| P-07 | **Interacción con Logo y RAM** | Modificar modo de logo y alternar switch de RAM en la nueva UI organizada; verificar estabilidad visual en sidebar. | Desarrollador |
| P-08 | **Estrés de juego (Minecraft $\to$ Home $\times 2$)** | Ejecutar Minecraft durante 30 s; volver al Home; verificar tiempo de retorno (<900 ms), vídeo continuo warm y 0 FATAL/ANR. | Desarrollador |
| P-09 | **Restauración final del dispositivo** | Restaurar configuración original del teléfono del desarrollador (MANUAL, widget OFF, fondos 248/249). | Desarrollador |

---

## 11. Criterios Objetivos de Aceptación

Para dar por concluida satisfactoriamente la Fase 8 una vez autorizada:
1. **Total real de tests unitarios PASS: 236/236** (226 baseline + 5 en 8A + 5 en 8B = 236 ejecutados, 236 aprobados, 0 fallos).
2. **0 errores en `lintDebug`** y `bundleRelease` firmado exitosamente.
3. **`git diff --check` limpio**, sin trailing whitespaces ni conflictos de formato.
4. **Ninguna regresión visual en modo `MANUAL`:** Diff de captura en POCO X6 idéntico al baseline histórico.
5. **Selector de `ThemeMode` 100% operativo** con persistencia comprobada tras reinicio forzado del proceso.
6. **Excepciones de color funcionales:** Creación, edición y retorno a `null` ("Automático") verificado visualmente en hardware real.
7. **0 llamadas duplicadas de polling** y respeto absoluto a la directiva de no tocar el subsistema de vídeo, accesibilidad ni la bóveda de Hidden Apps.
8. **Aceptación física explícita del desarrollador en POCO X6 5G.**

---

## 12. Dudas o Limitaciones Identificadas

1. **Ampliación futura de excepciones:** Actualmente `AdaptiveColorOverrides` soporta exclusivamente `clockColor`, `batteryColor`, `messagesColor` y `dateColor`. Si en el futuro se desea permitir excepciones para el logo (`miButtonColor`), el fondo del sidebar o los iconos, requerirá extender la data class `AdaptiveColorOverrides` en una fase posterior. Por ahora, se respeta estrictamente el alcance de los 4 elementos ya implementados.
2. **Diferimiento definitivo de `widgetOpacity`:** Confirmado que el slider no produce ningún efecto en los widgets nativos actuales. Se recomienda retirarlo de la interfaz en la reorganización para evitar confusión del usuario, o dejarlo como no interactivo / documentado como deuda técnica.
3. **Previsualización de fondo en pantalla de ajustes:** Actualmente `AdvancedSettingsScreen` muestra de fondo `DynamicBackground` fijado a la Vista 1 (Principal). Al editar opciones adaptativas, la pantalla responde al contexto visual de la página principal.
4. **Tema visual de la propia pantalla de ajustes (Investigación 8B):** `AdvancedSettingsScreen` sintetiza sus tokens locales mediante `LauncherThemeTokens.fromAdvancedStyleConfig(currentStyle)`, lo cual mantiene su tipografía y acentos en un estilo cyberpunk de alto contraste (cian/obsidian). Esto es deseable para legibilidad estricta de menús de configuración y evita replicar el análisis de fondo durante la navegación de ajustes. Queda documentado como decisión de diseño para ratificar antes del cierre de 8C.

---

## 13. RE-AUDITORÍA 2026-10-11 — Verificación del Estado Real y Punto de Reanudación

Ejecutada a petición del desarrollador ("revisa hasta donde ibas y continúa sin empezar de 0") en
modo estricto de solo lectura: 0 modificaciones en Kotlin/recursos, 0 commits, 0 push, 0 cambios
en el POCO X6. Solo inspección, lectura de código y re-ejecución de gates de lectura
(`testDebugUnitTest`, `git diff --check`).

### 13.1 Resumen ejecutivo de la re-auditoría

La Fase 8 **no está en estado "auditoría previa" sino en estado "implementación 3/4 completa sin
commit"**: las subfases **8A, 8B y 8C están implementadas en el working tree** (no committeadas) y
sus gates registrados; la sesión anterior quedó **interrumpida a mitad de la 8D** (validación
física), concretamente tras ejecutar P-01 y P-02 (parcial) sobre el POCO X6, que **quedó con
`themeMode = AUTO` sin restaurar** (respaldo original intacto). El trabajo de 8A-8C ha sido
re-verificado línea a línea en esta re-auditoría y sus afirmaciones de gates son ciertas: la suite
completa se re-ejecutó hoy con resultado **236/236 PASS (0 fallos, 0 errores, 0 omitidos)** y
`git diff --check` limpio en `app/`. No se ha detectado ninguna violación de los contratos
protegidos: el engine, el pipeline de vídeo, los gestos, Hidden Apps y el ciclo del widget RAM
están intactos.

### 13.2 Estado verificado de Git (2026-10-11)

- HEAD local `16eea76` (Fase 7), `origin/main` `05ef434`, main **adelantado 7 / detrás 0** — sin cambios respecto al checkpoint.
- **Working tree con trabajo de Fase 8 SIN COMMIT:**
  - `M app/src/main/java/com/daybreak/animelauncher/LauncherViewModel.kt` (+45/−…): extracción de `normalizeLoadedStyle()` (8A), fallback `adaptiveOverrides ?: AdaptiveColorOverrides()` (`LauncherViewModel.kt:139-153`, consumo en `loadState()` línea 368).
  - `M app/src/main/java/com/daybreak/animelauncher/ui/screens/AdvancedSettingsScreen.kt` (1027 líneas; +855/−514 vs HEAD): selector de `ThemeMode` (§líneas 369-439), grupo condicional por modo (444-655), `OverrideColorItem` (140-251), retiro del slider fantasma `widgetOpacity`, reorganización en 5 grupos (8C).
  - `?? app/src/test/java/com/daybreak/animelauncher/theming/AdaptiveThemeSettingsTest.kt` (292 líneas, 10 tests, sin rastrear).
  - `?? docs/UI-1-PHASE8-AUDIT.md` (este informe), `?? docs/assets/phase8-*` (10 evidencias del 11-10 19:48-20:23), `?? docs/evidence_p8/` (intento de 8D del 10-10, ~39 MB), `?? p8d-tap.sh`, `?? p8d-check.tmp`, `?? p8d-ui.tmp` (scratch de automatización adb en la raíz del repo).
- `git diff --check -- app/` → **exit 0** (limpio; solo aviso CRLF→LF informativo en `LauncherViewModel.kt`).

### 13.3 Verificación de gates re-ejecutada (2026-10-11)

| Gate | Resultado registrado (10-10) | Re-verificado hoy |
|---|---|---|
| `testDebugUnitTest` | 236/236 PASS | **236/236 PASS** (22 suites, 0 fallos/errores/omitidos; JAVA_HOME = JBR de Android Studio) |
| `git diff --check` (app/) | Limpio | **Limpio (exit 0)** |
| Compilación de fuentes | assembleDebug 6 s | Verificada implícitamente por la compilación de la suite de tests (compileDebugKotlin sin errores) |
| `lintDebug` / `bundleRelease` | PASS (0 errores, 80 warnings baseline) | **NO re-ejecutados** (pendientes para el cierre de 8D; no son bloqueantes de la re-auditoría) |

### 13.4 Verificación línea a línea de la implementación 8A-8C (hechos observados)

**8A — `LauncherViewModel.kt`:**
- `normalizeLoadedStyle()` (`LauncherViewModel.kt:139-153`): función pura `internal`, normaliza `themeMode`, `logoMode`, `logoIcon`, `logoText`, `adaptiveOverrides` (fallback a contenedor vacío) y re-afirma `showRamWidget`. Consumida en `loadState()` (línea 368) dentro del `try` de Gson; una NPE residual seguiría atrapada por el `catch` genérico, pero el caso null-explícito queda cubierto.
- `AdaptiveThemeSettingsTest` cubre: JSON antiguo sin campo, null explícito, round-trip de excepciones, reset individual a `null`, preservación de modo/logo/RAM + enum corrupto → MANUAL (test01-test05), más mutación de modos, asignación de las 4 excepciones, reset independiente, semántica null por defecto y round-trip mixto (test06-test10). **10/10 PASS hoy.**

**8B/8C — `AdvancedSettingsScreen.kt` (hechos, no recomendaciones):**
- **Grupo 1 (369-439):** selector de 3 chips `MANUAL`/`AUTO`/`AUTO_WITH_OVERRIDES` con `onPersist` inmediato y tarjeta de descripción contextual reactiva. Guard de no-reemisión si el modo no cambia (línea 415).
- **Grupo 2 (444-655), condicional estricto por `currentStyle.themeMode`:**
  - `MANUAL` (449-528): 6 `ColorSettingItem` (miButton, miText, clock, battery, messages, date) escriben los campos estáticos históricos; persistencia inmediata. Contrato MANUAL intacto (mismos campos, mismos presets).
  - `AUTO` (529-550): tarjeta informativa; 0 selectores manuales de texto; explica que las excepciones se conservan.
  - `AUTO_WITH_OVERRIDES` (551-654): 4 `OverrideColorItem` (clock, battery, messages, date) sobre `currentStyle.adaptiveOverrides.<campo>` con badge "Automático"/swatch HEX, chip "Auto" y botón "Restablecer" que escriben **estrictamente `null`** (577-587, 603-607, 628-629, 644-649). No se crean overrides para elementos sin soporte en el engine (scope respetado: solo los 4 campos de `AdaptiveColorOverrides`).
- **Grupo 3 Glass (660-714):** `accentColor`, `panelTransparency`, `glassBorderAlpha`, `cornerRadius` con patrón transient+persist-on-finish (D-05 respetado). `widgetOpacity` retirado de la UI; el campo permanece en `AdvancedStyleConfig` (línea 107) por compatibilidad Gson — **intencional**.
- **Grupo 4 Logo (719-830):** 3 modos, `OutlinedTextField` con `logoTextError` + contador `$count/10`, catálogo `LogoCatalog.KEYS` (8 iconos). Lógica Fase 6 intacta, sin duplicación.
- **Grupo 5 (835-1022):** switch `showRamWidget` (840-874) + barras lateral/diagonal (877-933) + triángulo y cajón Vista 2 (936-1022). Descripción del widget RAM correcta ("memoria RAM total y usada por el dispositivo" — `ActivityManager.MemoryInfo`, no PSS).
- La pantalla sigue autoproveyendo sus tokens vía `LauncherThemeTokens.fromAdvancedStyleConfig` (282-284) — decisión de diseño ratificada en 8C (menú legible, sin duplicar el motor).

**Consumidores (verificación de que la UI alimenta el engine real):**
- `AdaptiveThemeEngine.buildAutoPageTheme` aplica overrides SOLO en `AUTO_WITH_OVERRIDES` (líneas 127-131) vía `applyOverrides` (186-199): null → color generado; no-null → `parseHexColor` con fallback. Coincide 1:1 con la semántica de la UI.
- `AdaptiveThemeEngine.resolveManual` (268-305) consume los campos estáticos — paridad MANUAL.
- ViewOne/ViewTwo: colores de elementos via `elementColors` (no-null en AUTO) con fallback exacto a `style.<campo>` (ViewOne.kt:91-99; equivalente en ViewTwo); parámetros geométricos/estructurales (triangle*, diagonalBar*, sidebar*) siguen leyéndose directamente de `AdvancedStyleConfig` en ambos modos (ViewOne.kt:104,143,155,322; ViewTwo.kt:114,155,163,279) — arquitectura Fase 2/4 intacta.
- Polling RAM con gate `isAppForeground && pagerState.currentPage == page` (LauncherScreen.kt:667,687) — sin cambios en Fase 8.

### 13.5 Estado del POCO X6 y de la 8D (hechos)

- Respaldo original (11-10 19:48): `docs/assets/phase8-original-prefs.xml` — `themeMode=MANUAL`, `showRamWidget=false`, `logoMode=ICON_ONLY`, `logoText=""`, `logoIcon=AutoAwesome`, `adaptiveOverrides` vacío. **Intacto.**
- Estado actual verificado por `run-as` (11-10, lectura): clave autoritativa `launcher_style_config` → **`themeMode="AUTO"`**, resto de valores en default. El snapshot incrustado en `launcher_state` conserva `MANUAL` (solo se escribe por `updateState`, no por `persistStyleConfig` — comportamiento esperado).
- Conclusión: la sesión anterior murió entre P-02 (aplicado) y P-03; la restauración final (P-09) está pendiente. **El dispositivo NO está en su configuración original.**
- Duplicidad de evidencia: `docs/evidence_p8/` (10-10, intento anterior, ~39 MB con dumps XML) y `docs/assets/phase8-*` (11-10, intento vigente). Debe decidirse qué carpeta es canónica antes del commit (recomendación: mantener la del 11-10 en `docs/assets/` y archivar/borrar `docs/evidence_p8/`).
- Housekeeping pendiente antes de commit: `p8d-tap.sh`, `p8d-check.tmp`, `p8d-ui.tmp` (raíz del repo) son scratch de automatización; limpiar o mover a `docs/evidence_p8/`.
- El cerebro Obsidian (`Current_State`, `Session_Checkpoint`, `Changelog`, `Physical_Validation_Ledger`, `Adaptive_Theme_Architecture`) sigue en el estado pre-Fase 8 ("Fase 8 PLANNED", working tree "limpio", 226/226): **debe actualizarse al cerrar la fase**, no antes (regla del proyecto: documentar antes de cerrar cada fase).

### 13.6 Hallazgos menores de la re-auditoría (sin impacto funcional)

1. **Ternario redundante** en `OverrideColorItem` (`AdvancedSettingsScreen.kt:227`): `text = if (isEs) "Auto" else "Auto"` — ambas ramas idénticas. Cosmético; corregible en el cierre.
2. **Strings inline bilingües** (`if (isEs) ... else ...`) en lugar de `values/strings.xml` + `values-en/strings.xml`, que eran los archivos previstos en §8 del plan original. Desviación consciente coherente con el estilo preexistente del archivo (todo el histórico de la pantalla usa strings inline). Sin acción obligatoria; registrar como deuda si se quiere i18n formal.
3. **`widgetOpacity`** sigue siendo campo muerto en el modelo (0 consumidores), ahora sin control en UI: estado correcto y documentado (no es toggle fantasma — protegido UX-02 respetado).
4. La captura P-02 se limitó a fondo de imagen; el escenario AUTO sobre **vídeo** (P-02 completo) y el contraste por logcat `AdaptiveTheme` no tienen evidencia de este intento.

### 13.7 Punto exacto de reanudación (pendiente de autorización)

1. **Continuar 8D desde P-02 (vídeo)/P-03** con la build actual ya instalada en el POCO X6 — requiere autorización del desarrollador (interviene ajustes del teléfono).
2. Completar P-03..P-09, incluida **P-09 restauración del dispositivo** desde `phase8-original-prefs.xml` y verificación (MANUAL, widget OFF, ICON_ONLY, fondos originales).
3. Registrar aceptación física explícita del desarrollador (regla [[novalauncher-physical-validation]]: ninguna P se declara PASS sin su confirmación).
4. Housekeeping de evidencia (§13.5) + corrección cosmética de §13.6.1 (opcional).
5. Actualizar cerebro Obsidian (Current_State, Session_Checkpoint, Ledger, Changelog, Adaptive_Theme_Architecture §Pendiente) al cierre.
6. Commit de Fase 8 (orden explícita del desarrollador; push sigue prohibido sin orden).

**FIN DEL INFORME DE AUDITORÍA PREVIA A UI-1 FASE 8** *(actualizado con §13 — re-auditoría 2026-10-11)*

---

## 14. RESULTADOS 8D — EJECUCIÓN FÍSICA COMPLETA (2026-10-15)

Reanudación autorizada por el desarrollador tras la restauración intermedia documentada
(`phase8-pre-restore-current-prefs.xml` + capturas `phase8-restored-*`). Build validada:
**debug instalada 2026-10-10 12:09** (= código 8A-8C; sin reinstalación). Método: automatización
adb (`input tap/swipe` + verificación por capturas visuales y prefs `run-as`; `uiautomator dump`
**no funciona con fondo de vídeo en movimiento** — "could not get idle state" — hallazgo
metodológico clave de esta sesión).

**Regla de oro respetada:** ninguna P física se declara `PASS` sin confirmación del desarrollador.
Los estados abajo distinguen evidencia objetiva recogida por el agente (mediciones, prefs, píxeles,
logcat) de la aceptación visual pendiente.

### 14.1 Matriz de resultados P-01..P-09 (Validación Física y Aceptación)

| ID | Procedimiento resumido | Resultado real (objetivo) | Clasificación y Estado |
|---|---|---|---|
| P-01 | Paridad MANUAL (recorrido ajustes + Home; ejecutada 11-10) | Capturas ×7 (`phase8-p01-adv-*`, `phase8-p01-advanced-settings.png`, `phase8-p01-home-baseline.png`, `phase8-p01-reentry.png`). Re-entry y renderizado estables. *Limitación técnica documentada:* La evidencia no demuestra por sí sola una comparación pixel-perfect mediante diff automatizado de imágenes (requeriría normalización de brillo cross-time como en F2/F4); validación visual realizada. | **EJECUTADO — LIMITACIÓN DE AUTOMATIZACIÓN — ACEPTADO POR EL DESARROLLADOR (2026-10-17)** |
| P-02 | AUTO sobre **vídeo** (VID-20251112-WA0018, 6.6 s vía SAF picker + "Descartar propuesta"), ambas páginas | Engine logcat `source=video mode=AUTO contrastTP=20.08` (cálculo interno del motor sobre paleta agregada de 3 ticks); movimiento de vídeo 69.7% px/3 s; Vista 2 sobre imagen 18.84:1; reloj/batería/mensajes/fecha legibles (blanco sobre scrim); acento magenta derivado dinámicamente; **0 FATAL/ANR**; 2 líneas informativas previas `MediaCodec error -32`. | **EJECUTADO — RESULTADOS MEDIDOS CONSERVADOS — ACEPTADO POR EL DESARROLLADOR (2026-10-17)** |
| P-03 | `AUTO_WITH_OVERRIDES` + excepción reloj `#FF007F` (preset 3) | SharedPreferences `{"clockColor":"#FF007F"}` persistidas; UI muestra badge `#FF007F` y botón "Restablecer"; en Home: reloj rosa con 3.576 px rosa detectados en su región, mientras batería, mensajes y fecha se mantienen en blanco automático adaptativo; modo `AUTO_WITH_OVERRIDES` confirmado en logcat. | **EJECUTADO — RESULTADOS MEDIDOS CONSERVADOS — ACEPTADO POR EL DESARROLLADOR (2026-10-17)** |
| P-04 | Reset individual del reloj ("Restablecer" → null) | SharedPreferences `adaptiveOverrides` restablecidas a `{}` (null persistido con éxito); UI muestra badge "Automático"; en Home: 0 px rosa / 3.594 px blancos en reloj; modo y persistencia de otros elementos intactos. | **EJECUTADO — RESULTADOS MEDIDOS CONSERVADOS — ACEPTADO POR EL DESARROLLADOR (2026-10-17)** |
| P-05 | Persistencia tras cold start (force-stop + relanzar) | PID nuevo verificado (1231→4971 tras reintento de force-stop frente al re-spawn de HOME en HyperOS); preferencias idénticas pre/post (`AUTO_WITH_OVERRIDES` + excepción `#FF007F`); reloj rosa reaparece inmediatamente (4.223 px). | **EJECUTADO — RESULTADOS MEDIDOS CONSERVADOS — ACEPTADO POR EL DESARROLLADOR (2026-10-17)** |
| P-06 | Restauración vía UI (botón "Restaurar" en ajustes) | SharedPreferences restauradas exactamente a los valores por defecto del código: `themeMode="MANUAL"`, acento `#00F0FF`, excepciones `{}`, `logoMode="ICON_ONLY"`, `logoText=""`, `logoIcon="AutoAwesome"`, `showRamWidget=false`, `widgetOpacity=0.9` (campo conservado en modelo por compatibilidad); visual: chip "Manual" activo y paleta cian. | **EJECUTADO — RESULTADOS MEDIDOS CONSERVADOS — ACEPTADO POR EL DESARROLLADOR (2026-10-17)** |
| P-07 | Logo 3 modos + validación 10 chars + RAM on/off ambas páginas | Modos `TEXT_ONLY`, `ICON_PLUS_TEXT` e `ICON_ONLY` persistidos y verificados visualmente; texto de 12 chars disparó error visual "Máximo 10 caracteres" con contador 12/10, persistiendo el último valor válido (lógica Fase 6 preservada); "NOVA" en `ICON_PLUS_TEXT` renderizado sin clipping; RAM ON: actualización real de 6.8 a 6.9 GB en 6 s (Vista 1 vídeo) y lectura fresca en Vista 2 sin solapamientos con logo ni dock; RAM OFF: widget desaparece y layout colapsa adecuadamente. | **EJECUTADO — RESULTADOS MEDIDOS CONSERVADOS — ACEPTADO POR EL DESARROLLADOR (2026-10-17)** |
| P-08 | Estrés Minecraft ×2 + drawer + gestos | Run 1: retorno al Home en ~518 ms, RAM fresca 6.9→7.9/11.0 GB, 0 FATAL/ANR. Run 2 (vídeo recuperado tras re-selección accidental en §14.3): retorno **+379 ms** al resume, vídeo reanuda **warm (70.2% px cambian en 3 s)**, RAM 7.0/11.0 GB, **0 FATAL/ANR**. Drawer abre y cierra fluido. *Limitaciones técnicas registradas:* La automatización por `input` no pudo reproducir el gesto multitáctil de pinch-in (requiere hardware o instrumentación multitouch). No había widgets nativos de terceros configurados para probar interacción táctil interna. El servicio de accesibilidad quedó inactivo tras los reinicios de HyperOS: el gesto swipe-down mostró el diálogo `AccessibilityDisclosureDialog` (comportamiento fail-visible correcto según Fase 5A; el servicio NO quedó reactivado y requiere activación manual por el usuario). La aplicación sufrió reinicios de proceso por HyperOS (PIDs 1231→4971→25819→30911→8803); se verificó recuperación y persistencia completa, sin interpretar este hecho como ausencia absoluta de fallos en el SO. | **EJECUTADO PARCIALMENTE / LIMITADO POR AUTOMATIZACIÓN / SUBPRUEBA NO APLICABLE (sin widgets nativos) — ACEPTADO POR EL DESARROLLADOR (2026-10-17)** |
| P-09 | Restauración final desde respaldo original | Proceso detenido → restauración autoritativa mediante `run-as cat > launcher_prefs.xml` desde `phase8-original-prefs.xml` → XML verificado semánticamente idéntico (diff de claves y valores = 0) → launcher relanzado. Estado verificado: `MANUAL`, `showRamWidget=false`, `ICON_ONLY`, `logoText=""`, `logoIcon=AutoAwesome`, overrides `{}`, **fondos originales 248/249**; ambas páginas del Home visualmente restauradas (`phase8-p09-restored-home-view1.png`, `phase8-p09-restored-home-view2.png`). Archivo protegido `nova_hidden_apps_prefs.xml` intacto sin modificaciones por las pruebas (configuración BIOMETRIC preservada). 0 FATAL/ANR tras la restauración. | **RESTAURADO Y VERIFICADO EN HARDWARE — ACEPTADO POR EL DESARROLLADOR (2026-10-17)** |

### 14.2 Evidencia de la Fase 8

- **Línea base pre-Fase 8:** `docs/assets/phase8-00-estado-inicial.png`.
- **P-01 (Paridad MANUAL y recorrido de UI):** `phase8-p01-home-baseline.png`, `phase8-p01-advanced-settings.png`, `phase8-p01-adv-opened.png`, `phase8-p01-adv-top.png`, `phase8-p01-adv-mid.png`, `phase8-p01-adv-bottom.png`, `phase8-p01-adv-logo.png`, `phase8-p01-reentry.png`.
- **P-02 (AUTO sobre vídeo vivo):** `phase8-p02-home-before.png`, `phase8-p02-settings-fondo.png`, `phase8-p02-after-pick.png`, `phase8-p02-auto-selected.png`, `phase8-p02-home-after-auto.png`, `phase8-p02-video-view1.png`, `phase8-p02-video-view1-t2.png`, `phase8-p02-video-view2.png`. Logcat crudo: `phase8-p02-logcat.txt` (preservado localmente).
- **P-03 (AUTO_WITH_OVERRIDES y reloj rosa):** `phase8-p03-mode-selected.png`, `phase8-p03-clock-override-set.png`, `phase8-p03-home-clock-pink.png`. Logcat crudo: `phase8-p03-logcat.txt` (preservado localmente).
- **P-04 (Reset individual a null):** `phase8-p04-clock-reset.png`, `phase8-p04-home-clock-auto.png`.
- **P-05 (Persistencia cold start):** `phase8-p05-cold-start-home.png`.
- **P-06 (Restaurar defaults en UI):** `phase8-p06-ground-truth.png`, `phase8-p06-after-reset.png`.
- **P-07 (Logo 3 modos y RAM widget):** `phase8-p07-logo-text-12chars.png`, `phase8-p07-home-iconplustext.png`, `phase8-p07-ram-view1.png`, `phase8-p07-ram-view1-t6.png`, `phase8-p07-ram-view2.png`, `phase8-p07-ram-off.png`.
- **P-08 (Estrés Minecraft, vídeo warm y drawer):** `phase8-p08-run1-home-before.png`, `phase8-p08-run1-home-after.png`, `phase8-p08-run1-video-t0.png`, `phase8-p08-run1-video-t3.png`, `phase8-p08-pre-run2-view1.png`, `phase8-p08-run2-home-after.png`, `phase8-p08-drawer-open.png`, `phase8-p08-notifications.png`. Logcats de estrés: `phase8-p08-run1-logcat.txt`, `phase8-p08-run2-logcat.txt`, `phase8-p08-video-recovery-logcat.txt` (preservados localmente).
- **P-09 (Restauración y verificación final):** `phase8-restored-advanced-settings.png`, `phase8-restored-home-view1.png`, `phase8-restored-home-view2.png` (restauración intermedia 11-10); `phase8-p09-restored-home-view1.png`, `phase8-p09-restored-home-view2.png` (restauración final 15-10).
- **Evidencias locales protegidas (excluidas de Git por privacidad):** `phase8-original-prefs.xml`, `phase8-pre-restore-current-prefs.xml`, `phase8-p02-prefs-before.xml`, `phase8-p09-prefs-before-restore.xml`, `phase8-p09-device-after-restore.xml`. Contienen configuraciones y paquetes de apps reales instaladas en el dispositivo de prueba del desarrollador.

### 14.3 Incidentes y hallazgos de la sesión (hechos, no regresiones del código)

1. **`uiautomator dump` incompatible con vídeo vivo:** Con fondo de vídeo en reproducción continua el dump falla con "could not get idle state" y sirve el XML anterior, provocando taps a coordenadas desactualizadas. Mitigación: verificación visual directa por captura y SharedPreferences antes de cada interacción.
2. **Colateral de taps durante dump obsoleto:** Un tap accidental re-seleccionó temporalmente la imagen 248 como fondo en Vista 1 y otro activó `showRamWidget`. Ambos mecanismos operaron según el diseño del launcher. El estado se recuperó restableciendo el vídeo para el segundo run y restaurando el dispositivo por completo en P-09.
3. **Reinicio de proceso por HyperOS:** HyperOS recicló el proceso del launcher en varias ocasiones durante pruebas intensivas de background (PIDs observados: 1231 → 4971 → 25819 → 30911 → 8803). El launcher recuperó el estado persistido íntegramente en cada cold start.
4. **Transporte de preferencias para restauración:** La transferencia directa vía `adb push` a carpetas privadas falló por restricciones de storage en Android 16; se resolvió satisfactoriamente mediante `run-as cat >` preservando la estructura semántica idéntica del XML.
5. **Servicio de accesibilidad inactivo tras reinicios:** Tras los reinicios de proceso del SO, el servicio de accesibilidad quedó inactivo. Al ejecutar el gesto swipe-down, la app mostró correctamente el diálogo de divulgación prominente (`AccessibilityDisclosureDialog`), validando el comportamiento fail-visible de Fase 5A. El servicio requiere reactivación manual si el desarrollador desea usar gestos asistidos.

---

## 15. Cierre Formal de UI-1 Fase 8 (2026-10-17)

### 15.1 Resultados reales de los Gates Finales de Calidad

Ejecución formal realizada el 2026-10-17 con el entorno JDK verificado (`Android Studio JBR / OpenJDK 25.0.2`):

1. **Gate 1 — Pruebas unitarias (`.\gradlew.bat testDebugUnitTest`):**
   - **Descubiertas y ejecutadas:** 236 pruebas unitarias (22 test suites).
   - **Aprobadas:** **236/236 PASS (100%)**.
   - **Fallidas:** 0.
   - **Errores:** 0.
   - **Omitidas:** 0.
   - **Tiempo de ejecución:** 1m 08s.
2. **Gate 2 — Compilación debug (`.\gradlew.bat assembleDebug`):**
   - **Resultado:** **BUILD SUCCESSFUL** en 22s (3 executed, 66 up-to-date).
3. **Gate 3 — Análisis estático (`.\gradlew.bat lintDebug`):**
   - **Resultado:** **BUILD SUCCESSFUL** en 2m 20s.
   - **Errores:** **0 errores**.
   - **Advertencias:** 80 advertencias preexistentes en la baseline histórica, 6 hints. 0 advertencias nuevas introducidas en Fase 8.
4. **Gate 4 — Empaquetado Release (`.\gradlew.bat bundleRelease`):**
   - **Resultado:** **BUILD SUCCESSFUL** en 3m 25s.
   - Optimización y ofuscación R8 completada, lint vital verificado, bundle AAB firmado generado exitosamente.
5. **Gate 5 — Verificación de diff y estilo (`git diff --check -- app/ docs/`):**
   - **Resultado:** **PASS (0 errores de espacios en blanco o formato)**.

### 15.2 Housekeeping Controlado y Limpieza de Evidencias

- **Eliminación de duplicados exactos verificados por hash SHA-256:**
  - `p8d-check.tmp` (hash `FA5EB17CA6E8CF71FB4C0F61107DC7A9F14F4E07042FE1D55E686720609C92CB`): Eliminado tras verificar duplicidad idéntica con `docs/assets/phase8-pre-restore-current-prefs.xml`.
  - `p8d-v1check.tmp` (hash `59AB30627E6992B40E0A0FAEFB59BAC0CB584A25B6A5F5BC4BE109C7E14017A2`): Eliminado tras verificar duplicidad idéntica con `docs/assets/phase8-p08-pre-run2-view1.png`.
  - `p8d-v1b.tmp`: Eliminado tras verificar duplicidad idéntica con `p8d-v1a.tmp`.
- **Preservación y archivo de scratch de automatización:**
  - Archivos temporales de automatización de la raíz (`p8d-tap.sh`, `p8d-ui.tmp`, `-t.png`, y capturas intermedias `p8d-*`) movidos a la ubicación documentada `docs/evidence_p8/scratch_8d/` para conservar registro histórico sin contaminar la raíz del proyecto.
  - La carpeta `docs/evidence_p8/` (evidencias del primer intento del 10-10 y scratch 8D) se conserva intacta a nivel local como respaldo histórico no canónico.
- **Protección de datos sensibles y exclusión local en Git (`.git/info/exclude`):**
  - Los respaldos XML de configuración del dispositivo (`phase8-original-prefs.xml`, `phase8-pre-restore-current-prefs.xml`, `phase8-p02-prefs-before.xml`, `phase8-p09-prefs-before-restore.xml`, `phase8-p09-device-after-restore.xml`) se mantienen intactos en `docs/assets/` como evidencia local, pero quedan formalmente excluidos del commit mediante `.git/info/exclude` para proteger la privacidad de las aplicaciones y cuentas del desarrollador.
  - Los archivos de trazas crudas de logcat (`phase8-*-logcat.txt`, `phase8-p08-video-recovery-logcat.txt`) y la carpeta `docs/evidence_p8/` quedan igualmente excluidos localmente.
- **Corrección cosmética acotada:**
  - En `AdvancedSettingsScreen.kt:227` (`OverrideColorItem`), simplificación del ternario redundante `if (isEs) "Auto" else "Auto"` a la constante `"Auto"`. Sin otras alteraciones funcionales.

### 15.3 Declaración de Cierre Formal

**UI-1 Fase 8 (Advanced Settings: Integración del Tema Adaptativo) queda declarada formalmente como COMPLETADA, VERIFICADA Y ACEPTADA.**
El commit local agrupa coherentemente la implementación (8A, 8B, 8C), las pruebas automatizadas asociadas, la documentación de auditoría consolidada y las capturas finales necesarias de validación física, manteniendo los contratos protegidos del launcher estrictamente intactos.
