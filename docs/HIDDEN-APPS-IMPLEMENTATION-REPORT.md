# HIDDEN APPS IMPLEMENTATION REPORT

## 1. Estado
PASS

## 2. Archivos creados
1. `app/src/main/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsModels.kt`
2. `app/src/main/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsCrypto.kt`
3. `app/src/main/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsCredentialStore.kt`
4. `app/src/main/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsAuthenticator.kt`
5. `app/src/main/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsRepository.kt`
6. `app/src/main/java/com/daybreak/animelauncher/ui/screens/hiddenapps/HiddenAppsGesture.kt`
7. `app/src/main/java/com/daybreak/animelauncher/ui/screens/hiddenapps/PatternLockView.kt`
8. `app/src/main/java/com/daybreak/animelauncher/ui/screens/hiddenapps/HiddenAppsAuthDialog.kt`
9. `app/src/main/java/com/daybreak/animelauncher/ui/screens/hiddenapps/HiddenAppsSetupDialog.kt`
10. `app/src/main/java/com/daybreak/animelauncher/ui/screens/hiddenapps/HiddenAppsScreen.kt`
11. `app/src/test/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsGestureLogicTest.kt`
12. `app/src/test/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsAuthTest.kt`
13. `app/src/test/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsRepositoryTest.kt`
14. `app/src/test/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsDrawerIntegrationTest.kt`
15. `app/src/test/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsLifecycleTest.kt`
16. `docs/HIDDEN-APPS-IMPLEMENTATION.md`
17. `docs/HIDDEN-APPS-IMPLEMENTATION-REPORT.md`

## 3. Archivos modificados
1. `gradle/libs.versions.toml`: Declarada dependencia `androidx.biometric:biometric:1.1.0`.
2. `app/build.gradle.kts`: Agregada implementación de `libs.androidx.biometric`. `versionCode = 8` y `versionName = "1.6"` preservados intactos.
3. `app/src/main/AndroidManifest.xml`: Declarado permiso `android.permission.USE_BIOMETRIC`.
4. `app/src/main/java/com/daybreak/animelauncher/MainActivity.kt`: Herencia cambiada a `FragmentActivity` requerida por `androidx.biometric.BiometricPrompt`.
5. `app/src/main/res/xml/backup_rules.xml`: Excluido el archivo de SharedPreferences `nova_hidden_apps_vault.xml`.
6. `app/src/main/res/xml/data_extraction_rules.xml`: Excluido el archivo de SharedPreferences `nova_hidden_apps_vault.xml`.
7. `app/src/main/res/values/strings.xml`: Agregadas cadenas completas para configuración, autenticación y gestión en Español.
8. `app/src/main/res/values-en/strings.xml`: Agregadas cadenas completas para configuración, autenticación y gestión en Inglés.
9. `app/src/main/java/com/daybreak/animelauncher/LauncherViewModel.kt`: Agregado `hiddenAppsConfig` a `LauncherState` y métodos reactivos del repositorio.
10. `app/src/main/java/com/daybreak/animelauncher/ui/screens/AppDrawerScreen.kt`: Filtrado de `hiddenPackages` antes de `prepareDrawerCategory(appsInCat)`.
11. `app/src/main/java/com/daybreak/animelauncher/ui/screens/LauncherScreen.kt`: Vinculación del detector de gesto spread outward y renderizado condicional de diálogos/pantalla protegida.
12. `docs/PRIVACY_POLICY.md`: Documentada la existencia de Hidden Apps, almacenamiento local y ausencia de telemetría.
13. `website/backend/templates/politicas_de_privacidad.html`: Actualizada la versión web de la política de privacidad.

## 4. Archivos protegidos verificados sin cambios
Los siguientes componentes críticos y protegidos fueron expresamente auditados y NO han sufrido ninguna modificación:
- `VideoWallpaperManager.kt`: SIN CAMBIOS.
- `VideoBackground.kt`: SIN CAMBIOS.
- `WidgetHostManager.kt`: SIN CAMBIOS.
- `NativeWidgetView.kt`: SIN CAMBIOS.
- `ViewOne.kt`: SIN CAMBIOS.
- `ViewTwo.kt`: SIN CAMBIOS.
- `NotificationMonitorService.kt`: SIN CAMBIOS.
- `AccessibilityDisclosureDialog.kt`: SIN CAMBIOS.
- `ShortcutIcon.kt`: SIN CAMBIOS.
- `DrawerListItems.kt`: SIN CAMBIOS.
- `IconCache.kt` (y utilidades de iconos): SIN CAMBIOS.
- `LauncherNavState.kt`: SIN CAMBIOS (no se agregaron estados globales de navegación para Hidden Apps).
- `CRASH-FIX-01` en `AppDrawerScreen.kt`: 100% PRESERVADO.

*Nota:* Los cambios no committeados previos en `LauncherAccessibilityService.kt` y `accessibility_service_config.xml` correspondientes a compliance fueron estrictamente preservados según las instrucciones.

## 5. Gesture
- **Detector:** `Modifier.hiddenAppsSpreadGesture(...)` implementado en `HiddenAppsGesture.kt`.
- **Condiciones de Activación:**
  - Evaluado exclusivamente en páginas Home (`currentNavState == LauncherNavState.Home`).
  - Desactivado durante edición de widgets (`currentWidgetEditMode != null`), overlays (`pendingThemeProposalUri != null`), desbloqueos pendientes (`isUnlockPending`) o cuando los punteros tocan rectángulos de widgets excluidos.
- **Zona Central:** El punto medio geométrico entre ambos dedos al iniciar el contacto debe estar dentro de la zona central de la pantalla:
  - `CENTER_ZONE_FACTOR = 0.35f` (35% central relativo al ancho y alto).
- **Distancia y Zoom:**
  - Distancia mínima inicial escalada por densidad: `MIN_INITIAL_DISTANCE_DP = 24f` (convertida con `getMinInitialDistancePx(density)`).
  - Umbral de activación: `SPREAD_TRIGGER_ZOOM = 1.45f` (separación actual >= 1.45x la distancia inicial).
- **Control de Punteros y Exclusión Mutua:**
  - Tracking estricto por `PointerId` (exactamente 2 punteros).
  - Cancelación inmediata si se levanta un dedo, si ingresa un tercer dedo o si el usuario realiza un gesto inverso hacia adentro (`ratio < 0.85f`).
  - Descarte si los punteros ya fueron consumidos por otro detector.
  - Inhabilitación del Pinch-in de Settings en `LauncherScreen.kt` mientras Apps Ocultas esté activo (`hiddenAppsFlowState == Closed`).
  - One-shot trigger con consumo de eventos (`change.consume()`) para evitar colisiones con otros gestos.

## 6. Authentication
- **Modos Admitidos (`HiddenAppsAuthMode`):**
  - `NONE`: Sin configurar (no permite acceso directo).
  - `BIOMETRIC`: Biometría fuerte del sistema (`BIOMETRIC_STRONG`).
  - `DEVICE_CREDENTIAL`: Bloqueo del sistema Android (PIN, patrón o contraseña del dispositivo).
  - `OWN_PIN`: PIN propio de 6 a 12 dígitos.
  - `OWN_PATTERN`: Patrón propio en cuadrícula de 3x3 (mínimo 4 puntos).
  - `OWN_PASSWORD`: Contraseña alfanumérica de 8 a 64 caracteres.
- **Regla de Método Único:** Solo un método activo a la vez. El cambio de método requiere autenticación previa con el método actual antes de sobreescribir.
- **Cooldown:** Tras 5 intentos fallidos consecutivos con credenciales propias, se bloquea el acceso durante 30 segundos con cuenta regresiva.
- **Restablecimiento Protegido (Cierre de Bypass):** El reseteo destructivo exige validación formal de identidad:
  - En dispositivos con bloqueo de pantalla (`isDeviceSecure == true`), se permite confirmar con la credencial del sistema o la credencial de Nova.
  - En dispositivos sin bloqueo (`isDeviceSecure == false`), está prohibido el reseteo automático; se exige obligatoriamente ingresar la credencial Nova activa (`OWN_PIN`, `OWN_PATTERN` o `OWN_PASSWORD`) verificada vía `HiddenAppsRepository.verifyAndReset()`.
  - Reseteos cancelados o no autenticados no alteran ni desocultan ningún paquete.

## 7. Cryptography
- **Derivación de Clave:** PBKDF2WithHmacSHA256 con salt aleatorio seguro de 16 bytes (`SecureRandom`) y 120,000 iteraciones.
- **Comparación de Hashes:** Realizada en tiempo constante mediante `MessageDigest.isEqual(...)` para mitigar ataques de timing.
- **Cifrado de Bóveda:** Clave simétrica de 256 bits generada en hardware mediante `AndroidKeyStore` con transformación `AES/GCM/NoPadding` (vector de inicialización aleatorio de 12 bytes y tag de autenticación de 128 bits).
- **Protección de Memoria:** Arrays de caracteres se limpian sobreescribiéndolos con ceros (`HiddenAppsCrypto.wipe`) inmediatamente después de su uso criptográfico. Nunca se registran secretos en logs ni se guardan en texto claro.

## 8. Persistence
- **Configuración Funcional:** `HiddenAppsConfig(isConfigured, authMode, hiddenPackages)` se serializa como parte del objeto inmutable `LauncherState` en `launcher_prefs.xml`. No contiene hashes, sales ni credenciales.
- **Bóveda Criptográfica:** El registro cifrado (`ciphertext` e `IV` codificados en Base64) se almacena en el archivo SharedPreferences privado `nova_hidden_apps_vault.xml`.
- **Estado de Sesión:** `isAuthenticated` es estrictamente volátil en memoria y nunca se almacena en disco ni en `rememberSaveable` (decisión fail-closed documentada en código).

## 9. Drawer integration
- **Punto de Inserción:** En `AppDrawerScreen.kt`, el filtro `appsInCat.filter { it.packageName !in hiddenPackages }` se ejecuta inmediatamente antes de la llamada a `prepareDrawerCategory(appsInCat)`.
- **Consecuencias Inmediatas:**
  - Las aplicaciones ocultas quedan fuera de `PreparedDrawerSection.apps`.
  - No aparecen en el App Drawer principal.
  - No aparecen en la búsqueda (al alimentarse del resultado filtrado).
  - No aparecen en el rail A-Z ni generan secciones vacías innecesarias.
  - Los accesos directos ya fijados en `ViewOne` o `ViewTwo` no se eliminan.
  - El propio paquete de NovaLauncher (`com.daybreak.animelauncher`) no puede ser ocultado.

## 10. Lifecycle & Hardening
- **Invalidación Inmediata de Sesión:** Al pulsar Atrás (Back), botón Home, cambiar de aplicación, cerrar el modal o producirse `ON_STOP` en la Activity, el estado en memoria `isAuthenticated` se restablece a `false`.
- **Auto-Cierre por Inactividad (60 segundos):** En `HiddenAppsScreen`, la sesión se cierra automáticamente si no se detecta interacción durante 60 segundos. Todo toque en la pantalla (`PointerEventPass.Initial`) reinicia el temporizador.
- **Protección Visual (FLAG_SECURE):** Aplicado en `HiddenAppsSetupDialog` y `HiddenAppsAuthDialog` con `SecureFlagPolicy.SecureOn` y en la ventana de la Activity vía `DisposableEffect`, extendiéndose a `HiddenAppsScreen`, bloqueando capturas, grabaciones y previsualizaciones de multitarea durante todo el ciclo sensible.
- **Sincronización de Idioma Interno:** Componentes envueltos en `CompositionLocalProvider(LocalContext, LocalConfiguration)` configurado con `Locale.forLanguageTag(state.language)`, respondiendo instantáneamente al switch de idioma interno ("es" / "en").

## 11. Backup
- Los archivos `backup_rules.xml` y `data_extraction_rules.xml` excluyen formalmente:
  `<exclude domain="sharedpref" path="nova_hidden_apps_vault.xml" />`
  Esto evita restauraciones inconsistentes de datos cifrados que dependen de claves de hardware locales de Android Keystore.
- Si se restaura un estado con `isConfigured == true` pero sin vault, el sistema adopta una postura fail-closed (no permite accesos no autorizados ni desoculta paquetes).

## 12. Privacy / Play
- `docs/PRIVACY_POLICY.md` y `website/backend/templates/politicas_de_privacidad.html` actualizados.
- Se documenta con exactitud:
  - Todo el procesamiento es 100% local.
  - Nombres de paquete y estado de ocultación se guardan localmente en `LauncherState`.
  - Las credenciales propias nunca se almacenan en texto plano (PBKDF2 + AES-GCM Keystore).
  - Credenciales, hashes y material criptográfico nunca se transmiten.
  - Cero servidores externos, bases de datos o telemetría añadida.
  - No se declara recolección de datos no realizada por el código.

## 13. Unit tests
- **Resultado:** 107 tests ejecutados, 107 tests exitosos, 0 fallos, 0 omitidos.
  - Tests preexistentes: 62/62 aprobados.
  - Tests de Hidden Apps: 45/45 aprobados (incluyendo 7 tests de seguridad obligatorios de reset y vault, y tests de escalado por densidad y exclusión de punteros).
  - Comando: `./gradlew testDebugUnitTest`

## 14. Build
- **Resultado:** BUILD SUCCESSFUL.
  - Comando: `./gradlew assembleDebug`

## 15. Lint
- **lintDebug:** BUILD SUCCESSFUL (0 errores, 72 advertencias no bloqueantes).
- **lintVitalRelease:** BUILD SUCCESSFUL (0 errores, 0 advertencias).
  - Comando: `./gradlew lintVitalRelease`

## 16. Release AAB
- **Ruta exacta del artefacto generado:**
  `C:\Users\migue\AndroidStudioProjects\AnimeLauncher\app\build\outputs\bundle\release\app-release.aab`
- **Tamaño:** ~61.3 MB.
- **Firma:** Firmado con la configuración de release del proyecto.

## 17. Git status
Salida de `git status --short`:
```
 M app/build.gradle.kts
 M app/src/main/AndroidManifest.xml
 M app/src/main/java/com/daybreak/animelauncher/LauncherAccessibilityService.kt
 M app/src/main/java/com/daybreak/animelauncher/LauncherViewModel.kt
 M app/src/main/java/com/daybreak/animelauncher/MainActivity.kt
 M app/src/main/java/com/daybreak/animelauncher/ui/screens/AppDrawerScreen.kt
 M app/src/main/java/com/daybreak/animelauncher/ui/screens/LauncherScreen.kt
 M app/src/main/res/values-en/strings.xml
 M app/src/main/res/values/strings.xml
 M app/src/main/res/xml/accessibility_service_config.xml
 M app/src/main/res/xml/backup_rules.xml
 M app/src/main/res/xml/data_extraction_rules.xml
 M docs/PRIVACY_POLICY.md
 M gradle/libs.versions.toml
 M website/backend/templates/politicas_de_privacidad.html
?? COMPLIANCE-FIX-01-REPORT.md
?? app/src/main/java/com/daybreak/animelauncher/hiddenapps/
?? app/src/main/java/com/daybreak/animelauncher/ui/screens/hiddenapps/
?? app/src/test/java/com/daybreak/animelauncher/hiddenapps/
?? docs/HIDDEN-APPS-IMPLEMENTATION-REPORT.md
?? docs/HIDDEN-APPS-IMPLEMENTATION.md
```
- No se han realizado commits (`git commit` no ejecutado).
- No se ha realizado push (`git push` no ejecutado).
- Los cambios previos sin commit de accesibilidad y cumplimiento fueron preservados intactos.

## 18. Riesgos/pendientes
*(Pendientes exclusivamente para la posterior validación física en hardware real por parte del desarrollador)*
1. **Sensibilidad Táctil en POCO X6 5G:** Confirmar en mano la naturalidad y ergonomía del umbral de zoom de 1.45x y la zona central del 35% durante el gesto de separación de dos dedos.
2. **Desbloqueo Biométrico en Pantalla:** Validar la visualización fluida del prompt biométrico sobre la pantalla AMOLED a 120Hz del dispositivo físico.
3. **Comportamiento con Teclado de Sistema:** Comprobar visualmente que el teclado virtual no solape los campos de texto al introducir contraseñas largas en modo pantalla completa.
