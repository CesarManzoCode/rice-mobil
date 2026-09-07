package dev.cesarmanzocode.ricemobile.ui.shared

import android.animation.ValueAnimator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * System "remove animations" (contract §10): provided once by the host, re-evaluated on every
 * ON_RESUME so a mid-session toggle is honored, and read by every rice's own press/route motion.
 * No new permission, no app-level toggle of our own — this mirrors `ValueAnimator.areAnimatorsEnabled()`.
 */
val LocalReducedMotion = compositionLocalOf { false }

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
