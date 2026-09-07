package dev.cesarmanzocode.ricemobile.rice.ember

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val EMBER_BACKGROUND = Color(0xFF241512)
private val EMBER_INK = Color(0xFFF2E4D8)

/** Ember Forge Drawer structure: two columns of compact rows (contract prompt). */
@Composable
fun EmberDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(EMBER_BACKGROUND).safeDrawingPadding()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(8.dp)) {
                            items(model.results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                CompactRow(
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun CompactRow(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(entry = entry, size = 28.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = (if (isFavorite) "★ " else "") + entry.label, color = EMBER_INK, maxLines = 1)
    }
}
