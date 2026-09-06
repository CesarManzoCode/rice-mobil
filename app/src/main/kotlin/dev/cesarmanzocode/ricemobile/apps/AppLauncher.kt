package dev.cesarmanzocode.ricemobile.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager

enum class LaunchResult { Started, Unavailable, Denied }

/**
 * Opens a launchable identity via [LauncherApps.startMainActivity] (contract §3.5). Owns
 * platform services directly; never a ViewModel. Never uses getLaunchIntentForPackage,
 * which would lose activity aliases and user identity.
 */
class AppLauncher(
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
) {
    fun launch(key: AppKey): LaunchResult {
        val user = userManager.getUserForSerialNumber(key.userSerial) ?: return LaunchResult.Unavailable
        if (user != Process.myUserHandle()) return LaunchResult.Unavailable
        val component = ComponentName.unflattenFromString(key.component) ?: return LaunchResult.Unavailable
        return try {
            launcherApps.startMainActivity(component, user, null, null)
            LaunchResult.Started
        } catch (_: ActivityNotFoundException) {
            LaunchResult.Unavailable
        } catch (_: SecurityException) {
            LaunchResult.Denied
        } catch (_: IllegalStateException) {
            LaunchResult.Unavailable
        }
    }
}
