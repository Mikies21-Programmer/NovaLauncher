# HIDDEN-APPS-ZAI-AUDIT-01

> Auditoría técnica forense del feature Hidden Apps y del congelamiento de NovaLauncher al convertirse en launcher predeterminado.
> Fecha: 2026-10-02 · Workspace: `C:\Users\migue\AndroidStudioProjects\AnimeLauncher` (estado idéntico al 01-oct: cambios sin commit, último commit `eb09b71` "release: prepare NovaLauncher 1.5").
> Evidencia de dispositivo: POCO X6 5G (23122PCD1G), Android 16 / SDK 36, HyperOS OS3.0.302.0.WNREUXM, conectado por ADB.
> Etiquetas usadas: **[CONFIRMADO EN CÓDIGO]** · **[CONFIRMADO EN DISPOSITIVO]** · **[HIPÓTESIS]** · **[NO HAY EVIDENCIA SUFICIENTE]** · **[REQUIERE VERIFICACIÓN EN PLAY CONSOLE]** · **[REQUIERE REVISIÓN LEGAL/POLÍTICA]**

---

# 1. ESTADO ACTUAL

## 1.1 Implementado y completo [CONFIRMADO EN CÓDIGO]

Todo el feature Hidden Apps está escrito y sin commitear. Inventario real del workspace:

**Nuevos (sin trackear):**
- `app/src/main/java/com/daybreak/animelauncher/hiddenapps/` — `HiddenAppsModels.kt`, `HiddenAppsCrypto.kt`, `HiddenAppsCredentialStore.kt`, `HiddenAppsAuthenticator.kt`, `HiddenAppsRepository.kt`.
- `app/src/main/java/com/daybreak/animelauncher/ui/screens/hiddenapps/` — `HiddenAppsGesture.kt`, `PatternLockView.kt`, `HiddenAppsAuthDialog.kt`, `HiddenAppsSetupDialog.kt`, `HiddenAppsScreen.kt`.
- `app/src/test/java/com/daybreak/animelauncher/hiddenapps/` — 5 archivos de tests (45 tests de Hidden Apps, 107 totales según el informe del equipo).
- `docs/HIDDEN-APPS-IMPLEMENTATION.md`, `docs/HIDDEN-APPS-IMPLEMENTATION-REPORT.md`, `COMPLIANCE-FIX-01-REPORT.md`.

**Modificados (sin commit):** `gradle/libs.versions.toml` (+`androidx.biometric:1.1.0`), `app/build.gradle.kts` (dependencia biometric, versionCode 8 / versionName 1.6), `AndroidManifest.xml` (+`USE_BIOMETRIC`), `MainActivity.kt` (**ComponentActivity → FragmentActivity**), `LauncherViewModel.kt` (`hiddenAppsConfig` en `LauncherState`, repositorio, prune), `LauncherScreen.kt` (gesto spread + overlays + fail-closed), `AppDrawerScreen.kt` (filtrado hidden), `strings.xml` (es/en), `backup_rules.xml`, `data_extraction_rules.xml`, `LauncherAccessibilityService.kt` + `accessibility_service_config.xml` (compliance previo), docs de privacidad.

## 1.2 Incompleto o roto

1. **Freeze sin causa raíz confirmada** — el bloqueador absoluto (sección 8).
2. **[CONFIRMADO EN CÓDIGO] Crash en API 24-25:** `HiddenAppsCrypto.deriveHash` usa `PBKDF2WithHmacSHA256` (disponible desde API 26). Con `minSdk 24`, en Android 7.x `SecretKeyFactory.getInstance` lanza `NoSuchAlgorithmException` **sin capturar** → crash al configurar o verificar PIN/patrón/contraseña propias.
3. **[CONFIRMADO EN CÓDIGO] KDF en el hilo principal:** `HiddenAppsRepository.saveOwnCredential` (suspend) llama `HiddenAppsCrypto.deriveHash` **sin cambiar de dispatcher**; se invoca desde `rememberCoroutineScope()` (Main) en `HiddenAppsSetupDialog` → PBKDF2 de 120.000 iteraciones (~0,3–0,8 s) bloquea el Main en el setup. La verificación (`verifyOwnCredential`) SÍ usa `Dispatchers.Default` — asimetría confirmada.
4. **[CONFIRMADO EN CÓDIGO] Cooldown anti-brute-force solo en memoria** (`HiddenAppsAuthenticator`): reiniciar el proceso resetea intentos y cooldown.
5. **[CONFIRMADO EN CÓDIGO] Sin puerta de escape documentada:** modo `OWN_*` + vault perdido (restore de backup / Keystore invalidada) → `loadCredential()` null → verificación imposible siempre → apps ocultas irrecuperables salvo borrar datos.
6. **[CONFIRMADO EN CÓDIGO] Churn del detector de gesto:** `hiddenAppsSpreadGesture` usa `widgetBoundsList` como key de `pointerInput` → cancelación/relanzamiento de la corrutina en cada cambio real de bounds (`ViewOne.onGloballyPositioned` re-emite listas nuevas).
7. **[CONFIRMADO EN CÓDIGO] Recomposición por evento táctil:** `HiddenAppsScreen` escribe `lastInteractionTime` (estado observable) en cada evento de puntero (temporizador de 60 s) → recomposición por frame durante scroll.

## 1.3 Verificado NO usado / sin entradas alternativas

- **[CONFIRMADO EN CÓDIGO]** El único punto de entrada a Hidden Apps es el gesto spread en Home (grep repo-wide: cero referencias en `SettingsScreen`, widgets o navegación).
- **[CONFIRMADO EN CÓDIGO]** `RoleManager.ROLE_HOME` existe únicamente en `OnboardingScreen` (DefaultLauncherStep). No hay otros flujos de "establecer como predeterminado" dentro de la app.
- **[CONFIRMADO EN CÓDIGO]** No hay llamantes colgantes de `LauncherAccessibilityService.goBack/openRecents/goHome` (eliminados por COMPLIANCE-FIX-01).
- **[CONFIRMADO EN CÓDIGO]** Ningún `runBlocking`, `Thread.sleep`, `CountDownLatch`, ni `Mutex`/`synchronized` bloqueante en el paquete hiddenapps o en los flujos nuevos.

---

# 2. FLUJO HIDDEN APPS ACTUAL

[CONFIRMADO EN CÓDIGO] Flujo real:

```
Home (cualquier página, modificador a nivel HorizontalPager)
  → hiddenAppsSpreadGesture (2 dedos, zona central 35%, ratio ≥ 1.45x, one-shot, consume al disparar)
  → hiddenAppsFlowState = Setup (si !isConfigured) | Authenticating (si isConfigured)
      Setup: HiddenAppsSetupDialog → método (BIOMETRIC / DEVICE_CREDENTIAL / OWN_PIN / OWN_PATTERN / OWN_PASSWORD)
             → credenciales propias: input + confirmación → saveOwnCredential (KDF + vault Keystore) → setAuthMode
      Authenticating: HiddenAppsAuthDialog
             → BIOMETRIC: BiometricPrompt (BIOMETRIC_STRONG) auto-lanzado por LaunchedEffect(authMode)
             → DEVICE_CREDENTIAL: BiometricPrompt (API≥30) | KeyguardManager.createConfirmDeviceCredentialIntent (API 24-29)
             → OWN_*: verificación en Dispatchers.Default + cooldown 5 fallos/30 s
  → Visible: HiddenAppsScreen (FLAG_SECURE, pestañas Ocultas/Todas, búsqueda, timeout 60 s, reset protegido)
  → onClose/Back/HOME/ON_STOP → revocación de sesión → Closed (fail-closed, sin rememberSaveable)
```

Persistencia de estado: `HiddenAppsConfig(isConfigured, authMode, hiddenPackages)` vive dentro de `LauncherState` (Gson en `launcher_prefs`); credenciales cifradas en `nova_hidden_apps_vault` (Keystore AES-GCM-256, excluido de backups). Estado de sesión: exclusivamente en memoria.

Los overlays se renderizan dentro de un `CompositionLocalProvider(LocalContext/LocalConfiguration)` con el locale interno ("es"/"en").

---

# 3. GESTO DE DOS DEDOS

**Arquitectura encontrada [CONFIRMADO EN CÓDIGO]:** el detector (`HiddenAppsGesture.kt`) es el tercer `pointerInput` del `HorizontalPager` (cadena: swipe vertical → pinch-in → spread), todos en `PointerEventPass.Initial`, sobre el nodo raíz del pager — por eso funciona desde cualquier página Home y no vive dentro de `ViewOne`/`ViewTwo`.

- Requiere exactamente 2 punteros presionados; punto medio inicial en el 35% central; distancia inicial ≥ 24dp escalado por densidad; exclusión de rects físicos de widgets; condiciones `isHome && !editMode && !themeProposal && !unlockPending && flowState==Closed`.
- Disparo one-shot a ratio ≥ 1.45 con consumo de ambos cambios; cancelación si ratio < 0.85 (es pinch), si se levanta un dedo, entra un tercero, o los eventos llegan ya consumidos.

**Conflictos evaluados:**

| Conflicto | Resultado |
| :--- | :--- |
| vs pinch-in Settings (zoom < 0.75) | Sin conflicto: el pinch ve `Initial` antes, consume solo en su umbral; el spread cancela si ve consumo. Direcciones mutuamente excluyentes (0.85 / 1.45). [CONFIRMADO] |
| vs swipe vertical (drawer/notificaciones, 1 dedo) | Sin conflicto: el detector vertical se auto-cancela con ≥ 2 dedos en inicio. [CONFIRMADO] |
| vs HorizontalPager | Sin conflicto: el spread no consume salvo en el disparo. [CONFIRMADO] |
| vs widgets | Excluido por `widgetBoundsList` al inicio del gesto. [CONFIRMADO] |
| vs Drawer/Settings/diálogos | No se dispara: `isHome=false` en `InDrawer`; los diálogs Compose son ventanas separadas. [CONFIRMADO] |
| Doble disparo / estado pegado | One-shot con `hasTriggered`; tras disparar, `isHome` pasa a false y el `pointerInput` se reinicia limpio. [CONFIRMADO] |

**Defecto real [CONFIRMADO EN CÓDIGO]:** `widgetBoundsList` (lista nueva en cada pase de layout de widget, `ViewOne.kt:159-162`) actúa como **key de `pointerInput`** → churn de cancelación/relanzamiento de la corrutina ante cualquier cambio de bounds (settle de página, re-layout de widget). Riesgo: jank, no bloqueo. Arquitectura más segura: keys de primitivas + lectura de bounds por `rememberUpdatedState` dentro del loop (como ya hace el pinch con `Unit` como key).

---

# 4. AUTENTICACIÓN

## 4.1 Qué existe [CONFIRMADO EN CÓDIGO]

| Modo | Implementación | Almacenamiento |
| :--- | :--- | :--- |
| `BIOMETRIC` | `BiometricPrompt` androidx, `BIOMETRIC_STRONG`, executor Main, auto-launch en `LaunchedEffect(authMode)` | Ninguno (sin vault) |
| `DEVICE_CREDENTIAL` | API ≥ 30: `BiometricPrompt` con `DEVICE_CREDENTIAL \| BIOMETRIC_STRONG`; API 24-29: `KeyguardManager.createConfirmDeviceCredentialIntent` + ActivityResult | Ninguno |
| `OWN_PIN` | 6-12 dígitos, teclado propio | PBKDF2-SHA256, salt 16B, 120k iter, hash dentro del payload cifrado AES-GCM-256 (Keystore) |
| `OWN_PATTERN` | 3x3, 4-9 puntos, serialización determinista "0,4,8,6" | Igual |
| `OWN_PASSWORD` | 8-64 caracteres | Igual |

Comparación en tiempo constante (`MessageDigest.isEqual`), `wipe()` de CharArray, sin logs de secretos (verificado por grep). Cooldown 30 s tras 5 fallos (en memoria). Reset protegido: BIOMETRIC/DEVICE_CREDENTIAL re-piden credencial del sistema; OWN_* exigen la credencial Nova verificada. `HiddenAppsScreen` exige sesión para el reset destructivo.

## 4.2 Device Credential vs credencial propia — consecuencias arquitectónicas

- **Device Credential (biometría/PIN del teléfono):** cero secretos gestionados, cero KDF, cero lockout propio; la seguridad la da el sistema. Requiere `FragmentActivity` para `androidx.biometric` (motivo del cambio de `MainActivity`). Riesgo de producto: si el usuario no tiene bloqueo, el modo no está disponible (ya se maneja con `isDeviceSecure`). Es la opción **más segura y mantenible**.
- **Credencial propia de Nova:** introduce ciclo completo de gestión de secretos (KDF, salt, vault, lockout, recovery, wipe). Es donde están **todos** los defectos confirmados hoy: crash API < 26, KDF en Main, lockout sin escape, cooldown no persistente. De mantenerse, necesita correcciones obligatorias (sección 12).

## 4.3 Qué falta / roto

1. [CONFIRMADO] Crash KDF en API 24-25 (§1.2-2).
2. [CONFIRMADO] KDF de setup en Main (§1.2-3).
3. [CONFIRMADO] Cooldown no persistente (§1.2-4).
4. [CONFIRMADO] Lockout sin escape si el vault se pierde (§1.2-5).
5. [CONFIRMADO EN DISPOSITIVO] `MainActivity` ahora depende de `androidx.fragment:1.3.6` + `appcompat:1.6.1` (transitivos de `biometric:1.1.0`, ausentes del classpath anterior — árbol de dependencias resuelto verificado). Impacto: ciclo de vida de la Activity HOME (sección 8).

---

# 5. APP DRAWER

**Punto de integración [CONFIRMADO EN CÓDIGO — correcto]:** el filtro se aplica **una sola vez**, en `AppDrawerScreen.kt:135-143`, antes de `prepareDrawerCategory`:

```
installedApps → (filtro hiddenPackages aquí) → prepareDrawerCategory (grouping/sorting/indexing)
             → drawerData → search → A-Z rail → UI
```

Como búsqueda, agrupación, índice y A-Z se alimentan de `preparedCategory`/`drawerData`, las ocultas desaparecen de **todas** las superficies con un único filtro — fuente de verdad única, sin duplicación. `HiddenAppsScreen` recibe `installedApps` sin filtrar (fuente estable reutilizada). Los shortcuts fijados en Home siguen lanzando apps ocultas (decisión de producto documentada; mantener explícita).

# 6. PERSISTENCIA

- **Modelo [CONFIRMADO EN CÓDIGO]:** `hiddenPackages: Set<String>` de packageNames dentro de `HiddenAppsConfig` en `LauncherState`; serialización Gson en `launcher_prefs → "launcher_state"` vía `updateState → saveState` (síncrono en hilo llamador). IDs: packageName puro (sin ComponentName) — tolerante a cambios de componente.
- **Limpieza [CONFIRMADO]:** `pruneUninstalledPackages` en cada refresh (`refreshInstalledApps`, IO) + receiver de `PACKAGE_ADDED/REMOVED/REPLACED`. Desinstalación → reaparece; paquetes inválidos → filtrados sin excepción; reinstall del launcher → misma versión no cambia estado; limpieza de datos → fail-closed.
- **Vault [CONFIRMADO]:** `nova_hidden_apps_vault.xml` excluido explícitamente de cloud-backup y device-transfer (reglas XML verificadas).
- **[CONFIRMADO EN DISPOSITIVO — riesgo real materializado]:** `launcher_prefs` (con la lista oculta en texto claro + `isConfigured`/`authMode`) **NO está excluido** del backup y **se restauró automáticamente** al reinstalar la app durante la reproducción de hoy: el launcher arrancó directo al Home con la configuración del usuario (sin onboarding). Si `isConfigured=true` se restaura sin vault, en modo `OWN_*` el acceso queda bloqueado para siempre (§4.3-4); en modo `BIOMETRIC`/`DEVICE_CREDENTIAL` la protección sigue operativa (no dependen del vault).

# 7. CRASH-FIX-01

**[CONFIRMADO EN CÓDIGO — preservado y reforzado]:** las guardas de `activeLetter` en `AppDrawerScreen` (`getOrNull(firstVisible.index)`, `getOrNull(targetIndex)`, fallbacks a `items.first()/last()`) siguen intactas; se añadió guarda anti-desincronización (`firstVisible.index > items.lastIndex` → `items.firstOrNull()`) y las keys del `remember(preparedCategory, searchQuery)`/`remember(..., hiddenPackages)` siguen siendo las correctas. El filtrado oculto se integra **antes** de construir índices, por lo que no puede reintroducir la inconsistencia índice/lista corregida: el tamaño de la lista solo cambia en el mismo `remember` que la reconstruye, y `listState` se resetea a 0 en cambio de categoría/búsqueda (efecto ya existente, aplicado igualmente).

# 8. INVESTIGACIÓN DEL FREEZE

## 8.1 Forense del dispositivo (01-02 oct 2026)

**[CONFIRMADO EN DISPOSITIVO]**
- `com.daybreak.animelauncher` **no está instalada**; el rol HOME está en `com.mi.android.globallauncher`.
- **Dropbox (40 entradas, retención completa del periodo):** cero `data_app_anr` y cero `data_app_crash` para Nova. Únicos ANR/crash: Gboard (11:09 01-oct) y servicios de Google — ajenos.
- **`/data/anr` (90 trazas, 30-sep 11:26 → 02-oct):** cero trazas con `Cmd line: com.daybreak.animelauncher` (temp o finales). El estallido de trazas del 01-oct 19:16–19:24 corresponde a Instagram/FIFA/ChatGPT/Telegram/Revolut/SD Maid (uso normal; el tracer vuelca varios procesos en cada evento).
- **Bugreport completo:** `vers=8` (versionCode 8 = build Hidden Apps) instalado y desinstalado dentro del bucket daily 27→28-sep; el re-instalado del 01-oct con el mismo versionCode no genera transición registrada (silenciosa). Sin `SYSTEM_BOOT` entre 14:01 y la noche del 01-oct (no hubo reinicio durante el incidente).
- **Reproducción controlada de hoy (misma APK: artefacto de 27-sep 14:48, binario idéntico al del freeze):**
  1. `adb install -r` → Success.
  2. `cmd package set-home-activity com.daybreak.animelauncher/.MainActivity` → **Success** (concesión de rol por shell; el sistema NO relanza el intent HOME automáticamente con otro launcher en foreground; el proceso de Nova no arranca solo).
  3. `KEYCODE_HOME` → arranque como HOME: `Displayed ... +1s951ms`, proceso vivo (pid 24262), foco en `MainActivity`, y **captura de pantalla del launcher completamente renderizado e interactivo** (reloj, widgets, sidebar, contador de mensajes).
  4. Sesión completa (rol + arranque + operación): **1.171.238 líneas de logcat, 73.433 del paquete Nova, CERO ANR, CERO FATAL, CERO "Input dispatching timed out"**.
  5. Tras `pm clear` + flujo de onboarding (diálogo de riesgo MIUI incluido), el launcher siguió sano como HOME.
  6. Restauración: rol devuelto al launcher MIUI (`Success`), app desinstalada (`Success`). Teléfono dejado en estado original.
- **No automatizable:** el gesto spread de 2 dedos (HyperOS deniega la inyección directa por SELinux: `sendevent: Permission denied`). El tramo **gesto → Setup/Auth (BiometricPrompt) → cambio de default** no pudo ejecutarse sin manos humanas y sin tocar código.

## 8.2 Lectura forense del incidente

**[CONFIRMADO EN DISPOSITIVO]** El freeze **nunca produjo un ANR a nivel de app** (ni traza temporal ni dropbox). Un bloqueo del hilo principal > 5 s con ventana enfocada SIEMPRE genera "Input dispatching timed out" + traza en `/data/anr` + entrada en dropbox. La ausencia total restringe el espacio de causas a: (a) el proceso murió/fue eliminado antes de completar el ANR; (b) el "freeze" no fue bloqueo de hilo sino **visual/ventana** (superficie negra/pegada — FLAG_SECURE es candidato natural: `SecureFlagPolicy.SecureOn` + `FLAG_SECURE` sobre la ventana de la Activity HOME durante Setup/Auth); (c) el proceso fue congelado por el freezer de MIUI sin ruta de ANR; (d) el bloqueo ocurrió sin ventana enfocada (durante la transición del diálogo de rol).

## 8.3 Causas candidatas

### Causa A — Cambio estructural de la Activity HOME (FragmentActivity + librerías legacy transitivas)
- Archivo: `MainActivity.kt:31`; `libs.versions.toml` (`biometric=1.1.0`); `app/build.gradle.kts:99`.
- Evidencia: árbol resuelto `biometric:1.1.0 → appcompat:1.6.1 → fragment:1.3.6` (+emoji2/Startup), librerías **ausentes del classpath anterior**.
- Mecanismo [HIPÓTESIS]: maquinaria de FragmentManager en cada evento de ciclo de vida de la Activity HOME (`onNewIntent` HOME, `onActivityResult` del flujo de rol, recreaciones), sobre fragment 1.3.6 (2021) no probado con targetSdk 36 + HyperOS. Hoy se reprodujo el rol+arranque **sin** freeze, por lo que esta causa, si aplica, requiere las variables interactivas no automatizables.
- Severidad: crítica (path crítico HOME).

### Causa B — BiometricPrompt activo durante la transición de rol/default
- Archivo: `HiddenAppsAuthDialog.kt:132-219` (auto-launch), `HiddenAppsSetupDialog.kt` (onConfigured → Authenticating).
- Mecanismo [HIPÓTESIS]: el prompt biométrico es un Fragment dentro de `FragmentActivity`; una concesión/despacho HOME mientras la sesión biométrica está viva (usuario cancela, cambia de app, o el sistema relanza HOME) cruza `FragmentManager` + `BiometricFragment` 1.1.0 + la transición de rol. `onAuthenticationError` → `onDismiss` → `Closed`, pero si el callback nunca regresa (bugs conocidos de biometric 1.1.0 en ROMs nuevas), queda un flujo "Authenticating" huérfano con la ventana en estado intermedio.
- Severidad: alta.

### Causa C — Superficie segura negra ("freeze visual") durante el cambio de default con diálogos abiertos
- Archivo: `HiddenAppsSetupDialog.kt:85-90`, `HiddenAppsAuthDialog.kt:93-98`, `HiddenAppsScreen.kt:70-76` (`FLAG_SECURE` sobre la ventana de la Activity HOME).
- Mecanismo [HIPÓTESIS]: con `FLAG_SECURE` activo en la ventana HOME, las capturas/miniaturas/superficies del recorte de sistema (recents, transición de rol, capturas de MIUI) renderizan **negro**. Un usuario que abre el selector de launcher con el diálogo de Hidden Apps abierto ve el Home negro/pegado al volver — percepción de "congelado" — **sin que exista bloqueo de hilo ni ANR**, lo cual es consistente con la evidencia forense (8.2).
- Severidad: alta (percepción + posible render real pegado en HyperOS).

### Causa D — KDF de setup en el hilo principal
- Archivo: `HiddenAppsRepository.kt:79-98` (`saveOwnCredential` sin dispatcher) llamado desde `HiddenAppsSetupDialog` (Main).
- [CONFIRMADO EN CÓDIGO] el bloqueo (~0,3–0,8 s) ocurre al confirmar PIN/patrón/contraseña. Magnitud insuficiente para un freeze de 5 s por sí sola; puede contribuir si coincide con la transición de rol.
- Severidad: media.

### Causa E — Arranque HOME pesado preexistente
- `loadState()` en Main (prefs+Gson+IPC de widgets, `LauncherViewModel.kt:252-323`) + primera composición de `LauncherScreen` (pager+video+widgets).
- [CONFIRMADO EN DISPOSITIVO] pesado pero funcional: `Displayed +1s951ms` (frío) / `+1s695ms` (tibio) **sin freeze** en la reproducción de hoy. Descartado como causa única del freeze.
- Severidad: observación.

### Descartados con evidencia
- Lógica Hidden Apps ejecutándose al arrancar (flow `Closed`, vault lazy, sin Keystore en startup) — [CONFIRMADO EN CÓDIGO].
- Bucles A→B→A (auto-open de Settings/diálogos, relanzamiento de HOME intents, recomposición infinita del overlay cerrado) — [CONFIRMADO EN CÓDIGO]: ninguna ruta automática; el disclosure de accesibilidad es solo por tap; `openNotificationsWithFallback` ya no abre Settings.
- runBlocking/sleep/mutex bloqueante — [CONFIRMADO EN CÓDIGO]: inexistente.
- Recomposición infinita por `pageWidgetBoundsMap` — lecturas con igualdad de Rect y keys estables del resto de la cadena; solo el churn del spread (sección 3) es real y no bloquea — [CONFIRMADO EN CÓDIGO].

## 8.4 Cómo podría quedar atrapado el launcher

1. Freeze/ANR real del proceso HOME → toda pulsación Home aterriza en el launcher colgado + `excludeFromRecents` elimina la vía de escape por Recents → "teléfono atrapado" (relato del 01-oct). Recuperable por: cambiador del sistema vía sombra/notificaciones (lo que finalmente ocurrió), adb, o desinstalar (lo que ocurrió según el historial).
2. Superficie negra por FLAG_SECURE durante cambio de default → el usuario percibe launcher muerto, re-intenta el cambio de launcher sin éxito aparente → bucle de percepción (Causa C).
3. Sesión "Authenticating" huérfana + rol cambiando → diálogos/ventanas en estado intermedio que interfieren con el selector del sistema (Causa B).

## 8.5 Evidencia que falta y cómo obtenerla [NO HAY EVIDENCIA SUFICIENTE para cerrar]

Reproducción humana-instrumentada (30 min, teléfono conectado): `adb logcat -b main,system,events,crash -v time` corriendo mientras el desarrollador ejecuta a mano: gesto spread → Setup → credencial → (mantener diálogos) → cambiar default desde Settings del sistema y volver. Cualquier ANR quedará trazado; si la pantalla se pone negra sin ANR, la Causa C queda confirmada por contraste. Complementos: `adb shell kill -3 <pid>` si se congela (dump de hilos; el build debug es debuggable y vuelca a logcat), y screencap cada 2 s para distinguir "negro por FLAG_SECURE" de "frame pegado".

---

# 9. RIESGOS DE SEGURIDAD

1. **[CONFIRMADO EN CÓDIGO + DISPOSITIVO]** Lista de apps ocultas en texto claro dentro de `launcher_state`, incluida en cloud backup y **restaurada automáticamente en un dispositivo/reinstalación** (observado hoy). Expone qué apps están ocultas y si hay protección.
2. **[CONFIRMADO EN CÓDIGO]** Cooldown/intentos solo en memoria → brute force re-iniciable reiniciando el proceso.
3. **[CONFIRMADO EN CÓDIGO]** Lockout definitivo en modo `OWN_*` si el vault se pierde con restore (isConfigured=true sin vault).
4. **[CONFIRMADO EN CÓDIGO]** Secretos en `String` de Compose (`pinValue`, `passwordValue`, `confirmInput`) persisten en heap hasta GC pese al `wipe()` de CharArray (limitación documentable).
5. **[CONFIRMADO EN CÓDIGO]** KDF: PBKDF2-SHA256 120k + salt aleatorio + hash cifrado con clave de hardware — diseño sólido salvo los defectos 4.3-1/2.
6. **[CONFIRMADO EN CÓDIGO]** El propio launcher no puede ocultarse (doble guarda repo+UI).

# 10. RIESGOS DE RENDIMIENTO

1. **[CONFIRMADO EN DISPOSITIVO]** Arranque como HOME: `Displayed +1s951ms` (frío). Aceptable pero a monitorizar; no cambió respecto a v1.5.
2. **[CONFIRMADO EN CÓDIGO]** Churn del `pointerInput` del spread por keys (`widgetBoundsList`) — jank en cambios de bounds.
3. **[CONFIRMADO EN CÓDIGO]** Recomposición por evento táctil en `HiddenAppsScreen` (temporizador 60 s).
4. **[CONFIRMADO EN CÓDIGO]** PBKDF2 120k en Main durante setup (~0,3–0,8 s de UI congelada percibida en ese diálogo).
5. **[CONFIRMADO EN DISPOSITIVO — nuevo]** Bucle de reintento del video wallpaper al perderse el permiso SAF del URI restaurado: `SecurityException` de `MediaDocumentsProvider` repetida por `ExoPlayer`/`LoadTask` (5+ intentos/segundo observados) tras reinstalar con estado restaurado. No bloquea Main (hilo de loader) pero es churn continuo y spam de errores. Componente protegido — solo registrar como observación.
6. **[CONFIRMADO EN CÓDIGO]** Gson completo del estado en cada `updateState` (JSON pequeño; menor).

# 11. RIESGOS DE PLAY/PRIVACY

- **[CONFIRMADO EN CÓDIGO]** Permisos: solo se añadió `USE_BIOMETRIC` (normal). Sin ubicación, sin red nueva, sin telemetría.
- **[CONFIRMADO EN CÓDIGO]** Todo el procesamiento es local; `docs/PRIVACY_POLICY.md` y la web ya documentan Hidden Apps.
- **[REQUIERE VERIFICACIÓN EN PLAY CONSOLE]** Data Safety: confirmar que la lista oculta en backup no contradice la declaración ("datos no compartidos"); los backups de Android no son "transmisión del desarrollador", pero conviene revisar la respuesta de Data Safety vigente al enviar.
- **[REQUIERE REVISIÓN LEGAL/POLÍTICA]** Si se decide cifrar/excluir del backup la lista oculta (recomendado, §12), actualizar la política.
- **[REQUIERE VERIFICACIÓN EN PLAY CONSOLE]** `androidx.biometric:1.1.0` es una versión 2021: verificar compatibilidad declarada con targetSdk 36 en el momento del envío (no es rechazo automático, pero conviene actualizarla por mantenibilidad).

# 12. ARCHIVOS QUE DEBERÍAN MODIFICARSE

| # | Archivo | Motivo | Cambio necesario | Prioridad | Riesgo |
| :-- | :--- | :--- | :--- | :--- | :--- |
| 1 | `gradle/libs.versions.toml` + `app/build.gradle.kts` | biometric 1.1.0 inyecta appcompat 1.6.1 + fragment 1.3.6 (2021) en el path crítico HOME | Subir a androidx.biometric moderno (≥1.2.x) y validar el árbol resuelto | **P0** | Bajo |
| 2 | `MainActivity.kt` | Freeze en path HOME; única alteración estructural del camino | Decisión arquitectónica: (a) mantener FragmentActivity con biometric moderno validando en físico, o (b) revertir a ComponentActivity y aislar BiometricPrompt en una Activity dedicada translúcida | **P0** | Medio |
| 3 | `HiddenAppsCrypto.kt` | Crash API 24-25 (PBKDF2WithHmacSHA256 desde API 26) | Fallback KDF para API < 26 o deshabilitar modos OWN_* en esos niveles con mensaje claro | **P1** | Bajo |
| 4 | `HiddenAppsRepository.kt` | KDF de setup en Main | Envolver `deriveHash`/operaciones de vault en `Dispatchers.Default` (simétrico a `verifyOwnCredential`) | **P1** | Bajo |
| 5 | `ui/screens/hiddenapps/HiddenAppsGesture.kt` | Churn por keys | Keys de primitivas; bounds leídos por getter/`rememberUpdatedState` dentro del loop | **P1** | Bajo |
| 6 | `ui/screens/hiddenapps/HiddenAppsScreen.kt` | Recomposición por evento táctil | Timestamp no-observable + evaluación periódica (sin estado observable por puntero) | P2 | Bajo |
| 7 | `backup_rules.xml` + `data_extraction_rules.xml` | Lista oculta en texto claro viaja en backup (restauración observada) | Excluir `launcher_prefs`/`launcher_state` del backup (trade-off documentado: el usuario reconfigura tras restore) o cifrar la lista | P2 | Bajo |
| 8 | `HiddenAppsAuthenticator.kt` | Cooldown en memoria | Persistir timestamp de cooldown (en vault) y considerar lockout progresivo | P2 | Bajo |
| 9 | `HiddenAppsCredentialStore.kt` / docs | Lockout sin escape con vault perdido | Definir ruta de recuperación explícita (p. ej., reset autenticado por device credential cuando exista, documentado; degradación comunicada si no) | P2 | Medio |

# 13. ARCHIVOS QUE NO DEBEN TOCARSE

Protegidos (sin evidencia de relación directa con el freeze ni con Hidden Apps):

- `VideoWallpaperManager.kt`, `VideoBackground.kt` (el bucle de SecurityException observado es solo observación)
- `WidgetHostManager.kt`, `NativeWidgetView.kt`
- `ViewOne.kt`, `ViewTwo.kt`
- `LauncherAccessibilityService.kt` y `accessibility_service_config.xml` (estado COMPLIANCE-FIX-01 preservado)
- `NotificationMonitorService.kt`
- `LauncherNavState.kt`
- Navegación NavHost validada y gestos ya validados (swipe vertical, pinch-in, double-tap) salvo la exclusión mutua ya implementada
- `AndroidManifest.xml` (más allá de lo ya declarado)
- `AppDrawerScreen.kt` — **no requiere cambios** (filtrado correcto, CRASH-FIX-01 intacto)
- Edge-to-edge, ciclo de vida del video wallpaper, persistencia de estilos

# 14. ARQUITECTURA PROPUESTA

Sin código, el diseño recomendado:

1. **Gesture entry:** detector spread como modificador aislado a nivel de pager (como hoy), keys de primitivas (`isHome`, `isEditMode`, `isUnlockPending`), bounds por `rememberUpdatedState`/getter dentro del loop, one-shot con consumo. Sin `List<Rect>` en keys.
2. **State:** mantener la máquina local `Closed → Setup → Authenticating → Visible` en `LauncherScreen` (fail-closed, volátil), centralizando la revocación (Back/HOME/ON_STOP/timeout 60 s) en un único punto tipo `SessionController` para eliminar asignaciones duplicadas.
3. **Repository:** mantener `HiddenAppsRepository` (config en `LauncherState` + vault Keystore) con `prune` y self-hide; añadir dispatcher explícito en todas las operaciones criptográficas.
4. **Authentication gate:** **desacoplar la autenticación de la Activity HOME**. Opción recomendada: Activity dedicada translúcida (`Theme.Translucent.NoTitleBar`, `excludeFromRecents`) que hospede BiometricPrompt/ConfirmDeviceCredential y devuelva resultado — `MainActivity` vuelve a `ComponentActivity` y el HOME deja de depender de fragment. Alternativa válida (si la validación física del rol es limpia): FragmentActivity con biometric ≥ 1.2.x.
5. **Hidden drawer:** `HiddenAppsScreen` sobre la pila de Home (sin rutas NavHost) — como hoy; timer de inactividad sin recomposición por evento.
6. **App launch:** lanzamiento normal desde el cajón oculto; shortcuts ya fijados siguen funcionando (decisión de producto explícita).
7. **Lifecycle:** fail-closed actual correcto; documentar edge DEVICE_CREDENTIAL API 24-29 (ON_STOP durante auth externa revoca la sesión).
8. **Security:** cooldown persistido; ruta de escape documentada para vault perdido; lista oculta excluida del backup (o cifrada).
9. **Persistence:** la actual es estable; opcionalmente aislar hiddenPackages en prefs propio excluido de backup.

# 15. PLAN DE IMPLEMENTACIÓN PARA OTRA IA

Secuencia estricta (un solo prompt integral puede ejecutarla):

1. **P0-A — Library:** subir `androidx.biometric` (≥ 1.2.x) en `libs.versions.toml`/`build.gradle.kts`; ejecutar `:app:dependencies` y documentar el árbol resultante (fragment moderno, sin appcompat sorpresa).
2. **P0-B — Freeze path:** con el teléfono conectado, compilar e instalar debug; repetir la reproducción instrumentada de hoy (logcat + screencap) ejecutando a mano el flujo interactivo: gesto → Setup → credencial → cambiar default desde Settings del sistema → volver. Si reaparece el freeze: capturar traza (`kill -3` + logcat) y decidir entre (a) mantener FragmentActivity con biometric moderno o (b) Activity de auth dedicada translúcida + revertir `MainActivity` a `ComponentActivity`. Repetir hasta lograr 3 pasadas limpias del flujo completo.
3. **P1 — Correcciones confirmadas (quirúrgicas):** (3a) fallback KDF API < 26 en `HiddenAppsCrypto`; (3b) dispatcher en `saveOwnCredential`/vault ops; (3c) keys del gesto.
4. **P2 — Hardening:** timer de `HiddenAppsScreen` sin recomposición por evento; exclusión de backup de la lista oculta; cooldown persistido; ruta de escape de vault perdido.
5. **Validación:** ejecutar la matriz de la sección 16 en el POCO X6; registrar resultados físicos con confirmación del desarrollador (protocolo `novalauncher-physical-validation`).
6. **Cierre:** actualizar `AGENT_PROJECT_STATE.md`, informe de implementación, y solo entonces commitear (una fase = un commit lógico).

# 16. MATRIZ DE PRUEBAS

| # | Escenario | Qué verificar |
| :-- | :--- | :--- |
| 1 | Launcher normal (MIUI) | El cambio de default no afecta al sistema; retorno posible |
| 2 | Screen 1 — gesto | Spread dispara Setup/Auth desde página 1 |
| 3 | Screen 2 — gesto | Spread dispara igual desde página 2 |
| 4 | Gesto dos dedos | Cancelación por 3er dedo, pinch inverso, dedos sobre widget, umbral 1.45x natural |
| 5 | App Drawer | Ocultas ausentes; resto intacto |
| 6 | Búsqueda | Ocultas ausentes en resultados |
| 7 | A-Z | Rail sin letras fantasma; CRASH-FIX-01 estable |
| 8 | Categorías | Ocultas ausentes; auto-categorización no las re-añade |
| 9 | Widgets | Spread excluido sobre widgets; widgets operativos |
| 10 | Edit mode | Spread y pinch inhibidos; banner "Listo" |
| 11 | Pinch | Abre Settings con flow Closed; inhibido con Hidden Apps activo |
| 12 | Swipe vertical | Drawer y notificaciones operativos |
| 13 | Autenticación correcta (5 modos) | Acceso a HiddenAppsScreen |
| 14 | Autenticación incorrecta | Error visible, cooldown a los 5 fallos |
| 15 | Cancelación | Vuelta a Closed sin cambios de config |
| 16 | Back | Prioridad: Hidden → edit → drawer → página 0 |
| 17 | Home | HOME revoca sesión y cierra overlays |
| 18 | Pérdida de foreground | ON_STOP revoca sesión (fail-closed) |
| 19 | Relaunch | Sin sesión heredada; config persistente |
| 20 | Reboot | Estado íntegro; arranque < 2.5 s |
| 21 | App desinstalada (oculta) | Prune automático; reaparece en Drawer |
| 22 | App reinstalada | Sin duplicados ni crash |
| 23 | Cambio de launcher predeterminado (con Hidden Apps configurado) | **Sin freeze**; selector del sistema operativo; retorno posible — CRÍTICO |
| 24 | Retorno a otro launcher | Nova recuperable; sin estado pegado |
| 25 | Configuración del sistema (default apps) | Flujo Settings → HOME limpio |
| 26 | Fallo durante autenticación | Sin cambios de config; sin bypass |
| 27 | Repeated open/close (20x) | Sin fugas ni estados pegados |
| 28 | Crash recovery | Tras kill del proceso: fail-closed, config íntegra |
| 29 | API 24/25 (si disponible) | OWN_* degradado o funcional sin crash |
| 30 | Restore de backup | Comportamiento definido con vault ausente |

# 17. VEREDICTO TÉCNICO

| Hallazgo | Clasificación |
| :--- | :--- |
| Freeze del launcher al cambiar default (causa raíz no confirmada) | **BLOQUEADOR** — es la prioridad absoluta; no se implementa nada encima hasta cerrarlo con la reproducción humana-instrumentada (§8.5) y la decisión P0-B |
| `MainActivity` = FragmentActivity + fragment 1.3.6 + appcompat 1.6.1 (transitivos nuevos) en el path HOME | **BLOQUEADOR** (condicionado) — debe resolverse junto al freeze: upgrade de librería o desacople de la autenticación |
| Crash PBKDF2 en API 24-25 (minSdk 24) | **ALTO RIESGO** — crash confirmable por código en el rango de minSdk declarado |
| KDF de setup en Main (~0,5 s) | **RIESGO MEDIO** — UI congelada percibida en setup; no explica el freeze |
| Churn del gesto por keys + recomposición por evento táctil | **RIESGO MEDIO** — jank; sin bloqueo |
| Lista oculta en texto claro y viajando en cloud backup (restauración observada) | **ALTO RIESGO** (privacidad) — corrección P2 barata |
| Lockout sin escape (OWN_* + vault perdido) y cooldown en memoria | **RIESGO MEDIO** |
| Bucle SecurityException del video wallpaper tras restore | **OBSERVACIÓN** — componente protegido, solo registro |
| Filtrado del App Drawer (punto único, pre-índices) | **SIN HALLAZGO** — correcto |
| CRASH-FIX-01 | **SIN HALLAZGO** — preservado y reforzado |
| Gesto spread (exclusiones, one-shot, convivencia con pinch/swipe) | **SIN HALLAZGO** (salvo las keys, ya listadas) |
| Arranque HOME (+1.9 s) | **OBSERVACIÓN** — pesado pero funcional, sin regreso vs 1.5 |
| Play/Privacy | **OBSERVACIÓN** + **[REQUIERE VERIFICACIÓN EN PLAY CONSOLE]** en Data Safety y versión de biometric |

**Veredicto global: BLOQUEADOR.** La implementación de Hidden Apps es arquitectónicamente correcta en sus capas de datos, drawer y gesto, y la reproducción automatizable de hoy (rol + arranque HOME + operación, binario idéntico al del incidente) resultó limpia; pero (1) la causa del freeze reportado no puede darse por cerrada sin la reproducción humana del tramo interactivo (gesto → Setup/Auth → cambio de default), (2) el cambio estructural de la Activity HOME con librerías legacy inyectadas sigue siendo la hipótesis primaria y exige la decisión P0-B antes de continuar, y (3) existen cuatro defectos confirmados (crash API < 26, KDF en Main, churn de gesto, backup en claro) que deben corregirse antes de cualquier release. La recuperación del launcher quedó demostrada y ejercida: el rol es restaurable por adb y el teléfono quedó en su estado original.

---

*Anexo de evidencia en `C:\Users\migue\.zcode\tmp\bugreport-nova\`: `br-20261002.zip` (bugreport completo con `/data/anr`), `logs/repro.log` (sesión instrumentada), `nova_repro_1.png` (launcher vivo como HOME). Ningún archivo del repositorio fue modificado durante esta auditoría; este informe es el único archivo creado.*
