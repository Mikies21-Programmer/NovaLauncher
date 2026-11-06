# ORIVOLT HOME — Alpha AAB Readiness 08F-2

Fecha de preparación: 2026-11-05  
Alcance: preparación local de `versionCode 10` para el canal de prueba cerrada Alpha. No se generó ni se subió un AAB, no se modificó Play Console y no se crearon, copiaron ni sustituyeron claves.

## 1. Archivos modificados

| Archivo | Cambio |
| --- | --- |
| `app/build.gradle.kts` | Cambio quirúrgico de `versionCode` de `9` a `10`. |
| `docs/ORIVOLT_HOME_ALPHA_AAB_READINESS_08F-2.md` | Este informe de preparación. |

Se preservaron todos los cambios preexistentes del árbol de trabajo. No se modificaron el proyecto original AnimeLauncher, NovaPlayer, recursos multimedia/gráficos, preferencias ni Hidden Apps.

## 2. Versión configurada

La configuración de `defaultConfig` queda:

| Parámetro | Valor |
| --- | --- |
| `applicationId` | `com.daybreak.animelauncher` |
| `versionCode` | `10` |
| `versionName` | `1.6.1` |
| `targetSdk` | `36` |
| `namespace` | Sin cambios: `com.daybreak.animelauncher` |

El código 10 es superior al máximo confirmado en Play (9). No se creó otra identidad de aplicación.

## 3. Estado de la firma Release

La configuración `release` de Gradle está preparada para consumir propiedades locales desde `keystore.properties` y asignar el `signingConfig` de release siempre que `nova.skipReleaseSigning` no sea verdadero. No hay contraseñas, alias privados ni credenciales incluidos en el código ni en este documento.

Comprobaciones locales realizadas:

- La clave de subida original y el certificado público esperados están presentes fuera del repositorio, en la ubicación privada definida por el protocolo de release.
- `keystore.properties` no está presente en la raíz del proyecto; por tanto, la configuración local necesaria para firmar el AAB definitivo no está disponible actualmente.
- `.gitignore` excluye `*.jks`, `*.keystore`, `keystore.properties` y `*.pem`.
- No se detectó activación de `nova.skipReleaseSigning` en `gradle.properties` ni como variable de entorno de la sesión auditada. Para el AAB definitivo, no usar `-Pnova.skipReleaseSigning=true` ni una sobreescritura equivalente.

Pendiente de firma: restaurar de forma privada el `keystore.properties` local ya autorizado, apuntando a la clave de subida original. No crear una clave nueva, no copiar el JKS y no guardar secretos en el repositorio.

## 4. Seguridad de publicación revisada

- La configuración de exclusión de fondos históricos/recursos gráficos no licenciados permanece integrada en `app/build.gradle.kts` mediante `androidResources.ignoreAssetsPatterns`.
- Los dos videos heredados (`bg_view_one.mp4` y `bg_view_two.mp4`) siguen excluidos del empaquetado; no se encontró reincorporación de recursos Dreamina en las fuentes revisadas.
- La divulgación y consentimiento de Accesibilidad siguen presentes en los recursos en español e inglés; mantienen el carácter opcional y la limitación a acciones globales.
- Hidden Apps y las preferencias no fueron modificados por esta fase.
- Los gráficos pendientes de derechos continúan siendo un bloqueo de distribución hasta que exista evidencia de licencia/derechos o se confirme su exclusión efectiva del AAB final.
- La política HTTPS está integrada en `SettingsScreen.kt` como una URL `https://`; no fue modificada por esta fase. Su disponibilidad y contenido final deben validarse antes de distribuir.

## 5. Validaciones efectuadas

- Se leyó `AGENTS.md`: no existe ningún `AGENTS.md` dentro del workspace.
- Se inspeccionaron la configuración de versión, Release signing, exclusiones de secretos y controles de distribución.
- Se verificó la ausencia de configuración local de firma (`keystore.properties`) sin leer ni exponer material sensible.
- Se ejecutó `git diff --check` después del cambio. El resultado debe ser limpio antes de distribuir.
- No se ejecutaron pruebas, lint ni compilación en este sandbox; por ello no se declaran resultados de compilación o de firma.

## 6. Comandos para PowerShell normal

Ejecutar desde la raíz del repositorio. Antes de `bundleRelease`, restaurar el `keystore.properties` privado autorizado y confirmar que no se habilite `nova.skipReleaseSigning`.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
Remove-Item Env:ORG_GRADLE_PROJECT_nova.skipReleaseSigning -ErrorAction SilentlyContinue
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat bundleRelease
.\gradlew.bat lintVitalRelease
```

Verificar el AAB y sus parámetros, sin imprimir secretos:

```powershell
$aab = "app\build\outputs\bundle\release\app-release.aab"
Test-Path $aab
& "C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe" -verify -verbose -certs $aab
bundletool dump manifest --bundle=$aab | Select-String 'package="com.daybreak.animelauncher"|versionCode="10"|targetSdkVersion="36"'
Select-String -Path "app\build.gradle.kts" -Pattern 'applicationId = "com.daybreak.animelauncher"|versionCode = 10|versionName = "1.6.1"|targetSdk = 36'
git diff --check
```

Nota: el comando requiere `bundletool` disponible en `PATH`; es la herramienta adecuada para inspeccionar un `.aab`. La verificación de `jarsigner` debe confirmar que el certificado corresponde a la clave de subida autorizada antes de cualquier distribución.

## 7. Pendientes antes de distribuir

1. Restaurar el archivo privado `keystore.properties` autorizado para la clave de subida original.
2. Ejecutar todos los comandos anteriores sin la propiedad `nova.skipReleaseSigning`.
3. Confirmar la identidad del certificado de subida mediante el proceso interno autorizado, sin registrar credenciales en el repositorio.
4. Verificar el contenido del AAB final: HTTPS, ausencia de videos Dreamina, exclusión de fondos históricos y recursos sin derechos.
5. Resolver o documentar formalmente los recursos gráficos pendientes de derechos antes de enviar el AAB a Alpha.

No se realizaron commits, push, carga a Play Console, reanudación de Alpha, cambios de testers ni envío a revisión.
