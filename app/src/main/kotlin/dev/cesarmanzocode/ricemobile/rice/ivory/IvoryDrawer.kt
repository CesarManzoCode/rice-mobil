package dev.cesarmanzocode.ricemobile.rice.ivory

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.apps.CatalogStatus
import dev.cesarmanzocode.ricemobile.apps.IconTreatment
import dev.cesarmanzocode.ricemobile.rice.DrawerHierarchy
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.DrawerView
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.SearchImeOptions
import dev.cesarmanzocode.ricemobile.ui.shared.rememberSearchKeyboardActions

/**
 * Ivory Drawer (approved mockup, Sprint 3 second pass): the same editorial page language, but the
 * landing view is now Recientes + Categorías instead of jumping straight into a 71-app index.
 * "Todas las apps" keeps the original alphabetical index with margin letters — that structure was
 * already right, it just should not be the *first* thing shown.
 */
@Composable
fun IvoryDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf<DrawerView>(DrawerView.Browse) }
    val searching = model.query.isNotEmpty()
    BackHandler(enabled = !searching && view != DrawerView.Browse) { view = DrawerView.Browse }

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
                    } else if (searching) {
                        LazyColumn {
                            items(model.results, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                IndexRow(entry = entry, isFavorite = entry.key in model.favoriteKeys, indent = false, actions = actions)
                            }
                        }
                    } else {
                        Crossfade(targetState = view, animationSpec = tween(150), label = "ivory-drawer-view") { target ->
                            when (target) {
                                DrawerView.Browse -> BrowseView(
                                    model = model,
                                    onOpenCategory = { view = DrawerView.Category(it) },
                                    onOpenAllApps = { view = DrawerView.AllApps },
                                    actions = actions,
                                )
                                is DrawerView.Category -> {
                                    val group = DrawerHierarchy.categorize(model.results).firstOrNull { it.category == target.category }
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        CategoryHeader(title = stringResource(target.category.labelRes), onBack = { view = DrawerView.Browse })
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(group?.apps.orEmpty(), key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                                                IndexRow(entry = entry, isFavorite = entry.key in model.favoriteKeys, indent = false, actions = actions)
                                            }
                                        }
                                    }
                                }
                                DrawerView.AllApps -> AllAppsIndex(entries = model.results, favoriteKeys = model.favoriteKeys, onBack = { view = DrawerView.Browse }, actions = actions)
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
private fun CategoryHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "←",
            color = IVORY_INK,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 12.dp).clickable(onClick = onBack),
        )
        Text(text = title, color = IVORY_INK, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
private fun BrowseView(
    model: DrawerModel,
    onOpenCategory: (dev.cesarmanzocode.ricemobile.apps.AppCategory) -> Unit,
    onOpenAllApps: () -> Unit,
    actions: RiceActions,
) {
    val categories = remember(model.results) { DrawerHierarchy.categorize(model.results) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "drawer-title") {
            Text(text = stringResource(R.string.drawer_title), color = IVORY_INK, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, modifier = Modifier.padding(bottom = 16.dp))
        }
        if (model.recentApps.isNotEmpty()) {
            item(key = "recent-header") { SectionLabel(stringResource(R.string.drawer_section_recent)) }
            items(model.recentApps.take(4), key = { "recent:${it.key.userSerial}:${it.key.component}" }) { entry ->
                IndexRow(entry = entry, isFavorite = entry.key in model.favoriteKeys, indent = false, actions = actions)
            }
            item(key = "recent-rule") { HorizontalDivider(color = IVORY_RULE, modifier = Modifier.padding(vertical = 10.dp)) }
        }
        item(key = "categories-header") { SectionLabel(stringResource(R.string.drawer_section_categories)) }
        items(categories, key = { "category:${it.category}" }) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable { onOpenCategory(group.category) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(group.category.labelRes), color = IVORY_INK, fontFamily = FontFamily.Serif, fontSize = 18.sp)
                Text(
                    text = pluralStringResource(R.plurals.category_app_count, group.apps.size, group.apps.size),
                    color = IVORY_SECONDARY,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                )
            }
            HorizontalDivider(color = IVORY_RULE.copy(alpha = 0.5f))
        }
        item(key = "all-apps") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onOpenAllApps)
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(R.string.drawer_section_all_apps)} ↗",
                    color = IVORY_INK,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(text = "${model.results.size}", color = IVORY_SECONDARY, fontFamily = FontFamily.SansSerif, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = IVORY_SECONDARY,
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun AllAppsIndex(entries: List<AppEntry>, favoriteKeys: Set<AppKey>, onBack: () -> Unit, actions: RiceActions) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryHeader(title = stringResource(R.string.drawer_section_all_apps), onBack = onBack)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        val groups = remember(entries) { DrawerHierarchy.groupByInitial(entries) }
        LazyColumn(modifier = Modifier.weight(1f)) {
            for ((letter, apps) in groups) {
                item(key = "header-$letter") { GroupHeader(letter) }
                items(apps, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                    IndexRow(entry = entry, isFavorite = entry.key in favoriteKeys, indent = true, actions = actions)
                }
                item(key = "rule-$letter") { HorizontalDivider(color = IVORY_RULE, modifier = Modifier.padding(vertical = 6.dp)) }
            }
        }
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
