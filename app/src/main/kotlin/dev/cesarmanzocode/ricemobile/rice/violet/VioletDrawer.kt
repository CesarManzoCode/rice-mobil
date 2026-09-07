package dev.cesarmanzocode.ricemobile.rice.violet

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private val VIOLET_BACKDROP = Color(0xFF241A38)
private val VIOLET_SHEET = Color(0xFF352654)
private val VIOLET_INK = Color(0xFFEDE6F5)

/** Violet Night Drawer structure: a sheet rising from the bottom (contract prompt). */
@Composable
fun VioletDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(VIOLET_BACKDROP)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(VIOLET_SHEET)
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
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
                            LazyColumn {
                                items(model.results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                    SheetRow(
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SheetRow(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(entry = entry, size = 32.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = entry.label, color = VIOLET_INK)
        if (isFavorite) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "★", color = VIOLET_INK)
        }
    }
}
