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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.apps.packageNameGuess
import dev.cesarmanzocode.ricemobile.launcher.ClockProvider
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockDate
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockTime
import dev.cesarmanzocode.ricemobile.ui.shared.rememberCurrentLocale
import dev.cesarmanzocode.ricemobile.ui.shared.rememberIs24HourFormat
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperBackdrop
import java.time.format.FormatStyle

internal val IVORY_BACKGROUND = Color(0xFFF3EBDD)
internal val IVORY_INK = Color(0xFF25231E)
internal val IVORY_SECONDARY = Color(0xFF655F55)
internal val IVORY_RULE = Color(0xFFAAA08D)

/**
 * Ivory Home (contract §18.5): a small dateline first, generous top air, a serif hour sitting at
 * mid-height, then favorites as a numbered textual index — no icons on Home, an editorial choice.
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
            Spacer(modifier = Modifier.weight(1f))
            ClockBlock()
            Spacer(modifier = Modifier.weight(0.7f))
            FavoritesIndex(favorites = model.favorites, actions = actions)
            Spacer(modifier = Modifier.padding(top = 24.dp))
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
            fontSize = 72.sp,
            lineHeight = 76.sp,
        )
        HorizontalDivider(color = IVORY_RULE, thickness = 1.dp, modifier = Modifier.width(56.dp).padding(top = 10.dp))
    }
}

@Composable
private fun FavoritesIndex(favorites: List<FavoriteSlot>, actions: RiceActions) {
    if (favorites.isEmpty()) {
        Text(text = stringResource(R.string.favorites_empty_hint), color = IVORY_SECONDARY, fontFamily = FontFamily.Serif, fontSize = 17.sp)
        return
    }
    Column {
        for ((index, slot) in favorites.withIndex()) {
            val app = slot.app
            val label = app?.label ?: "${slot.key.packageNameGuess} · ${stringResource(R.string.favorite_unavailable)}"
            val removeLabel = stringResource(R.string.action_remove_favorite)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .combinedClickable(
                        onClick = { app?.let { actions.openApp(it.key) } },
                        onLongClick = { actions.showAppMenu(slot.key) },
                        onLongClickLabel = removeLabel,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "%02d".format(index + 1),
                    color = IVORY_RULE,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    modifier = Modifier.width(28.dp),
                )
                Text(
                    text = label,
                    color = if (app != null) IVORY_INK else IVORY_SECONDARY,
                    fontFamily = FontFamily.Serif,
                    fontSize = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (index != favorites.lastIndex) HorizontalDivider(color = IVORY_RULE.copy(alpha = 0.5f))
        }
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
