package dev.cesarmanzocode.ricemobile.rice.ember

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import dev.cesarmanzocode.ricemobile.apps.AppCategory
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.rice.CategoryGroup
import dev.cesarmanzocode.ricemobile.rice.DrawerHierarchy
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.DrawerView
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.ScreenRect
import dev.cesarmanzocode.ricemobile.ui.shared.SearchImeOptions
import dev.cesarmanzocode.ricemobile.ui.shared.appCellPressable
import dev.cesarmanzocode.ricemobile.ui.shared.rememberSearchKeyboardActions

/**
 * Ember Drawer (approved mockup, Sprint 3 second pass): título + count stay, but the landing view
 * is now an industrial dashboard — Acceso rápido (recent apps as tiles), Recientes (a denser
 * recent list), and Categorías as two-column panels with icon/name/count — before "Todas las
 * apps" as a secondary, explicit destination. The original two-column compact rows are reused
 * for both a category's apps and the full "Todas" escape hatch.
 */
@Composable
fun EmberDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf<DrawerView>(DrawerView.Browse) }
    val searching = model.query.isNotEmpty()
    BackHandler(enabled = !searching && view != DrawerView.Browse) { view = DrawerView.Browse }

    Column(modifier = modifier.fillMaxSize().background(EMBER_CARBON).safeDrawingPadding()) {
        DrawerHeader(model = model, view = view, searching = searching, onBack = { view = DrawerView.Browse })
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
                    } else if (searching) {
                        CompactGrid(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
                    } else {
                        Crossfade(targetState = view, animationSpec = tween(150), label = "ember-drawer-view") { target ->
                            when (target) {
                                DrawerView.Browse -> BrowseView(
                                    model = model,
                                    onOpenCategory = { view = DrawerView.Category(it) },
                                    onOpenAllApps = { view = DrawerView.AllApps },
                                    actions = actions,
                                )
                                is DrawerView.Category -> {
                                    val group = DrawerHierarchy.categorize(model.results).firstOrNull { it.category == target.category }
                                    CompactGrid(entries = group?.apps.orEmpty(), favoriteKeys = model.favoriteKeys, actions = actions)
                                }
                                DrawerView.AllApps -> CompactGrid(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
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
private fun DrawerHeader(model: DrawerModel, view: DrawerView, searching: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            if (!searching && view != DrawerView.Browse) {
                Text(
                    text = "←",
                    color = EMBER_COPPER,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 10.dp).clickable(onClick = onBack),
                )
            }
            Text(
                text = when {
                    searching || view == DrawerView.Browse -> stringResource(R.string.drawer_title)
                    view is DrawerView.Category -> stringResource(view.category.labelRes).uppercase()
                    else -> stringResource(R.string.drawer_section_all_apps).uppercase()
                },
                color = EMBER_COPPER,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
            )
        }
        if (model.status == CatalogStatus.Ready) {
            Text(
                text = pluralStringResource(R.plurals.catalog_result_count, model.results.size, model.results.size),
                color = EMBER_SECONDARY,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
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
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        if (model.recentApps.isNotEmpty()) {
            item(key = "quick-header") { PanelLabel(stringResource(R.string.ember_quick_access)) }
            item(key = "quick-row") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    for (entry in model.recentApps.take(4)) {
                        QuickTile(entry = entry, isFavorite = entry.key in model.favoriteKeys, actions = actions)
                    }
                }
            }
        }
        item(key = "categories-header") { PanelLabel(stringResource(R.string.drawer_section_categories)) }
        item(key = "categories-grid") { CategoryPanels(groups = categories, onClick = onOpenCategory) }
        item(key = "all-apps") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(EMBER_CUT)
                    .background(EMBER_SURFACE)
                    .border(1.dp, EMBER_COPPER.copy(alpha = 0.5f), EMBER_CUT)
                    .clickable(onClick = onOpenAllApps)
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.drawer_section_all_apps).uppercase(), color = EMBER_INK, fontFamily = FontFamily.Monospace, fontSize = 13.sp, letterSpacing = 1.sp)
                Text(text = "${model.results.size} →", color = EMBER_COPPER, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun PanelLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = EMBER_COPPER,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 10.dp),
    )
}

@Composable
private fun QuickTile(entry: AppEntry, isFavorite: Boolean, actions: RiceActions) {
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Column(
        modifier = Modifier.appCellPressable(
            pressScale = RiceMotion.Ember.pressScale,
            pressMs = RiceMotion.Ember.pressMs,
            pressSpec = RiceMotion.Ember.pressSpec,
            onClick = { actions.openApp(entry.key) },
            onLongClickAt = { rect -> actions.showAppMenu(entry.key, rect) },
            onLongClickLabel = toggleLabel,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(entry = entry, size = 40.dp, plateShape = EMBER_CUT, plateColor = EMBER_SURFACE)
        Text(
            text = entry.label,
            color = EMBER_INK,
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp).width(60.dp),
        )
    }
}

@Composable
private fun CategoryPanels(groups: List<CategoryGroup>, onClick: (AppCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in groups.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (group in row) {
                    CategoryPanel(group = group, onClick = { onClick(group.category) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryPanel(group: CategoryGroup, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clip(EMBER_CUT)
            .background(EMBER_SURFACE)
            .border(1.dp, EMBER_COPPER.copy(alpha = 0.5f), EMBER_CUT)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = stringResource(group.category.labelRes), color = EMBER_INK, fontFamily = FontFamily.SansSerif, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            text = pluralStringResource(R.plurals.category_app_count, group.apps.size, group.apps.size),
            color = EMBER_COPPER,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun CompactGrid(entries: List<AppEntry>, favoriteKeys: Set<AppKey>, actions: RiceActions) {
    if (entries.isEmpty()) {
        EmptyState(message = stringResource(R.string.catalog_empty), textColor = EMBER_SECONDARY)
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fontScale = LocalDensity.current.fontScale
        val columns = if (maxWidth < 340.dp || fontScale > 1.3f) 1 else 2
        LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.padding(horizontal = 8.dp)) {
            items(entries, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                CompactCell(
                    entry = entry,
                    isFavorite = entry.key in favoriteKeys,
                    onClick = { actions.openApp(entry.key) },
                    onLongClickAt = { rect -> actions.showAppMenu(entry.key, rect) },
                )
            }
        }
    }
}

@Composable
private fun CompactCell(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClickAt: (ScreenRect) -> Unit) {
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .appCellPressable(
                pressScale = RiceMotion.Ember.pressScale,
                pressMs = RiceMotion.Ember.pressMs,
                pressSpec = RiceMotion.Ember.pressSpec,
                onClick = onClick,
                onLongClickAt = onLongClickAt,
                onLongClickLabel = toggleLabel,
            )
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
