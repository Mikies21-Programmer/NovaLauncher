# Estado Persistente del Proyecto — NovaLauncher (Agent State)

> **Documento maestro de sincronización, continuidad operativa y auditoría entre sesiones, Antigravity y Claude.**<br>
> *Última actualización: 19 de septiembre de 2026 (Post Checkpoint Fase 2E — Adaptive Wallpaper Theming).*<br>
> *Regla de seguridad estricta: CERO secretos, contraseñas, keystores ni claves privadas en este repositorio.*

---

## 1. Identidad y Parámetros Base
* **Proyecto:** NovaLauncher
* **Repositorio Principal:** `Mikies21-Programmer/NovaLauncher`
* **Directorio Local:** `C:\Users\migue\AndroidStudioProjects\AnimeLauncher`
* **Application ID:** `com.daybreak.animelauncher`
* **Versión Actual:** `versionCode = 6`, `versionName = "1.4"`
* **SDKs:** `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`
* **Política de Privacidad Pública:** `https://mikies21-programmer.github.io/NovaLauncher-Privacy/`
* **Contacto Oficial:** `dbreak472@gmail.com`

---

## 2. Estado Oficial de Módulos y Fases

### Tabla de Estado Oficial de Adaptive Wallpaper Theming

| Fase | Alcance Técnico | Estado Oficial |
| :--- | :--- | :---: |
| **Fase 1** | Wallpaper Analyzer (`Bitmap` → `WallpaperAnalyzer` → `ThemePalette`) | ✅ **COMPLETADA / VALIDADA** |
| **Fase 2B** | Runtime Theme Token Layer (`LauncherThemeTokens` + `LocalLauncherThemeTokens`) | ✅ **COMPLETADA / VALIDADA / CHECKPOINT** |
| **Fase 2C** | Theme Proposal Generator (`ThemePalette` → `ThemeProposalGenerator` → `List<ThemeProposal>`) | ✅ **COMPLETADA / VALIDADA / CHECKPOINT** |
| **Fase 2D** | Theme Proposal Flow + Preview + Apply para IMÁGENES | ✅ **COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT** |
| **Fase 2E** | Adaptive Theme para VIDEO (`VideoFrameExtractor` + `ThemePaletteAggregator` + Preview) | ✅ **COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT** |

---

### Detalle de Fases de Adaptive Wallpaper Theming

#### Fase 1 — Fundamentos y Wallpaper Analyzer (✅ COMPLETADA / VALIDADA)
* **Objetivo:** Subsistema puro de análisis cromático sobre bitmaps estáticos desacoplado de Compose, ViewModels y persistencia.
* **Componentes:**
  * `ThemePalette`: Modelo inmutable con `@get:ColorInt dominantColor`, `vibrantColor`, `mutedColor`, `isDark: Boolean`, y `averageLuminance: Float`. Incluye `DEFAULT` seguro y neutro para recuperaciones sin excepciones.
  * `WallpaperAnalyzer`: Objeto singleton con `analyze(bitmap, dispatcher = Dispatchers.Default)` y `analyzeSync(bitmap)`.
* **Memoria y Performance:** Redimensionamiento previo a ~200x200 px (`TARGET_MAX_DIMENSION = 200`), memoria contenida en ~160 KB, reciclaje garantizado del bitmap temporal en `finally`. Despachado fuera del hilo principal (`Dispatchers.Default`).
* **Calidad Cromática:** `.clearFilters()` en `Palette.Builder` para clasificar fondos puros oscuros, claros o monocromáticos; luminancia relativa ponderada por población de swatches; clasificación `isDark = averageLuminance < 0.5f`.
* **Tests:** 11 pruebas unitarias en `WallpaperAnalyzerTest` (100% PASS).

#### Fase 2B — Runtime Theme Token Layer (✅ COMPLETADA / VALIDADA / CHECKPOINT)
* **Modelo Inmutable Puro:** Creado `LauncherThemeTokens` (`accentColor`, `surfaceColor`, `textPrimaryColor`, `textSecondaryColor`, `borderColor`) con enteros `@ColorInt Int` y parser hexadecimal propio libre de Compose y dependencias Android.
* **Defaults Compatibles:** Paridad visual 100% con la estética cyberpunk original (`#00F0FF` para acento/borde, `#08080C` para superficies oscuras, `#FFFFFF` para texto principal, `#CCCCCC` para secundario).
* **CompositionLocal Global:** Creado `LocalLauncherThemeTokens` inyectado en el root real de composición (`MainActivity.kt` envolviendo a `NavHost`).
* **Consumo Conectado:** Consumo reactivo en `AppDrawerScreen.kt`, `LauncherScreen.kt`, `SettingsScreen.kt` (GlassCard reactivo) y `AdvancedSettingsScreen.kt`.
* **Tests:** 4 pruebas unitarias en `LauncherThemeTokensTest` (100% PASS).

#### Fase 2C — Theme Proposal Generator (✅ COMPLETADA / VALIDADA / CHECKPOINT)
* **Objetivo:** Generación determinista y pura de propuestas semánticas: `ThemePalette` → `ThemeProposalGenerator` → `List<ThemeProposal>`.
* **Modelo:** `ThemeProposal` (`id`, `palette`, `proposedTokens`, `isDark`, `label`, `order`) como estructura transitoria en memoria libre de persistencia.
* **Garantía Estricta de Contraste:** Validación y ajuste sistemático de luminancia por HSL para ratios mínimos WCAG 2.1: $\ge 4.5:1$ en `textPrimaryColor`, $\ge 3.0:1$ en `textSecondaryColor` y `accentColor`, $\ge 1.5:1$ en `borderColor`.
* **Deduplicación Perceptual y Sanity Check:**
  * Evaluado con 5 paletas representativas (oscura azul/violeta, oscura rojiza/magenta, clara azul, clara cálida y monocromática).
  * Resultados: propuestas perceptualmente diferenciadas, deduplicación funcional (distancia Manhattan RGB), contraste WCAG AAA/AA y reducción a 1 sola propuesta en monocromáticas sin inventar variantes artificiales.
* **Tests:** 12 pruebas unitarias en `ThemeProposalGeneratorTest` (100% PASS).

#### Fase 2D — Propuestas de Tema + Preview + Aplicar para Imágenes (✅ COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT)
* **Flujo Validado Físicamente:** Galería → Análisis asíncrono → `ThemeProposalDialog` (1 a 4 propuestas) → **Aplicar**, **Personalizar**, **Descartar**, **Back**.
* **Independencia de Wallpaper:** El wallpaper nuevo permanece vigente en las 4 acciones; el tema se persiste en *Aplicar*, se usa como base en *Personalizar*, o se restaura el tema anterior en *Descartar* y *Back*.
* **Correcciones Validadas:**
  1. *Personalizar:* Enrutamiento directo hacia `AdvancedSettingsScreen` pasando `initialShowAdvanced = true` a través de `openAdvancedInSettings` en `MainActivity` y `SettingsScreen`.
  2. *Contraste Category Pills:* Sustitución de colores hardcodeados por tokens semánticos `themeTokens.accent`, `themeTokens.textSecondary` y borde `themeTokens.border.copy(alpha = 0.15f)`, legibles en tema claro y oscuro.
* **Tests:** 12 pruebas unitarias en `ThemeProposalFlowHandlerTest` (100% PASS).

#### Fase 2E — Adaptive Theme para VIDEO (✅ COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT)
* **Flujo Validado Físicamente:**
  ```text
  Video seleccionado desde Galería (.mp4, URI)
                  ↓
  Detección MIME (video/*)
                  ↓
  ThemeProposalDialog desplegado
                  ↓
  VideoFrameExtractor (Dispatchers.IO): 3 frames representativos (10%, 50%, 90%)
    └── Acotado a ~400 px (evita 4K en RAM); MediaMetadataRetriever liberado en finally
                  ↓
  WallpaperAnalyzer (Dispatchers.Default): análisis individual por frame
    └── Frame central (50%) conservado para el preview superior
    └── Frames auxiliares reciclados inmediatamente
                  ↓
  ThemePaletteAggregator: Medoide central geométrico en RGB (sin mezcla artificial)
    └── averageLuminance media aritmética (isDark coherente)
                  ↓
  ThemeProposalFlowHandler → 1 a 4 propuestas semánticas
                  ↓
  Aplicar / Personalizar / Descartar / Back (Wallpaper de video aplicado exitosamente)
  ```
* **Preview Nativo Sin Dependencias:** Coil renderiza directamente el `Bitmap` representativo mediante `AsyncImage(model = ImageRequest.Builder(context).data(previewBitmap ?: imageUri)...)`, eliminando dependencias adicionales como `coil-video` o reproductores embebidos en el diálogo.
* **Ciclo de Vida Limpio:** `previewBitmap` se recicla en `DisposableEffect.onDispose`; cero corrutinas ni bitmaps retenidos tras cerrar el diálogo.
* **Tests:** 7 pruebas unitarias en `ThemePaletteAggregatorTest` (100% PASS). Total global de tests unitarios: **52/52 PASS**.

---

### Diagnóstico de Comportamiento Preexistente de Video

* **Observación Fásica:** Al tener un video como fondo de pantalla, abrir una aplicación multimedia pesada (ej. TikTok, YouTube, Cámara) y regresar a NovaLauncher, de forma intermitente el video puede quedar congelado en el último frame.
* **Investigación y Desacoplamiento:**
  * Se confirmó que **Fase 2E no introdujo el problema**: sus componentes son de ejecución puntual y están completamente destruidos al momento del fallo; el problema se reproduce igualmente con los videos preexistentes (`R.raw.bg_view_one`) anteriores a Fase 1.
  * Causa identificada: [VideoWallpaperManager.kt](file:///c:/Users/migue/AndroidStudioProjects/AnimeLauncher/app/src/main/java/com/daybreak/animelauncher/ui/components/VideoWallpaperManager.kt) (código protegido intacto). Cuando una app pesada reclama decodificadores hardware, SurfaceFlinger invalida la `SurfaceTexture` del `TextureView`. En `onResume()`, `syncSlots()` no detecta cambio de URI ni de página, por lo que no re-vincula `PlayerView.player` ni lanza el watchdog de recuperación, dejando a ExoPlayer decodificando hacia una superficie desconectada.
* **Estado Oficial:**
  ```text
  Known pre-existing video resume issue — independent performance/stability task
  ```
  *(NO marcado como resuelto; se mantendrá como tarea independiente de estabilización para no mezclar Theming con el reproductor).*

---

### Hitos Históricos Anteriores Consolidados

* **Fase 5A a 5D (PASS):** Prominent Disclosure in-app, Tarjeta de Privacidad, repositorio `NovaLauncher-Privacy` en GitHub Pages y auditoría integral de Play Console.
* **Fase 5E-0 a 5E-2 (PASS):** Upload Key v2 generada (`upload-keystore-v2.jks`) y certificado PEM (`upload_certificate-v2.pem`) fuera del repo.
* **PERFORMANCE-03 (VERIFIED):** Capacidad de `IconCache` fijada en 128 (`MAX_ENTRIES = 128`), 100% hit rate en hardware real (412 hits / 0 misses).
* **DRAWER & MOTION SYSTEM CHECKPOINT (PASS / VALIDATED):** Stream híbrido en `LazyColumn` a 120 FPS, `AlphabetIndexRail` conflated, swipe horizontal entre categorías y Nova Motion System (6/6 completo con corrección `ON_STOP` en Resume Fade).

---

## 3. Arquitectura Actual del Adaptive Theming

1. **`WallpaperAnalyzer`:** Singleton asíncrono para análisis cromático y downscaling de bitmaps a ~200 px sin afectar el hilo principal.
2. **`ThemePalette`:** Estructura inmutable portadora de colores (`dominantColor`, `vibrantColor`, `mutedColor`, `isDark`, `averageLuminance`).
3. **`ThemeProposal`:** Entidad transitoria que encapsula propuestas semánticas (`id`, `palette`, `proposedTokens`, `isDark`, `label`, `order`).
4. **`ThemeProposalGenerator`:** Motor matemático puro en Kotlin estándar con contrastes WCAG 2.1 y deduplicación perceptual.
5. **`ThemeProposalFlowHandler`:** Controlador de transiciones de estado (`handleApply`, `handleDiscard`, `handleBack`, `handleCustomize`) y conversión bidireccional con `AdvancedStyleConfig`.
6. **`ThemeProposalDialog`:** Diálogo de pantalla completa Glass (`usePlatformDefaultWidth = false`) con miniatura reactiva `LauncherThemeMiniature`.
7. **`VideoFrameExtractor`:** Extractor puntual de 3 frames (10%, 50%, 90%) en `Dispatchers.IO` escalados a ~400 px con liberación estricta de `MediaMetadataRetriever`.
8. **`ThemePaletteAggregator`:** Agregador puro que selecciona el medoide geométrico RGB entre las paletas de los frames muestreados.
9. **`LauncherThemeTokens` & `LocalLauncherThemeTokens`:** Capa de tokens visuales runtime provista globalmente en `MainActivity`.
10. **`AdvancedStyleConfig`:** Modelo de configuración existente y fuente única de persistencia en DataStore.

---

## 4. Decisiones de Producto Consolidadas

* **A) Propuesta Opcional:** Adaptive Theming es una sugerencia opcional; el usuario siempre puede aplicar el fondo sin adoptar el tema.
* **B) Labels Semánticos:** Etiquetas universales `Dominante`, `Vibrante`, `Equilibrado` (cero marketing artificial).
* **C) Cardinalidad Flexible (1 a 4):** El sistema emite entre 1 y 4 propuestas según la riqueza del wallpaper.
* **D) Previsualización Transitoria:** La exploración opera en memoria (`updateStyleConfigTransient`); la persistencia solo ocurre en Apply o ajustes avanzados.
* **E) Independencia de Fondo y Tema en Descartar/Back:** Se aplica el fondo nuevo y se conserva el tema previo del usuario.
* **F) Personalización Editable:** "Personalizar" entrega la propuesta como base editable dentro de `AdvancedSettingsScreen`.
* **G) Alcance de Video Resuelto:** Videos alimentan propuestas idénticas mediante muestreo de 3 frames sin análisis continuo en reproducción.

---

## 5. Componentes Protegidos e Intactos

Queda terminantemente prohibido modificar o refactorizar sin evidencia y aprobación explícita:

* `VideoWallpaperManager.kt`
* `VideoBackground.kt`
* `WidgetHostManager.kt`
* `NativeWidgetView.kt`
* `ViewOne.kt`
* `ViewTwo.kt`
* `LauncherAccessibilityService.kt`
* `NotificationMonitorService.kt`
* `LauncherNavState`
* `IconCache`
* `ShortcutIcon`
* `AlphabetIndexRail`
* Sistema de gestures del Drawer
* Motion System del Drawer
* Lógica de scroll y selección/cambio de categorías del Drawer
* Ciclo de vida y binding de AppWidgets

---

## 6. Nuevo Bloque Prioritario: Performance Engineering / App Performance

* **ESTADO INICIAL:** ⏳ **AUDITORÍA PENDIENTE (CERO CÓDIGO / CERO OPTIMIZACIONES PREMATURAS)**
* **Objetivo:** Conseguir que NovaLauncher sea extraordinariamente fluido y estable en una gama amplia de dispositivos Android (desde hardware modesto hasta gama alta), evitando optimizaciones a ciegas y manteniendo intactas las funcionalidades validadas.

### Principio Fundamental de Rendimiento
NovaLauncher **NO** debe intentar maximizar el consumo de CPU/GPU/RAM constantemente. La estrategia arquitectónica oficial es:
> *"Usar únicamente los recursos necesarios para mantener la experiencia objetivo y aumentar/reducir la calidad dinámicamente cuando la capacidad y la carga real del dispositivo lo permitan."*

```text
DEVICE CAPABILITY  +  CURRENT LOAD  +  FRAME BUDGET
                        ↓
       ADAPTIVE QUALITY / RESOURCE BUDGET
```
* **Objetivos concretos:** Mantener 120/90/60 FPS estables según pantalla, reducir jank (frames caídos), eliminar trabajo redundante en segundo plano, preservar batería, contener presión de memoria y permitir calidad ultra solo cuando sea seguro.

### Modelo de Capacidad del Dispositivo (Hipótesis para la Auditoría)
No clasificar dispositivos únicamente por fabricante o modelo comercial. Investigar un perfil de capacidades observable y conservador:
* Memoria RAM total y disponible (`ActivityManager.MemoryInfo`).
* Nivel de API / Versión de Android.
* Resolución nativa y tasa de refresco (`Display.mode`).
* Capacidad de CPU/GPU inferida de forma ligera y no invasiva.
* Presión de memoria del sistema (`onTrimMemory`).
* Estado térmico cuando la API esté disponible (`PowerManager.OnThermalStatusChangedListener`).
* Perfiles conceptuales hipotéticos: `CONSTRAINED`, `STANDARD`, `HIGH` *(Hipótesis de diseño; Claude evaluará si son necesarios o si conviene un modelo continuo).*

### Quality Budget (Presupuesto de Calidad)
Cada subsistema potencialmente costoso debe tener un presupuesto evaluado:
* Animaciones y Motion System.
* Blur y transparencias en GlassCard.
* Wallpaper dinámico de video y prebuffering.
* Cantidad de recursos precargados y tamaño de cachés (`IconCache`).
* Procesamiento de imágenes y Adaptive Theming.
* Ciclo de vida y actualización de widgets nativos.
* Recomposiciones y frecuencia de emisión de estados en Compose.
* *Regla:* Primero medir para determinar qué es realmente costoso y qué ya está optimizado.

### Filosofía de Optimización
```text
MEDIR  →  LOCALIZAR  →  CAMBIAR (QUIRÚRGICO)  →  MEDIR OTRA VEZ
```
* **Prohibido:** `SUPONER → CAMBIAR TODO → ESPERAR QUE SEA MÁS RÁPIDO`.
* Toda optimización debe contar con: hipótesis formal, métrica asociada, baseline medido, cambio mínimo aislado, comparativa cuantitativa antes/después y validación física en hardware real.

### Escenarios Críticos a Medir (Journeys Candidatos A–T)
1. **A.** Cold start (desde proceso muerto).
2. **B.** Warm start (regreso desde segundo plano).
3. **C.** Home completamente cargado y reposo.
4. **D.** Abrir y cerrar App Drawer.
5. **E.** Scroll rápido del Drawer (124+ apps).
6. **F.** Scrubbing continuo del `AlphabetIndexRail`.
7. **G.** Cambio de categorías (swipe y tabs).
8. **H.** Búsqueda en tiempo real de aplicaciones.
9. **I.** Apertura y cierre de Settings general.
10. **J.** Apertura y manipulación de sliders en Advanced Settings.
11. **K.** Renderizado de Widgets nativos en pantalla de inicio.
12. **L.** Widgets durante scroll y paginación horizontal.
13. **M.** Wallpaper de imagen estática (carga y render).
14. **N.** Wallpaper de video en reproducción continua.
15. **O.** Regreso desde otra aplicación externa pesada (TikTok/Cámara).
16. **P.** Transiciones del Motion System.
17. **Q.** Cambio de fondo de pantalla en vivo.
18. **R.** Flujo de análisis y generación de Adaptive Theming.
19. **S.** Uso prolongado sostenido (fugas de memoria / retención de buffers).
20. **T.** Comportamiento bajo presión extrema de memoria (`TRIM_MEMORY`).

### Métricas Candidatas
* **Startup:** Time To Initial Display (TTID), Time To Full Display (TTFD).
* **Runtime / Jank:** `frameDurationCpuMs`, `frameOverrunMs`, percentiles P50, P90, P95, P99 de renderizado.
* **Memoria:** PSS / RSS usado, tasa de crecimiento, frecuencia y pausas de GC, detección de fugas.
* **CPU/GPU:** Porcentaje de uso en reposo vs interacción, picos y temperatura.
* **Batería:** Consumo energético por trabajo redundante.
* *Nota:* Los valores objetivo se establecerán a partir del baseline empírico; no se inventarán umbrales arbitrarios.

### Herramientas de Auditoría a Evaluar
* Jetpack Macrobenchmark (priorizando recorridos reales de usuario en hardware físico).
* `FrameTimingMetric` y `StartupTimingMetric`.
* Android Studio Profiler (CPU, Memory, Energy).
* Perfetto / System Trace.
* Baseline Profiles y Startup Profiles (evaluar preparación del proyecto, impacto esperado y coste de mantenimiento).

### Áreas de Investigación en Jetpack Compose (Sin prejuzgar bugs)
* Estabilidad de parámetros y lambdas (`@Stable`, `@Immutable`).
* Uso de `derivedStateOf` vs lecturas directas.
* Defer reads mediante lambdas de Modifier (`graphicsLayer { alpha = ... }`, `offset { ... }`).
* Claves explícitas (`key`) y `contentType` en `LazyColumn` / `LazyVerticalGrid`.
* Evitar recomposiciones innecesarias en observadores de alta frecuencia.

### Multigama — Objetivo Real
* La aplicación debe comportarse de forma excelente en gama baja (2-3 GB RAM, CPUs modestas), gama media (POCO X6 5G de referencia) y gama alta (SoCs flagship, 120+ Hz).
* La auditoría debe proponer una metodología para validar o simular dispositivos con restricciones sin depender exclusivamente del POCO X6.

### Lo Que NO Se Debe Hacer (Anti-Patrones)
* NO realizar optimizaciones globales sin datos de profiling previos.
* NO eliminar funcionalidades ni degradar la calidad visual por "intuición de performance".
* NO modificar componentes protegidos sin evidencia reproducible.
* NO agregar cachés ni concurrencia arbitraria sin cuantificar su beneficio y consumo de RAM.
* NO tocar widgets estables ni eliminar animaciones ya aprobadas.
* NO tomar mediciones en modo Debug como referencia de rendimiento de Release.

### Performance Regression Policy
Toda optimización debe respetar la matriz: **FUNCIONALIDAD + PERFORMANCE**. Un cambio no se acepta si mejora una métrica aislada pero degrada la experiencia del usuario (ej. eliminar jank pero provocar parpadeo de placeholders, o acelerar el arranque pero romper la carga de widgets).

### Estado de Rendimiento Actual Demostrado
* `IconCache = 128` y Drawer en `LazyColumn` plana validados físicamente a 120 FPS sin jank con 124 apps instaladas.
* Nova Motion System 6/6 completamente validado.
* Adaptive Wallpaper Theming opera en memoria transitoria sin retención de recursos.
* Defecto de video resume intermitente aislado en `VideoWallpaperManager` como tarea independiente.
* No existen aún baselines formales de Macrobenchmark, capability profile dinámico ni quality budget implementado.

---

## 7. Próxima Acción Oficial

* **NEXT:** `Performance Engineering — Architecture & Measurement Audit`
* **Primera Acción:** Claude realizará una auditoría profunda de rendimiento y escalabilidad arquitectónica sobre el código real del proyecto antes de cualquier intervención.
* **Alcance de la Auditoría:** Inspección de arquitectura, identificación de cuellos de botella reales, diseño de baselines, selección de critical user journeys, propuesta metodológica multigama, evaluación de Macrobenchmark/Baseline Profiles y definición de paquetes mínimos de trabajo.
* **Restricción:** Claude **NO** debe modificar código del proyecto.

---

## 8. Protocolo de Trabajo y Workflow Oficial

1. **Auditoría / Análisis Técnico** (delimitación del problema con métricas).
2. **Revisión del Reporte** (validación de hipótesis).
3. **Prompt Quirúrgico de Implementación** (alcance mínimo y cerrado).
4. **Implementación con Antigravity** (un solo agente modificando código a la vez).
5. **Verificación Automatizada Completa:** `testDebugUnitTest`, `assembleDebug`, `lintDebug`, `bundleRelease`.
6. **Prueba Física en Dispositivo Real (POCO X6 5G).**
7. **Medición Cuantitativa Comparativa** (antes vs después).
8. **Correcciones Dirigidas** (si se presentan desviaciones).
9. **Confirmación Física.**
10. **Actualización del Cerebro** (`AGENT_PROJECT_STATE.md`).
11. **Git Checkpoint Oficial.**

---

## 9. Seguridad de Credenciales y Clave de Subida (Upload Key v2)

* **Ubicación Keystore Privada:** `C:\Users\migue\NovaLauncher-Keys\upload-keystore-v2.jks` *(FUERA DEL REPO)*
* **Ubicación Certificado PEM Público:** `C:\Users\migue\NovaLauncher-Keys\upload_certificate-v2.pem` *(FUERA DEL REPO)*
* **Alias Oficial:** `novalauncher-upload-v2`
* **Algoritmo y Tamaño:** RSA 4096 bits (`sha384RSA`), validez de 10,000 días (expira en febrero de 2054).
* **Huella Digital SHA-256 del Certificado:**
  ```text
  2A:DF:B7:D2:4E:62:7C:29:52:42:2B:28:D7:11:F3:D9:34:F1:9E:66:FB:B3:05:3F:AB:14:E7:97:82:39:3B:F1
  ```
* **Regla de Seguridad:** Cero claves ni contraseñas en el repositorio. Archivos sensibles ignorados por `.gitignore`.

---

## 10. Validaciones Automatizadas Consolidadas

* `git diff --check`: **PASS** (0 errores de formato, fin de línea o sintaxis).
* `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (52/52 pruebas unitarias aprobadas al 100%).
* `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (APK generado e instalado exitosamente).
* `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errores).
* `.\gradlew.bat bundleRelease`: **BUILD SUCCESSFUL** (Minificación R8 y `lintVitalRelease` exitosos; AAB generado).

---

## 11. Tareas Pendientes y Hoja de Ruta

1. **Performance Engineering — Auditoría Inicial por Claude:** Inspección arquitectónica y diseño del plan de medición de rendimiento multigama.
2. **Estabilización de Video Resume (Independiente):** Diagnóstico y corrección de la re-vinculación de `TextureView` en `VideoWallpaperManager` al regresar de apps pesadas.
3. **Google Play Console — Restablecimiento de Upload Key:** Confirmar el procesamiento del certificado `upload_certificate-v2.pem` por parte de Google (ventana de 24-48 horas).
4. **Fase 5E-3 — Configuración Segura de Firma:** Configurar `signingConfigs.release` en Gradle mediante `keystore.properties` desacoplado fuera del control de versiones.
5. **Accesibilidad y Ficha de Tienda:** Preparar video demostrativo de accesibilidad para Play Console y activos gráficos finales.
