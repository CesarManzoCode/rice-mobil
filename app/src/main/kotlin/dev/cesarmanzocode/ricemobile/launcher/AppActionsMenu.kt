package dev.cesarmanzocode.ricemobile.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.rice.RiceRegistry
import dev.cesarmanzocode.ricemobile.ui.shared.LocalReducedMotion
import dev.cesarmanzocode.ricemobile.ui.shared.ScreenRect
import kotlin.math.roundToInt

/**
 * The anchored, per-rice long-press context menu (UX overhaul §7-9): replaces the previous
 * generic Material `DropdownMenu`, which had no anchor position and simply appeared "de golpe".
 * [request] carries the pressed item's own window-relative bounds ([AppMenuRequest.anchor]) —
 * the menu grows *from that point* on open and, on dismiss, [AnimatedVisibility] keeps this
 * composable (and the [Popup] inside it) alive long enough to shrink back toward it instead of
 * disappearing instantly (contract item 5/6: "no un simple corte", "dismiss vuelve al punto de
 * origen"). Lives in `launcher`, not `ui.shared`, for the same reason [WallpaperRetryBanner] does:
 * it is a host-owned global overlay (contract §6.2), not a per-rice widget.
 */
@Composable
fun AppActionsMenuHost(
    request: AppMenuRequest?,
    riceId: RiceId,
    favoriteKeys: Set<AppKey>,
    onDismiss: () -> Unit,
    onToggleFavorite: (AppKey) -> Unit,
) {
    // Frozen at the last non-null request so the exit animation below has something to shrink
    // back toward even after `request` itself has already gone null (the ViewModel dismisses
    // immediately; only this composable's own presence lingers for the animation).
    var lastRequest by remember { mutableStateOf(request) }
    if (request != null) lastRequest = request
    val current = lastRequest ?: return
    val reducedMotion = LocalReducedMotion.current
    val haptics = LocalHapticFeedback.current
    val style = RiceRegistry.of(riceId).motion.menuStyle
    val density = LocalDensity.current
    val isFavorite = current.key in favoriteKeys

    AnimatedVisibility(
        visible = request != null,
        // No visual transform here: a Popup is a portal outside the normal layout tree, so the
        // outer wrapper is zero-sized regardless. What matters is that its exit duration matches
        // the real (inner) exit below — Compose does not dispose this subtree until every
        // animateEnterExit driven by this same Transition finishes, not just the outer's own.
        enter = EnterTransition.None,
        exit = if (reducedMotion) ExitTransition.None else fadeOut(tween(EXIT_MS)),
    ) {
        Popup(
            popupPositionProvider = remember(current.anchor, density) {
                AnchoredMenuPositionProvider(current.anchor, density)
            },
            onDismissRequest = onDismiss,
            // Back is already owned by LauncherHost's own BackHandler (gated on
            // LauncherNavigation.menuBackHandlerEnabled) so predictive-back/back-precedence stays
            // single-sourced; the Popup itself only needs to own outside-tap dismiss.
            properties = PopupProperties(focusable = true, dismissOnBackPress = false, dismissOnClickOutside = true),
        ) {
            Column(
                modifier = Modifier
                    .animateEnterExit(
                        enter = scaleIn(
                            animationSpec = if (reducedMotion) tween(0) else tween(ENTER_MS),
                            initialScale = 0.7f,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                        ) + fadeIn(tween(if (reducedMotion) 0 else ENTER_MS)),
                        exit = scaleOut(
                            animationSpec = if (reducedMotion) tween(0) else tween(EXIT_MS),
                            targetScale = 0.78f,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                        ) + fadeOut(tween(if (reducedMotion) 0 else EXIT_MS)),
                    )
                    .widthIn(min = 172.dp, max = 240.dp)
                    .clip(style.shape)
                    .background(style.background)
                    .border(style.borderWidth, style.border, style.shape),
            ) {
                MenuAction(
                    text = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite),
                    ink = style.ink,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite(current.key)
                        onDismiss()
                    },
                )
            }
        }
    }
}

private const val ENTER_MS = 140
private const val EXIT_MS = 110

@Composable
private fun MenuAction(text: String, ink: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .defaultMinSize(minWidth = 172.dp, minHeight = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, color = ink)
    }
}

/** Positions purely from the pressed item's own captured [ScreenRect] — never from the Popup's
 * own [anchorBounds], which would just be this composable's call-site position (the full-screen
 * host root), not the item that was actually pressed. Prefers opening below the item, flips above
 * when there isn't room, and always clamps inside the window so it never renders off-screen. */
private class AnchoredMenuPositionProvider(
    private val anchor: ScreenRect,
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gap = with(density) { 8.dp.roundToPx() }
        val desiredX = (anchor.centerX - popupContentSize.width / 2f).roundToInt()
        val maxX = (windowSize.width - popupContentSize.width - gap).coerceAtLeast(gap)
        val x = desiredX.coerceIn(gap, maxX)

        val spaceBelow = windowSize.height - anchor.bottom
        val openBelow = spaceBelow >= popupContentSize.height + gap
        val rawY = if (openBelow) (anchor.bottom + gap) else (anchor.top - popupContentSize.height - gap)
        val maxY = (windowSize.height - popupContentSize.height - gap).coerceAtLeast(gap)
        val y = rawY.roundToInt().coerceIn(gap, maxY)
        return IntOffset(x, y)
    }
}
