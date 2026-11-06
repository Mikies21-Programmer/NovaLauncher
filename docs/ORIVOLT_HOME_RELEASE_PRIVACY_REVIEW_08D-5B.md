# Orivolt Home — revisión de privacidad de Release 08D-5B

**Fecha de revisión:** 2026-11-04  
**Candidato revisado:** `com.daybreak.animelauncher`, versión `1.6.1` (`versionCode` 9), manifiesto extraído de bundletool en `ORIVOLT_HOME_RELEASE_MANIFEST_08D-5.xml` y classpath `releaseRuntimeClasspath` en `ORIVOLT_HOME_RELEASE_DEPENDENCIES_08D-5.txt`.  
**Alcance:** revisión estática y de los artefactos Release ya generados. No se instaló, desinstaló ni se borraron datos en un dispositivo real; no se modificó código, Gradle, manifiesto ni recursos.

## Resumen ejecutivo

**Podemos pasar a pruebas dinámicas. No existe un bloqueo técnico real para iniciarlas.** El manifiesto efectivo, las reglas de respaldo y el árbol de dependencias Release son coherentes con los informes 08D-4: no se detectó un SDK de analítica, anuncios, crash reporting, backend o identidad; tampoco se encontró en el código propio un cliente HTTP, `WebView`, `URLConnection`, Retrofit, Firebase ni telemetría.

Esto **no** permite concluir todavía que la aplicación no transmite datos. La Release conserva `INTERNET` y `ACCESS_NETWORK_STATE`; además, Coil trae OkHttp de forma transitiva y Media3 contiene capacidades generales de fuentes de datos. El código limita los medios de los flujos propios a URI locales, pero esa conclusión requiere observación en ejecución. `allowBackup=true` permite que Android/proveedor incluya datos generales elegibles: tampoco equivale a que ya se haya efectuado una transmisión, pero exige una prueba separada de backup/restauración antes de cerrar Data Safety.

La política HTML de Orivolt (`docs/orivolt-home-privacy-policy.html`) conserva correctamente el carácter pendiente de tráfico y Data Safety. El documento histórico `docs/PRIVACY_POLICY.md` sí afirma ausencia absoluta de transmisión y Data Safety “no recopila/no comparte”; no es coherente con el estado probatorio actual y **no debe usarse como base de la declaración final**.

## Evidencia revisada

- `docs/ORIVOLT_HOME_RELEASE_MANIFEST_08D-5.xml`: manifiesto efectivo extraído del paquete Release.
- `docs/ORIVOLT_HOME_RELEASE_DEPENDENCIES_08D-5.txt`: resolución de `releaseRuntimeClasspath`, no sólo Debug.
- `app/src/main/AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml` y los servicios/consumidores citados abajo: contraste de utilización en fuente actual. El árbol de trabajo ya contenía cambios ajenos; no se atribuyen ni se alteraron en esta fase.
- Informes 08D-4: auditoría, matriz Data Safety, validación dinámica pendiente y lista de pendientes.
- `docs/orivolt-home-privacy-policy.html`, `docs/ORIVOLT_HOME_PRIVACY_POLICY_REVIEW.md` y `docs/PRIVACY_POLICY.md`: contraste de declaraciones.

## Manifiesto efectivo y permisos

| Elemento efectivo | Estado | Evidencia y uso real | Riesgo / conclusión |
| --- | --- | --- | --- |
| `INTERNET` | **CONFIRMADO** | Está en el manifiesto Release. No se detectaron APIs de red ni URL remotas en código propio, pero Coil trae OkHttp. | Permite sockets; por sí solo no prueba envío. Prueba de red pendiente. |
| `ACCESS_NETWORK_STATE` | **CONFIRMADO** | Está en el manifiesto Release, aportado por una dependencia fusionada; no se detectó uso de `ConnectivityManager` en el código propio. | Permite consultar estado de red, no transmite por sí mismo. Origen exacto no queda demostrado por el informe de dependencias. |
| `USE_BIOMETRIC` y `USE_FINGERPRINT` | **CONFIRMADO** | Release declara ambos. `HiddenAppsAuthenticator` consulta `BiometricManager`; `HiddenAppsAuthDialog` usa `BiometricPrompt`. | Función local y opcional para Apps ocultas. La app recibe el resultado, no plantillas; validar el flujo de sistema en emulador. |
| `REQUEST_DELETE_PACKAGES` | **CONFIRMADO** | `AppSystemActions.kt` crea `ACTION_UNINSTALL_PACKAGE` para un paquete existente y Android presenta/aplica su propio control. | No desinstala silenciosamente; el dato entregado a Android es el identificador de paquete elegido. Validar sólo en AVD. |
| `BIND_APPWIDGET` | **CONFIRMADO** | Permiso efectivo y función de host de widgets. | El proveedor del widget es otro proceso/app y puede comunicarse por su cuenta. No atribuir su tráfico a Orivolt. |
| `SET_WALLPAPER` | **CONFIRMADO** | Permiso efectivo; no se observó una ruta de red derivada de él. | No demuestra comunicación; flujo funcional puede probarse aparte si se expone. |
| `BIND_ACCESSIBILITY_SERVICE` | **CONFIRMADO** | Servicio exportado, habilitable sólo en Ajustes Android; XML con `canRetrieveWindowContent=false` y `canPerformGestures=false`. `onAccessibilityEvent` no procesa eventos; sólo acciones globales de bloqueo/panel. | Acceso privilegiado opcional. Confirmar en AVD que el opt-in es explícito y que no hay contenido de ventana. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | **CONFIRMADO** | Servicio exportado y sujeto al consentimiento de Ajustes. `NotificationMonitorService` usa paquete, categoría e `isOngoing`; calcula un contador en `StateFlow`. | No se detectó lectura de `extras`, texto, remitente o adjuntos ni persistencia. Validar con notificaciones sintéticas. |
| `queries` MAIN/LAUNCHER | **CONFIRMADO** | Está en el manifiesto efectivo; se usa para inventario de apps del cajón. | No es permiso de red ni `QUERY_ALL_PACKAGES`. |
| `InitializationProvider` / ProfileInstaller | **CONFIRMADO** | Componentes AndroidX efectivos; el receiver ProfileInstaller está protegido por `android.permission.DUMP`. | Inicialización/rendimiento local; no se detectó servicio externo. |

El manifiesto efectivo no declara permisos de ubicación, contactos, cámara, micrófono, almacenamiento amplio, identificador publicitario ni `QUERY_ALL_PACKAGES`. **NO DETECTADO** significa sólo que no figura en el manifiesto revisado; no garantiza ausencia de una capacidad futura ni de un comportamiento de otro proceso.

## Dependencias Release y capacidad de comunicación

Las dependencias directas Release son: Kotlin/Compose/Activity/Core/Lifecycle, Material Icons, `androidx.biometric:biometric`, Navigation Compose, `io.coil-kt:coil-compose:2.6.0`, Gson, ProfileInstaller, `androidx.media3:media3-exoplayer`/`media3-ui` 1.4.1 y Palette. Su clasificación es:

| Categoría | Bibliotecas relevantes | Estado | Lectura de riesgo |
| --- | --- | --- | --- |
| Interfaz/navegación | Compose, Material3, Activity, Navigation, Lifecycle, Core, Palette | **CONFIRMADO** | UI, ciclo de vida y color; no se detectó una finalidad de red propia. |
| Almacenamiento/serialización | Gson; `media3-database` transitiva | **CONFIRMADO** | Gson soporta serialización local. La presencia de `media3-database` no acredita que la app cree o transmita una base de datos. |
| Biometría/seguridad | AndroidX Biometric y APIs Android | **CONFIRMADO** | Diálogo/resultado de autenticación delegado al sistema. |
| Multimedia/imágenes | Coil 2.6.0; Media3 ExoPlayer/UI/datasource/extractor/decoder | **CONFIRMADO** | Capaces de abrir fuentes según URI; no son SDK de analítica. En los flujos propios, `LauncherMediaUriValidator` admite sólo `content://`, `android.resource://` y `file:///android_asset/`, y rechaza HTTP/HTTPS/FTP antes de Coil/ExoPlayer. |
| Red transitiva | `com.squareup.okhttp3:okhttp:4.12.0` y Okio, bajo Coil | **CONFIRMADO (presencia/capacidad)** | OkHttp puede comunicar con servidores si recibe una petición/URL. Su presencia no prueba una solicitud ni transmisión de Orivolt. |
| Analítica, publicidad, crash reporting, servicios externos | Firebase, Crashlytics, Google Analytics, Sentry, Amplitude, Mixpanel, AdMob, Billing y equivalentes buscados | **NO DETECTADO** | No aparecieron en el classpath Release ni en la búsqueda dirigida de fuente. Sin garantía de ausencia total: la observación dinámica aún es necesaria. |

`LauncherMediaUriValidator.kt` limita las rutas de medios propias y los consumidores de fondo/icono/vídeo lo invocan. Esto es evidencia estática favorable contra una carga remota accidental por esos flujos, no evidencia de que ningún byte haya dejado el dispositivo. Un `content://` seleccionado por el usuario puede pertenecer a un proveedor de documentos/nube; si ese proveedor descarga bytes, su tráfico pertenece a su UID/proceso, no automáticamente a Orivolt.

## Respaldo y restauración

**CONFIRMADO:** el manifiesto Release efectivo usa `android:allowBackup="true"`, `@xml/backup_rules` y `@xml/data_extraction_rules`. Las reglas API 30 y anteriores excluyen `nova_hidden_apps_vault.xml` y `nova_hidden_apps_prefs.xml`; las reglas API 31+ repiten ambas exclusiones en `cloud-backup` y `device-transfer`.

**PENDIENTE:** los demás datos privados elegibles —por ejemplo `launcher_prefs`, referencias URI, diseño, accesos e IDs de widget— podrían ser respaldados, restaurados o transferidos según versión Android, transporte, cuenta y configuración. Las reglas no prueban que un backup se ejecute, su destino ni su retención. Debe probarse en AVD aislado.

## Coherencia de privacidad y Data Safety

La política HTML preparada para Orivolt es **coherente, con pendientes explícitos**, respecto de: inventario local, URI y selectores, widgets de terceros, metadatos mínimos de notificación, accesibilidad sin contenido de ventana, biometría delegada y exclusiones de Hidden Apps. También reconoce que Android/proveedor controla la ejecución y retención de backup general.

No es consistente todavía afirmar en Play “no se recopilan/no se comparten datos”, “cifrado en tránsito” o ausencia total de tráfico. Esas respuestas siguen **PENDIENTES** de la prueba Release y de resolver el significado operativo de backup para Data Safety. La política HTML aún contiene marcadores de titular, correo y fecha, y la URL in-app es histórica; son pendientes de publicación, no un bloqueo para las pruebas dinámicas.

`docs/PRIVACY_POLICY.md` contiene afirmaciones absolutas incompatibles con la cautela requerida (por ejemplo, transmisión inexistente y Data Safety sin recopilación/compartición). Es un riesgo documental si se publica o se toma como declaración vigente; esta revisión no lo modifica.

## Prueba reproducible de red: AVD aislado

Objetivo: atribuir conexiones al UID/proceso de Orivolt durante los flujos Release, no sólo observar una pantalla vacía del inspector.

1. Crear un AVD nuevo y desechable (misma API objetivo o superior), sin cuenta personal, sin SIM y sin datos personales. Anotar imagen/API, huella/configuración, versión de Orivolt y SHA-256 del AAB/APK instalado. Usar una red de laboratorio dedicada o una red del host con proxy/captura controlados.
2. Instalar únicamente el artefacto Release candidato mediante la ruta de prueba autorizada. Identificar antes el UID de `com.daybreak.animelauncher` con `adb shell dumpsys package com.daybreak.animelauncher` y registrar paquetes/procesos auxiliares presentes. No usar el POCO.
3. Capturar simultáneamente: (a) paquetes/flujo por UID en el emulador o VPN de captura de prueba, (b) DNS y conexiones TCP/TLS del host/proxy, y (c) logcat filtrado por proceso. Registrar el inicio/fin exactos de cada caso. Si se usa MITM, instalar sólo un CA de laboratorio en el AVD y no iniciar sesión en servicios personales.
4. Con estado limpio del AVD, ejecutar y marcar por separado: primer inicio/onboarding; establecer HOME; abrir/cerrar cajón y búsqueda; navegar HOME; abrir/cerrar Drawer; Settings y cada cambio de widget/RAM/apariencia; Apps ocultas sin y con autenticación de prueba; reiniciar proceso y actividad.
5. Repetir después de habilitar explícitamente Notification Listener y Accessibility. Generar únicamente notificaciones de prueba sin texto sensible. Confirmar también deshabilitar ambos permisos y repetir el intervalo de reposo.
6. Probar una imagen y un vídeo que residan localmente en el AVD. Repetir con un proveedor de documentos de prueba separado, etiquetando su UID. Agregar y retirar un widget de prueba de una app conocida; cualquier tráfico del proveedor se registra por separado, no como tráfico Orivolt. No abrir la URL de privacidad dentro de la corrida base; si se prueba, atribuirla al navegador.
7. Para cada caso, conservar un registro saneado: marca temporal, UID/proceso, destino/IP/SNI/DNS cuando esté disponible, protocolo, bytes, acción de UI y explicación. Un resultado sin conexión atribuible al UID de Orivolt sólo permite “no observado bajo esta cobertura y estos flujos”.

**Límite del Network Inspector:** una vista vacía no prueba ausencia total. Puede no cubrir tráfico de inicio temprano, tráfico nativo/dependencias, otros procesos/UID, DNS, QUIC, tráfico fuera de su ventana, TLS no descifrable, VPN/proxy, caché o conexiones de sistema. Por ello se requiere correlación con captura por UID y red del host; incluso así, una prueba finita no prueba ausencia universal.

## Prueba reproducible de backup/restauración: sólo emuladores

1. Crear dos AVDs nuevos y desechables del mismo nivel API: **A** (origen) y **B** (destino). Usar una cuenta/backup transport de prueba o la infraestructura de backup explícitamente documentada para ese AVD; nunca una cuenta, teléfono ni copia real. Guardar snapshots iniciales apagados para poder reiniciar el experimento.
2. Instalar la misma Release en A y crear datos exclusivamente sintéticos: preferencias de interfaz, una URI de archivo local de prueba, icono, widget de prueba, una entrada Hidden Apps y una credencial de prueba que no sea reutilizable. Registrar sólo nombres de archivo, presencia/ausencia y hashes/tamaños; no exfiltrar secretos ni contenido de notificaciones.
3. Solicitar/forzar el backup mediante el transporte de laboratorio compatible con la imagen (por ejemplo las herramientas `bmgr` sólo si el transporte confirma que soporta Auto Backup). Registrar transporte, cuenta de prueba, comandos/resultados, hora y paquete. Si el AVD no dispone de transporte funcional, el resultado correcto es **PENDIENTE**, no “sin backup”.
4. Restaurar exclusivamente en B, o restablecer A a su snapshot inicial y restaurar mediante el mismo transporte. Verificar: qué elementos de `launcher_prefs` volvieron; validez de la URI; estado/ID del widget; ausencia de `nova_hidden_apps_prefs.xml` y `nova_hidden_apps_vault.xml`; y que Apps ocultas no recupere configuración ni credencial. Verificar localmente que la clave Keystore no es migrada.
5. Repetir para transferencia dispositivo-a-dispositivo sólo si la imagen/transport de laboratorio lo soporta y registrarlo como flujo distinto. Restaurar los snapshots o eliminar sólo los AVDs al terminar; no tocar el POCO ni datos reales.

## Criterio de salida

Se puede iniciar ya la validación dinámica. Data Safety continúa **PENDIENTE** hasta que se documenten (1) todas las conexiones atribuibles a la UID/proceso Release o su no observación con límites, (2) los flujos opcionales de notificaciones/accesibilidad/medios/widgets, y (3) el resultado real del backup y restore general en AVD. Si aparece un destino no explicado, un SDK inesperado o restauración contraria a las exclusiones, actualizar primero esta matriz y la política antes de cualquier declaración de Play.
