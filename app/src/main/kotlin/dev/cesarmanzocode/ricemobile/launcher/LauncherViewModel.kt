package dev.cesarmanzocode.ricemobile.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.AppLauncher
import dev.cesarmanzocode.ricemobile.apps.AppSearch
import dev.cesarmanzocode.ricemobile.apps.AppsRepository
import dev.cesarmanzocode.ricemobile.apps.LaunchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

private const val CANCELLATION_CHECK_STRIDE = 64

class LauncherViewModel(
    private val repository: AppsRepository,
    private val launcher: AppLauncher,
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
        transient,
        searchResults,
    ) { catalog, transientValue, results ->
        LauncherState(
            screen = transientValue.screen,
            catalogStatus = catalog.status,
            apps = catalog.apps,
            query = transientValue.query,
            results = results,
            isDefaultHome = transientValue.isDefaultHome,
            message = transientValue.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherState())

    private var launchInFlight = false

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

    fun goHome() {
        transient.update(LauncherNavigation::goHome)
    }

    fun retryCatalog() {
        repository.requestRefresh()
    }

    fun updateHomeRoleStatus(isDefaultHome: Boolean) {
        transient.update { it.copy(isDefaultHome = isDefaultHome) }
    }

    /** Releases the double-tap guard; call from Activity.onResume (contract §3.5). */
    fun onActivityResumed() {
        launchInFlight = false
    }

    fun openApp(key: AppKey) {
        if (launchInFlight) return
        when (launcher.launch(key)) {
            LaunchResult.Started -> {
                launchInFlight = true
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
}

class LauncherViewModelFactory(
    private val repository: AppsRepository,
    private val launcher: AppLauncher,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LauncherViewModel(repository, launcher) as T
    }
}
