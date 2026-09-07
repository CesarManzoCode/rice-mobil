package dev.cesarmanzocode.ricemobile.launcher

import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.rice.RiceId

enum class LauncherScreen { Home, Drawer, RicePicker }

enum class UiMessageType { CatalogRefreshFailed, AppLaunchFailed, FavoriteLimitReached, WallpaperFailed }

data class UiMessage(val id: Long, val type: UiMessageType)

sealed interface WallpaperStatus {
    data object Idle : WallpaperStatus
    data object Applying : WallpaperStatus
    data class Failed(val riceId: RiceId) : WallpaperStatus
}

/**
 * Route/query/menu/message/operative-status: the transient part of launcher state (contract
 * §6.3). Persisted preferences (rice/favorites/wallpaper marker) never live here — they come
 * exclusively from [dev.cesarmanzocode.ricemobile.preferences.PreferencesRepository].
 */
data class TransientState(
    val screen: LauncherScreen = LauncherScreen.Home,
    val query: String = "",
    val isDefaultHome: Boolean = false,
    val appMenu: AppKey? = null,
    val message: UiMessage? = null,
)

/** Full S2 launcher state (contract §5). */
data class LauncherState(
    val preferencesReady: Boolean = false,
    val rice: RiceId = RiceId.Default,
    val screen: LauncherScreen = LauncherScreen.Home,
    val catalogStatus: CatalogStatus = CatalogStatus.Loading,
    val apps: List<AppEntry> = emptyList(),
    val query: String = "",
    val results: List<AppEntry> = emptyList(),
    val favoriteKeys: List<AppKey> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val recentApps: List<AppEntry> = emptyList(),
    val appMenu: AppKey? = null,
    val isDefaultHome: Boolean = false,
    val preferencesWritable: Boolean = true,
    val wallpaperStatus: WallpaperStatus = WallpaperStatus.Idle,
    val message: UiMessage? = null,
)
