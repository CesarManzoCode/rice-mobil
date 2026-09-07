package dev.cesarmanzocode.ricemobile.rice.monochrome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppCategory
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.apps.IconTreatment
import dev.cesarmanzocode.ricemobile.rice.CategoryGroup
import dev.cesarmanzocode.ricemobile.rice.DrawerHierarchy
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.DrawerView
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.SearchImeOptions
import dev.cesarmanzocode.ricemobile.ui.shared.rememberSearchKeyboardActions
import kotlinx.coroutines.launch

/**
 * Monochrome Drawer (approved mockup, Sprint 3 second pass): a plain "APPS" header with a live
 * result count, a bottom search field styled as a thick underline, and — instead of one flat list
 * of every app — a browse landing of Recientes + Categorías with an explicit "Todas las apps"
 * escape hatch that shows an A-Z index with a tappable rail, matching the mockup's structure.
 * Searching (query non-empty) always shows flat results immediately, skipping this hierarchy.
 */
@Composable
fun MonochromeDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf<DrawerView>(DrawerView.Browse) }
    val searching = model.query.isNotEmpty()
    BackHandler(enabled = !searching && view != DrawerView.Browse) { view = DrawerView.Browse }

    Column(modifier = modifier.fillMaxSize().background(MONOCHROME_BACKGROUND).safeDrawingPadding()) {
        DrawerHeader(model = model, view = view, searching = searching, onBack = { view = DrawerView.Browse })
        HorizontalDivider(color = MONOCHROME_BORDER)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (model.status) {
                CatalogStatus.Loading -> EmptyState(message = stringResource(R.string.catalog_loading), textColor = MONOCHROME_SECONDARY)
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
                    } else if (searching) {
                        FlatList(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
                    } else {
                        Crossfade(targetState = view, animationSpec = tween(150), label = "monochrome-drawer-view") { target ->
                            when (target) {
                                DrawerView.Browse -> BrowseView(
                                    model = model,
                                    onOpenCategory = { view = DrawerView.Category(it) },
                                    onOpenAllApps = { view = DrawerView.AllApps },
                                    actions = actions,
                                )
                                is DrawerView.Category -> {
                                    val group = DrawerHierarchy.categorize(model.results).firstOrNull { it.category == target.category }
                                    FlatList(entries = group?.apps.orEmpty(), favoriteKeys = model.favoriteKeys, actions = actions)
                                }
                                DrawerView.AllApps -> AllAppsView(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
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
private fun DrawerHeader(model: DrawerModel, view: DrawerView, searching: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (!searching && view != DrawerView.Browse) {
                Text(
                    text = "←",
                    color = MONOCHROME_INK,
                    fontSize = 26.sp,
                    modifier = Modifier.padding(end = 12.dp).clickable(onClick = onBack),
                )
            }
            Text(
                text = when {
                    searching || view == DrawerView.Browse -> stringResource(R.string.drawer_title)
                    view is DrawerView.Category -> stringResource(view.category.labelRes).uppercase()
                    else -> stringResource(R.string.drawer_section_all_apps).uppercase()
                },
                color = MONOCHROME_INK,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
            )
        }
        if (model.status == CatalogStatus.Ready) {
            Text(
                text = pluralStringResource(R.plurals.catalog_result_count, model.results.size, model.results.size),
                color = MONOCHROME_SECONDARY,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
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
        if (model.recentApps.isNotEmpty()) {
            item(key = "recent-header") {
                SectionHeader(stringResource(R.string.drawer_section_recent))
            }
            items(model.recentApps.take(5), key = { "recent:${it.key.userSerial}:${it.key.component}" }) { entry ->
                AppRow(entry = entry, isFavorite = entry.key in model.favoriteKeys, compact = true, actions = actions)
            }
            item(key = "recent-rule") { HorizontalDivider(color = MONOCHROME_BORDER, modifier = Modifier.padding(vertical = 8.dp)) }
        }
        item(key = "categories-header") { SectionHeader(stringResource(R.string.drawer_section_categories)) }
        items(categories, key = { "category:${it.category}" }) { group ->
            CategoryRow(group = group, onClick = { onOpenCategory(group.category) })
        }
        item(key = "all-apps-rule") { HorizontalDivider(color = MONOCHROME_BORDER, modifier = Modifier.padding(vertical = 8.dp)) }
        item(key = "all-apps") { AllAppsRow(total = model.results.size, onClick = onOpenAllApps) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = MONOCHROME_SECONDARY,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
private fun CategoryRow(group: CategoryGroup, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(group.category.labelRes), color = MONOCHROME_INK, fontSize = 17.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.category_app_count, group.apps.size, group.apps.size),
                color = MONOCHROME_SECONDARY,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text(text = "→", color = MONOCHROME_SECONDARY, fontSize = 15.sp)
        }
    }
    HorizontalDivider(color = MONOCHROME_BORDER.copy(alpha = 0.4f))
}

@Composable
private fun AllAppsRow(total: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(R.string.drawer_section_all_apps), color = MONOCHROME_INK, fontSize = 17.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)
        Text(
            text = "$total →",
            color = MONOCHROME_SECONDARY,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun FlatList(entries: List<AppEntry>, favoriteKeys: Set<AppKey>, actions: RiceActions) {
    if (entries.isEmpty()) {
        EmptyState(message = stringResource(R.string.catalog_empty), textColor = MONOCHROME_SECONDARY)
        return
    }
    LazyColumn {
        items(entries, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
            AppRow(entry = entry, isFavorite = entry.key in favoriteKeys, compact = false, actions = actions)
            HorizontalDivider(color = MONOCHROME_BORDER.copy(alpha = 0.4f))
        }
    }
}

/** A-Z index with a tappable letter rail (contract §18.2 "rail A–Z a la derecha"): the rail
 * scrolls the list to that letter's first item rather than being purely decorative. */
@Composable
private fun AllAppsView(entries: List<AppEntry>, favoriteKeys: Set<AppKey>, actions: RiceActions) {
    val groups = remember(entries) { DrawerHierarchy.groupByInitial(entries) }
    val listState = rememberLazyListStateFor(groups)
    val scope = rememberCoroutineScope()
    // Header + rows: precompute each letter's flat item index once per grouping.
    val letterIndex = remember(groups) {
        val map = LinkedHashMap<String, Int>()
        var index = 0
        for ((letter, apps) in groups) {
            map[letter] = index
            index += 1 + apps.size
        }
        map
    }

    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxHeight()) {
            for ((letter, apps) in groups) {
                item(key = "header-$letter") {
                    Text(
                        text = letter,
                        color = MONOCHROME_SECONDARY,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 4.dp),
                    )
                }
                items(apps, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                    AppRow(entry = entry, isFavorite = entry.key in favoriteKeys, compact = false, actions = actions)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .verticalScroll(rememberScrollState())
                .padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (letter in groups.map { it.first }) {
                Text(
                    text = letter,
                    color = MONOCHROME_SECONDARY,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(vertical = 2.dp, horizontal = 6.dp)
                        .clickable {
                            letterIndex[letter]?.let { index -> scope.launch { listState.animateScrollToItem(index) } }
                        },
                )
            }
        }
    }
}

@Composable
private fun rememberLazyListStateFor(groups: List<Pair<String, List<AppEntry>>>): LazyListState {
    // Keyed on the grouping identity so switching category/search context resets scroll.
    return remember(groups.map { it.first }) { LazyListState() }
}

@Composable
private fun AppRow(entry: AppEntry, isFavorite: Boolean, compact: Boolean, actions: RiceActions) {
    val interactionSource = remember { MutableInteractionSource() }
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 52.dp else 60.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { actions.openApp(entry.key) },
                onLongClick = { actions.showAppMenu(entry.key) },
                onLongClickLabel = toggleLabel,
            )
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            entry = entry,
            size = if (compact) 30.dp else 36.dp,
            treatment = IconTreatment.Monochrome,
            tint = MONOCHROME_INK,
            plateShape = RectangleShape,
            plateColor = Color.Transparent,
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
            cursorBrush = SolidColor(MONOCHROME_INK),
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
