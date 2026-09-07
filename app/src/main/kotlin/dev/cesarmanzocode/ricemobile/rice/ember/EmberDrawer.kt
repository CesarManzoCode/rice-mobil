package dev.cesarmanzocode.ricemobile.rice.ember

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.SearchImeOptions
import dev.cesarmanzocode.ricemobile.ui.shared.rememberSearchKeyboardActions

/** Ember Drawer (contract §18.4): two dense columns of compact horizontal rows (icon beside the
 * label, never above it). Collapses to one column under 340dp width or a large fontScale, so
 * neither the touch target nor the text ever shrinks to keep two columns (contract §18.1). */
@Composable
fun EmberDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(EMBER_CARBON).safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.drawer_title), color = EMBER_COPPER, fontFamily = FontFamily.Monospace, fontSize = 13.sp, letterSpacing = 1.sp)
            if (model.status == CatalogStatus.Ready) {
                Text(
                    text = pluralStringResource(R.plurals.catalog_result_count, model.results.size, model.results.size),
                    color = EMBER_SECONDARY,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (model.status) {
                CatalogStatus.Loading -> EmptyState(message = stringResource(R.string.catalog_loading), textColor = EMBER_SECONDARY)
                is CatalogStatus.Failed -> EmptyState(
                    message = stringResource(R.string.catalog_error),
                    textColor = EMBER_SECONDARY,
                    accentColor = EMBER_COPPER,
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = actions.retryCatalog,
                )
                CatalogStatus.Ready -> {
                    if (model.results.isEmpty()) {
                        EmptyState(message = stringResource(R.string.catalog_empty), textColor = EMBER_SECONDARY)
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val fontScale = LocalDensity.current.fontScale
                            val columns = if (maxWidth < 340.dp || fontScale > 1.3f) 1 else 2
                            LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.padding(horizontal = 8.dp)) {
                                items(model.results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                    CompactCell(
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
        }
        EmberSearchField(
            query = model.query,
            onQueryChange = actions.updateQuery,
            resultCount = model.results.size,
            onSearchSingleResult = { model.results.singleOrNull()?.let { actions.openApp(it.key) } },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun CompactCell(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = toggleLabel)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(entry = entry, size = 32.dp, plateShape = EMBER_CUT, plateColor = EMBER_SURFACE)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = entry.label,
            color = EMBER_INK,
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isFavorite) {
            Text(text = "●", color = EMBER_COPPER, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EmberSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    onSearchSingleResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardActions = rememberSearchKeyboardActions(resultCount, onSearchSingleResult)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = EMBER_INK, fontSize = 15.sp, fontFamily = FontFamily.Monospace),
        cursorBrush = SolidColor(EMBER_INK),
        keyboardOptions = SearchImeOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .heightIn(min = 52.dp)
            .background(EMBER_SURFACE, RectangleShape)
            .border(1.dp, EMBER_COPPER.copy(alpha = 0.6f), RectangleShape)
            .padding(horizontal = 16.dp),
        decorationBox = { inner ->
            // No fillMaxSize: measured before the weighted catalog Box in this Column, a
            // height-filling box here would claim the whole remaining Column height instead of
            // just this field's own row (contract bug: catalog left with no space).
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(text = stringResource(R.string.search_hint), color = EMBER_SECONDARY, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                }
                inner()
            }
        },
    )
}
