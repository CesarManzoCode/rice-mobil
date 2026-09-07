package dev.cesarmanzocode.ricemobile.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cesarmanzocode.ricemobile.apps.IconLoader
import dev.cesarmanzocode.ricemobile.rice.monochrome.MonochromeDrawer
import dev.cesarmanzocode.ricemobile.rice.monochrome.MonochromeHome

/**
 * S1 host: only Monochrome exists, so it renders directly (contract §14). S2 adds the rice
 * registry lookup and picker route in this same file (contract §6.3).
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

    BackHandler(enabled = LauncherNavigation.imeBackHandlerEnabled(state.screen, imeVisible)) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    BackHandler(enabled = LauncherNavigation.drawerBackHandlerEnabled(state.screen, imeVisible)) {
        viewModel.goHome()
    }
    BackHandler(enabled = LauncherNavigation.homeNoOpBackHandlerEnabled(state.screen, state.isDefaultHome)) {
        // No-op: a default Home never exits to a previous Home.
    }

    when (state.screen) {
        LauncherScreen.Home -> MonochromeHome(
            isDefaultHome = state.isDefaultHome,
            onOpenDrawer = viewModel::openDrawer,
            onRequestHomeRole = onRequestHomeRole,
            modifier = modifier,
        )
        LauncherScreen.Drawer -> MonochromeDrawer(
            query = state.query,
            results = state.results,
            catalogStatus = state.catalogStatus,
            iconLoader = iconLoader,
            onQueryChange = viewModel::updateQuery,
            onOpenApp = viewModel::openApp,
            onRetry = viewModel::retryCatalog,
            modifier = modifier,
        )
    }
}
