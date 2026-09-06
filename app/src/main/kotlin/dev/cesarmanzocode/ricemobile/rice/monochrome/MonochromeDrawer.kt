package dev.cesarmanzocode.ricemobile.rice.monochrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.apps.IconLoader
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.SearchField

private val MONOCHROME_BACKGROUND = Color(0xFF0A0A0A)
private val MONOCHROME_INK = Color(0xFFF5F5F0)

/**
 * Provisional Monochrome Drawer (contract §14): a single list with a bottom search field.
 * Favorites/long-press menu land in S2 once RiceActions exists.
 */
@Composable
fun MonochromeDrawer(
    query: String,
    results: List<AppEntry>,
    catalogStatus: CatalogStatus,
    iconLoader: IconLoader,
    onQueryChange: (String) -> Unit,
    onOpenApp: (AppKey) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MONOCHROME_BACKGROUND).safeDrawingPadding()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (catalogStatus) {
                CatalogStatus.Loading -> EmptyState(message = stringResource(R.string.catalog_loading))
                is CatalogStatus.Failed -> EmptyState(
                    message = stringResource(R.string.catalog_error),
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = onRetry,
                )
                CatalogStatus.Ready -> {
                    if (results.isEmpty()) {
                        EmptyState(message = stringResource(R.string.catalog_empty))
                    } else {
                        LazyColumn {
                            items(results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                AppRow(entry = entry, iconLoader = iconLoader, onClick = { onOpenApp(entry.key) })
                            }
                        }
                    }
                }
            }
        }
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            resultCount = results.size,
            onSearchSingleResult = { results.singleOrNull()?.let { onOpenApp(it.key) } },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun AppRow(entry: AppEntry, iconLoader: IconLoader, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(entry = entry, iconLoader = iconLoader, size = 36.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = entry.label, color = MONOCHROME_INK)
    }
}
