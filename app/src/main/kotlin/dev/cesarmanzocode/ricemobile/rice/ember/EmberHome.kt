package dev.cesarmanzocode.ricemobile.rice.ember

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.IconTreatment
import dev.cesarmanzocode.ricemobile.launcher.ClockProvider
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.system.rememberBatterySnapshot
import dev.cesarmanzocode.ricemobile.system.rememberNextAlarmSnapshot
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface
import dev.cesarmanzocode.ricemobile.ui.shared.ScreenRect
import dev.cesarmanzocode.ricemobile.ui.shared.appCellPressable
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockDate
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockTime
import dev.cesarmanzocode.ricemobile.ui.shared.formatEpochTime
import dev.cesarmanzocode.ricemobile.ui.shared.rememberCurrentLocale
import dev.cesarmanzocode.ricemobile.ui.shared.rememberIs24HourFormat
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperBackdrop
import java.time.format.FormatStyle

internal val EMBER_CARBON = Color(0xFF171411)
internal val EMBER_SURFACE = Color(0xFF24201B)
internal val EMBER_INK = Color(0xFFF1E8DC)
internal val EMBER_COPPER = Color(0xFFD99A67)
internal val EMBER_SECONDARY = Color(0xFFC0ABA0)
internal val EMBER_CUT = CutCornerShape(6.dp)

/**
 * Ember Home (approved mockup, Sprint 3 second pass): a compressed header, a dashboard of two
 * small real-data blocks (battery + next alarm — the mockup's card pair), a wide "acceso rápido"
 * block built from local recent-app history (replacing the mockup's music card, never fabricated
 * media), the favorites matrix, and a clear "todas las apps" trigger. Denser and more dashboard-
 * shaped than the previous pass, matching the mockup's industrial-panel feel.
 */
@Composable
fun EmberHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        // UX overhaul §1: see MonochromeHome's identical wiring for why onSwipeUp is now a no-op.
        onSwipeUp = {},
        onLongPress = actions.openPicker,
        onDragStart = actions.beginDrawerDrag,
        onDrag = actions.dragDrawer,
        onDragEnd = actions.endDrawerDrag,
        modifier = modifier.fillMaxSize(),
    ) {
        WallpaperBackdrop(spec = EmberForgeRice.wallpaper, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
            HeaderRow(isDefaultHome = model.isDefaultHome, onRequestHome = actions.requestHomeRole)
            Spacer(modifier = Modifier.height(20.dp))
            GlanceRow()
            Spacer(modifier = Modifier.height(10.dp))
            QuickAccessBlock(recentApps = model.recentApps, actions = actions)
            Spacer(modifier = Modifier.weight(1f))
            FavoritesMatrix(favorites = model.favorites, actions = actions)
            Spacer(modifier = Modifier.height(16.dp))
            TriggerBar(actions = actions)
        }
    }
}

@Composable
private fun HeaderRow(isDefaultHome: Boolean, onRequestHome: () -> Unit) {
    val now by ClockProvider.rememberNow()
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatClockTime(now, is24Hour, locale),
                color = EMBER_INK,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Box(modifier = Modifier.width(3.dp).height(34.dp).background(EMBER_COPPER))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = formatClockDate(now, locale, FormatStyle.SHORT),
                    color = EMBER_SECONDARY,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                if (!isDefaultHome) {
                    Text(
                        text = stringResource(R.string.action_use_as_home),
                        color = EMBER_COPPER,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.defaultMinSize(minHeight = 24.dp).clickable(onClick = onRequestHome),
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.ember_focus_tagline),
            color = EMBER_SECONDARY,
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** The mockup's two small cards (battery, next event) filled with real data; a card is simply
 * absent when its data isn't available, and the row disappears if neither applies. */
@Composable
private fun GlanceRow() {
    val battery by rememberBatterySnapshot()
    val alarm by rememberNextAlarmSnapshot()
    if (battery == null && alarm == null) return
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        battery?.let { snapshot ->
            val suffix = if (snapshot.isCharging) " · ${stringResource(R.string.battery_charging)}" else ""
            GlanceCard(
                title = stringResource(R.string.battery_label),
                value = "${snapshot.percent}%",
                caption = suffix.removePrefix(" · "),
                modifier = Modifier.weight(1f),
            )
        }
        alarm?.let { snapshot ->
            GlanceCard(
                title = stringResource(R.string.next_alarm_label),
                value = formatEpochTime(snapshot.triggerAtMillis, is24Hour, locale),
                caption = "",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GlanceCard(title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(EMBER_CUT)
            .background(EMBER_SURFACE)
            .border(1.dp, EMBER_COPPER.copy(alpha = 0.5f), EMBER_CUT)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text = title.uppercase(), color = EMBER_COPPER, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 0.5.sp)
        Text(text = value, color = EMBER_INK, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
        if (caption.isNotEmpty()) {
            Text(text = caption, color = EMBER_SECONDARY, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}

/** The mockup's wide media block, replaced with real local history (contract: never fake media).
 * Absent entirely until the launcher has actually opened something. */
@Composable
private fun QuickAccessBlock(recentApps: List<AppEntry>, actions: RiceActions) {
    if (recentApps.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EMBER_CUT)
            .background(EMBER_SURFACE)
            .border(1.dp, EMBER_COPPER.copy(alpha = 0.5f), EMBER_CUT)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(text = stringResource(R.string.ember_quick_access), color = EMBER_COPPER, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (entry in recentApps.take(4)) {
                Column(
                    modifier = Modifier.appCellPressable(
                        pressScale = RiceMotion.Ember.pressScale,
                        pressMs = RiceMotion.Ember.pressMs,
                        pressSpec = RiceMotion.Ember.pressSpec,
                        onClick = { actions.openApp(entry.key) },
                        onLongClickAt = { rect -> actions.showAppMenu(entry.key, rect) },
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppIcon(entry = entry, size = 34.dp, plateShape = EMBER_CUT, plateColor = EMBER_CARBON)
                    Text(
                        text = entry.label,
                        color = EMBER_INK,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp).width(56.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesMatrix(favorites: List<FavoriteSlot>, actions: RiceActions) {
    if (favorites.isEmpty()) {
        Text(text = stringResource(R.string.favorites_empty_hint), color = EMBER_SECONDARY, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        return
    }
    // UX overhaul §9: a LazyVerticalGrid (no explicit height, so it still sizes to its own content
    // like the Column+chunked(2) it replaces) reproduces the exact same 1-full-width+2-column
    // layout for free via a span on the first item — and gets add/remove/reflow animation via
    // animateItem() that the hand-chunked version couldn't have without a lot more bookkeeping.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            favorites,
            key = { _, slot -> "${slot.key.userSerial}:${slot.key.component}" },
            span = { index, _ -> GridItemSpan(if (index == 0) 2 else 1) },
        ) { index, slot ->
            FavoriteBlock(
                slot = slot,
                actions = actions,
                modifier = Modifier.fillMaxWidth().heightIn(min = if (index == 0) 72.dp else 64.dp).animateItem(),
            )
        }
    }
}

@Composable
private fun FavoriteBlock(slot: FavoriteSlot, actions: RiceActions, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val app = slot.app
    val background = if (pressed) EMBER_INK else EMBER_SURFACE
    val border = if (pressed) EMBER_SURFACE else EMBER_COPPER.copy(alpha = 0.5f)
    val labelColor = if (pressed) EMBER_SURFACE else EMBER_INK
    val removeLabel = stringResource(R.string.action_remove_favorite)
    // Ember's own press mechanic (an inverted fill, not a scale — §5/§19: "presión física", never
    // the shared scale-tween every other cell uses) still needs the item's own bounds to anchor
    // the context menu, so this captures them the same way appCellPressable does internally.
    var bounds by remember { mutableStateOf(ScreenRect.Zero) }
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val window = coordinates.boundsInWindow()
                bounds = ScreenRect(window.left, window.top, window.right, window.bottom)
            }
            .clip(EMBER_CUT)
            .background(background)
            .border(1.dp, border, EMBER_CUT)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { app?.let { actions.openApp(it.key) } },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    actions.showAppMenu(slot.key, bounds)
                },
                onLongClickLabel = removeLabel,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (app != null) {
            AppIcon(entry = app, size = 36.dp, plateShape = EMBER_CUT, plateColor = Color.Transparent)
        } else {
            Box(modifier = Modifier.size(36.dp))
        }
        Text(
            text = app?.label ?: stringResource(R.string.favorite_unavailable),
            color = labelColor,
            fontFamily = FontFamily.SansSerif,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TriggerBar(actions: RiceActions) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .clip(EMBER_CUT)
                .background(EMBER_SURFACE)
                .border(1.dp, EMBER_COPPER.copy(alpha = 0.5f), EMBER_CUT)
                .clickable(onClick = actions.openDrawer)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.ember_all_apps), color = EMBER_INK, fontFamily = FontFamily.Monospace, fontSize = 13.sp, letterSpacing = 1.sp)
            Text(text = "→", color = EMBER_COPPER, fontSize = 16.sp)
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(EMBER_CUT)
                .background(EMBER_SURFACE)
                .border(1.dp, EMBER_COPPER, EMBER_CUT)
                .clickable(onClick = actions.openPicker),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "R", color = EMBER_COPPER, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
