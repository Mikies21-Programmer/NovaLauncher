# Estado Persistente del Proyecto — NovaLauncher (Agent State)

> **Documento de sincronización y continuidad operativa entre sesiones y agentes.**  
> *Última actualización: 17 de septiembre de 2026.*  
> *Regla de seguridad: CERO secretos, contraseñas ni claves privadas en este documento.*

---

## 1. Identidad y Parámetros Base
* **Proyecto:** NovaLauncher
* **Repositorio Principal:** `Mikies21-Programmer/NovaLauncher`
* **Directorio Local:** `C:\Users\migue\AndroidStudioProjects\AnimeLauncher`
* **Application ID:** `com.daybreak.animelauncher`
* **Versión Actual:** `versionCode = 6`, `versionName = "1.4"`
* **SDKs:** `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`
* **Política de Privacidad Pública:** `https://mikies21-programmer.github.io/NovaLauncher-Privacy/`
* **Contacto Oficial:** `dbreak472@gmail.com`

---

## 2. Fase Actual del Proyecto
* **Fase Activa:** **FASE 5E — GESTIÓN DE UPLOAD KEY Y PREPARACIÓN DE RELEASE**
* **Hitos Recientes Completados:**
  * **Fase 5A (PASS):** Implementación de Prominent Disclosure in-app y consentimiento afirmativo para `LauncherAccessibilityService` (`AccessibilityDisclosureDialog.kt`). Validado físicamente en POCO X6 5G.
  * **Fase 5B (PASS):** Tarjeta "Información y Privacidad" en `SettingsScreen.kt` con versión dinámica y enlace nativo `Intent.ACTION_VIEW` a la web de privacidad.
  * **Fase 5C (PASS):** Repositorio dedicado `NovaLauncher-Privacy` actualizado con contacto oficial `dbreak472@gmail.com` y publicado en GitHub Pages.
  * **Fase 5D (PASS):** Auditoría integral de Google Play Console (Data Safety, Accessibility Declaration, Listing, Build Release).
  * **Fase 5E-0 (PASS):** Auditoría de claves de firma; confirmación de ausencia de keystore local previo.
  * **Fase 5E-1 & 5E-2 (PASS):** Generación de la nueva **Upload Key v2** (`upload-keystore-v2.jks`) y exportación del certificado público RFC/PEM (`upload_certificate-v2.pem`) fuera del repositorio en `C:\Users\migue\NovaLauncher-Keys\`.
  * **PERFORMANCE-03 (VERIFIED):**
    * Capacidad de `IconCache` fijada definitivamente en 128 entradas (`MAX_ENTRIES = 128`).
    * Validación física en POCO X6 5G (124 aplicaciones instaladas): scroll rápido completamente fluido, sin tirones/trabones visibles, iconos aparecen de inmediato y sin parpadeo de placeholders.
    * Evidencia empírica en sesión física instrumentada: 412 requests / 412 hits / 0 misses (100% hit rate).
    * Retiro total de instrumentación temporal de profiling (`AppDrawerPerfProfiler`, tag `NOVALAUNCHER_PERF`, contadores y trazas) tras el cierre de la fase.
  * **DRAWER & MOTION SYSTEM CHECKPOINT (PASS / VALIDATED):**
    * **App Drawer Rediseñado:** Stream híbrido en `LazyColumn` plana con `DrawerListItem.Header` y `DrawerListItem.AppRow`, eliminando anidamientos y garantizando 120 FPS sin jank.
    * **AlphabetIndexRail Optimizado:** Scrubbing instantáneo con canal conflated ("latest event wins"), pulso visual y tarjeta activa dinámica (`derivedStateOf`) sincronizada de A a Z.
    * **Swipe Horizontal entre Categorías:** Navegación por swipe no circular (Swipe izquierda: `actualIndex + 1`, Swipe derecha: `actualIndex - 1`), desambiguación con touch slop en `PointerEventPass.Initial` que respeta scroll vertical, tap, long press, rail y buscador. Ruta conceptual unificada `selectCategory(index)` compartida con las category pills.
    * **Nova Motion System (6/6 Completo):**
      1. *Drawer Open:* Fade + Scale In (160 ms, `FastOutSlowInEasing`).
      2. *Drawer Close:* Fade + Scale Out (120 ms, `FastOutLinearInEasing`).
      3. *Category Pill Motion:* Sliding glass pill en `PrimaryScrollableTabRow`.
      4. *Alphabet Rail Feedback:* Tarjeta flotante con pulso dinámico.
      5. *App Row Tap Feedback:* Micro-atenuación alpha inmediata (0.75f, 70 ms) en pulsación.
      6. *Resume Fade:* Fade sutil (~100 ms) al regresar de apps externas.
    * **Corrección False Trigger Resume Fade:** Sustitución de `ON_PAUSE` por `ON_STOP` en el observador de ciclo de vida, erradicando el micro-parpadeo al presionar botón Home en pantalla principal.
    * **Componentes Protegidos:** Intactos al 100% (`ShortcutIcon`, `IconCache`, `LauncherViewModel`, `VideoWallpaperManager`, `VideoBackground`, `WidgetHostManager`, `NativeWidgetView`, `ViewOne`, `ViewTwo`, `LauncherAccessibilityService`, `NotificationMonitorService`, `LauncherNavState`).

---

## 3. Estado de la Clave de Subida (Upload Key v2)
* **Ubicación Keystore Privada:** `C:\Users\migue\NovaLauncher-Keys\upload-keystore-v2.jks` *(FUERA DEL REPO)*
* **Ubicación Certificado PEM Público:** `C:\Users\migue\NovaLauncher-Keys\upload_certificate-v2.pem` *(FUERA DEL REPO)*
* **Alias Oficial:** `novalauncher-upload-v2`
* **Algoritmo y Tamaño:** RSA 4096 bits (`sha384RSA`), validez de 10,000 días (expira en febrero de 2054).
* **Huella Digital SHA-256 del Certificado Público:**
  ```text
  2A:DF:B7:D2:4E:62:7C:29:52:42:2B:28:D7:11:F3:D9:34:F1:9E:66:FB:B3:05:3F:AB:14:E7:97:82:39:3B:F1
  ```

---

## 4. Validaciones Automatizadas
* `.\gradlew.bat assembleDebug`: **BUILD SUCCESSFUL**
* `.\gradlew.bat testDebugUnitTest`: **BUILD SUCCESSFUL** (0 fallos)
* `.\gradlew.bat lintDebug`: **BUILD SUCCESSFUL** (0 errores)
* `.\gradlew.bat bundleRelease`: **BUILD SUCCESSFUL** (R8 + `lintVitalRelease` PASS, AAB generado)

---

## 5. Tareas Pendientes
1. **Validación Física Final por el Desarrollador (POCO X6 5G):**
   Confirmación del checklist de 21 puntos (Resume Fade sin flash al pulsar Home + Swipe horizontal entre categorías).
2. **Google Play Console — Restablecimiento de Upload Key:**
   Subir `upload_certificate-v2.pem` en la sección *"Solicitar cambio de la clave de subida"* de Play Console y aguardar la aprobación/activación de Google (suele tomar de 24 a 48 horas).
3. **Configuración Segura de Firma en Gradle (Fase 5E-3):**
   Configurar `signingConfigs.release` mediante un archivo de propiedades desacoplado e ignorado por Git (ej. `keystore.properties`) para que `bundleRelease` firme automáticamente el AAB sin exponer contraseñas.
4. **Declaración y Video de Accesibilidad:**
   Preparar y adjuntar el video explicativo de YouTube (no listado) demostrando el diálogo de divulgación destacada y los gestos globales.
5. **Ficha de Play Store:**
   Subir iconos (512x512), gráfico de funciones (1024x500) y capturas de pantalla de la app.

---

## 6. Riesgos Identificados
* **Exposición accidental de claves:** Mitigado con reglas en `.gitignore` (`*.jks`, `*.keystore`, `keystore.properties`, `*.pem`) y almacenamiento físico en `C:\Users\migue\NovaLauncher-Keys\`.
* **Desfase en la activación de la Upload Key en Play Console:** Si se intenta subir un AAB firmado antes de que Google procese el cambio de clave de subida, Play Console rechazará el archivo por discrepancia de huella digital.

---

## 7. Siguiente Acción Recomendada
1. Confirmar el envío del archivo `upload_certificate-v2.pem` en Google Play Console.
2. Una vez aceptado el reset en Play Console (o mientras se espera la ventana de tiempo), preparar la integración técnica limpia de la firma en `app/build.gradle.kts` vía `keystore.properties` desacoplado.
