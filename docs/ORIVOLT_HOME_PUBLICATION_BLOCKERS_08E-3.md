# Orivolt Home — bloqueos de publicación en Google Play (08E-3)

## Bloqueos que deben resolverse antes de enviar a revisión

| Bloqueo | Evidencia actual | Acción de cierre |
| --- | --- | --- |
| Derechos de activos de ficha | El expediente 08D-2 bloquea icono, capas adaptativas, pictogramas, fondos y vídeos por falta de prueba comercial. | Aportar cadena documental de derechos de cada archivo que se vaya a usar o reemplazarlo por un activo con licencia demostrable. No usar el icono actual como icono de Play mientras siga bloqueado. |
| Capturas auténticas y saneadas | Aún no hay capturas Release aprobadas. | Ejecutar el guion 08E-3 en dispositivo/perfil de prueba y revisar identidad, datos personales, recursos y funciones visibles. |
| Data Safety | La matriz 08D-4 y la revisión Release 08D-5B la declaran pendiente. | Completar y documentar pruebas Release de tráfico por UID y de backup/restauración en AVD; responder Data Safety sólo con la evidencia resultante. |
| Datos de cuenta de Play | No están documentados los campos comerciales y de contacto definitivos. | Completar correo de soporte, titular/contacto aplicable, distribución, precio, clasificación de contenido, anuncios y público objetivo; verificar que el desarrollador público sea DayBreak472. |
| Identidad de paquete | La Release usa `com.daybreak.animelauncher`, aunque la marca visible es Orivolt Home. | Confirmar que la ficha ya creada en Play Console usa exactamente ese package name. No cambiar `applicationId`, namespace, versionCode ni firma: un package nuevo no puede sustituir a una app ya creada ni recibir sus actualizaciones. |

## Riesgos que requieren comprobación final

- Confirmar visualmente que la interfaz capturada no muestra NovaLauncher, AnimeLauncher ni `nova_launcher_icon.png`.
- Mantener fuera del marketing y de cualquier futura distribución los fondos históricos, los 116 WebP sin clearance y los MP4 bloqueados.
- La AAB auditada excluye esos fondos/vídeos, pero aún contiene 24 pictogramas sin clearance; evitar mostrar deliberadamente esos recursos hasta resolver su procedencia.
- La URL de política configurada en la app es `https://mikies21-programmer.github.io/OrivoltHome-Privacy/`; comprobar su disponibilidad, titular y coherencia final antes de enviar la ficha.

## No son cambios autorizados en esta fase

No cambiar código de producción, Gradle, Manifest, permisos, accesibilidad, icono de launcher, `applicationId`, namespaces, versionCode ni firma. No generar recursos gráficos, no publicar, no crear commits y no hacer push.
