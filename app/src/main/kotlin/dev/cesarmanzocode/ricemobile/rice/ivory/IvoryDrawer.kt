package dev.cesarmanzocode.ricemobile.rice.ivory

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.SearchField

private val IVORY_BACKGROUND = Color(0xFFF3ECDF)
private val IVORY_INK = Color(0xFF241E14)
private val IVORY_RULE = Color(0xFFBDB09A)

/** Ivory Paper Drawer structure: an editorial index list, no icons/grid (contract prompt). */
@Composable
fun IvoryDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(IVORY_BACKGROUND).safeDrawingPadding()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
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
                        LazyColumn {
                            items(model.results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                IndexRow(
                                    entry = entry,
                                    isFavorite = entry.key in model.favoriteKeys,
                                    onClick = { actions.openApp(entry.key) },
                                    onLongClick = { actions.showAppMenu(entry.key) },
                                )
                                HorizontalDivider(color = IVORY_RULE)
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
private fun IndexRow(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = entry.label, color = IVORY_INK)
        Spacer(modifier = Modifier.width(8.dp))
        if (isFavorite) Text(text = "★", color = IVORY_INK)
    }
}
