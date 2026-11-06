package com.daybreak.animelauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/** Stateless entry point: Android owns confirmation and package restrictions. */
internal object AppSystemActions {
    enum class Action { INFO, UNINSTALL }
    enum class Result { STARTED, PACKAGE_MISSING, UNAVAILABLE }

    @Suppress("DEPRECATION") // Official confirmation Intent, supported from minSdk 24.
    fun intent(action: Action, packageName: String): Intent {
        require(packageName.isNotBlank())
        val uri = Uri.fromParts("package", packageName, null)
        return when (action) {
            Action.INFO -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Action.UNINSTALL -> Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri)
                .putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
    }

    fun launch(
        context: Context,
        packageName: String?,
        action: Action,
        startActivity: (Intent) -> Unit
    ): Result {
        if (packageName.isNullOrBlank()) return Result.PACKAGE_MISSING
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(packageName, 0)
            startActivity(intent(action, packageName))
            Result.STARTED
        } catch (_: PackageManager.NameNotFoundException) {
            Result.PACKAGE_MISSING
        } catch (_: RuntimeException) {
            // Includes missing handlers, SecurityException and OEM launch failures.
            Result.UNAVAILABLE
        }
    }

    fun errorMessage(result: Result, isEs: Boolean): String? = when (result) {
        Result.STARTED -> null
        Result.PACKAGE_MISSING -> if (isEs) "La aplicaciÃ³n ya no estÃ¡ instalada." else "The app is no longer installed."
        Result.UNAVAILABLE -> if (isEs) "Android no permite abrir esta acciÃ³n para la aplicaciÃ³n." else "Android cannot open this action for the app."
    }
}

