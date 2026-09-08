package dev.cesarmanzocode.ricemobile.rice.monochrome

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.IconTreatment
import dev.cesarmanzocode.ricemobile.apps.packageNameGuess
import dev.cesarmanzocode.ricemobile.launcher.ClockProvider
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.system.rememberBatterySnapshot
import dev.cesarmanzocode.ricemobile.system.rememberNextAlarmSnapshot
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface
import dev.cesarmanzocode.ricemobile.ui.shared.appCellPressable
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockAmPm
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockDate
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockTime
import dev.cesarmanzocode.ricemobile.ui.shared.formatEpochTime
import dev.cesarmanzocode.ricemobile.ui.shared.rememberCurrentLocale
import dev.cesarmanzocode.ricemobile.ui.shared.rememberIs24HourFormat
import dev.cesarmanzocode.ricemobile.ui.shared.rememberPressScale
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperBackdrop
import java.time.format.FormatStyle

internal val MONOCHROME_BACKGROUND = Color(0xFF0A0A0A)
internal val MONOCHROME_INK = Color(0xFFF5F5F0)
internal val MONOCHROME_SECONDARY = Color(0xFFA3A3A0)
internal val MONOCHROME_BORDER = Color(0xFF454545)

/**
 * Monochrome Home (approved mockup, Sprint 3 second pass): a rigid start-aligned column with an
 * oversized clock, a short editorial tagline, one compact real-data module (battery/next alarm —
 * never fabricated), and a linear list of favorite rows at the bottom. The middle stays quiet on
 * purpose, but is no longer *empty*: the glance module is the one piece of always-true content
 * that fills the gap the mockup shows without inventing weather/media we don't have.
 */
@Composable
fun MonochromeHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        // UX overhaul §1: the old fire-once threshold is fully superseded by the live drag below
        // (matches Arctic's own wiring, interaction sprint §2-3) — `onSwipeUp = {}` keeps
        // HomeGestureSurface's own accumulator harmless-but-unused instead of double-driving the
        // transition once both fire on release.
        onSwipeUp = {},
        onLongPress = actions.openPicker,
        onDragStart = actions.beginDrawerDrag,
        onDrag = actions.dragDrawer,
        onDragEnd = actions.endDrawerDrag,
        modifier = modifier.fillMaxSize(),
    ) {
        WallpaperBackdrop(spec = MonochromeRice.wallpaper, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            ClockBlock(isDefaultHome = model.isDefaultHome, onRequestHome = actions.requestHomeRole)
            Spacer(modifier = Modifier.padding(top = 20.dp))
            GlanceModule()
            Spacer(modifier = Modifier.weight(1f))
            FavoritesBlock(favorites = model.favorites, actions = actions)
        }
    }
}

@Composable
private fun ClockBlock(isDefaultHome: Boolean, onRequestHome: () -> Unit) {
    val now by ClockProvider.rememberNow()
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()

    Column {
        if (!isDefaultHome) {
            Box(
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).clickable(onClick = onRequestHome),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.action_use_as_home),
                    color = MONOCHROME_SECONDARY,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatClockTime(now, is24Hour, locale),
                color = MONOCHROME_INK,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 88.sp,
                lineHeight = 92.sp,
            )
            if (!is24Hour) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatClockAmPm(now, locale),
                    color = MONOCHROME_SECONDARY,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
        }
        HorizontalDivider(
            color = MONOCHROME_BORDER,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth(0.42f).padding(vertical = 10.dp),
        )
        Text(
            text = formatClockDate(now, locale, FormatStyle.FULL),
            color = MONOCHROME_SECONDARY,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

/** A single compact, dark, horizontal module (mockup: the media-player-shaped block right under
 * the clock) — filled with the one thing we can say honestly: battery and, when the system has
 * one, the next alarm. Absent both, it disappears rather than showing an empty box. */
@Composable
private fun GlanceModule() {
    val battery by rememberBatterySnapshot()
    val alarm by rememberNextAlarmSnapshot()
    if (battery == null && alarm == null) return
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MONOCHROME_BORDER)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        battery?.let { snapshot ->
            val chargingSuffix = if (snapshot.isCharging) " · ${stringResource(R.string.battery_charging).uppercase()}" else ""
            Text(
                text = "${stringResource(R.string.battery_label).uppercase()} ${snapshot.percent}%$chargingSuffix",
                color = MONOCHROME_INK,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            )
        }
        alarm?.let { snapshot ->
            if (battery != null) Spacer(modifier = Modifier.padding(top = 6.dp))
            Text(
                text = "${stringResource(R.string.next_alarm_label).uppercase()} · ${formatEpochTime(snapshot.triggerAtMillis, is24Hour, locale)}",
                color = MONOCHROME_SECONDARY,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
private fun FavoritesBlock(favorites: List<FavoriteSlot>, actions: RiceActions) {
    Column {
        if (favorites.isEmpty()) {
            Text(
                text = stringResource(R.string.favorites_empty_hint),
                color = MONOCHROME_SECONDARY,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        } else {
            // UX overhaul §9: LazyColumn + animateItem() — no explicit height/weight modifier, so
            // this still sizes to its own content like the plain Column it replaces (LazyColumn
            // only fills a *bounded* incoming constraint when its content is actually that tall);
            // TriggerBar below keeps its usual position.
            LazyColumn {
                itemsIndexed(favorites, key = { _, slot -> "${slot.key.userSerial}:${slot.key.component}" }) { index, slot ->
                    Column(modifier = Modifier.animateItem()) {
                        FavoriteRow(index = index + 1, slot = slot, actions = actions)
                        if (index != favorites.lastIndex) {
                            HorizontalDivider(color = MONOCHROME_BORDER.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.padding(top = 16.dp))
        TriggerBar(actions = actions)
    }
}

@Composable
private fun FavoriteRow(index: Int, slot: FavoriteSlot, actions: RiceActions) {
    val app = slot.app
    val removeLabel = stringResource(R.string.action_remove_favorite)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .appCellPressable(
                pressScale = RiceMotion.Monochrome.pressScale,
                pressMs = RiceMotion.Monochrome.pressMs,
                pressSpec = RiceMotion.Monochrome.pressSpec,
                onClick = { app?.let { actions.openApp(it.key) } },
                onLongClickAt = { rect -> actions.showAppMenu(slot.key, rect) },
                onLongClickLabel = removeLabel,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%02d".format(index),
            color = MONOCHROME_SECONDARY,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.width(32.dp),
        )
        if (app != null) {
            AppIcon(
                entry = app,
                size = 30.dp,
                treatment = IconTreatment.Monochrome,
                tint = MONOCHROME_INK,
                plateShape = RectangleShape,
                plateColor = Color.Transparent,
            )
        } else {
            Box(modifier = Modifier.width(30.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app?.label ?: "${slot.key.packageNameGuess} · ${stringResource(R.string.favorite_unavailable)}",
            color = if (app != null) MONOCHROME_INK else MONOCHROME_SECONDARY,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TriggerBar(actions: RiceActions) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MonoTrigger(text = stringResource(R.string.action_open_drawer), onClick = actions.openDrawer)
        MonoTrigger(text = stringResource(R.string.action_open_rice_picker).uppercase(), onClick = actions.openPicker, alignEnd = true)
    }
}

@Composable
private fun MonoTrigger(text: String, onClick: () -> Unit, alignEnd: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressAlpha by rememberPressScale(interactionSource, 0.55f, RiceMotion.Monochrome.pressMs)
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .alpha(pressAlpha)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(text = text, color = MONOCHROME_INK, fontFamily = FontFamily.Monospace, fontSize = 14.sp, letterSpacing = 1.5.sp)
    }
}
