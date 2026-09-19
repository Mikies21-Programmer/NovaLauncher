# Regla Central de Operación — NovaLauncher

## 1. Identidad del Proyecto
* **Nombre de la Aplicación:** NovaLauncher (nombre visible en sistema: `NOVALAUNCHER`).
* **Application ID / Package Name:** `com.daybreak.animelauncher`
* **Namespace:** `com.daybreak.animelauncher`
* **Stack Tecnológico:** Android Nativo en Kotlin con Jetpack Compose (Material3, Navigation Compose, Coil Compose, AndroidX Media3 ExoPlayer).
* **Compilación:** `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`.

---

## 2. Principio de Cambio Controlado (Non-Negotiable)
1. **Auditar Primero:** Jamás modificar código ni configuraciones en fases de auditoría o investigación.
2. **Proponer y Acotar:** Presentar los hallazgos y proponer únicamente la solución mínima necesaria.
3. **Alcance Aprobado Estricto:** Implementar única y exclusivamente lo requerido por la fase activa.
4. **Prohibición de Cambios Oportunistas:** Nunca aprovechar una tarea acotada para realizar refactors, limpiezas de código, formateos masivos o "mejoras" no solicitadas.
5. **Protección de Funcionalidades Validadas:** Toda funcionalidad que haya sido verificada físicamente en el hardware real es intocable salvo instrucción explícita del desarrollador.

---

## 3. Flujo Operativo Estándar
Todo ciclo de trabajo en NovaLauncher debe seguir esta secuencia estricta:
```
1. Audit (Investigación estricta sin tocar código)
   ↓
2. Report (Presentación estructurada de hallazgos y propuesta)
   ↓
3. Implementation Prompt (Confirmación o instrucción del desarrollador)
   ↓
4. Controlled Implementation (Edición quirúrgica de archivos mínimos)
   ↓
5. Automated Validation (assembleDebug, test, lintDebug, assembleRelease / bundleRelease)
   ↓
6. Physical Validation by Developer (Prueba en dispositivo físico real, ej. POCO X6)
   ↓
7. Documentation Update (Actualización de docs y AGENT_PROJECT_STATE.md)
   ↓
8. Git Checkpoint by Developer (El desarrollador realiza commit/push a su discreción)
```

---

## 4. Responsabilidad y Validación Física
* **Las pruebas físicas en dispositivo real son responsabilidad del desarrollador.**
* El agente de IA **NO debe afirmar que una prueba física pasó (PASS)** a menos que:
  a) El desarrollador haya confirmado expresamente los resultados en el chat, o
  b) Haya evidencia verificable de ejecución (ej. comandos adb específicos con salidas registradas y validadas por el usuario).

---

## 5. Gestión de Git y Seguridad
* **Autonomía Restringida:** El agente **NO** ejecuta `git commit` ni `git push` en el repositorio de NovaLauncher salvo instrucción explícita y directa del usuario.
* **Inspección de Cambios:** Antes de finalizar cualquier tarea de implementación, el agente debe revisar `git diff` y reportar `git status`.
* **Cero Secretos en el Repositorio:**
  * Ningún almacén de claves (`.jks`, `.keystore`) debe ubicarse dentro del repositorio `AnimeLauncher`.
  * Nunca incluir contraseñas, alias con contraseñas, ni tokens en archivos rastreados por Git.
  * Respetar estrictamente las exclusiones de `.gitignore` (`*.jks`, `*.keystore`, `keystore.properties`, `*.pem`).
  * Nunca imprimir contraseñas en reportes, logs ni respuestas de chat.

---

## 6. Componentes Estrictamente Protegidos (STRICTLY PROTECTED)
Los siguientes archivos y componentes forman el núcleo estable de NovaLauncher y **NO deben modificarse** salvo orden explícita y con plan de regresión previo:

1. `VideoWallpaperManager.kt` (Gestión del ciclo de vida y decodificación de video en bucle).
2. `VideoBackground.kt` (Superficie Compose de renderizado de video wallpaper).
3. `WidgetHostManager.kt` (Ciclo de vida, binding y purge de widgets huérfanos de Android).
4. `NativeWidgetView.kt` (Alojamiento Compose de `AppWidgetHostView`).
5. `ViewOne.kt` (Escritorio principal 1 con soporte de widgets y barra de apps).
6. `ViewTwo.kt` (Escritorio secundario 2 con layout diagonal y fondo independiente).
7. `LauncherAccessibilityService.kt` (Servicio de accesibilidad para acciones globales del sistema).
8. `accessibility_service_config.xml` (Configuración de accesibilidad con `canRetrieveWindowContent="false"`).
9. `NotificationMonitorService.kt` (Servicio de escucha y conteo de notificaciones de mensajería).
10. `AccessibilityDisclosureDialog.kt` (Componente de divulgación destacada y consentimiento in-app).
11. `LauncherNavState.kt` (Máquina de estado y navegación central del launcher).

**Mecanismos y Comportamientos Adicionales Protegidos:**
* Navegación horizontal y transiciones entre View 1 y View 2.
* App Drawer (cajón de aplicaciones, búsqueda y categorías).
* Doble toque (*Double Tap*) para apagar pantalla (`GLOBAL_ACTION_LOCK_SCREEN`).
* Deslizar abajo (*Swipe Down*) para expandir notificaciones (`GLOBAL_ACTION_NOTIFICATIONS`).
* Exclusión de gestos táctiles en áreas de widgets (`widget gesture exclusion`).
* Configuración de pantalla completa de borde a borde (*Edge-to-Edge*).
* Estética y persistencia de Glass UI (GlassCards, desenfoques y paleta Cyberpunk).

---

## 7. Semántica Validada del Contador de Notificaciones
* **Regla Semántica:** El contador numérico en la interfaz **NO representa el total acumulado de mensajes** individuales.
* **Significado Real:** Representa el **número de aplicaciones únicas de mensajería que tienen al menos una notificación activa no continua**.
  * Ejemplo 1: 10 mensajes de WhatsApp = `1` en el contador.
  * Ejemplo 2: 1 mensaje de WhatsApp + 3 de Telegram = `2` en el contador.
  * Ejemplo 3: WhatsApp + Telegram + Gmail = `3` en el contador.
* **Filtros Estrictos Obligatorios:**
  * Excluir notificaciones continuas (`sbn.isOngoing == true`).
  * Excluir llamadas activas (`Notification.CATEGORY_CALL`).
  * Excluir llamadas perdidas (`Notification.CATEGORY_MISSED_CALL`).

---

## 8. Accesibilidad y Cumplimiento de Políticas
* **Propósito Exclusivo:** Apagar la pantalla (`LOCK_SCREEN`) y desplegar notificaciones (`NOTIFICATIONS`) a solicitud del usuario mediante gestos.
* **Aislamiento:**
  * `canRetrieveWindowContent = false`.
  * No lee contenido de pantalla de ninguna aplicación.
  * No registra eventos de teclado ni pulsaciones.
  * No almacena ni transmite datos de accesibilidad.
* **Divulgación Destacada (*Prominent Disclosure*):**
  * Toda invocación cuando el servicio está inactivo debe mostrar primero `AccessibilityDisclosureDialog`.
  * La redirección a `ACTION_ACCESSIBILITY_SETTINGS` solo ocurre tras consentimiento afirmativo ("Aceptar y configurar").
  * Si el usuario pulsa "Ahora no", se cancela sin salir de la app.

---

## 9. Privacidad, Datos y Ficha Pública
* **Procesamiento 100% Local:** Cero servidores propios, cero analítica (sin Firebase Analytics, Google Analytics ni Mixpanel), cero crash reporting externo (sin Crashlytics ni Sentry), cero SDKs de publicidad (sin AdMob) y cero pasarelas de pago (sin Play Billing).
* **Gestión de Medios:** Uso exclusivo de Storage Access Framework (SAF) con permisos locales persistentes de lectura (`FLAG_GRANT_READ_URI_PERMISSION`).
* **Política de Privacidad Pública Canónica:**  
  URL: `https://mikies21-programmer.github.io/NovaLauncher-Privacy/`  
  Repositorio: `Mikies21-Programmer/NovaLauncher-Privacy`
* **Contacto Oficial de Privacidad y Desarrollador:**  
  Correo: `dbreak472@gmail.com`

---

## 10. Play Store, Firma y Release
* **Play App Signing:** ACTIVO en Google Play Console (Google administra la App Signing Key).
* **Clave de Subida (Upload Key):**
  * La clave privada (`upload-keystore-v2.jks`) debe permanecer siempre **fuera del repositorio** (`C:\Users\migue\NovaLauncher-Keys\`).
  * Alias de subida oficial v2: `novalauncher-upload-v2` (RSA 4096 bits, SHA384withRSA).
  * Certificado público exportado para solicitud de reset: `upload_certificate-v2.pem`.
* **Generación de Artefactos de Release:**
  * APK Release: `.\gradlew.bat assembleRelease`
  * App Bundle Release: `.\gradlew.bat bundleRelease` (produce `app/build/outputs/bundle/release/app-release.aab`).
* **Verificaciones Previas a Subida:**
  * Validar siempre `applicationId`, `versionCode`, `versionName` y `targetSdk`.
  * Confirmar que el AAB esté firmado con la Upload Key registrada antes de cargarlo en Play Console.
