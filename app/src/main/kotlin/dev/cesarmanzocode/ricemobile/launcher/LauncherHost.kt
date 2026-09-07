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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.IconLoader
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.MotionDisplacement
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.rice.RicePicker
import dev.cesarmanzocode.ricemobile.rice.RiceRegistry
import dev.cesarmanzocode.ricemobile.ui.shared.AppActionsMenu
import dev.cesarmanzocode.ricemobile.ui.shared.LocalIconLoader
import dev.cesarmanzocode.ricemobile.ui.shared.ProvideReducedMotion
import dev.cesarmanzocode.ricemobile.ui.shared.rememberReducedMotion
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException

private val PREFERENCES_LOADING_BACKGROUND = Color(0xFF0A0A0A)

private data class RouteKey(val riceId: RiceId, val screen: LauncherScreen)

private fun displacementPx(displacement: MotionDisplacement, fullHeight: Int, density: Density): Int = when (displacement) {
    is MotionDisplacement.Fixed -> with(density) { displacement.distance.roundToPx() }
    is MotionDisplacement.HeightFraction -> (displacement.fraction * fullHeight).roundToInt()
}

/**
 * Owns transitions between routes, the global menu/message overlay, the picker, and window
 * concerns (contract §6.2) — never the design of a rice's Home/Drawer. Renders only the current
 * rice, one instance at a time (contract §10: "no mantener cinco composiciones vivas").
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
    val imeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current

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

    // Drawer(!IME)/Picker exit as one predictive-back-capable handler (contract §7/§10): on API
    // 34+ this drives real gesture progress; on older APIs `PredictiveBackHandler` degrades to a
    // single commit, so it fully replaces (not adds to) the old plain BackHandler for this case.
    var skipNextTransition by remember { mutableStateOf(false) }
    val backProgress = remember { Animatable(0f) }
    val routeBackEnabled = LauncherNavigation.drawerBackHandlerEnabled(state.screen, imeVisible) ||
        LauncherNavigation.pickerBackHandlerEnabled(state.screen)
    PredictiveBackHandler(enabled = routeBackEnabled) { progress ->
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

    if (!state.preferencesReady) {
        // Neutral surface until preferences load: never guess the rice before we know it.
        Box(modifier = modifier.fillMaxSize().background(PREFERENCES_LOADING_BACKGROUND))
        return
    }

    val actions = remember(viewModel) {
        RiceActions(
            openDrawer = viewModel::openDrawer,
            openPicker = viewModel::openPicker,
            goHome = viewModel::goHome,
            updateQuery = viewModel::updateQuery,
            openApp = viewModel::openApp,
            showAppMenu = viewModel::showAppMenu,
            requestHomeRole = onRequestHomeRole,
            retryCatalog = viewModel::retryCatalog,
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
            val routeKey = RouteKey(state.rice, state.screen)

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
                when (key.screen) {
                    LauncherScreen.Home -> rice.Home(
                        model = HomeModel(
                            favorites = state.favoriteSlots,
                            isDefaultHome = state.isDefaultHome,
                            recentApps = state.recentApps,
                        ),
                        actions = actions,
                    )
                    LauncherScreen.Drawer -> rice.Drawer(
                        model = DrawerModel(
                            query = state.query,
                            results = state.results,
                            favoriteKeys = state.favoriteKeys.toSet(),
                            status = state.catalogStatus,
                            recentApps = state.recentApps,
                        ),
                        actions = actions,
                    )
                    LauncherScreen.RicePicker -> RicePicker(current = state.rice, onSelect = viewModel::selectRice)
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
