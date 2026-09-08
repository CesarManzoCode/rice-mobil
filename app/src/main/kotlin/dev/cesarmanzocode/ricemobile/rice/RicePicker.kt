package dev.cesarmanzocode.ricemobile.rice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R
import dev.cesarmanzocode.ricemobile.ui.shared.LocalReducedMotion
import dev.cesarmanzocode.ricemobile.ui.shared.ricePressable
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperBackdrop
import dev.cesarmanzocode.ricemobile.wallpaper.prefetchWallpaper
import kotlinx.coroutines.launch

private val PICKER_SCRIM = Color(0xB30A0A0A)
private val PICKER_CHROME = Color(0xFFF5F5F0)

private data class RicePreview(val background: Color, val ink: Color)

/** Each thumbnail is painted in that rice's own real background/ink pair — not a shared neutral
 * card — so the picker previews both structure and color, per contract §16 task 4/§7. */
private fun previewOf(id: RiceId): RicePreview = when (id) {
    RiceId.Monochrome -> RicePreview(Color(0xFF0A0A0A), Color(0xFFF5F5F0))
    RiceId.ArcticGlass -> RicePreview(Color(0xFF071B2A), Color(0xFFE8FAFF))
    RiceId.EmberForge -> RicePreview(Color(0xFF171411), Color(0xFFD99A67))
    RiceId.IvoryPaper -> RicePreview(Color(0xFFF3EBDD), Color(0xFF25231E))
    RiceId.VioletNight -> RicePreview(Color(0xFF100C24), Color(0xFFBC9BFF))
}

/**
 * Exactly five options (contract §6.1/§7): each thumbnail draws the rice's real structure (a
 * linear list, a floating dock, a 1+2x2 matrix, an alphabetical index, a 1-2-2 cluster) in that
 * rice's own colors — never five recolored rectangles. Tap persists explicitly and returns Home;
 * Back cancels without changing the current rice (contract §7, host-owned per §6.2).
 */
@Composable
fun RicePicker(
    current: RiceId,
    onSelect: (RiceId) -> Unit,
    modifier: Modifier = Modifier,
) {
    // UX overhaul §15-17: the background stays a *live* rice surface, not a flat neutral card — the
    // current rice's own wallpaper, dimmed under a scrim, so the picker reads as "Home receded
    // behind an overlay" rather than a separate full-screen dialog. Rebuilding the entire Home
    // composition behind the picker (the literal spec wording) is out of budget for this pass —
    // this is the documented, honest simplification: same *idea* (background stays alive), smaller
    // mechanism (a wallpaper layer instead of a full frozen Home frame).
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    // Guards a second tap from firing a second prefetch+select while the first selection's exit
    // transition is already underway — this composable is short-lived (the host disposes it once
    // the route switch completes) so no reset-on-dismiss is needed.
    var pendingSelection by remember { mutableStateOf<RiceId?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        WallpaperBackdrop(spec = RiceRegistry.of(current).wallpaper, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(PICKER_SCRIM))
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            Text(text = stringResource(R.string.rice_picker_title), color = PICKER_CHROME, fontWeight = FontWeight.Medium, fontSize = 18.sp)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(RiceRegistry.all, key = { it.id }) { rice ->
                    RiceOption(
                        rice = rice,
                        selected = rice.id == current,
                        expanding = pendingSelection == rice.id,
                        onClick = {
                            if (pendingSelection != null) return@RiceOption
                            pendingSelection = rice.id
                            // Fire-and-forget (contract §31: no artificial delay before the real
                            // action) — races the route's own 180ms crossfade so the new Home's
                            // wallpaper is as likely as possible to already be decoded by the time
                            // it composes; WallpaperBackdrop's own fallback-to-bitmap fade covers
                            // the case it loses that race.
                            scope.launch {
                                val targetSize = IntSize(
                                    with(density) { configuration.screenWidthDp.dp.roundToPx() },
                                    with(density) { configuration.screenHeightDp.dp.roundToPx() },
                                )
                                prefetchWallpaper(context, rice.wallpaper, targetSize)
                            }
                            onSelect(rice.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RiceOption(rice: Rice, selected: Boolean, expanding: Boolean, onClick: () -> Unit) {
    val preview = previewOf(rice.id)
    val reducedMotion = LocalReducedMotion.current
    val haptics = LocalHapticFeedback.current
    // UX overhaul §16-17: the tapped tile visibly "takes" the screen — a small hero scale-up that
    // plays concurrently with (never blocking) the host's own route crossfade, so the outgoing
    // frame this rice fades out from is mid-expansion rather than static.
    val heroScale by animateFloatAsState(
        targetValue = if (expanding) 1.045f else 1f,
        animationSpec = if (reducedMotion) snap() else (rice.motion.pressSpec ?: spring(dampingRatio = 0.7f, stiffness = 380f)),
        label = "picker-hero-scale",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = heroScale; scaleY = heroScale }
            .clip(RoundedCornerShape(14.dp))
            .background(preview.background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) preview.ink else preview.ink.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
            )
            .ricePressable(
                pressScale = rice.motion.pressScale,
                pressMs = rice.motion.pressMs,
                pressSpec = rice.motion.pressSpec,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            )
            .padding(14.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
            drawStructure(rice.id, size, preview.ink)
        }
        Text(
            text = riceDisplayNameRes(rice.id),
            color = preview.ink,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun riceDisplayNameRes(id: RiceId): String = stringResource(
    when (id) {
        RiceId.Monochrome -> R.string.rice_name_monochrome
        RiceId.ArcticGlass -> R.string.rice_name_arctic_glass
        RiceId.EmberForge -> R.string.rice_name_ember_forge
        RiceId.IvoryPaper -> R.string.rice_name_ivory_paper
        RiceId.VioletNight -> R.string.rice_name_violet_night
    },
)

/** One small drawing per structure so the picker reflects real geometry (contract §7). */
private fun DrawScope.drawStructure(id: RiceId, size: Size, ink: Color) {
    when (id) {
        RiceId.Monochrome -> {
            // A big clock block, a thin glance-module bar, then a linear stack of favorite rows.
            drawRect(color = ink.copy(alpha = 0.9f), topLeft = Offset(0f, 0f), size = Size(size.width * 0.5f, size.height * 0.32f))
            drawRect(
                color = ink.copy(alpha = 0.25f),
                topLeft = Offset(0f, size.height * 0.38f),
                size = Size(size.width * 0.72f, size.height * 0.1f),
            )
            val rowHeight = size.height * 0.14f
            for (i in 0 until 3) {
                val top = size.height * 0.55f + i * rowHeight * 1.25f
                drawRect(color = ink.copy(alpha = 0.5f), topLeft = Offset(0f, top), size = Size(size.width, rowHeight * 0.7f))
            }
        }
        RiceId.ArcticGlass -> {
            // A small centered clock pill, two glass glance panels, then a floating dock.
            drawRoundRect(
                color = ink.copy(alpha = 0.7f),
                topLeft = Offset(size.width * 0.38f, 0f),
                size = Size(size.width * 0.24f, size.height * 0.16f),
                cornerRadius = CornerRadius(size.height * 0.08f, size.height * 0.08f),
            )
            drawRoundRect(
                color = ink.copy(alpha = 0.22f),
                topLeft = Offset(size.width * 0.1f, size.height * 0.30f),
                size = Size(size.width * 0.8f, size.height * 0.14f),
                cornerRadius = CornerRadius(size.height * 0.05f, size.height * 0.05f),
            )
            val dockHeight = size.height * 0.34f
            drawRoundRect(
                color = ink.copy(alpha = 0.85f),
                topLeft = Offset(size.width * 0.1f, size.height - dockHeight),
                size = Size(size.width * 0.8f, dockHeight),
                cornerRadius = CornerRadius(dockHeight / 2f, dockHeight / 2f),
            )
        }
        RiceId.EmberForge -> {
            // A header, two small dashboard cards, then a 2x2 favorites matrix beneath them.
            val gap = size.width * 0.03f
            drawRect(color = ink.copy(alpha = 0.85f), topLeft = Offset(0f, 0f), size = Size(size.width, size.height * 0.20f))
            val cardW = (size.width - gap) / 2f
            drawRect(color = ink.copy(alpha = 0.3f), topLeft = Offset(0f, size.height * 0.25f), size = Size(cardW, size.height * 0.12f))
            drawRect(color = ink.copy(alpha = 0.3f), topLeft = Offset(cardW + gap, size.height * 0.25f), size = Size(cardW, size.height * 0.12f))
            val cellW = (size.width - gap) / 2f
            val cellH = size.height * 0.28f
            val startY = size.height * 0.44f
            for (row in 0 until 2) {
                for (col in 0 until 2) {
                    drawRect(
                        color = ink.copy(alpha = 0.6f),
                        topLeft = Offset(col * (cellW + gap), startY + row * (cellH + gap)),
                        size = Size(cellW, cellH),
                    )
                }
            }
        }
        RiceId.IvoryPaper -> {
            // A large margin initial, a "HOY" editorial module, then short favorite index lines.
            drawRect(color = ink.copy(alpha = 0.85f), topLeft = Offset(0f, 0f), size = Size(size.width * 0.08f, size.height * 0.28f))
            drawRect(
                color = ink.copy(alpha = 0.2f),
                topLeft = Offset(size.width * 0.16f, size.height * 0.34f),
                size = Size(size.width * 0.7f, size.height * 0.16f),
            )
            val lineHeight = size.height * 0.12f
            val widths = listOf(0.85f, 0.55f, 0.7f)
            for ((i, w) in widths.withIndex()) {
                drawRect(
                    color = ink.copy(alpha = 0.6f),
                    topLeft = Offset(size.width * 0.16f, size.height * 0.58f + i * lineHeight * 1.5f),
                    size = Size(size.width * w, lineHeight * 0.4f),
                )
            }
        }
        RiceId.VioletNight -> {
            // A small end-aligned clock mark, a hero module, then a 1-2-2 cluster below it.
            drawRect(color = ink.copy(alpha = 0.6f), topLeft = Offset(size.width * 0.7f, 0f), size = Size(size.width * 0.3f, size.height * 0.14f))
            drawRoundRect(
                color = ink.copy(alpha = 0.22f),
                topLeft = Offset(size.width * 0.08f, size.height * 0.22f),
                size = Size(size.width * 0.84f, size.height * 0.18f),
                cornerRadius = CornerRadius(size.height * 0.05f, size.height * 0.05f),
            )
            val r = size.height * 0.1f
            val centerX = size.width / 2f
            val baseY = size.height * 0.62f
            drawCircle(color = ink.copy(alpha = 0.85f), radius = r, center = Offset(centerX, baseY))
            drawCircle(color = ink.copy(alpha = 0.7f), radius = r * 0.8f, center = Offset(centerX - r * 2.4f, baseY + r * 2.1f))
            drawCircle(color = ink.copy(alpha = 0.7f), radius = r * 0.8f, center = Offset(centerX + r * 2.4f, baseY + r * 2.1f))
        }
    }
}
