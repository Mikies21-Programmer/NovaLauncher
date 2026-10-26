# Estado Persistente del Proyecto — NovaLauncher (Agent State)

> **Documento maestro de sincronización, continuidad operativa y auditoría entre sesiones, Antigravity y Claude.**<br>
> *Última actualización: 26 de octubre de 2026 (Release Symbols + Privacy Policy Sync 1.6.1 versionCode 9 — AAB y mapping verificados; sincronización web completada; NOVA-RESUME-FADE-01, NOVA-VIDEO-RESUME-02 y NOVA-APP-DRAWER-ICONS-01: FIX APPLIED — PENDING DEVICE CONFIRMATION).*<br>
> *Regla de seguridad estricta: CERO secretos, contraseñas, keystores ni claves privadas en este repositorio.*

---

## 1. Identidad y Parámetros Base
* **Proyecto:** NovaLauncher
* **Repositorio Principal:** `Mikies21-Programmer/NovaLauncher`
* **Directorio Local:** `C:\Users\migue\AndroidStudioProjects\AnimeLauncher`
* **Application ID:** `com.daybreak.animelauncher`
* **Versión Candidata (Release):** `versionCode = 9`, `versionName = "1.6.1"` (Compilada y verificada en AAB)
* **Versión Previa:** `versionCode = 8`, `versionName = "1.6"`
* **SDKs:** `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`
* **Política de Privacidad Pública:** `https://mikies21-programmer.github.io/NovaLauncher-Privacy/`
* **Contacto Oficial:** `dbreak472@gmail.com`

---

## 2. Estado Oficial de Módulos y Fases

### Tabla de Estado Oficial de Ciclo de Vida y Motion (NOVA-RESUME-FADE-01 / NOVA-VIDEO-RESUME-02)

| Fase / Tarea | Alcance Técnico | Estado Oficial |
| :--- | :--- | :---: |
| **NOVA-RESUME-FADE-01** | Eliminación de Resume Fade (`resumeAlpha`, `hasBeenStopped`, `resumeAnimJobHolder`, `graphicsLayer` del Box raíz en `LauncherScreen.kt`), preservación fail-closed de Hidden Apps en `ON_STOP` vía función pura `shouldRevokeHiddenAppsSession()` y 7 pruebas unitarias nuevas | ⏳ **FIX APPLIED — PENDING DEVICE CONFIRMATION** |
| **NOVA-VIDEO-RESUME-02** | Auditoría forense de reanudación de video, extracción de política pura de recuperación `evaluateVideoResumeAction` (recuperación determinista de STATE_IDLE, STATE_ENDED y playerError; preservación estricta sin reinicio de STATE_READY y STATE_BUFFERING) y 6 pruebas unitarias nuevas | ⏳ **FIX APPLIED — PENDING DEVICE CONFIRMATION** |
| **NOVA-APP-DRAWER-ICONS-01** | Precalentamiento de iconos en segundo plano, coalescencia de solicitudes en vuelo, desacople de cancelación de composables, invalidación reactiva por ciclo de vida de paquetes, eliminación del fallback genérico blanco durante carga normal y 10 pruebas unitarias nuevas | ⏳ **FIX APPLIED — PENDING DEVICE CONFIRMATION** |

---

### Tabla de Estado Oficial de Apps Ocultas (Hidden Apps)

| Fase | Alcance Técnico | Estado Oficial |
| :--- | :--- | :---: |
| **HIDDEN-APPS-AUDIT-01** | Auditoría estática integral de arquitectura, seguridad y flujos | ✅ **COMPLETADA** |
| **HIDDEN-APPS-IMPLEMENTATION-01** | Implementación completa (Gesto spread, Bóveda Keystore, Auth, UI, Drawer) | ✅ **COMPLETADA** |
| **HIDDEN-APPS-FIX-01** | Cierre de bypass de reset, FLAG_SECURE total, timeout 60s, density dp en gesto, exclusión mutua de gestos, sincronización de idioma, hardening de sesión y tests | ✅ **COMPLETADA** |
| **HIDDEN-APPS-BACKUP-ISOLATION** | Aislamiento estricto de Android Backup (`nova_hidden_apps_prefs.xml`), migración segura (Caso A vs B), exclusión en XMLs, prevención de lockout, suite de 127 tests (100% PASS), validación física en POCO X6 5G (P1-P4, P6, P8-P10, C1, C2 aprobados; P5 D2D Xiaomi pendiente; P7 riesgo preexistente) | ✅ **CONSOLIDADA / VALIDADA FÍSICAMENTE** |

---

### Tabla de Estado Oficial de Performance Engineering

| Fase | Alcance Técnico | Estado Oficial |
| :--- | :--- | :---: |
| **P0** | Auditoría estática y localización de hotspots | ✅ **COMPLETADA** |
| **P1** | Instrumentación (`:benchmark`) y Baseline Cuantitativa empírica | ✅ **COMPLETADA / VALIDADA** |
| **P2-01** | Optimización Quirúrgica del Search del App Drawer | ✅ **COMPLETADA / VALIDADA FÍSICAMENTE** |
| **P2-02A** | loadState Trace (I/O, Gson, Widgets en hilo principal) | ✅ **COMPLETADA** |
| **P2-02B** | Cold Start Map (Timeline completo y delimitación de frames) | ✅ **COMPLETADA** |
| **P2-02C** | Composition Trace (Perfetto track_event y hotspots de Compose) | ✅ **COMPLETADA** |
| **P2-03** | EXP-PAGER-ZERO-PRELOAD (`beyondViewportPageCount = 0`) | ✅ **COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT PENDIENTE** |
| **P2-04** | Siguiente Hotspot de Composición / Layout | ⏳ **AUDITORÍA PENDIENTE** |

---

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

## 4. Arquitectura y Seguridad de Apps Ocultas (Hidden Apps)

Apps Ocultas (`HIDDEN-APPS-FIX-01`) proporciona una bóveda privada local para ocultar aplicaciones seleccionadas del App Drawer, el rail A-Z, las categorías y los resultados de búsqueda, con aislamiento criptográfico y fail-closed de sesión:

1. **`HiddenAppsModels.kt`:** Modelos de datos inmutables y enums para modos de autenticación (`NONE`, `BIOMETRIC`, `DEVICE_CREDENTIAL`, `OWN_PIN`, `OWN_PATTERN`, `OWN_PASSWORD`), estados de flujo de UI (`Closed`, `Setup`, `Auth`, `AuthenticatedScreen`) y estado de configuración (`HiddenAppsConfig`).
2. **`HiddenAppsCrypto.kt`:** Derivación criptográfica mediante PBKDF2WithHmacSHA256 con salt criptográfico de 16 bytes (`SecureRandom`), 120,000 iteraciones, comparación en tiempo constante (`MessageDigest.isEqual`), validadores estrictos y sanitización en memoria (`wipe`).
3. **`HiddenAppsCredentialStore.kt`:** Cifrado por hardware en `AndroidKeyStore` con clave simétrica AES-GCM (256 bits, `NoPadding`) y persistencia del ciphertext e IV en un archivo SharedPreferences exclusivo (`nova_hidden_apps_vault.xml`), excluido formalmente de backups en la nube y transferencias de dispositivo.
4. **`HiddenAppsPreferencesStore.kt`:** Almacén persistente exclusivo (`nova_hidden_apps_prefs.xml`) para la configuración funcional (`isConfigured`, `authMode`, `hiddenPackages`), excluido explícitamente de cloud backup, device transfer y legacy backups en `data_extraction_rules.xml` y `backup_rules.xml`.
5. **`HiddenAppsMigrator.kt`:** Resolvedor y migrador que distingue de forma determinista entre actualización local (Caso A: usuario existente en el mismo dispositivo conservando contexto de seguridad Keystore/biometría) y restore de backup antiguo / instalación nueva (Caso B: descarta configuración huérfana para evitar estados zombies y lockout).
6. **`HiddenAppsAuthenticator.kt`:** Coordinador de verificación de credenciales con cooldown de 30 segundos tras 5 intentos fallidos consecutivos, soporte para `BiometricPrompt` (`BIOMETRIC_STRONG`) y credenciales del dispositivo (`createConfirmDeviceCredentialIntent`).
7. **`HiddenAppsRepository.kt`:** Repositorio central que coordina la persistencia del estado aislado y las credenciales en `HiddenAppsCredentialStore`. Incorpora `verifyAndReset()` para eliminar cualquier camino de bypass de reset.
8. **`HiddenAppsGesture.kt`:** Detector táctil multitáctil de 2 dedos (separación hacia afuera) con zona central geométrica (35%), zoom trigger >= 1.45x, escala inicial dependiente de densidad (`MIN_INITIAL_DISTANCE_DP = 24f`) y exclusión de gestos consumidos.
9. **`HiddenAppsScreen.kt`:** Pantalla modal autenticada con `FLAG_SECURE` en la ventana, filtrado por pestañas ("Todas" / "Ocultas"), búsqueda en vivo, auto-cierre tras 60 segundos de inactividad táctil (`PointerEventPass.Initial`) y diálogo de confirmación de reseteo.
10. **`HiddenAppsAuthDialog.kt` & `HiddenAppsSetupDialog.kt`:** Modales seguros con `SecureFlagPolicy.SecureOn` y `FLAG_SECURE` en la Activity Window. El flujo de reseteo exige de forma obligatoria la credencial Nova actualmente configurada o credencial segura del dispositivo (prohibido reset sin autenticación cuando `isDeviceSecure == false`).
11. **Integración con `AppDrawerScreen.kt`:** Filtrado de `hiddenPackages` antes de `prepareDrawerCategory(appsInCat)`, garantizando desaparición completa del Drawer, A-Z y búsqueda, preservando al 100% las guardas de `CRASH-FIX-01`.

### Decisiones de Arquitectura y Seguridad Consolidadas (HIDDEN-APPS-FIX-01 / HIDDEN-APPS-BACKUP-ISOLATION)
* **A) Reset Protegido (Eliminación de Bypass):** El reseteo de protección nunca puede ejecutarse sin autenticación. En dispositivos sin bloqueo del sistema (`isDeviceSecure == false`), se exige estrictamente la credencial Nova (`OWN_PIN`, `OWN_PATTERN`, `OWN_PASSWORD`) mediante `verifyAndReset()`. Un reseteo cancelado o con credencial errónea jamás altera la configuración ni revela paquetes.
* **B) FLAG_SECURE Completo:** Aplicado mediante `SecureFlagPolicy.SecureOn` y en la ventana de la Activity a `HiddenAppsSetupDialog`, `HiddenAppsAuthDialog` y `HiddenAppsScreen`, impidiendo capturas de pantalla, grabaciones y previsualizaciones en multitarea durante todo el ciclo sensible.
* **C) Auto-Cierre por Inactividad (60 segundos):** En `HiddenAppsScreen` autenticada, un temporizador de 60 segundos descarta la sesión y vuelve a `Closed` si no hay actividad táctil. Cualquier toque en pantalla (`PointerEventPass.Initial`) reinicia el contador.
* **D) Consistencia de Idioma:** Las pantallas y modales de Apps Ocultas se envuelven en `CompositionLocalProvider(LocalContext, LocalConfiguration)` configurado con `Locale.forLanguageTag(state.language)`, respondiendo instantáneamente al toggle de idioma interno de NovaLauncher ("es" / "en").
* **E) Sin Acceso Alternativo (Descubrimiento Único por Gesto):** No se añaden accesos en Settings, widgets, iconos ni destinos NavHost. El gesto spread outward es la única vía de acceso.
* **F) Exclusión Mutua de Gestos:** `LauncherScreen.kt` comprueba `hiddenAppsFlowState == Closed` antes de disparar el pinch-in de Settings, y `HiddenAppsGesture.kt` descarta punteros consumidos, impidiendo que una secuencia multitáctil continua dispare consecutivamente ambos flujos.
* **G) Aislamiento Estricto de Android Backup (Decisión D-33):** El estado funcional de Hidden Apps se mantiene fuera del Android Backup. Se persiste exclusivamente en `nova_hidden_apps_prefs.xml` y se excluye formalmente en `backup_rules.xml` y `data_extraction_rules.xml`. `LauncherViewModel.saveState()` persiste `hiddenAppsConfig = HiddenAppsConfig()` por defecto en `launcher_prefs.xml`, eliminando cualquier filtración de privacidad hacia la nube de Google o riesgo de lockout al restaurar copias de seguridad.
* **H) Residual C2 Documentado (Ventana Transitoria de Primer Launch):** Caracterizado empíricamente en hardware real POCO X6 5G:
  - Requiere upgrade real previo (`lastUpdateTime > firstInstallTime`).
  - La instalación nunca ha ejecutado la build de aislamiento (sin marcador `nova_local_install.marker` en `noBackupFilesDir`).
  - Se restaura configuración legacy de `BIOMETRIC` o `DEVICE_CREDENTIAL`.
  - Ocurre exclusivamente en el primer launch.
  - Es una ventana de un solo arranque (tras el cual se estampa el marcador y se crea la preferencia aislada).
  - No afecta a modos `OWN_*` (`OWN_PIN`, `OWN_PATTERN`, `OWN_PASSWORD`), los cuales exigen estrictamente el vault y Keystore local.
  - No produce lockout.
  - No produce bypass.
  - Es recuperable mediante reset protegido (`verifyAndReset()`).
  - Quedará cerrada definitivamente en una futura release cuando el marcador local sea requisito obligatorio para migrar modos del sistema.
* **I) Estado de Validación Física P5 (D2D Xiaomi):** Permanece formalmente como **PENDING / NOT PHYSICALLY VALIDATED**. Las reglas XML contienen la exclusión en `<device-transfer>`, pero la transferencia D2D específica de Xiaomi no fue probada físicamente en hardware real.
* **J) Riesgo Preexistente P7 (Keystore Invalidado):** Si el Keystore del dispositivo resulta invalidado a nivel de sistema, una configuración válida previa de Hidden Apps impedirá la autenticación con credenciales `OWN_*`. Se documenta formalmente como **riesgo PREEXISTENTE** inherente a la arquitectura de `AndroidKeyStore` de hardware, NO introducido ni asociado a la implementación de aislamiento de backup.

---

## 5. Tabla Oficial de Gestos del Sistema (Home Screen Gestures)

| Gesto | Punteros y Región | Acción / Resultado | Componente Destino | Inhibidores / Exclusiones |
| :--- | :--- | :--- | :--- | :--- |
| **Doble Toque (Double Tap)** | 1 dedo, área vacía del escritorio | Bloquear / Apagar pantalla | `LauncherAccessibilityService` (`GLOBAL_ACTION_LOCK_SCREEN`) | Área de widgets, iconos de apps, modo edición |
| **Deslizar Abajo (Swipe Down)** | 1 dedo, arrastre vertical hacia abajo | Expandir panel de notificaciones | `LauncherAccessibilityService` (`GLOBAL_ACTION_NOTIFICATIONS`) | Área de widgets, inicio sobre iconos, modo edición |
| **Deslizar Arriba (Swipe Up)** | 1 dedo, arrastre vertical hacia arriba | Desplegar App Drawer | `AppDrawerScreen` | Área de widgets, modo edición |
| **Pellizcar hacia Adentro (Pinch In)** | 2 dedos, contracción (zoom < 0.75x) | Abrir Ajustes de NovaLauncher | `SettingsScreen` | Modo edición de widgets, inicio en widgets, Apps Ocultas activa |
| **Separación hacia Afuera (Spread Outward)** | 2 dedos, inicio en zona central 35%, separación >= 1.45x, distancia inicial >= 24dp | Abrir flujo protegido de Apps Ocultas | `HiddenAppsScreen` / Diálogos Setup/Auth | Modo edición de widgets, overlays activos, inicio en widgets, pinch en curso, sesión ya abierta |

---

## 6. Decisiones de Producto Consolidadas (Theming y Sistema)

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
* `WidgetHostManager.kt` *(Nota: Puede ser inspeccionado para la auditoría de Cold Start al formar parte del coste detectado en loadState, pero no debe modificarse durante la auditoría).*
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
* Adaptive Wallpaper Theming (Tokens, Palette, Analyzer, Generador y Flow)

---

## 6. Performance Engineering / App Performance

### Estado Oficial de Fases
* **P0 — Auditoría Estática de Hotspots:** ✅ **COMPLETADA**
* **P1 — Instrumentación y Baseline Cuantitativa:** ✅ **COMPLETADA / VALIDADA**
* **P2-01 — Optimización Quirúrgica del Search del App Drawer:** ✅ **COMPLETADA / VALIDADA FÍSICAMENTE**
* **P2-02A — loadState Trace (I/O, Gson, Widgets en hilo principal):** ✅ **COMPLETADA**
* **P2-02B — Cold Start Map (Timeline completo y delimitación de frames):** ✅ **COMPLETADA**
* **P2-02C — Composition Trace (Perfetto track_event y hotspots de Compose):** ✅ **COMPLETADA**
* **P2-03 — EXP-PAGER-ZERO-PRELOAD (`beyondViewportPageCount = 0`):** ✅ **COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT PENDIENTE**
* **P2-04 — Siguiente Hotspot de Composición / Layout:** ⏳ **AUDITORÍA PENDIENTE**

---

### P1 — Infraestructura y Baseline Oficial de Referencia

Se cuenta con infraestructura de medición cuantitativa real, aislada en el módulo `:benchmark` (`com.android.test`), ejecutada sobre hardware físico sin modificar la app de producción en Release:
* **Módulo:** `:benchmark` (Jetpack Macrobenchmark 1.3.3, UIAutomator 2.3.0, AndroidX Test Runner 1.6.2).
* **Test Suites:** `StartupBenchmark` (`P1-STARTUP-COLD`, `P1-STARTUP-WARM`) y `DrawerFrameBenchmark` (`P1-DRAWER-SCROLL`, `P1-DRAWER-SEARCH`).
* **BuildType:** `benchmark` (`initWith(release)`, R8 minificado habilitado, shrinking activo, `<profileable android:shell="true" />`).
* **Documentación Oficial:** [`docs/PERFORMANCE_BASELINE_P1.md`](file:///c:/Users/migue/AndroidStudioProjects/AnimeLauncher/docs/PERFORMANCE_BASELINE_P1.md).

#### Dispositivo Físico de Referencia (Baseline Congelada)
* **Modelo:** POCO X6 5G (`23122PCD1G`, codename `garnet`)
* **SoC / CPU:** Qualcomm Snapdragon 7s Gen 2 (8 núcleos hasta 2.40 GHz, frecuencias sin modificar)
* **Memoria RAM:** 12 GB LPDDR4X (~4.7 GB disponible en ejecución)
* **Sistema Operativo:** Android 16 (API 36, user/release-keys)
* **Pantalla / Refresh Rate:** 1220×2712 px @ 120.0 Hz nativos (presupuesto de frame objetivo: **8.33 ms**)

#### Baseline de Startup (Congelada Histórica)
* **Cold TTID:** Mínimo: 537 ms | Mediana: **739 ms** | Máximo: 978 ms | Media: 736.8 ms
* **Warm TTID:** Mínimo: 120 ms | Mediana: **139 ms** | Máximo: 170 ms | Media: 144.0 ms (81.2% más rápido que cold)
* **TTFD:** No disponible actualmente porque la app no implementa `reportFullyDrawn()`. *(Regla estricta: NO inventar métricas ni estimar valores teóricos).*

#### Baseline de Frames en App Drawer (Congelada)
* **P1-DRAWER-SCROLL (120 Hz):**
  * `frameDurationCpuMs`: P50 = 5.9 ms | P90 = 11.4 ms | P95 = 12.9 ms | P99 = 30.0 ms
  * `frameOverrunMs`: P50 = -0.04 ms | P90 = 9.75 ms | P95 = 10.35 ms | P99 = 24.62 ms
* **P1-DRAWER-SEARCH (Baseline previa a optimización):**
  * `frameDurationCpuMs`: P50 = 8.2 ms | P90 = 20.9 ms | P95 = 31.6 ms | P99 = 66.6 ms (pico en traza: 218.3 ms)
  * `frameOverrunMs`: P50 = 2.5 ms | P90 = 23.9 ms | P95 = 41.7 ms | P99 = 112.5 ms

---

### P2-01 — Optimización Quirúrgica del Search del App Drawer (✅ COMPLETADA / VALIDADA FÍSICAMENTE)

* **Causa Técnica Confirmada:** En cada pulsación de tecla, `buildDrawerData` ejecutaba en el hilo principal: llamadas JNI a `Normalizer.normalize(..., Form.NFD)` por cada app instalada, `groupBy` repetido, reordenamiento alfabético O(N log N) con `sortedWith` y reconstrucción efímera de objetos `DrawerListItem`.
* **Solución Quirúrgica:**
  1. Desacoplamiento de trabajo estable (`prepareDrawerCategory`) y dinámico (`filterDrawerData`).
  2. La estructura categorizada y ordenada alfabéticamente se prepara **una única vez** al cambiar apps o categorías.
  3. Pre-normalización de nombres (`lowerName`) en memoria transitoria local.
  4. Filtrado directo O(N) por `searchQuery` reutilizando instancias precalculadas, sin sorting, sin grouping y con cero llamadas a `Normalizer`.
  5. Retorno instantáneo O(1) de `baseData` cuando la búsqueda está vacía.

#### Resultados Comparativos Empíricos (Hardware POCO X6 5G — 120 Hz)

| Métrica | Baseline P1 (Antes) | Optimizado P2-01 (Después) | Delta Numérico | Delta % |
| :--- | :---: | :---: | :---: | :---: |
| **`frameDurationCpuMs` P50** | 8.2 ms | **5.2 ms** | -3.0 ms | **-36.6%** |
| **`frameDurationCpuMs` P90** | 20.9 ms | **14.6 ms** | -6.3 ms | **-30.1%** |
| **`frameDurationCpuMs` P95** | 31.6 ms | **19.9 ms** | -11.7 ms | **-37.0%** |
| **`frameDurationCpuMs` P99** | 66.6 ms | **34.4 ms** | -32.2 ms | **-48.3%** |
| **`frameOverrunMs` P50** | 2.5 ms | **1.6 ms** | -0.9 ms | **-36.0%** |
| **`frameOverrunMs` P90** | 23.9 ms | **12.9 ms** | -11.0 ms | **-46.0%** |
| **`frameOverrunMs` P95** | 41.7 ms | **17.1 ms** | -24.6 ms | **-59.0%** |
| **`frameOverrunMs` P99** | 112.5 ms | **36.3 ms** | -76.2 ms | **-67.7%** |

* **Validaciones Aprobadas:**
  * Pruebas Unitarias: **62/62 PASS** (incluyendo 10 pruebas de equivalencia en `DrawerSearchPerformanceUnitTest`).
  * `assembleDebug` PASS, `lintDebug` PASS (0 errores), `bundleRelease` PASS.
  * Validación Física en POCO X6: Búsqueda instantánea sin stuttering perceptible; scroll y `AlphabetIndexRail` sin regresiones.

---

### Incidente de Benchmark y Regla Permanente

> [!WARNING]
> **Incidente Registrado:** Durante la preparación del entorno de benchmark se desinstaló temporalmente el paquete activo `com.daybreak.animelauncher`, lo que requirió reinstalarlo manualmente al ser el launcher home predeterminado del sistema.
>
> **Regla Permanente de Operación:**
> **"NO DESINSTALAR `com.daybreak.animelauncher` SI ES EL HOME ACTIVO."**
> Para benchmarking y pruebas futuras utilizar exclusivamente `am force-stop`, cambio transitorio de foreground a Settings (`am start -S com.android.settings/.Settings`), o reemplazo directo (`install -r -t`). Esto no constituye un bug de producción.

---

### P2-02 — Diagnóstico Integral de Cold Start (✅ COMPLETADA)

* **P2-02A (`loadState` Trace):** Se demostró empíricamente en variante optimizada `benchmark` (R8) que `loadState()` consume únicamente **10.06 ms** de mediana (~1.6% del Cold Start). No era el bottleneck dominante de 739 ms.
* **P2-02B (Cold Start Map):** Mapeo exhaustivo de timeline en Perfetto (`linux.ftrace`):
  * `bindApplication`: 87.29 ms
  * `activityResume`: 96.31 ms
  * **Primer `Choreographer#doFrame`:** **201.22 ms** (dominado por `AndroidOwner:onMeasure` en **85.09 ms**).
* **P2-02C (Composition Trace):** Rastreo de composición con Perfetto `track_event`:
  * Subcomposición de `BoxWithConstraints`: **20.83 ms** (ViewOne: 13.16 ms + ViewTwo: 7.67 ms).
  * Multiplicación por `ShortcutIcon`: **18.65 ms** / 36 llamadas (~0.51 ms por instancia, costo nominal inflado por doble pase y precarga).
  * Precomposición no deseada de `ViewTwo`: **9.49 ms** de composición propia en el primer frame.
  * `ScaffoldLayout`: 10.93 ms (Material 3) y `NavHost`: 10.34 ms (router raíz).

---

### P2-03 — EXP-PAGER-ZERO-PRELOAD (✅ COMPLETADA / VALIDADA FÍSICAMENTE / CHECKPOINT PENDIENTE)

#### 1. Cambio Exacto Realizado
En [LauncherScreen.kt:369](file:///c:/Users/migue/AndroidStudioProjects/AnimeLauncher/app/src/main/java/com/daybreak/animelauncher/ui/screens/LauncherScreen.kt#L369):
```kotlin
HorizontalPager(
    state = pagerState,
    userScrollEnabled = (navState == LauncherNavState.Home),
    beyondViewportPageCount = 0, // Cambio exclusivo: de 1 a 0
    key = { it },
    ...
```
**Fue el único cambio productivo del experimento.** Cero cambios adicionales.

#### 2. Resultados Oficiales (Hardware Real POCO X6 5G — 120 Hz)
Medición formal con variante `benchmark` (R8, sin runtime-tracing que contamine CPU):

* **Cold Start — Baseline A (`beyondViewportPageCount = 1`):**
  * `TotalTime` median = **532 ms** (Runs: 548, 532, 519, 538, 523 ms)
  * `WaitTime` median = **538 ms** (Runs: 551, 538, 525, 542, 527 ms)
  * `Startup Jank Rate` = **16.29%**
* **Cold Start — Baseline B (`beyondViewportPageCount = 0`):**
  * `TotalTime` median = **519 ms** (Runs: 491, 505, 519, 615, 637 ms)
  * `WaitTime` median = **523 ms** (Runs: 499, 511, 523, 620, 643 ms)
  * `Startup Jank Rate` = **5.98%**
* **Delta Cuantitativo:**
  * `TotalTime` median = **-13 ms (-2.4%)**
  * `WaitTime` median = **-15 ms (-2.8%)**
  * `TotalTime` mínimo = **491 ms** (Récord histórico absoluto, rompe barrera de 500 ms)
  * `Startup Jank Rate` = **-10.31% (-63.3% relativo)**
* **Warm Start (5 runs):**
  * `TotalTime` median = **124 ms** (Paridad 1:1, cero regresión).
* **Primer Swipe ViewOne → ViewTwo (Hardware Real a 120 Hz / presupuesto 8.33 ms):**
  * *Immediate (swipe en cuanto aparece Home):* P50 ≈ **5 ms** | P90 ≈ **10 ms** | P95 ≈ **14 ms** | Jank = **0.00%** (162 frames, 0 janky)
  * *Delayed 300 ms:* P50 ≈ **6 ms** | P90 ≈ **8 ms** | P95 ≈ **13 ms** | Jank = **1.45%** (138 frames, 2 janky)
  * *Repeated:* P50 ≈ **6 ms** | P90 ≈ **8 ms** | P95 ≈ **9 ms** | Jank = **0.00%** (142 frames, 0 janky)

#### 3. Interpretación Sobria
* **P2-03 se conserva porque:**
  1. Mejora de forma reproducible la mediana de Cold Start (**-13 ms**).
  2. Reduce drásticamente la contención y jank observada en startup (**-63.3%**).
  3. No degrada Warm Start (**124 ms** paritario).
  4. No degrada el primer swipe (P50 de 5 ms, holgadamente bajo el presupuesto de 8.33 ms a 120 Hz).
  5. Cero regresiones visuales o funcionales en fondos, widgets o navegación durante la validación física.
* **IMPORTANTE:** No afirmar que todos los 13 ms de mejora provienen exclusivamente de ViewTwo. Existe variabilidad entre runs; el resultado se registra como mejora empírica global del experimento A/B.

#### 4. Decisión sobre ViewTwo
* `ViewTwo` ya **NO** se precompone en cold start al operar con `beyondViewportPageCount = 0`.
* **NO** realizar ahora otra optimización sobre `ViewTwo`, `ShortcutIcon` ni `BoxWithConstraints`. Cualquier cambio adicional requiere nuevo profiling y experimento aislado.

---

### Próximo Hotspot: P2-04 — Composición / Layout (⏳ AUDITORÍA PENDIENTE)

* **Estado:** ⏳ **AUDITORÍA PENDIENTE**
* **Candidatos documentados de P2-02C:**
  1. `BoxWithConstraints` (Subcomposición en pase de layout).
  2. `ShortcutIcon` (Granularidad de nodos e interacción táctil).
  3. Composición inicial restante del árbol raíz.
* **Regla Estricta:** **NO asumir ninguno como próximo cambio definitivo.** Claude deberá realizar una auditoría específica antes de autorizar o modificar código.

---

### Workflow Oficial de Performance Engineering

```text
P0 Auditoría Estática [COMPLETADA]
       ↓
P1 Baseline Cuantitativa (:benchmark, TTID, TTFD, Frames) [COMPLETADA]
       ↓
P2-01 Optimización Search (DrawerListItems / AppDrawerScreen) [COMPLETADA]
       ↓
P2-02 Diagnóstico Integral Cold Start (A: loadState, B: Timeline Map, C: Composition Trace) [COMPLETADA]
       ↓
P2-03 EXP-PAGER-ZERO-PRELOAD (HorizontalPager beyondViewportPageCount = 0) [COMPLETADA]
       ↓
P2-04 Siguiente Hotspot de Composición / Layout [AUDITORÍA PENDIENTE]
       ↓
P2-05 saveState() (Mutaciones y persistencia no síncrona)
       ↓
P2-06 Compose Stability (Profiling de recomposiciones en LauncherState)
```

* **Regla Inquebrantable:**
  ```text
  MEDIR  →  LOCALIZAR  →  CAMBIAR (QUIRÚRGICO)  →  MEDIR OTRA VEZ
  ```
  *Exactamente un experimento por cambio. Prohibido acumular optimizaciones.*

---

## 7. Próxima Acción Oficial

* **NEXT:** `Performance Engineering — Fase P2-04: Auditoría Técnica del Siguiente Hotspot de Composición / Layout`.
* **Responsable:** Claude realizará una auditoría específica antes de autorizar cualquier modificación de código.
* **Restricción Estricta:** Cero modificaciones de código en producción durante la fase de auditoría.

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
* **Estado en Google Play Console:** Solicitud de restablecimiento de Upload Key enviada con el certificado público `upload_certificate-v2.pem` (ventana de procesamiento de Google: 24-48 horas). Requiere confirmación manual por parte del desarrollador en la consola web antes del envío formal del bundle.

---

## 10. Validaciones Automatizadas Consolidadas

* `git diff --check`: **PASS** (0 errores de formato, fin de línea o sintaxis en código de producción).
* `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (**259/259 pruebas unitarias aprobadas al 100%**: 236 suite base preexistente + 7 suite `LauncherLifecyclePolicyTest` + 6 suite `VideoResumeRecoveryPolicyTest` + 10 suite `IconCacheTest`).
* `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL** (APK generado en `app\build\outputs\apk\debug\app-debug.apk`, 87,041,306 bytes, SHA-256: `7A2DA6D25406D90AD90005AB90C6FBB5EA4AD8F624F47EFAE11D12560A760578`).
* `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errores, 81 advertencias baseline preexistentes).
* `.\gradlew.bat lintVitalRelease`: **BUILD SUCCESSFUL** (0 errores, 0 advertencias).
* `.\gradlew.bat bundleRelease`: **BUILD SUCCESSFUL** (Minificación R8, encogimiento de recursos y firma con `upload-keystore-v2.jks`).
  * **AAB Generado y Verificado:** `app\build\outputs\bundle\release\app-release.aab`
  * **Tamaño:** 64,491,244 bytes (~61.50 MB)
  * **SHA-256:** `233C2C7774220F305392C0AE826526E34972BD70FD01C5457FB74D2CE6BFBF8F`
  * **Firma Criptográfica Verificada:** `jarsigner -verify -verbose -certs` confirma firma válida con `CN=NovaLauncher Upload v2, OU=Mobile, O=NovaLauncher, C=MX` (RSA 4096-bit, SHA-256 digest).
  * **Manifiesto AAB Verificado:** `package="com.daybreak.animelauncher"`, `versionCode="9"`, `versionName="1.6.1"`, `minSdk="24"`, `targetSdk="36"`.
* Suite de Benchmarks en Hardware Físico (`POCO X6 5G`): **PASS** (`StartupBenchmark` + `DrawerFrameBenchmark`).

---

## 11. Tareas Pendientes y Hoja de Ruta

1. **HIDDEN-APPS-BACKUP-ISOLATION (Consolidada):** Batería de validación física en POCO X6 5G ejecutada exitosamente (P1, P2, P6, P10, P3, P4, P8, P9, C1, C2 aprobados; P5 D2D Xiaomi pendiente; P7 preexistente documentado).
2. **NOVA-RESUME-FADE-01 (Validación Física en Hardware POCO X6 5G):** Confirmación física en dispositivo real de reanudación limpia sin fade de 100 ms ni Scaffold negro visible, preservación de animación de desbloqueo e integridad fail-closed de Apps Ocultas. Estado: **FIX APPLIED — PENDING DEVICE CONFIRMATION**.
3. **NOVA-VIDEO-RESUME-02 (Validación Física en Hardware POCO X6 5G):** Confirmación física tras apertura/cierre de apps multimedia pesadas (TikTok, Cámara, YouTube). El código garantiza recuperación ante estados rotos/ENDED/error sin causar parpadeos negros espurios; la detección de pérdida de presentación en códecs hardware sin error de Media3 permanece sujeta a validación empírica en dispositivo real. Estado: **FIX APPLIED — PENDING DEVICE CONFIRMATION**.
4. **NOVA-APP-DRAWER-ICONS-01 (Validación Física en Hardware POCO X6 5G):** Confirmación en dispositivo físico tras apertura inicial del cajón y desplazamiento vertical rápido. Ausencia total del icono genérico blanco de Android, renderizado inmediato de iconos precalentados y placeholders sutiles no intrusivos durante cargas asíncronas. Estado: **FIX APPLIED — PENDING DEVICE CONFIRMATION**.
5. **Performance Engineering — Fase P2-04 (Composición / Layout):** Auditoría técnica y selección del próximo experimento A/B de optimización sobre los hotspots de composición restantes.
6. **Google Play Console — Restablecimiento de Upload Key:** Pendiente confirmación externa por parte del desarrollador en Google Play Console para validar que la clave `upload_certificate-v2.pem` ya se encuentre activa en el backend de Google (ventana 24-48h).
7. **Fase 5E-3 — Configuración Segura de Firma:** ✅ **COMPLETADA LOCALMENTE** (Configuración en Gradle vía `keystore.properties` desacoplado fuera de Git, validada exitosamente mediante `bundleRelease`).
8. **Accesibilidad y Ficha de Tienda:** Preparar video demostrativo de accesibilidad para Play Console y activos gráficos finales.
9. **Deuda Técnica — Rastreo de artefactos en `benchmark/build`:** Limpiar artefactos compilados rastreados previamente en Git en el directorio `benchmark/build/` mediante `git rm -r --cached benchmark/build/` en una tarea/commit dedicado e independiente, sin alterar la lógica funcional del launcher.

---

## 12. Registro de Versiones y Changelog

### Versión Candidata 1.6.1 (versionCode = 9) — 26 de octubre de 2026 (Release Preflight, Symbols & Privacy Sync)
* **Preparación y Empaquetado para Google Play (RELEASE PREFLIGHT 1.6.1):**
  - **Identidad y Versión:** Actualizados `versionCode = 9` y `versionName = "1.6.1"` en `app/build.gradle.kts` (`minSdk = 24`, `targetSdk = 36`).
  - **Generación de Android App Bundle:** AAB generado y compilado con éxito en `app\build\outputs\bundle\release\app-release.aab` (64,491,244 bytes, SHA-256: `233C2C7774220F305392C0AE826526E34972BD70FD01C5457FB74D2CE6BFBF8F`).
  - **Firma Criptográfica Verificada:** Firmado con la clave oficial `novalauncher-upload-v2` (`upload-keystore-v2.jks`, RSA 4096-bit). Verificación con `jarsigner` exitosa (`jar verified`).
  - **Configuración de Símbolos Nativos (NDK):** Incorporado `ndk { debugSymbolLevel = "FULL" }` en `buildTypes.release` de `app/build.gradle.kts`.
  - **Auditoría Técnica de Bibliotecas Nativas:**
    - Se identificaron e inspeccionaron las bibliotecas .so del AAB original: `libandroidx.graphics.path.so` en 4 arquitecturas (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`).
    - Verificación de correspondencia 100% de tamaño y SHA-256 entre el AAB original y la compilación actual.
    - Se constató que dicha biblioteca proviene de la dependencia de terceros `androidx.graphics:graphics-path` y se distribuye pre-stripped desde el repositorio oficial de Google AndroidX en Maven, emitiendo AGP `Unable to extract native debug metadata ... because the native debug metadata has already been stripped.` No se requiere ZIP nativo externo adicional.
  - **Preservación de Artefactos de Soporte:** AAB original, `mapping.txt` (SHA-256: `4ACCA1BF8103EFED7E7976DE8EBAE950EE1BAF59CC3F890540CD39CB6B27FF58`) e informe de verificación archivados fuera del repositorio en `C:\Users\migue\NovaLauncher-ReleaseArchive\v1.6.1-code9\`.
  - **Sincronización de Política de Privacidad:**
    - Documento maestro `docs/PRIVACY_POLICY.md` actualizado a versión 1.6.1 (26 de octubre de 2026), incorporando secciones explícitas de Cajón de Aplicaciones/IconCache, RAM Monitor (`ActivityManager.MemoryInfo`), Temas Adaptativos y transparencia del permiso INTERNET.
    - Repositorio web público `Mikies21-Programmer/NovaLauncher-Privacy` sincronizado con paridad 1:1 en `index.html`.
  - **Validaciones Automatizadas al 100%:**
    - `testDebugUnitTest`: 259/259 pruebas unitarias aprobadas (100% PASS).
    - `lintRelease`: Aprobado (0 errores, 81 advertencias en baseline).
    - `bundleRelease`: Compilación exitosa con R8 minification y resource shrinking.
  - **Correcciones Incluidas (Pendientes de Confirmación Física en Hardware Real):**
    - `NOVA-RESUME-FADE-01`: Eliminación de fade de 100 ms y scaffold negro en reanudación. (Estado: **FIX APPLIED — PENDING DEVICE CONFIRMATION**).
    - `NOVA-VIDEO-RESUME-02`: Recuperación determinista de fondo de video ante estados rotos/ENDED sin recargas espurias en estados listos. (Estado: **FIX APPLIED — PENDING DEVICE CONFIRMATION**).
    - `NOVA-APP-DRAWER-ICONS-01`: Precalentamiento asíncrono, coalescencia de decodificación y eliminación del fallback blanco genérico en el cajón de aplicaciones. (Estado: **FIX APPLIED — PENDING DEVICE CONFIRMATION**).
  - **Bloqueos Restantes Previos a Carga en Tienda:** Confirmación manual por parte del desarrollador en Google Play Console de la activación de la clave de subida v2 y verificación de que `versionCode = 9` no haya sido subido previamente.

### Versión 1.6 (versionCode = 8) — 25 de octubre de 2026
* **Carga y Estabilización de Iconos en App Drawer (NOVA-APP-DRAWER-ICONS-01) — FIX APPLIED — PENDING DEVICE CONFIRMATION:**
  - **Precalentamiento en Segundo Plano (`preloadIcons`):** Al detectar o refrescar la lista de aplicaciones instaladas en `LauncherViewModel.refreshInstalledApps()`, se lanza automáticamente la pre-carga secuencial/asíncrona de los iconos (orden A-Z visible al abrir el cajón) respetando estrictamente el presupuesto máximo de memoria (`MAX_ENTRIES = 128`, ≤4.6 MB).
  - **Coalescencia y Deduplicación en Vuelo (`inFlightJobs`):** Implementada concurrencia cooperativa con `ConcurrentHashMap<String, Deferred<ImageBitmap?>>` en `IconCache`. Múltiples solicitudes o recomposiciones concurrentes para el mismo paquete comparten la misma tarea de decodificación, erradicando IPCs duplicadas hacia `PackageManagerService`.
  - **Desacople de Cancelación (`cacheScope`):** La decodificación se ejecuta en un `SupervisorJob` desacoplado del ciclo de vida efímero de las filas de la `LazyColumn`. Al desplazarse a alta velocidad, la tarea no se cancela al salir el elemento de la pantalla, completándose e insertándose en el LRU para que al reingresar esté disponible inmediatamente.
  - **Eliminación del Fallback Genérico Blanco:** Modificado `ShortcutIcon` para distinguir formalmente entre estados `Loading` (placeholder sutil translúcido adaptado a la forma del icono sin parpadeos blancos), `Loaded` (renderizado inmediato de `ImageBitmap`) y `Failed` (fallback explícito `Icons.Outlined.Android` únicamente ante fallo comprobado de PackageManager).
  - **Invalidación Reactiva por Ciclo de Vida (`invalidate`):** `LauncherViewModel.packageReceiver` invoca `IconCache.invalidate(changedPackage)` ante `ACTION_PACKAGE_REMOVED`, `ACTION_PACKAGE_REPLACED` o `ACTION_PACKAGE_ADDED`, purgando entradas desactualizadas y evitando iconos obsoletos tras actualizar apps.
  - **Suite de Pruebas Automatizadas:** 10 pruebas unitarias nuevas en `IconCacheTest.kt` cubriendo caché fría/caliente, coalescencia concurrente, desacople de cancelación, fallos reales, invalidación y límite de memoria (259/259 tests unitarios aprobados al 100%).

* **Auditoría y Recuperación de Video al Reanudar (NOVA-VIDEO-RESUME-02) — FIX APPLIED — PENDING DEVICE CONFIRMATION:**
  - **Auditoría Forense de Ciclo de Vida de Video:** Análisis exhaustivo de los estados de ExoPlayer (`STATE_IDLE`, `STATE_BUFFERING`, `STATE_READY`, `STATE_ENDED`), ciclo de vida de `TextureView` y reclamación de códecs hardware en HyperOS.
  - **Extracción de Política Pura:** Creada `VideoResumeRecoveryPolicy.kt` con función pura `evaluateVideoResumeAction()`, garantizando re-preparación determinista únicamente ante estados objetivamente rotos (`STATE_IDLE`, `STATE_ENDED` o `playerError != null`) y protegiendo los estados válidos (`STATE_READY`, `STATE_BUFFERING`) contra reinicios espurios, bucles de reintento o parpadeos negros (*Zero Black Flash*).
  - **Integración Localizada en VideoWallpaperManager:** Reemplazadas las evaluaciones ad-hoc en `onResume()` y `assignSlotToPage()` por la política unificada `evaluateVideoResumeAction`.
  - **Suite de Pruebas Automatizadas:** 6 pruebas unitarias nuevas en `VideoResumeRecoveryPolicyTest.kt` cubriendo exhaustivamente todos los estados y combinaciones (249/249 tests unitarios aprobados al 100%).

* **Corrección de Resume Fade (NOVA-RESUME-FADE-01) — FIX APPLIED — PENDING DEVICE CONFIRMATION:**
  - **Eliminación del Fade en Reanudación:** Retirado el estado y la lógica transitoria de alfa (`resumeAlpha`, `hasBeenStopped`, `resumeAnimJobHolder`) y la rama `ON_RESUME` asociada en `LauncherScreen.kt`.
  - **Retiro de graphicsLayer Raíz:** Eliminado `.graphicsLayer { alpha = resumeAlpha.value }` del `Box` raíz en `LauncherScreen.kt`, conservando la estructura de modificadores y todos los componentes hijos (pager, drawer, overlays, diálogos).
  - **Preservación Fail-Closed de Apps Ocultas:** Encapsulada la regla de ciclo de vida en `LauncherLifecyclePolicy.kt` (`shouldRevokeHiddenAppsSession(event)`), garantizando revocación inmediata (`isHiddenAppsAuthenticated = false`, `hiddenAppsFlowState = Closed`) exclusivamente en `Lifecycle.Event.ON_STOP`.
  - **Suite de Pruebas Automatizadas:** 7 pruebas unitarias nuevas en `LauncherLifecyclePolicyTest.kt` evaluando exhaustivamente todos los eventos de `Lifecycle.Event` (243/243 tests unitarios aprobados al 100%).

### Versión 1.6 (versionCode = 8) — 27 de septiembre de 2026
* **Cierre y Hardening de Apps Ocultas (HIDDEN-APPS-FIX-01):**
  - **Eliminación del Bypass de Reseteo:** Corrección crítica en `HiddenAppsAuthDialog` y `HiddenAppsRepository.verifyAndReset()`. En dispositivos sin bloqueo seguro del sistema (`isDeviceSecure == false`), el restablecimiento exige obligatoriamente la credencial Nova activa (`OWN_PIN`, `OWN_PATTERN`, `OWN_PASSWORD`), bloqueando cualquier reseteo no autenticado.
  - **Protección Visual Anti-Espía (FLAG_SECURE):** Cobertura exhaustiva con `SecureFlagPolicy.SecureOn` y flag de ventana en `HiddenAppsSetupDialog`, `HiddenAppsAuthDialog` y `HiddenAppsScreen` durante la captura de secretos.
  - **Auto-Cierre por Inactividad:** Cierre fail-closed automático tras 60 segundos sin actividad en la pantalla protegida autenticada, reiniciado con cualquier interacción táctil (`PointerEventPass.Initial`).
  - **Gesto de Separación Adaptativo:** Sustitución de umbral rígido por distancia inicial escalada por densidad (`MIN_INITIAL_DISTANCE_DP = 24f`), con comprobación de punteros consumidos.
  - **Exclusión Mutua de Gestos:** Prevención de disparo consecutivo e involuntario entre Pinch-in (Settings) y Spread (Apps Ocultas).
  - **Sincronización de Idioma Interno:** Interfaz de Apps Ocultas reactiva al toggle de idioma `state.language` ("es" / "en") mediante `CompositionLocalProvider`.
  - **Validación Automatizada de Seguridad:** Incorporación de 7 pruebas obligatorias de seguridad en `HiddenAppsAuthTest` y 2 pruebas de robustez en `HiddenAppsGestureLogicTest`, alcanzando 107/107 tests unitarios aprobados.
