package dev.cesarmanzocode.ricemobile.ui.shared

import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * System "remove animations" (contract §10): provided once by the host, re-evaluated on every
 * ON_RESUME so a mid-session toggle is honored, and read by every rice's own press/route motion.
 * No new permission, no app-level toggle of our own — this mirrors `ValueAnimator.areAnimatorsEnabled()`.
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * Live 0f..1f progress of the Home<->Drawer gesture-driven transition (interaction sprint), 0 at
 * rest on Home and 1 at rest on Drawer. A *function*, not a `Float`: [dev.cesarmanzocode.ricemobile.launcher.LauncherHost]
 * doesn't recompose every drag frame (only at gesture/screen boundaries), so the only way for a
 * rice to see the smooth, continuous value is to call this inside its own `graphicsLayer { }`
 * block — same draw-time-only read this whole mechanism relies on everywhere else. Lets a rice
 * add its *own* subtle reaction (Arctic: dock lowering, wallpaper parallax) without the host
 * hardcoding any rice's visuals. Default `{ 0f }` so a rice that never reads this
 * (Monochrome/Ember/Ivory/Violet, for now) is unaffected.
 */
val LocalDrawerDragProgress = compositionLocalOf<() -> Float> { { 0f } }

/**
 * Small, centralized motion vocabulary (interaction sprint §23) — durations/specs genuinely shared
 * across more than one call site, not a design-system. Per-rice specifics stay in [dev.cesarmanzocode.ricemobile.rice.RiceMotion];
 * this is only the handful of values the *host*'s gesture mechanics need.
 */
object MotionTokens {
    const val Instant = 80
    const val Fast = 140
    const val Standard = 220
    const val Emphasis = 300

    /** Home<->Drawer panel settle after a released drag or a tap-triggered open/close: a short,
     * critically-damped-ish spring — never a slow decorative bounce (contract: "sin snap torpe"
     * but also "no bonito pero pesado"). */
    val PanelSettleSpring: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 420f)

    /** Reduced-motion stand-in for [PanelSettleSpring]: still animated (a hard cut reads as more
     * broken than a fast fade), just short enough to feel immediate. */
    val ReducedMotionSettle: TweenSpec<Float> = tween(120, easing = FastOutSlowInEasing)

    /** Distance a finger must travel to fully open/close the Home<->Drawer sheet. Deliberately far
     * short of the full screen height — a real launcher's drawer pull never asks for a full-height
     * drag — while still being long enough that per-pixel progress reads as deliberate, not twitchy. */
    val DrawerDragTravel = 320.dp
    const val DrawerOpenThreshold = 0.35f
    const val DrawerCloseThreshold = 0.65f
    val DrawerFlingVelocity = 1100.dp // per second; matches HomeGestureSurface's own fling feel.
}

@Composable
fun rememberReducedMotion(): Boolean {
    var reduced by remember { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reduced = !ValueAnimator.areAnimatorsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return reduced
}

@Composable
fun ProvideReducedMotion(reduced: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalReducedMotion provides reduced, content = content)
}

/**
 * The press feedback every rice's contract §10 row specifies as "escala X, N ms": a scale-only
 * `graphicsLayer` driven by [interactionSource]'s pressed state. Shared *mechanics*, not a shared
 * visual — each rice wires this into its own cell shape/border/tint, and Ivory (pressScale=1f)
 * simply never sees the scale move, using its own tint/background change instead (contract §18.5).
 */
@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    scale: Float,
    durationMs: Int,
): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    return animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = if (reducedMotion) snap() else tween(durationMs),
        label = "press-scale",
    )
}

/**
 * Interaction sprint §6: the shared "touch down -> scale -> spring back" mechanic every tappable
 * app/favorite/category/chip should carry, wired to [rememberPressScale] + a click/long-click, with
 * no Material ripple (`indication = null`) so it never fights a rice's own glass/paper/mono
 * language. [pressScale]/[pressMs] are per-rice values (see [dev.cesarmanzocode.ricemobile.rice.RiceMotion]),
 * never hardcoded here — a rice overrides by passing its own numbers, exactly like every other
 * §10 press treatment. Ivory's tint-only press (contract §18.5, pressScale=1f) is still expressible:
 * a scale of 1f simply never moves.
 */
fun Modifier.ricePressable(
    pressScale: Float,
    pressMs: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interactionSource, pressScale, pressMs)
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickLabel,
        )
}
