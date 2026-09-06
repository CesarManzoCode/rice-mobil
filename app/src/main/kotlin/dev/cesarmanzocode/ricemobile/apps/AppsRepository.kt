package dev.cesarmanzocode.ricemobile.apps

import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.content.ComponentName
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Catalog refresh/loading state, owned here because it describes the repository's snapshot. */
sealed interface CatalogStatus {
    data object Loading : CatalogStatus
    data object Ready : CatalogStatus
    data class Failed(val recoverable: Boolean = true) : CatalogStatus
}

data class CatalogSnapshot(val status: CatalogStatus, val apps: List<AppEntry>)

/**
 * Enumerates launchable activities for [Process.myUserHandle] only (contract §3.3) and
 * reconciles the snapshot on [LauncherApps.Callback] events (contract §3.4). All enumeration
 * happens off the calling thread; callbacks only invalidate and enqueue a refresh.
 */
class AppsRepository(
    private val launcherApps: LauncherApps,
    private val userManager: UserManager,
    private val packageManager: PackageManager,
    private val ownApplicationIds: Set<String>,
    private val repositoryScope: CoroutineScope,
) {
    private val _catalog = MutableStateFlow(CatalogSnapshot(CatalogStatus.Loading, emptyList()))
    val catalog: StateFlow<CatalogSnapshot> = _catalog.asStateFlow()

    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private var consumerJob: Job? = null
    private var started = false

    /** Bumped per package on every callback event; feeds [AppEntry.iconRevision]. */
    private val iconRevisions = ConcurrentHashMap<String, Long>()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(user, packageName)
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(user, packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(user, packageName)
        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = changed(user, *packageNames)

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = changed(user, *packageNames)

        override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) =
            changed(user, *packageNames)

        override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) =
            changed(user, *packageNames)
    }

    private fun changed(user: UserHandle, vararg packageNames: String) {
        if (user != Process.myUserHandle()) return
        for (packageName in packageNames) {
            iconRevisions.merge(packageName, 1L, Long::plus)
        }
        requestRefresh()
    }

    /** Idempotent: safe to call from every `onStart`. */
    fun start() {
        if (started) return
        started = true
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        consumerJob = repositoryScope.launch { consumeRefreshRequests() }
        requestRefresh()
    }

    /** Idempotent: safe to call from every `onStop`. */
    fun stop() {
        if (!started) return
        started = false
        launcherApps.unregisterCallback(callback)
        consumerJob?.cancel()
        consumerJob = null
    }

    fun requestRefresh() {
        refreshRequests.trySend(Unit)
    }

    private suspend fun consumeRefreshRequests() {
        for (unused in refreshRequests) {
            refreshOnce()
        }
    }

    private suspend fun refreshOnce() {
        _catalog.value = withContext(Dispatchers.IO) { buildSnapshot() }
    }

    private fun buildSnapshot(): CatalogSnapshot {
        val previousApps = _catalog.value.apps
        return try {
            val self = Process.myUserHandle()
            check(self in launcherApps.profiles) { "current user is not an accessible profile" }
            val serial = userManager.getSerialNumberForUser(self)
            check(serial >= 0) { "no serial for current user" }

            val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolves = if (Build.VERSION.SDK_INT >= 33) {
                packageManager.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(mainIntent, 0)
            }
            val actualComponents = resolves.mapNotNull { info ->
                info.activityInfo?.let { ComponentName(it.packageName, it.name) }
            }.toSet()

            val entries = launcherApps.getActivityList(null, self)
                .filter { it.componentName in actualComponents }
                .filterNot { it.componentName.packageName in ownApplicationIds }
                .distinctBy { it.componentName }
                .map { info -> toEntry(info, serial) }

            CatalogSnapshot(CatalogStatus.Ready, AppSearch.order(entries))
        } catch (e: SecurityException) {
            CatalogSnapshot(CatalogStatus.Failed(recoverable = true), previousApps)
        } catch (e: IllegalStateException) {
            CatalogSnapshot(CatalogStatus.Failed(recoverable = true), previousApps)
        }
    }

    private fun toEntry(info: LauncherActivityInfo, serial: Long): AppEntry {
        val label = runCatching { info.label?.toString() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: info.componentName.packageName
        return AppEntry(
            key = AppKey(userSerial = serial, component = info.componentName.flattenToString()),
            packageName = info.componentName.packageName,
            label = label,
            normalizedLabel = AppSearch.normalize(label),
            normalizedPackage = AppSearch.normalize(info.componentName.packageName),
            iconRevision = iconRevisions[info.componentName.packageName] ?: 0L,
        )
    }
}
