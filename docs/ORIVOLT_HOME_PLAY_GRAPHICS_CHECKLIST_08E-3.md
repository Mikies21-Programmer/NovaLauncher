# Orivolt Home — checklist de recursos gráficos de Play (08E-3)

**Estado de autorización:** ningún recurso gráfico existente queda autorizado automáticamente para marketing. Antes de subir cualquier archivo, el titular debe aportar autoría/titularidad, cesión o licencia comercial, territorio/alcance y revisión de similitud, conforme al expediente 08D-2.

## Especificaciones de carga

| Recurso | Requisito de Play | Preparar / no preparar ahora |
| --- | --- | --- |
| Icono de ficha | PNG de 32 bits con alfa, 512 × 512 px, máximo 1 MB | **No cargar aún.** Crear o seleccionar un icono sólo tras acreditar derechos; no modifica el icono de la app. |
| Banner destacado | JPEG o PNG de 24 bits, sin alfa, 1024 × 500 px | **No generar ni cargar aún.** Debe usar únicamente arte y tipografía con derechos documentados. |
| Capturas de teléfono | JPEG o PNG de 24 bits, sin alfa; mínimo 320 px, máximo 3840 px por lado y relación máxima 2:1 | Capturas reales de la Release. Preparar al menos 4 verticales de 1080 × 1920 px para recomendación; Play exige un mínimo de 2 capturas para publicar. |

Las medidas y formatos se contrastaron el 2026-11-04 con la guía oficial de [recursos de vista previa de Google Play](https://support.google.com/googleplay/android-developer/answer/9866151).

## Inventario verificado de la Release

| Grupo | Evidencia | Estado para ficha de Play |
| --- | --- | --- |
| Icono de launcher actual | El manifiesto usa `@mipmap/ic_launcher` y `@mipmap/ic_launcher_round`; la AAB contiene sus PNG por densidad y XML adaptativos v26. | **Bloqueado.** Es una familia técnica distinta de la auditada en el original y no tiene derechos acreditados. No cambiar la app en esta fase. |
| Capas adaptativas | `ic_launcher.xml` e `ic_launcher_round.xml`, con fondo `ic_launcher_background.xml` y foreground PNG. | **Bloqueado** como fuente del icono de ficha hasta documentar sus derechos. |
| `nova_launcher_icon.png` | Está empaquetado en los recursos Release, aunque no es el icono declarado por el manifiesto. | **Bloqueado.** Riesgo de identidad histórica y de derechos; no usarlo en banner, icono ni capturas promocionales. |
| 24 pictogramas PNG de `assets/iconos/` | La AAB Release contiene 24 PNG. Los 30 iconos de marcas de terceros están excluidos por Gradle. | **Bloqueado.** La procedencia/trazabilidad no acredita licencia comercial ni revisión de similitud. No exhibirlos deliberadamente en la ficha. |
| Fondos históricos JPG/PNG, fondos WebP y MP4 predeterminados | La AAB revisada no contiene `assets/fondos/*` ni `bg_view_one.mp4`/`bg_view_two.mp4`; el degradado estático se dibuja por código. | No usar ni promocionar estos fondos o vídeos. El degradado estático puede aparecer en capturas reales porque no depende de un recurso gráfico externo. |

## Riesgo de identidad residual

La interfaz y las cadenas públicas revisadas muestran **Orivolt Home**. Persisten identificadores internos o nombres técnicos `com.daybreak.animelauncher`, `Theme.AnimeLauncher`, `AnimeLauncherTheme`, `NovaApplication` y referencias textuales a NovaLauncher/AnimeLauncher en código, comentarios y documentación. No se encontró una etiqueta pública de launcher con esos nombres en el manifiesto o `strings.xml` revisados.

Antes de capturar, comprobar visualmente cada pantalla: si aparece “NovaLauncher” o “AnimeLauncher” en UI, diálogo, versión o recurso, no usar esa captura. No presentar el icono `nova_launcher_icon.png` como marca de Orivolt.

## Lista de control previa a cada carga

- [ ] Archivo creado/seleccionado después de obtener evidencia documental de derechos comerciales.
- [ ] Sin fondos históricos, WebP bloqueados, vídeos bloqueados, personajes, marcas, logotipos o pictogramas de terceros.
- [ ] Sin afirmaciones de ranking, premios, precio, promociones ni relación con Google u otra empresa.
- [ ] Captura auténtica de la Release, sin marcos de teléfono, dedos, montajes ni funciones no disponibles.
- [ ] Sin datos personales, notificaciones, nombres privados de apps, cuentas ni credenciales.
- [ ] Texto incorporado localizado por idioma, si se añade texto a un banner o captura.
- [ ] Texto alternativo de hasta 140 caracteres preparado para cada recurso al cargarlo.
