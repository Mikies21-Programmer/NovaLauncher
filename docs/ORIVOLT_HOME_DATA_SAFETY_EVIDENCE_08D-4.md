# Orivolt Home — matriz de evidencia Data Safety 08D-4

Estado de la matriz: **no apta aún para enviar respuestas finales a Play**. “No observado” describe sólo código y artefactos estáticos revisados; nunca equivale a ausencia probada de transmisión.

| Tipo de dato / elemento | Finalidad | Evidencia | Local / posible salida | Protección y eliminación | Declaración actual |
| --- | --- | --- | --- | --- | --- |
| Apps instaladas: etiqueta, paquete, actividad, icono | Cajón, búsqueda, accesos, categorías y ocultación | `LauncherViewModel.kt:getInstalledApps` | En memoria; algunos paquetes/categorías persisten en preferencias. Backup general posible. | Sandbox Android; Hidden Apps excluido de backup; reset limpia Hidden Apps. | Uso local confirmado; transmisión/Play pendiente. |
| Selección de app oculta y modo de seguridad | Ocultar apps y proteger pantalla | `HiddenAppsPreferencesStore.kt` | Privado, local; excluido de backup y transferencia. | `MODE_PRIVATE`; restablecimiento borra. | Confirmado local/no diseñado para backup; red pendiente general. |
| PIN/patrón/contraseña propios: hash, salt, modo, ciphertext/IV | Verificación de Hidden Apps | `HiddenAppsCrypto.kt`, `HiddenAppsCredentialStore.kt` | Privado, local; vault excluido de backup. | PBKDF2, AES-GCM, clave Android Keystore no exportable; reset borra vault y clave. | Confirmado. No declarar que se almacena la credencial en claro. |
| Resultado biométrico/credencial del dispositivo | Desbloquear Hidden Apps | `HiddenAppsAuthenticator.kt` y AndroidX Biometric | Resultado de sistema en flujo de ejecución; no plantillas biométricas observadas. | Controlado por Android. | Confirmado con prueba dinámica pendiente. |
| Metadatos de notificación: paquete, categoría, continua | Contar apps de mensajería con notificación activa | `NotificationMonitorService.kt` | Memoria mientras el servicio funciona; no persistencia observada. | Servicio sólo si usuario lo habilita; contador se reinicia. | Confirmado; validar en hardware. |
| Contenido de notificaciones | No es finalidad | No hay lectura de `extras`, texto, remitente o adjuntos en el servicio | No observado | N/A | No declarar tratamiento de contenido; prueba dinámica recomendada. |
| Datos de accesibilidad | Acciones globales del launcher | `LauncherAccessibilityService.kt`, `accessibility_service_config.xml` | No persistencia ni procesado de eventos observado. | `canRetrieveWindowContent=false`. | Confirmado estáticamente; validar permisos/servicio en hardware. |
| URI y contenido de foto/vídeo/icono elegido | Fondo, tema, vídeo, icono personalizado | OpenDocument/PickVisualMedia; `LauncherMediaUriValidator.kt` | URI se persiste; contenido local vía proveedor. Android Backup de la URI/configuración general posible. El proveedor cloud elegido podría transferir contenido. | Permiso URI de lectura persistible; no permisos generales de fotos/archivos. | Uso local y origen iniciado por usuario confirmados; clasificación Play/backup pendiente. |
| Preferencias de launcher, diseño, accesos, IDs de widget | Personalización y restauración | `LauncherViewModel.kt:saveState` | `launcher_prefs`; elegible para backup Android según plataforma. | Sandbox; modificación/sobrescritura por configuración; borrar datos/desinstalar sujetos a Android. | Confirmado; retención/transferencia de backup pendiente. |
| Proveedores y contenido de widgets | Alojar widget seleccionado | `WidgetHostManager.kt` | IDs/previews locales; el widget proveedor puede comunicar datos por sí mismo. | Android y app proveedora controlan contenido; borrar widget borra ID host. | No atribuir prácticas del proveedor a Orivolt; verificar integración. |
| RAM global | Mostrar indicador RAM | `ram/RamMonitor.kt` | Memoria de proceso; no persistencia observada. | N/A | Confirmado local. |
| Datos de diagnóstico, analítica, identificadores publicitarios | Ninguna finalidad hallada | Fuentes propias y dependencias declaradas: sin Firebase/analytics/ads | No observado; `INTERNET` y OkHttp transitivo existen. | N/A | **Pendiente de tráfico Release; no marcar “no recopila” aún.** |

## Comunicaciones: clasificación

| Canal | Iniciación | Receptor posible | Estado |
| --- | --- | --- | --- |
| Código propio HTTP/WebView/telemetría | Automática | N/A | No encontrado estáticamente. |
| Coil/OkHttp o Media3 | Podría ser automática si recibe URI remota | Host de URI | Dependencias tienen capacidad; validador propio bloquea esquemas remotos. Requiere Release + prueba. |
| Widget | Después de selección/vinculación del usuario | App proveedora/servicios de esta | Externa e iniciada por usuario; fuera del control de Orivolt. |
| Document provider/nube | Al elegir medio | Proveedor elegido | Externa e iniciada por usuario; URI puede persistir. |
| Navegador para política/enlace | Al pulsar enlace | Navegador/sitio externo | Externa e iniciada por usuario. La URL activa es histórica y no se tocó. |
| Android Backup / device transfer | Plataforma, configuración de cuenta y dispositivo | Android/proveedor de backup | Posible para estado general; Hidden Apps excluido. Debe resolverse para Data Safety. |

## Respuestas que pueden y no pueden cerrarse

- Confirmable: la app trata localmente los elementos de la matriz para las finalidades indicadas; Hidden Apps está excluido de las reglas de backup aplicables y sus credenciales propias disponen de los mecanismos criptográficos indicados.
- Pendiente: existencia de conexiones automáticas en la Release efectiva; si alguna preferencia general se transmite mediante backup y cómo Play exige declararlo; cualquier respuesta de “cifrado en tránsito”; destinatarios efectivos fuera de Android/proveedores elegidos; declaración final de colección/compartición.
