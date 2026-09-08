package dev.cesarmanzocode.ricemobile.rice

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.ui.shared.ScreenRect
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperSpec

/** A favorite slot: [app] is null when the identity is temporarily unavailable (contract §5.2). */
@Immutable
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

/** All lists here are freshly-derived snapshots from [dev.cesarmanzocode.ricemobile.launcher.LauncherViewModel]
 * and never mutated after construction, so this is honestly immutable — not just "probably fine"
 * (contract perf: a rice's Home must be skippable when an unrelated part of launcher state, e.g.
 * a toast or wallpaper status, changes but the home model itself did not). */
@Immutable
data class HomeModel(
    val favorites: List<FavoriteSlot>,
    val isDefaultHome: Boolean,
    /** Most-recent-first, this launcher's own local history only (never Android UsageStats).
     * Already capped/deduped upstream; a rice shows as many as its layout wants, or none. */
    val recentApps: List<AppEntry> = emptyList(),
)

/** See [HomeModel] docs: same immutability guarantee applies here. */
@Immutable
data class DrawerModel(
    val query: String,
    val results: List<AppEntry>,
    val favoriteKeys: Set<AppKey>,
    val status: CatalogStatus,
    /** Same local history as [HomeModel.recentApps]; a Drawer's "Recientes" section. */
    val recentApps: List<AppEntry> = emptyList(),
)

data class RiceActions(
    /** Committed intent to be on the Drawer — a tap affordance calls this directly; the host
     * always runs the same gesture-driven 0->1 settle before actually switching screens
     * (interaction sprint §5: "debe iniciar inmediatamente la misma transición"). */
    val openDrawer: () -> Unit,
    val openPicker: () -> Unit,
    /** Committed intent to be back on Home from the Drawer; same settle-then-switch treatment. */
    val goHome: () -> Unit,
    val updateQuery: (String) -> Unit,
    val openApp: (AppKey) -> Unit,
    /** [anchor] is the pressed item's own window-relative bounds (UX overhaul §7-9), captured by
     * [dev.cesarmanzocode.ricemobile.ui.shared.appCellPressable] — the menu anchors to it instead
     * of appearing with no spatial relationship to what was pressed. */
    val showAppMenu: (AppKey, ScreenRect) -> Unit,
    val requestHomeRole: () -> Unit,
    val retryCatalog: () -> Unit,
    /** Live Home->Drawer drag reporting (interaction sprint §3), additive to [openDrawer]: a rice
     * wires these into its own swipe surface to make the Drawer follow the finger instead of only
     * reacting once a threshold fires. Optional — a rice that never calls these simply never drives
     * a live drag, and [openDrawer] alone still works (a plain, spring-settled 0->1 transition). */
    val beginDrawerDrag: () -> Unit = {},
    val dragDrawer: (deltaUpPx: Float) -> Unit = {},
    val endDrawerDrag: (velocityUpPxPerSec: Float) -> Unit = {},
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
    /** UX overhaul §19-20: the *mechanic* every press uses ([dev.cesarmanzocode.ricemobile.ui.shared.rememberPressScale])
     * is shared, this is what makes the *result* not read as "five skins of the same animation" —
     * null falls back to the plain `tween(pressMs)` every rice used before this pass. */
    val pressSpec: AnimationSpec<Float>? = null,
    val menuStyle: ContextMenuStyle,
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
            // "Quirúrgico": critically damped, near-zero overshoot — a snap, not a bounce.
            pressSpec = spring(dampingRatio = 1f, stiffness = 1400f),
            menuStyle = ContextMenuStyle(
                background = Color(0xFF0A0A0A),
                ink = Color(0xFFF5F5F0),
                border = Color(0xFFF5F5F0),
                shape = RectangleShape,
                borderWidth = 1.dp,
            ),
        )
        val Arctic = RiceMotion(
            enterMs = 280,
            exitMs = 220,
            enterEasing = FastOutSlowInEasing,
            exitEasing = FastOutSlowInEasing,
            displacement = MotionDisplacement.Fixed(28.dp),
            pressScale = 0.96f,
            pressMs = 100,
            // "Líquido pero rápido": a hair of overshoot, glass settling into place.
            pressSpec = spring(dampingRatio = 0.78f, stiffness = 520f),
            menuStyle = ContextMenuStyle(
                background = Color(0xE60F2436),
                ink = Color(0xFFF5FBFF),
                border = Color(0x82FFFFFF),
                shape = RoundedCornerShape(20.dp),
                borderWidth = 1.dp,
            ),
        )
        val Ember = RiceMotion(
            enterMs = 170,
            exitMs = 140,
            enterEasing = FastOutSlowInEasing,
            exitEasing = FastOutLinearInEasing,
            displacement = MotionDisplacement.Fixed(18.dp),
            pressScale = 0.98f,
            pressMs = 70,
            // "Piezas encajando": an underdamped spring — a real mechanical overshoot on release.
            pressSpec = spring(dampingRatio = 0.42f, stiffness = 700f),
            menuStyle = ContextMenuStyle(
                background = Color(0xFF24201B),
                ink = Color(0xFFF1E8DC),
                border = Color(0xFFD99A67),
                shape = CutCornerShape(6.dp),
                borderWidth = 1.dp,
            ),
        )
        val Ivory = RiceMotion(
            enterMs = 200,
            exitMs = 160,
            enterEasing = FastOutSlowInEasing,
            exitEasing = FastOutSlowInEasing,
            displacement = MotionDisplacement.Fixed(8.dp),
            pressScale = 1f, // Ivory's press is a tint/background change, not a scale (§10).
            pressMs = 90,
            // Editorial: almost no bounce, a soft settle — "poco bounce" per the brief.
            pressSpec = spring(dampingRatio = 1f, stiffness = 380f),
            menuStyle = ContextMenuStyle(
                background = Color(0xFFF3EBDD),
                ink = Color(0xFF25231E),
                border = Color(0xFFAAA08D),
                shape = RoundedCornerShape(2.dp),
                borderWidth = 1.dp,
            ),
        )
        val Violet = RiceMotion(
            enterMs = 340,
            exitMs = 260,
            enterEasing = FastOutSlowInEasing,
            exitEasing = FastOutSlowInEasing,
            displacement = MotionDisplacement.HeightFraction(0.12f),
            pressScale = 0.96f,
            pressMs = 110,
            // "Profundidad/halo": a slow, soft spring — cinematic, never snappy.
            pressSpec = spring(dampingRatio = 0.62f, stiffness = 260f),
            menuStyle = ContextMenuStyle(
                background = Color(0xF2201638),
                ink = Color(0xFFF3EDFF),
                border = Color(0xFFBC9BFF),
                shape = RoundedCornerShape(22.dp),
                borderWidth = 1.dp,
            ),
        )
    }
}

/** UX overhaul §7-9: the anchored context menu's chrome, one instance per rice so it reads as
 * "part of the rice" instead of a shared Material widget with recolored text (contract §16: shared
 * infra, never shared visual result). */
@Immutable
data class ContextMenuStyle(
    val background: Color,
    val ink: Color,
    val border: Color,
    val shape: Shape,
    val borderWidth: Dp,
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
