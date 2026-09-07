package dev.cesarmanzocode.ricemobile.rice.arctic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
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

/** Arctic Drawer (contract §18.3): a nearly full-bleed rounded glass panel with a grid inside —
 * 4 columns nominal, reduced to 3 under width/fontScale pressure — and a search capsule anchored
 * inside the panel's own bottom, never a separate Material field. */
@Composable
fun ArcticDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(ARCTIC_BACKGROUND).safeDrawingPadding().padding(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow(8.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(ARCTIC_GLASS)
                .border(1.dp, ARCTIC_BORDER, RoundedCornerShape(28.dp))
                .padding(16.dp),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (model.status) {
                    CatalogStatus.Loading -> EmptyState(message = stringResource(R.string.catalog_loading), textColor = ARCTIC_SECONDARY)
                    is CatalogStatus.Failed -> EmptyState(
                        message = stringResource(R.string.catalog_error),
                        textColor = ARCTIC_SECONDARY,
                        accentColor = ARCTIC_ACCENT,
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = actions.retryCatalog,
                    )
                    CatalogStatus.Ready -> {
                        if (model.results.isEmpty()) {
                            EmptyState(message = stringResource(R.string.catalog_empty), textColor = ARCTIC_SECONDARY)
                        } else {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val fontScale = LocalDensity.current.fontScale
                                val nominal = (maxWidth / 76.dp).toInt().coerceAtLeast(1)
                                val columns = if (fontScale > 1.3f) (nominal - 1).coerceAtLeast(2) else nominal.coerceIn(3, 4)
                                LazyVerticalGrid(columns = GridCells.Fixed(columns)) {
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
            }
            ArcticSearchField(
                query = model.query,
                onQueryChange = actions.updateQuery,
                resultCount = model.results.size,
                onSearchSingleResult = { model.results.singleOrNull()?.let { actions.openApp(it.key) } },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun GridCell(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Column(
        modifier = Modifier
            .heightIn(min = 84.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = toggleLabel)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(entry = entry, size = 44.dp, plateShape = CircleShape, plateColor = Color(0x33FFFFFF))
        Text(
            text = entry.label,
            color = ARCTIC_INK,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (isFavorite) Text(text = "·", color = ARCTIC_ACCENT, fontSize = 13.sp)
    }
}

@Composable
private fun ArcticSearchField(
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
        textStyle = TextStyle(color = ARCTIC_INK, fontSize = 15.sp),
        cursorBrush = SolidColor(ARCTIC_INK),
        keyboardOptions = SearchImeOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x33FFFFFF))
            .border(1.dp, ARCTIC_BORDER, RoundedCornerShape(50))
            .padding(horizontal = 18.dp),
        decorationBox = { inner ->
            // No fillMaxSize here: inside a Column this box is measured before the weighted
            // catalog Box, so a height-filling modifier would claim the *whole* remaining
            // Column height (not just this field's share) and starve the grid below it.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(text = stringResource(R.string.search_hint), color = ARCTIC_SECONDARY, fontSize = 15.sp)
                }
                inner()
            }
        },
    )
}
