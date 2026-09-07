package dev.cesarmanzocode.ricemobile.rice

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperSpec

/** A favorite slot: [app] is null when the identity is temporarily unavailable (contract §5.2). */
data class FavoriteSlot(val key: AppKey, val app: AppEntry?)

/**
 * Pure derivation (contract §5.2): a favorite key not present in [apps] becomes an unavailable
 * slot instead of disappearing. No Android types, so this is directly unit-testable; an empty
 * or loading [apps] list never removes anything from [favoriteKeys], it only marks every slot
 * unavailable.
 */
fun buildFavoriteSlots(favoriteKeys: List<AppKey>, apps: List<AppEntry>): List<FavoriteSlot> {
    val appsByKey = apps.associateBy { it.key }
    return favoriteKeys.map { key -> FavoriteSlot(key = key, app = appsByKey[key]) }
}

data class HomeModel(
    val favorites: List<FavoriteSlot>,
    val isDefaultHome: Boolean,
)

data class DrawerModel(
    val query: String,
    val results: List<AppEntry>,
    val favoriteKeys: Set<AppKey>,
    val status: CatalogStatus,
)

data class RiceActions(
    val openDrawer: () -> Unit,
    val openPicker: () -> Unit,
    val goHome: () -> Unit,
    val updateQuery: (String) -> Unit,
    val openApp: (AppKey) -> Unit,
    val showAppMenu: (AppKey) -> Unit,
    val requestHomeRole: () -> Unit,
    val retryCatalog: () -> Unit,
)

/**
 * Enter/exit timings from contract §10. S2 only carries these numbers on the interface;
 * the host does not yet drive `AnimatedContent`/`PredictiveBackHandler` from them (that
 * wiring, and the rest of §10's easing/press detail, is Sprint 3 polish).
 */
data class RiceMotion(
    val drawerEnterMs: Int,
    val drawerExitMs: Int,
)

/**
 * Contract §6.1. Five real implementations; no `BaseRiceHome`. Each rice owns its full
 * geometry and receives only models/actions, never a ViewModel, DataStore or Activity.
 */
interface Rice {
    val id: RiceId
    val wallpaper: WallpaperSpec
    val motion: RiceMotion
    val lightSystemBars: Boolean

    @Composable
    fun Home(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier)

    @Composable
    fun Drawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier)
}
