# Orivolt Home — Google Play readiness 08E-1

**Scope:** static Play-readiness work on 2026-11-04. This is not Google Play approval and does not complete Data Safety. No emulator, device, installation, uninstallation, signing, commit, push, or original AnimeLauncher modification was performed.

## Accessibility audit and change

`LauncherAccessibilityService.kt` was reviewed directly. Its only callable actions are screen lock (`GLOBAL_ACTION_LOCK_SCREEN`, API 28+) and notification-panel opening (`GLOBAL_ACTION_NOTIFICATIONS`). Its event callback is empty; it keeps only an in-memory service instance. `accessibility_service_config.xml` disables window-content retrieval and gesture performance. Thus the service itself does not obtain window text/content, nor does its implementation persist or share event data. The final absence of all app traffic remains subject to the privacy validation already marked pending in 08D-4/08D-5B.

Before this phase, `AccessibilityDisclosureDialog` already appeared before inactive-service paths opened Accessibility Settings, with cancel and accept options. The wording has been made more exact in Spanish and English. Both `LauncherScreen` and `SettingsScreen` open `ACTION_ACCESSIBILITY_SETTINGS` only from `onAccept`; rejection only dismisses the dialog. The Settings entry remains a voluntary way to retry. `isAccessibilityTool` was not added.

## Privacy-policy integration

The historical NovaLauncher URL was removed from `SettingsScreen.kt`. `PRIVACY_POLICY_URL` is deliberately `null`, and the policy button is disabled, so the build does not direct a person to an unrelated policy. When the legal owner has published and validated the final public HTTPS policy, replace **only** that `null` value with the approved URL, validate unauthenticated access and Orivolt Home content, then use the same value in Play Console. Do not substitute a placeholder or an unverified address.

## Visible identity and resources

The public Settings attribution and notification-access text now say Orivolt Home. Technical namespaces, theme names, packages, and `applicationId com.daybreak.animelauncher` are intentionally unchanged. Internal references to NovaLauncher/AnimeLauncher remain and should not be treated as public identity until a separately scoped migration.

The manifest uses `@mipmap/ic_launcher`/round variants with adaptive-icon foreground/background resources. The 08D-2 dossier blocks the icon family, 24 pictograms, 116 WebP candidates, historical backgrounds, and video candidates from release/listing use until rights are evidenced. No assets were copied; the two historical MP4 deletions pre-existed this phase and were not changed.

## Remaining publication blockers

1. Legal owner details and a live HTTPS privacy-policy URL.
2. Rights evidence for every listing and packaged visual asset; in particular the icon and approved screenshots.
3. The intentionally postponed dynamic/emulator privacy, backup, and optional-permission tests from 08D-4/08D-5B. They are not approved here.
4. Completion of Data Safety from evidence, not from this readiness draft.
5. Console-specific review of the current accessibility declaration and video requirements.

## Safe validation commands (PowerShell)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
.\gradlew.bat assembleRelease
git diff --check
```

These commands do not authorize an emulator or physical-device test. Run them from the project root; report an environmental or sandbox failure as a failure/pending result, never as a pass.
