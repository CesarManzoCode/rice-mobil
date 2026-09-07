package dev.cesarmanzocode.ricemobile.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.AppLauncher
import dev.cesarmanzocode.ricemobile.apps.AppSearch
import dev.cesarmanzocode.ricemobile.apps.AppsRepository
import dev.cesarmanzocode.ricemobile.apps.CatalogSnapshot
import dev.cesarmanzocode.ricemobile.apps.LaunchResult
import dev.cesarmanzocode.ricemobile.preferences.FavoriteToggleResult
import dev.cesarmanzocode.ricemobile.preferences.PreferencesRepository
import dev.cesarmanzocode.ricemobile.preferences.PreferencesSnapshot
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.rice.buildFavoriteSlots
import dev.cesarmanzocode.ricemobile.rice.RiceRegistry
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperController
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperControllerStatus
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperMarkerPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

private const val CANCELLATION_CHECK_STRIDE = 64

class LauncherViewModel(
    private val repository: AppsRepository,
    private val launcher: AppLauncher,
    private val preferencesRepository: PreferencesRepository,
    private val wallpaperController: WallpaperController,
) : ViewModel() {

    private val transient = MutableStateFlow(TransientState())

    /** Every Flow here has a single owner (contract §6.3); mapLatest drops stale filters. */
    private val searchResults = combine(
        repository.catalog,
        transient.map { it.query }.distinctUntilChanged(),
    ) { snapshot, query -> snapshot.apps to query }
        .mapLatest { (apps, query) -> cooperativeFilter(apps, query) }

    val uiState: StateFlow<LauncherState> = combine(
        repository.catalog,
        preferencesRepository.snapshots,
        transient,
        searchResults,
        wallpaperController.status,
    ) { catalog, prefsSnapshot, transientValue, results, wallpaperStatus ->
        reduceToLauncherState(catalog, prefsSnapshot, transientValue, results, wallpaperStatus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherState())

    private var launchInFlight = false

    /** True once a wallpaper consistency check has already run for the current foreground
     * entry (contract §8: retry at most once per entry to foreground). */
    private var wallpaperCheckedThisForeground = false

    private fun reduceToLauncherState(
        catalog: CatalogSnapshot,
        prefsSnapshot: PreferencesSnapshot,
        transientValue: TransientState,
        results: List<AppEntry>,
        wallpaperStatus: WallpaperControllerStatus,
    ): LauncherState {
        val prefs = prefsSnapshot.preferences
        val appsByKey = catalog.apps.associateBy { it.key }
        val favorites = prefs.favorites.mapNotNull { appsByKey[it] }
        // Recents are a convenience shortcut, not a persisted identity contract: an uninstalled
        // recent simply drops out (never rendered as an "unavailable" slot like a favorite).
        val recentApps = prefs.recentApps.mapNotNull { appsByKey[it] }
        return LauncherState(
            preferencesReady = true, // this reducer only runs once the preferences flow emitted.
            rice = prefs.rice,
            screen = transientValue.screen,
            catalogStatus = catalog.status,
            apps = catalog.apps,
            query = transientValue.query,
            results = results,
            favoriteKeys = prefs.favorites,
            favorites = favorites,
            recentApps = recentApps,
            appMenu = transientValue.appMenu,
            isDefaultHome = transientValue.isDefaultHome,
            preferencesWritable = prefsSnapshot is PreferencesSnapshot.Ready,
            wallpaperStatus = when (wallpaperStatus) {
                WallpaperControllerStatus.Idle -> WallpaperStatus.Idle
                is WallpaperControllerStatus.Applying -> WallpaperStatus.Applying
                is WallpaperControllerStatus.Failed -> WallpaperStatus.Failed(wallpaperStatus.riceId)
            },
            message = transientValue.message,
        )
    }

    /** Builds the ordered favorite slots a [dev.cesarmanzocode.ricemobile.rice.Rice] renders;
     * a missing key becomes an unavailable slot rather than disappearing (contract §5.2). */
    fun favoriteSlots(state: LauncherState): List<FavoriteSlot> =
        buildFavoriteSlots(state.favoriteKeys, state.apps)

    private suspend fun cooperativeFilter(apps: List<AppEntry>, query: String): List<AppEntry> {
        val tokens = AppSearch.tokensOf(query)
        if (tokens.isEmpty()) return apps
        return withContext(Dispatchers.Default) {
            coroutineScope {
                val matched = ArrayList<AppEntry>()
                for ((index, entry) in apps.withIndex()) {
                    if (index % CANCELLATION_CHECK_STRIDE == 0) coroutineContext.ensureActive()
                    if (AppSearch.matches(entry, tokens)) matched.add(entry)
                }
                matched
            }
        }
    }

    fun updateQuery(raw: String) {
        transient.update { it.copy(query = AppSearch.clampQuery(raw)) }
    }

    fun openDrawer() {
        transient.update(LauncherNavigation::openDrawer)
    }

    fun openPicker() {
        transient.update(LauncherNavigation::openPicker)
    }

    fun goHome() {
        transient.update(LauncherNavigation::goHome)
    }

    fun retryCatalog() {
        repository.requestRefresh()
    }

    fun updateHomeRoleStatus(isDefaultHome: Boolean) {
        transient.update { it.copy(isDefaultHome = isDefaultHome) }
    }

    /** Releases the double-tap guard and re-checks wallpaper consistency once per foreground
     * entry (contract §3.5, §8). Call from Activity.onResume. */
    fun onActivityResumed() {
        launchInFlight = false
        wallpaperCheckedThisForeground = false
        ensureWallpaperConsistency()
    }

    fun openApp(key: AppKey) {
        if (launchInFlight) return
        when (launcher.launch(key)) {
            LaunchResult.Started -> {
                launchInFlight = true
                // Fire-and-forget: recording local history never delays returning Home or the
                // launch itself (contract §3.5 "no retrasar apertura por una animación").
                viewModelScope.launch { preferencesRepository.recordAppOpened(key) }
                goHome()
            }
            LaunchResult.Unavailable -> {
                transient.update { LauncherNavigation.launchFailed(it, System.nanoTime()) }
                repository.requestRefresh()
            }
            LaunchResult.Denied -> {
                transient.update { LauncherNavigation.launchFailed(it, System.nanoTime()) }
            }
        }
    }

    /** Contract §3.5/§7: a Home intent clears drawer/query/menu immediately, unconditionally. */
    fun resetToHome() {
        transient.update(LauncherNavigation::resetToHome)
    }

    // --- Favorites / app menu (contract §5.2, §7, §11) ---

    fun showAppMenu(key: AppKey) {
        transient.update { LauncherNavigation.showAppMenu(it, key) }
    }

    fun dismissAppMenu() {
        transient.update(LauncherNavigation::dismissAppMenu)
    }

    /** Toggles [key] in favorites via a single DataStore transaction (contract §11): the
     * "limit reached" outcome is decided inside that transaction, never against stale state. */
    fun toggleFavorite(key: AppKey) {
        viewModelScope.launch {
            val result = preferencesRepository.toggleFavorite(key)
            if (result == FavoriteToggleResult.LimitReached) {
                transient.update { LauncherNavigation.favoriteLimitReached(it, System.nanoTime()) }
            }
        }
    }

    // --- Rice selection + wallpaper (contract §6, §8) ---

    /** Persist -> return Home -> render the new rice -> request its wallpaper asynchronously
     * (contract §7). The wallpaper write never blocks this call or Home. */
    fun selectRice(id: RiceId) {
        viewModelScope.launch {
            preferencesRepository.selectRice(id)
            goHome()
            wallpaperController.request(id, RiceRegistry.of(id).wallpaper)
        }
    }

    fun retryWallpaper() {
        val state = uiState.value
        wallpaperController.request(state.rice, RiceRegistry.of(state.rice).wallpaper)
    }

    /** At most once per foreground entry (contract §8): if the persisted marker does not match
     * the current rice's spec, the wallpaper write was never confirmed (or the process died
     * mid-commit) and is retried; a match makes this a no-op. */
    private fun ensureWallpaperConsistency() {
        if (wallpaperCheckedThisForeground) return
        wallpaperCheckedThisForeground = true
        viewModelScope.launch {
            val current = preferencesRepository.snapshots.map { it.preferences }.first()
            val spec = RiceRegistry.of(current.rice).wallpaper
            if (WallpaperMarkerPolicy.needsRetry(current.appliedWallpaper, current.rice, spec)) {
                wallpaperController.request(current.rice, spec)
            }
        }
    }
}

class LauncherViewModelFactory(
    private val repository: AppsRepository,
    private val launcher: AppLauncher,
    private val preferencesRepository: PreferencesRepository,
    private val wallpaperController: WallpaperController,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LauncherViewModel(repository, launcher, preferencesRepository, wallpaperController) as T
    }
}
