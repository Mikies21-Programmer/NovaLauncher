# HIDDEN-APPS-STABILIZATION-01 — Informe de auditoría, reparación y validación

> Fecha: 2026-10-02 · Dispositivo de validación: POCO X6 5G (`2e81d6a9`), Android 16 / HyperOS (BP2A.250605.031.A3)
> Base de trabajo: commit `eb09b71` + implementación Hidden Apps sin commitear + correcciones de esta sesión (sin commit, según reglas).
> Fuentes: `docs/HIDDEN-APPS-ZAI-AUDIT-01.md` (auditoría forense), sesión de reproducción del 01-02 oct.

---

## 1. Estado actual

La aplicación compila, pasa 107/107 tests unitarios, se instala y ejecuta en el teléfono real como launcher HOME, abre y cierra el App Drawer, sobrevive a tres ciclos completos de cambio de launcher predeterminado y conserva el rol HOME tras reiniciar el teléfono. La cadena de logcat de toda la validación (283.330 líneas, 16.003 del paquete) registra **cero ANR y cero "Input dispatching timed out"** de NovaLauncher.

**Actualización 15:31-15:51 del 02-oct:** el intento de validación interactiva reprodujo el fallo original y dejó la **causa raíz confirmada con stack trace** (Problema #10): el proceso HOME crasheaba al componer el diálogo de autenticación de Hidden Apps. Corregido, recompilado e instalado a las 15:51. Queda pendiente la **re-validación física del flujo interactivo** (gesto → setup → autenticación → cambio de launcher), que requiere las manos del desarrollador porque HyperOS bloquea la inyección táctil automatizable (SELinux: `sendevent: Permission denied`).

## 2. Problemas encontrados

| # | Problema | Archivo | Componente | Línea aprox. | Causa raíz | Evidencia | Impacto | Solución aplicada |
| :-- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | KDF de PBKDF2 (120k iteraciones) ejecutándose en el hilo principal durante la configuración del bloqueo ("bloqueador") | `HiddenAppsRepository.kt` | `saveOwnCredential` | 79-98 | La función suspend corría en el dispatcher del llamador (coroutineScope de UI = Main); `deriveHash` bloquea Main ~0,3–0,8 s justo al confirmar PIN/patrón/contraseña | [CONFIRMADO EN CÓDIGO] asimetría con `verifyOwnCredential` (sí usa `Dispatchers.Default`) | Jank/bloqueo percibido en el paso exacto que el usuario asocia al freeze; contribuidor plausible del incidente | `withContext(Dispatchers.Default)` envolviendo KDF + bóveda + try/catch fail-closed |
| 2 | Crash al configurar/verificar credenciales propias en Android 7.x (API 24-25) | `HiddenAppsCrypto.kt` | `deriveHash` | 36-48 | `PBKDF2WithHmacSHA256` no está registrada antes de API 26 y `NoSuchAlgorithmException` no se capturaba (minSdk 24) | [CONFIRMADO EN CÓDIGO] docs de `SecretKeyFactory` | Crash en el subconjunto de dispositivos minSdk | Fallback a `PBKDF2WithHmacSHA1` capturando `NoSuchAlgorithmException`; registros locales al dispositivo, el algoritmo nunca cruza equipos |
| 3 | Stack legacy (fragment 1.3.6 de 2021 + appcompat) inyectado en el ciclo de vida de la Activity HOME | `gradle/libs.versions.toml` | dependencia `androidx.biometric:1.1.0` | 11 | `biometric:1.1.0` arrastra `fragment:1.3.6`; `MainActivity` (HOME) es `FragmentActivity` → maquinaria de FragmentManager 2021 en cada `onNewIntent`/`onActivityResult` del flujo HOME | [CONFIRMADO EN DISPOSITIVO] árbol resuelto; fue el sospechoso primario del freeze (auditoría ZAI-01 §2) | Riesgo de freeze/trap en el camino más crítico del launcher | Upgrade a `biometric:1.4.0-alpha07` (release vigente de Google): fragment resuelve ahora a **1.6.2**; API usada idéntica; 107 tests y flujos del dispositivo validados |
| 4 | Superficie negra del launcher durante transiciones del sistema con diálogos de Hidden Apps abiertos | `HiddenAppsAuthDialog.kt`, `HiddenAppsSetupDialog.kt` | FLAG_SECURE en la ventana de la Activity | 92-98 / 84-90 | `FLAG_SECURE` sobre la **ventana de la Activity HOME** ennegrece el launcher en recents/capturas/transición de rol — consistente con el forense (freeze SIN traza ANR: el proceso nunca dejó de responder, la superficie quedaba negra/pegada) | [CONFIRMADO EN CÓDIGO] + forense ZAI-01 §8.2 (cero ANR del incidente real) | Percepción de "congelado" + posible render pegado en HyperOS; teléfono "atrapado" | FLAG_SECURE retirado de la ventana HOME en ambos diálogos; la protección real se mantiene (`SecureFlagPolicy.SecureOn` en la ventana del diálogo, que es donde vive el contenido sensible) y el FLAG_SECURE a pantalla completa permanece en `HiddenAppsScreen` |
| 5 | Detector del gesto spread se cancelaba/relanzaba continuamente | `HiddenAppsGesture.kt` | `hiddenAppsSpreadGesture` | 161 | `widgetBoundsList` (lista nueva en cada pase de layout de widget) era **key de `pointerInput`** | [CONFIRMADO EN CÓDIGO] | Jank y churn de corrutinas en Home | Keys reducidas a primitivas; bounds leídos por getter `() -> List<Rect>` dentro del ciclo (patrón del pinch, key `Unit`) |
| 6 | Recomposición de pantalla completa por evento táctil | `HiddenAppsScreen.kt` | temporizador de inactividad 60 s | 84-89, 120 | El timestamp de interacción era `mutableLongStateOf` escrito en **cada** evento de puntero | [CONFIRMADO EN CÓDIGO] | Jank durante scroll del cajón oculto | Timestamp en objeto no observable + comprobación periódica cada 5 s |
| 7 | Fallo de bóveda silencioso al guardar credencial | `HiddenAppsSetupDialog.kt` | confirmación de credencial | 363-374, 385-397, 431-443 | `saveOwnCredential()==false` no mostraba nada (Keystore invalidada → usuario sin feedback) | [CONFIRMADO EN CÓDIGO] | Usuario cree que configuró el bloqueo cuando no se guardó | Mensaje de error nuevo `hidden_apps_setup_save_failed` (es/en) en las 3 ramas |
| 8 | Lista de apps ocultas viaja en claro en el backup en la nube | `backup_rules.xml`, `data_extraction_rules.xml` | persistencia | — | Solo el vault estaba excluido; `launcher_prefs` (con `hiddenPackages`, `isConfigured`, `authMode`) se respalda y **se restauró automáticamente** en la reinstalación del 02-oct | [CONFIRMADO EN DISPOSITIVO] | Privacidad + escenario de lockout si el vault no se restaura | **No modificado en esta fase** (decisión de producto: excluir = el usuario pierde su config al migrar). Documentado en §8 |
| 9 | Lockout sin escape en modo OWN_* si el vault se pierde (restore) | `HiddenAppsAuthenticator.kt` | verificación | 89-92 | `loadCredential()==null` → verificación imposible siempre | [CONFIRMADO EN CÓDIGO] | Apps ocultas irrecuperables salvo borrar datos | **No modificado en esta fase** (escapar degradaría la seguridad; requiere decisión de producto). Documentado en §8 |
| 10 | **CRASH — causa raíz confirmada del incidente original**: `IllegalStateException: No ActivityResultRegistryOwner was provided via LocalActivityResultRegistryOwner` al componer el diálogo de autenticación | `ui/screens/LauncherScreen.kt` (bloque de idioma de Hidden Apps) + mecanismo interno de `activity-compose` 1.9.0 | `CompositionLocalProvider` (1100-1116) / `HiddenAppsAuthDialog.kt:107,118` | En activity-compose 1.9.0 `LocalActivityResultRegistryOwner` ya no se provee en la raíz de `setContent`: su fallback **desenrolla la cadena `ContextWrapper` de `LocalContext`** buscando la Activity. El bloque de Hidden Apps envolvía los diálogos en `LocalContext provides createConfigurationContext(...)` — un Context pelado que no es `ContextWrapper` — así que el fallback devuelve null y `checkNotNull` lanza ISE en **cada composición** del diálogo (verificado en bytecode de 1.9.0: default `aconst_null`, bucle `instanceof ContextWrapper`) | [CONFIRMADO EN DISPOSITIVO] 2 FATAL el 02-oct 15:31:55 y 15:32:14 (pids 406/6499, log `p0b/interactive.log`), misma traza; los otros 3 launchers de LauncherScreen (widgets/media, fuera del wrapper) nunca crashearon | El proceso HOME **muere** justo tras configurar/autenticar el bloqueo; los relanzamientos en cadena durante la transición de launcher = el "congelamiento" percibido y el flujo de launcher predeterminado roto. Mismo mecanismo anulaba `LocalOnBackPressedDispatcherOwner`: `BackHandler` de `HiddenAppsScreen` y back-dismiss de los diálogos quedaban muertos (sin crash) | Owner y dispatcher reales de la Activity provistos explícitamente dentro del bloque de idioma (`listOfNotNull` + spread en `CompositionLocalProvider`); el override de idioma se conserva intacto |
| 11 | **Regresión de la ruta biométrica tras fix #10**: al seleccionar biométrica en Setup, la configuración se guarda (`authMode=BIOMETRIC, isConfigured=true`) pero el BiometricPrompt **nunca aparece**, el flujo queda atrapado en `Authenticating` sin UI, y spread (centro→afuera) y pinch (afuera→centro) dejan de responder — ambos detectores exigen `hiddenAppsFlowState == Closed` | `HiddenAppsAuthDialog.kt` | `LaunchedEffect(authMode)` + resolución de la Activity | 130 (antes del fix): `context as? FragmentActivity` donde `context = LocalContext.current` es el **contexto de configuración del bloque de idioma**, nunca la Activity → null → `if (fragmentActivity != null)` se salta completo: sin prompt, sin error, sin UI (en modo BIOMETRIC sin `errorMessage` el diálogo no compone nada) | [CONFIRMADO EN DISPOSITIVO] intento 15:57:40-45 del 02-oct: toques en MainActivity, Setup llama `canAuthenticateBiometric` (único evento `BiometricService/PreAuthInfo` de la ventana, 15:57:45), **cero actividad de `BiometricFragment`/`authenticate` en todo el log**, 0 FATAL; prefs: `authMode=BIOMETRIC, isConfigured=true` persistidos; ambos gestos gated a `== Closed` en LauncherScreen (líneas ~528 y ~546) | Bloqueo total e invisible de Hidden Apps con biometría: cada gesto futuro recae en `Authenticating` (por `isConfigured=true`) y re-entra al pozo; recuperable solo vía Back/HOME | `fragmentActivity` resuelto desde `LocalActivityResultRegistryOwner.current` (fijado por fix #10) con fallback a `LocalContext`; y la rama sin Activity ahora es **fail-visible** (`errorMessage=strAuthFailed`) en lugar de fail-invisible; el mismo cast de la rama de reset (línea 601) usa el mismo owner |

## 3. Problema del congelamiento

**Causa raíz CONFIRMADA el 02-oct a las 15:31 con reproducción y stack trace (Problema #10):** al componer `HiddenAppsAuthDialog`, `rememberLauncherForActivityResult` lanzaba `IllegalStateException` porque el bloque de idioma de Hidden Apps sobrescribía `LocalContext` con un contexto de configuración que oculta la Activity al mecanismo de resolución de activity-compose 1.9.0. El proceso HOME **moría de forma determinista** en el instante exacto que el usuario describe ("después de seleccionar/configurar el bloqueador": Setup termina → flujo pasa a `Authenticating` → crash).

La secuencia percibida en el incidente original: el launcher (HOME) se cerraba y relanzaba repetidamente con los diálogos de Hidden Apps en pantalla, en medio de la transición de launcher predeterminado → el teléfono parecía congelado y Android no completaba la selección. El forense de ZAI-01 no halló ANR porque nunca hubo ANR: había **crash** (y las entradas de dropbox del incidente, de días antes, ya no estaban disponibles).

Los tres mecanismos contribuyentes corregidos antes siguen siendo defectos reales pero secundarios (#1 jank del KDF, #3 stack fragment legacy, #4 superficie negra por FLAG_SECURE). El mecanismo fino del incidente queda cerrado con evidencia: **el crash del #10 ya no existe en el binario actual** (corregido 15:51, pendiente de re-validación física del flujo completo).

## 4. Problema del launcher

El Manifest estaba **correcto desde el origen** [CONFIRMADO EN CÓDIGO]: `MainActivity` con `launchMode="singleTask"`, `excludeFromRecents`, intent-filter `MAIN + LAUNCHER + HOME + DEFAULT` — Android puede seleccionarla como launcher sin ambigüedad. Lo que impedía "completar el flujo" era **el crash determinista del proceso HOME al componer el diálogo de autenticación** (§3, Problema #10): Android completaba el cambio de rol, pero el launcher recién activado moría/relanzaba en cadena. Validado hoy en el dispositivo: 3 ciclos completos MIUI↔Nova limpios, rol persistido tras reboot, y arranque como HOME operativo; tras el fix del #10 (15:51) el binario reintenta el mismo flujo con el crash eliminado.

## 5. Cajón oculto

- Integración en el pipeline correcta y única: filtro `hiddenPackages` **antes** de `prepareDrawerCategory` → desaparece de Drawer, búsqueda, categorías y A-Z con una sola fuente de verdad. CRASH-FIX-01 preservado y reforzado (auditoría ZAI-01 §7) — **sin cambios en esta fase porque es correcto**.
- Corregido dentro del cajón oculto: churn del detector de entrada (#5), recomposición por evento (#6), error silencioso al guardar (#7), superficie negra por FLAG_SECURE (#4).
- Validado en dispositivo: apertura del App Drawer por swipe, render A-Z/categorías/búsqueda correcto (captura), drawer sobre build corregida.

## 6. Cambios realizados

| Archivo | Cambio |
| :--- | :--- |
| `gradle/libs.versions.toml` | `biometric 1.1.0 → 1.4.0-alpha07` (fragment 1.3.6 → 1.6.2 en el árbol resuelto) |
| `hiddenapps/HiddenAppsCrypto.kt` | Fallback `PBKDF2WithHmacSHA1` para API < 26 (+import, documentación) |
| `hiddenapps/HiddenAppsRepository.kt` | `saveOwnCredential` en `Dispatchers.Default` + fail-closed con try/catch |
| `ui/screens/hiddenapps/HiddenAppsGesture.kt` | Firma `currentWidgetBoundsList: () -> List<Rect>`; keys del `pointerInput` sin la lista |
| `ui/screens/LauncherScreen.kt` | Call site del gesto actualizado al getter |
| `ui/screens/hiddenapps/HiddenAppsScreen.kt` | Temporizador 60 s sin estado observable por evento |
| `ui/screens/hiddenapps/HiddenAppsAuthDialog.kt` | FLAG_SECURE retirado de la ventana HOME (se conserva SecureOn del diálogo); import limpio |
| `ui/screens/hiddenapps/HiddenAppsSetupDialog.kt` | Ídem + error visible si la bóveda falla; imports limpios |
| `res/values/strings.xml`, `res/values-en/strings.xml` | Nueva cadena `hidden_apps_setup_save_failed` (es/en) |
| `ui/screens/LauncherScreen.kt` (fix #10, 15:51) | Owner/dispatcher de la Activity provistos dentro del bloque de idioma de Hidden Apps: `LocalActivityResultRegistryOwner provides` + `LocalOnBackPressedDispatcherOwner provides` vía `listOfNotNull` + spread — repara el crash del diálogo de autenticación y el Back de `HiddenAppsScreen`, sin tocar el override de idioma ni los detectores de gesto |
| `ui/screens/hiddenapps/HiddenAppsAuthDialog.kt` (fix #11, 16:12) | `fragmentActivity` resuelto de `LocalActivityResultRegistryOwner.current` con fallback a `LocalContext` (BiometricPrompt vuelve a lanzarse en BIOMETRIC/DEVICE_CREDENTIAL, auth y reset); rama sin Activity ahora fail-visible (`errorMessage`); eliminados los 2 casts internos `context as? FragmentActivity` |

**No modificado deliberadamente:** `MainActivity` (FragmentActivity se mantiene; la alternativa de Activity de auth dedicada queda como plan B documentado), gestos validados, Views, servicios protegidos, reglas de backup, `AppDrawerScreen` (correcto), Manifest.

## 7. Pruebas realizadas (teléfono real)

| Prueba | Resultado |
| :--- | :--- |
| `testDebugUnitTest` | **107/107 PASS, 0 fallos** (incluye 45 de Hidden Apps) |
| `assembleDebug` | BUILD SUCCESSFUL; instalado vía adb (`Success`) |
| Arranque como HOME (build corregida) | Foco en `MainActivity`, proceso vivo, escritorio renderizado (captura `fix_home_start.png`) |
| App Drawer (swipe up automatizado) | Abre; búsqueda/categorías/A-Z correctos (captura `fix_drawer.png`); cierra con Back |
| Cambio de launcher predeterminado MIUI↔Nova ×3 | 3/3 limpios; ambos launchers suben; mismo pid (warm start por `onNewIntent`) |
| Reinicio completo del teléfono | Boot ~53 s; **rol HOME persistido** (Nova); el arranque quedó tras el keyguard (el dispositivo se bloqueó con mi reinicio — requiere desbloqueo del usuario para ver el primer frame) |
| Logcat de toda la validación (283.330 líneas) | **Cero ANR, cero FATAL, cero input-timeout de Nova**; única nota: primer arranque post-instalación +5,6 s (dexopt de primera ejecución; normal) |
| Reproducción del gesto + flujo completo de Hidden Apps (1.er intento, 15:31) | **FALLO REPRODUCIDO**: 2 FATAL `IllegalStateException` en `HiddenAppsAuthDialog` (pids 406/6499, 15:31:55 y 15:32:14) — Problema #10 confirmado en físico |
| Fix #10: `testDebugUnitTest` + `assembleDebug` (15:47) | **107/107 PASS**, BUILD SUCCESSFUL; instalado a las 15:51 (`Success`); smoke-check post-instalación: proceso vivo, foco en `MainActivity`, cero FATAL |
| Reproducción del gesto + flujo completo de Hidden Apps (re-test con fix #10) | **REGRESIÓN #11 DESCUBIERTA (15:57)**: biométrica guarda config pero el prompt nunca aparece; flujo atrapado en `Authenticating` sin UI; spread y pinch muertos (gated a `Closed`); 0 FATAL, cero actividad de BiometricPrompt en log |
| Fix #11: `testDebugUnitTest` + `assembleDebug` (16:12) | **107/107 PASS**, BUILD SUCCESSFUL; instalado (`Success`); smoke-check: proceso vivo, cero FATAL |
| Flujo biométrico completo con fix #11 + comparación PIN + ambos gestos | **PENDIENTE DE VALIDACIÓN FÍSICA** — requiere manos del desarrollador (SELinux impide inyectar multitáctil) |

## 8. Problemas restantes

1. **Re-validación física del flujo interactivo** (biometría completa + comparación PIN + ambos gestos): pendiente del desarrollador con la build de fix #11 (instalada 16:12). Nota: el teléfono quedó con `authMode=BIOMETRIC, isConfigured=true` persistido del intento de las 15:57 — con fix #11 el gesto debe abrir el BiometricPrompt directamente; el re-test del Setup biométrico se hace desde `HiddenAppsScreen → cambiar método`.
2. **Lista oculta en backup en claro** (Problema #8): requiere decisión de producto (excluir del backup vs privacidad).
3. **Lockout sin escape con vault perdido** (Problema #9): requiere decisión de producto/de seguridad.
4. **Cooldown anti-brute-force solo en memoria**: reinicio del proceso lo resetea.
5. **`biometric:1.4.0-alpha07` es canal alpha** (no existe estable moderna; `1.1.0` era de 2021). La superficie de API usada es estable desde 1.1; riesgo bajo, monitorizable.
6. **Bucle de reintento del video wallpaper** al perderse el permiso SAF de un URI restaurado (SecurityException repetida de ExoPlayer): componente protegido, sin impacto de Main thread; solo observación.
7. Primer arranque tras instalar: +5,6 s hasta primer frame (dexopt); mejora solo en las siguientes ejecuciones (~+1,9 s medido el 02-oct con la build previa).
8. **Regla arquitectural derivada del #10**: cualquier nuevo `CompositionLocalProvider(LocalContext provides ...)` que envuelva composables con `rememberLauncherForActivityResult`, `BackHandler` o `Dialog(dismissOnBackPress)` debe volver a proveer `LocalActivityResultRegistryOwner`/`LocalOnBackPressedDispatcherOwner` (mecanismo de resolución por desenrollado de Context en activity-compose ≥1.9.0; verificado en bytecode). No hay test de composición que cubra esto (la reproducción exige el gesto físico).
9. **Regla derivada del #11**: dentro del bloque de idioma de Hidden Apps, `LocalContext` NO es la Activity. Todo componente que necesite `FragmentActivity` (BiometricPrompt) debe resolverla vía `LocalActivityResultRegistryOwner.current`. Resolución por `LocalContext as? Activity` es un bug latente en ese ámbito.

## 9. Riesgos para Antigravity (mapa técnico de riesgo)

**Zona roja — no tocar sin protocolo (auditoría + prueba en físico previas):**
- `MainActivity` (cliclo de vida HOME, `onNewIntent`, navegación) — el freeze original vivió aquí.
- `LauncherScreen.kt` — pila de 3 detectores `pointerInput` en `Initial` + máquina de estados `hiddenAppsFlowState`; cualquier key nueva de `pointerInput` que reciba objetos mutables reabre el churn (#5).
- `VideoWallpaperManager` / `VideoBackground` — ciclo de vida sensibelísimo a focus/pause; dejar de protegidos.
- `WidgetHostManager` / `NativeWidgetView` / `ViewOne` / `ViewTwo` — IPC de widgets y emisión de bounds (`onWidgetBoundsChanged`) que alimentan los detectores.

**Zona amarilla — tocar con tests:**
- `LauncherViewModel.updateState/saveState` — persistencia Gson síncrona en el hilo llamador; todo lo que se añada a `LauncherState` engorda cada guardado.
- `HiddenAppsCredentialStore` — cualquier cambio de formato del vault debe mantener fail-closed.
- `AppDrawerScreen` — las guardas de CRASH-FIX-01 y el `remember(preparedCategory, ...)` son el contrato; no filtrar después de `prepareDrawerCategory`.
- `backup_rules.xml` / `data_extraction_rules.xml` — atados al formato del vault.

**Contratos que no romper:**
- El gesto spread es la ÚNICA entrada a Hidden Apps (no añadir accesos laterales sin decidirlo).
- `hiddenPackages` se filtra una sola vez, antes de `prepareDrawerCategory`.
- La sesión de Hidden Apps es fail-closed (Back/HOME/ON_STOP revocan).
- FragmentActivity es requisito de `androidx.biometric` — si se revierte a `ComponentActivity`, hay que reubicar el BiometricPrompt primero.
- Dentro del bloque de idioma de Hidden Apps, `LocalContext` no es la Activity: `LocalActivityResultRegistryOwner`/`LocalOnBackPressedDispatcherOwner` se proveen explícitos y cualquier `FragmentActivity` se resuelve de ahí (#10/#11). No envolver NADA que use launchers/BackHandler/`as? Activity` en un nuevo override de `LocalContext` sin reaprovisionar esos locals.
- Spread y pinch solo corren con `hiddenAppsFlowState == Closed`: cualquier estado nuevo del flujo debe garantizar salida visible (prompt, diálogo o error) — nunca un estado sin UI.

## 10. Estado final

| Área | Estado |
| :--- | :--- |
| Compilación y tests | **CORRECTO** (107/107, BUILD SUCCESSFUL — re-validado tras fix #10) |
| Establecimiento como launcher (registro, rol, ciclos, reboot) | **CORREGIDO / CORRECTO** — 3 ciclos + reboot validados en físico |
| Congelamiento | **CAUSA RAÍZ CONFIRMADA Y CORREGIDA** (crash #10: ISE en `HiddenAppsAuthDialog` por locals de Activity ocultos; fix 15:51) — flujo interactivo completo **NO VERIFICADO** (pendiente re-test físico) |
| Bloqueo/autenticación de Hidden Apps (KDF, fallback API<26, feedback de bóveda, registro de launchers) | **CORREGIDO** — flujo de autenticación **NO VERIFICADO** en físico desde el fix #10 |
| Ruta biométrica (BiometricPrompt, éxito/cancel/error, reset biométrico) | **CORREGIDO** (#11: FragmentActivity resuelta del owner; fail-visible) — **NO VERIFICADO** en físico |
| Cajón oculto (entrada, integración drawer, timer, churn, Back) | **CORREGIDO** — apertura/gesto/Back **NO VERIFICADO** en físico desde el fix #10 |
| App Drawer normal, búsqueda, A-Z | **CORRECTO** (validado en físico con build corregida) |
| Persistencia (estado, rol tras reboot) | **CORRECTO** (rol persistido; estado validado el 02-oct con restore) |
| Backup/privacidad de la lista oculta | **REQUIERE ATENCIÓN** (decisión de producto) |
| Lockout con vault perdido / cooldown persistente | **REQUIERE ATENCIÓN** (decisión de producto) |
| Dependencia alpha de biometric | **REQUIERE ATENCIÓN** (monitorizar release estable) |

---

*Criterio de éxito pendiente de cierre: desbloquear el teléfono y ejecutar a mano el flujo completo (gesto → configurar PIN → ocultar una app → verificar drawer → cambio de launcher desde Ajustes del sistema ×2). Con eso, las áreas NO VERIFICADO pasan a CORRECTO/CORREGIDO.*
