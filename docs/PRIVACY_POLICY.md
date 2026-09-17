# Política de Privacidad de NovaLauncher

**Última actualización:** 17 de septiembre de 2026  
**Versión de la aplicación:** 1.4  
**Identificador de paquete (Package ID):** `com.daybreak.animelauncher`

---

## 1. Identificación y Filosofía de la Aplicación

NovaLauncher es una aplicación de pantalla de inicio (lanzador o *launcher*) para el sistema operativo Android, diseñada para ofrecer una interfaz personalizada y fluida con estética Cyberpunk / Anime.

La privacidad del usuario es un principio fundamental del diseño de NovaLauncher. Esta política describe de manera transparente qué datos o recursos del sistema son accedidos, procesados o almacenados localmente por la aplicación, y confirma que **ningún dato personal ni de uso es transmitido fuera de su dispositivo**.

---

## 2. Naturaleza del Procesamiento: Funcionamiento Local (Offline-First)

NovaLauncher opera como una aplicación local e independiente:
- **Sin cuentas de usuario:** No se requiere ni existe registro, inicio de sesión, contraseñas ni vinculación con cuentas externas.
- **Sin servidores remotos:** NovaLauncher no mantiene servidores propios, bases de datos en la nube ni servicios de sincronización remota.
- **Diferenciación de tratamiento:**
  - **Acceso local:** La aplicación interactúa con APIs del sistema Android en su propio dispositivo para proporcionar las funciones que usted solicita.
  - **Procesamiento local:** Las operaciones lógicas (como contar notificaciones activas o interpretar gestos) se ejecutan exclusivamente en la memoria de su dispositivo.
  - **Almacenamiento local:** Las configuraciones de diseño se guardan en el almacenamiento privado de la aplicación en su dispositivo.
  - **Transmisión fuera del dispositivo:** **Inexistente**. La aplicación no envía información a servidores externos ni a terceros.
  - **Recolección en Data Safety (Google Play):** Conforme a las directrices de Google Play, dado que los datos no se transmiten fuera del dispositivo ni se almacenan de forma persistente para fines de seguimiento, NovaLauncher se clasifica como una aplicación que **no recopila ni comparte datos de usuario**.

---

## 3. Servicio de Accesibilidad (AccessibilityService)

NovaLauncher incluye la capacidad de utilizar la API `AccessibilityService` de Android bajo el componente `LauncherAccessibilityService`.

### Finalidad exclusiva y opcional
El uso de este servicio es **completamente opcional** y tiene como único fin permitir la ejecución de gestos de navegación y conveniencia iniciados explícitamente por el usuario desde la pantalla de inicio:
- **Doble toque en área vacía:** Ejecuta la acción global del sistema para apagar y bloquear la pantalla (`GLOBAL_ACTION_LOCK_SCREEN`).
- **Deslizar hacia abajo:** Ejecuta la acción global del sistema para desplegar el panel de notificaciones (`GLOBAL_ACTION_NOTIFICATIONS`).
- **Toque sobre el contador de mensajes:** Despliega el panel de notificaciones del sistema (`GLOBAL_ACTION_NOTIFICATIONS`).

### Garantías técnicas y de privacidad
- **Sin lectura de pantalla:** En la configuración del servicio se establece formalmente `android:canRetrieveWindowContent="false"`. NovaLauncher **no inspecciona**, **no lee** y **no tiene acceso** al árbol de vistas, ventanas o contenido visual de otras aplicaciones ni del sistema operativo.
- **Sin captura de texto ni pulsaciones:** El servicio no recopila texto escrito por el usuario, credenciales ni eventos de teclado.
- **Sin almacenamiento de eventos:** El método de recepción de eventos de accesibilidad (`onAccessibilityEvent`) no procesa ni registra información.
- **Sin transmisión remota:** Ningún dato relacionado con la accesibilidad se guarda en disco ni se envía a través de la red.
- **Divulgación destacada y consentimiento previo:** NovaLauncher presenta un diálogo in-app de divulgación destacada antes de dirigir al usuario a la pantalla de configuración de accesibilidad de Android, requiriendo el consentimiento afirmativo del usuario para su habilitación.

---

## 4. Acceso a Notificaciones (NotificationListenerService)

NovaLauncher incluye el servicio `NotificationMonitorService` utilizando el permiso de sistema `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`.

### Finalidad exclusiva
Este servicio se utiliza únicamente para mostrar un indicador numérico en la pantalla principal que refleja la cantidad de aplicaciones de comunicación que tienen mensajes o notificaciones no leídas activas.

### Tratamiento de la información
- **Metadatos mínimos inspeccionados:** La aplicación únicamente comprueba:
  1. El nombre del paquete (`packageName`) de la aplicación que originó la notificación para determinar si pertenece a aplicaciones de mensajería conocidas (por ejemplo, WhatsApp, Telegram, SMS o correo electrónico).
  2. La categoría de sistema de la notificación (`category`) para admitir mensajes o correos, y descartar estrictamente llamadas activas (`CATEGORY_CALL`) o llamadas perdidas (`CATEGORY_MISSED_CALL`).
  3. El estado de la notificación (`isOngoing`) para ignorar procesos continuos como descargas de archivos o reproductores multimedia.
- **Sin acceso al contenido:** NovaLauncher **no lee**, **no procesa** ni almacena el contenido textual del mensaje, el asunto, el nombre del remitente ni los archivos adjuntos.
- **Procesamiento efímero en memoria RAM:** El cálculo se realiza mediante conteo en memoria volátil deduplicando aplicaciones únicas. No se guarda un registro histórico ni se persiste ningún dato de las notificaciones en archivos ni bases de datos.
- **Sin transmisión:** Los datos de las notificaciones nunca salen de la memoria temporal de su dispositivo.

---

## 5. Fotos, Videos e Iconos de la Galería

NovaLauncher permite personalizar el aspecto visual de las pantallas (fondos de pantalla estáticos, fondos de video dinámicos e iconos personalizados).

### Mecanismo de selección (Storage Access Framework - SAF)
- **Selectores nativos del sistema:** La selección de archivos se realiza exclusivamente a través de los componentes del sistema Android (`OpenDocument` y `PickVisualMedia`).
- **Sin acceso general al almacenamiento:** NovaLauncher no solicita permisos invasivos ni generales de almacenamiento (como `READ_EXTERNAL_STORAGE` ni `MANAGE_EXTERNAL_STORAGE`).
- **Uso de referencias URI locales:** La aplicación conserva únicamente el identificador local (`Uri`) otorgado por el sistema con permisos persistentes de lectura (`takePersistableUriPermission`) para poder decodificar y mostrar el fondo o icono elegido en su pantalla de inicio.
- **Privacidad de los medios:** Las imágenes, videos y gráficos seleccionados por usted permanecen en su ubicación original en su dispositivo y **nunca son transferidos ni cargados a servidores externos**.

---

## 6. Widgets del Sistema

NovaLauncher incluye compatibilidad para alojar widgets nativos desarrollados por otras aplicaciones que usted tenga instaladas en su dispositivo (`BIND_APPWIDGET`).

- **Alojamiento local:** El lanzador actúa únicamente como contenedor visual (anfitrión de widgets) según lo previsto por el marco de trabajo de Android.
- **Datos de los widgets:** Los datos, contenidos o interacciones que se visualizan dentro de cada widget son generados y administrados directamente por la aplicación proveedora de dicho widget, no por NovaLauncher. NovaLauncher no intercepta, no almacena ni transmite el contenido interno procesado por dichos widgets.

---

## 7. Fondo de Pantalla del Sistema

Cuando usted configura un fondo visual en NovaLauncher, la aplicación puede interactuar con el administrador de fondos de Android (`WallpaperManager`) utilizando el permiso `SET_WALLPAPER`. Esta interacción se realiza de forma estrictamente local para sincronizar la vista previa del sistema en pantallas del sistema como el selector de tareas recientes (multitarea).

---

## 8. Conectividad, Red y Servicios Externos

De acuerdo con la auditoría técnica del código fuente de NovaLauncher:
- **Sin servidores propios:** No existen servidores, APIs REST, servicios web ni infraestructura de backend conectados a la aplicación.
- **Sin servicios de analítica:** No se utilizan herramientas de análisis de uso o telemetría (no se incluye Google Analytics, Firebase Analytics, Flurry, Mixpanel ni similares).
- **Sin servicios de reporte de fallos:** No se incluyen SDKs de crash reporting como Firebase Crashlytics, Sentry o Bugsnag.
- **Sin publicidad ni monetización:** La aplicación no incluye bibliotecas de anuncios (AdMob, Unity Ads, etc.) ni rastreadores publicitarios.
- **Librerías externas utilizadas:** Las dependencias de terceros presentes en la aplicación (Jetpack Compose, Coil, Gson y AndroidX Media3 ExoPlayer) se emplean exclusivamente para renderizado gráfico, serialización local en almacenamiento privado y reproducción local de video sin emitir telemetría externa.

---

## 9. Datos y Permisos no Tratados

Para mayor claridad y transparencia, NovaLauncher **NO** solicita, no accede ni procesa:
- **Ubicación:** No se solicitan permisos de GPS, red o ubicación en segundo plano (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`).
- **Contactos y Libreta de direcciones:** No se solicita acceso a contactos (`READ_CONTACTS`).
- **Cámara y Micrófono:** No se solicitan permisos de hardware para capturar imágenes o grabar audio (`CAMERA`, `RECORD_AUDIO`). Cualquier acceso directo a la cámara desde el escritorio se limita a abrir la aplicación de cámara instalada en el sistema mediante un Intent estándar.
- **Identificadores publicitarios y de hardware:** No se recopilan ni leen identificadores únicos como el Identificador de Publicidad de Google (AAID), `ANDROID_ID` ni números de serie del dispositivo.
- **Datos financieros o de pago:** No se solicitan datos bancarios, tarjetas ni se integran pasarelas de pago dentro de la aplicación.

---

## 10. Seguridad y Protección de la Información

NovaLauncher implementa medidas técnicas basadas en el modelo de seguridad de Android:
- **Principio de mínimo privilegio:** La aplicación únicamente utiliza los permisos indispensables para operar como lanzador de inicio.
- **Almacenamiento privado:** Todas las preferencias de personalización del usuario (colores, citas, orden de iconos) se guardan en el almacenamiento protegido de la aplicación (`SharedPreferences` en modo privado), inaccesible para otras aplicaciones estándar del dispositivo.
- **Aislamiento de procesos:** La ejecución se mantiene dentro de la sandbox segura provista por el sistema operativo Android.

---

## 11. Cambios a esta Política de Privacidad

Podemos actualizar esta Política de Privacidad periódicamente si se modifican las funciones de la aplicación o los requisitos normativos y de la plataforma. Cualquier cambio sustancial será documentado indicando la nueva fecha de actualización al inicio de este documento y, de ser necesario, se notificará a través de las notas de la versión en Google Play Store o en los ajustes de la aplicación.

---

## 12. Contacto

Si tiene preguntas, inquietudes o comentarios sobre esta Política de Privacidad o las prácticas de NovaLauncher, puede ponerse en contacto con el desarrollador a través de:

**Correo electrónico:** `[CORREO DE CONTACTO DEL DESARROLLADOR]`  
**Aplicación:** NovaLauncher (`com.daybreak.animelauncher`)
