package dev.cesarmanzocode.ricemobile.rice.monochrome

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.apps.IconTreatment
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.SearchImeOptions
import dev.cesarmanzocode.ricemobile.ui.shared.rememberSearchKeyboardActions

/** Monochrome Drawer (contract §18.2): one single list with rule lines between rows, a plain
 * "APPS" header with a live result count, and a bottom search field styled as a thick underline
 * instead of a bordered Material field. */
@Composable
fun MonochromeDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(MONOCHROME_BACKGROUND).safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.drawer_title),
                color = MONOCHROME_INK,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
            )
            if (model.status == CatalogStatus.Ready) {
                Text(
                    text = pluralStringResource(R.plurals.catalog_result_count, model.results.size, model.results.size),
                    color = MONOCHROME_SECONDARY,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
            }
        }
        HorizontalDivider(color = MONOCHROME_BORDER)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (model.status) {
                CatalogStatus.Loading -> EmptyState(
                    message = stringResource(R.string.catalog_loading),
                    textColor = MONOCHROME_SECONDARY,
                )
                is CatalogStatus.Failed -> EmptyState(
                    message = stringResource(R.string.catalog_error),
                    textColor = MONOCHROME_SECONDARY,
                    accentColor = MONOCHROME_INK,
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = actions.retryCatalog,
                )
                CatalogStatus.Ready -> {
                    if (model.results.isEmpty()) {
                        EmptyState(message = stringResource(R.string.catalog_empty), textColor = MONOCHROME_SECONDARY)
                    } else {
                        LazyColumn {
                            items(model.results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                AppRow(
                                    entry = entry,
                                    isFavorite = entry.key in model.favoriteKeys,
                                    onClick = { actions.openApp(entry.key) },
                                    onLongClick = { actions.showAppMenu(entry.key) },
                                )
                                HorizontalDivider(color = MONOCHROME_BORDER.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }

        MonochromeSearchField(
            query = model.query,
            onQueryChange = actions.updateQuery,
            resultCount = model.results.size,
            onSearchSingleResult = { model.results.singleOrNull()?.let { actions.openApp(it.key) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun AppRow(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = toggleLabel,
            )
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            entry = entry,
            size = 36.dp,
            treatment = IconTreatment.Monochrome,
            tint = MONOCHROME_INK,
            plateShape = androidx.compose.ui.graphics.RectangleShape,
            plateColor = androidx.compose.ui.graphics.Color.Transparent,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = entry.label, color = MONOCHROME_INK, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (isFavorite) {
            Box(modifier = Modifier.width(10.dp).heightIn(min = 10.dp).background(MONOCHROME_INK))
        }
    }
}

@Composable
private fun MonochromeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    onSearchSingleResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardActions = rememberSearchKeyboardActions(resultCount, onSearchSingleResult)
    Column(modifier = modifier) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = MONOCHROME_INK, fontSize = 17.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MONOCHROME_INK),
            keyboardOptions = SearchImeOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(text = stringResource(R.string.search_hint), color = MONOCHROME_SECONDARY, fontSize = 17.sp)
                    }
                    inner()
                }
            },
        )
        HorizontalDivider(color = MONOCHROME_INK, thickness = 2.dp, modifier = Modifier.padding(top = 8.dp))
    }
}
