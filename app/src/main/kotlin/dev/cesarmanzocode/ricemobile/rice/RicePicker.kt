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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.R

private val PICKER_BACKGROUND = Color(0xFF141414)
private val PICKER_INK = Color(0xFFF5F5F0)
private val PICKER_ACCENT = Color(0xFF2A2A2A)

/**
 * Exactly five options (contract §6.1/§7): the thumbnail draws each rice's real structure
 * (dock, matrix, index list, cluster...), not five recolored rectangles.
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
        Text(text = stringResource(R.string.rice_picker_title), color = PICKER_INK)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(RiceRegistry.all, key = { it.id }) { rice ->
                RiceOption(
                    rice = rice,
                    selected = rice.id == current,
                    onClick = { onSelect(rice.id) },
                )
            }
        }
    }
}

@Composable
private fun RiceOption(rice: Rice, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PICKER_ACCENT)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = PICKER_INK,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.6f)) {
            drawStructure(rice.id, size, PICKER_INK)
        }
        Text(text = riceDisplayNameRes(rice.id), color = PICKER_INK, modifier = Modifier.padding(top = 8.dp))
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
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStructure(id: RiceId, size: Size, ink: Color) {
    when (id) {
        RiceId.Monochrome -> {
            // Vertical stack: a title bar and a linear list of rows.
            val rowHeight = size.height / 5f
            for (i in 0 until 4) {
                drawRect(
                    color = ink.copy(alpha = if (i == 0) 0.9f else 0.35f),
                    topLeft = Offset(0f, (i + 1) * rowHeight),
                    size = Size(size.width, rowHeight * 0.7f),
                )
            }
        }
        RiceId.ArcticGlass -> {
            // Floating horizontal dock near the bottom.
            val dockHeight = size.height * 0.22f
            drawRoundRect(
                color = ink.copy(alpha = 0.85f),
                topLeft = Offset(size.width * 0.15f, size.height - dockHeight - size.height * 0.08f),
                size = Size(size.width * 0.7f, dockHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(dockHeight / 2f, dockHeight / 2f),
            )
        }
        RiceId.EmberForge -> {
            // A compact matrix/block grid.
            val cell = size.minDimension / 4f
            val cols = 3
            val rows = 2
            val startX = (size.width - cols * cell) / 2f
            val startY = size.height * 0.15f
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    drawRect(
                        color = ink.copy(alpha = 0.8f),
                        topLeft = Offset(startX + col * cell * 1.15f, startY + row * cell * 1.15f),
                        size = Size(cell, cell),
                    )
                }
            }
        }
        RiceId.IvoryPaper -> {
            // Textual index: short rule lines of varying width, editorial.
            val lineHeight = size.height / 6f
            val widths = listOf(0.9f, 0.6f, 0.75f, 0.5f)
            for ((i, w) in widths.withIndex()) {
                drawRect(
                    color = ink.copy(alpha = 0.8f),
                    topLeft = Offset(0f, (i + 1) * lineHeight),
                    size = Size(size.width * w, lineHeight * 0.35f),
                )
            }
        }
        RiceId.VioletNight -> {
            // 1-2-2 cluster of circles.
            val r = size.minDimension / 8f
            val centerX = size.width / 2f
            drawCircle(color = ink.copy(alpha = 0.85f), radius = r, center = Offset(centerX, r * 1.4f))
            drawCircle(
                color = ink.copy(alpha = 0.7f),
                radius = r,
                center = Offset(centerX - r * 2.2f, r * 3.6f),
            )
            drawCircle(
                color = ink.copy(alpha = 0.7f),
                radius = r,
                center = Offset(centerX + r * 2.2f, r * 3.6f),
            )
            drawCircle(
                color = ink.copy(alpha = 0.6f),
                radius = r,
                center = Offset(centerX - r * 2.2f, r * 5.8f),
            )
            drawCircle(
                color = ink.copy(alpha = 0.6f),
                radius = r,
                center = Offset(centerX + r * 2.2f, r * 5.8f),
            )
        }
    }
}
