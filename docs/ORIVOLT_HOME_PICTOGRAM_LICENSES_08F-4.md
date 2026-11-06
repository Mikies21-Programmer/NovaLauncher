# Orivolt Home — pictogram replacements (08F-4)

Date: 2026-11-06

## Scope and result

The 24 former PNG default-pictogram assets under `app/src/main/assets/iconos/` were removed from the publication copy. Their persisted `file:///android_asset/iconos/<name>` preference values remain stable and are now mapped in `CustomIconMigrationPolicy` to the Material Icons Compose vectors already supplied by `androidx.compose.material:material-icons-extended`.

No vector, bitmap, or brand asset was downloaded or copied into this repository. The existing Material Icons dependency was already declared in `app/build.gradle.kts`.

## License

The replacement glyphs are Material Icons by Google, licensed under the Apache License, Version 2.0. The license permits commercial use, modification, and distribution provided that the license and notices are retained. Source and license: <https://github.com/google/material-design-icons/blob/master/LICENSE>.

The application distributes the vectors through the existing AndroidX Compose Material Icons dependency; this change does not add a library.

## Explicit mapping

| Former PNG(s) | Material icon |
|---|---|
| abrir-documento | `Outlined.Description` |
| agregar-usuario | `Outlined.PersonAdd` |
| ajuste, big-gear, configuraciones, rueda-de-engranaje | `Outlined.Settings` |
| camara-fotografica | `Outlined.CameraAlt` |
| carpeta | `Outlined.Folder` |
| chat | `Outlined.ChatBubbleOutline` |
| descarga-directa, descargas | `Outlined.Download` |
| enviar | `AutoMirrored.Outlined.Send` |
| galeria-de-fotos, galeria-de-fotos (1), galeria-de-imagenes | `Outlined.Collections` |
| llamada, llamada-telefonica, llamada-telefonica (1) | `Outlined.Phone` |
| mapas-y-banderas | `Outlined.Map` |
| marcador-de-posicion | `Outlined.LocationOn` |
| perfil, usuario, usuario (1) | `Outlined.Person` |
| subir | `Outlined.FileUpload` |

The original launcher icon family is intentionally out of scope. Its provenance remains unproven, as documented in `ORIVOLT_HOME_PLAY_ASSET_DOSSIER_08D-2.md`; do not treat this record as a license for it.
