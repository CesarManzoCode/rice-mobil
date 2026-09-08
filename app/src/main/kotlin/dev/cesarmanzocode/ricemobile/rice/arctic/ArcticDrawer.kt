package dev.cesarmanzocode.ricemobile.rice.arctic

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.EmptyState
import dev.cesarmanzocode.ricemobile.ui.shared.LocalReducedMotion
import dev.cesarmanzocode.ricemobile.ui.shared.MotionTokens
import dev.cesarmanzocode.ricemobile.ui.shared.ScreenRect
import dev.cesarmanzocode.ricemobile.ui.shared.SearchImeOptions
import dev.cesarmanzocode.ricemobile.ui.shared.appCellPressable
import dev.cesarmanzocode.ricemobile.ui.shared.ricePressable
import dev.cesarmanzocode.ricemobile.ui.shared.rememberSearchKeyboardActions
import kotlinx.coroutines.launch

/** What the drawer's content area is currently showing, unified so the *same* [AnimatedContent]
 * (interaction sprint §10, §11, §13) directs Browse<->Category, Browse<->AllApps *and*
 * Browse<->SearchResults — never the "Crossfade used as a hammer" the sprint calls out. */
private sealed interface DrawerContentKey {
    data object Search : DrawerContentKey
    data class Hierarchy(val view: DrawerView) : DrawerContentKey
}

private fun DrawerView.depth(): Int = if (this is DrawerView.Browse) 0 else 1

/**
 * Arctic Drawer V2 (Sprint 3, mockup reconstruction): edge-to-edge (no floating rounded card —
 * the mockup's drawer is a flat full-bleed panel, unlike Home's floating modules), search on top,
 * a fully-scrolling chip row, Recientes, a *separate* Favoritos row (the mockup's "Frecuentes"
 * without UsageStats), a denser 2-column Categorías grid with real glyphs, and a "Todas las apps"
 * preview card that shows real apps instead of ending on a bare button. The full A-Z destination
 * is a structured, letter-headered list with a tappable rail — not a repeat of the search grid.
 */
@Composable
fun ArcticDrawer(model: DrawerModel, actions: RiceActions, modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf<DrawerView>(DrawerView.Browse) }
    val searching = model.query.isNotEmpty()
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    BackHandler(enabled = !searching && view != DrawerView.Browse) { view = DrawerView.Browse }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ARCTIC_BACKGROUND)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        ArcticSearchField(
            query = model.query,
            onQueryChange = actions.updateQuery,
            resultCount = model.results.size,
            onSearchSingleResult = { model.results.singleOrNull()?.let { actions.openApp(it.key) } },
            modifier = Modifier.fillMaxWidth(),
        )
        val pushed = !searching && view != DrawerView.Browse
        if (pushed) {
            DrawerSubHeader(
                title = when (val v = view) {
                    is DrawerView.Category -> stringResource(v.category.labelRes)
                    else -> stringResource(R.string.drawer_section_all_apps)
                },
                onBack = { view = DrawerView.Browse },
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
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
                        val contentKey: DrawerContentKey = if (searching) DrawerContentKey.Search else DrawerContentKey.Hierarchy(view)
                        AnimatedContent(
                            targetState = contentKey,
                            transitionSpec = {
                                val searchChanged = (initialState is DrawerContentKey.Search) != (targetState is DrawerContentKey.Search)
                                when {
                                    reducedMotion -> fadeIn(tween(MotionTokens.Fast)) togetherWith fadeOut(tween(MotionTokens.Fast))
                                    searchChanged -> {
                                        // §13: only Browse<->SearchResults gets motion — typing more
                                        // letters keeps the same key (Search), so results update in
                                        // place with zero animation per keystroke.
                                        val enteringSearch = targetState is DrawerContentKey.Search
                                        val distancePx = with(density) { 6.dp.roundToPx() }
                                        val enter = fadeIn(tween(MotionTokens.Fast)) +
                                            slideInVertically(tween(MotionTokens.Fast)) { if (enteringSearch) distancePx else -distancePx }
                                        val exit = fadeOut(tween(MotionTokens.Fast)) +
                                            slideOutVertically(tween(MotionTokens.Fast)) { if (enteringSearch) -distancePx else distancePx }
                                        enter togetherWith exit
                                    }
                                    else -> {
                                        // §10/§11: Browse<->Category and Browse<->AllApps both "profundizar"
                                        // via the same directional slide — the container moves, never the
                                        // individual cells (§10: "no animar cada item individual").
                                        val initialDepth = (initialState as? DrawerContentKey.Hierarchy)?.view?.depth() ?: 0
                                        val targetDepth = (targetState as? DrawerContentKey.Hierarchy)?.view?.depth() ?: 0
                                        val forward = targetDepth > initialDepth
                                        val enterPx = with(density) { 24.dp.roundToPx() }
                                        val exitPx = with(density) { 16.dp.roundToPx() }
                                        val enter = fadeIn(tween(MotionTokens.Standard)) +
                                            slideInHorizontally(tween(MotionTokens.Standard)) { if (forward) enterPx else -exitPx }
                                        val exit = fadeOut(tween(MotionTokens.Standard)) +
                                            slideOutHorizontally(tween(MotionTokens.Standard)) { if (forward) -exitPx else enterPx }
                                        enter togetherWith exit
                                    }
                                }
                            },
                            label = "arctic-drawer-content",
                        ) { key ->
                            when (key) {
                                DrawerContentKey.Search -> AppGrid(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
                                is DrawerContentKey.Hierarchy -> when (val target = key.view) {
                                    DrawerView.Browse -> BrowseView(
                                        model = model,
                                        onOpenCategory = { view = DrawerView.Category(it) },
                                        onOpenAllApps = { view = DrawerView.AllApps },
                                        actions = actions,
                                    )
                                    is DrawerView.Category -> {
                                        val group = DrawerHierarchy.categorize(model.results).firstOrNull { it.category == target.category }
                                        AppGrid(entries = group?.apps.orEmpty(), favoriteKeys = model.favoriteKeys, actions = actions)
                                    }
                                    DrawerView.AllApps -> AllAppsView(entries = model.results, favoriteKeys = model.favoriteKeys, actions = actions)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSubHeader(title: String, onBack: () -> Unit) {
    val backLabel = stringResource(R.string.drawer_back)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                .clickable(onClick = onBack)
                .semantics { contentDescription = backLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "←", color = ARCTIC_ACCENT, fontSize = 17.sp)
        }
        Text(text = title.uppercase(), color = ARCTIC_INK, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
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
    val favoriteApps = remember(model.results, model.favoriteKeys) { model.results.filter { it.key in model.favoriteKeys } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "chips") {
            ChipRow(categories = categories, onSelectCategory = onOpenCategory, onSelectAll = onOpenAllApps)
        }
        if (model.recentApps.isNotEmpty()) {
            item(key = "recent") {
                AppRowSection(title = stringResource(R.string.drawer_section_recent), apps = model.recentApps, actions = actions)
            }
        }
        if (favoriteApps.isNotEmpty()) {
            item(key = "favorites") {
                AppRowSection(title = stringResource(R.string.drawer_section_favorites), apps = favoriteApps, actions = actions)
            }
        }
        item(key = "categories-header") { SectionHeader(stringResource(R.string.drawer_section_categories)) }
        item(key = "categories-grid") { CategoryGrid(groups = categories, onClick = onOpenCategory) }
        item(key = "all-apps-preview") {
            AllAppsPreview(entries = model.results, total = model.results.size, onOpen = onOpenAllApps)
        }
        item(key = "bottom-space") { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ChipRow(categories: List<CategoryGroup>, onSelectCategory: (AppCategory) -> Unit, onSelectAll: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 18.dp)) {
        item(key = "chip-all") { Chip(text = stringResource(R.string.drawer_section_all_apps), selected = true, onClick = onSelectAll) }
        listItems(categories, key = { "chip:${it.category}" }) { group ->
            Chip(text = stringResource(group.category.labelRes), selected = false, onClick = { onSelectCategory(group.category) })
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    // elevated = false: a LazyRow can have several chips visible/composed at once, and a per-chip
    // drop shadow adds no perceptible depth at this size — the gradient fill + border already
    // reads as glass (contract perf §11/§12: cheaper equivalent instead of removing the material).
    ArcticGlassSurface(
        modifier = Modifier.heightIn(min = 40.dp),
        shape = RoundedCornerShape(50),
        tint = if (selected) ARCTIC_ACCENT else ARCTIC_GLASS_TINT,
        baseAlpha = if (selected) 0.34f else 0.24f,
        elevated = false,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) ARCTIC_INK else ARCTIC_SECONDARY,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = ARCTIC_INK,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

/** Shared by Recientes and Favoritos: a horizontally-scrolling row (never a fixed Row that could
 * overflow at a narrow width or a large fontScale) of up to 5 real apps. */
@Composable
private fun AppRowSection(title: String, apps: List<AppEntry>, actions: RiceActions) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        SectionHeader(title)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for (entry in apps.take(5)) {
                AppRowTile(entry = entry, actions = actions)
            }
        }
    }
}

@Composable
private fun AppRowTile(entry: AppEntry, actions: RiceActions) {
    Column(
        modifier = Modifier.appCellPressable(
            pressScale = RiceMotion.Arctic.pressScale,
            pressMs = RiceMotion.Arctic.pressMs,
            pressSpec = RiceMotion.Arctic.pressSpec,
            onClick = { actions.openApp(entry.key) },
            onLongClickAt = { rect -> actions.showAppMenu(entry.key, rect) },
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(entry = entry, size = 46.dp, plateShape = CircleShape, plateColor = ARCTIC_ICON_PLATE)
        Text(
            text = entry.label,
            color = ARCTIC_INK,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp).width(58.dp),
        )
    }
}

@Composable
private fun CategoryGrid(groups: List<CategoryGroup>, onClick: (AppCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 20.dp)) {
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
    // elevated = false: up to 8 of these render at once in the categories grid — see Chip above.
    ArcticGlassSurface(
        modifier = modifier.heightIn(min = 76.dp, max = 88.dp),
        shape = RoundedCornerShape(20.dp),
        baseAlpha = 0.24f,
        elevated = false,
        onClick = onClick,
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(ARCTIC_ICON_PLATE),
                contentAlignment = Alignment.Center,
            ) {
                CategoryGlyph(category = group.category, tint = ARCTIC_ACCENT, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(group.category.labelRes),
                    color = ARCTIC_INK,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(R.plurals.category_app_count, group.apps.size, group.apps.size),
                    color = ARCTIC_SECONDARY,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(text = "→", color = ARCTIC_SECONDARY, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AllAppsPreview(entries: List<AppEntry>, total: Int, onOpen: () -> Unit) {
    ArcticGlassSurface(modifier = Modifier.fillMaxWidth(), baseAlpha = 0.24f, onClick = onOpen) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.drawer_section_all_apps).uppercase(),
                    color = ARCTIC_INK,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = pluralStringResource(R.plurals.catalog_result_count, total, total), color = ARCTIC_SECONDARY, fontSize = 12.sp)
                    Text(text = "A–Z", color = ARCTIC_ACCENT, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (entry in entries.take(4)) {
                        AppIcon(entry = entry, size = 38.dp, plateShape = CircleShape, plateColor = ARCTIC_ICON_PLATE)
                    }
                }
            }
        }
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
                    onLongClickAt = { rect -> actions.showAppMenu(entry.key, rect) },
                )
            }
        }
    }
}

@Composable
private fun GridCell(entry: AppEntry, isFavorite: Boolean, onClick: () -> Unit, onLongClickAt: (ScreenRect) -> Unit) {
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Column(
        modifier = Modifier
            .heightIn(min = 84.dp)
            .appCellPressable(
                pressScale = RiceMotion.Arctic.pressScale,
                pressMs = RiceMotion.Arctic.pressMs,
                pressSpec = RiceMotion.Arctic.pressSpec,
                onClick = onClick,
                onLongClickAt = onLongClickAt,
                onLongClickLabel = toggleLabel,
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(entry = entry, size = 44.dp, plateShape = CircleShape, plateColor = ARCTIC_ICON_PLATE)
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

/**
 * The full "Todas las apps" destination: a structured, letter-headered list (never a repeat of
 * the flat search grid) with a tappable A-Z rail that scrolls to that letter — an optional
 * addition the task allows, and useful once the list is a few dozen apps long.
 */
@Composable
private fun AllAppsView(entries: List<AppEntry>, favoriteKeys: Set<AppKey>, actions: RiceActions) {
    if (entries.isEmpty()) {
        EmptyState(message = stringResource(R.string.catalog_empty), textColor = ARCTIC_SECONDARY)
        return
    }
    val groups = remember(entries) { DrawerHierarchy.groupByInitial(entries) }
    val listState = remember(groups.map { it.first }) { LazyListState() }
    val scope = rememberCoroutineScope()
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
                        color = ARCTIC_ACCENT,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                }
                listItems(apps, key = { "${it.key.userSerial}:${it.key.component}" }) { entry ->
                    AllAppsRow(entry = entry, isFavorite = entry.key in favoriteKeys, actions = actions)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for ((letter, _) in groups) {
                Text(
                    text = letter,
                    color = ARCTIC_SECONDARY,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(vertical = 3.dp, horizontal = 6.dp)
                        .clickable {
                            letterIndex[letter]?.let { index -> scope.launch { listState.animateScrollToItem(index) } }
                        },
                )
            }
        }
    }
}

@Composable
private fun AllAppsRow(entry: AppEntry, isFavorite: Boolean, actions: RiceActions) {
    val toggleLabel = stringResource(if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .appCellPressable(
                pressScale = RiceMotion.Arctic.pressScale,
                pressMs = RiceMotion.Arctic.pressMs,
                pressSpec = RiceMotion.Arctic.pressSpec,
                onClick = { actions.openApp(entry.key) },
                onLongClickAt = { rect -> actions.showAppMenu(entry.key, rect) },
                onLongClickLabel = toggleLabel,
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(entry = entry, size = 36.dp, plateShape = CircleShape, plateColor = ARCTIC_ICON_PLATE)
        Text(
            text = entry.label,
            color = ARCTIC_INK,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        if (isFavorite) Text(text = "·", color = ARCTIC_ACCENT, fontSize = 16.sp)
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
    val reducedMotion = LocalReducedMotion.current
    var focused by remember { mutableStateOf(false) }
    // §12: "field gana énfasis" on focus — a short (120-180ms) tint/border brighten, never a
    // hard cut and never a slow decorative one.
    val focusProgress by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(160),
        label = "search-focus",
    )
    val glowTint = lerp(ARCTIC_GLASS_TINT, ARCTIC_ACCENT, focusProgress * 0.5f)
    // No `remember` here: focusProgress changes every animation frame during the ~160ms tween, so
    // it would never actually hit, cache or not — a plain per-recomposition Brush is the honest cost.
    val glowBorder = Brush.verticalGradient(
        listOf(
            lerp(ARCTIC_BORDER_HI, ARCTIC_ACCENT, focusProgress),
            lerp(ARCTIC_BORDER, ARCTIC_ACCENT.copy(alpha = 0.55f), focusProgress),
        ),
    )
    ArcticGlassSurface(
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(50),
        tint = glowTint,
        baseAlpha = 0.26f + focusProgress * 0.08f,
        borderBrush = glowBorder,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchGlyph(tint = lerp(ARCTIC_SECONDARY, ARCTIC_ACCENT, focusProgress), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = ARCTIC_INK, fontSize = 15.sp),
                    cursorBrush = SolidColor(ARCTIC_INK),
                    keyboardOptions = SearchImeOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                    // fillMaxWidth, never fillMaxSize/fillMaxHeight: this field is a *non-weighted*
                    // first child of the outer Column, measured before the weighted catalog Box
                    // below it — a height-filling decoration here reproduces the exact bug fixed
                    // earlier this sprint (an unweighted sibling claiming the Column's full height).
                    decorationBox = { inner ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(text = stringResource(R.string.search_hint), color = ARCTIC_SECONDARY, fontSize = 15.sp)
                            }
                            inner()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.16f
        val radius = w * 0.30f
        val center = Offset(w * 0.42f, h * 0.42f)
        drawCircle(color = tint, radius = radius, center = center, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        val handleStart = Offset(center.x + radius * 0.72f, center.y + radius * 0.72f)
        val handleEnd = Offset(w * 0.88f, h * 0.88f)
        drawLine(color = tint, start = handleStart, end = handleEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

/** Simple line-art glyphs per category — never a plain grey circle (contract §"ICONOS"). Drawn,
 * not a new drawable asset: the project has no icon-font/vector library dependency to add one. */
@Composable
private fun CategoryGlyph(category: AppCategory, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.10f
        val outline = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (category) {
            AppCategory.Communication -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.16f),
                    size = Size(w * 0.76f, h * 0.54f),
                    cornerRadius = CornerRadius(w * 0.16f),
                    style = outline,
                )
                val tail = Path().apply {
                    moveTo(w * 0.30f, h * 0.68f)
                    lineTo(w * 0.24f, h * 0.88f)
                    lineTo(w * 0.48f, h * 0.70f)
                    close()
                }
                drawPath(tail, color = tint)
            }
            AppCategory.Productivity -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.20f, h * 0.14f),
                    size = Size(w * 0.60f, h * 0.72f),
                    cornerRadius = CornerRadius(w * 0.10f),
                    style = outline,
                )
                val check = Path().apply {
                    moveTo(w * 0.34f, h * 0.52f)
                    lineTo(w * 0.46f, h * 0.64f)
                    lineTo(w * 0.68f, h * 0.36f)
                }
                drawPath(check, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            AppCategory.Multimedia -> {
                drawCircle(color = tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = outline)
                val play = Path().apply {
                    moveTo(w * 0.42f, h * 0.34f)
                    lineTo(w * 0.42f, h * 0.66f)
                    lineTo(w * 0.68f, h * 0.5f)
                    close()
                }
                drawPath(play, color = tint)
            }
            AppCategory.News -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.16f),
                    size = Size(w * 0.68f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.10f),
                    style = outline,
                )
                val lineY = floatArrayOf(0.36f, 0.5f, 0.64f)
                val lineW = floatArrayOf(0.44f, 0.44f, 0.28f)
                for (i in lineY.indices) {
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.30f, h * lineY[i]),
                        end = Offset(w * (0.30f + lineW[i]), h * lineY[i]),
                        strokeWidth = w * 0.055f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            AppCategory.MapsTravel -> {
                val drop = Path().apply {
                    moveTo(w * 0.5f, h * 0.86f)
                    lineTo(w * 0.32f, h * 0.50f)
                    lineTo(w * 0.68f, h * 0.50f)
                    close()
                }
                drawPath(drop, color = tint)
                drawCircle(color = tint, radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.36f), style = outline)
            }
            AppCategory.Games -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.34f),
                    size = Size(w * 0.76f, h * 0.36f),
                    cornerRadius = CornerRadius(w * 0.18f),
                    style = outline,
                )
                drawCircle(color = tint, radius = w * 0.045f, center = Offset(w * 0.68f, h * 0.46f))
                drawCircle(color = tint, radius = w * 0.045f, center = Offset(w * 0.78f, h * 0.56f))
                drawLine(color = tint, start = Offset(w * 0.24f, h * 0.52f), end = Offset(w * 0.24f, h * 0.40f), strokeWidth = w * 0.055f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.18f, h * 0.46f), end = Offset(w * 0.30f, h * 0.46f), strokeWidth = w * 0.055f, cap = StrokeCap.Round)
            }
            AppCategory.Tools -> {
                rotate(degrees = 45f) {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(w * 0.14f, h * 0.44f),
                        size = Size(w * 0.72f, h * 0.12f),
                        cornerRadius = CornerRadius(w * 0.06f),
                    )
                }
                drawCircle(color = tint, radius = w * 0.13f, center = Offset(w * 0.24f, h * 0.24f), style = outline)
                drawCircle(color = tint, radius = w * 0.13f, center = Offset(w * 0.76f, h * 0.76f), style = outline)
            }
            AppCategory.Other -> {
                val positions = listOf(
                    Offset(w * 0.30f, h * 0.30f), Offset(w * 0.70f, h * 0.30f),
                    Offset(w * 0.30f, h * 0.70f), Offset(w * 0.70f, h * 0.70f),
                )
                for (p in positions) drawCircle(color = tint, radius = w * 0.09f, center = p)
            }
        }
    }
}
