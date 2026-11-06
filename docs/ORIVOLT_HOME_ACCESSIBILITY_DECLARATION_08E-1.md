# Orivolt Home — Accessibility declaration 08E-1

**Status:** evidence-backed draft for Play Console. It is not a completed declaration or approval.

## Service and purpose

`LauncherAccessibilityService` is optional. It invokes only Android global actions:

- `GLOBAL_ACTION_LOCK_SCREEN`, for the double-tap-to-lock gesture (Android 9+).
- `GLOBAL_ACTION_NOTIFICATIONS`, for swipe-down and message-counter actions.

`onAccessibilityEvent` has no processing logic. The service XML sets `canRetrieveWindowContent="false"` and `canPerformGestures="false"`; it subscribes to `typeWindowStateChanged` but does not inspect, persist, transmit, or otherwise use the events. No sharing path was found in this service. This statement concerns the service implementation only; it does not replace the pending overall network and backup validation.

The manifest does not set `android:isAccessibilityTool="true"`.

## User consent flow

When a requested optional action cannot run because the service is inactive, the app shows a standalone bilingual disclosure. It names the concrete actions, states the window-content limitation and event handling, and offers **Accept & Configure** / **Not now**. Only acceptance launches `Settings.ACTION_ACCESSIBILITY_SETTINGS`; cancellation closes the dialog and does not launch Settings. The person can start the voluntary flow again through Settings or the optional gesture.

## Console draft

Use the closest current Play Console declaration option that describes an optional launcher feature, not an accessibility tool for people with disabilities. Suggested explanation:

> Orivolt Home optionally uses Android Accessibility Service only to lock the screen from a launcher double-tap and open the notification panel from a launcher gesture or counter. The service does not retrieve window content, does not perform gestures, and its event callback does not process events. A prominent in-app disclosure appears before Android Accessibility Settings is opened; the user can accept or cancel.

Before submitting, compare this wording and the selected Console category against the then-current Console form and actual release build.

## Required future video evidence

Record on a clean test device or emulator only when authorized; do not use personal notifications or accounts.

1. Start with Accessibility disabled; show Orivolt Home and the feature entry point.
2. Trigger double tap, swipe down, and message-counter paths while disabled; show the disclosure each time without accepting.
3. Tap **Not now** and show that Android Accessibility Settings does not open.
4. Start the flow again, show the disclosure in the active app language, then tap **Accept & Configure**.
5. Show Android Accessibility Settings, the named Orivolt Home service, and the user-controlled enable step. Do not falsely represent the app as an accessibility tool.
6. After enabling, demonstrate only the two declared results: lock screen and notification panel. Record no private notification content.
7. Disable the service and show the optional flow remains available.

Keep an unedited source recording, device/API and build/version details, recording date, and SHA-256 of the submitted artifact. Capture only data-free UI and redact any incidental identifiers before uploading.
