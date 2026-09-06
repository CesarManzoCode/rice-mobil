package dev.cesarmanzocode.ricemobile.launcher

import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus

/**
 * S1 has only Home and Drawer; RicePicker joins in S2 once a Rice selector exists
 * (contract §4, "estado completo y acciones en S2").
 */
enum class LauncherScreen { Home, Drawer }

enum class UiMessageType { CatalogRefreshFailed, AppLaunchFailed }

data class UiMessage(val id: Long, val type: UiMessageType)

/**
 * Route/query/message/role-status: the transient part of launcher state, held together
 * because it shares a single owner and update cadence (contract §6.3).
 */
data class TransientState(
    val screen: LauncherScreen = LauncherScreen.Home,
    val query: String = "",
    val isDefaultHome: Boolean = false,
    val message: UiMessage? = null,
)

/**
 * S1 subset of the full launcher state from contract §5; S2 extends this same data class
 * with rice/favorites/wallpaper fields rather than introducing a second state holder.
 */
data class LauncherState(
    val screen: LauncherScreen = LauncherScreen.Home,
    val catalogStatus: CatalogStatus = CatalogStatus.Loading,
    val apps: List<AppEntry> = emptyList(),
    val query: String = "",
    val results: List<AppEntry> = emptyList(),
    val isDefaultHome: Boolean = false,
    val message: UiMessage? = null,
)
