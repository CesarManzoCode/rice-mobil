package dev.cesarmanzocode.ricemobile.rice.arctic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import dev.cesarmanzocode.ricemobile.system.BatterySnapshot
import dev.cesarmanzocode.ricemobile.system.NextAlarmSnapshot
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

internal val ARCTIC_BACKGROUND = Color(0xFF071B2A)
internal val ARCTIC_INK = Color(0xFFE8FAFF)
internal val ARCTIC_SECONDARY = Color(0xFFACCAD5)
internal val ARCTIC_ACCENT = Color(0xFF78DCEF)
internal val ARCTIC_GLASS = Color(0xE0142E42)
internal val ARCTIC_BORDER = Color(0x2EFFFFFF)
/** Icon plate contract §9 "halo cyan muy sutil": a faint brand-cyan tint instead of plain white,
 * so an icon plate reads as Arctic's own rather than a generic translucent circle. */
internal val ARCTIC_ICON_PLATE = Color(0x3378DCEF)

/**
 * Arctic Home (approved mockup, Sprint 3 second pass): a light centered clock, the mockup's
 * weather+agenda glass panel pair replaced with real data (battery/next alarm, and a recent-apps
 * quick panel — never fabricated), and the same floating dock + Apps pill + Rice circle at the
 * bottom. Panels keep the mockup's mass/position/materiality even though their content changed.
 */
@Composable
fun ArcticHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize(),
    ) {
        WallpaperBackdrop(spec = ArcticGlassRice.wallpaper, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Spacer(modifier = Modifier.height(28.dp))
            ClockPill(isDefaultHome = model.isDefaultHome, onRequestHome = actions.requestHomeRole)
            Spacer(modifier = Modifier.height(20.dp))
            GlassPanels(recentApps = model.recentApps, actions = actions)
            Spacer(modifier = Modifier.weight(1f))
            AppsPill(onClick = actions.openDrawer)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Dock(favorites = model.favorites, actions = actions, modifier = Modifier.weight(1f))
                RiceCircle(onClick = actions.openPicker)
            }
        }
    }
}

@Composable
private fun ClockPill(isDefaultHome: Boolean, onRequestHome: () -> Unit) {
    val now by ClockProvider.rememberNow()
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = formatClockTime(now, is24Hour, locale),
            color = ARCTIC_INK,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 76.sp,
        )
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(ARCTIC_GLASS)
                .border(1.dp, ARCTIC_BORDER, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(text = formatClockDate(now, locale, FormatStyle.MEDIUM), color = ARCTIC_SECONDARY, fontSize = 14.sp)
        }
        if (!isDefaultHome) {
            Text(
                text = stringResource(R.string.action_use_as_home),
                color = ARCTIC_ACCENT,
                fontSize = 13.sp,
                modifier = Modifier.defaultMinSize(minHeight = 40.dp).padding(top = 8.dp).clickable(onClick = onRequestHome),
            )
        }
    }
}

/** The mockup's weather + agenda glass panel pair, filled with real data instead: battery/next
 * alarm on one panel, a recent-apps quick panel on the other. Either panel — or the whole row —
 * disappears when it has nothing true to show. */
@Composable
private fun GlassPanels(recentApps: List<AppEntry>, actions: RiceActions) {
    val battery by rememberBatterySnapshot()
    val alarm by rememberNextAlarmSnapshot()
    val showGlance = battery != null || alarm != null
    val showRecents = recentApps.isNotEmpty()
    if (!showGlance && !showRecents) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showGlance) GlancePanel(battery = battery, alarm = alarm)
        if (showRecents) RecentsPanel(recentApps = recentApps, actions = actions)
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ARCTIC_GLASS)
            .border(1.dp, ARCTIC_BORDER, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
private fun GlancePanel(battery: BatterySnapshot?, alarm: NextAlarmSnapshot?) {
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()
    GlassPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            battery?.let { snapshot ->
                val suffix = if (snapshot.isCharging) " · ${stringResource(R.string.battery_charging)}" else ""
                LabeledValue(label = stringResource(R.string.battery_label), value = "${snapshot.percent}%$suffix")
            }
            alarm?.let { snapshot ->
                LabeledValue(label = stringResource(R.string.next_alarm_label), value = formatEpochTime(snapshot.triggerAtMillis, is24Hour, locale))
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(text = label.uppercase(), color = ARCTIC_SECONDARY, fontSize = 11.sp, letterSpacing = 0.5.sp)
        Text(text = value, color = ARCTIC_INK, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RecentsPanel(recentApps: List<AppEntry>, actions: RiceActions) {
    GlassPanel {
        Text(text = stringResource(R.string.home_recent_title).uppercase(), color = ARCTIC_SECONDARY, fontSize = 11.sp, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (entry in recentApps.take(4)) {
                Column(
                    modifier = Modifier.combinedClickable(
                        onClick = { actions.openApp(entry.key) },
                        onLongClick = { actions.showAppMenu(entry.key) },
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AppIcon(entry = entry, size = 36.dp, plateShape = CircleShape, plateColor = ARCTIC_ICON_PLATE)
                }
            }
        }
    }
}

@Composable
private fun AppsPill(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(50))
                .background(ARCTIC_GLASS)
                .border(1.dp, ARCTIC_BORDER, RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.arctic_explore_hint) + " ↑", color = ARCTIC_INK, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Dock(favorites: List<FavoriteSlot>, actions: RiceActions, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(28.dp), clip = false)
            .clip(RoundedCornerShape(28.dp))
            .background(ARCTIC_GLASS)
            .border(1.dp, ARCTIC_BORDER, RoundedCornerShape(28.dp))
            .heightIn(min = 76.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (favorites.isEmpty()) {
            Text(
                text = stringResource(R.string.favorites_empty_hint),
                color = ARCTIC_SECONDARY,
                fontSize = 12.sp,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        } else {
            for (slot in favorites) {
                DockSlot(slot = slot, actions = actions, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DockSlot(slot: FavoriteSlot, actions: RiceActions, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource, RiceMotion.Arctic.pressScale, RiceMotion.Arctic.pressMs)
    val app = slot.app
    val removeLabel = stringResource(R.string.action_remove_favorite)
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { app?.let { actions.openApp(it.key) } },
                onLongClick = { actions.showAppMenu(slot.key) },
                onLongClickLabel = removeLabel,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (app != null) {
            AppIcon(entry = app, size = 40.dp, plateShape = CircleShape, plateColor = ARCTIC_ICON_PLATE)
            Text(
                text = app.label,
                color = ARCTIC_INK,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0x1AFFFFFF)))
            Text(text = "·", color = ARCTIC_SECONDARY, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun RiceCircle(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(ARCTIC_GLASS)
            .border(1.dp, ARCTIC_BORDER, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "◐", color = ARCTIC_ACCENT, fontSize = 18.sp)
    }
}
