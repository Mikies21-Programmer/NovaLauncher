---
name: novalauncher-play-release
description: Procedimiento de preparación, verificación de firma (keystore/AAB), cumplimiento de políticas y empaquetado para Google Play Console en NovaLauncher.
---

# Skill: Preparación de Release y Play Console — NovaLauncher

## Propósito
Guiar la preparación de artefactos de producción (`.aab`), verificación de firmas digitales y cumplimiento de directrices técnicas de Google Play Store sin exponer credenciales ni alterar configuraciones de forma descuidada.

---

## Directrices de Firma y Secretos
1. **Separación de Almacenes:**
   * La clave privada de subida (`upload-keystore-v2.jks`) reside **FUERA** del repositorio en `C:\Users\migue\NovaLauncher-Keys\`.
   * El certificado público para Play Console es `upload_certificate-v2.pem`.
   * Alias oficial: `novalauncher-upload-v2`.
2. **Cero Secretos en Git:**
   * El archivo `.gitignore` debe proteger siempre `*.jks`, `*.keystore`, `keystore.properties` y `*.pem`.
   * Jamás incluir contraseñas (`storePassword`, `keyPassword`) en `build.gradle.kts` de forma visible. Utilizar archivos locales desacoplados (`keystore.properties`) o variables de entorno.

---

## Verificación de Parámetros de Release
Antes de compilar un bundle para Play Store, auditar:
* `applicationId`: Debe ser exactamente `com.daybreak.animelauncher`.
* `versionCode`: Debe ser estrictamente superior al último subido a Play Console.
* `versionName`: Debe corresponder al hito público (ej. `"1.4"`).
* `minSdk`: `24` (soporta Android 7.0+).
* `targetSdk`: Cumplir con el requisito de Google Play (targetSdk ≥ 34/35/36).

---

## Compilación y Verificación de AAB
Para generar el App Bundle de release:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat bundleRelease
```
Comprobar el artefacto en `app/build/outputs/bundle/release/app-release.aab`:
```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe" -verify -verbose -certs "app/build/outputs/bundle/release/app-release.aab"
```
Verificar que la firma corresponda a la Upload Key autorizada antes de subir a la consola.

---

## Declaraciones Clave en Play Console
* **Accessibility Service:** Declarar solo acciones globales (`LOCK_SCREEN` y `NOTIFICATIONS`). `canRetrieveWindowContent="false"`. Proveer enlace a video de YouTube demostrando el Prominent Disclosure in-app.
* **Data Safety:** Declarar que la app no recopila ni comparte datos de usuario; procesamiento efímero y local de notificaciones y medios.
* **Anuncios:** Seleccionar "No contiene anuncios".
* **Compras:** Seleccionar "No contiene compras dentro de la aplicación".
