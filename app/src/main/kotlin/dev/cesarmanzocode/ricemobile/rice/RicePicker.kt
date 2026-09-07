package dev.cesarmanzocode.ricemobile.rice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cesarmanzocode.ricemobile.R

private val PICKER_BACKGROUND = Color(0xFF141414)
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
    Column(
        modifier = modifier.fillMaxSize().background(PICKER_BACKGROUND).safeDrawingPadding().padding(16.dp),
    ) {
        Text(text = stringResource(R.string.rice_picker_title), color = PICKER_CHROME, fontWeight = FontWeight.Medium, fontSize = 18.sp)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(RiceRegistry.all, key = { it.id }) { rice ->
                RiceOption(rice = rice, selected = rice.id == current, onClick = { onSelect(rice.id) })
            }
        }
    }
}

@Composable
private fun RiceOption(rice: Rice, selected: Boolean, onClick: () -> Unit) {
    val preview = previewOf(rice.id)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(preview.background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) preview.ink else preview.ink.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
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
            // A big clock block, then a linear stack of favorite rows.
            drawRect(color = ink.copy(alpha = 0.9f), topLeft = Offset(0f, 0f), size = Size(size.width * 0.5f, size.height * 0.32f))
            val rowHeight = size.height * 0.14f
            for (i in 0 until 3) {
                val top = size.height * 0.55f + i * rowHeight * 1.25f
                drawRect(color = ink.copy(alpha = 0.5f), topLeft = Offset(0f, top), size = Size(size.width, rowHeight * 0.7f))
            }
        }
        RiceId.ArcticGlass -> {
            // A small centered clock pill, then a floating rounded dock near the bottom.
            drawRoundRect(
                color = ink.copy(alpha = 0.7f),
                topLeft = Offset(size.width * 0.38f, 0f),
                size = Size(size.width * 0.24f, size.height * 0.16f),
                cornerRadius = CornerRadius(size.height * 0.08f, size.height * 0.08f),
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
            // One full-width block, then a 2x2 grid beneath it (contract §18.4's 1+2x2 matrix).
            val gap = size.width * 0.03f
            drawRect(color = ink.copy(alpha = 0.85f), topLeft = Offset(0f, 0f), size = Size(size.width, size.height * 0.28f))
            val cellW = (size.width - gap) / 2f
            val cellH = size.height * 0.3f
            val startY = size.height * 0.42f
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
            // A large margin initial, then short editorial rule lines (an index, not a grid).
            drawRect(color = ink.copy(alpha = 0.85f), topLeft = Offset(0f, 0f), size = Size(size.width * 0.08f, size.height * 0.28f))
            val lineHeight = size.height * 0.14f
            val widths = listOf(0.85f, 0.55f, 0.7f)
            for ((i, w) in widths.withIndex()) {
                drawRect(
                    color = ink.copy(alpha = 0.6f),
                    topLeft = Offset(size.width * 0.16f, i * lineHeight * 1.6f),
                    size = Size(size.width * w, lineHeight * 0.4f),
                )
            }
        }
        RiceId.VioletNight -> {
            // A small end-aligned clock mark, then a 1-2-2 cluster of circles below it.
            drawRect(color = ink.copy(alpha = 0.6f), topLeft = Offset(size.width * 0.7f, 0f), size = Size(size.width * 0.3f, size.height * 0.14f))
            val r = size.height * 0.11f
            val centerX = size.width / 2f
            val baseY = size.height * 0.55f
            drawCircle(color = ink.copy(alpha = 0.85f), radius = r, center = Offset(centerX, baseY))
            drawCircle(color = ink.copy(alpha = 0.7f), radius = r * 0.8f, center = Offset(centerX - r * 2.4f, baseY + r * 2.1f))
            drawCircle(color = ink.copy(alpha = 0.7f), radius = r * 0.8f, center = Offset(centerX + r * 2.4f, baseY + r * 2.1f))
        }
    }
}
