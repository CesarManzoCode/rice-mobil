package dev.cesarmanzocode.ricemobile.rice

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    /** Most-recent-first, this launcher's own local history only (never Android UsageStats).
     * Already capped/deduped upstream; a rice shows as many as its layout wants, or none. */
    val recentApps: List<AppEntry> = emptyList(),
)

data class DrawerModel(
    val query: String,
    val results: List<AppEntry>,
    val favoriteKeys: Set<AppKey>,
    val status: CatalogStatus,
    /** Same local history as [HomeModel.recentApps]; a Drawer's "Recientes" section. */
    val recentApps: List<AppEntry> = emptyList(),
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

/** How a route (Drawer/Picker) enters relative to its final position (contract §10). */
sealed interface MotionDisplacement {
    /** A fixed offset, independent of the container's measured size. */
    data class Fixed(val distance: Dp) : MotionDisplacement

    /** A fraction of the container's height, used only by Violet's sheet (§18.6: ".12 de altura"). */
    data class HeightFraction(val fraction: Float) : MotionDisplacement
}

/**
 * Full §10 motion contract per rice: enter/exit duration+easing, displacement, and the press
 * feedback (scale + duration) every interactive cell in that rice uses. The host drives
 * route/rice transitions from this (contract §16 task 7); presses are applied locally by each
 * rice via [dev.cesarmanzocode.ricemobile.ui.shared.rememberPressScale] so the *values* are
 * shared, never a shared visual widget.
 */
data class RiceMotion(
    val enterMs: Int,
    val exitMs: Int,
    val enterEasing: Easing,
    val exitEasing: Easing,
    val displacement: MotionDisplacement,
    val pressScale: Float,
    val pressMs: Int,
) {
    /** Contract §10 table, one instance per rice; kept in this shared file since the *values*
     * are normative, even though each rice applies them to its own geometry. */
    companion object {
        val Monochrome = RiceMotion(
            enterMs = 140,
            exitMs = 110,
            enterEasing = LinearOutSlowInEasing,
            exitEasing = LinearOutSlowInEasing,
            displacement = MotionDisplacement.Fixed(12.dp),
            pressScale = 0.98f,
            pressMs = 70,
        )
        val Arctic = RiceMotion(
            enterMs = 280,
            exitMs = 220,
            enterEasing = FastOutSlowInEasing,
            exitEasing = FastOutSlowInEasing,
            displacement = MotionDisplacement.Fixed(28.dp),
            pressScale = 0.96f,
            pressMs = 100,
        )
        val Ember = RiceMotion(
            enterMs = 170,
            exitMs = 140,
            enterEasing = FastOutSlowInEasing,
            exitEasing = FastOutLinearInEasing,
            displacement = MotionDisplacement.Fixed(18.dp),
            pressScale = 0.98f,
            pressMs = 70,
        )
        val Ivory = RiceMotion(
            enterMs = 200,
            exitMs = 160,
            enterEasing = FastOutSlowInEasing,
            exitEasing = FastOutSlowInEasing,
            displacement = MotionDisplacement.Fixed(8.dp),
            pressScale = 1f, // Ivory's press is a tint/background change, not a scale (§10).
            pressMs = 90,
        )
        val Violet = RiceMotion(
            enterMs = 340,
            exitMs = 260,
            enterEasing = FastOutSlowInEasing,
            exitEasing = FastOutSlowInEasing,
            displacement = MotionDisplacement.HeightFraction(0.12f),
            pressScale = 0.96f,
            pressMs = 110,
        )
    }
}

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
