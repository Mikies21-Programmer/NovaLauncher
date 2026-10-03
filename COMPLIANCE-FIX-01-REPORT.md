# Reporte Técnico de Cumplimiento: COMPLIANCE-FIX-01
**NovaLauncher — Hardening de Google Play / Accesibilidad / Documentación**

- **Fecha:** 24 de septiembre de 2026
- **Baseline Git:** commit `eb09b71` (`release: prepare NovaLauncher 1.5`)
- **Versión de Aplicación:** `versionName = "1.5"`, `versionCode = 7`, `targetSdk = 36`
- **Estado Git:** Modificaciones de código completadas. Cero commits / cero pushes ejecutados (`NO COMMIT / NO PUSH`).

---

## 1. Archivos Modificados

Se intervinieron de forma quirúrgica exactamente 7 archivos en el repositorio:

1. `app/src/main/java/com/daybreak/animelauncher/ui/screens/LauncherScreen.kt`
2. `app/src/main/res/xml/accessibility_service_config.xml`
3. `app/src/main/java/com/daybreak/animelauncher/LauncherAccessibilityService.kt`
4. `app/src/main/res/values/strings.xml`
5. `app/src/main/res/values-en/strings.xml`
6. `docs/PRIVACY_POLICY.md`
7. `website/backend/templates/politicas_de_privacidad.html`

Ningún otro archivo del repositorio fue modificado.

---

## 2. Cambios Exactos Realizados

### 2.1 Eliminación de Bypass de Prominent Disclosure (B1)
- **Archivo:** `app/src/main/java/com/daybreak/animelauncher/ui/screens/LauncherScreen.kt`
- **Cambio:** En la función `openNotificationsWithFallback()`, dentro de la rama `else` donde `disclosureAction == null`, se eliminó completamente el bloque `try/catch` que invocaba `Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)`.
- **Comportamiento Resultante:** Únicamente se despliega el `Toast` informativo informando que el servicio es requerido y se retorna. Ya no existe ruta que lance la configuración del sistema sin que previamente se haya desplegado y aceptado el `AccessibilityDisclosureDialog`.

### 2.2 Eliminación de Capacidad no Utilizada en Accesibilidad
- **Archivo:** `app/src/main/res/xml/accessibility_service_config.xml`
- **Cambio:**
  - `android:canPerformGestures="true"` cambiado a `android:canPerformGestures="false"`.
  - `android:canRetrieveWindowContent="false"` se mantuvo estrictamente intacto.

### 2.3 Eliminación de Dead Code en Companion Object
- **Archivo:** `app/src/main/java/com/daybreak/animelauncher/LauncherAccessibilityService.kt`
- **Cambio:** Se eliminaron las funciones no utilizadas del `companion object`:
  - `goBack()`
  - `openRecents()`
  - `goHome()`
- **Preservados intactos:**
  - `lockScreen()`
  - `openNotifications()`
  - Ciclo de vida del servicio (`onAccessibilityEvent`, `onServiceConnected`, `onUnbind`, `onDestroy`) y variable `instance`.

### 2.4 Actualización de Textos de Prominent Disclosure
- **Archivos:**
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
- **Cambio:** Se actualizó exclusivamente la cadena `accessibility_disclosure_message` para describir explícitamente las tres rutas reales de uso:
  - Doble toque: bloquear la pantalla.
  - Deslizar hacia abajo: abrir el panel de notificaciones.
  - Tocar el contador de mensajes: abrir el panel de notificaciones.
- **Garantías Técnicas Conservadas Explícitamente:**
  - No lee contenido de otras aplicaciones.
  - No extrae texto de pantalla.
  - No registra ni transmite contenido de eventos.
  - Función opcional.
  - Aceptación obligatoria del usuario antes de configurar en ajustes.
- Título, botones y apariencia del diálogo permanecen intactos.

### 2.5 Sincronización de Política de Privacidad (Documentación Interna)
- **Archivo:** `docs/PRIVACY_POLICY.md`
- **Cambios:**
  - Versión de la aplicación actualizada de `1.4` a `1.5`.
  - Placeholder `[CORREO DE CONTACTO DEL DESARROLLADOR]` reemplazado por `dbreak472@gmail.com`.
  - Sección 8 de dependencias externas actualizada para incluir explícitamente `androidx.palette:palette-ktx` y `androidx.navigation:navigation-compose`.

### 2.6 Sincronización de Plantilla Web de Políticas
- **Archivo:** `website/backend/templates/politicas_de_privacidad.html`
- **Cambios:**
  - Versión actualizada de `1.4` a `1.5`.
  - Placeholder `[CORREO DE CONTACTO DEL DESARROLLADOR]` sustituido por `dbreak472@gmail.com`.
  - Sección 6 actualizada para incluir `androidx.palette:palette-ktx` y `androidx.navigation:navigation-compose`.

---

## 3. Búsquedas Repo-Wide y Verificaciones Estáticas

| Búsqueda | Criterio / Patrón | Resultado |
| :--- | :--- | :---: |
| `dispatchGesture` | Repo-wide | **0 ocurrencias** (Confirmado que no se utilizaba `canPerformGestures`) |
| `goBack` | Repo-wide | **0 referencias** activas tras eliminación |
| `openRecents` | Repo-wide | **0 referencias** activas tras eliminación |
| `goHome` | Repo-wide | **0 referencias** activas tras eliminación |
| `canPerformGestures="false"` | `accessibility_service_config.xml` | **Confirmado `false`** |
| `canRetrieveWindowContent="false"` | `accessibility_service_config.xml` | **Confirmado `false` intacto** |
| `ACTION_ACCESSIBILITY_SETTINGS` | Repo-wide | **Únicamente 3 puntos legítimos**: (1) `AccessibilityDisclosureDialog.onAccept` en LauncherScreen, (2) `AccessibilityDisclosureDialog.onAccept` en SettingsScreen, (3) `SettingsScreen` cuando el servicio **ya** está activo |

---

## 4. Resultados de Pruebas Automatizadas

Todas las suites requeridas se ejecutaron con JVM compatible (JBR 25 / Java 21) y finalizaron con éxito:

```text
1. testDebugUnitTest:   BUILD SUCCESSFUL (100% PASS - 62/62 pruebas unitarias)
2. assembleDebug:       BUILD SUCCESSFUL (app-debug.apk generado en app/build/outputs/apk/debug/)
3. lintDebug:           BUILD SUCCESSFUL (0 errores)
4. bundleRelease:       BUILD SUCCESSFUL (lintVitalRelease: 0 errores/warnings, R8 minificado, AAB firmado generado)
5. git diff --check:    PASS (0 errores de espacios en blanco o formato)
```

---

## 5. Batería de Validación Funcional Física (Hardware Real)

> [!NOTE]
> De conformidad con la regla de oro de `novalauncher-physical-validation`, el agente no marca pruebas físicas como `PASS` por su cuenta. Se entrega a continuación el protocolo ordenado para ejecución por parte del desarrollador en el dispositivo físico (POCO X6 5G):

### Procedimiento de Instalación
```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\..\..\..\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\app-debug.apk"
```

### Checklist de Pruebas de Flujo Principal
- [ ] **A. Accessibility desactivado:** Con el servicio apagado en Ajustes de Android:
  - [ ] **B. Doble toque en área vacía:** Se despliega el diálogo `AccessibilityDisclosureDialog`.
  - [ ] **C. Deslizar hacia abajo (Swipe down):** Se despliega el diálogo `AccessibilityDisclosureDialog`.
  - [ ] **D. Tap en contador de notificaciones:** Se despliega el diálogo `AccessibilityDisclosureDialog`.
- [ ] **F. Cancelar divulgación:** Pulsar *"Ahora no"* en el diálogo:
  - El diálogo se cierra inmediatamente.
  - **NO** se abren los Ajustes del sistema (`Settings.ACTION_ACCESSIBILITY_SETTINGS`).
- [ ] **G. Aceptar divulgación:** Pulsar *"Aceptar y configurar"*:
  - Se abre la pantalla de Configuración de Accesibilidad del sistema operativo.
- [ ] **H. Servicio Activado:** Activar el servicio en Ajustes y regresar al launcher:
  - [ ] Doble toque apaga y bloquea la pantalla correctamente.
  - [ ] Deslizar hacia abajo despliega el panel de notificaciones del sistema.
  - [ ] Tocar el contador de mensajes despliega el panel de notificaciones del sistema.

### Checklist de No Regresión
- [ ] Video wallpaper fluido en bucle en View 1 y View 2.
- [ ] Widgets interactivos y conservan escala en View 1.
- [ ] Cajón de aplicaciones (App Drawer) y búsqueda rápida sin alteraciones.
- [ ] `NotificationMonitorService` continúa operando normalmente en segundo plano.

---

## 6. Advertencias y Observaciones Fuera de Alcance

1. **Placeholder detectado en documentación web complementaria:**
   Durante la búsqueda repo-wide se detectó que el archivo `docs/index.html` (línea 326) aún contiene el placeholder `[CORREO DE CONTACTO DEL DESARROLLADOR]`. Conforme al principio de cambio acotado y no realizar modificaciones oportunistas no solicitadas, dicho archivo **no fue modificado** en este ciclo y se recomienda actualizarlo en un pase documental posterior.
2. **Estado de Control de Versiones:**
   El árbol de trabajo contiene únicamente los 7 archivos modificados y este reporte local. No se ha ejecutado ningún `git commit` ni `git push`, en apego estricto a las instrucciones del desarrollador.
