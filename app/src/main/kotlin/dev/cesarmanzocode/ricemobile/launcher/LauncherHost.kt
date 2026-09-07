package dev.cesarmanzocode.ricemobile.launcher

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.IconLoader
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RicePicker
import dev.cesarmanzocode.ricemobile.rice.RiceRegistry
import dev.cesarmanzocode.ricemobile.ui.shared.AppActionsMenu
import dev.cesarmanzocode.ricemobile.ui.shared.LocalIconLoader

private val PREFERENCES_LOADING_BACKGROUND = Color(0xFF0A0A0A)

/**
 * Owns transitions between routes, the global menu/message overlay, the picker, and window
 * concerns (contract §6.2) — never the design of a rice's Home/Drawer. Renders only the
 * current rice, one instance at a time.
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

    BackHandler(enabled = LauncherNavigation.menuBackHandlerEnabled(state.appMenu)) {
        viewModel.dismissAppMenu()
    }
    BackHandler(enabled = LauncherNavigation.imeBackHandlerEnabled(state.screen, imeVisible)) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    BackHandler(enabled = LauncherNavigation.pickerBackHandlerEnabled(state.screen)) {
        viewModel.goHome()
    }
    BackHandler(enabled = LauncherNavigation.drawerBackHandlerEnabled(state.screen, imeVisible)) {
        viewModel.goHome()
    }
    BackHandler(enabled = LauncherNavigation.homeNoOpBackHandlerEnabled(state.screen, state.isDefaultHome)) {
        // No-op: a default Home never exits to a previous Home.
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

    CompositionLocalProvider(LocalIconLoader provides iconLoader) {
        val rice = RiceRegistry.of(state.rice)
        when (state.screen) {
            LauncherScreen.Home -> rice.Home(
                model = HomeModel(favorites = viewModel.favoriteSlots(state), isDefaultHome = state.isDefaultHome),
                actions = actions,
                modifier = modifier,
            )
            LauncherScreen.Drawer -> rice.Drawer(
                model = DrawerModel(
                    query = state.query,
                    results = state.results,
                    favoriteKeys = state.favoriteKeys.toSet(),
                    status = state.catalogStatus,
                ),
                actions = actions,
                modifier = modifier,
            )
            LauncherScreen.RicePicker -> RicePicker(
                current = state.rice,
                onSelect = viewModel::selectRice,
                modifier = modifier,
            )
        }

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
