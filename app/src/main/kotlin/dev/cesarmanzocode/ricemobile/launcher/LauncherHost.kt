package dev.cesarmanzocode.ricemobile.launcher

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.IconLoader
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.MotionDisplacement
import dev.cesarmanzocode.ricemobile.rice.Rice
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.rice.RicePicker
import dev.cesarmanzocode.ricemobile.rice.RiceRegistry
import dev.cesarmanzocode.ricemobile.ui.shared.LocalDrawerDragProgress
import dev.cesarmanzocode.ricemobile.ui.shared.LocalIconLoader
import dev.cesarmanzocode.ricemobile.ui.shared.MotionTokens
import dev.cesarmanzocode.ricemobile.ui.shared.ProvideReducedMotion
import dev.cesarmanzocode.ricemobile.ui.shared.rememberReducedMotion
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val PREFERENCES_LOADING_BACKGROUND = Color(0xFF0A0A0A)
private val DRAWER_SCRIM_MAX_ALPHA = 0.45f
private val HOME_RECEDE_MIN_SCALE = 0.95f
private val HOME_RECEDE_MIN_ALPHA = 0.9f

/** Whether the current route pair is Home<->Drawer of the *same* rice — the gesture-driven overlay
 * owns this pair entirely; a rice change or the Picker still go through [AnimatedContent] below. */
private data class RiceRouteKey(val riceId: RiceId, val isPicker: Boolean)

private fun displacementPx(displacement: MotionDisplacement, fullHeight: Int, density: Density): Int = when (displacement) {
    is MotionDisplacement.Fixed -> with(density) { displacement.distance.roundToPx() }
    is MotionDisplacement.HeightFraction -> (displacement.fraction * fullHeight).roundToInt()
}

/**
 * Owns transitions between routes, the global menu/message overlay, the picker, and window
 * concerns (contract §6.2), never the design of a rice's Home/Drawer. Renders only the current
 * rice, one instance at a time (contract §10: "no mantener cinco composiciones vivas").
 *
 * Home<->Drawer (interaction sprint §2-5, §31) is no longer a plain [AnimatedContent] route swap:
 * it is a live, gesture-driven 0f..1f panel (`drawerProgress` below) that a rice's swipe surface
 * can follow continuously, that a tap affordance animates through the same 0->1 path, and that
 * system predictive back scrubs directly — never a separate, disconnected "enter animation" that
 * only starts once the state has already changed. [AnimatedContent] still owns the Picker and a
 * rice change (contract §10: "crossfade de 180 ms" is a fixed contract value, untouched here).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LauncherHost(
    viewModel: LauncherViewModel,
    iconLoader: IconLoader,
    onRequestHomeRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Frozen at whatever Home/Drawer was showing the instant `screen` becomes RicePicker, and live
    // (tracking `state.screen`) the rest of the time. AnimatedContent below keeps the *outgoing*
    // content lambda invocation alive (with this same live `state`) for the whole Picker enter/exit
    // transition — without this, that instance would flip to reading `screen == RicePicker` too and
    // render nothing instead of the Home/Drawer it's supposed to be fading out from/into.
    var lastNonPickerScreen by remember { mutableStateOf(LauncherScreen.Home) }
    if (state.screen != LauncherScreen.RicePicker) lastNonPickerScreen = state.screen
    val imeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Read fresh from any closure below, however long that closure has been remembered for
    // (Kotlin captures the delegate, not a value snapshot — same guarantee `by remember` already
    // relies on elsewhere in this function, e.g. `dragActive`/`drawerSettle` below).
    val reducedMotionState = rememberUpdatedState(reducedMotion)
    val densityState = rememberUpdatedState(density)

    BackHandler(enabled = LauncherNavigation.menuBackHandlerEnabled(state.appMenu)) {
        viewModel.dismissAppMenu()
    }
    BackHandler(enabled = LauncherNavigation.imeBackHandlerEnabled(state.screen, imeVisible)) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    BackHandler(enabled = LauncherNavigation.homeNoOpBackHandlerEnabled(state.screen, state.isDefaultHome)) {
        // No-op: a default Home never exits to a previous Home.
    }

    // --- Home<->Drawer gesture-driven panel (interaction sprint §2-5) ---
    // `dragActive`/`rawDrawerProgress` are the live value while a finger is down (updated
    // synchronously, no coroutine hop, so the panel tracks the finger 1:1); `drawerSettle` is the
    // authoritative value the rest of the time (resting, or mid spring-settle). Both are genuine
    // Compose State, so a `graphicsLayer { }` block reading them redraws without recomposing the
    // Home/Drawer subtree underneath (contract §10/§25: "animar alpha/translation/scale... no
    // recalcular catálogo... por frame").
    var dragActive by remember { mutableStateOf(false) }
    var rawDrawerProgress by remember { mutableFloatStateOf(0f) }
    val drawerSettle = remember { Animatable(if (state.screen == LauncherScreen.Drawer) 1f else 0f) }

    // Defensive resync only — the normal path already leaves `drawerSettle` at the right resting
    // value before `state.screen` changes (see `settleDrawer` below), so this never causes a
    // visible jump; it only guards against `screen` changing from somewhere outside this gesture
    // (e.g. a rice switch resetting to Home).
    LaunchedEffect(state.screen) {
        if (!dragActive) {
            val target = if (state.screen == LauncherScreen.Drawer) 1f else 0f
            if (drawerSettle.value != target) drawerSettle.snapTo(target)
        }
    }

    // Stable identity (created once): safe to pass through a CompositionLocal or as a plain
    // parameter without ever looking "changed" to Compose's equality check, unlike a fresh
    // `::function` reference recreated on every recomposition would.
    val drawerProgress = remember { { if (dragActive) rawDrawerProgress else drawerSettle.value } }

    fun settleDrawer(target: Float, onArrived: (() -> Unit)? = null) {
        val start = drawerProgress()
        dragActive = false
        scope.launch {
            if (drawerSettle.value != start) drawerSettle.snapTo(start)
            val spec = if (reducedMotionState.value) MotionTokens.ReducedMotionSettle else MotionTokens.PanelSettleSpring
            drawerSettle.animateTo(target, spec)
            onArrived?.invoke()
        }
    }

    fun beginDrawerDrag() {
        dragActive = true
        rawDrawerProgress = drawerSettle.value
    }

    /** [deltaTowardOpenPx] is signed: positive moves the panel toward Drawer(1), negative toward
     * Home(0) — the open gesture (finger moving up) and the close gesture (finger moving down,
     * §"grabber" below) both funnel through this one accumulator. */
    fun dragDrawerBy(deltaTowardOpenPx: Float) {
        if (!dragActive) return
        val d = with(densityState.value) { MotionTokens.DrawerDragTravel.toPx() }
        rawDrawerProgress = (rawDrawerProgress + deltaTowardOpenPx / d).coerceIn(0f, 1f)
    }

    fun endOpenDrag(velocityUpPxPerSec: Float) {
        if (!dragActive) return
        val flingPx = with(densityState.value) { MotionTokens.DrawerFlingVelocity.toPx() }
        val commit = rawDrawerProgress > MotionTokens.DrawerOpenThreshold || velocityUpPxPerSec > flingPx
        if (commit) settleDrawer(1f) { viewModel.openDrawer() } else settleDrawer(0f)
    }

    fun endCloseDrag(velocityDownPxPerSec: Float) {
        if (!dragActive) return
        val flingPx = with(densityState.value) { MotionTokens.DrawerFlingVelocity.toPx() }
        val commit = rawDrawerProgress < MotionTokens.DrawerCloseThreshold || velocityDownPxPerSec > flingPx
        if (commit) settleDrawer(0f) { viewModel.goHome() } else settleDrawer(1f)
    }

    // Picker predictive back keeps the pre-existing mechanism (an AnimatedContent still owns that
    // route): a translate+fade preview of the *whole* outgoing content while the gesture streams,
    // then either a same-frame commit or an animated cancel back to 0.
    var skipNextTransition by remember { mutableStateOf(false) }
    val backProgress = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = LauncherNavigation.pickerBackHandlerEnabled(state.screen)) { progress ->
        try {
            progress.collect { event -> backProgress.snapTo(event.progress) }
            skipNextTransition = true
            viewModel.goHome()
            backProgress.snapTo(0f)
        } catch (cancellation: CancellationException) {
            backProgress.animateTo(0f, tween(150))
            throw cancellation
        }
    }

    // Drawer predictive back (interaction sprint §31) drives the *same* live panel a manual drag
    // would: the Drawer visibly follows the system back gesture, and cancelling springs back open
    // instead of jumping — never a separate, disconnected preview.
    PredictiveBackHandler(enabled = LauncherNavigation.drawerBackHandlerEnabled(state.screen, imeVisible)) { progress ->
        dragActive = true
        try {
            progress.collect { event -> rawDrawerProgress = (1f - event.progress).coerceIn(0f, 1f) }
            dragActive = false
            viewModel.goHome()
            drawerSettle.snapTo(0f)
        } catch (cancellation: CancellationException) {
            dragActive = false
            settleDrawer(1f)
            throw cancellation
        }
    }

    if (!state.preferencesReady) {
        // Neutral surface until preferences load: never guess the rice before we know it.
        Box(modifier = modifier.fillMaxSize().background(PREFERENCES_LOADING_BACKGROUND))
        return
    }

    val actions = remember(viewModel) {
        RiceActions(
            // A tap affordance runs the exact same 0->1 settle a completed gesture would (§5):
            // never a separate, instant route switch.
            openDrawer = { settleDrawer(1f) { viewModel.openDrawer() } },
            openPicker = viewModel::openPicker,
            goHome = { settleDrawer(0f) { viewModel.goHome() } },
            updateQuery = viewModel::updateQuery,
            openApp = viewModel::openApp,
            showAppMenu = viewModel::showAppMenu,
            requestHomeRole = onRequestHomeRole,
            retryCatalog = viewModel::retryCatalog,
            beginDrawerDrag = { beginDrawerDrag() },
            dragDrawer = { deltaUpPx -> dragDrawerBy(deltaUpPx) },
            endDrawerDrag = { velocityUpPxPerSec -> endOpenDrag(velocityUpPxPerSec) },
        )
    }

    val view = LocalView.current
    LaunchedEffect(state.rice) {
        val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        val lightBackground = RiceRegistry.of(state.rice).lightSystemBars
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightBackground
        }
    }

    ProvideReducedMotion(reducedMotion) {
        CompositionLocalProvider(LocalIconLoader provides iconLoader) {
            val routeKey = RiceRouteKey(state.rice, state.screen == LauncherScreen.RicePicker)

            AnimatedContent(
                targetState = routeKey,
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = backProgress.value
                        translationY = p * size.height * 0.04f
                        alpha = 1f - p * 0.35f
                    },
                transitionSpec = {
                    val instant = skipNextTransition
                    val riceChanged = initialState.riceId != targetState.riceId
                    when {
                        instant -> EnterTransition.None togetherWith ExitTransition.None
                        riceChanged || reducedMotion -> {
                            val crossfadeMs = if (reducedMotion) 0 else 180
                            fadeIn(tween(crossfadeMs)) togetherWith fadeOut(tween(crossfadeMs))
                        }
                        else -> {
                            val motion = RiceRegistry.of(targetState.riceId).motion
                            val enter = fadeIn(tween(motion.enterMs, easing = motion.enterEasing)) +
                                slideInVertically(tween(motion.enterMs, easing = motion.enterEasing)) { fullHeight ->
                                    displacementPx(motion.displacement, fullHeight, density)
                                }
                            val exit = fadeOut(tween(motion.exitMs, easing = motion.exitEasing)) +
                                slideOutVertically(tween(motion.exitMs, easing = motion.exitEasing)) { fullHeight ->
                                    displacementPx(motion.displacement, fullHeight, density)
                                }
                            enter togetherWith exit
                        }
                    }
                },
                label = "rice-route",
            ) { key ->
                val rice = RiceRegistry.of(key.riceId)
                if (key.isPicker) {
                    RicePicker(current = state.rice, onSelect = viewModel::selectRice)
                } else {
                    HomeDrawerPanel(
                        screen = lastNonPickerScreen,
                        rice = rice,
                        homeModel = HomeModel(
                            favorites = state.favoriteSlots,
                            isDefaultHome = state.isDefaultHome,
                            recentApps = state.recentApps,
                        ),
                        drawerModel = DrawerModel(
                            query = state.query,
                            results = state.results,
                            favoriteKeys = state.favoriteKeys.toSet(),
                            status = state.catalogStatus,
                            recentApps = state.recentApps,
                        ),
                        actions = actions,
                        dragActiveNow = dragActive,
                        progressProvider = drawerProgress,
                        onGrabberDragStart = ::beginDrawerDrag,
                        onGrabberDrag = { deltaDownPx -> dragDrawerBy(-deltaDownPx) },
                        onGrabberDragEnd = ::endCloseDrag,
                        onGrabberTap = { settleDrawer(0f) { viewModel.goHome() } },
                    )
                }
            }

            LaunchedEffect(routeKey) { skipNextTransition = false }

            state.appMenu?.let { key ->
                AppActionsMenu(
                    key = key,
                    isFavorite = key in state.favoriteKeys,
                    onDismiss = viewModel::dismissAppMenu,
                    onToggleFavorite = viewModel::toggleFavorite,
                )
            }

            if (state.wallpaperStatus is WallpaperStatus.Failed) {
                WallpaperRetryBanner(onRetry = viewModel::retryWallpaper, modifier = modifier)
            }
        }
    }
}

/**
 * The live Home<->Drawer surface (interaction sprint §2-5, §27): Home stays composed and visible
 * underneath (receding very slightly in scale/alpha as `progressProvider()` rises), a scrim ramps
 * in over it, and the Drawer — a grabber handle above the rice's own content — slides up from
 * below in lock-step with the same value. [progressProvider] is a function, not a `Float`
 * parameter, precisely so this composable does not itself recompose every drag tick: every reader
 * of the live value sits inside a `graphicsLayer { }` block (contract §10/§25/§26 — draw-time
 * property changes only, never a recomposition of the catalog/grid underneath).
 */
@Composable
private fun HomeDrawerPanel(
    screen: LauncherScreen,
    rice: Rice,
    homeModel: HomeModel,
    drawerModel: DrawerModel,
    actions: RiceActions,
    dragActiveNow: Boolean,
    progressProvider: () -> Float,
    onGrabberDragStart: () -> Unit,
    onGrabberDrag: (Float) -> Unit,
    onGrabberDragEnd: (Float) -> Unit,
    onGrabberTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // These two booleans are the only *recomposition*-triggering reads in this function — they
    // flip only at gesture/screen boundaries (a handful of times per interaction), never per
    // frame. The continuous per-frame value only ever reaches a `graphicsLayer` block below.
    val homePresent = dragActiveNow || screen == LauncherScreen.Home
    val drawerPresent = dragActiveNow || screen == LauncherScreen.Drawer

    Box(modifier = modifier.fillMaxSize()) {
        if (homePresent) {
            // Provides the *function*, not its current value — see [LocalDrawerDragProgress]. A
            // rice (Arctic) calls it inside its own `graphicsLayer` blocks for a smooth, per-frame
            // parallax without this Box itself needing to recompose on every drag tick.
            CompositionLocalProvider(LocalDrawerDragProgress provides progressProvider) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = progressProvider()
                            val scale = HOME_RECEDE_MIN_SCALE + (1f - HOME_RECEDE_MIN_SCALE) * (1f - p)
                            scaleX = scale
                            scaleY = scale
                            alpha = HOME_RECEDE_MIN_ALPHA + (1f - HOME_RECEDE_MIN_ALPHA) * (1f - p)
                        },
                ) {
                    rice.Home(model = homeModel, actions = actions)
                }
            }
        }
        if (drawerPresent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progressProvider().coerceIn(0f, 1f) * DRAWER_SCRIM_MAX_ALPHA }
                    .background(Color.Black),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = (1f - progressProvider()) * size.height },
            ) {
                DrawerGrabber(
                    onDragStart = onGrabberDragStart,
                    onDrag = onGrabberDrag,
                    onDragEnd = onGrabberDragEnd,
                    onTap = onGrabberTap,
                )
                Box(modifier = Modifier.weight(1f)) {
                    rice.Drawer(model = drawerModel, actions = actions)
                }
            }
        }
    }
}

/**
 * A small bottom-sheet-style handle above the rice's own Drawer content (interaction sprint §4,
 * §21, §30): dragging it down closes the panel (mirroring the open gesture) without competing
 * with the Drawer's own scrollable content for vertical drag priority, and it is tappable too —
 * closing is never gesture-only (contract accessibility: back/affordance always available
 * alongside a gesture).
 */
@Composable
private fun DrawerGrabber(
    onDragStart: () -> Unit,
    onDrag: (deltaDownPx: Float) -> Unit,
    onDragEnd: (velocityDownPxPerSec: Float) -> Unit,
    onTap: () -> Unit,
) {
    val dragState = rememberDraggableState { delta -> onDrag(delta) }
    val label = stringResource(R.string.action_close_drawer_hint)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = dragState,
                onDragStarted = { onDragStart() },
                onDragStopped = { velocity -> onDragEnd(velocity) },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.32f)),
        )
    }
}

/** Contract §8: a wallpaper failure never reverts the rice or blocks Home, it only offers retry. */
@Composable
private fun WallpaperRetryBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE6202020))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.wallpaper_failed), color = Color.White)
            TextButton(onClick = onRetry) { Text(text = stringResource(R.string.action_retry)) }
        }
    }
}
