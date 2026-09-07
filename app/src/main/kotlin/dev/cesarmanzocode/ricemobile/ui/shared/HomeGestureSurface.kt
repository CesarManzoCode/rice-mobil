package dev.cesarmanzocode.ricemobile.ui.shared

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val SWIPE_THRESHOLD = 64.dp
private val SWIPE_MIN_DISTANCE = 24.dp
private val SWIPE_MIN_VELOCITY = 900.dp // per second

/**
 * Home's free background: swipe up opens the drawer once (contract §7). Ignores horizontal
 * drags, never captures a gesture a child already consumed, and long press is a separate
 * detector that never launches on release.
 */
@Composable
fun HomeGestureSurface(
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var accumulatedUp by remember { mutableFloatStateOf(0f) }
    val thresholdPx = with(density) { SWIPE_THRESHOLD.toPx() }
    val minDistancePx = with(density) { SWIPE_MIN_DISTANCE.toPx() }
    val minVelocityPxPerSec = with(density) { SWIPE_MIN_VELOCITY.toPx() }

    val dragState = rememberDraggableState { delta -> accumulatedUp -= delta }

    var gestureModifier: Modifier = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = dragState,
        onDragStarted = { accumulatedUp = 0f },
        onDragStopped = { velocity ->
            val movedEnough = accumulatedUp >= thresholdPx
            val fastEnough = accumulatedUp >= minDistancePx && -velocity >= minVelocityPxPerSec
            if (movedEnough || fastEnough) onSwipeUp()
            accumulatedUp = 0f
        },
    )
    if (onLongPress != null) {
        gestureModifier = gestureModifier.pointerInput(onLongPress) {
            detectTapGestures(onLongPress = { onLongPress() })
        }
    }

    Box(modifier = modifier.then(gestureModifier), content = content)
}
