# Orivolt Home — política de privacidad para revisión humana

**Estado:** PASS CON OBSERVACIONES

**Fase:** 08D-3

**Preparada:** [FECHA DE VIGENCIA PENDIENTE]
**Aplicación:** Orivolt Home (`com.daybreak.animelauncher`)

## Alcance y evidencia usada

Este borrador se preparó contra el código y la configuración presentes en el proyecto de publicación. La búsqueda dirigida no encontró un archivo identificable como «matriz de privacidad aprobada 08D» en los ámbitos autorizados. Por tanto, no se le atribuye a ese artefacto ninguna conclusión no verificable.

| Declaración incluida | Evidencia local |
| --- | --- |
| Inventario de aplicaciones para el cajón | `LauncherViewModel.kt` usa `PackageManager.queryIntentActivities` para el intent MAIN/LAUNCHER. |
| Preferencias, configuración y referencias URI | `LauncherViewModel.kt`, `LauncherScreen.kt`, `SettingsScreen.kt` y `AppDrawerScreen.kt` usan almacenamiento privado y selectores Android. |
| Widgets | `WidgetHostManager.kt` y `BIND_APPWIDGET` en el manifiesto. |
| Notificaciones opcionales | `NotificationMonitorService.kt`: cuenta paquetes únicos tras usar `packageName`, `category` e `isOngoing`; no consulta extras de notificación. |
| Accesibilidad opcional | `LauncherAccessibilityService.kt` y `accessibility_service_config.xml`: acciones globales; `canRetrieveWindowContent=false`; el manejador de eventos no procesa el evento. |
| Hidden Apps y seguridad | `HiddenAppsPreferencesStore.kt`, `HiddenAppsCredentialStore.kt` y `HiddenAppsMigrator.kt`: preferencias privadas aisladas, cifrado AES-GCM mediante Android Keystore y derivación PBKDF2 para credenciales propias. |
| Exclusiones de respaldo | `backup_rules.xml` y `data_extraction_rules.xml`: excluyen `nova_hidden_apps_prefs.xml` y `nova_hidden_apps_vault.xml` de los conjuntos aplicables. |

## Declaraciones deliberadamente pendientes

- No se afirma que Orivolt Home «no recopila» ni «no comparte» datos en sentido absoluto, ni se fija todavía la respuesta de Google Play Data Safety. Falta cerrar y registrar la verificación de tráfico en ejecución y la revisión de las dependencias empaquetadas.
- No se declara identidad legal, dirección postal, correo de privacidad ni fecha de vigencia: el HTML usa marcadores visibles.
- La política no promete que un respaldo general vaya a ocurrir ni que sea controlado por Orivolt Home. Android y el proveedor de respaldo determinan la elegibilidad efectiva, la ejecución, la cuenta y la conservación de esos respaldos.

## Archivo público preparado

`docs/orivolt-home-privacy-policy.html` contiene las versiones completas en español e inglés, con navegación por idioma, diseño adaptable, UTF-8 y sin recursos externos, scripts, inicio de sesión ni llamadas de red propias. Puede alojarse como página estática HTTPS pública y accesible sin autenticación.

Antes de publicarlo, sustituir exactamente estos marcadores en ambas versiones:

1. `[NOMBRE LEGAL O NOMBRE DEL TITULAR PENDIENTE]`
2. `[CORREO DE PRIVACIDAD PENDIENTE]`
3. `[FECHA DE VIGENCIA PENDIENTE]`

## Conexión posterior (no realizada en esta fase)

Cuando exista una URL HTTPS pública definitiva, sustituir únicamente el valor de `PRIVACY_POLICY_URL` en `app/src/main/java/com/daybreak/animelauncher/ui/screens/SettingsScreen.kt` (actualmente conserva la URL histórica de NovaLauncher) y registrar la misma URL en Google Play Console. Verificar entonces que la URL abre sin inicio de sesión, muestra Orivolt Home y expone los dos idiomas.
