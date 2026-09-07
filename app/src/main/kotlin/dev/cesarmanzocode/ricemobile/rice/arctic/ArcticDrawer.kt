package dev.cesarmanzocode.ricemobile.rice.arctic

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.SearchField

private val ARCTIC_BACKGROUND = Color(0xFFD8E6EE)
private val ARCTIC_INK = Color(0xFF1E2A30)
private val ARCTIC_PANEL = Color(0xB3FFFFFF)

/** Arctic Glass Drawer structure: a grid of apps inside a floating panel (contract prompt). */
@Composable
fun ArcticDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(ARCTIC_BACKGROUND).safeDrawingPadding().padding(16.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ARCTIC_PANEL)
                .padding(12.dp),
        ) {
            when (model.status) {
                CatalogStatus.Loading -> EmptyState(message = stringResource(R.string.catalog_loading))
                is CatalogStatus.Failed -> EmptyState(
                    message = stringResource(R.string.catalog_error),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = actions.retryCatalog,
                )
                CatalogStatus.Ready -> {
                    if (model.results.isEmpty()) {
                        EmptyState(message = stringResource(R.string.catalog_empty))
                    } else {
                        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 84.dp)) {
                            items(model.results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                GridCell(
                                    entry = entry,
                                    isFavorite = entry.key in model.favoriteKeys,
                                    onClick = { actions.openApp(entry.key) },
                                    onLongClick = { actions.showAppMenu(entry.key) },
                                )
                            }
                        }
                    }
                }
            }
        }
        SearchField(
            query = model.query,
            onQueryChange = actions.updateQuery,
            resultCount = model.results.size,
            onSearchSingleResult = { model.results.singleOrNull()?.let { actions.openApp(it.key) } },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

@Composable
private fun GridCell(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(entry = entry, size = 44.dp)
        Text(text = (if (isFavorite) "★ " else "") + entry.label, color = ARCTIC_INK)
    }
}
