# Estado Persistente del Proyecto — NovaLauncher (Agent State)

> **Documento maestro de sincronización, continuidad operativa y auditoría entre sesiones, Antigravity y Claude.**<br>
> *Última actualización: 19 de septiembre de 2026 (Post Checkpoint Fase 2D — Adaptive Wallpaper Theming).*<br>
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
| **Fase 2E** | Adaptive Theme para VIDEO (Extracción y análisis de frames representativos) | ⏳ **PRÓXIMA FASE / SOLO AUDITORÍA Y DISEÑO** |

> [!IMPORTANT]
> **Fase 2E todavía NO está implementada.** Su alcance inicial es estrictamente de auditoría técnica y diseño arquitectónico para no desestabilizar la reproducción de video ni los componentes de ExoPlayer.

---

### Detalle de Fases de Adaptive Wallpaper Theming

#### Fase 1 — Fundamentos y Wallpaper Analyzer (✅ COMPLETADA / VALIDADA)
* **Objetivo:** Subsistema puro de análisis cromático sobre bitmaps estáticos desacoplado de Compose, ViewModels y persistencia.
* **Componentes:**
  * `ThemePalette`: Modelo inmutable con `@get:ColorInt dominantColor`, `vibrantColor`, `mutedColor`, `isDark: Boolean`, y `averageLuminance: Float`. Incluye `DEFAULT` seguro y neutro para recuperaciones sin excepciones.
  * `WallpaperAnalyzer`: Objeto singleton con `analyze(bitmap, dispatcher = Dispatchers.Default)` y `analyzeSync(bitmap)`.
* **Memoria y Performance:** Redimensionamiento previo a ~200x200 px (`TARGET_MAX_DIMENSION = 200`), memoria contenida en ~160 KB, reciclaje garantizado del bitmap temporal en `finally`. El bitmap original nunca es alterado. Despachado fuera del hilo principal (`Dispatchers.Default`).
* **Calidad Cromática:** `.clearFilters()` en `Palette.Builder` para clasificar fondos puros oscuros, claros o monocromáticos; cálculo de luminancia relativa ponderada por población de cada swatch; clasificación `isDark = averageLuminance < 0.5f`.
* **Tests:** 11 pruebas unitarias en `WallpaperAnalyzerTest` (100% PASS).

#### Fase 2B — Runtime Theme Token Layer (✅ COMPLETADA / VALIDADA / CHECKPOINT)
* **Modelo Inmutable Puro:** Creado `LauncherThemeTokens` (`accentColor`, `surfaceColor`, `textPrimaryColor`, `textSecondaryColor`, `borderColor`) con enteros `@ColorInt Int` y parser hexadecimal propio libre de Compose y dependencias Android.
* **Defaults Compatibles:** Paridad visual 100% con la estética cyberpunk original (`#00F0FF` para acento/borde, `#08080C` para superficies oscuras, `#FFFFFF` para texto principal, `#CCCCCC` para secundario).
* **CompositionLocal Global:** Creado `LocalLauncherThemeTokens` (con extensiones `.accent`, `.surface`, `.textPrimary`, `.textSecondary`, `.border`) inyectado en el root real de composición (`MainActivity.kt` envolviendo a `NavHost`).
* **Consumo Conectado:** Consumo reactivo en `AppDrawerScreen.kt`, `LauncherScreen.kt`, `SettingsScreen.kt` (GlassCard reactivo) y `AdvancedSettingsScreen.kt`.
* **Tests:** 4 pruebas unitarias en `LauncherThemeTokensTest` (100% PASS).

#### Fase 2C — Theme Proposal Generator (✅ COMPLETADA / VALIDADA / CHECKPOINT)
* **Objetivo:** Generación determinista y pura de propuestas semánticas: `ThemePalette` → `ThemeProposalGenerator` → `List<ThemeProposal>`.
* **Modelo:** `ThemeProposal` (`id`, `palette`, `proposedTokens`, `isDark`, `label`, `order`) como estructura transitoria en memoria libre de persistencia.
* **Garantía Estricta de Contraste:** Validación y ajuste sistemático de luminancia por HSL para ratios mínimos WCAG 2.1: $\ge 4.5:1$ en `textPrimaryColor`, $\ge 3.0:1$ en `textSecondaryColor` y `accentColor`, $\ge 1.5:1$ en `borderColor`.
* **Deduplicación Perceptual y Sanity Check:**
  * Evaluado con 5 paletas representativas: oscura azul/violeta, oscura rojiza/magenta, clara azul, clara cálida y monocromática.
  * Resultados: propuestas perceptualmente diferenciadas, deduplicación funcional (distancia Manhattan RGB), contraste WCAG AAA/AA, coherencia accent/surface y legibilidad en claros y oscuros. En paletas monocromáticas se reduce a 1 sola propuesta en lugar de inventar variantes artificiales.
* **Tests:** 12 pruebas unitarias en `ThemeProposalGeneratorTest` (100% PASS).

#### Fase 2D — Propuestas de Tema + Preview + Aplicar para Imágenes (✅ COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT)
* **Flujo Final Validado Físicamente en POCO X6 5G:**
  ```text
  Imagen seleccionada (Galería)
          ↓
  Análisis asíncrono (WallpaperAnalyzer + ThemeProposalGenerator)
          ↓
  ThemeProposalDialog (1 a 4 propuestas semánticas con miniatura visual)
          ↓
  ┌─────────────────┬──────────────────┬─────────────────┬─────────────────┐
  │     Aplicar     │   Personalizar   │    Descartar    │      Back       │
  ├─────────────────┼──────────────────┼─────────────────┼─────────────────┤
  │ Wallpaper nuevo │ Wallpaper nuevo  │ Wallpaper nuevo │ Wallpaper nuevo │
  │ +               │ +                │ +               │ +               │
  │ Persistir tema  │ Abre directamente│ Conserva tema   │ Conserva tema   │
  │ en DataStore    │ AdvancedSettings │ previo          │ previo          │
  │                 │ con propuesta    │                 │                 │
  └─────────────────┴──────────────────┴─────────────────┴─────────────────┘
  ```
* **Comportamiento Físico Validado:**
  * **Aplicar:** Aplica el wallpaper seleccionado y persiste la propuesta de tema seleccionada en preferencias.
  * **Descartar:** Aplica el wallpaper seleccionado y conserva el tema visual previo del usuario.
  * **Back:** Aplica el wallpaper seleccionado y conserva el tema visual previo del usuario.
  * **Personalizar:** Abre directamente `AdvancedSettingsScreen`, utiliza la propuesta seleccionada como punto de partida modificable, mantiene el wallpaper seleccionado y persiste únicamente si el usuario guarda desde la vista avanzada.
* **Corrección de Defectos Post-Validación Física:**
  1. *Bug Personalizar:* Corregido enrutamiento directo hacia `AdvancedSettingsScreen` pasando `initialShowAdvanced = true` a través de `openAdvancedInSettings` en `MainActivity` y `SettingsScreen`. Retorno limpio con BackHandler al inicio.
  2. *Bug Contraste Category Pills:* Eliminados colores fijos `Color.White` en `AppDrawerScreen.kt`. Reemplazados por tokens semánticos `themeTokens.accent` (seleccionado), `themeTokens.textSecondary` (no seleccionado) y borde `themeTokens.border.copy(alpha = 0.15f)`. Probado físicamente en tema claro sobre wallpaper blanco con legibilidad impecable.
* **Tests:** 12 pruebas unitarias en `ThemeProposalFlowHandlerTest` (100% PASS).

---

### Hitos Históricos Anteriores Consolidados

* **Fase 5A (PASS):** Prominent Disclosure in-app y consentimiento afirmativo para `LauncherAccessibilityService` (`AccessibilityDisclosureDialog.kt`). Validado en hardware real.
* **Fase 5B (PASS):** Tarjeta "Información y Privacidad" en `SettingsScreen.kt` con versión dinámica y enlace nativo a la política web.
* **Fase 5C (PASS):** Repositorio dedicado `NovaLauncher-Privacy` publicado en GitHub Pages con contacto oficial `dbreak472@gmail.com`.
* **Fase 5D (PASS):** Auditoría integral de Google Play Console (Data Safety, Accessibility Declaration, Listing, Build Release).
* **Fase 5E-0 a 5E-2 (PASS):** Generación de nueva **Upload Key v2** (`upload-keystore-v2.jks`) y exportación del certificado público RFC/PEM (`upload_certificate-v2.pem`) fuera del repo en `C:\Users\migue\NovaLauncher-Keys\`.
* **PERFORMANCE-03 (VERIFIED):** Capacidad de `IconCache` fijada en 128 entradas (`MAX_ENTRIES = 128`). 100% hit rate en hardware real (412 hits / 0 misses). Retiro de instrumentación temporal.
* **DRAWER & MOTION SYSTEM CHECKPOINT (PASS / VALIDATED):**
  * Stream híbrido en `LazyColumn` plana con `DrawerListItem.Header` y `DrawerListItem.AppRow` a 120 FPS.
  * `AlphabetIndexRail` con canal conflated, tarjeta activa dinámica y scrubbing continuo de A a Z.
  * Swipe horizontal entre categorías desambiguado con touch slop en `PointerEventPass.Initial`.
  * Nova Motion System (6/6 completo): Drawer Open/Close, Category Pill, Rail Feedback, Row Tap Feedback y Resume Fade con corrección `ON_STOP` para evitar micro-parpadeos.

---

## 3. Arquitectura Actual del Adaptive Theming

El subsistema de Theming Adaptativo de NovaLauncher está estructurado en capas desacopladas con responsabilidades estrictas:

1. **`WallpaperAnalyzer`:**
   * Singleton asíncrono responsable de extraer la paleta cromática dominante a partir de un `Bitmap`.
   * Realiza downscaling a ~200x200 px, limpieza de filtros y cálculo de luminancia relativa sin afectar el hilo principal.
2. **`ThemePalette`:**
   * Estructura inmutable portadora de colores extraídos (`dominantColor`, `vibrantColor`, `mutedColor`, `isDark`, `averageLuminance`).
   * Libre de dependencias con Compose o vistas.
3. **`ThemeProposal`:**
   * Entidad de datos transitoria que encapsula una propuesta semántica generada (`id`, `palette`, `proposedTokens`, `isDark`, `label`, `order`).
4. **`ThemeProposalGenerator`:**
   * Motor matemático determinista puro en Kotlin estándar.
   * Transforma una `ThemePalette` en una lista de 1 a 4 `ThemeProposal` aplicando reglas WCAG 2.1 para contraste de texto y bordes.
5. **`ThemeProposalFlowHandler`:**
   * Controlador lógico puro responsable de las transiciones de estado (`handleApply`, `handleDiscard`, `handleBack`, `handleCustomize`) y de la conversión bidireccional entre `ThemeProposal` y `AdvancedStyleConfig`.
6. **`ThemeProposalDialog`:**
   * Diálogo de pantalla completa (`usePlatformDefaultWidth = false`, fondo Glass `#030305` con 95% de opacidad) para la previsualización interactiva de propuestas mediante miniatura sintética `LauncherThemeMiniature`.
7. **`LauncherThemeTokens` & `LocalLauncherThemeTokens`:**
   * Capa global de tokens visuales runtime (`accentColor`, `surfaceColor`, `textPrimaryColor`, `textSecondaryColor`, `borderColor`).
   * Provistos de forma reactiva en `MainActivity` mediante `CompositionLocalProvider` sobre todo el árbol de navegación.
8. **`AdvancedStyleConfig`:**
   * Modelo de configuración y persistencia del launcher. Mantiene total compatibilidad histórica y continúa siendo la fuente única de guardado en disco.

---

## 4. Decisiones de Producto Consolidadas

Las siguientes decisiones se encuentran cerradas y no deben reabrirse sin justificación explícita del usuario:

* **A) Propuesta Opcional:** El Adaptive Wallpaper Theming es una recomendación opcional, nunca un requisito bloqueante para cambiar de fondo de pantalla.
* **B) Labels Semánticos:** Las propuestas emplean exclusivamente etiquetas semánticas universales:
  * `Dominante`
  * `Vibrante`
  * `Equilibrado`
  *(Prohibido el uso de nombres de fantasía o marketing artificial).*
* **C) Cardinalidad Flexible (1 a 4):** El sistema puede emitir entre 1 y 4 propuestas dependiendo de la riqueza cromática del fondo. La UI jamás debe asumir un número fijo de 3.
* **D) Previsualización Transitoria:** La exploración y selección de temas en el diálogo opera en memoria sobre estado transitorio (`updateStyleConfigTransient`). La persistencia en almacenamiento solo se ejecuta mediante la acción explícita de "Aplicar" (o guardar desde ajustes avanzados).
* **E) Independencia de Fondo y Tema en Descartar/Back:** Al descartar o pulsar atrás, el nuevo fondo de pantalla se conserva y se restaura el tema visual previo del usuario.
* **F) Personalización Editable:** La acción "Personalizar" entrega la propuesta elegida como estado base editable dentro de `AdvancedSettingsScreen`.
* **G) Exclusión Inicial de Video:** La reproducción y selección de video wallpaper permanecen fuera del flujo automático de Fase 2D.

---

## 5. Componentes Protegidos e Intactos

Queda terminantemente prohibido modificar, refactorizar o alterar las siguientes piezas arquitectónicas críticas:

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

> [!CAUTION]
> **Fase 2E debe diseñarse alrededor del sistema actual de video.** No se permite modificar inicialmente `VideoWallpaperManager` ni `VideoBackground`.

---

## 6. Hoja de Ruta y Reglas para la Próxima Fase 2E (Video Adaptive Theming)

### Naturaleza de la Fase 2E
* **ESTADO:** ⏳ **SOLO AUDITORÍA Y DISEÑO TÉCNICO. CERO IMPLEMENTACIÓN DE CÓDIGO.**

### Objetivo Conceptual
```text
Video Wallpaper (.mp4 / URI)
       ↓
Extracción representativa de frames (MediaMetadataRetriever / alternativa no bloqueante)
       ↓
Análisis cromático puntual (WallpaperAnalyzer)
       ↓
Generación de propuestas (ThemeProposalGenerator)
       ↓
Mismo modelo ThemeProposal y mismo diálogo ThemeProposalDialog (Fase 2D)
```

### Reglas Técnicas Obligatorias para Fase 2E
1. **Sin Análisis Continuo:** Prohibido analizar el video frame por frame durante la reproducción.
2. **Ejecución Única:** El análisis ocurre exclusivamente al seleccionar o cambiar el video de fondo, con estrategia de caché posterior.
3. **Puntos Críticos a Auditar:**
   * Métodos de extracción de frames eficientes en Android (APIs nativas vs ExoPlayer).
   * Cantidad óptima de frames (ej. 1 a 3 frames: inicio, mitad, tercio) y representatividad temporal.
   * Impacto en videos de alta resolución (1080p, 4K) y videos de larga duración.
   * Consumo de CPU, memoria RAM y contención de asignaciones de `Bitmap`.
   * Mecanismos de cancelación inmediata si el usuario cancela o sale de la pantalla.
   * Elección del `CoroutineDispatcher` apropiado (`Dispatchers.IO` / `Dispatchers.Default`).
   * Algoritmo de combinación o fusión de paletas multiframe.
   * Manejo de fallbacks ante fallos de decodificación de códecs locales o formatos incompatibles.
   * Experiencia de usuario (latencia percibida, spinners o indicadores de carga no invasivos).

---

## 7. Protocolo de Trabajo con Claude (Auditor / Revisor Adversarial)

Para mantener la máxima robustez en el proyecto:

1. **Rol de Claude:** Actuará exclusivamente como **AUDITOR Y REVISOR ADVERSARIAL**.
2. **Contexto de Operación:** Claude debe inspeccionar el estado real del proyecto tomando [AGENT_PROJECT_STATE.md](file:///c:/Users/migue/AndroidStudioProjects/AnimeLauncher/docs/AGENT_PROJECT_STATE.md) como única fuente de verdad.
3. **Prohibición de Edición:** Claude **NO** debe modificar código directamente.
4. **Entregables de Claude:** Informes de arquitectura, identificación de vulnerabilidades, análisis de rendimiento y recomendaciones técnicas estructuradas.
5. **Implementación:** Antigravity implementará código únicamente tras revisar, validar y autorizar formalmente el diseño de auditoría.
6. **Protección Estricta:** No se aceptarán propuestas que sugieran modificar innecesariamente los componentes protegidos.

---

## 8. Workflow Oficial del Proyecto

Todo avance técnico en NovaLauncher debe apegarse al siguiente ciclo de 11 pasos:

1. **Auditoría / Análisis Técnico** (identificación de límites y componentes afectados).
2. **Revisión del Reporte** (validación de supuestos y descarte de refactors oportunistas).
3. **Prompt Quirúrgico de Implementación** (delimitación estricta de alcance).
4. **Implementación con Antigravity** (edición mínima indispensable y aislada).
5. **Verificación Automatizada Completa:**
   * `testDebugUnitTest`
   * `assembleDebug`
   * `lintDebug`
   * `bundleRelease`
6. **Prueba Física en Dispositivo Real (POCO X6 5G).**
7. **Correcciones Dirigidas** (solamente si la validación física evidencia defectos).
8. **Nueva Prueba Física de Confirmación.**
9. **Actualización del Cerebro** (`AGENT_PROJECT_STATE.md`).
10. **Git Checkpoint Oficial.**
11. **Paso a la Siguiente Fase.**

> [!IMPORTANT]
> **Principio de Aislamiento:** UN SOLO AGENTE MODIFICANDO EL PROYECTO A LA VEZ.

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
* **Regla de Seguridad de Credenciales:** Prohibido exponer a agentes contraseñas, archivos `.jks`, `.keystore`, `.pem` o `keystore.properties`. Todas las reglas de exclusión deben permanecer activas en `.gitignore`.

---

## 10. Validaciones Automatizadas y Físicas

### Validaciones Automatizadas (100% PASS)
* `git diff --check`: **PASS** (0 errores de formato, fin de línea o sintaxis).
* `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (45/45 pruebas unitarias aprobadas al 100%).
* `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (APK generado e instalado exitosamente).
* `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errores).
* `.\gradlew.bat bundleRelease`: **BUILD SUCCESSFUL** (Minificación R8 y `lintVitalRelease` exitosos; AAB generado).

### Validación Física en Hardware Real (POCO X6 5G — HyperOS / Android 14)
* **Flujo Normal de Selección:** **PASS** (despliegue fluido del diálogo tras elegir imagen).
* **Acción Aplicar:** **PASS** (wallpaper nuevo aplicado y tema persistido correctamente).
* **Acción Descartar:** **PASS** (wallpaper nuevo aplicado y estilo previo restaurado).
* **Acción Back:** **PASS** (wallpaper nuevo aplicado y estilo previo restaurado).
* **Acción Personalizar:** **PASS** (apertura directa de `AdvancedSettingsScreen` con la propuesta como base).
* **Contraste Extremo (Tema Claro / Wallpaper Blanco):** **PASS** (category pills con contraste óptimo y texto legible).
* **Rendimiento e Integridad:** **PASS** (cero memory leaks, cero bloqueos del hilo principal, 120 FPS sostenidos).

---

## 11. Tareas Pendientes y Siguientes Pasos

1. **Fase 2E — Adaptive Theme para VIDEO:** Realizar auditoría técnica y diseño arquitectónico para la extracción de frames representativos sin modificar componentes protegidos.
2. **Google Play Console — Restablecimiento de Upload Key:** Confirmar el procesamiento del certificado `upload_certificate-v2.pem` por parte de Google Play Console (ventana de 24-48 horas).
3. **Fase 5E-3 — Configuración Segura de Firma:** Configurar `signingConfigs.release` en Gradle mediante `keystore.properties` desacoplado fuera del control de versiones.
4. **Accesibilidad y Ficha de Tienda:** Preparar video demostrativo de accesibilidad para Play Console y activos gráficos de la ficha (icono 512x512 y banner 1024x500).
