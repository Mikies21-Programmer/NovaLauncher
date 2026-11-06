# Orivolt Home — expediente de recursos para Google Play (08D-2)

Fecha de inspección: 2026-11-03. Alcance: procedencia y derechos de recursos gráficos y multimedia. No se repitieron auditorías de código o rendimiento. No se copió, eliminó ni sustituyó ningún recurso; no se modificaron Kotlin, Gradle, preferencias ni el proyecto original.

## Dictamen

**Estado global: BLOQUEADO para integrar o distribuir en Google Play.** Hay trazabilidad técnica suficiente para identificar los candidatos, verificar su integridad y planificar una migración que preserve preferencias, pero no hay prueba automática de titularidad, cesión ni permiso comercial para los activos que se pretenden distribuir. Un hash, una aprobación visual o una etiqueta de IA no son prueba de licencia.

| Grupo definitivo del original | Cantidad | Clasificación de procedencia | Derechos acreditados | Estado |
|---|---:|---|---|---|
| Fondos WebP q95 | 116 | 110 generados mediante una herramienta integrada no identificada en el registro; 6 referencias de producción anteriores con herramienta no documentada de forma suficiente | No: faltan titular, cuenta/servicio, términos vigentes y clearance de similitud | **BLOQUEADO** |
| Loops MP4 candidatos | 2 | Render procedural local (`System.Drawing`) y codificación Windows Media Foundation; sin insumos artísticos externos declarados | No: faltan declaración de autor/titular y autorización comercial formal | **BLOQUEADO** |
| Pictogramas PNG | 24 | Trabajo interno: 18 SVG nuevos y 6 SVG reutilizados de 07C-7A, rasterizados | No: faltan titular/cesión, alcance y revisión de similitud | **BLOQUEADO** |
| Icono Orivolt y capas adaptativas | 1 familia | PNG fuente seleccionado por el propietario; capas XML derivadas | No: la trazabilidad no acredita autoría, titular ni licencia comercial | **BLOQUEADO** |
| JPG/PNG y MP4 históricos en publicación | 116 fondos + 2 MP4 | Procedencia desconocida | No | **BLOQUEADO: sustituir, no redistribuir** |

El registro por archivo —nombre, ruta, SHA-256, origen documentado, titular conocido, derechos acreditados y estado— está en `ORIVOLT_HOME_PLAY_ASSET_REGISTRY_08D-2.csv`. Sus hashes fueron recalculados directamente desde el original durante esta inspección.

## Recursos identificados y trazabilidad

### Fondos definitivos WebP

La colección definitiva está en el proyecto original, `app/src/main/assets/fondos/`: Paisajes 64, Horizonte 11, Retratos 28, cool 4 y terror 9 (116; 54,512,334 bytes). Cada pieza es WebP VP8 con pérdida, q95 / `-m 6` / `-sharp_yuv`, 1024 × 1536. El registro `07C-7C-3D-OPT/candidate-registry.json` y el informe de optimización documentan coincidencia con los PNG maestros, `webpinfo` PASS y decodificación `dwebp` PASS para 116/116; eso acredita integridad técnica, no derechos.

`07C-7C-3C` documenta seis referencias anteriores (Paisajes 01–02, Horizonte 01, Retratos 01, Cool 01 y Terror 01) y 110 imágenes generadas mediante una «integrated image-generation tool», sin imágenes externas. El servicio, la cuenta, los términos aplicables, el titular y el permiso de distribución comercial no están archivados de forma verificable. Por ello no se clasifican como recursos propios con derechos acreditados ni como terceros licenciados.

### Videos definitivos candidatos

Los dos candidatos no activos son:

| Recurso | SHA-256 | Datos documentados | Estado |
|---|---|---|---|
| `docs/resource-candidates/07C-7C-3E-A/videos/orivolt_orbit_loop.mp4` | `12DBEE8FCEE4C814E53F7261839082C53E8BAC0E4DD2ECCFA26EAA133E0BEAAA` | H.264/AVC, 1080 × 1920, 16 s, 30 CFR, sin audio; render procedural local | **BLOQUEADO** por derechos no acreditados |
| `docs/resource-candidates/07C-7C-3E-A/videos/orivolt_nebula_loop.mp4` | `A9CDAAF368D0350DD0854F46A1D3EBDAE9CF8057B61CEC9BECC7914B85D4C7B1` | H.264/AVC, 1080 × 1920, 16 s, 30 CFR, sin audio; render procedural local | **BLOQUEADO** por derechos no acreditados |

El informe `07C-7C-3E-A` declara que no tomó imágenes, videos, personajes, marcas o franquicias como fuente artística. También deja explícitamente pendiente la declaración de titular, autorización comercial, aprobación visual y revisión de similitud. No se infiere licencia de Windows, Media Foundation ni de esa declaración técnica.

### Los 24 pictogramas originales

Los PNG activos del original en `assets/iconos/` conservan los mismos nombres y URI persistentes. El registro `ORIVOLT_ORIGINAL_PICTOGRAM_REGISTRY_07C-7C-2.md` vincula los 24 PNG a 18 SVG internos nuevos y seis SVG reutilizados de 07C-7A, y conserva los SHA-256 exactos. La publicación no contiene esos mismos bytes: los 24 hashes difieren. Aunque el expediente técnico los denomina trabajo interno y registra aprobación visual, no acredita identidad del autor/titular, cesión, territorio, alcance comercial o revisión de similitud; no se declaran Material ni Apache-2.0.

### Icono y recursos adaptativos

El `AndroidManifest.xml` del original declara `@mipmap/ic_launcher` y `@mipmap/ic_launcher_round`. Ambos XML adaptativos enlazan a `@drawable/ic_orivolt_launcher_background`, `foreground` y `monochrome`. La fuente declarada es `drawable-nodpi/orivolt_launcher_original.png` (SHA-256 `4BFF0870980FE2ACDE7DE4DACB5DFFF8B502A7566A1D2FEDC10CA0D4C51DD7DC`); el resto de rutas y hashes está en el registro CSV.

La documentación 07C-5 describe que el PNG fue seleccionado por el propietario y que la familia adaptativa representa un hogar con órbitas violeta/cian. Eso es procedencia operativa, no prueba de quién creó el PNG, de cesión o de disponibilidad de marca. El icono histórico 07C-4 tampoco aporta esa prueba y no debe cargarse como icono de Play.

## Recursos históricos que deben sustituirse

La copia de publicación conserva exactamente 116 fondos históricos bajo `app/src/main/assets/fondos/` (115 JPG y 1 PNG; 33,962,626 bytes), frente a los 116 WebP definitivos del original. El inventario 07C-2 atribuye a los históricos procedencia desconocida y riesgos de fan art, figuras humanas, imágenes generativas sin expediente y/o terceros; no se deben integrar ni mantener en una distribución de Play si siguen sin pruebas.

También conserva MP4 históricos activos:

| Ruta en publicación | SHA-256 actual | Situación |
|---|---|---|
| `app/src/main/res/raw/bg_view_one.mp4` | `DE58AE21AF3D3924624D4E07520FBD32D77D38208B13FCE2F23F711CB80B9EE7` | Histórico, procedencia y licencia no verificadas |
| `app/src/main/res/raw/bg_view_two.mp4` | `CD9CAE9BBF4080772C4E7F7B08BC8242C0C7F6CA58AFD2E7DC79A7027DFE98CE` | Histórico, procedencia y licencia no verificadas |

Los bytes activos del original para esos mismos identificadores son distintos (`6AAC793F…69BA9C2`, 19,548,830 B; y `06BF27AD…5F3D2A`, 18,118,308 B). No existe documentación localizada que vincule esos bytes actuales del original con los dos candidatos procedurales ni que acredite sus derechos. Por tanto, los candidatos MP4 identificados arriba no deben copiarse ni sustituir los históricos hasta aprobación documental y técnica separada.

La publicación también conserva un icono de launcher y 24 pictogramas distintos de la familia identificada en el original. Sus rutas/hashes no coinciden; son recursos históricos independientes y no adquieren derechos por esta auditoría.

## Evidencias que debe aportar el propietario

1. Para cada fondo generado: identidad del servicio y cuenta que lo generó, fecha, identificador/exportación verificable, versión de términos vigente en esa fecha, cláusula que habilite distribución comercial en una app y en marketing, titular declarado, y revisión humana de similitud/copyright. Para retratos, confirmar además que no representan a una persona real identificable o adjuntar el permiso pertinente.
2. Para las seis referencias anteriores: su fuente primaria, autor/titular, cadena de cesiones y el mismo alcance comercial; si fueron generadas, el expediente de servicio/cuenta/términos.
3. Para los MP4: declaración firmada del creador/titular que cubra código/render/arte y distribución comercial mundial, confirmación de que no hay audio ni insumos de terceros, aprobación visual y revisión de similitud. Si se utilizó software o material de terceros, su licencia exacta y cumplimiento.
4. Para cada pictograma: autor/titular de los SVG, cesión o licencia comercial para Orivolt Home, fecha, alcance territorial y revisión de similitud. La aprobación visual sola no basta.
5. Para el icono: fuente original, autor/titular, cesión o licencia comercial, evidencia de que el propietario puede usarlo como marca/listing, y búsqueda/revisión de conflicto figurativo en los territorios objetivo.
6. Para cualquier recurso histórico que se quisiera conservar: licencia o permiso por archivo; si no se presenta, retirarlo de un futuro paquete autorizado. No usar capturas, fondos elegidos por usuarios ni recursos de terceros como evidencia sustitutiva.

## Plan de integración posterior a la aprobación

No ejecutar antes de que el estado jurídico de los 116 WebP y los dos MP4 sea **LISTO PARA INTEGRACIÓN**.

1. Verificar de nuevo todos los SHA-256 contra este expediente y copiar sólo los 116 WebP aprobados desde el original, preservando las cinco carpetas, nombres y orden de categoría. Mantener los masters fuera del paquete; no copiar JPG/PNG históricos ni candidatos no aprobados.
2. Recuperar del original `WallpaperMigrationPolicy` y su mapa literal de 116 URI históricos → URI WebP. La política debe ser idempotente y cambiar únicamente `ViewConfig.backgroundUri` reconocido; no tocar `content://`, URI desconocidas, videos, campos de preferencias ni los demás datos de `LauncherState`.
3. Recuperar/ejecutar las pruebas de `WallpaperMigrationPolicyTest`: 116 mapeos únicos, migración una sola vez, preservación de URI de usuario/video/desconocidas, identidad del estado ya actualizado y categorías/preajustes correctos. Hacer una actualización sobre preferencias de prueba con URI históricos, comprobar persistencia tras relanzar y validar visualmente las 116 piezas en el dispositivo autorizado.
4. Para video, sustituir sólo los bytes aprobados conservando exactamente `R.raw.bg_view_one` y `R.raw.bg_view_two`; así los defaults continúan funcionando cuando `backgroundUri` está vacío y no se modifica la preferencia del usuario. Validar codec, reproducción, bucle, reanudación, temperatura/RAM y ambos reproductores sin alterar Media3.
5. Inspeccionar el APK/AAB resultante para confirmar inclusión de los 116 WebP/MP4 aprobados y ausencia de los 116 JPG/PNG y MP4 históricos. Documentar hashes post-empaquetado y la prueba física autorizada.

## NL-AUD-002

Se buscó por nombre y contenido en XML tanto en el original como en la publicación: se encontraron **0 XML** asociados a `NL-AUD-002`. Por tanto, los seis XML de esa denominación no están incorporados en la copia de publicación; este expediente no autoriza incorporarlos.

## Fuentes consultadas

- `AnimeLauncher/AGENTS.md` y `OrivoltHome-Publicacion/docs/AGENT_PROJECT_STATE.md`.
- `AnimeLauncher/docs/PLAY_ASSET_LICENSE_INVENTORY_07C-2.md`.
- `ORIVOLT_ORIGINAL_PICTOGRAM_REGISTRY_07C-7C-2.md`, `ORIVOLT_WALLPAPER_INTEGRATION_07C-7C-3D.md`, `ORIVOLT_WALLPAPER_WEBP_INTEGRATION_07C-7C-3D-OPT.md`.
- `resource-candidates/07C-7C-3C/07C-7C-3C_REPORT.md`, `07C-7C-3D-OPT/07C-7C-3D-OPT_REPORT.md` y `07C-7C-3E-A/07C-7C-3E-A_REPORT.md`.
