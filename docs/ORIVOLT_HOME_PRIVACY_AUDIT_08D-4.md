# Orivolt Home — auditoría final de privacidad 08D-4

**Resultado: PASS CON OBSERVACIONES.** La implementación inspeccionada permite documentar con precisión el tratamiento local y las protecciones de Hidden Apps. No permite todavía una afirmación absoluta de “sin recopilación”, “sin compartición” o “sin transmisión”: faltan la inspección de la variante Release empaquetada y pruebas de tráfico en dispositivo. No se modificó código, Manifest, Gradle ni ningún recurso.

## Alcance y límite de evidencia

Revisión estática del árbol de trabajo actual, `app/src/main/AndroidManifest.xml`, fuentes Kotlin, reglas de respaldo, dependencias declaradas y el modelo de librerías disponible para **debug**. No se leyó ningún valor de preferencias ni contenido de usuario. El árbol ya tenía cambios sin relación con esta auditoría; se preservaron.

Una búsqueda estática no demuestra que una app o dependencia no transmita datos. En consecuencia, las conclusiones de red son condicionadas hasta la validación dinámica descrita en `ORIVOLT_HOME_PRIVACY_DYNAMIC_VALIDATION_08D-4.md`.

## Permisos, componentes y uso efectivo

| Área | Declarado / componente | Uso verificado | Dato tratado y destino | Conclusión |
| --- | --- | --- | --- | --- |
| Inventario de apps | `<queries>` MAIN/LAUNCHER | `LauncherViewModel.getInstalledApps()` consulta `PackageManager.queryIntentActivities`; se actualiza al cambiar paquetes. | Etiqueta, paquete, actividad e icono; memoria y estado privado para cajón/categorías/ocultas. | Función real. No hay `QUERY_ALL_PACKAGES`. |
| Notificaciones | Servicio exportado con `BIND_NOTIFICATION_LISTENER_SERVICE`; consentimiento en Ajustes Android. | `NotificationMonitorService` lee notificaciones activas, usa paquete, categoría y `isOngoing`, y mantiene un contador de paquetes únicos en `StateFlow`. | Metadatos de notificación en memoria. No hay acceso del código a `extras`, texto, remitente o adjuntos. | Función opcional real; requiere prueba dinámica de alcance OEM. |
| Accesibilidad | Servicio exportado con `BIND_ACCESSIBILITY_SERVICE`; opt-in de Android. | Acciones globales: bloquear pantalla y abrir panel de notificaciones. `onAccessibilityEvent` no procesa el evento. | No se recupera contenido de ventanas: XML establece `canRetrieveWindowContent=false`; no hay persistencia. | Función opcional real. |
| Hidden Apps | Sin permiso especial. | Guarda conjunto de identificadores de paquete y modo de autenticación; elimina paquetes ya desinstalados. | `nova_hidden_apps_prefs`: paquetes y configuración. Credencial propia: registro cifrado en `nova_hidden_apps_vault`. | Local; detalle de seguridad abajo. |
| Widgets | `BIND_APPWIDGET`; host propio. | Enumera proveedores, carga preview/icono, enlaza/configura widgets que el usuario elige y guarda IDs en estado. | Metadatos/ID del widget y contenido renderizado por su proveedor. | El proveedor puede tener red y tratamiento propios; no es SDK de Orivolt. |
| Preferencias | Sin permiso. | `launcher_prefs` serializa el estado: apariencia, estructura, accesos, categorías, IDs de widgets y URIs de medios/iconos. | Almacenamiento privado (`MODE_PRIVATE`). | Generalmente restaurable por Android. |
| RAM | Sin permiso. | `ActivityManager.getMemoryInfo` muestra RAM global del sistema. | Totales/disponible/low-memory, en memoria. | No lee procesos, apps ni `/proc`. |
| Fondos e imágenes personales | `OpenDocument` / `PickVisualMedia`; no se declaran permisos de almacenamiento/fotos. | Conserva URI y solicita permiso persistible de lectura; el validador permite sólo `content`, `android.resource` y `file:///android_asset/`. | Referencia URI; contenido se lee localmente desde el proveedor al renderizar/analizar. | No hay permiso general. Una URI puede referir a un proveedor de documentos/nube elegido por la persona. |
| Desinstalación de apps | `REQUEST_DELETE_PACKAGES`. | Lanza `ACTION_UNINSTALL_PACKAGE`, que Android confirma/restringe. | Identificador del paquete dirigido a Android. | Acción explícita del usuario; no borra paquetes por sí misma. |
| Fondo de pantalla | `SET_WALLPAPER`. | Declarado; no se observó en el flujo auditado un uso que copie medios fuera de la sandbox. | Pendiente de prueba de flujo, si se expone en la UI candidata. | No usar para inferir red. |
| Internet | `INTERNET`. | No se hallaron clientes HTTP, `WebView`, URLConnection, Retrofit, Firebase, analítica ni telemetría en fuentes propias. | Permiso disponible; no demuestra inactividad de dependencias. | Pendiente de validación dinámica. |

## Almacenamiento, seguridad y eliminación

- `launcher_prefs` usa almacenamiento privado y Gson. Incluye configuración general, referencias URI y datos de diseño; `saveState` fuerza `hiddenAppsConfig` vacío para aislarla.
- `nova_hidden_apps_prefs.xml` contiene configuración y paquetes ocultos. `nova_hidden_apps_vault.xml` contiene IV y ciphertext, no la credencial en claro. La clave AES-256 GCM se genera como no exportable en Android Keystore; el registro se deriva con PBKDF2 (120 000 iteraciones; fallback SHA-1 sólo API 24–25).
- `backup_rules.xml` (API 30-) y `data_extraction_rules.xml` (API 31+) excluyen ambos archivos de Hidden Apps de cloud backup y device transfer. `allowBackup=true` deja elegibles los datos generales por decisión de Android/proveedor: no se puede asegurar que un respaldo ocurra ni su retención.
- Restablecer Hidden Apps borra preferencias de su módulo, vault y clave Keystore, y vacía los paquetes. Quitar un widget borra su ID de host. Cambiar configuración sobrescribe el estado. La desinstalación/borrado de datos se rige por Android; la posible conservación de copias ya efectuadas depende del proveedor de backup.

## Dependencias y comunicaciones

Dependencias directas: AndroidX/Compose, Navigation, Biometric, Coil 2.6.0, Gson 2.10.1, ProfileInstaller 1.4.1, Media3 1.4.1 y Palette. El modelo de artefactos debug además resuelve, entre otras transitivas, `io.coil-kt:coil`, `com.squareup.okhttp3:okhttp:4.12.0`, Okio, Guava y módulos AndroidX/Media3. No aparecen Firebase, Crashlytics, Google Analytics, Sentry, Amplitude, Mixpanel ni SDK publicitario.

Coil incorpora OkHttp como capacidad de carga remota; aquí los puntos de carga de medios pasan por `LauncherMediaUriValidator`, que rechaza HTTP/HTTPS/FTP. Media3 también puede reproducir URIs remotas en general, pero el gestor de vídeo aplica el mismo validador. Esto reduce el riesgo del flujo propio, pero no sustituye la inspección del AAB/Release ni la observación de red. Los widgets y el selector de documentos son procesos/proveedores de terceros iniciados por elección del usuario; sus comunicaciones no son comunicaciones automáticas de Orivolt.

La configuración `release` activa minificación y reducción de recursos; no declara un SDK de analítica ni una regla que habilite red. No se construyó ni firmó Release para esta auditoría y el inventario transitorio disponible es debug, por lo que el contenido final de Release sigue pendiente de comprobar.

## Política HTML: contraste

`orivolt-home-privacy-policy.html` está respaldada para: inventario de launcher, preferencias/URI, RAM, widgets como componentes de su proveedor, metadatos limitados de notificaciones, accesibilidad sin contenido de ventana, diseño criptográfico de Hidden Apps, exclusiones de backup y eliminación/restablecimiento local.

Mantener como pendientes —y no endurecer a una promesa absoluta— sus avisos actuales de tráfico, dependencias y Data Safety. La frase que describe medios como referencias URI y acceso sin permiso general está respaldada; debe conservar el matiz de que un proveedor de documentos/nube elegido por el usuario puede comunicarse por su cuenta. Antes de publicación faltan titular legal, correo de privacidad, fecha de vigencia y URL HTTPS definitiva; la URL activa de la app no se modificó y aún apunta a la histórica de NovaLauncher.

## Cierre para Google Play Console

No completar todavía las casillas que dependan de “colección”, “compartición”, “cifrado en tránsito” o destinatarios externos. Falta exactamente: (1) generar/inspeccionar el AAB Release candidato y su manifiesto/dependencias fusionadas, (2) ejecutar la matriz de tráfico y flujos privilegiados en dispositivo, (3) decidir y registrar el efecto de Android Backup sobre preferencias generales según las respuestas que exige Play, y (4) publicar una política HTTPS con sus marcadores reemplazados y actualizar la URL cuando exista autorización.

Véanse `ORIVOLT_HOME_DATA_SAFETY_EVIDENCE_08D-4.md` y `ORIVOLT_HOME_PRIVACY_DYNAMIC_VALIDATION_08D-4.md`.
