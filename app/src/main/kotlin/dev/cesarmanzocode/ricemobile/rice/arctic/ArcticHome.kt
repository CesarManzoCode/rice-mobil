package dev.cesarmanzocode.ricemobile.rice.arctic

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
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.ui.shared.AppIcon
import dev.cesarmanzocode.ricemobile.ui.shared.HomeGestureSurface
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockDate
import dev.cesarmanzocode.ricemobile.ui.shared.formatClockTime
import dev.cesarmanzocode.ricemobile.ui.shared.rememberCurrentLocale
import dev.cesarmanzocode.ricemobile.ui.shared.rememberIs24HourFormat
import dev.cesarmanzocode.ricemobile.ui.shared.rememberPressScale
import dev.cesarmanzocode.ricemobile.launcher.ClockProvider
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperBackdrop
import java.time.format.FormatStyle

internal val ARCTIC_BACKGROUND = Color(0xFF071B2A)
internal val ARCTIC_INK = Color(0xFFE8FAFF)
internal val ARCTIC_SECONDARY = Color(0xFFACCAD5)
internal val ARCTIC_ACCENT = Color(0xFF78DCEF)
internal val ARCTIC_GLASS = Color(0xE0142E42)
internal val ARCTIC_BORDER = Color(0x2EFFFFFF)

/** Arctic Home (contract §18.3): a light clock centered in the upper half, wide empty air, and a
 * floating glass dock near the thumb with an "Apps" pill above it and a discreet Rice circle
 * beside it in the same bottom row. */
@Composable
fun ArcticHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize(),
    ) {
        WallpaperBackdrop(spec = ArcticGlassRice.wallpaper, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            Spacer(modifier = Modifier.height(40.dp))
            ClockPill(isDefaultHome = model.isDefaultHome, onRequestHome = actions.requestHomeRole)
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
            AppIcon(entry = app, size = 40.dp, plateShape = CircleShape, plateColor = Color(0x33FFFFFF))
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
