package dev.cesarmanzocode.ricemobile.launcher

import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.ui.shared.ScreenRect

enum class LauncherScreen { Home, Drawer, RicePicker }

enum class UiMessageType { CatalogRefreshFailed, AppLaunchFailed, FavoriteLimitReached, WallpaperFailed }

data class UiMessage(val id: Long, val type: UiMessageType)

/** A long-press context menu request (UX overhaul §7-9): [anchor] is the pressed item's own
 * window-relative bounds, captured by [dev.cesarmanzocode.ricemobile.ui.shared.appCellPressable] —
 * carried through transient state so the menu can render anchored to it instead of "de golpe". */
data class AppMenuRequest(val key: AppKey, val anchor: ScreenRect)

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
    val appMenu: AppMenuRequest? = null,
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
    /** Precomputed by the ViewModel (contract perf §5): a rice's Home renders this directly
     * instead of re-deriving it from [favoriteKeys]/[apps] on every recomposition. */
    val favoriteSlots: List<FavoriteSlot> = emptyList(),
    val recentApps: List<AppEntry> = emptyList(),
    val appMenu: AppMenuRequest? = null,
    val isDefaultHome: Boolean = false,
    val preferencesWritable: Boolean = true,
    val wallpaperStatus: WallpaperStatus = WallpaperStatus.Idle,
    val message: UiMessage? = null,
)
