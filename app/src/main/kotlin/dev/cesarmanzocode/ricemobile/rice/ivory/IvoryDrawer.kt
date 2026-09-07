package dev.cesarmanzocode.ricemobile.rice.ivory

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
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
import dev.cesarmanzocode.ricemobile.apps.IconTreatment
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.SearchImeOptions
import dev.cesarmanzocode.ricemobile.ui.shared.rememberSearchKeyboardActions

/** Ivory Drawer (contract §18.5): an alphabetically grouped editorial index, not a grid. A large
 * letter sits in the margin; rows are offset to start+32dp. Query results skip the headers and
 * read as a plain list; a rule only ever separates groups, never each row. */
@Composable
fun IvoryDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(IVORY_BACKGROUND).safeDrawingPadding()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp)) {
            when (model.status) {
                CatalogStatus.Loading -> EmptyState(message = stringResource(R.string.catalog_loading), textColor = IVORY_SECONDARY)
                is CatalogStatus.Failed -> EmptyState(
                    message = stringResource(R.string.catalog_error),
                    textColor = IVORY_SECONDARY,
                    accentColor = IVORY_INK,
                    actionLabel = stringResource(R.string.action_retry),
                    onAction = actions.retryCatalog,
                )
                CatalogStatus.Ready -> {
                    if (model.results.isEmpty()) {
                        EmptyState(message = stringResource(R.string.catalog_empty), textColor = IVORY_SECONDARY)
                    } else if (model.query.isNotEmpty()) {
                        LazyColumn {
                            items(model.results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                IndexRow(entry = entry, isFavorite = entry.key in model.favoriteKeys, indent = false, actions = actions)
                            }
                        }
                    } else {
                        val groups = remember(model.results) { groupByInitial(model.results) }
                        LazyColumn {
                            for ((letter, entries) in groups) {
                                item(key = "header-$letter") { GroupHeader(letter) }
                                items(entries, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                    IndexRow(entry = entry, isFavorite = entry.key in model.favoriteKeys, indent = true, actions = actions)
                                }
                                item(key = "rule-$letter") { HorizontalDivider(color = IVORY_RULE, modifier = Modifier.padding(vertical = 6.dp)) }
                            }
                        }
                    }
                }
            }
        }
        IvorySearchField(
            query = model.query,
            onQueryChange = actions.updateQuery,
            resultCount = model.results.size,
            onSearchSingleResult = { model.results.singleOrNull()?.let { actions.openApp(it.key) } },
            modifier = Modifier.fillMaxWidth().padding(28.dp),
        )
    }
}

@Composable
private fun GroupHeader(letter: String) {
    Text(
        text = letter,
        color = IVORY_INK,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun IndexRow(entry: AppEntry, isFavorite: Boolean, indent: Boolean, actions: RiceActions) {
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = if (indent) 32.dp else 0.dp)
            .combinedClickable(
                onClick = { actions.openApp(entry.key) },
                onLongClick = { actions.showAppMenu(entry.key) },
                onLongClickLabel = toggleLabel,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            entry = entry,
            size = 28.dp,
            treatment = IconTreatment.Monochrome,
            tint = IVORY_INK,
            plateShape = RectangleShape,
            plateColor = Color.Transparent,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = entry.label,
            color = IVORY_INK,
            fontFamily = FontFamily.Serif,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isFavorite) {
            Text(text = "·", color = IVORY_SECONDARY, fontFamily = FontFamily.Serif, fontSize = 22.sp)
        }
    }
}

/** Consecutive same-initial runs (results are already Collator-sorted upstream, contract §5.1),
 * grouped stably; names without a leading letter fall into "#" (contract §18.5). */
private fun groupByInitial(results: List<AppEntry>): List<Pair<String, List<AppEntry>>> {
    val groups = LinkedHashMap<String, MutableList<AppEntry>>()
    for (entry in results) {
        val first = entry.label.trim().firstOrNull()
        val key = if (first != null && first.isLetter()) first.uppercaseChar().toString() else "#"
        groups.getOrPut(key) { mutableListOf() }.add(entry)
    }
    return groups.map { it.key to it.value }
}

@Composable
private fun IvorySearchField(
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
            textStyle = TextStyle(color = IVORY_INK, fontSize = 17.sp, fontFamily = FontFamily.Serif),
            cursorBrush = SolidColor(IVORY_INK),
            keyboardOptions = SearchImeOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(text = stringResource(R.string.search_hint), color = IVORY_SECONDARY, fontFamily = FontFamily.Serif, fontSize = 17.sp)
                    }
                    inner()
                }
            },
        )
        HorizontalDivider(color = IVORY_RULE, thickness = 1.dp, modifier = Modifier.padding(top = 6.dp))
    }
}
