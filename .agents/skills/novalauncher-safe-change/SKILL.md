---
name: novalauncher-safe-change
description: Protocolo de implementación controlada y modificación quirúrgica en NovaLauncher. Úsalo al aplicar cambios aprobados protegiendo componentes críticos y evitando refactors oportunistas.
---

# Skill: Implementación Segura y Quirúrgica — NovaLauncher

## Propósito
Guiar la implementación de cambios de manera controlada, asegurando que solo se modifique el alcance estrictamente aprobado por el desarrollador y protegiendo el núcleo del launcher contra regresiones.

---

## Principios Fundamentales
1. **Edición Quirúrgica:** Emplear `replace_file_content` o `multi_replace_file_content` para bloques contiguos precisos. Nunca reemplazar archivos enteros si no es necesario.
2. **Prohibición de Refactors Oportunistas:** Si detectas código mejorable, desactualizado o redundante que NO forma parte del objetivo actual, **NO LO TOQUES**. Repórtalo como observación separada si es relevante.
3. **Respeto a Componentes Protegidos:** Los componentes marcados como *Strictly Protected* (`VideoWallpaperManager`, `WidgetHostManager`, `NotificationMonitorService`, `LauncherAccessibilityService`, etc.) no pueden modificarse salvo requerimiento directo y con justificación explícita.

---

## Checklist Previo a la Edición
- [ ] ¿El cambio fue solicitado y aprobado explícitamente?
- [ ] ¿Afecta a algún componente protegido?
- [ ] ¿Se preservan los comentarios y la estructura existente?
- [ ] ¿Se evita la introducción de dependencias externas no aprobadas?

---

## Verificación Posterior a la Edición
Inmediatamente después de aplicar cambios de código, ejecutar la suite de validación estándar:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat assembleRelease
```
Comprobar el impacto con Git:
```powershell
git diff
git status
```
Confirmar que el diff refleja **única y exclusivamente** las modificaciones autorizadas.
