---
tipo: roadmap
proyecto: Orivolt Home
fecha: 2026-11-06
estado: propuesta-no-implementada
tags: [orivolt, roadmap, calidad, producto, monetizacion]
---

# Hoja de ruta técnica y de producto

Referencia: [[01 - Repositorios investigados 2026-11-06]]. Alcance: plan para la app existente `com.daybreak.animelauncher`. **No declara cambios ejecutados**. En cada fase, implementar en una rama aislada, documentar el antes/después y conservar un rollback reproducible.

## P0: experiencia y cumplimiento antes de cualquier función Pro

### R1 · Temas iniciales realmente utilizables
- Hecho verificado: `ThemePresets.defaultThemes` y `categories` son listas vacías; la UI sigue prometiendo temas.
- Entregable: 4–6 estilos originales generados con colores y gradientes por código, miniaturas coherentes con el resultado real, opción personal de fondo existente, tema claro/oscuro y preview sin bloquear el onboarding.
- No agregar JPG/WebP/MP4 ajenos o generados sin comprobación de derechos.
- Criterio: instalar limpio, seleccionar y cambiar un estilo, cerrar/reabrir, recuperar las preferencias anteriores, probar selector sin recursos y contrastes legibles.
- Fuentes: patrones de onboarding de Dislauncher; composables de Orivolt; no trasplantar proyectos.

### R2 · Cerrar bloqueos de distribución
- Continuar expedientes internos `08D-4`, `08D-5B`, `08E-3`, `08F-2` y `08F-4`.
- Completar licencia/autoría del icono y recursos realmente empaquetados, política HTTPS, Data Safety con captura y revisión de tráfico de la variante Release.
- Confirmar firma de subida y canal cerrado sin alterar claves, ficha ni testers sin autorización explícita adicional.
- Criterio: AAB inspeccionado, sin assets bloqueados ni secretos, permisos justificados, soporte en Play y checklist firmado.

### R3 · Puerta de calidad repetible
- Agregar flujo de CI aislado para lint, pruebas unitarias, compilación debug y reporte (sin secretos, sin release firmado en CI).
- Exigir prueba de regresión en el Home real y restauración de una configuración de prueba.
- No elevar dependencias/AGP masivamente en la misma PR.
- Criterio: PR falla por test roto; advertencias y suites quedan registradas.

## P1: funciones que los usuarios esperan de un launcher

### R4 · Carpetas y organización de Home/dock
- Modelo: item ID estable, contenedor HOME/DOCK/FOLDER, orden, coordenadas, package+activity, esquema de migración de `ViewConfig` existente.
- Diseñar lógica pura de mover, agrupar, separar, eliminar y restaurar carpetas con tests antes de UI drag-and-drop. 
- Invariantes: ninguna app duplicada accidentalmente; una operación no pierde favoritos ni widgets; no se altera Hidden Apps; coordenadas siempre dentro de la cuadrícula.
- Referencias: ARK `HomeLayoutRepository.kt`, `FolderMoveTest.kt`, `ReorderPlanner.kt`; biblioteca `Calvin-LL/Reorderable` sólo tras proof-of-concept y licencias.
- Medir gestos y conflictos con `LauncherScreen` (pinch, spread de apps ocultas, swipe, tap).

### R5 · Compatibilidad con icon packs
- Descubrir proveedores instalados y `appfilter.xml`, aplicar por component y perfil, fallback del icono del sistema, invalidación por `PACKAGE_CHANGED/REMOVED` y cache acotada.
- No incluir logos o packs comerciales dentro del APK.
- Tests de apps sin icono mapeado, icon pack eliminado, icono personalizado anterior, modo sin permisos extra.
- Referencia: ARK `IconPack.kt` + compatibilidad con Lawnicons.

### R6 · Respaldo portátil y recuperación
- Exportación/importación de JSON versionado y validado; solo settings/layout no sensible, archivos de usuario vía SAF.
- Excluir expresamente vault, credenciales, secretos y listas de Hidden Apps; revalidar URIs de medios; reconciliar widgets y apps ausentes sin fallos.
- Recuperación parcial explícita y vista previa antes de sobrescribir; no migrar a Room sin plan de compatibilidad.
- Referencia: ARK `BackupCodec.kt` + `BackupRepository.kt`.
- Tests de versiones antiguas, datos corruptos, archivos grandes, proveedores faltantes, cambios de Android y restauración cruzada.

## P2: diferenciación y rendimiento

### R7 · Búsqueda ampliada y local
- Mantener búsqueda e índice actual; añadir un agregador para apps, comandos y ajustes. Contactos solo como función opcional con consentimiento y permisos en contexto, no por defecto.
- Cada proveedor falla de forma aislada, límite de tiempo y resultados ordenados.
- Referencia: ARK `SearchAggregator.kt`, búsqueda rápida KISS/Kvaesitso como diseño, no como código GPL.

### R8 · Rendimiento medible
- Baseline por perfiles de dispositivos: arranque frío/caliente, volver desde otra app, abrir/cerrar cajón, reanudar vídeo, gestos/frames, RAM y batería.
- Registrar P50/P95 y no regresiones; verificar APK Release en dispositivos de distinta gama. Sin objetivos numéricos inventados antes de la línea base.
- Aprovechar `benchmark/`, `baseline-prof.txt`, Perfetto y pruebas existentes. Valorar estructura de QA de Now in Android.

### R9 · Personalización distintiva
- Presets originales, modo Glass accesible y opcional, temas adaptativos por foto/vídeo sin bloquear interacciones, widgets legibles y ahorro de batería.
- Probar modo batería baja, movimiento reducido, contraste alto y dispositivos que eliminan procesos.
- Nunca reducir funciones HOME esenciales por monetización.

## P3: producto comercial cuando haya evidencia

### R10 · Experimento Free/Pro, no antes
- Free útil: apps, carpetas, búsqueda, widgets, icon packs y restauración básica.
- Pro hipotético: colecciones de estilos y escenas originales, efectos visuales opcionales, configuración avanzada.
- Comparar prototipos y disposición a pagar con usuarios, registrar conversión, retención, desinstalaciones y estabilidad; no asumir ventas.
- Si procede, usar Google Play Billing para bienes digitales, verificar compra/restore y seguir política de Google Play. No introducir publicidad invasiva en Home.

## Registro obligatorio por PR en Obsidian

Cada mejora futura debe contener:
1. Problema y evidencia reproducible.
2. Fuente GitHub, commit/tag consultado, licencia + NOTICE y módulos realmente reutilizados.
3. Diseño propio propuesto, dependencias, minSdk, permisos y Data Safety.
4. Antes/después con pruebas, métricas o capturas reales sin datos privados.
5. Riesgo, rollback y decisión `GO / NO-GO`.
6. Fecha, commit Orivolt, estado (propuesta / en curso / validada / distribuida).

**Orden recomendado:** R1 → R2/R3 → R4 → R5 → R6 → R7/R8 → R9 → R10. No lanzar pagos antes del mínimo de calidad y pruebas con usuarios.
