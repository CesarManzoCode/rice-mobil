package dev.cesarmanzocode.ricemobile.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
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

    // Back precedence (contract §7): IME first, then Drawer -> Home, then Home no-op only
    // while this app actually holds the Home role.
    BackHandler(enabled = state.screen == LauncherScreen.Drawer && imeVisible) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    BackHandler(enabled = state.screen == LauncherScreen.Drawer && !imeVisible) {
        viewModel.goHome()
    }
    BackHandler(enabled = state.screen == LauncherScreen.Home && state.isDefaultHome) {
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
