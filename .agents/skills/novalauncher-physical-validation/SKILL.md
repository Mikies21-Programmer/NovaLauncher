---
name: novalauncher-physical-validation
description: Protocolo para la preparación y registro de pruebas físicas en hardware real (POCO X6 5G) para NovaLauncher. Asegura que ninguna validación física se marque como aprobada sin confirmación explícita del desarrollador.
---

# Skill: Validación Física en Dispositivo Real — NovaLauncher

## Propósito
Establecer un estándar riguroso para la ejecución de pruebas físicas en hardware real (POCO X6 5G / HyperOS / Android 14+), asegurando que el agente actúe como facilitador y registrador objetivo, sin dar por aprobadas pruebas que requieren interacción humana directa.

---

## Regla de Oro
> **EL AGENTE NUNCA AFIRMA QUE UNA PRUEBA FÍSICA ES "PASS" POR SU CUENTA.**  
> Los estados `PASS` en pruebas manuales (gestos, respuesta táctil, apagado de pantalla, regreso desde apps externas, reinicios visuales) únicamente se asignan cuando el desarrollador ha comunicado la evidencia o confirmado el resultado en el chat.

---

## Estructura Estándar de la Batería de Pruebas Físicas

Al preparar una fase de validación física, el agente debe generar un checklist detallado agrupado por subsistemas:

### 1. Pruebas de Flujo Principal (Objetivo de la Fase)
* Describir la acción exacta del usuario (ej. pulsar un botón, hacer doble toque, deslizar).
* Describir el comportamiento esperado (ej. aparición de diálogo, apertura de navegador externo, cierre de pantalla).
* Describir el comportamiento de descarte o cancelación (ej. pulsar "Ahora no", presionar tecla Atrás).

### 2. Pruebas de No Regresión (Obligatorias en Cada Fase)
* **Widgets:** Verificar que los widgets nativos (ej. Google Calendar) sigan interactivos, escalados y actualizados en View 1.
* **Video Wallpaper:** Comprobar que el video en bucle continúe reproduciéndose sin pausas, parpadeos negros ni fugas de memoria tras cambiar de pantalla.
* **Navegación y Gestos:**
  * Swipe horizontal entre View 1 y View 2 fluido.
  * Doble toque para apagar pantalla (`GLOBAL_ACTION_LOCK_SCREEN`).
  * Deslizamiento hacia abajo para abrir notificaciones (`GLOBAL_ACTION_NOTIFICATIONS`).
  * Apertura y cierre del App Drawer.
* **Notificaciones:** Comprobar que el contador numérico en la barra lateral refleje exactamente el número de apps de mensajería con mensajes pendientes (excluyendo llamadas activas).
* **Configuraciones y Glass UI:** Verificar que la paleta cyan/dark y los estilos translúcidos persistan correctamente.

---

## Procedimiento de Despliegue en el Dispositivo
1. Verificar que el dispositivo esté conectado mediante ADB:
   ```powershell
   & "C:\Program Files\Android\Android Studio\jbr\bin\..\..\..\platform-tools\adb.exe" devices
   ```
2. Instalar el APK debug conservando datos de usuario:
   ```powershell
   & "C:\Program Files\Android\Android Studio\jbr\bin\..\..\..\platform-tools\adb.exe" -s <device_id> install -r "app\build\outputs\apk\debug\app-debug.apk"
   ```
3. Entregar el checklist ordenado al desarrollador y esperar su retroalimentación.
