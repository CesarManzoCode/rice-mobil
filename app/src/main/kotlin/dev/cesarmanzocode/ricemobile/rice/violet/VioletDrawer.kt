package dev.cesarmanzocode.ricemobile.rice.violet

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperBackdrop

private val VIOLET_SHEET = Color(0xFF241A3D)

/** Violet Drawer (contract §18.6): a real bottom sheet anchored at 88% of the usable height, a
 * non-modal scrim over the wallpaper strip it leaves visible above, and a 3-column grid inside.
 * Lives inside the normal route (part of `LauncherScreen.Drawer`), never a `ModalBottomSheet`
 * with its own back stack. */
@Composable
fun VioletDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        WallpaperBackdrop(spec = VioletNightRice.wallpaper, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(Color(0x40000000)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(VIOLET_SHEET.copy(alpha = 0.94f))
                .safeDrawingPadding()
                .padding(20.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.drawer_title), color = VIOLET_INK, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = actions.goHome),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "✕", color = VIOLET_SECONDARY, fontSize = 16.sp)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (model.status) {
                    CatalogStatus.Loading -> EmptyState(message = stringResource(R.string.catalog_loading), textColor = VIOLET_SECONDARY)
                    is CatalogStatus.Failed -> EmptyState(
                        message = stringResource(R.string.catalog_error),
                        textColor = VIOLET_SECONDARY,
                        accentColor = VIOLET_ACCENT,
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = actions.retryCatalog,
                    )
                    CatalogStatus.Ready -> {
                        if (model.results.isEmpty()) {
                            EmptyState(message = stringResource(R.string.catalog_empty), textColor = VIOLET_SECONDARY)
                        } else {
                            LazyVerticalGrid(columns = GridCells.Fixed(3)) {
                                items(model.results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                    SheetCell(
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

            VioletSearchField(
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
private fun SheetCell(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Column(
        modifier = Modifier
            .heightIn(min = 92.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = toggleLabel)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(entry = entry, size = 50.dp, plateShape = CircleShape, plateColor = Color(0x33000000))
        Text(
            text = entry.label,
            color = VIOLET_INK,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (isFavorite) Text(text = "·", color = VIOLET_ACCENT, fontSize = 13.sp)
    }
}

@Composable
private fun VioletSearchField(
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
        textStyle = TextStyle(color = VIOLET_INK, fontSize = 15.sp),
        cursorBrush = SolidColor(VIOLET_INK),
        keyboardOptions = SearchImeOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x40FFFFFF))
            .padding(horizontal = 18.dp),
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(text = stringResource(R.string.search_hint), color = VIOLET_SECONDARY, fontSize = 15.sp)
                }
                inner()
            }
        },
    )
}
