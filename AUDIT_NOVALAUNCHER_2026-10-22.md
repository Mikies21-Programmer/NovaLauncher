# Auditoría forense NovaLauncher — Informe de progreso

**Fecha de ejecución:** 2026-10-22  
**Alcance de esta ejecución:** Fase 0 — Acceso e identidad; revisión específica de NL-AUD-002; remediación y publicación acotadas del árbol actual  
**Modo:** auditoría de solo lectura; remediación y publicación autorizadas sobre el índice, `.gitignore` y documentación. No se modificaron fuentes, recursos funcionales, Gradle ni manifiestos.  
**Estado global:** Fase 0 completada; NL-AUD-002 con retirada publicada en `main`, historial pendiente y plan de purga documentado (no ejecutado); Fases 1–8 pendientes.  
**Remediación NL-AUD-002:** retirada, reglas de `.gitignore` y actualización documental publicadas en `origin/main` mediante el commit `615852d8383c20f42c2ef04f47044dc650ea263c`; push normal completado; historial sin reescribir. El plan de purga histórica está en la sección 11 y no se ejecutó.

---

## 0. Resumen de la Fase 0

Se verificó acceso real al repositorio local y al remoto de GitHub. El directorio de trabajo es
`C:\Users\migue\AndroidStudioProjects\AnimeLauncher`, corresponde a NovaLauncher
(`applicationId com.daybreak.animelauncher`), y su remoto `origin` es
`https://github.com/Mikies21-Programmer/NovaLauncher.git`.

En el estado inicial, el código local **no estaba exactamente en el mismo commit que el remoto**:

- `HEAD` inicial (antes de la remediación): `1c17d38c9650740e2b9596d2d8e992d6a17270af` (`main`), un commit por delante de `origin/main`.
- `origin/main` inicial y la rama remota `main`: `f1b088bc941070c99ee50929b45d40312c1d5999`.
- Estado actual tras la publicación: `HEAD` y `origin/main` = `615852d8383c20f42c2ef04f47044dc650ea263c` (sección 10).
- El commit local adicional solo elimina dos líneas de `docs/UI-1-PHASE8-AUDIT.md`; no cambia código.
- Las 215 entradas preexistentes de `git status` corresponden exclusivamente a artefactos generados
  bajo `benchmark/build/`; la única entrada adicional es este informe no rastreado. **No hay
  modificaciones locales en fuentes, recursos, Gradle ni manifiesto.**

También se detectaron dos riesgos de repositorio que no requieren análisis arquitectónico para
confirmarse: artefactos de compilación versionados y snapshots de preferencias del dispositivo con
paquetes de aplicaciones instaladas en un repositorio público. Se registran como `NL-AUD-001` y
`NL-AUD-002`, respectivamente. El resto de la auditoría técnica queda para las fases siguientes.

La revisión específica de `NL-AUD-002` del 2026-10-22 confirmó **seis XML versionados** con el mismo
conjunto de **87 paquetes** y datos personales; no se encontraron secretos ni credenciales en esos
archivos. La retirada y la prevención se publicaron en `main`; el historial sigue pendiente y la
Fase 1 no se ha iniciado.

---

## 1. Evidencia de acceso

### 1.1 Comandos ejecutados y resultados reales

| Comprobación | Comando | Resultado observado |
|---|---|---|
| Directorio de trabajo | `pwd` | `C:\Users\migue\AndroidStudioProjects\AnimeLauncher` |
| Repositorio Git | `git rev-parse --is-inside-work-tree` (implícito en `git status`) | Repositorio válido; rama `main` |
| Estado corto inicial | `git status -sb` | `## main...origin/main [ahead 1]` |
| Remoto | `git remote -v` | `origin https://github.com/Mikies21-Programmer/NovaLauncher.git` (fetch y push) |
| SHA local inicial | `git rev-parse HEAD` | `1c17d38c9650740e2b9596d2d8e992d6a17270af` |
| SHA remoto inicial | `git ls-remote origin refs/heads/main` | `f1b088bc941070c99ee50929b45d40312c1d5999 refs/heads/main` |
| Referencia remota local inicial | `git rev-parse origin/main` | `f1b088bc941070c99ee50929b45d40312c1d5999` |
| Distancia inicial | `git rev-list --count origin/main..HEAD` / `HEAD..origin/main` | `1` / `0` |
| Metadatos GitHub | API `repos/Mikies21-Programmer/NovaLauncher` | Repositorio público, rama por defecto `main`, `pushed_at 2026-10-17T08:14:17Z`, tamaño reportado 360 410 KB |

La consulta a la API de GitHub y `git ls-remote` confirman que la red estaba disponible y que el
remoto existe y es accesible. No se trabajó sobre una copia simulada.

### 1.2 Historial reciente verificado

Estado inicial, antes de la remediación:

```text
1c17d38 2026-10-17 (HEAD -> main) docs: remove duplicated header in Phase 8 audit report
f1b088b 2026-10-17 (origin/main, origin/HEAD) feat: complete adaptive theme settings UI (UI-1 phase 8)
16eea76 2026-10-10 feat: add global device RAM widget
f4c72a0 2026-10-08 feat: add configurable launcher logo
7539e09 2026-10-08 feat: add adaptive video theme sampling
08340c6 2026-10-04 feat: add adaptive glass premium
35669b5 2026-10-04 feat: enable adaptive colors for image backgrounds
e6f97ea 2026-10-04 feat: scope adaptive theme per launcher page
ec49dc8 2026-10-04 feat: add adaptive theme runtime core
05ef434 2026-10-04 docs: close memory l2 audit
```

Tras la publicación, `615852d` es `HEAD` y `origin/main` (sección 10).

- El commit local adicional (`1c17d38`) modifica únicamente `docs/UI-1-PHASE8-AUDIT.md`:
  `1 file changed, 2 deletions(-)`.
- `f1b088b` era el commit remoto inicial y también es ancestro de `HEAD`.
- `05ef434` **no es el remoto actual**: es un ancestro a 9 commits de `HEAD`
  (`docs: close memory l2 audit`, 2026-10-04). La referencia histórica que lo situaba como remoto
  era correcta en su momento, pero está desactualizada.
- En el estado inicial, el commit mencionado como local, `f1b088b`, era en realidad el remoto.

### 1.3 Modificaciones sin commit en la revisión inicial

Estado inicial, antes de la remediación. `git status --porcelain` devolvió 215 entradas:

| Tipo | Cantidad | Ubicación |
|---|---:|---|
| Archivos rastreados modificados (`M`) | 157 | `benchmark/build/**` |
| Archivos rastreados eliminados (`D`) | 28 | `benchmark/build/**` |
| Archivos no rastreados (`??`) | 30 | `benchmark/build/**` |
| **Total** | **215** | **Todos bajo `benchmark/build/`** |
| Entradas fuera de directorios `build/` | **0** | — |

Conclusión: el árbol está sucio por una compilación previa del módulo `benchmark`, no por trabajo
funcional pendiente. No se ejecutó `clean`, ni `checkout`, ni ningún comando que alterara ese estado.

### 1.4 Worktrees

Estado inicial. `git worktree list` muestra dos ubicaciones:

```text
C:/Users/migue/AndroidStudioProjects/AnimeLauncher  1c17d38 [main]
C:/Users/migue/nova-baseline                        5ec65d3 (detached HEAD)
```

La auditoría se realiza sobre el worktree principal. El worktree `nova-baseline` no fue analizado en
esta fase y no se modificó. Tras la publicación, el worktree principal está en `615852d`; `nova-baseline`
sigue en `5ec65d3`.

---

## 2. Identidad del proyecto y del código auditado

| Elemento | Valor verificado | Fuente |
|---|---|---|
| Nombre Gradle | `AnimeLauncher` | `settings.gradle.kts` |
| Módulos | `:app`, `:benchmark` | `settings.gradle.kts` |
| `applicationId` | `com.daybreak.animelauncher` | `app/build.gradle.kts:25` |
| `namespace` | `com.daybreak.animelauncher` | `app/build.gradle.kts:17` |
| Versión | `versionCode 8`, `versionName "1.6"` | `app/build.gradle.kts:28-29` |
| SDK | `minSdk 24`, `targetSdk 36`, `compileSdk 36` | `app/build.gradle.kts:18-27` |
| Kotlin | `2.2.10` | `gradle/libs.versions.toml` |
| AGP | `9.3.1` | `gradle/libs.versions.toml` |
| Gradle Wrapper | `9.5.0-bin`, checksum SHA-256 declarado | `gradle/wrapper/gradle-wrapper.properties` |
| Java/JVM | Java 17, `jvmToolchain(17)` | `app/build.gradle.kts` |
| Compose BOM | `2026.02.01` | `gradle/libs.versions.toml` |
| Release | `isMinifyEnabled = true`, `isShrinkResources = true`, R8 | `app/build.gradle.kts:58-59` |
| Firma | `keystore.properties` local, no rastreado por Git | `app/build.gradle.kts`; `git ls-files` |
| Repositorio remoto | Público, `main`, `Mikies21-Programmer/NovaLauncher` | API GitHub |

`keystore.properties` y `local.properties` existen en el disco pero **no están rastreados** por Git
(`git ls-files --error-unmatch` falla para ambos). No se leyó ni se transcribió ningún secreto de
firma.

---

## 3. Mapa real de archivos y componentes

### 3.1 Volumen del código

| Conjunto | Archivos | Observación |
|---|---:|---|
| Kotlin de producción (`app/src/main/java`) | 61 | 14 865 líneas |
| Pruebas unitarias (`app/src/test`) | 22 archivos | 236 anotaciones `@Test` detectadas |
| Pruebas instrumentadas (`app/src/androidTest`) | 1 | Solo `ExampleInstrumentedTest.kt` |
| Benchmarks (`benchmark/src` + `app/src/benchmark`) | 3 archivos Kotlin | Benchmark y Baseline Profile |
| Archivos rastreados totales | 1 496 | 1 011 son artefactos de `benchmark/build` |
| Documentación y evidencias | 144 archivos bajo `docs/` | Incluye informes, capturas y trazas |

### 3.2 Paquetes Kotlin reales

```text
com.daybreak.animelauncher
├── hiddenapps/     credenciales, autenticación, repositorio, migrador, modelos, preferencias
├── memory/         MemoryManager
├── ram/            RamMonitor
├── theming/        motor adaptativo, tokens, analizador, video sampler, propuestas, logo
├── ui/
│   ├── components/ VideoWallpaperManager, VideoBackground, widgets de reloj/batería/RAM
│   ├── screens/    LauncherScreen, SettingsScreen, AdvancedSettingsScreen, AppDrawerScreen,
│   │               OnboardingScreen, BackgroundSelectionScreen, ViewOne, ViewTwo,
│   │               hiddenapps/ (pantalla, auth, setup, gesto)
│   └── theme/      Theme, Color, Type
└── widget/         WidgetHostManager, NativeWidgetView
```

### 3.3 Puntos de entrada y componentes declarados

| Componente | Archivo | Evidencia |
|---|---|---|
| `Application` | `NovaApplication.kt:15` | `onTrimMemory` en línea 17; `onLowMemory` en línea 22 |
| Actividad principal | `MainActivity.kt:31` | `FragmentActivity`; `setContent` en línea 188; `NavHost` con destinos `onboarding`, `launcher`, `settings` |
| Servicio de accesibilidad | `LauncherAccessibilityService.kt` | Declarado en manifiesto líneas 59-62, `exported="true"` con permiso `BIND_ACCESSIBILITY_SERVICE` |
| Listener de notificaciones | `NotificationMonitorService.kt` | Declarado en manifiesto líneas 71-75, `exported="true"` con permiso `BIND_NOTIFICATION_LISTENER_SERVICE` |
| Receiver de pantalla | `MainActivity.kt:178` | Registro de `ACTION_USER_PRESENT` y `ACTION_SCREEN_OFF`; liberación en `onDestroy` línea 295 |
| ViewModel principal | `LauncherViewModel.kt:197` | `AndroidViewModel`; `SharedPreferences("launcher_prefs")` línea 198; Gson línea 201; `loadState()` línea 305 |
| Ciclo de vídeo | `ui/components/VideoWallpaperManager.kt` | llamadas desde `onResume`, `onPause`, `onStop`, `onDestroy` |

Manifiesto (`app/src/main/AndroidManifest.xml`):

- Permisos: `INTERNET` (línea 5), `BIND_APPWIDGET` (6), `SET_WALLPAPER` (7), `USE_BIOMETRIC` (8).
- `android:allowBackup="true"` (línea 19).
- `MainActivity` exportada con `MAIN`, `LAUNCHER`, `HOME` y `DEFAULT` (líneas 42-56).
- Reglas de backup: `app/src/main/res/xml/backup_rules.xml` y
  `app/src/main/res/xml/data_extraction_rules.xml` excluyen
  `nova_hidden_apps_vault.xml` y `nova_hidden_apps_prefs.xml`.
- Configuración de accesibilidad: `accessibility_service_config.xml` declara
  `canRetrieveWindowContent="false"` y `canPerformGestures="false"`.
- Release: `app/proguard-rules.pro` conserva modelos Gson, Media3, Coil y clases de actividad.

La existencia de estos componentes se comprobó en el árbol actual; su comportamiento y seguridad se
evalúan en las fases 1, 4 y 5, no en esta fase.

### 3.4 Documentación y evidencias

Hay 25 documentos en la raíz de `docs/` (24 Markdown y 1 HTML), incluidos:

- `AGENT_PROJECT_STATE.md`
- `HIDDEN-APPS-*.md` (implementación, estabilización, auditoría y aislamiento de backup)
- `MEMORY-L1-*` y `MEMORY-L2-AUDIT.md`
- `PERFORMANCE_*.md`
- `UI-1-PHASE1..8-*.md`
- `PRIVACY_POLICY.md`

También hay capturas, dumps XML, trazas de Perfetto y logs bajo `docs/assets/` y
`docs/performance_traces/`. Las afirmaciones de esos documentos **no se dan por válidas**: se
contrastarán con el código en las fases correspondientes.

---

## 4. Hallazgos confirmados en Fase 0

### NL-AUD-001 — Artefactos de compilación versionados en `benchmark/build`

| Campo | Valor |
|---|---|
| **Severidad** | P2 — media |
| **Confianza** | Confirmada |
| **Ubicación** | `benchmark/build/**` (1 011 archivos rastreados); `.gitignore` raíz; ausencia de `benchmark/.gitignore` |
| **Categoría** | Higiene de repositorio / cadena de build / distribución |

**Evidencia**

- `git ls-files` devuelve 1 496 archivos; 1 011 están bajo `benchmark/build/` (67,6 % del total).
- El módulo `benchmark` tiene 1 016 archivos rastreados: 1 011 de `build/` y solo 5 de configuración
  o código.
- No existe `benchmark/.gitignore`. El `.gitignore` raíz solo contiene `/build`, que ignora el
  `build/` de la raíz, no los `build/` de los módulos. `app/.gitignore` sí contiene `/build`, pero
  `benchmark/` no tiene el equivalente.
- `git status` muestra 215 cambios, todos en `benchmark/build/`: 157 modificados, 28 eliminados y
  30 no rastreados.
- El pack local mide 285,74 MiB (`git count-objects -vH`); GitHub reporta 360 410 KB para el
  repositorio. Entre los archivos más grandes hay APKs de benchmark de ~35 MB y trazas de
  `docs/performance_traces/` de hasta ~40 MB.
- Ejemplo de artefacto rastreado: `benchmark/build/outputs/apk/debug/benchmark-debug.apk`.

**Condición de fallo**

Cada ejecución del módulo `benchmark` reescribe archivos que Git considera versionados. El árbol
queda permanentemente sucio, y cualquier `git add -A` o commit descuidado incorpora binarios
generados, obsoletos o específicos de la máquina.

**Impacto**

- Clones y fetches innecesariamente grandes.
- Ruido en revisiones y riesgo de commitear APKs, dex, cachés de Kotlin o artefactos de otra
  variante.
- Posible confusión entre el código fuente y el binario que realmente se probó.
- No afecta al comportamiento en tiempo de ejecución de la app.

**Verificación**

```powershell
git ls-files | Measure-Object
git ls-files | Select-String '(^|/)build/'
git ls-files benchmark/build | Measure-Object
git status --porcelain | Where-Object { $_ -notmatch 'build/' }
Test-Path benchmark/.gitignore
git count-objects -vH
```

**Recomendación mínima**

1. Añadir `benchmark/.gitignore` con `/build` (y, si se desea, patrones para APK, dex y cachés).
2. En una tarea autorizada, dejar de rastrear los artefactos:
   `git rm -r --cached benchmark/build` y commitear solo el `.gitignore`.
3. Evaluar si las trazas de `docs/performance_traces/` deben permanecer en Git, moverse a un
   almacenamiento externo o usar Git LFS. No es necesario reescribir historia para los artefactos de
   build salvo que se quiera reducir el tamaño del pack.
4. Verificar que CI no dependa de esos binarios versionados.

**Riesgo de regresión**

Bajo para la app. Puede afectar a scripts o documentación que asuman que
`benchmark/build/outputs/...` existe en un clon limpio. El módulo `benchmark` debe seguir
compilando desde cero.

---

### NL-AUD-002 — Preferencias reales del dispositivo en repositorio público (revisión específica)

| Campo | Valor |
|---|---|
| **Severidad** | P1 — alta |
| **Confianza** | Confirmada para la exposición y los datos personales; potencial para la cita no default; **sin secretos confirmados** |
| **Ubicación** | 6 XML rastreados: `phase6-prefs-snapshot.xml`, `phase7-prefs-snapshot.xml`, `p7-a.xml`, `p7-light.xml`, `p7-video.xml`, `p7-working.xml` |
| **Estado** | **PENDIENTE DE REMEDIACIÓN** (retirada publicada en `main`; historial pendiente; no resuelto) |
| **Categoría** | Privacidad / datos personales / cadena de suministro |

Esta sección sustituye la evaluación inicial de dos archivos por una revisión específica de los seis
XML reales que contienen `launcher_state`. No se reproducen nombres de paquetes, categorías
asignadas, la cita, los URIs completos ni valores privados.

#### 1. Identidad de los archivos

| Archivo | Tamaño local | Blob Git | Hash del `launcher_state` (prefijo) | Commit de alta |
|---|---:|---|---|---|
| `docs/assets/phase6-prefs-snapshot.xml` | 12 413 B | `44dc0a46…` | `B37D5E01…` | `f4c72a0` (2026-10-08) |
| `docs/assets/phase7-prefs-snapshot.xml` | 12 408 B | `44dc0a46…` | `B37D5E01…` | `16eea76` (2026-10-10) |
| `docs/assets/p7-a.xml` | 12 435 B | `28ea1cf6…` | `97DE7DCC…` | `16eea76` (2026-10-10) |
| `docs/assets/p7-light.xml` | 12 439 B | `61aa3ecc…` | `97DE7DCC…` | `16eea76` (2026-10-10) |
| `docs/assets/p7-video.xml` | 12 439 B | `2e9e3dfa…` | `A9B0F5E1…` | `16eea76` (2026-10-10) |
| `docs/assets/p7-working.xml` | 12 444 B | `4d85d235…` | `B37D5E01…` | `16eea76` (2026-10-10) |

**phase6 y phase7:**

- Son **idénticos a nivel de blob Git**: ambos apuntan a
  `44dc0a4610197596fe13548a659ff45197574e41`.
- En disco no son byte a byte iguales: `phase6` está en CRLF y `phase7` en LF; la diferencia es de
  5 bytes. `git diff --no-index --ignore-cr-at-eol` no muestra diferencias de contenido.
- GitHub sirve ambos con longitud remota 12 408 B; la diferencia local de `phase6` es solo el CRLF.
- `phase7` fue añadido en `16eea76` como **C100** (copia al 100 %) de `phase6`, según
  `git log --follow --name-status`.

**p7-a, p7-light, p7-video y p7-working:**

- Son archivos distintos, no copias exactas de phase6/phase7.
- Comparten el mismo esquema (`map` con `launcher_state` y `launcher_style_config`) y el **mismo
  conjunto exacto de 87 paquetes**; cambian el estado de tema/fondo entre variantes.
- `p7-a` y `p7-light` comparten el mismo `launcher_state`; `p7-video` tiene otro estado; y
  `p7-working` comparte el estado de phase6/phase7. El hash del conjunto de paquetes es idéntico en
  los seis archivos (`687DAFF9…`).

#### 2. Commits, ramas y exposición pública

- `phase6-prefs-snapshot.xml` fue añadido en `f4c72a0` (2026-10-08 21:51:29 -0600,
  `feat: add configurable launcher logo`).
- `phase7-prefs-snapshot.xml` y los cuatro `p7-*.xml` fueron añadidos en `16eea76`
  (2026-10-10 10:41:37 -0600, `feat: add global device RAM widget`).
- En la revisión inicial, los seis estaban presentes en `HEAD` y en `origin/main`. Tras la
  publicación de `615852d`, ya no están en la punta de `main`; siguen en `f4c72a0` y `16eea76`.
- Solo `main` contiene estos commits: `git branch -a --contains f4c72a0` devuelve `main` y
  `origin/main`; ningún tag local o remoto contiene `f4c72a0` ni `16eea76`.
- El tag remoto `v1.6.0-stable` apunta a `5ec65d3`, anterior a la exposición.
- El repositorio es **público** (`private=false`). En la consulta del 2026-10-22: `forks=0`,
  `network=0`, `stars=1`.
- Se comprobó acceso público por URL cruda en tres de ellos: `phase6`, `phase7` y `p7-a`
  respondieron HTTP 200 con longitudes 12 408, 12 408 y 12 435 B, respectivamente.
- Los commits están fechados el 2026-10-08 (phase6) y el 2026-10-10 (las otras cinco copias);
  la presencia en `origin/main` está confirmada en la consulta del 2026-10-22. No se verificó la hora
  exacta del push, por lo que no se afirma una duración exacta de exposición.

#### 3. Copias adicionales

**Dentro del repositorio público (rastreadas):**

- Se encontraron **seis** XML rastreados con `launcher_state`, no dos:
  - los dos `*-prefs-snapshot.xml`;
  - los cuatro `p7-*.xml`.
- No hay otros XML rastreados con `launcher_state` (6 de 241 XML rastreados).
- No se encontraron snapshots de preferencias borrados en el historial; las altas de
  `launcher_state` en XML se concentran en `f4c72a0` y `16eea76`.
- Los cuatro `p7-*.xml` no son duplicados exactos, pero sí **variantes del mismo estado personal**:
  mismo conjunto de 87 paquetes, misma asignación de las 83 entradas de categoría, 20 paquetes en
  accesos del Home y las mismas dos referencias `content://` por archivo (la segunda varía entre tres
  valores).
- `p7-a.xml`, `p7-light.xml`, `p7-video.xml` y `p7-working.xml` están mencionados como “snapshots de
  config de prueba” en `docs/UI-1-PHASE7-REPORT.md:149`. Retirarlos exige actualizar esa referencia
  o sustituirla por evidencia redactada. `phase6` y `phase7` no aparecen referenciados en el árbol.

**Fuera de Git, en el working tree local:**

- Existen seis XML locales con `launcher_state` que **no están rastreados** y están cubiertos por
  `.git/info/exclude`:
  `phase8-original-prefs.xml`, `phase8-p02-prefs-before.xml`,
  `phase8-p09-prefs-before-restore.xml`, `phase8-pre-restore-current-prefs.xml`,
  `phase8-p09-device-after-restore.xml` y `docs/evidence_p8/original_launcher_prefs.xml`.
- Los cinco primeros contienen 86 entradas de categoría y un conjunto distinto de **90 paquetes**;
  el de `evidence_p8` contiene 87. No están publicados, pero no deben commitearse.
- `.git/info/exclude` es local y no se propaga a otros clones. Además, `.gitignore` **no** bloquea
  actualmente un futuro `docs/assets/*prefs*.xml`: `git check-ignore --no-index` devuelve código 1.
  Esto es una brecha de prevención, no una exposición actual.

#### 4. Tipos de información contenida

| Bloque | Contenido verificado | Clasificación |
|---|---|---|
| `launcher_state` | 87 paquetes distintos: 35 con prefijos asociados a sistema/Google/Xiaomi y 52 con otros prefijos; 83 asignaciones a categorías (17 Entretenimiento, 49 Finanzas, 17 Redes Sociales); 20 paquetes en accesos del Home; 16 paquetes aparecen en ambos conjuntos | **Datos personales confirmados; sensibilidad alta** por la huella de apps y la categoría financiera |
| `viewConfigs[].backgroundUri` | 12 URIs `content://` en total (dos por archivo; 11 de imagen y 1 de vídeo), autoridad `com.android.providers.media.documents`, con identificadores de documento codificados; sin HTTP, rutas `/storage/`, nombre de archivo, extensión, query, fragmento ni token; el URI de la vista 1 varía entre tres valores | **Dato personal confirmado; no es un secreto**: referencia a imágenes/vídeo seleccionados por el usuario; no permite recuperar el contenido sin el dispositivo/proveedor ni transfiere el permiso persistente |
| `viewConfigs[].quote` | Dos textos por archivo: uno de 27 caracteres, idéntico en los seis y distinto de los defaults actuales; otro de 20 caracteres que coincide con un default del código. El de 27 tiene 22 letras, 4 espacios, 1 símbolo matemático; sin dígitos, URL, email ni saltos de línea | **Potencialmente personal** el de 27 (podría ser un lema del usuario o un default de otra versión); el de 20 es un default. Ninguno es identificador directo |
| `viewConfigs[].diagonalShortcuts` / `sidebarShortcuts` / `gridPages` | 38 accesos totales: 20 con paquete y 18 placeholders por defecto; 20 etiquetas de apps reales y 18 etiquetas por defecto; sin iconos personalizados (`customAppIcons=0`) | Datos personales por los paquetes; las etiquetas son nombres de apps, no texto libre |
| `drawerConfig.categories` | 4 categorías con los ids/nombres por defecto; listas de paquetes pobladas: 17/49/17; todas las entradas son únicas | **Datos personales confirmados**: la pertenencia de apps a Finanzas/Redes/Entretenimiento revela uso e intereses |
| `hiddenAppsConfig` | `isConfigured=false`, `authMode=NONE`, `hiddenPackages=[]` en los seis archivos; el objeto solo tiene esos tres campos | **Sin datos de apps ocultas ni credenciales** en estas copias; no contiene campos de PIN, patrón, contraseña ni hash |
| `launcher_style_config` | 27 claves en phase6/phase7 y 28 en los `p7-*`; tema/color/logo; `logoText` es la marca “NOVA”; sin paquetes ni identificadores personales | Configuración de baja sensibilidad; en phase6/phase7 el `styleConfig` anidado y la clave independiente difieren en `logoMode`, `miButtonColor` y `adaptiveOverrides` (estado duplicado e inconsistente, no secreto) |
| `gesturesConfig`, `language`, `viewCount`, onboarding | Gestos en valores por defecto; `language=es`; `viewCount=2`; onboarding completado con paso 0; sin widgets nativos (`nativeWidgetIds=[]`) | Configuración genérica de baja sensibilidad |
| Nombres de apps y etiquetas | Nombres de aplicaciones y 18 etiquetas por defecto; sin dígitos, emails ni URLs en las etiquetas | No son texto personal libre; combinados con los paquetes sí identifican apps |

#### 4.1 Clasificación de campos por sensibilidad

| Nivel | Campos / rutas | Estado verificado en los seis | Criterio |
|---|---|---|---|
| **Alta** | `drawerConfig.categories[].packageNames`; `packageName` de `diagonalShortcuts` y `sidebarShortcuts`; `AppShortcut.id` de los accesos con paquete | 87 paquetes; 83 entradas de categoría; 20 paquetes en Home; 16 solapamientos; 20 IDs con `/` | Identificadores de apps instaladas y su organización; dato personal confirmado. No publicar. |
| **Alta/Media** | `viewConfigs[].backgroundUri`; `customAppIcons` (vacío); `AppShortcut.customIconUri` (vacío) | 12 URI (11 de imagen, 1 de vídeo); 0 iconos personalizados | Referencia a contenido seleccionado por el usuario; no es credencial; redactar o vaciar en fixtures. |
| **Media/Potencial** | `viewConfigs[].quote`; `AppShortcut.name` de apps reales | Dos citas por archivo (27 y 20 caracteres); la de 27 no coincide con los defaults actuales y la de 20 sí; 20 etiquetas de apps y 18 por defecto | Texto editable o nombres de apps; no identificador directo; sustituir por texto sintético. |
| **Baja** | `launcher_style_config`, `styleConfig`, `gesturesConfig`, `language`, `viewCount`, onboarding, `drawerConfig.categories[].id/name`, IDs/etiquetas por defecto, estructura de `gridPages` | Tema/colores/logo “NOVA”; gestos por defecto; `es`; 2 vistas; paso 0; 9 placeholders de grid por vista | Configuración genérica; no identifica por sí sola. |
| **Vacío / no presente** | `hiddenAppsConfig.hiddenPackages`; `hiddenAppsConfig.authMode`; `customAppIcons`; `nativeWidgetIds` | `[]`, `NONE`, `{}`, `[]` | Sin datos actuales. Si se pueblan: `hiddenPackages` alta; `authMode` media; iconos media/alta; widgets baja/media. |

En `gridPages` no hay paquetes reales: 9 placeholders por vista. Si se poblara con paquetes reales,
pasaría a sensibilidad alta.

Los niveles anteriores describen el riesgo de privacidad, no la presencia de credenciales. Una lista
vacía no autoriza a publicar el esquema: si en el futuro se rellena, cambia de nivel.

#### 4.2 URI `content://`: identificadores y permisos

- Hay **12 URI** en total (dos por archivo): 11 apuntan a imagen y 1 a vídeo. Todas usan la autoridad
  `com.android.providers.media.documents` y la forma `content://<authority>/document/<id>`.
- La ruta tiene dos segmentos; el identificador está codificado (`%3A`), mide 16 caracteres y es
  numérico. No hay nombre de archivo, extensión, ruta `/storage/`, host HTTP, puerto, usuario,
  contraseña, fragmento ni query string. El URI de la vista 0 es común a los seis archivos; el de la
  vista 1 presenta tres valores distintos.
- **No son secretos ni capacidades de acceso.** No incluyen token, firma, caducidad, `grant` ni
  credencial embebida. Son identificadores estables de documentos; el URI por sí solo no concede
  permiso a un tercero.
- El código de la app solicita permiso persistente de lectura con
  `takePersistableUriPermission(..., FLAG_GRANT_READ_URI_PERMISSION)` en
  `LauncherScreen.kt:405-407`, `SettingsScreen.kt:89-91` y `AppDrawerScreen.kt:594`. Ese permiso vive
  en el almacén de permisos de Android del dispositivo, no dentro del XML.
  `LauncherViewModel.kt:672` abre el flujo y depende de ese permiso.
- Por tanto, publicar el URI no transfiere el permiso. El riesgo es de **identificador personal y
  linkabilidad**, no de credencial filtrada. No se requiere rotar tokens ni revocar permisos por
  estos seis archivos; sí retirarlos o redactarlos. Si en el futuro un URI incluye query/token,
  apunta a un proveedor exportado o el dispositivo se pierde, se aplica la sección 8.6.
- `customAppIcons` está vacío; si se poblara, el mismo análisis se aplicaría a los URI de iconos.

#### 5. Clasificación explícita

**Datos personales confirmados**

- La lista de 87 paquetes de aplicaciones y su organización en categorías, incluidas 49 entradas
  en Finanzas y 17 en Redes Sociales.
- Los 20 paquetes usados en accesos del Home y su distribución entre las dos vistas.
- Las 12 referencias `content://` a contenido seleccionado por el usuario (11 imágenes y 1 vídeo).
- El layout y las preferencias de tema del launcher, en cuanto a configuración del dispositivo.

**Información potencialmente sensible, no confirmada como identificador personal**

- La cita de 27 caracteres: es texto editable por el usuario y no coincide con los defaults
  actuales; la segunda cita, de 20 caracteres, sí coincide con un default. La de 27 no contiene
  nombre, email, teléfono, URL ni otro identificador directo; no se puede determinar con estos datos
  si es un lema propio o un default de otra versión.
- Las referencias de fondo: revelan que existen imágenes/vídeo seleccionados y sus identificadores,
  pero no el contenido ni el permiso de acceso.
- La combinación de categorías (Finanzas/Redes) con la lista de apps permite inferir intereses y
  hábitos; es el núcleo del riesgo de perfilado.

**Secretos o credenciales confirmados**

- **Ninguno en estos seis archivos.** No se encontraron campos de credenciales ni valores con
  formato de contraseña, token, clave API/privada, JWT, cookie, email, MAC, UUID o ruta de archivo
  con los patrones aplicados.
- Las 12 URI `content://` **no se clasifican como secretos**: no contienen token, firma, query ni
  `grant`; son identificadores de documentos y el permiso persistente es local al dispositivo
  (sección 4.2). No requieren rotación.
- `hiddenAppsConfig` está vacío y en modo `NONE`; no contiene material del almacén
  `nova_hidden_apps_vault` ni credenciales de apps ocultas.
- Esta conclusión se limita a estos XML. No sustituye la auditoría de secretos de todo el
  repositorio, que corresponde a la Fase 5.

#### 6. Condición de fallo e impacto

La condición es simplemente que el repositorio siga siendo público y estos seis archivos
permanezcan en `main`. No hace falta ejecutar la app.

Impacto observado o razonablemente derivado:

- Huella de aplicaciones instaladas y clasificadas por el usuario, con especial sensibilidad por la
  categoría Finanzas.
- Inferencia de hábitos, servicios, país o intereses a partir de la combinación de apps.
- Material útil para phishing dirigido, ingeniería social o perfilado.
- Las referencias a imágenes/vídeo y la cita añaden contexto personal, aunque no permiten por sí
  solas recuperar los contenidos ni conceden acceso.
- No se ha demostrado compromiso de credenciales ni ejecución remota a partir de estos archivos.
- La exposición ya ocurrió en commits públicos; un borrado en un commit nuevo no elimina el
  historial anterior.

#### 7. Verificación reproducible

```powershell
# Presencia y copias
git ls-files | Select-String 'prefs|p7-'
git grep -l 'launcher_state' HEAD -- '*.xml'
git grep -l 'launcher_state' origin/main -- '*.xml'

# Identidad de phase6/phase7
git hash-object docs/assets/phase6-prefs-snapshot.xml
git hash-object docs/assets/phase7-prefs-snapshot.xml
git diff --no-index --ignore-cr-at-eol --stat -- `
  docs/assets/phase6-prefs-snapshot.xml docs/assets/phase7-prefs-snapshot.xml

# Commits y ramas
git log --all --follow --format='%H %ad %s' --date=iso -- docs/assets/phase6-prefs-snapshot.xml
git log --all --follow --format='%H %ad %s' --date=iso -- docs/assets/phase7-prefs-snapshot.xml
git log --all --find-object=44dc0a4610197596fe13548a659ff45197574e41 --oneline
git branch -a --contains f4c72a0
git tag --contains f4c72a0

# Comprobación de contenido sin imprimir valores
[xml]$x = Get-Content docs/assets/phase6-prefs-snapshot.xml -Raw
$j = ($x.map.string | Where-Object { $_.name -eq 'launcher_state' }).InnerText | ConvertFrom-Json
$cat = @(); foreach ($c in $j.drawerConfig.categories) { $cat += @($c.packageNames) }
$homeShortcuts = @(); foreach ($v in $j.viewConfigs) {
  $homeShortcuts += @($v.diagonalShortcuts) + @($v.sidebarShortcuts)
  foreach ($p in $v.gridPages) { $homeShortcuts += @($p) }
}
$homePkgs = @($homeShortcuts | Where-Object { $_.packageName } | ForEach-Object { $_.packageName })
@($cat + $homePkgs | Sort-Object -Unique).Count
```

Resultados observados: 87 paquetes únicos; 83 entradas de categoría; 20 paquetes de Home; 16 de
solapamiento; seis XML rastreados con `launcher_state`; dos blobs idénticos para phase6/phase7.

#### 8. Propuesta final de remediación y ejecución local

> **Ejecución y publicación (2026-10-22):** la parte A —retirada, reglas de `.gitignore` y actualización documental— se ejecutó, verificó y publicó en `origin/main` con el commit `615852d8383c20f42c2ef04f47044dc650ea263c` mediante push normal. La parte B —tratamiento del historial— no se ejecutó. El detalle y las comprobaciones están en las secciones 9 y 10.

**8.1 — Alcance y decisión**

La remediación se divide en dos decisiones independientes:

- **A. Árbol actual y prevención:** retirar los seis XML de la versión publicada, impedir que vuelvan
  a entrar y sustituir la evidencia por fixtures saneados.
- **B. Historial:** decidir si se reescribe para eliminar la exposición de los commits antiguos.

La decisión A es necesaria aunque no se reescriba el historial. No se encontraron credenciales ni URI
con capacidad de acceso; por tanto, no hay rotación de secretos asociada a estos seis archivos
(sección 8.6). La parte A se ejecutó y publicó el 2026-10-22; la parte B no se ejecutó.

Orden ejecutado para la parte A: inventario y verificación → retirada del índice → `.gitignore` →
actualización documental → verificación. Quedan pendientes los fixtures saneados y la
decisión/ejecución del historial. No se creó un respaldo externo porque `git rm --cached` conserva
las copias locales y los blobs siguen en el historial; la tarea prohibía copias innecesarias.

**8.2 — Retirada de la versión actual (publicada en main)**

1. Se retiraron los seis XML del índice sin borrar las copias locales con `git rm --cached` sobre
   los seis paths.
2. Se actualizaron `.gitignore` y `docs/UI-1-PHASE7-REPORT.md:149` para que la documentación no
   enlace los XML crudos y use una descripción redactada.
3. Se verificó que el índice no incluye los XML locales `phase8-*` ni `docs/evidence_p8/`.
4. Se creó un único commit, `615852d8383c20f42c2ef04f47044dc650ea263c`, y se publicó con
   `git push origin main` normal; el commit no incluye el informe ni `benchmark/build/` (sección 10).

No se creó un respaldo externo: `git rm --cached` conserva la copia de trabajo y los blobs siguen
en el historial; la tarea prohibía copias innecesarias. Las seis copias locales se conservaron y se
verificaron por tamaño (sección 9).

Comandos ejecutados:

```powershell
$paths = @(
    'docs/assets/phase6-prefs-snapshot.xml'
    'docs/assets/phase7-prefs-snapshot.xml'
    'docs/assets/p7-a.xml'
    'docs/assets/p7-light.xml'
    'docs/assets/p7-video.xml'
    'docs/assets/p7-working.xml'
)
git rm --cached -- $paths
git add -- .gitignore docs/UI-1-PHASE7-REPORT.md
git commit -m "chore(privacy): remove device preference snapshots"
git push origin main
```

El `git add` fue específico, no `git add -A`. El commit y el push se registran en la sección 10.
`git rm --cached` retira los XML del árbol de Git pero deja las copias locales; con las reglas de
`.gitignore` quedarán fuera de futuros commits. No usar `git rm` sin `--cached` mientras se quiera
conservar la copia local.

**Criterio de aceptación de esta etapa:** `git ls-files` no muestra ninguno de los seis paths;
`docs/UI-1-PHASE7-REPORT.md:149` ya no enlaza los XML crudos; el commit publicado no incluye
`phase8-*`, `docs/evidence_p8/`, el informe de auditoría ni `benchmark/build/`. Verificado en las
secciones 9 y 10.

**8.3 — Prevención y fixtures saneados**

1. Se añadieron patrones versionables al `.gitignore` (sección 9):
   `docs/assets/*prefs*.xml`, `docs/assets/p7-a.xml`, `docs/assets/p7-light.xml`,
   `docs/assets/p7-video.xml`, `docs/assets/p7-working.xml`, `docs/assets/phase8-*.xml`,
   `docs/evidence_p8/`.
2. Se recomienda añadir un guard de pre-commit y CI que rechace cualquier XML que contenga
   `<string name="launcher_state">` o `launcher_style_config`, aunque el nombre no coincida. No se
   añadió script ni dependencia nueva; el guard queda documentado abajo.
3. Documentar que `.git/info/exclude` es solo local y no sustituye a las reglas versionables del
   `.gitignore`.
4. Revisar capturas, dumps y logs antes de publicarlos; pueden contener paquetes, rutas o textos.

Patrones aplicados en el `.gitignore` del árbol actual:

```gitignore
# NL-AUD-002 — snapshots de preferencias reales del dispositivo
# Alcance limitado a docs/assets/ y docs/evidence_p8/; no oculta recursos funcionales
# ni fixtures de prueba saneados.
docs/assets/*prefs*.xml
docs/assets/p7-a.xml
docs/assets/p7-light.xml
docs/assets/p7-video.xml
docs/assets/p7-working.xml
docs/assets/phase8-*.xml
docs/evidence_p8/
```

Los archivos ya rastreados no se ignoran solos: `git rm --cached` sigue siendo obligatorio. Los
fixtures saneados no deben guardarse en `docs/assets/`; si se colocan en
`app/src/test/resources/fixtures/` o `app/src/androidTest/assets/fixtures/`, estos patrones no los
ocultan.

Guard de contenido propuesto (pre-commit y CI), independiente del nombre del archivo:

```powershell
$bad = @()
$xmlFiles = @(
    git diff --cached --name-only --diff-filter=ACMR -- '*.xml'
    git ls-files '*.xml'
) | Sort-Object -Unique | Where-Object {
    $_ -notmatch '^(app/src/test/resources/fixtures/|app/src/androidTest/assets/fixtures/)'
}
foreach ($f in $xmlFiles) {
    if (Select-String -Path $f -Pattern 'launcher_state|launcher_style_config' -Quiet) {
        $bad += $f
    }
}
if ($bad.Count -gt 0) {
    throw "XML crudo de preferencias en el repositorio: $($bad -join ', ')"
}
```

Para pre-commit, la primera consulta cubre los XML preparados; para CI, `git ls-files` cubre el
árbol versionado. El guard excluye solo las rutas de fixtures saneados; cualquier otro XML con esos
marcadores falla. No requiere dependencias nuevas. Complementar con un escáner de secretos (por
ejemplo, `gitleaks`) para tokens, claves y credenciales; el guard anterior protege datos de
dispositivo, no secretos.

No se crearon fixtures saneados en esta ejecución; la recomendación queda pendiente para una fase
posterior.

**Fixtures de prueba saneados**

- Preferible: construir el estado con un builder en el test, sin XML versionado.
- Si hace falta un XML de compatibilidad, usar solo datos sintéticos: paquetes `com.example.*` o
  `com.test.*`; `backgroundUri` vacío o `content://example.invalid/document/test-image`; cita
  `"Test quote"` o vacía; `hiddenPackages=[]` y `authMode=NONE`; `customAppIcons={}` y
  `nativeWidgetIds=[]`; IDs de accesos y etiquetas sintéticos.
- No incluir paquetes reales, nombres de apps reales, la cita de 27 caracteres, IDs de documento
  reales, la autoridad `com.android.providers.media.documents`, IDs de widgets del dispositivo,
  rutas locales, emails, timestamps ni datos de cuenta.
- Ubicarlos en `app/src/test/resources/fixtures/` o `app/src/androidTest/assets/fixtures/`, con un
  `README` que declare “datos sintéticos”; nunca en `docs/assets/`.
- **Criterio de aceptación:** los fixtures no contienen la lista de 87 paquetes, la cita real, la
  lista de apps ocultas ni URI del proveedor de medios; los tests usan conjuntos pequeños y
  sintéticos.

**8.4 — Historial: privacidad y coordinación**

Hechos verificados: el repositorio es público; `forks=0` y `network=0` en la consulta; ningún tag
contiene los commits de exposición; solo `main` contiene esos commits; hay un commit local por
delante de `origin/main` y worktrees locales. El borrado en HEAD no elimina la historia.

| Dimensión | Solo HEAD | Reescritura |
|---|---|---|
| **Privacidad** | Los commits antiguos siguen accesibles por SHA, clones y URLs crudas; la fuga persiste | Elimina los paths de las referencias reescritas, pero no de clones, forks, cachés, artefactos de CI ni copias ya descargadas; los SHAs antiguos pueden seguir visibles temporalmente |
| **Coordinación** | Baja: un commit nuevo; no cambian SHAs | Alta: cambian todos los descendientes; force-push, re-clonado/reset, revisión de tags, releases, PRs, badges, firmas y CI |
| **Tags y forks** | No hay tags que contengan la exposición; `forks=0` en la consulta | Los tags deben revisarse/recrearse; los forks existentes conservan su historia y no se pueden purgar desde este repositorio |
| **Resultado** | Rápido y seguro para el árbol actual, insuficiente para la exposición histórica | Mayor reducción de exposición, sin garantía de borrado retroactivo |

- **Opción A — solo HEAD:** retirar los seis archivos del árbol actual (realizado y publicado en
  `main` con `615852d`). Es rápido y no rompe SHAs, pero los commits antiguos siguen accesibles y
  la fuga persiste en el historial público.
- **Opción B — reescribir historia (recomendada si se quiere eliminar la exposición):** purgar los
  seis paths de todas las referencias con `git filter-repo --invert-paths` o BFG, force-push
  coordinado y verificación posterior. Dado que hay 49 apps en Finanzas y 17 en Redes, la
  exposición es suficientemente sensible como para justificar esta opción.
- **Opción C — contención temporal:** hacer el repositorio privado o archivarlo mientras se decide.
  Reduce la exposición futura, pero no elimina copias ya públicas, forks ni cachés.

**Recomendación:** la retirada y la prevención ya se publicaron en `main`; falta la decisión sobre
el historial. Si el repositorio va a seguir público y la huella de 87 apps (especialmente Finanzas y
Redes) sigue siendo inaceptable, reescribir el historial está justificado como medida de privacidad.
No se justifica solo por las URI:
son identificadores sin capacidad de acceso. Si no se reescribe, hacer el repositorio privado o
archivarlo y documentar formalmente el riesgo residual; no declarar erradicación. La reescritura no
sustituye la revocación de credenciales si alguna aparece (sección 8.6).

**8.4.1 — Costes de coordinación y límites de la reescritura**

- Cambian los SHAs de todos los commits descendientes de la primera introducción; los enlaces,
  badges, tags, releases y referencias a commits antiguos dejan de ser válidos.
- Hay que force-push a `main`; los colaboradores deben re-clonar o hacer un reset cuidadoso. Los
  commits firmados pierden la firma válida.
- Los tags y ramas deben revisarse o recrearse. En este repositorio, ningún tag contiene los
  commits de exposición, pero `main` y sus descendientes sí cambian.
- GitHub puede conservar objetos inalcanzables o vistas cacheadas durante un tiempo; puede requerir
  soporte para purgar cachés. Los forks conservan su propia historia; en la consulta actual hay
  `forks=0`, pero eso puede cambiar.
- Los artefactos de CI, cachés y copias locales de otros equipos no se pueden purgar desde el
  repositorio.
- No hay secretos que rotar por estos seis archivos, pero los datos personales ya no pueden
  “despublicarse” retroactivamente; valorar si corresponde comunicación o medida legal.
- Antes de reescribir: espejo/backup del repositorio, ventana de coordinación y bloqueo temporal de
  pushes.

El procedimiento reproducible y no ejecutado está en la sección 11. El siguiente bloque es solo un
esbozo histórico y no debe usarse sin el respaldo, el aislamiento y las comprobaciones de esa sección:

```powershell
git filter-repo --invert-paths `
  --path docs/assets/phase6-prefs-snapshot.xml `
  --path docs/assets/phase7-prefs-snapshot.xml `
  --path docs/assets/p7-a.xml `
  --path docs/assets/p7-light.xml `
  --path docs/assets/p7-video.xml `
  --path docs/assets/p7-working.xml
git push --force-with-lease origin main
```

No se usará BFG en este plan. No ejecutar el force-push sin coordinar clones, ramas y tags.

**8.5 — Verificación y criterios de aceptación**

Comandos de comprobación para Windows PowerShell. La parte A (índice, prevención, commit y push) se
ejecutó; los comandos de reescritura de historial, clon limpio y Gradle no se ejecutaron en esta
fase:

```powershell
$paths = @(
    'docs/assets/phase6-prefs-snapshot.xml'
    'docs/assets/phase7-prefs-snapshot.xml'
    'docs/assets/p7-a.xml'
    'docs/assets/p7-light.xml'
    'docs/assets/p7-video.xml'
    'docs/assets/p7-working.xml'
)

# Índice actual y punta de main: debe quedar vacío
git ls-files -- $paths
# Tras el commit, HEAD ya no contiene los seis; los commits históricos sí.
git grep -l 'launcher_state' HEAD -- 'docs/assets/*.xml'

# Prevención: debe imprimir la regla que cubre el ejemplo
git check-ignore --no-index -v docs/assets/example-prefs.xml

# Historial (no ejecutado en esta fase; referencia)
$blobs = @(
    '44dc0a4610197596fe13548a659ff45197574e41'
    '28ea1cf6150f5841e1aa5e14d0b6c1e8f47954fe'
    '61aa3eccebb118600acfe0f3dfca9121c9da41b6'
    '2e9e3dfa9a5c2e01661cb4bd949ab1fd2a0e35af'
    '4d85d235493cfb3be057e1374ad14878bef085a5'
)
foreach ($b in $blobs) { git log --all --find-object=$b --oneline }
git rev-list --objects --all | Select-String '44dc0a46|28ea1cf6|61aa3ecc|2e9e3dfa|4d85d235'

# Estado (Gradle no ejecutado: no hay cambios funcionales)
git status --porcelain=v1 -uall
# .\gradlew.bat test
```

En la verificación previa al commit, `git grep` sobre `HEAD` encontraba los seis XML. Tras la
publicación de `615852d`, `HEAD` ya no los contiene; los commits históricos `f4c72a0` y `16eea76`
siguen expuestos. En un clon limpio tras la reescritura no debe encontrar ninguno. No se ejecutó
`.\gradlew.bat test`: no se modificó código funcional y ejecutarlo
alteraría los artefactos preexistentes de `benchmark/build/`. Un clon limpio debe repetir las
comprobaciones (no ejecutado en esta fase):

```powershell
$verify = Join-Path $env:TEMP 'nova-verify'
$remote = 'https://github.com/Mikies21-Programmer/NovaLauncher.git'
git clone $remote $verify
git -C $verify ls-files -- $paths
git -C $verify grep -l 'launcher_state' HEAD -- 'docs/assets/*.xml'
```

Criterios de aceptación:

| Área | Criterio |
|---|---|
| **Árbol actual** | Ninguno de los seis paths en `git ls-files` ni en la punta de `main`; las copias locales existen y quedan ignoradas; los commits históricos todavía los contienen; `UI-1-PHASE7-REPORT.md` actualizado; no se añaden `phase8-*` ni `docs/evidence_p8/` |
| **Prevención** | Reglas de `.gitignore` incluidas en `main`; cubren los nombres y el guard recomendado rechaza un XML crudo aunque tenga otro nombre; `.git/info/exclude` no es el único control |
| **Fixtures** | Pendiente: no se crearon fixtures saneados en esta fase; solo datos sintéticos, sin paquetes reales, URI del proveedor de medios, cita real, apps ocultas, iconos ni widgets del dispositivo; tests verdes pendientes |
| **Historial** | Si se reescribe: ningún path/blob en las referencias y clon limpio sin XML; URLs crudas antiguas sin contenido. Si no se reescribe: repositorio privado/archivado y riesgo residual aceptado por escrito |
| **Estado** | `NL-AUD-002` no se cierra hasta decidir/verificar el historial; la retirada y la prevención ya están publicadas en `main`; no hay rotación de secretos por estos XML |

**8.6 — Credenciales, tokens o URI con acceso**

Si una revisión posterior encuentra una credencial, un token o un URI que **conceda acceso**, la
respuesta cambia: la reescritura del historial no revoca nada por sí sola.

1. **Detener la publicación** y no copiar el valor en el informe, un issue o un chat.
2. **Revocar/rotar en el emisor:** contraseña, clave API, token OAuth/JWT, cookie de sesión, clave
   privada o de firma. Invalidar sesiones y actualizar CI/secrets.
3. **Asumir compromiso:** revisar logs de acceso, usos anómalos y alcance; notificar según
   corresponda. La rotación es obligatoria aunque se reescriba la historia.
4. **Retirar del árbol y del historial,** purgar cachés/forks cuando sea posible y verificar que el
   valor antiguo ya no autentica.
5. **URI con acceso:** si es una URL firmada o con token, revocar/expirar el enlace, rotar la clave
   de firma y desactivar la compartición; si es un `content://` con permiso persistente, liberar el
   permiso en el dispositivo (`releasePersistableUriPermission`), limpiar datos de la app o revocar
   la compartición del proveedor. Si el dispositivo se perdió, aplicar borrado remoto.
6. **Casos actuales:** los seis XML no contienen credenciales ni URI con capacidad de acceso; las 12
   URI son identificadores de documento sin token ni `grant` (sección 4.2). No requieren rotación;
   sí retirada/redacción. Si en el futuro se rellena `hiddenAppsConfig.hiddenPackages`, se eleva a
   sensibilidad alta; `authMode` distinto de `NONE` es media, y el almacén
   `nova_hidden_apps_vault` se trata por separado.

**8.7 — Estado**

`NL-AUD-002` permanece **P1 — PENDIENTE DE REMEDIACIÓN**. La retirada, las reglas de `.gitignore` y
la actualización documental se ejecutaron, verificaron y publicaron en `origin/main` con el commit
`615852d8383c20f42c2ef04f47044dc650ea263c`; el push fue normal. La reescritura del historial no se
ejecutó. Los seis ya no están en la punta de `main`; siguen presentes en `f4c72a0` y `16eea76`. El
plan de purga histórica está documentado en la sección 11; no se ejecutó. No se marcará como resuelto
hasta decidir y verificar el tratamiento del historial (o aceptar formalmente el riesgo residual si
no se reescribe).

**Riesgo de regresión**

La retirada afecta a la documentación de la Fase 7 que citaba `p7-*.xml`, no al código de la app ni
a la compilación. La reescritura de historia afecta a SHAs, clones, tags y enlaces; debe hacerse con
autorización explícita. No se modificó código, recursos funcionales, Gradle ni manifiesto; se
retiraron seis XML de evidencia y se modificaron `.gitignore`, `docs/UI-1-PHASE7-REPORT.md` y este
informe.

#### 9. Verificación previa al commit (2026-10-22)

Esta sección registra el estado verificado antes de publicar; el estado posterior al push está en la
sección 10.

**Estado inicial registrado:** `HEAD` `1c17d38c9650740e2b9596d2d8e992d6a17270af`, `origin/main`
`f1b088bc941070c99ee50929b45d40312c1d5999`; 216 entradas de estado = 215 artefactos preexistentes
de `benchmark/build/` + este informe no rastreado. Los seis blobs coincidían con la sección 1.

**Archivos y cambios verificados:**

| Archivo / ámbito | Cambio | Estado |
|---|---|---|
| Seis XML de `docs/assets/` | Retirados del índice con `git rm --cached`; copias locales intactas | Eliminaciones preparadas y luego publicadas en `615852d` |
| `.gitignore` | Reglas NL-AUD-002 para `docs/assets/` y `docs/evidence_p8/` | Incluido en `615852d` |
| `docs/UI-1-PHASE7-REPORT.md` | Línea 149 sustituye los nombres de los XML por una descripción redactada | Incluido en `615852d` |
| `AUDIT_NOVALAUNCHER_2026-10-22.md` | Este apartado y corrección de estados | No rastreado |
| Código, recursos funcionales, Gradle, manifiesto | Sin cambios | Verificado |
| `phase8-*`, `docs/evidence_p8/` | No añadidos | Verificado; 44 rutas `docs/assets/phase8-*` ya rastreadas, sin cambios |

**Comandos y resultados previos al commit:**

| Comprobación | Resultado |
|---|---|
| `git ls-files -- <seis>` | 0 paths |
| `git status --porcelain=v1 -uall` | 224 entradas: 215 `benchmark/build/` preexistentes + 8 cambios preparados + este informe no rastreado |
| `git diff --cached --name-status` | Exactamente `.gitignore`, `docs/UI-1-PHASE7-REPORT.md` y las seis eliminaciones |
| `git diff --cached --check` | Sin errores (exit 0) |
| `git check-ignore --no-index -v` | Los seis coinciden con reglas; `docs/assets/example-prefs.xml` coincide con `*prefs*.xml`; `docs/evidence_p8/` coincide; `AndroidManifest.xml`, los tres XML funcionales de `app/src/main/res/xml/` y un fixture saneado hipotético no coinciden |
| `git diff --cached --name-only -- app/src ...` | 0 cambios en código, recursos, Gradle o manifiesto |
| `git diff --cached --name-only` filtrado por `phase8|evidence_p8` | 0 |
| `Get-Item` de los seis XML | Existen; tamaños 12 413, 12 408, 12 435, 12 439, 12 439 y 12 444 B, iguales a los iniciales |
| `git ls-tree -r HEAD -- <seis>` | 6 paths antes del commit; 0 después de `615852d` (sección 10) |
| `git grep -l launcher_state HEAD -- 'docs/assets/*.xml'` | 6 coincidencias antes del commit; 0 después (sección 10) |
| `git cat-file -e <blobs>` | Los cinco blobs existen |
| `git log --all --format=%h -- <seis>` | 3 commits tocan los paths: `615852d` (retirada) + `16eea76` + `f4c72a0` |
| `git rev-parse HEAD` / `origin/main` | Iniciales; después de la publicación ambos = `615852d` (sección 10) |

No hay entradas no rastreadas fuera de `benchmark/build/` y este informe.

**Limitaciones y pendientes:**

- La retirada ya está publicada en `main`; los commits históricos `f4c72a0` y `16eea76` siguen
  conteniendo los seis XML. La reescritura del historial no se ejecutó.
- Las copias locales se conservan y quedan ignoradas; no se borraron ni se creó un respaldo externo.
- El guard de contenido está documentado en 8.3, pero no se añadió como script de CI/pre-commit ni
  con dependencias nuevas.
- No se ejecutaron Gradle ni tests: no se modificó código funcional y se preservaron los 215
  artefactos preexistentes de `benchmark/build/`. No se verificó un clon limpio.
- No se crearon fixtures saneados ni se modificó documentación funcional distinta de la línea
  redactada.
- `NL-AUD-002` no se declara resuelto: la retirada ya está publicada, pero falta decidir y verificar
  el tratamiento del historial (o aceptar formalmente el riesgo residual).

**Prevención recomendada:** el guard de contenido de la sección 8.3, sin dependencias nuevas, debe
ejecutarse en pre-commit sobre los XML preparados y en CI sobre `git ls-files '*.xml'`; el
`.gitignore` es una barrera de nombre, no una garantía de contenido.

#### 10. Publicación y verificación remota (2026-10-22)

**Commit de remediación:** `615852d8383c20f42c2ef04f47044dc650ea263c`, mensaje
`chore(privacy): remove device preference snapshots`; padre `1c17d38c9650740e2b9596d2d8e992d6a17270af`.
El commit contiene exactamente 8 archivos: `.gitignore`, `docs/UI-1-PHASE7-REPORT.md` y las seis
eliminaciones. No contiene el informe de auditoría ni `benchmark/build/`.

**Push:** `git push origin main` normal, sin `--force`; resultado `f1b088b..615852d main -> main`.
El push publicó también el commit local anterior `1c17d38`; no se eliminó ni reescribió. `HEAD` y
`origin/main` quedaron en `615852d8383c20f42c2ef04f47044dc650ea263c`, con 0 commits de diferencia.
`git status -sb` mostró `## main...origin/main`, sin indicación de commits pendientes.

**Verificación en la punta remota:**

- `git ls-remote origin refs/heads/main` devolvió `615852d8383c20f42c2ef04f47044dc650ea263c`.
- `git ls-tree -r --name-only 615852d -- <seis>` devolvió 0 paths.
- Las reglas de `.gitignore` están presentes en `615852d`; la descripción redactada de la Fase 7
  también.
- El informe de auditoría sigue sin rastrear y `benchmark/build/` sigue fuera del commit.

**Exposición histórica:** `git log --all --format=%h -- <seis>` incluye `615852d` (la retirada),
`16eea76` y `f4c72a0`. `git grep` encontró XML crudos en `f4c72a0` (1) y `16eea76` (6), pero 0 en
`HEAD`. La retirada corrige la punta actual; no elimina la exposición de los commits históricos.

**Estado local posterior:** `git status --porcelain` devolvió 216 entradas: 215 artefactos
preexistentes de `benchmark/build/` + este informe no rastreado. Las seis copias locales siguen
existiendo y están ignoradas. No se usaron `--force`, rebase, reset, `git filter-repo` ni BFG.

#### 11. Plan controlado de purga histórica (solo lectura, no ejecutado)

**Estado:** plan documentado el 2026-10-22. No se creó ningún clon, no se ejecutó `git filter-repo`
ni BFG, no se ejecutó ningún push y no se modificaron referencias. El repositorio principal, sus
ramas, tags, worktrees, los 215 artefactos de `benchmark/build/` y este informe permanecen intactos.
La Fase 1 de la auditoría sigue pendiente.

##### 11.1 Alcance verificado

| Referencia | SHA / destino | ¿Contiene `f4c72a0` o `16eea76`? | Nota |
|---|---|---|---|
| `refs/heads/main` local | `615852d8383c20f42c2ef04f47044dc650ea263c` | Sí | Rama de trabajo principal |
| `refs/remotes/origin/main` y `origin/HEAD` | `615852d8383c20f42c2ef04f47044dc650ea263c` | Sí | Seguimiento del remoto |
| Remoto `refs/heads/main` | `615852d8383c20f42c2ef04f47044dc650ea263c` | Sí | Única rama remota con exposición |
| Remoto `refs/tags/v1.6.0-stable` | tag `84f0ed01b2ae20020cfacc633252f4d85ab36cf5` → commit `5ec65d379149067e350410ec4e7fff03e295de68` | No | Tag anotado; su commit es ancestro de `f4c72a0`; 0 paths de los seis |
| Remoto `refs/pull/*/head` | — | No | `git ls-remote` devolvió 0 refs de PR |
| Ramas locales de respaldo | `eb09b71f…`, `f7f026f1…` | No | `git for-each-ref --contains f4c72a0` solo devolvió `main`/`origin` |
| Tags locales de respaldo | `2ce454e3…`, `f7f026f1…` | No | No contienen los commits afectados |
| Worktree `nova-baseline` | `5ec65d379149067e350410ec4e7fff03e295de68` | No | Detached; anterior a `f4c72a0` |
| Forks de GitHub | 0 (`forks_count` y `network_count`) | No | Dato de la API en esta revisión; pueden aparecer forks después |

**Commits afectados por la purga de los seis paths:** 5 commits, todos en la cadena de `main`.
`git log --all --full-history -- <seis>` devolvió exactamente 3 commits que tocan los paths
(`f4c72a0`, `16eea76`, `615852d`); la purga reescribe esos 3 y los 2 descendientes, total 5.
Cualquier commit descendiente del primero que introdujo un path cambia de SHA aunque su árbol
efectivo no cambie, porque cambia su padre.

| Commit | Fecha | Mensaje | Papel |
|---|---|---|---|
| `f4c72a0b44439a44435ddc2facf348ae7d71c53f` | 2026-10-08 | `feat: add configurable launcher logo` | Introduce `phase6-prefs-snapshot.xml` |
| `16eea76da84fead3ce5f60b3be07bcd6f3c246fe` | 2026-10-10 | `feat: add global device RAM widget` | Introduce `phase7-prefs-snapshot.xml` y los cuatro `p7-*.xml` |
| `f1b088bc941070c99ee50929b45d40312c1d5999` | 2026-10-17 | `feat: complete adaptive theme settings UI (UI-1 phase 8)` | Descendiente; se reescribe por cambio de padre |
| `1c17d38c9650740e2b9596d2d8e992d6a17270af` | 2026-10-17 | `docs: remove duplicated header in Phase 8 audit report` | Descendiente; se reescribe |
| `615852d8383c20f42c2ef04f47044dc650ea263c` | 2026-10-22 | `chore(privacy): remove device preference snapshots` | Elimina los seis; también cambia `.gitignore` y documentación |

**Blobs afectados:** 5 blobs únicos para 6 paths. `phase6` y `phase7` comparten el mismo blob.

| Path(s) | Blob | Bytes |
|---|---|---:|
| `docs/assets/phase6-prefs-snapshot.xml` y `docs/assets/phase7-prefs-snapshot.xml` | `44dc0a4610197596fe13548a659ff45197574e41` | 12 408 |
| `docs/assets/p7-a.xml` | `28ea1cf6150f5841e1aa5e14d0b6c1e8f47954fe` | 12 435 |
| `docs/assets/p7-light.xml` | `61aa3eccebb118600acfe0f3dfca9121c9da41b6` | 12 439 |
| `docs/assets/p7-video.xml` | `2e9e3dfa9a5c2e01661cb4bd949ab1fd2a0e35af` | 12 439 |
| `docs/assets/p7-working.xml` | `4d85d235493cfb3be057e1374ad14878bef085a5` | 12 439 |

Los tamaños son los del blob en Git; las copias de trabajo pueden variar por finales de línea. El
historial actual de `main` tiene 43 commits. Con `--prune-empty=never`, la historia reescrita debe
conservar 43 commits y reescribir solo los 5 afectados. Los SHA nuevos no se conocen hasta ejecutar
el ensayo; no se inventan en este informe.

##### 11.2 Estrategia de aislamiento

1. No ejecutar la purga en `C:\Users\migue\AndroidStudioProjects\AnimeLauncher` ni en el worktree
   `nova-baseline`. Un `git worktree` comparte el almacén de objetos y refs; no sirve como aislamiento.
2. Crear un **clon espejo de trabajo** independiente, fuera del repositorio principal, y un **clon
   espejo de respaldo** separado. El repositorio principal no se toca.
3. Conservar el clon de respaldo sin reescribir; usarlo para comparar y restaurar si el ensayo falla.
4. Filtrar únicamente los seis paths explícitos, sin globs amplios, en el clon de trabajo.
5. No ejecutar force-push en esta fase. La publicación requerirá una autorización humana separada.

##### 11.3 Copia de seguridad verificable

Comandos propuestos para una fase autorizada; **no ejecutados ahora**:

```powershell
$ErrorActionPreference = 'Stop'
$Remote = 'https://github.com/Mikies21-Programmer/NovaLauncher.git'
$Root = 'C:\Users\migue\AndroidStudioProjects\NovaLauncher-purge-20261022'
$Bundle = Join-Path $Root 'NovaLauncher-all-refs-20261022.bundle'
$Backup = Join-Path $Root 'NovaLauncher-backup-20261022.git'
$Work = Join-Path $Root 'NovaLauncher-work-20261022.git'

New-Item -ItemType Directory -Force -Path $Root | Out-Null

# 1) Bundle de todas las refs del repositorio principal, sin tocar refs.
git bundle create $Bundle --all
git bundle verify $Bundle
Get-FileHash $Bundle -Algorithm SHA256 | Format-List

# 2) Clon espejo de respaldo del remoto, sin reescribir.
git clone --mirror $Remote $Backup
git -C $Backup fsck --full
git -C $Backup show-ref | Set-Content (Join-Path $Root 'backup-refs.txt')

# 3) Clon de trabajo independiente a partir del respaldo, con --no-local.
git clone --mirror --no-local $Backup $Work
git -C $Work fsck --full
git -C $Work rev-parse refs/heads/main
git -C $Work rev-parse 'refs/tags/v1.6.0-stable^{}'
```

Criterios de la copia: el bundle debe verificar; el clon de respaldo debe pasar `git fsck --full`;
`main` debe ser `615852d` y el tag debe resolver a `5ec65d3`. Guardar el SHA-256 del bundle y la
salida de `show-ref` fuera del repositorio. **El bundle y el clon de respaldo contienen los blobs
sensibles**: deben almacenarse cifrados, con acceso restringido, y no subirse a GitHub. El bundle no
incluye el informe no rastreado ni los 215 artefactos de `benchmark/build/`; no se deben tocar.

##### 11.4 Ensayo de la purga en el clon de trabajo

Comando propuesto; **no ejecutado**. Verificar antes la versión de `git-filter-repo`; si no está
instalado, usar un entorno aislado y registrar la versión. No usar `--force` para saltar la
comprobación de clon fresco.

```powershell
$Paths = @(
    'docs/assets/phase6-prefs-snapshot.xml'
    'docs/assets/phase7-prefs-snapshot.xml'
    'docs/assets/p7-a.xml'
    'docs/assets/p7-light.xml'
    'docs/assets/p7-video.xml'
    'docs/assets/p7-working.xml'
)

git -C $Work filter-repo --sensitive-data-removal --invert-paths `
  --prune-empty=never --preserve-commit-hashes --preserve-commit-encoding `
  --path 'docs/assets/phase6-prefs-snapshot.xml' `
  --path 'docs/assets/phase7-prefs-snapshot.xml' `
  --path 'docs/assets/p7-a.xml' `
  --path 'docs/assets/p7-light.xml' `
  --path 'docs/assets/p7-video.xml' `
  --path 'docs/assets/p7-working.xml'

$GitDir = git -C $Work rev-parse --absolute-git-dir
$NewMain = git -C $Work rev-parse refs/heads/main
$NewTag = git -C $Work rev-parse refs/tags/v1.6.0-stable
Write-Output "new_main=$NewMain"
Write-Output "new_tag=$NewTag"
Copy-Item (Join-Path $GitDir 'filter-repo\commit-map') (Join-Path $Root 'commit-map.txt')
Copy-Item (Join-Path $GitDir 'filter-repo\ref-map') (Join-Path $Root 'ref-map.txt')
```

Si `filter-repo` rechaza el clon por no considerarlo fresco, **detenerse** y revisar; no forzar. Si
alguna opción no existe en la versión instalada, detenerse y documentar la diferencia; no improvisar
una purga alternativa en el repositorio principal. No hacer push desde el clon de trabajo en esta
fase.

##### 11.5 Comprobaciones posteriores al ensayo

```powershell
# 1) Los seis paths no deben aparecer en ninguna ref reescrita.
git -C $Work log --all --full-history --name-status -- $Paths
git -C $Work rev-list --objects --all -- $Paths
git -C $Work ls-tree -r --name-only refs/heads/main -- $Paths

# 2) Deben quedar solo main y el tag; el tag no debe cambiar.
git -C $Work for-each-ref --format='%(refname)|%(objectname)|%(objecttype)'
git -C $Work rev-parse 'refs/tags/v1.6.0-stable^{}'

# 3) El árbol de la punta debe ser idéntico al árbol actual de 615852d.
$OldTree = git -C $Backup rev-parse '615852d8383c20f42c2ef04f47044dc650ea263c^{tree}'
$NewTree = git -C $Work rev-parse 'refs/heads/main^{tree}'
if ($OldTree -ne $NewTree) { throw 'El árbol de la punta cambió fuera de los seis paths' }

# 4) Los cinco blobs antiguos deben haber desaparecido del clon de trabajo.
$OldBlobs = @(
    '44dc0a4610197596fe13548a659ff45197574e41'
    '28ea1cf6150f5841e1aa5e14d0b6c1e8f47954fe'
    '61aa3eccebb118600acfe0f3dfca9121c9da41b6'
    '2e9e3dfa9a5c2e01661cb4bd949ab1fd2a0e35af'
    '4d85d235493cfb3be057e1374ad14878bef085a5'
)
foreach ($blob in $OldBlobs) {
    git -C $Work cat-file -e $blob 2>$null
    if ($LASTEXITCODE -eq 0) { throw "Blob todavía presente: $blob" }
}

# 5) Debe conservarse el número de commits y no debe haber refs de reemplazo.
git -C $Work rev-list --count refs/heads/main
git -C $Work for-each-ref refs/replace
```

Resultados esperados: los tres primeros comandos no muestran paths; `for-each-ref` muestra solo
`refs/heads/main` y `refs/tags/v1.6.0-stable`; el tag apunta a `5ec65d3`; el árbol de la punta coincide
con `615852d`; los cinco blobs antiguos no existen en el clon de trabajo; el conteo es 43; no hay
`refs/replace`. Además, para cada par `old`/`new` de `commit-map`, comparar los manifiestos
`git ls-tree -r --full-tree` excluyendo los seis paths: deben ser idénticos; y comparar
`%an|%ae|%ad|%s` de los pares: autor, fecha y mensaje deben conservarse. Si algo difiere fuera de los
seis paths, **abortar el ensayo** y recrear el clon de trabajo desde el respaldo.

##### 11.6 Lista de comprobación previa al force-push

Esta lista es para una fase posterior y **no se ejecutó**:

- [ ] Autorización humana explícita para reescribir `main` y publicar el resultado.
- [ ] Ventana de mantenimiento anunciada; nadie más puede hacer push durante la operación.
- [ ] `git ls-remote origin refs/heads/main` sigue devolviendo `615852d…`.
- [ ] El tag remoto sigue en `84f0ed0…` → `5ec65d3…`; no hay PR refs ni ramas nuevas.
- [ ] Bundle verificado, clon de respaldo con `fsck --full` correcto y SHA-256 registrado.
- [ ] El clon de trabajo está fuera del repositorio principal; no contiene `benchmark/build/` ni el
      informe no rastreado.
- [ ] Los seis paths y los cinco blobs antiguos están ausentes en el clon de trabajo.
- [ ] El árbol de la punta coincide con `615852d`; no hay diferencias fuera de los seis paths.
- [ ] El tag no cambió y no se van a tocar tags ni otras ramas.
- [ ] La protección de rama permite la operación; si la rechaza, detenerse y coordinar, sin saltarla.
- [ ] Existe un plan de verificación posterior y de comunicación a clones/forks.

Comando futuro propuesto, **solo tras autorización y solo para `main`**; no usar `--mirror` ni
`--tags`. Verificar antes `git -C $Work remote -v`: si `origin` sigue existiendo, usar
`git remote set-url`; si fue eliminado por `filter-repo`, añadirlo de nuevo.

```powershell
git -C $Work remote add origin $Remote
git -C $Work push --force-with-lease=refs/heads/main:615852d8383c20f42c2ef04f47044dc650ea263c `
  origin refs/heads/main:refs/heads/main
```

Detenerse si el remoto cambió, si el push es rechazado, si el tag cambió o si la comprobación de
objetos no es concluyente. No usar `--force` genérico, `--mirror`, `--prune` ni `--tags` para evitar
arrastrar otras refs.

##### 11.7 Lista de comprobación posterior a la purga

- [ ] `git ls-remote origin refs/heads/main` devuelve el SHA nuevo esperado.
- [ ] El tag remoto sigue en `84f0ed0…` → `5ec65d3…`; no se borraron ni movieron tags.
- [ ] Un clon limpio y separado del remoto no contiene ninguno de los seis paths en ninguna ref.
- [ ] `git log --all --full-history --name-status -- <seis>` y
      `git rev-list --objects --all -- <seis>` están vacíos en el clon limpio.
- [ ] Los cinco blobs antiguos no resuelven en el clon limpio tras el GC.
- [ ] `main` conserva 43 commits, el árbol de la punta y los mensajes/autores esperados.
- [ ] No hay `refs/replace` ni refs ocultas que reintroduzcan los blobs.
- [ ] Se solicitó a GitHub Support la purga de cachés/vistas y el GC de objetos inalcanzables; se
      volvió a comprobar después de la ventana indicada por GitHub.
- [ ] Los colaboradores fueron informados para reclonar; no se permite `git pull && git push` desde
      clones antiguos.
- [ ] El repositorio local original y `nova-baseline` se tratan según una decisión separada; no se
      limpian ni se resetean en esta fase.
- [ ] El informe de auditoría registra el SHA nuevo, el resultado y la exposición residual.

##### 11.8 Exposición residual que no elimina la reescritura

- **GitHub:** el commit antiguo puede seguir accesible por SHA directo, API, vistas cacheadas o
  almacenamiento interno hasta que GitHub ejecute el GC y purgue cachés; hay que abrir una solicitud
  de soporte y verificar después.
- **Forks:** la API devolvió 0 forks y 0 en la red en esta revisión, pero cualquier fork creado antes
  o después conserva su propia historia; no se puede purgar desde este repositorio.
- **Clones y descargas:** clones locales, copias ZIP/tar, artefactos de CI, cachés de herramientas y
  copias en otros equipos pueden conservar los blobs. Una reescritura no revoca esas copias.
- **Repositorio local:** el repo principal, sus reflogs y el clon de respaldo conservan los objetos
  antiguos por diseño. `nova-baseline` comparte el almacén local. No se limpian en esta fase.
- **Copias locales adicionales:** la búsqueda textual encontró `launcher_state` en 5 XML
  `docs/assets/phase8-*.xml` y en `docs/evidence_p8/original_launcher_prefs.xml`, todos ignorados y
  no rastreados, además de los seis objetivo. No forman parte de esta purga y requieren una decisión
  separada; no se inspeccionó su contenido. También hay coincidencias en binarios de benchmark y
  referencias en documentación, no evaluadas como XML crudo.
- **Motores de búsqueda y terceros:** pueden conservar metadatos o copias; no hay control directo.
- **Tags/ramas:** el tag `v1.6.0-stable` no contiene los seis paths y no necesita reescritura; no se
  debe borrar ni mover. No hay PR refs remotas en la consulta.
- **Datos personales:** no hay secretos confirmados que rotar, pero la exposición de datos personales
  ya ocurrió; la purga reduce la disponibilidad futura, no la deshace.

##### 11.9 Criterios de aceptación del plan

1. Los seis paths están ausentes en todas las refs del clon reescrito y, tras la publicación
   autorizada, en la punta remota de `main`.
2. Los cinco blobs antiguos no existen en el clon de trabajo tras el GC; los commits afectados
   antiguos son inalcanzables en ese clon.
3. Se reescriben exactamente 5 commits y se conservan 43; mensajes, autores, fechas y árbol de la
   punta no cambian fuera de los seis paths.
4. El tag `v1.6.0-stable`, las ramas/tags locales de respaldo y `nova-baseline` no se modifican.
5. El repositorio principal, los 215 artefactos de `benchmark/build/` y el informe local no se
   modifican durante el ensayo.
6. La copia de seguridad verifica, tiene SHA-256 registrado y está almacenada de forma segura.
7. La verificación remota y la exposición residual quedan documentadas; el hallazgo no se marca como
   resuelto solo por la reescritura.
8. Los XML locales `phase8-*` y `docs/evidence_p8/original_launcher_prefs.xml` quedan fuera de este
   alcance y no se borran; se documentan como copias locales pendientes de una decisión separada.

##### 11.10 Decisiones que requieren autorización humana

1. Autorizar la creación del bundle y de los clones espejo fuera del repositorio principal.
2. Autorizar la ejecución de `git filter-repo` en el clon de trabajo y el tratamiento seguro del
   clon de respaldo, que contiene los datos sensibles.
3. Autorizar la ventana de mantenimiento y el force-push de `main` (o rechazarlo).
4. Autorizar la solicitud a GitHub Support para purgar cachés y ejecutar el GC remoto.
5. Decidir si se limpia o se reemplaza el repositorio local y cómo tratar `nova-baseline`; no se hará
   sin una orden separada.
6. Decidir la retención, el cifrado y la eliminación segura del bundle y del clon de respaldo.
7. Decidir la comunicación a colaboradores, forks y copias descargadas.
8. La Fase 1 de la auditoría sigue pendiente y no se inicia con este plan.

---

## 5. Observaciones preliminares (no son hallazgos cerrados)

Estos puntos se detectaron al localizar archivos, pero **no se evalúan como defectos** hasta la fase
correspondiente:

| Observación | Fase prevista |
|---|---|
| `allowBackup="true"` con exclusión de dos SharedPreferences de Hidden Apps; hay que verificar nombres reales, Keystore y rutas de recuperación | Fase 5 |
| `HiddenAppsCredentialStore` declara AES-GCM de 256 bits, IV de 12 bytes y tag de 128 bits; hay que revisar generación de clave, gestión de errores y migración | Fase 5 |
| `BIND_APPWIDGET` es un permiso de nivel signature; el lint lo desactiva globalmente (`ProtectedPermissions`) | Fase 7 |
| Versiones de AndroidX potencialmente antiguas frente a `compileSdk 36` (`core-ktx 1.10.1`, `lifecycle 2.6.1`, `activity-compose 1.8.0`) | Fases 1 y 7 |
| El documento de estado afirma 236 pruebas y 236 PASS; hay 236 anotaciones `@Test`, pero no se ejecutaron aún | Fase 6 |
| `MainActivity` fija `isAppearanceLightStatusBars = false`; hay que comprobar el comportamiento en temas claros | Fase 2 |
| `VideoWallpaperManager` es un objeto global; hay que verificar liberación y concurrencia entre páginas y ciclos de vida | Fase 4 |
| `docs/performance_traces/` contiene trazas de hasta ~40 MB; impacto en clone y privacidad | Fases 3 y 7 |

---

## 6. Limitaciones de esta fase

1. **Solo Fase 0.** No se ha auditado todavía la arquitectura, la persistencia, el rendimiento, el
   vídeo, la criptografía, las pruebas ni el release.
2. **No se ejecutaron compilaciones ni pruebas.** No se afirma que el proyecto compile ni que los
   tests pasen. Los resultados de `docs/AGENT_PROJECT_STATE.md` son afirmaciones del proyecto, no
   verificaciones de esta auditoría.
3. **No se inspeccionó el APK/AAB** ni el dispositivo físico. No se realizaron pruebas en el POCO X6
   5G ni se intervino en ningún dispositivo.
4. **El worktree `nova-baseline` (5ec65d3) no fue auditado.** Solo se auditó el worktree principal.
5. **El árbol está sucio por artefactos de build preexistentes.** No se limpió ni se revirtió nada.
6. **NL-AUD-002 está confirmado y revisado, con remediación parcial del árbol actual.** La revisión
   específica del 2026-10-22 abarcó seis XML versionados y confirmó datos personales. La retirada,
   las reglas de `.gitignore` y la actualización documental se publicaron en `main` con el commit
   `615852d`; la reescritura del historial no se ejecutó. La auditoría de secretos de todo el
   repositorio sigue en Fase 5.
7. **No se leyó el contenido íntegro de los 61 archivos Kotlin.** La Fase 0 se limita a identidad,
   inventario y puntos de entrada; el análisis detallado empieza en Fase 1.

---

## 7. Progreso de la auditoría

| Fase | Estado | Evidencia / nota |
|---|---|---|
| 0 — Acceso e identidad | **COMPLETADA** | Este documento |
| 1 — Arquitectura, Kotlin y compilación | PENDIENTE | No iniciada |
| 2 — Persistencia y UI-1 | PENDIENTE | — |
| 3 — Memoria y rendimiento | PENDIENTE | — |
| 4 — Fondos de vídeo y navegación | PENDIENTE | — |
| 5 — Seguridad y privacidad | PENDIENTE | NL-AUD-002 revisado; retirada publicada en `main` y prevención incluida; historial pendiente |
| 6 — Pruebas y calidad | PENDIENTE | — |
| 7 — Distribución y release | PENDIENTE | NL-AUD-001 se ampliará aquí |
| 8 — Consolidación | PENDIENTE | — |

**Revisión específica NL-AUD-002 (2026-10-22):** completada. Se verificaron seis XML versionados,
87 paquetes, datos personales confirmados y ausencia de secretos en esos archivos. La retirada y la
prevención se publicaron en `main` con `615852d`; el historial sigue pendiente y la remediación no se
declara cerrada. **La Fase 1 no se ha iniciado.**

### Archivos concretos previstos para la Fase 1

- `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`.
- `app/src/main/AndroidManifest.xml`.
- `NovaApplication.kt`, `MainActivity.kt`, `LauncherViewModel.kt`.
- `ui/screens/LauncherScreen.kt`, `SettingsScreen.kt`, `AdvancedSettingsScreen.kt`,
  `AppDrawerScreen.kt`, `OnboardingScreen.kt`, `ViewOne.kt`, `ViewTwo.kt`.
- `ui/screens/LauncherNavState.kt`, `ui/components/VideoWallpaperManager.kt`,
  `ui/components/VideoBackground.kt`.
- `theming/*.kt`, `hiddenapps/*.kt`, `memory/*.kt`, `ram/*.kt`, `widget/*.kt`.
- `app/src/test/**` y `app/src/androidTest/**` para inventariar contratos, sin evaluarlos aún como
  calidad de pruebas (Fase 6).

No se repitieron hallazgos ni se eliminó contenido de fases anteriores porque esta es la primera
entrega.

---

## 8. Conclusión de la Fase 0

El acceso al código es real y verificable. El repositorio local es NovaLauncher, está conectado al
remoto correcto y el código fuente no tiene cambios locales sin commit. `HEAD` y `origin/main`
coinciden en `615852d`; el remoto incluye el commit local anterior y el de remediación. El árbol de
trabajo conserva 215 artefactos de `benchmark/build/` y este informe no rastreado. Los dos riesgos
confirmados en esta fase son de repositorio y privacidad, no de lógica de la aplicación: artefactos
de build versionados y preferencias reales del dispositivo en un repositorio público. La revisión
específica de `NL-AUD-002` confirmó seis XML, 87 paquetes y datos personales, sin secretos en esos
archivos; la retirada y la prevención se publicaron en `main`, pero el historial sigue pendiente y el
hallazgo no se declara resuelto.

No se declara nada sobre la calidad funcional, la seguridad criptográfica, el rendimiento ni la
distribución hasta completar las fases restantes.

---

**FASE COMPLETADA: 0/8**

**REVISIÓN NL-AUD-002: COMPLETADA — RETIRADA PUBLICADA EN MAIN (`615852d`); HISTORIAL PENDIENTE; PLAN DE PURGA DOCUMENTADO (NO EJECUTADO)**

**SIGUIENTE FASE: 1 — Arquitectura, Kotlin y compilación (no iniciada)**
