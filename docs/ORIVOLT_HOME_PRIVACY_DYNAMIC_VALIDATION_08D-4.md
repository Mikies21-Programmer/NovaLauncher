# Orivolt Home — verificaciones dinámicas pendientes 08D-4

Estas comprobaciones son obligatorias antes de cerrar Data Safety. Ejecutarlas sobre el AAB/APK **Release candidato** instalado en un dispositivo de prueba, sin usar preferencias ni archivos personales reales. Registrar fecha, versión/versionCode, hash del artefacto, dispositivo/API, red activa, resultado y captura/log saneado.

## 1. Integridad del candidato Release

1. Generar el artefacto Release conforme al flujo autorizado, sin cambiar dependencias ni firma para esta auditoría.
2. Inspeccionar el manifiesto fusionado y las dependencias de Release, no sólo debug. Confirmar los permisos/componentes de la matriz, ausencia/presencia de SDKs inesperados y que minify/resource shrink no alteró el comportamiento.
3. Extraer lista de dominios/URLs y clases de red del artefacto; conservar sólo el informe técnico, nunca secretos ni contenido de usuario.

**Pasa si:** inventario coincide con esta auditoría o se actualiza la matriz antes de Play. **Falla/bloquea si:** aparece SDK de analítica, anuncios, crash reporting o red no evaluado.

## 2. Tráfico de red

1. Preparar proxy de inspección o captura de red del dispositivo que cubra tráfico de la UID de Orivolt; anotar limitaciones de TLS/Android y no interceptar cuentas o contenido personal.
2. Con una red controlada, capturar: primer inicio limpio, onboarding, abrir/cerrar cajón, buscar apps, activar/desactivar RAM, cambiar preferencias, abrir/cerrar Hidden Apps, y reiniciar la app.
3. Repetir con listener de notificaciones y accesibilidad habilitados, usando notificaciones de prueba sin contenido sensible.
4. Seleccionar una imagen/vídeo local de prueba; después repetir usando un proveedor de documentos de prueba. Distinguir tráfico de Orivolt del proceso del proveedor.
5. Añadir/quitar un widget de prueba; atribuir cualquier red al UID/proceso del proveedor, no por simple coincidencia temporal.
6. Pulsar el enlace de privacidad; confirmar que la comunicación pertenece al navegador y que la URL se actualizará sólo al publicar la página definitiva.

**Pasa si:** no se detecta tráfico de UID/proceso Orivolt no explicado durante los flujos autónomos, o todo tráfico encontrado queda documentado y se refleja en Data Safety. **No concluir “sin tráfico” si la herramienta no cubre VPN, DNS, TLS o tráfico de sistema.**

## 3. Permisos y minimización

1. Revocar/otorgar Notification Listener y Accessibility desde Ajustes; confirmar que las funciones quedan inactivas sin opt-in y que no se muestra contenido de notificaciones ni ventanas de otras apps.
2. Con notificaciones de prueba, comprobar que el contador usa sólo el número de apps y que no persiste tras detener/reiniciar el servicio.
3. Seleccionar y retirar medios de prueba; comprobar que sólo se guardan/reusan URIs permitidas y que HTTP/HTTPS/FTP no se cargan.
4. Añadir/quitar widgets; verificar eliminación del ID del host y que los datos del proveedor no se copian a preferencias de Orivolt.

## 4. Almacenamiento, backup y borrado

1. Crear configuración de prueba que incluya una app oculta, una credencial de prueba, URI, icono y widget. Inspeccionar sólo nombres de archivo, claves/esquema y tamaño; no copiar valores/secretos.
2. Confirmar que Hidden Apps usa sus dos almacenes separados y que las reglas de backup del artefacto excluyen ambos para cloud backup y device transfer.
3. Ejecutar restablecimiento de Hidden Apps: confirmar que desaparecen su configuración, vault y clave Keystore; confirmar que las apps reaparecen.
4. Probar backup/restore o device transfer en un entorno de prueba autorizado. Determinar exactamente si `launcher_prefs` se restaura, si una URI persistida sigue válida y si Hidden Apps queda desactivada/no restaurada. Registrar el proveedor de backup y la versión Android.
5. Borrar datos o desinstalar la app de prueba y validar la limpieza local. Tratar la retención de backup como propiedad documentada por el proveedor, no como una garantía de la app.

## Decisión posterior

Sólo tras documentar los cuatro bloques se puede decidir la sección Data Safety. Si el backup general efectivamente sale del dispositivo, consultar la guía vigente de Play y declarar exactamente lo requerido para ese flujo; no inferir la respuesta desde esta auditoría. Publicar la política sólo cuando contenga titular, correo, fecha y URL HTTPS accesible sin autenticación.
