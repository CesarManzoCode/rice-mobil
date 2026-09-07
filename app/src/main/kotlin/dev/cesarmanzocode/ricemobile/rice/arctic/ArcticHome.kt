package dev.cesarmanzocode.ricemobile.rice.arctic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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

internal val ARCTIC_BACKGROUND = Color(0xFF071322)
internal val ARCTIC_INK = Color(0xFFF5FBFF)
internal val ARCTIC_SECONDARY = Color(0xFFB7D2DE)
internal val ARCTIC_ACCENT = Color(0xFF7FE3F2)

/** Base tint for a still glass panel; [ARCTIC_DOCK_TINT] is a touch brighter/cooler so the
 * floating dock reads as "más luminoso, integrado con wallpaper" instead of matching the panels
 * above it exactly (contract §"DOCK"). Both are combined with [ArcticGlassSurface]'s gradient +
 * top highlight, never a flat `Color.copy(alpha=x)` fill. */
internal val ARCTIC_GLASS_TINT = Color(0xFF1C3B52)
internal val ARCTIC_DOCK_TINT = Color(0xFF2E5A70)
internal val ARCTIC_BORDER = Color(0x38FFFFFF)
internal val ARCTIC_BORDER_HI = Color(0x82FFFFFF)
internal val ARCTIC_SHADOW = Color(0x59021019)

/** Icon plate contract §9 "halo cyan muy sutil": a faint brand-cyan tint instead of plain white,
 * so an icon plate reads as Arctic's own rather than a generic translucent circle. */
internal val ARCTIC_ICON_PLATE = Color(0x3378DCEF)

/**
 * Arctic Glass V2 (Sprint 3, mockup reconstruction): the approved mockup's mass/density/depth,
 * reproduced with real data. A dominant left-aligned clock, two glass modules with the mockup's
 * weather+agenda mass (battery/next-alarm, and recents/quick-access — never fabricated), decorative
 * page dots, and a brighter floating dock. Glass is a real material built from gradients + a top
 * highlight + a soft border ([ArcticGlassSurface]), not a flat translucent rectangle.
 */
@Composable
fun ArcticHome(model: HomeModel, actions: RiceActions, modifier: Modifier = Modifier) {
    HomeGestureSurface(
        onSwipeUp = actions.openDrawer,
        onLongPress = actions.openPicker,
        modifier = modifier.fillMaxSize(),
    ) {
        WallpaperBackdrop(spec = ArcticGlassRice.wallpaper, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Spacer(modifier = Modifier.height(40.dp))
            ArcticClockBlock(isDefaultHome = model.isDefaultHome, onRequestHome = actions.requestHomeRole)
            Spacer(modifier = Modifier.height(26.dp))
            GlanceModule(modifier = Modifier.fillMaxWidth(0.86f).align(Alignment.CenterHorizontally))
            val quickApps = quickAccessApps(model)
            if (quickApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                QuickAccessModule(
                    apps = quickApps,
                    onOpenAll = actions.openDrawer,
                    actions = actions,
                    modifier = Modifier.fillMaxWidth(0.86f).align(Alignment.CenterHorizontally),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            PageDots(onOpenDrawer = actions.openDrawer, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.86f).align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArcticDock(favorites = model.favorites, actions = actions, modifier = Modifier.weight(1f))
                ArcticRiceButton(onClick = actions.openPicker)
            }
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

/** Recientes if there are at least two (a single recent app is too thin a "masa visual" on its
 * own — falls back to favorites per contract), otherwise favorites fill the row; never fabricated,
 * never duplicated, and the whole module disappears rather than render an empty shell. */
private fun quickAccessApps(model: HomeModel): List<AppEntry> {
    if (model.recentApps.size >= 2) return model.recentApps.take(4)
    val favoriteApps = model.favorites.mapNotNull { it.app }
    return (model.recentApps + favoriteApps).distinctBy { it.key }.take(4)
}

/**
 * Arctic's own glass material: a vertical tint gradient (never one flat alpha color), a soft
 * top-third highlight standing in for a specular sheen, a bright-to-dim border gradient, and a
 * short shadow for lift off the wallpaper. Shared by every Arctic surface — Home modules, dock,
 * and the Drawer panel/tiles — so the *material* is consistent without a shared visual widget
 * dictating layout (contract §16: "shared geometry: no").
 */
@Composable
internal fun ArcticGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    tint: Color = ARCTIC_GLASS_TINT,
    baseAlpha: Float = 0.30f,
    onClick: (() -> Unit)? = null,
    // Explicit rather than relying on constraint propagation: a caller with an unbounded max
    // height (e.g. a pill inside a LazyRow with no height of its own) can't safely centre its
    // content by giving that content fillMaxSize() — "no effect" is exactly what fillMax*
    // documents for an unbounded max, so a Chip-style single-line pill passes Alignment.Center
    // here instead of depending on that edge case resolving the way it happens to for a bounded
    // caller like the dock.
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 16.dp, shape = shape, clip = false, ambientColor = ARCTIC_SHADOW, spotColor = ARCTIC_SHADOW)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to tint.copy(alpha = (baseAlpha + 0.12f).coerceAtMost(1f)),
                    0.55f to tint.copy(alpha = baseAlpha),
                    1f to tint.copy(alpha = (baseAlpha + 0.06f).coerceAtMost(1f)),
                ),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(width = 1.dp, brush = Brush.verticalGradient(listOf(ARCTIC_BORDER_HI, ARCTIC_BORDER)), shape = shape),
        contentAlignment = contentAlignment,
    ) {
        // matchParentSize (not fillMaxHeight(fraction)): the Box's own height comes from
        // [content] below, an unbounded constraint from the Column above it, so fillMaxHeight
        // would silently no-op (Compose only honors a height fraction against a *bounded* max
        // height). The top-only sheen instead comes from where the gradient stops, not from the
        // overlay's measured size.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.16f),
                        0.42f to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                ),
        )
        content()
    }
}

@Composable
private fun ArcticClockBlock(isDefaultHome: Boolean, onRequestHome: () -> Unit) {
    val now by ClockProvider.rememberNow()
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()
    val dateText = remember(now, locale) {
        formatClockDate(now, locale, FormatStyle.FULL).replaceFirstChar { it.titlecase(locale) }
    }
    Column(modifier = Modifier.padding(start = 32.dp, end = 24.dp)) {
        Text(
            text = formatClockTime(now, is24Hour, locale),
            color = ARCTIC_INK,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            fontSize = 88.sp,
            lineHeight = 92.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = dateText, color = ARCTIC_INK.copy(alpha = 0.82f), fontSize = 19.sp, fontWeight = FontWeight.Medium)
        if (!isDefaultHome) {
            Text(
                text = stringResource(R.string.action_use_as_home),
                color = ARCTIC_ACCENT,
                fontSize = 13.sp,
                modifier = Modifier.defaultMinSize(minHeight = 40.dp).padding(top = 10.dp).clickable(onClick = onRequestHome),
            )
        }
    }
}

/** The mockup's weather card mass, filled with real battery/next-alarm data. Reorganizes rather
 * than leaving a gap when one of the two is missing, and disappears entirely only when both are
 * (contract §"CERO DATOS FALSOS": never a fabricated placeholder). */
@Composable
private fun GlanceModule(modifier: Modifier = Modifier) {
    val battery by rememberBatterySnapshot()
    val alarm by rememberNextAlarmSnapshot()
    if (battery == null && alarm == null) return
    val is24Hour = rememberIs24HourFormat()
    val locale = rememberCurrentLocale()

    ArcticGlassSurface(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            val snapshot = battery
            if (snapshot != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    // weight(1f) so a long localized label at a large fontScale wraps inside its
                    // own column instead of pushing the fixed-size ring out of the Row's bounds.
                    Column(modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp)) {
                        Text(text = "${snapshot.percent}%", color = ARCTIC_INK, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = stringResource(if (snapshot.isCharging) R.string.battery_charging else R.string.battery_label),
                            color = ARCTIC_SECONDARY,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    BatteryRing(percent = snapshot.percent, charging = snapshot.isCharging, modifier = Modifier.size(50.dp))
                }
            } else if (alarm != null) {
                // Battery unavailable (contract-legal: registerReceiver hasn't delivered yet) —
                // the alarm becomes the module's primary display instead of leaving a gap.
                Text(text = formatEpochTime(alarm!!.triggerAtMillis, is24Hour, locale), color = ARCTIC_INK, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                Text(text = stringResource(R.string.next_alarm_label), color = ARCTIC_SECONDARY, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
            }
            if (snapshot != null && alarm != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ARCTIC_BORDER))
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.next_alarm_label).uppercase(), color = ARCTIC_SECONDARY, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    Text(text = formatEpochTime(alarm!!.triggerAtMillis, is24Hour, locale), color = ARCTIC_INK, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** A ring instead of a literal battery glyph: reads clearly at 50dp, never confusable with a
 * charging animation, and needs no new asset. Full ring = 100%, cyan when charging. */
@Composable
private fun BatteryRing(percent: Int, charging: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.14f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = ARCTIC_BORDER_HI,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = if (charging) ARCTIC_ACCENT else ARCTIC_INK,
            startAngle = -90f,
            sweepAngle = 360f * (percent / 100f).coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

/** The mockup's agenda-card mass, filled with recents/favorites (§quickAccessApps). */
@Composable
private fun QuickAccessModule(apps: List<AppEntry>, onOpenAll: () -> Unit, actions: RiceActions, modifier: Modifier = Modifier) {
    ArcticGlassSurface(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_recent_title).uppercase(),
                    color = ARCTIC_INK,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp),
                )
                Text(
                    text = stringResource(R.string.home_recent_see_all),
                    color = ARCTIC_ACCENT,
                    fontSize = 13.sp,
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp).padding(vertical = 8.dp).clickable(onClick = onOpenAll),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                for (entry in apps) {
                    AppQuickTile(entry = entry, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun AppQuickTile(entry: AppEntry, actions: RiceActions) {
    Column(
        modifier = Modifier.combinedClickable(
            onClick = { actions.openApp(entry.key) },
            onLongClick = { actions.showAppMenu(entry.key) },
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(entry = entry, size = 44.dp, plateShape = CircleShape, plateColor = ARCTIC_ICON_PLATE)
        Text(
            text = entry.label,
            color = ARCTIC_INK,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp).width(50.dp),
        )
    }
}

/**
 * Purely decorative (contract §"PAGE DOTS": never simulates paging that doesn't exist), but the
 * whole row is also this Home's one explicit, accessible "open the drawer" affordance — the same
 * action `HomeGestureSurface`'s swipe-up already performs, just reachable without a gesture
 * (TalkBack, a stylus, a d-pad). A 48dp target with a real content description, not a fake nav bit.
 */
@Composable
private fun PageDots(onOpenDrawer: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.action_open_drawer_hint)
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onOpenDrawer)
            .semantics(mergeDescendants = true) { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { index ->
                val active = index == 0
                Box(
                    modifier = Modifier
                        .size(if (active) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (active) ARCTIC_INK else ARCTIC_SECONDARY.copy(alpha = 0.55f)),
                )
            }
        }
    }
}

@Composable
private fun ArcticDock(favorites: List<FavoriteSlot>, actions: RiceActions, modifier: Modifier = Modifier) {
    ArcticGlassSurface(
        modifier = modifier.heightIn(min = 108.dp, max = 122.dp),
        shape = RoundedCornerShape(32.dp),
        tint = ARCTIC_DOCK_TINT,
        baseAlpha = 0.34f,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            horizontalArrangement = if (favorites.isEmpty()) Arrangement.Center else Arrangement.SpaceEvenly,
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
            // 46dp — the lower end of the mockup's 46-54dp range, not the middle: with 5
            // favorites (the contract's max) on a narrow device, a weighted dock slot can be
            // under 50dp wide, and a fixed-size icon is clamped to whatever width it's given.
            AppIcon(entry = app, size = 46.dp, plateShape = CircleShape, plateColor = ARCTIC_ICON_PLATE)
            Text(
                text = app.label,
                color = ARCTIC_INK,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )
        } else {
            Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0x1FFFFFFF)))
            Text(text = "·", color = ARCTIC_SECONDARY, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun ArcticRiceButton(onClick: () -> Unit) {
    val label = stringResource(R.string.action_open_rice_picker)
    ArcticGlassSurface(
        modifier = Modifier.size(56.dp).semantics(mergeDescendants = true) { role = Role.Button; contentDescription = label },
        shape = CircleShape,
        tint = ARCTIC_DOCK_TINT,
        baseAlpha = 0.34f,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "◐", color = ARCTIC_ACCENT, fontSize = 19.sp, modifier = Modifier.clearAndSetSemantics {})
    }
}
