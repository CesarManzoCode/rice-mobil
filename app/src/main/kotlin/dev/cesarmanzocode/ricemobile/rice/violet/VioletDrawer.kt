package dev.cesarmanzocode.ricemobile.rice.violet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperBackdrop

private val VIOLET_SHEET = Color(0xFF241A3D)

/** Violet Drawer (approved mockup, Sprint 3 second pass): the same real bottom sheet leaving the
 * wallpaper visible above it, but the landing view is now chips + Recientes + a Categorías grid
 * before an explicit "Todas las apps" — not a flat 3-column grid of every app. */
@Composable
fun VioletDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf<DrawerView>(DrawerView.Browse) }
    val searching = model.query.isNotEmpty()
    BackHandler(enabled = !searching && view != DrawerView.Browse) { view = DrawerView.Browse }

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
                Text(
                    text = if (!searching && view != DrawerView.Browse) {
                        (view as? DrawerView.Category)?.let { stringResource(it.category.labelRes) } ?: stringResource(R.string.drawer_section_all_apps)
                    } else {
                        stringResource(R.string.drawer_title)
                    },
                    color = VIOLET_INK,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = {
                        if (!searching && view != DrawerView.Browse) view = DrawerView.Browse else actions.goHome()
                    }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = if (!searching && view != DrawerView.Browse) "←" else "✕", color = VIOLET_SECONDARY, fontSize = 16.sp)
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
                        } else if (searching) {
                            SheetGrid(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
                        } else {
                            Crossfade(targetState = view, animationSpec = tween(150), label = "violet-drawer-view") { target ->
                                when (target) {
                                    DrawerView.Browse -> BrowseView(
                                        model = model,
                                        onOpenCategory = { view = DrawerView.Category(it) },
                                        onOpenAllApps = { view = DrawerView.AllApps },
                                        actions = actions,
                                    )
                                    is DrawerView.Category -> {
                                        val group = DrawerHierarchy.categorize(model.results).firstOrNull { it.category == target.category }
                                        SheetGrid(entries = group?.apps.orEmpty(), favoriteKeys = model.favoriteKeys, actions = actions)
                                    }
                                    DrawerView.AllApps -> SheetGrid(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
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
private fun BrowseView(
    model: DrawerModel,
    onOpenCategory: (AppCategory) -> Unit,
    onOpenAllApps: () -> Unit,
    actions: RiceActions,
) {
    val categories = remember(model.results) { DrawerHierarchy.categorize(model.results) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "chips") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                item(key = "chip-all") { Chip(text = stringResource(R.string.drawer_section_all_apps), onClick = onOpenAllApps) }
                columnItems(categories.take(4), key = { "chip:${it.category}" }) { group ->
                    Chip(text = stringResource(group.category.labelRes), onClick = { onOpenCategory(group.category) })
                }
            }
        }
        if (model.recentApps.isNotEmpty()) {
            item(key = "recent-header") { SectionLabel(stringResource(R.string.drawer_section_recent)) }
            item(key = "recent-row") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    for (entry in model.recentApps.take(4)) {
                        RecentBubble(entry = entry, actions = actions)
                    }
                }
            }
        }
        item(key = "categories-header") { SectionLabel(stringResource(R.string.drawer_section_categories)) }
        item(key = "categories-grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                for (row in categories.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (group in row) {
                            CategoryPanel(group = group, onClick = { onOpenCategory(group.category) }, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item(key = "all-apps") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x22FFFFFF))
                    .clickable(onClick = onOpenAllApps)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.drawer_section_all_apps), color = VIOLET_INK, fontSize = 15.sp)
                Text(text = "${model.results.size} →", color = VIOLET_SECONDARY, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Chip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x22FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = VIOLET_SECONDARY, fontSize = 13.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text.uppercase(), color = VIOLET_SECONDARY, fontSize = 11.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 10.dp))
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
        AppIcon(entry = entry, size = 48.dp, plateShape = CircleShape, plateColor = Color(0x33000000))
        Text(
            text = entry.label,
            color = VIOLET_INK,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun CategoryPanel(group: CategoryGroup, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x22FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(text = stringResource(group.category.labelRes), color = VIOLET_INK, fontSize = 14.sp)
        Text(
            text = pluralStringResource(R.plurals.category_app_count, group.apps.size, group.apps.size),
            color = VIOLET_SECONDARY,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SheetGrid(entries: List<AppEntry>, favoriteKeys: Set<AppKey>, actions: RiceActions) {
    if (entries.isEmpty()) {
        EmptyState(message = stringResource(R.string.catalog_empty), textColor = VIOLET_SECONDARY)
        return
    }
    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
        items(entries, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
            SheetCell(
                entry = entry,
                isFavorite = entry.key in favoriteKeys,
                onClick = { actions.openApp(entry.key) },
                onLongClick = { actions.showAppMenu(entry.key) },
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
            // No fillMaxSize: this field is measured before the weighted catalog Box in the
            // sheet's Column, so a height-filling box here would claim the whole remaining sheet
            // height instead of its own row, leaving the grid with no space.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(text = stringResource(R.string.search_hint), color = VIOLET_SECONDARY, fontSize = 15.sp)
                }
                inner()
            }
        },
    )
}
