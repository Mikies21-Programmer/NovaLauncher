# Orivolt Home — lista de verificaciones pendientes 08D-4

Estado: **pendiente antes de finalizar Data Safety en Google Play Console.** El procedimiento reproducible y criterios de aprobación están en `ORIVOLT_HOME_PRIVACY_DYNAMIC_VALIDATION_08D-4.md`.

- [ ] Generar e inspeccionar el AAB/APK **Release** candidato: manifiesto fusionado, permisos, componentes y dependencias empaquetadas.
- [ ] Confirmar si la Release incluye algún SDK o capacidad de comunicación no visible en el modelo debug revisado.
- [ ] Capturar tráfico atribuible a la UID/proceso de Orivolt durante primer inicio y flujos autónomos; documentar limitaciones de la captura.
- [ ] Repetir la captura con Notification Listener y Accessibility habilitados mediante datos de prueba.
- [ ] Probar selección de medio local y de proveedor de documentos; atribuir correctamente tráfico del proveedor frente al de Orivolt.
- [ ] Añadir y eliminar un widget de prueba; separar cualquier comunicación del proceso proveedor.
- [ ] Verificar que URIs HTTP/HTTPS/FTP no se cargan desde los flujos de medios del launcher.
- [ ] Verificar en hardware que notificaciones no exponen texto/remitente/adjuntos y que accesibilidad no recupera contenido de ventana.
- [ ] Verificar almacenes de Hidden Apps, exclusión de backup/cloud/device transfer y restablecimiento de vault/clave con una credencial de prueba.
- [ ] Ejecutar backup/restore o transferencia de dispositivo autorizados para establecer qué ocurre con `launcher_prefs`, URIs y Hidden Apps.
- [ ] Comprobar limpieza local tras borrar datos/desinstalar en dispositivo de prueba; documentar la retención que dependa del proveedor de backup.
- [ ] Con los resultados, completar sólo las respuestas Data Safety respaldadas y actualizar la matriz si se observa una transmisión.
- [ ] Antes de publicar, completar titular legal, correo, fecha y URL HTTPS pública de la política; no cambiar la URL activa sin esa publicación.
