---
name: novalauncher-audit
description: Guía y protocolo de auditoría estricta previa a cualquier modificación de código en NovaLauncher. Úsalo al iniciar fases de análisis, investigación técnica o diagnóstico sin realizar cambios en el repositorio.
---

# Skill: Auditoría Técnica Estricta — NovaLauncher

## Propósito
Garantizar que toda intervención en NovaLauncher esté precedida por una investigación exhaustiva, objetiva y no destructiva, evitando ediciones prematuras de código, suposiciones infundadas o refactors no solicitados.

---

## Reglas de la Fase de Auditoría
1. **Modo Solo Lectura:** Bajo ninguna circunstancia modificar archivos fuente, recursos, manifiesto ni scripts de compilación durante una auditoría.
2. **Sin Commits ni Pushes:** No ejecutar comandos de modificación en Git.
3. **Uso de Herramientas de Inspección:**
   * `view_file` para revisar archivos específicos y rangos de líneas.
   * `grep_search` para rastrear referencias de métodos, identificadores o rutas.
   * `run_command` únicamente para comandos de solo lectura (`git status`, `git diff`, `dumpsys`, etc.).

---

## Flujo de Trabajo
1. **Definir el Alcance:** Delimitar exactamente qué subsistema, servicio o componente se va a auditar (ej. Accessibility, NotificationListener, Data Safety, Signing).
2. **Revisar Componentes Protegidos:** Comprobar si el área auditada tiene interacción con componentes protegidos (`VideoWallpaperManager`, `WidgetHostManager`, `NotificationMonitorService`, `LauncherAccessibilityService`, etc.).
3. **Verificar Estado Real del Código:** No asumir comportamientos basados en memoria de chat; leer directamente el código actual y la configuración en disco.
4. **Construir Informe Estructurado:**
   * **Resumen Ejecutivo:** Diagnóstico claro en 1-2 párrafos.
   * **Hallazgos Técnicos:** Tablas o listas con archivo, línea y comportamiento verificado.
   * **Riesgos y Dependencias:** Identificar impactos potenciales en otros componentes.
   * **Propuesta Concreta:** Opciones mínimas para la fase de implementación sin ejecutar cambios todavía.
