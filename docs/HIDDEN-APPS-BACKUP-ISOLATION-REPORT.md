# INFORME TÃ‰CNICO: AISLAMIENTO DE ANDROID BACKUP Y MIGRACIÃ“N DETERMINISTA DE HIDDEN APPS

**Fecha:** 2026-10-04
**Fase:** `HIDDEN-APPS-BACKUP-ISOLATION`
**Baseline:** `Mikies21-Programmer/NovaLauncher` @ `5ec65d379149067e350410ec4e7fff03e295de68` (Tag `v1.6.0-stable`)
**Estado:** CONSOLIDADO Y VALIDADO (127/127 Tests PASS, assembleDebug PASS, lintDebug 0 errores, bundleRelease PASS, ValidaciÃ³n FÃ­sica en POCO X6 5G PASS)

---

## 1. Problema Diagnosticado y Causa RaÃ­z

En la implementaciÃ³n previa de Hidden Apps, el estado funcional (`isConfigured`, `authMode`, `hiddenPackages`) formaba parte de la data class `LauncherState`, la cual se serializaba en formato JSON dentro de `launcher_prefs.xml` (`KEY_LAUNCHER_STATE`).

Por el contrario, el material criptogrÃ¡fico y las credenciales cifradas residÃ­an en `nova_hidden_apps_vault.xml`, el cual estaba explÃ­citamente excluido de backups en `backup_rules.xml` y `data_extraction_rules.xml`.

### Consecuencias identificadas (BUG-018 y BUG-019):
1. **Lockout Irreversible / Estado Zombie (BUG-019):** Al reinstalar la aplicaciÃ³n o restaurar un backup de Google Drive en un dispositivo nuevo, Android restauraba `launcher_prefs.xml` con `isConfigured=true` y la lista `hiddenPackages`. Sin embargo, `nova_hidden_apps_vault.xml` y la clave simÃ©trica AES-GCM en el `AndroidKeyStore` de hardware no se restauraban. Como resultado, las aplicaciones quedaban invisibles en el Drawer pero era matemÃ¡ticamente imposible desbloquearlas o recuperarlas, produciendo un bloqueo permanente sin bypass.
2. **Fuga de Privacidad hacia la Nube de Google (BUG-018):** `hiddenPackages` (los nombres de paquetes de las aplicaciones que el usuario decidiÃ³ ocultar) se enviaban a los servidores de copia de seguridad de Google Cloud Backup dentro de `launcher_prefs.xml`.

---

## 2. DecisiÃ³n ArquitectÃ³nica Formal (D-33)

### DecisiÃ³n D-33: El estado funcional de Hidden Apps se mantiene fuera del Android Backup.

Se establece formalmente que:
- Ninguna propiedad relacionada con Hidden Apps (`isConfigured`, `authMode`, `hiddenPackages`) se almacena en archivos sujetos a copia de seguridad o transferencia.
- `launcher_prefs.xml` continÃºa respaldÃ¡ndose normalmente para preservar fondos, pÃ¡ginas, widgets y configuraciones cosmÃ©ticas legÃ­timas del usuario, pero su campo `hiddenAppsConfig` se sanea a su valor por defecto vacÃ­o en cada persistencia (`LauncherViewModel.saveState()`).
- El estado funcional de Apps Ocultas reside en un archivo de `SharedPreferences` privado y exclusivo: `nova_hidden_apps_prefs.xml`.
- `nova_hidden_apps_prefs.xml` estÃ¡ formalmente excluido de `backup_rules.xml` (Full Backup legacy) y `data_extraction_rules.xml` (Cloud Backup y Device-to-Device Transfer).

---

## 3. Estrategia de MigraciÃ³n Determinista (Caso A vs Caso B)

Para resolver la transiciÃ³n sin perjudicar a usuarios legÃ­timos existentes ni revivir estados zombies tras restauraciones, `HiddenAppsMigrator` implementa la siguiente lÃ³gica determinista:

```mermaid
flowchart TD
    Start[Inicio de NovaLauncher / loadState] --> CheckPref[Â¿Existe nova_hidden_apps_prefs.xml?]
    CheckPref -- SÃ --> LoadNew[Cargar desde HiddenAppsPreferencesStore]
    CheckPref -- NO --> CheckLegacy[Â¿Existe hiddenAppsConfig en launcher_prefs?]
    CheckLegacy -- NO / VacÃ­o --> DefaultState[Estado por defecto: isConfigured=false]
    CheckLegacy -- SÃ configurado --> EvalContext{Evaluar Contexto de Seguridad}

    EvalContext -- Modo OWN_* --> CheckVault{Â¿Existe credencial vÃ¡lida en Android Keystore?}
    CheckVault -- SÃ (Caso A: Upgrade local) --> Migrate[Migrar a nova_hidden_apps_prefs.xml y purgar legacy]
    CheckVault -- NO (Caso B: Restore huÃ©rfano) --> PurgeZombie[DESCARTAR: Purgar legacy, evitar lockout]

    EvalContext -- Modo BIOMETRIC / DEVICE_CREDENTIAL --> CheckInstall{lastUpdateTime > firstInstallTime<br>Y Hardware disponible?}
    CheckInstall -- SÃ (Caso A: Upgrade local) --> Migrate
    CheckInstall -- NO (Caso B: Restore en terminal nuevo) --> PurgeZombie
```

### Detalle de Casos:
* **Caso A â€” Usuario existente actualizando la app (Upgrade local):**
  - Posee `launcher_prefs` antiguo con `isConfigured=true`.
  - Si `authMode` es `OWN_PIN`, `OWN_PATTERN` o `OWN_PASSWORD`: `credentialStore.loadCredential()` verifica que la bÃ³veda `nova_hidden_apps_vault.xml` y la clave de `AndroidKeyStore` estÃ¡n presentes y son descifrables.
  - Si `authMode` es `BIOMETRIC` o `DEVICE_CREDENTIAL`: se verifica que la fecha de actualizaciÃ³n del paquete es posterior a la instalaciÃ³n (`lastUpdateTime > firstInstallTime`) y que el dispositivo soporta y tiene configurada la credencial/biometrÃ­a correspondiente.
  - **AcciÃ³n:** Se migra transparentemente toda la configuraciÃ³n a `nova_hidden_apps_prefs.xml` y se purga el residuo en `launcher_prefs.xml`. Las apps permanecen ocultas y la contraseÃ±a intacta.
* **Caso B â€” InstalaciÃ³n nueva / RestauraciÃ³n de backup antiguo:**
  - Existe un `launcher_prefs` restaurado desde Google Backup con `isConfigured=true` y `hiddenPackages`.
  - Para `OWN_*`: No existe clave en Keystore (las claves de Keystore nunca se respaldan) o la bÃ³veda falta.
  - Para `BIOMETRIC`/`DEVICE_CREDENTIAL`: La instalaciÃ³n es limpia (`firstInstallTime == lastUpdateTime` en un terminal nuevo o tras wipe).
  - **AcciÃ³n:** Se rechaza de forma absoluta la reactivaciÃ³n, purgando el estado en `launcher_prefs.xml`. No hay apps atrapadas, no hay bloqueo fantasma y el usuario inicia con el Drawer limpio.

---

## 4. Archivos Modificados y Creados

### Archivos Creados:
1. `app/src/main/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsPreferencesStore.kt`:
   - Encapsula la persistencia en `nova_hidden_apps_prefs.xml`.
   - Modos `isConfigured`, `authMode` y `hiddenPackages` (`Set<String>`).
2. `app/src/main/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsMigrator.kt`:
   - LÃ³gica de migraciÃ³n segura y purga de estado legacy huÃ©rfano.
   - Marcador local `nova_local_install.marker` en `noBackupFilesDir` para generaciÃ³n local durable.
3. `app/src/test/java/com/daybreak/animelauncher/hiddenapps/HiddenAppsBackupMigrationTest.kt`:
   - Suite de 20 pruebas unitarias automatizadas cubriendo todos los escenarios de migraciÃ³n, exclusiÃ³n de XMLs y seÃ±ales de noBackup.

### Archivos Modificados:
1. `app/src/main/res/xml/backup_rules.xml`:
   - Agregada exclusiÃ³n `<exclude domain="sharedpref" path="nova_hidden_apps_prefs.xml" />`.
2. `app/src/main/res/xml/data_extraction_rules.xml`:
   - Agregada exclusiÃ³n en `<cloud-backup>` y `<device-transfer>`.
3. `app/src/main/java/com/daybreak/animelauncher/LauncherViewModel.kt`:
   - En `loadState()`: resuelve configuraciÃ³n mediante `HiddenAppsMigrator.resolveConfig()`.
   - En `saveState()`: persiste siempre `hiddenAppsConfig = HiddenAppsConfig()` en `launcher_prefs.xml`.
   - En `updateState()` y setters de Hidden Apps: persiste de forma asÃ­ncrona hacia `HiddenAppsPreferencesStore`.
4. `docs/PRIVACY_POLICY.md` & `website/backend/templates/politicas_de_privacidad.html`:
   - ActualizaciÃ³n del apartado de almacenamiento local de Apps Ocultas.
5. `docs/AGENT_PROJECT_STATE.md`:
   - DocumentaciÃ³n del estado del proyecto, validaciÃ³n fÃ­sica y decisiÃ³n de arquitectura D-33.

---

## 5. Matriz de Cobertura de Tests Unitarios (20/20 PASS)

| Test ID | Escenario Validado | Resultado |
|---|---|---|
| **T-MIG-01** | Launcher antiguo con OWN_PIN y vault vÃ¡lida migra exitosamente a nuevo storage | âœ… PASS |
| **T-MIG-02** | Launcher antiguo sin Hidden Apps configuradas no migra ni genera cambios | âœ… PASS |
| **T-MIG-03** | Backup restaurado de OWN_PIN sin Keystore/vault es DESCARTADO (sin lockout ni zombies) | âœ… PASS |
| **T-MIG-04** | Nueva configuraciÃ³n de Hidden Apps se persiste exclusivamente en `nova_hidden_apps_prefs.xml` | âœ… PASS |
| **T-MIG-05** | `nova_hidden_apps_prefs.xml` estÃ¡ formalmente excluido en `backup_rules.xml` | âœ… PASS |
| **T-MIG-06** | `nova_hidden_apps_prefs.xml` estÃ¡ formalmente excluido en `<cloud-backup>` de `data_extraction_rules.xml` | âœ… PASS |
| **T-MIG-07** | `nova_hidden_apps_prefs.xml` estÃ¡ formalmente excluido en `<device-transfer>` de `data_extraction_rules.xml` | âœ… PASS |
| **T-MIG-08** | OWN_* con vault vÃ¡lida preserva desbloqueo y autenticaciÃ³n | âœ… PASS |
| **T-MIG-09** | OWN_* sin vault no obtiene bypass bajo ninguna circunstancia (fail-closed) | âœ… PASS |
| **T-MIG-10** | Paquetes de restore invÃ¡lido no reaparecen en App Drawer | âœ… PASS |
| **T-MIG-11** | Reset protegido continÃºa limpiando almacÃ©n aislado y credenciales | âœ… PASS |
| **T-MIG-12** | BIOMETRIC y DEVICE_CREDENTIAL migran en upgrade y se descartan en restore nuevo | âœ… PASS |
| **T-MIG-13** | Upgrade local BIOMETRIC conserva configuraciÃ³n | âœ… PASS |
| **T-MIG-14** | Upgrade local DEVICE_CREDENTIAL conserva configuraciÃ³n | âœ… PASS |
| **T-MIG-15** | Restore nuevo BIOMETRIC descarta legacy | âœ… PASS |
| **T-MIG-16** | Restore nuevo DEVICE_CREDENTIAL descarta legacy | âœ… PASS |
| **T-MIG-17** | Restore sobre instalaciÃ³n existente con store presente prevalece sobre legacy | âœ… PASS |
| **T-MIG-18** | Marcador local ausente: comportamiento fail-safe sin excepciones | âœ… PASS |
| **T-MIG-19** | Marcador local presente no rompe upgrade vÃ¡lido | âœ… PASS |
| **T-MIG-20** | Marcador local se crea en todas las rutas de resoluciÃ³n | âœ… PASS |

---

## 6. Resultados de ValidaciÃ³n Automatizada

- **Tests Unitarios (`testDebugUnitTest`):**
  `BUILD SUCCESSFUL` â€” **127/127 tests PASS (100%)** (62 suite base + 45 suite Apps Ocultas + 20 suite MigraciÃ³n y Aislamiento Backup).
- **CompilaciÃ³n Debug (`assembleDebug`):**
  `BUILD SUCCESSFUL` â€” 0 errores.
- **AnÃ¡lisis EstÃ¡tico (`lintDebug`):**
  `BUILD SUCCESSFUL` â€” **0 errores de lint**.
- **Bundle de Release (`bundleRelease`):**
  `BUILD SUCCESSFUL` â€” Signed AAB generado sin advertencias crÃ­ticas.
- **Integridad de Git (`git diff --check`):**
  Sin espacios en blanco residuales ni conflictos de formato (cÃ³digo 0).

---

## 7. Resultados de ValidaciÃ³n FÃ­sica en Hardware Real (POCO X6 5G â€” Android 16)

La baterÃ­a completa de pruebas fÃ­sicas en hardware real se ejecutÃ³ sobre el dispositivo de referencia:

| Caso de Prueba | DescripciÃ³n del Escenario | Resultado FÃ­sico |
|---|---|:---:|
| **P1** | ConfiguraciÃ³n y autenticaciÃ³n con `OWN_PIN` tras ciclo de vida | âœ… **PASS** |
| **P2** | ConfiguraciÃ³n y autenticaciÃ³n con `BIOMETRIC` tras ciclo de vida | âœ… **PASS** |
| **P6** | ConfiguraciÃ³n y autenticaciÃ³n con `DEVICE_CREDENTIAL` tras ciclo de vida | âœ… **PASS** |
| **P10** | Reinicio completo del dispositivo (`REBOOT`): marker y config sobreviven, 0 FATAL | âœ… **PASS** |
| **P3** | Legacy restore huÃ©rfano (`OWN_*` sin keystore / vault en terminal limpio): descartado | âœ… **PASS** |
| **P4** | Aislamiento de backup: exclusiÃ³n formal en reglas XML + restore real local con `bmgr` | âœ… **PASS** |
| **P8** | Cold start post-reboot: curva de asentamiento 9048 ms â†’ 3911 ms â†’ ~2800 ms estable | âœ… **PASS** |
| **P9** | Estado en disco (`disk state`): persistencia en `nova_hidden_apps_prefs.xml` y marker en `noBackupFilesDir` | âœ… **PASS** |
| **C1** | InstalaciÃ³n nueva + legacy BIOMETRIC: rechazo estricto (no migra, sin lockout) | âœ… **PASS** |
| **C2** | Residual fÃ­sicamente reproducido y documentado (ver secciÃ³n 8) | âœ… **DOCUMENTADO** |
| **P5** | Device-to-Device (D2D) en Xiaomi | â³ **PENDING / NOT PHYSICALLY VALIDATED** |
| **P7** | Keystore invalidado + configuraciÃ³n vÃ¡lida de Hidden Apps | âš ï¸ **RIESGO PREEXISTENTE** |

> [!NOTE]
> **P5 (Device-to-Device Transfer en Xiaomi):** Permanece formalmente como **PENDING / NOT PHYSICALLY VALIDATED**. El mecanismo de transferencia D2D de Xiaomi no fue probado fÃ­sicamente en esta ronda de validaciÃ³n. Las reglas XML ya contienen la exclusiÃ³n formal en `<device-transfer>`, pero no se afirma validaciÃ³n fÃ­sica en hardware Xiaomi D2D.

> [!WARNING]
> **P7 (Keystore Invalidado â€” Riesgo Preexistente):**
> Si el Keystore del sistema resulta invalidado (por ejemplo, al cambiar de credencial de pantalla en ciertas versiones de Android o por corrupciÃ³n del enclave seguro), una configuraciÃ³n vÃ¡lida previa de Hidden Apps con modos `OWN_*` impedirÃ¡ la autenticaciÃ³n.
> Este es un comportamiento criptogrÃ¡fico inherente a `AndroidKeyStore` de hardware y es un **riesgo PREEXISTENTE**, NO introducido ni asociado a esta implementaciÃ³n de aislamiento de backup.

---

## 8. DescripciÃ³n Detallada del Residual C2

Durante la fase de anÃ¡lisis de casos lÃ­mite (Q3/Q6) y validaciÃ³n fÃ­sica se identificÃ³ y caracterizÃ³ exhaustivamente el residual C2:

### CaracterÃ­sticas exactas del Residual C2:
- **Requiere upgrade real previo:** El dispositivo destino debe haber recibido una actualizaciÃ³n real de APK (haciendo que `lastUpdateTime > firstInstallTime`).
- **InstalaciÃ³n virgen de aislamiento:** La instalaciÃ³n en dicho dispositivo nunca ha ejecutado previamente la build de aislamiento de Hidden Apps (por lo que aÃºn no se ha estampado el marcador local `nova_local_install.marker` ni creado `nova_hidden_apps_prefs.xml`).
- **RestauraciÃ³n forÃ¡nea legacy:** Se restaura una configuraciÃ³n legacy de `BIOMETRIC` o `DEVICE_CREDENTIAL` proveniente de un backup previo.
- **Ocurrencia en primer launch:** La migraciÃ³n forÃ¡nea ocurre exclusivamente durante el primer arranque (`first launch`) tras la restauraciÃ³n.
- **Ventana de un solo arranque:** Es una ventana estrictamente de un solo arranque. A partir del primer launch, `nova_hidden_apps_prefs.xml` queda creado y el marcador estampado, cerrando la posibilidad de futuras migraciones forÃ¡neas.
- **No afecta a modos propios:** **No afecta a `OWN_*`** (`OWN_PIN`, `OWN_PATTERN`, `OWN_PASSWORD`), ya que estos exigen de forma infranqueable la presencia de la clave de hardware en `AndroidKeyStore` y el descifrado del vault.
- **Sin lockout:** No produce bloqueo irreversible del sistema ni de la pantalla.
- **Sin bypass:** No otorga acceso no autorizado a datos protegidos.
- **Totalmente recuperable:** Es recuperable mediante el reseteo protegido de Hidden Apps (`verifyAndReset()`).
- **Cierre definitivo planificado:** QuedarÃ¡ cerrada en una futura release cuando el marcador `nova_local_install.marker` sea un requisito obligatorio para migrar modos del sistema (upgrade legÃ­timos ya contarÃ¡n con el marcador estampado por la build actual).

---

## 9. Matriz de Riesgos Residuales

| Riesgo | Probabilidad | Severidad | MitigaciÃ³n Implementada |
|---|---|---|---|
| Usuario existente que actualice pierde sus apps ocultas | Muy baja | Alta | `HiddenAppsMigrator` verifica la presencia de la credencial en `AndroidKeyStore` o contexto de actualizaciÃ³n del paquete. Si es vÃ¡lido, preserva la lista intacta y la traslada a `nova_hidden_apps_prefs.xml`. |
| Usuario con backup antiguo en Google Drive sufre lockout al cambiar de telÃ©fono | Nula | CrÃ­tica | El migrador descarta cualquier configuraciÃ³n de backup si la bÃ³veda/Keystore no existe en el terminal, iniciando el launcher en estado limpio y seguro. |
| Inconsistencia de estado en memoria Compose | Nula | Media | `LauncherState.hiddenAppsConfig` se mantiene en el flujo StateFlow en memoria; las vistas de Compose no sufren cambios de API ni recomposiciones anÃ³malas. |
| Fuga de nombres de apps en futuras copias de Google Drive | Nula | Alta | `saveState()` sanea explÃ­citamente `hiddenAppsConfig = HiddenAppsConfig()` al serializar hacia `launcher_prefs.xml`. |
| Residual C2 (Ventana de 1 launch en upgrade no ejecutado + restore) | Muy baja | Baja | Mitigado por diseÃ±o: sin lockout, sin bypass, recuperable por reset, y se cerrarÃ¡ en la siguiente release mediante requisito del marcador local. |
