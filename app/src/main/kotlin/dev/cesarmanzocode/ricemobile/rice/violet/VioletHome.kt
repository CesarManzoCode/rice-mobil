package dev.cesarmanzocode.ricemobile.rice.violet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.launcher.ClockProvider
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.system.rememberBatterySnapshot
import dev.cesarmanzocode.ricemobile.system.rememberNextAlarmSnapshot
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockDate
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockTime
import dev.cesarmanzocode.ricemobile.ui.shared.formatEpochTime
import dev.cesarmanzocode.ricemobile.ui.shared.rememberCurrentLocale
import dev.cesarmanzocode.ricemobile.ui.shared.rememberIs24HourFormat
import dev.cesarmanzocode.ricemobile.ui.shared.rememberPressScale
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperBackdrop
import java.time.format.FormatStyle

internal val VIOLET_INDIGO = Color(0xFF100C24)
internal val VIOLET_VIOLET = Color(0xFF302053)
internal val VIOLET_INK = Color(0xFFF3EDFF)
internal val VIOLET_SECONDARY = Color(0xFFC3B3D9)
internal val VIOLET_ACCENT = Color(0xFFBC9BFF)

/**
 * Violet Home (approved mockup, Sprint 3 second pass): the wallpaper stays the protagonist, the
 * clock keeps its small end-aligned corner, and the mockup's music hero module is now a real
 * hero card — most-recent local app front and center, battery/next alarm as a compact overline —
 * same size/position/materiality as the mockup, never fabricated media. The 1-2-2 favorites
 * cluster and footer are unchanged.
 */
@Composable
fun VioletHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize(),
    ) {
        WallpaperBackdrop(spec = VioletNightRice.wallpaper, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ClockBlock(isDefaultHome = model.isDefaultHome, onRequestHome = actions.requestHomeRole)
            }
            Spacer(modifier = Modifier.weight(0.6f))
            HeroModule(recentApps = model.recentApps, actions = actions)
            Spacer(modifier = Modifier.weight(1f))
            Cluster(favorites = model.favorites, actions = actions)
            Spacer(modifier = Modifier.padding(top = 20.dp))
            FooterRow(actions = actions)
        }
    }
}

@Composable
private fun ClockBlock(isDefaultHome: Boolean, onRequestHome: () -> Unit) {
    val now by ClockProvider.rememberNow()
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = formatClockTime(now, is24Hour, locale),
            color = VIOLET_INK,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 56.sp,
        )
        Text(
            text = formatClockDate(now, locale, FormatStyle.MEDIUM),
            color = VIOLET_SECONDARY,
            fontSize = 13.sp,
        )
        if (!isDefaultHome) {
            Text(
                text = stringResource(R.string.action_use_as_home),
                color = VIOLET_ACCENT,
                fontSize = 12.sp,
                modifier = Modifier.defaultMinSize(minHeight = 40.dp).padding(top = 6.dp).clickable(onClick = onRequestHome),
            )
        }
    }
}

/** Same mass/position as the mockup's music card: a large violet panel with the most recent app
 * front and center and a compact battery/alarm overline — real data only, hidden if there's
 * truly nothing (no history yet and no battery reading, which will not happen on a real device). */
@Composable
private fun HeroModule(recentApps: List<AppEntry>, actions: RiceActions) {
    val battery by rememberBatterySnapshot()
    val alarm by rememberNextAlarmSnapshot()
    val mostRecent = recentApps.firstOrNull()
    if (mostRecent == null && battery == null && alarm == null) return
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(VIOLET_VIOLET.copy(alpha = 0.55f))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        if (battery != null || alarm != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                battery?.let { snapshot ->
                    val suffix = if (snapshot.isCharging) " · ${stringResource(R.string.battery_charging)}" else ""
                    Text(text = "${snapshot.percent}%$suffix", color = VIOLET_SECONDARY, fontSize = 12.sp)
                }
                alarm?.let { snapshot ->
                    Text(text = formatEpochTime(snapshot.triggerAtMillis, is24Hour, locale), color = VIOLET_SECONDARY, fontSize = 12.sp)
                }
            }
            if (mostRecent != null) Spacer(modifier = Modifier.padding(top = 12.dp))
        }
        if (mostRecent != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, VIOLET_ACCENT.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(3.dp),
                ) {
                    AppIcon(entry = mostRecent, size = 52.dp, plateShape = RoundedCornerShape(16.dp), plateColor = VIOLET_INDIGO.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.padding(start = 14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.home_recent_title).uppercase(), color = VIOLET_SECONDARY, fontSize = 11.sp, letterSpacing = 0.5.sp)
                    Text(
                        text = mostRecent.label,
                        color = VIOLET_INK,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .background(VIOLET_ACCENT.copy(alpha = 0.25f))
                        .clickable { actions.openApp(mostRecent.key) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "▶", color = VIOLET_ACCENT, fontSize = 14.sp)
                }
            }
        }
    }
}

/** Deterministic 1-2-2 rows (contract §18.6): a partial 1-2 count centers naturally since every
 * row is its own centered Row, never a fixed absolute layout tied to one screen size. */
@Composable
private fun Cluster(favorites: List<FavoriteSlot>, actions: RiceActions) {
    if (favorites.isEmpty()) {
        Text(
            text = stringResource(R.string.favorites_empty_hint),
            color = VIOLET_SECONDARY,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    val rowSizes = listOf(1, 2, 2)
    var index = 0
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        for (rowSize in rowSizes) {
            if (index >= favorites.size) break
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                var slotInRow = 0
                repeat(rowSize) {
                    if (index < favorites.size) {
                        val isPrincipal = index == 0
                        val stagger = if (rowSize == 2 && slotInRow == 1) 16.dp else 0.dp
                        Box(modifier = Modifier.padding(top = stagger, start = if (slotInRow > 0) 24.dp else 0.dp)) {
                            Node(slot = favorites[index], principal = isPrincipal, actions = actions)
                        }
                        index++
                        slotInRow++
                    }
                }
            }
        }
    }
}

@Composable
private fun Node(slot: FavoriteSlot, principal: Boolean, actions: RiceActions) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource, RiceMotion.Violet.pressScale, RiceMotion.Violet.pressMs)
    val app = slot.app
    val removeLabel = stringResource(R.string.action_remove_favorite)
    val nodeSize = if (principal) 56.dp else 44.dp
    val targetSize = if (nodeSize < 56.dp) 56.dp else nodeSize

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = targetSize, minHeight = targetSize)
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { app?.let { actions.openApp(it.key) } },
                    onLongClick = { actions.showAppMenu(slot.key) },
                    onLongClickLabel = removeLabel,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (app != null) {
                AppIcon(entry = app, size = nodeSize, plateShape = CircleShape, plateColor = VIOLET_VIOLET.copy(alpha = 0.85f))
            } else {
                Box(modifier = Modifier.size(nodeSize).clip(CircleShape).background(VIOLET_VIOLET.copy(alpha = 0.5f)))
            }
        }
        Text(
            text = app?.label ?: stringResource(R.string.favorite_unavailable),
            color = VIOLET_INK,
            fontSize = 13.sp,
            maxLines = if (principal) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp).widthIn(max = 72.dp),
        )
    }
}

@Composable
private fun FooterRow(actions: RiceActions) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(50))
                .background(VIOLET_VIOLET.copy(alpha = 0.6f))
                .clickable(onClick = actions.openDrawer)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "${stringResource(R.string.violet_explore_hint)} ↑", color = VIOLET_INK, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = actions.openPicker),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(VIOLET_ACCENT))
        }
    }
}
