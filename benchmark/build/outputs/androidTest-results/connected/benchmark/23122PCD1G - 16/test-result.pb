

2e81d6a9primary"ÌJÍJ
á*
com.google.testing.platform“PLUGIN_ERROR"TEST*êErrorName: INSTALL_FAILED_VERIFICATION_FAILURE
NameSpace: DdmlibAndroidDeviceController
ErrorCode: 1
ErrorType: TEST
Message: Failed to install split APK(s): [C:\Users\migue\AndroidStudioProjects\AnimeLauncher\benchmark\build\outputs\apk\benchmark\benchmark-benchmark.apk]:ª'com.google.testing.platform.core.error.UtpException: ErrorName: PLUGIN_ERROR
NameSpace: com.google.testing.platform
ErrorCode: 2002
ErrorType: TEST
Message: ErrorName: INSTALL_FAILED_VERIFICATION_FAILURE
NameSpace: DdmlibAndroidDeviceController
ErrorCode: 1
ErrorType: TEST
Message: Failed to install split APK(s): [C:\Users\migue\AndroidStudioProjects\AnimeLauncher\benchmark\build\outputs\apk\benchmark\benchmark-benchmark.apk]
	at com.google.testing.platform.plugin.PluginLifecycleKt.invokeOrThrow(PluginLifecycle.kt:553)
	at com.google.testing.platform.plugin.PluginLifecycleKt.invokeOrThrow$default(PluginLifecycle.kt:519)
	at com.google.testing.platform.plugin.PluginLifecycle$onBeforeAll$1$1.invoke(PluginLifecycle.kt:241)
	at com.google.testing.platform.plugin.PluginLifecycle$onBeforeAll$1$1.invoke(PluginLifecycle.kt:235)
	at com.google.testing.platform.core.telemetry.common.noop.NoopDiagnosticsScope.recordEvent(NoopDiagnosticsScope.kt:35)
	at com.google.testing.platform.core.telemetry.SequentialEventRecordRequest.record$java_com_google_testing_platform_core_telemetry_telemetry_api(EventRecordRequest.kt:71)
	at com.google.testing.platform.core.telemetry.DiagnosticsExtKt.record(DiagnosticsExt.kt:27)
	at com.google.testing.platform.core.telemetry.TelemetryKt.createEvent(Telemetry.kt:60)
	at com.google.testing.platform.plugin.PluginLifecycle.onBeforeAll(PluginLifecycle.kt:233)
	at com.google.testing.platform.executor.SingleDeviceExecutor$execute$2$1.invoke(SingleDeviceExecutor.kt:170)
	at com.google.testing.platform.executor.SingleDeviceExecutor$execute$2$1.invoke(SingleDeviceExecutor.kt:170)
	at com.google.testing.platform.lib.cancellation.ProcessCancellationContext.runUnlessCancelled(ProcessCancellationContext.kt:159)
	at com.google.testing.platform.executor.SingleDeviceExecutor.execute(SingleDeviceExecutor.kt:170)
	at com.google.testing.platform.RunnerImpl.run(RunnerImpl.kt:132)
	at com.google.testing.platform.server.strategy.NonInteractiveServerStrategy$run$exitCode$1$2.invoke(NonInteractiveServerStrategy.kt:126)
	at com.google.testing.platform.server.strategy.NonInteractiveServerStrategy$run$exitCode$1$2.invoke(NonInteractiveServerStrategy.kt:126)
	at com.google.testing.platform.core.telemetry.common.noop.NoopDiagnosticsScope.recordEvent(NoopDiagnosticsScope.kt:35)
	at com.google.testing.platform.core.telemetry.SequentialEventRecordRequest.record$java_com_google_testing_platform_core_telemetry_telemetry_api(EventRecordRequest.kt:71)
	at com.google.testing.platform.core.telemetry.DiagnosticsExtKt.record(DiagnosticsExt.kt:27)
	at com.google.testing.platform.core.telemetry.TelemetryKt.createEvent(Telemetry.kt:60)
	at com.google.testing.platform.server.strategy.NonInteractiveServerStrategy.run(NonInteractiveServerStrategy.kt:123)
	at com.google.testing.platform.main.MainKt$main$3$1.invokeSuspend(Main.kt:80)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:34)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	at java.base/java.lang.Thread.run(Thread.java:1583)
Caused by: com.google.testing.platform.core.error.UtpException: ErrorName: INSTALL_FAILED_VERIFICATION_FAILURE
NameSpace: DdmlibAndroidDeviceController
ErrorCode: 1
ErrorType: TEST
Message: Failed to install split APK(s): [C:\Users\migue\AndroidStudioProjects\AnimeLauncher\benchmark\build\outputs\apk\benchmark\benchmark-benchmark.apk]
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDeviceController$executeAsync$deferred$1.invokeSuspend(DdmlibAndroidDeviceController.kt:273)
	... 5 more
Caused by: com.android.ddmlib.InstallException: Failed to commit install session 1007220573 with command package install-commit 1007220573. Error: INSTALL_FAILED_VERIFICATION_FAILURE: Install canceled by user
	at com.android.ddmlib.SplitApkInstallerBase.installCommit(SplitApkInstallerBase.java:171)
	at com.android.ddmlib.SplitApkInstaller.install(SplitApkInstaller.java:85)
	at com.android.ddmlib.IDeviceSharedImpl.installPackages(IDeviceSharedImpl.java:395)
	at com.android.ddmlib.internal.DeviceImpl.lambda$installPackages$34(DeviceImpl.java:1491)
	at com.android.ddmlib.internal.DeviceImpl.logRun3(DeviceImpl.java:1833)
	at com.android.ddmlib.internal.DeviceImpl.installPackages(DeviceImpl.java:1488)
	at com.android.ddmlib.internal.DeviceImpl.lambda$installPackages$35(DeviceImpl.java:1503)
	at com.android.ddmlib.internal.DeviceImpl.logRun3(DeviceImpl.java:1833)
	at com.android.ddmlib.internal.DeviceImpl.installPackages(DeviceImpl.java:1499)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDevice.installPackages(DdmlibAndroidDevice.kt:80)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDeviceController$executeAsync$deferred$1.invokeSuspend(DdmlibAndroidDeviceController.kt:251)
	... 5 more
› 
À
DdmlibAndroidDeviceController#INSTALL_FAILED_VERIFICATION_FAILURE"TEST*íFailed to install split APK(s): [C:\Users\migue\AndroidStudioProjects\AnimeLauncher\benchmark\build\outputs\apk\benchmark\benchmark-benchmark.apk]:Âcom.google.testing.platform.core.error.UtpException: ErrorName: INSTALL_FAILED_VERIFICATION_FAILURE
NameSpace: DdmlibAndroidDeviceController
ErrorCode: 1
ErrorType: TEST
Message: Failed to install split APK(s): [C:\Users\migue\AndroidStudioProjects\AnimeLauncher\benchmark\build\outputs\apk\benchmark\benchmark-benchmark.apk]
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDeviceController$executeAsync$deferred$1.invokeSuspend(DdmlibAndroidDeviceController.kt:273)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:34)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	at java.base/java.lang.Thread.run(Thread.java:1583)
Caused by: com.android.ddmlib.InstallException: Failed to commit install session 1007220573 with command package install-commit 1007220573. Error: INSTALL_FAILED_VERIFICATION_FAILURE: Install canceled by user
	at com.android.ddmlib.SplitApkInstallerBase.installCommit(SplitApkInstallerBase.java:171)
	at com.android.ddmlib.SplitApkInstaller.install(SplitApkInstaller.java:85)
	at com.android.ddmlib.IDeviceSharedImpl.installPackages(IDeviceSharedImpl.java:395)
	at com.android.ddmlib.internal.DeviceImpl.lambda$installPackages$34(DeviceImpl.java:1491)
	at com.android.ddmlib.internal.DeviceImpl.logRun3(DeviceImpl.java:1833)
	at com.android.ddmlib.internal.DeviceImpl.installPackages(DeviceImpl.java:1488)
	at com.android.ddmlib.internal.DeviceImpl.lambda$installPackages$35(DeviceImpl.java:1503)
	at com.android.ddmlib.internal.DeviceImpl.logRun3(DeviceImpl.java:1833)
	at com.android.ddmlib.internal.DeviceImpl.installPackages(DeviceImpl.java:1499)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDevice.installPackages(DdmlibAndroidDevice.kt:80)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDeviceController$executeAsync$deferred$1.invokeSuspend(DdmlibAndroidDeviceController.kt:251)
	... 5 more
å
â*†Failed to commit install session 1007220573 with command package install-commit 1007220573. Error: INSTALL_FAILED_VERIFICATION_FAILURE: Install canceled by user:„com.android.ddmlib.InstallException: Failed to commit install session 1007220573 with command package install-commit 1007220573. Error: INSTALL_FAILED_VERIFICATION_FAILURE: Install canceled by user
	at com.android.ddmlib.SplitApkInstallerBase.installCommit(SplitApkInstallerBase.java:171)
	at com.android.ddmlib.SplitApkInstaller.install(SplitApkInstaller.java:85)
	at com.android.ddmlib.IDeviceSharedImpl.installPackages(IDeviceSharedImpl.java:395)
	at com.android.ddmlib.internal.DeviceImpl.lambda$installPackages$34(DeviceImpl.java:1491)
	at com.android.ddmlib.internal.DeviceImpl.logRun3(DeviceImpl.java:1833)
	at com.android.ddmlib.internal.DeviceImpl.installPackages(DeviceImpl.java:1488)
	at com.android.ddmlib.internal.DeviceImpl.lambda$installPackages$35(DeviceImpl.java:1503)
	at com.android.ddmlib.internal.DeviceImpl.logRun3(DeviceImpl.java:1833)
	at com.android.ddmlib.internal.DeviceImpl.installPackages(DeviceImpl.java:1499)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDevice.installPackages(DdmlibAndroidDevice.kt:80)
	at com.android.tools.utp.plugins.deviceprovider.ddmlib.DdmlibAndroidDeviceController$executeAsync$deferred$1.invokeSuspend(DdmlibAndroidDeviceController.kt:251)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:34)
	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	at java.base/java.lang.Thread.run(Thread.java:1583)
