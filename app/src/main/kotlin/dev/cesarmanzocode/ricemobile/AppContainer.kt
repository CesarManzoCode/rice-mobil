package dev.cesarmanzocode.ricemobile

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import dev.cesarmanzocode.ricemobile.apps.AppsRepository
import dev.cesarmanzocode.ricemobile.apps.IconLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Both application IDs are excluded from the catalog, whichever variant is running. */
private val OWN_APPLICATION_IDS = setOf(
    "dev.cesarmanzocode.ricemobile",
    "dev.cesarmanzocode.ricemobile.debug",
)

private const val MAX_ICON_CACHE_BYTES = 24 * 1024 * 1024

/**
 * Manual DI holder scoped to the application process. Instances are added here as
 * their owning sprint introduces them (apps.* in S1, preferences/wallpaper in S2).
 */
class AppContainer(applicationContext: Context) {

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val launcherApps = applicationContext.getSystemService(LauncherApps::class.java)
    private val userManager = applicationContext.getSystemService(UserManager::class.java)
    private val packageManager = applicationContext.packageManager

    val appsRepository = AppsRepository(
        launcherApps = launcherApps,
        userManager = userManager,
        packageManager = packageManager,
        ownApplicationIds = OWN_APPLICATION_IDS,
        repositoryScope = appScope,
    )

    val iconLoader = IconLoader(launcherApps, maxCacheBytes = MAX_ICON_CACHE_BYTES)
}
