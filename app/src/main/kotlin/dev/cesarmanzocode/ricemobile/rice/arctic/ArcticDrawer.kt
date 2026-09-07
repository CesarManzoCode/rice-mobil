package dev.cesarmanzocode.ricemobile.rice.arctic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppCategory
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.rice.CategoryGroup
import dev.cesarmanzocode.ricemobile.rice.DrawerHierarchy
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.DrawerView
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.SearchImeOptions
import dev.cesarmanzocode.ricemobile.ui.shared.rememberSearchKeyboardActions

/**
 * Arctic Drawer (approved mockup, Sprint 3 second pass): search now lives at the *top* of the
 * glass panel (matching the mockup, unlike every other rice's bottom search), followed by a
 * horizontal chip row, Recientes, a Categorías tile grid, and a structured "Todas las apps"
 * destination — instead of a bare 4-column grid of every app from the first frame.
 */
@Composable
fun ArcticDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf<DrawerView>(DrawerView.Browse) }
    val searching = model.query.isNotEmpty()
    BackHandler(enabled = !searching && view != DrawerView.Browse) { view = DrawerView.Browse }

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
            ArcticSearchField(
                query = model.query,
                onQueryChange = actions.updateQuery,
                resultCount = model.results.size,
                onSearchSingleResult = { model.results.singleOrNull()?.let { actions.openApp(it.key) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.padding(top = 12.dp))
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
                        } else if (searching) {
                            AppGrid(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
                        } else {
                            when (val current = view) {
                                DrawerView.Browse -> BrowseView(
                                    model = model,
                                    onOpenCategory = { view = DrawerView.Category(it) },
                                    onOpenAllApps = { view = DrawerView.AllApps },
                                    actions = actions,
                                )
                                is DrawerView.Category -> {
                                    val group = DrawerHierarchy.categorize(model.results).firstOrNull { it.category == current.category }
                                    AppGrid(entries = group?.apps.orEmpty(), favoriteKeys = model.favoriteKeys, actions = actions)
                                }
                                DrawerView.AllApps -> AppGrid(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseView(
    model: DrawerModel,
    onOpenCategory: (AppCategory) -> Unit,
    onOpenAllApps: () -> Unit,
    actions: RiceActions,
) {
    val categories = remember(model.results) { DrawerHierarchy.categorize(model.results) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "chips") {
            ChipRow(categories = categories, onSelectCategory = onOpenCategory, onSelectAll = onOpenAllApps)
        }
        if (model.recentApps.isNotEmpty()) {
            item(key = "recent-header") { SectionLabel(stringResource(R.string.drawer_section_recent)) }
            item(key = "recent-row") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    for (entry in model.recentApps.take(5)) {
                        RecentBubble(entry = entry, actions = actions)
                    }
                }
            }
        }
        item(key = "categories-header") { SectionLabel(stringResource(R.string.drawer_section_categories)) }
        item(key = "categories-grid") { CategoryTiles(groups = categories, onClick = onOpenCategory) }
        item(key = "all-apps") { AllAppsRow(total = model.results.size, onClick = onOpenAllApps) }
    }
}

@Composable
private fun ChipRow(categories: List<CategoryGroup>, onSelectCategory: (AppCategory) -> Unit, onSelectAll: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
        item(key = "chip-all") { Chip(text = stringResource(R.string.drawer_section_all_apps), selected = true, onClick = onSelectAll) }
        columnItems(categories.take(4), key = { "chip:${it.category}" }) { group ->
            Chip(text = stringResource(group.category.labelRes), selected = false, onClick = { onSelectCategory(group.category) })
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) ARCTIC_ACCENT.copy(alpha = 0.28f) else Color(0x22FFFFFF))
            .border(1.dp, if (selected) ARCTIC_ACCENT else ARCTIC_BORDER, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = if (selected) ARCTIC_INK else ARCTIC_SECONDARY, fontSize = 13.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), color = ARCTIC_SECONDARY, fontSize = 11.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun RecentBubble(entry: AppEntry, actions: RiceActions) {
    Column(
        modifier = Modifier.combinedClickable(
            onClick = { actions.openApp(entry.key) },
            onLongClick = { actions.showAppMenu(entry.key) },
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(entry = entry, size = 44.dp, plateShape = CircleShape, plateColor = Color(0x33FFFFFF))
        Text(
            text = entry.label,
            color = ARCTIC_INK,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun CategoryTiles(groups: List<CategoryGroup>, onClick: (AppCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 16.dp)) {
        for (row in groups.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (group in row) {
                    CategoryTile(group = group, onClick = { onClick(group.category) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryTile(group: CategoryGroup, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x22FFFFFF))
            .border(1.dp, ARCTIC_BORDER, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(text = stringResource(group.category.labelRes), color = ARCTIC_INK, fontSize = 14.sp)
        Text(
            text = pluralStringResource(R.plurals.category_app_count, group.apps.size, group.apps.size),
            color = ARCTIC_SECONDARY,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AllAppsRow(total: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x22FFFFFF))
            .border(1.dp, ARCTIC_BORDER, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.drawer_section_all_apps), color = ARCTIC_INK, fontSize = 15.sp)
        Text(text = "$total →", color = ARCTIC_SECONDARY, fontSize = 13.sp)
    }
}

@Composable
private fun AppGrid(entries: List<AppEntry>, favoriteKeys: Set<AppKey>, actions: RiceActions) {
    if (entries.isEmpty()) {
        EmptyState(message = stringResource(R.string.catalog_empty), textColor = ARCTIC_SECONDARY)
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fontScale = LocalDensity.current.fontScale
        val nominal = (maxWidth / 76.dp).toInt().coerceAtLeast(1)
        val columns = if (fontScale > 1.3f) (nominal - 1).coerceAtLeast(2) else nominal.coerceIn(3, 4)
        LazyVerticalGrid(columns = GridCells.Fixed(columns)) {
            items(entries, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                GridCell(
                    entry = entry,
                    isFavorite = entry.key in favoriteKeys,
                    onClick = { actions.openApp(entry.key) },
                    onLongClick = { actions.showAppMenu(entry.key) },
                )
            }
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
            // No fillMaxSize: a height-filling box here would claim the whole remaining Column
            // height (it's measured before the weighted catalog Box below it) instead of its own
            // row, starving the grid — the contract bug this pass fixed.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(text = stringResource(R.string.search_hint), color = ARCTIC_SECONDARY, fontSize = 15.sp)
                }
                inner()
            }
        },
    )
}
