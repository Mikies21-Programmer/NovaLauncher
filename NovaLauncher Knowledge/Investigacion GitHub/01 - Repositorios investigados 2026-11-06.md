---
tipo: benchmark
proyecto: Orivolt Home
fecha: 2026-11-06
base: a8f29c11672c4c3dbb7458360d283dc9ef0d5cca
estado: investigacion-estatica
tags: [orivolt, github, open-source, licencias, launcher]
---

# Repositorios investigados · 6 noviembre 2026

## Alcance, metodología y límites

Se seleccionaron **12 proyectos principales** por similitud funcional o utilidad para un launcher Android con Kotlin y Jetpack Compose. Se revisaron metadatos públicos, READMEs, licencias declaradas y, para ARK, archivos concretos de iconos, búsqueda, backups, layout y pruebas. No se ha ejecutado ni compilado el código externo, ni se ha auditado cada dependencia o subdirectorio. Las licencias indicadas son las de nivel repositorio/README; verificar **cada archivo, dependencia, material gráfico y NOTICE** antes de incorporar código o distribuir.

Orivolt: `minSdk 24`, `targetSdk 36`, `applicationId com.daybreak.animelauncher`, `versionName 1.6.1` y `versionCode 10` en `app/build.gradle.kts` del commit base. No cambiar identidad, firma o políticas de backup sin revisión.

## Clasificación

| Proyecto y fuente | Licencia observada | Utilidad concreta | Decisión |
|---|---|---|---|
| [ARK-launcher](https://github.com/jrs8205/ARK-launcher) | Apache-2.0 (LICENSE y NOTICE) | Carpetas, drag-and-drop, icon packs, backup versionado, búsqueda por proveedores, pruebas | **PRIORIDAD 1** para estudiar/adaptar módulos delimitados; minSdk externo 30, no fusionar app completa |
| [Calvin-LL/Reorderable](https://github.com/Calvin-LL/Reorderable) | Apache-2.0 | Reordenar LazyGrid/LazyList en Compose | **PRIORIDAD 1**, prueba de compatibilidad Compose/gestos y licencias transitivas |
| [Lawnicons](https://github.com/LawnchairLauncher/lawnicons) | Apache-2.0 | Referencia del formato appfilter y iconos temáticos | Estudiar interoperabilidad, **no empaquetar iconos de marcas sin revisión** |
| [Now in Android](https://github.com/android/nowinandroid) | Apache-2.0 | Arquitectura Compose, testing y configuración de builds | Referencia de ingeniería; no traer módulos sin necesidad |
| [Dislauncher](https://github.com/insomdroid/Dislauncher) | MIT | Onboarding claro, preferencias, seguridad de distribución, avisos de privacidad | Estudiar patrón de producto pequeño y CI, validar madurez |
| [AOSP Launcher3](https://android.googlesource.com/platform/packages/apps/Launcher3/) | Base AOSP Apache-2.0; verificar cada archivo | Contratos HOME, widgets, reordenamiento, perfiles de pantalla | Estudiar algoritmos y APIs; no trasplantar arquitectura View a Compose |
| [Lawnchair](https://github.com/LawnchairLauncher/lawnchair) | GitHub API devuelve NOASSERTION; múltiples componentes | Compatibilidad moderna, icon packs, Material You, QA de launcher | **Sólo referencia** hasta auditar licencia por archivo y dependencias |
| [Kvaesitso](https://github.com/MM2-0/Kvaesitso) | GPL-3.0 (algunos módulos distintos) | Búsqueda local, agrupación de resultados, Compose | Estudiar ideas; **no copiar código GPL a producto cerrado** sin resolver obligaciones |
| [KISS Launcher](https://github.com/Neamar/KISS) | GPL-3.0+ | Búsqueda instantánea y priorización de apps frecuentes | UX como inspiración, no copiar código |
| [Neo Launcher](https://github.com/NeoApplications/Neo-Launcher) | GPL-3.0+ | Gestos, organización, compatibilidad de launcher | UX y pruebas; sin incorporación directa de GPL |
| [Fossify Launcher](https://github.com/FossifyOrg/Launcher) | GPL-3.0 | Comportamientos clásicos y accesibilidad | Inspiración y QA; sin copia directa |
| [One4Home wallpaper bridge](https://github.com/one4studio/one4home-wallpaper-bridge) | MIT | Consentimiento explícito, URI acotada, interoperabilidad de fondos estáticos | Referencia de seguridad; **no es el código de One4Home** |

Adicionalmente: [Arcticons](https://github.com/Arcticons-Team/Arcticons) publica código GPL-3.0 y gráficos bajo CC BY-SA 4.0. **No incluir los 15 000+ iconos en Orivolt** sin estudio específico de atribución, derechos de marca y obligaciones de compartir/adaptar. Referencia para pruebas de compatibilidad del ecosistema de icon packs.

> Las licencias permisivas también exigen conservar avisos, atribuciones y condiciones. «Público», «gratis» y «Open Source» no son equivalentes a «sin restricciones». El código propio publicado sin licencia explícita tampoco adquiere automáticamente una licencia de reutilización.

## ARK: piezas exactas para estudiar

1. **Paquetes de iconos:** `core/data/src/main/kotlin/org/arkikeskus/launcher/data/IconPack.kt`; detecta paquetes compatibles y lee mapeos `appfilter.xml`. Orivolt usa iconos de aplicaciones y URIs personalizados, pero no confirma un proveedor de icon packs completo. Exigir fallback, perfiles de trabajo, apps desinstaladas, caché e invalidación por paquetes.
2. **Carpetas/dock y persistencia:** `core/data/src/main/kotlin/org/arkikeskus/launcher/data/HomeLayoutRepository.kt`; funciones para crear, mover y deshacer carpetas; utiliza transacciones Room. Sus pruebas de regresión `core/data/src/test/kotlin/org/arkikeskus/launcher/data/FolderMoveTest.kt` cubren reordenar, fusionar, evitar duplicados y restaurar carpetas al quedarse con un elemento. En Orivolt no conviene introducir Room de golpe: diseñar modelo y migración antes.
3. **Reordenamiento puro:** `core/data/src/main/kotlin/org/arkikeskus/launcher/data/ReorderPlanner.kt`; algoritmo de ubicación en cuadrícula desacoplado y testeable. Útil para pruebas de solapamiento, fuera de límites, widgets y cambios de orientación.
4. **Backups:** `core/data/src/main/kotlin/org/arkikeskus/launcher/data/backup/BackupCodec.kt` y `BackupRepository.kt`; esquema JSON versionado con lectura de formatos antiguos, distingue elementos restaurados/omitidos. En Orivolt exportar preferencias generales **pero no** bóveda/secretos de Hidden Apps; los IDs de widgets deben reconciliarse, no copiarse a otro dispositivo.
5. **Búsqueda:** `core/data/src/main/kotlin/org/arkikeskus/launcher/data/search/SearchAggregator.kt`; reúne proveedores sin dejar que un fallo individual rompa la búsqueda; investigar diseño local y opcional. Evitar permiso READ_CONTACTS por defecto.

## Evidencia concreta de brechas de Orivolt

- `app/src/main/java/com/daybreak/animelauncher/ThemePresets.kt` define `defaultThemes = emptyList()` y `categories = emptyList()`; el selector y el onboarding todavía enseñan una sección de temas predefinidos. **Brecha P0 verificable**, consecuencia de retirar assets sin derechos acreditados.
- `LauncherViewModel.kt` concentra estado de preferencias, paneles, widgets, accesos y categorías; contiene funciones de agregar/borrar accesos y páginas pero no identificamos persistencia de carpetas estándar Home/dock equivalente a ARK.
- `AppDrawerScreen.kt` ya incluye búsqueda, scroll por índice y LazyVerticalGrid: **mejorar el módulo, no reemplazarlo**.
- `WidgetHostManager.kt` ya administra AppWidgetHost, proveedores y dimensiones: asegurar no-regresión, no sustituir.
- Motor adaptativo de foto/vídeo, Glass UI, RAM y Hidden Apps son diferenciales que deben conservarse.
- `docs/ORIVOLT_HOME_PRIVACY_PENDING_CHECKS_08D-4.md` mantiene pruebas de la Release y Data Safety sin completar.
- `docs/ORIVOLT_HOME_PICTOGRAM_LICENSES_08F-4.md` documenta la sustitución reciente de 24 pictogramas por Material Icons; **no** acredita el icono principal ni los fondos pendientes.
- No se encontró directorio `.github/workflows/` en el árbol de la rama `main` inspeccionada. Falta puerta de CI obligatoria.

## Reglas de integración

- Escribir soluciones pequeñas y probadas, siguiendo contratos de Android y las fuentes como referencia; cuando se reutilice código con permiso, documentar origen, licencia, autoría, avisos y cambios.
- NO copiar componentes GPL a un producto cuya política de código no está decidida; si se quisiera, primero evaluar todas las obligaciones y licencias con asesoría adecuada.
- No descargar ni empaquetar recursos visuales de terceros automáticamente: separar código de logos, marcas, fuentes y fondos.
- No cambiar `applicationId`, firma, versionado de Play Console ni restauración segura de apps ocultas.
- Mantener `minSdk 24`, probar API 24/29/30/33/35/36 según disponibilidad y fabricantes distintos.
- Medir rendimiento en el **mismo dispositivo y configuración** antes/después; `BUILD SUCCESSFUL` no equivale a UI estable ni validación comercial.

## Qué falta

Pruebas en hardware ajeno, métricas de usuarios, revisión exhaustiva de licencias transitivas, comparación de pantallas de accesibilidad, análisis de UI con grupos de prueba, verificación dinámica de seguridad y pruebas de compatibilidad de dependencias. No es aún aprobación comercial ni implementación.

Siguiente nota: [[02 - Hoja de ruta tecnica Orivolt Home]].
