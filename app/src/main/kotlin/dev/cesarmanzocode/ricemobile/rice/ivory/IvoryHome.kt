package dev.cesarmanzocode.ricemobile.rice.ivory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.packageNameGuess
import dev.cesarmanzocode.ricemobile.launcher.ClockProvider
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.system.rememberBatterySnapshot
import dev.cesarmanzocode.ricemobile.system.rememberNextAlarmSnapshot
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockDate
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockTime
import dev.cesarmanzocode.ricemobile.ui.shared.formatEpochTime
import dev.cesarmanzocode.ricemobile.ui.shared.rememberCurrentLocale
import dev.cesarmanzocode.ricemobile.ui.shared.rememberIs24HourFormat
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperBackdrop
import java.time.format.FormatStyle

internal val IVORY_BACKGROUND = Color(0xFFF3EBDD)
internal val IVORY_INK = Color(0xFF25231E)
internal val IVORY_SECONDARY = Color(0xFF655F55)
internal val IVORY_RULE = Color(0xFFAAA08D)
private val IVORY_PLATE = Color(0xFFE9DFCC)

/**
 * Ivory Home (approved mockup, Sprint 3 second pass): a dateline first, a huge serif hour, a
 * short editorial line, a rich "HOY" module built entirely from real device state (battery, next
 * alarm, most recent app — never a fabricated event/weather line), and favorites as editorial
 * icon tiles at the bottom. The previous pass left this screen reading as an empty document; the
 * mockup's "HOY" block plus icon favorites is what actually fills it with honest content.
 */
@Composable
fun IvoryHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize(),
    ) {
        WallpaperBackdrop(spec = IvoryPaperRice.wallpaper, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp)) {
            DateLine(isDefaultHome = model.isDefaultHome, onRequestHome = actions.requestHomeRole)
            Spacer(modifier = Modifier.weight(0.7f))
            ClockBlock()
            Spacer(modifier = Modifier.padding(top = 14.dp))
            Text(
                text = stringResource(R.string.ivory_tagline),
                color = IVORY_SECONDARY,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
            )
            Spacer(modifier = Modifier.weight(0.6f))
            HoyModule(recentApps = model.recentApps)
            Spacer(modifier = Modifier.padding(top = 22.dp))
            FavoritesRow(favorites = model.favorites, actions = actions)
            Spacer(modifier = Modifier.padding(top = 20.dp))
            FooterRow(actions = actions)
        }
    }
}

@Composable
private fun DateLine(isDefaultHome: Boolean, onRequestHome: () -> Unit) {
    val now by ClockProvider.rememberNow()
    val locale = rememberCurrentLocale()
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = formatClockDate(now, locale, FormatStyle.MEDIUM),
            color = IVORY_SECONDARY,
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.sp,
        )
        if (!isDefaultHome) {
            Text(
                text = stringResource(R.string.action_use_as_home),
                color = IVORY_SECONDARY,
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                modifier = Modifier.defaultMinSize(minHeight = 24.dp).clickable(onClick = onRequestHome),
            )
        }
    }
}

@Composable
private fun ClockBlock() {
    val now by ClockProvider.rememberNow()
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()
    Column {
        Text(
            text = formatClockTime(now, is24Hour, locale),
            color = IVORY_INK,
            fontFamily = FontFamily.Serif,
            fontSize = 76.sp,
            lineHeight = 80.sp,
        )
        HorizontalDivider(color = IVORY_RULE, thickness = 1.dp, modifier = Modifier.width(56.dp).padding(top = 10.dp))
    }
}

/** Editorial "HOY" block (mockup: a rich agenda-shaped list) filled with the only things we can
 * say truthfully without a calendar permission — battery, next alarm, most recent app. A row is
 * simply absent when its data isn't available; the block itself disappears if none apply. */
@Composable
private fun HoyModule(recentApps: List<AppEntry>) {
    val battery by rememberBatterySnapshot()
    val alarm by rememberNextAlarmSnapshot()
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()
    val mostRecent = recentApps.firstOrNull()
    val batteryLabel = stringResource(R.string.battery_label)
    val chargingLabel = stringResource(R.string.battery_charging)
    val alarmLabel = stringResource(R.string.next_alarm_label)
    val recentLabel = stringResource(R.string.home_recent_title)

    val rows = buildList {
        battery?.let { snapshot ->
            val suffix = if (snapshot.isCharging) " · $chargingLabel" else ""
            add(batteryLabel to "${snapshot.percent}%$suffix")
        }
        alarm?.let { snapshot ->
            add(alarmLabel to formatEpochTime(snapshot.triggerAtMillis, is24Hour, locale))
        }
        mostRecent?.let { entry ->
            add(recentLabel to entry.label)
        }
    }
    if (rows.isEmpty()) return

    Column {
        Text(
            text = stringResource(R.string.ivory_hoy_title).uppercase(),
            color = IVORY_SECONDARY,
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
        )
        Spacer(modifier = Modifier.padding(top = 10.dp))
        Column {
            for ((index, row) in rows.withIndex()) {
                val (label, value) = row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = label, color = IVORY_INK, fontFamily = FontFamily.Serif, fontSize = 16.sp)
                    Text(text = value, color = IVORY_SECONDARY, fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                }
                if (index != rows.lastIndex) HorizontalDivider(color = IVORY_RULE.copy(alpha = 0.5f))
            }
        }
    }
}

/** Editorial icon tiles (approved mockup: favorites as a plain icon row, not the previous
 * text-only index) — a soft cream plate around each icon, serif caption below. */
@Composable
private fun FavoritesRow(favorites: List<FavoriteSlot>, actions: RiceActions) {
    if (favorites.isEmpty()) {
        Text(text = stringResource(R.string.favorites_empty_hint), color = IVORY_SECONDARY, fontFamily = FontFamily.Serif, fontSize = 17.sp)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        for (slot in favorites) {
            FavoriteTile(slot = slot, actions = actions)
        }
    }
}

@Composable
private fun FavoriteTile(slot: FavoriteSlot, actions: RiceActions) {
    val app = slot.app
    val removeLabel = stringResource(R.string.action_remove_favorite)
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .combinedClickable(
                onClick = { app?.let { actions.openApp(it.key) } },
                onLongClick = { actions.showAppMenu(slot.key) },
                onLongClickLabel = removeLabel,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (app != null) {
            AppIcon(entry = app, size = 44.dp, plateShape = RoundedCornerShape(12.dp), plateColor = IVORY_PLATE)
        } else {
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .padding(1.dp),
            )
        }
        Text(
            text = app?.label ?: "${slot.key.packageNameGuess} · ${stringResource(R.string.favorite_unavailable)}",
            color = if (app != null) IVORY_INK else IVORY_SECONDARY,
            fontFamily = FontFamily.Serif,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp).widthIn(max = 64.dp),
        )
    }
}

@Composable
private fun FooterRow(actions: RiceActions) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Box(
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).clickable(onClick = actions.openDrawer),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "${stringResource(R.string.drawer_library_title)} ↗",
                color = IVORY_INK,
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                textDecoration = TextDecoration.Underline,
            )
        }
        Box(
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).clickable(onClick = actions.openPicker),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = stringResource(R.string.ivory_edition_action),
                color = IVORY_SECONDARY,
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
            )
        }
    }
}
