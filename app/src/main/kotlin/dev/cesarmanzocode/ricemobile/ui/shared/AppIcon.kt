package dev.cesarmanzocode.ricemobile.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.IconKey
import dev.cesarmanzocode.ricemobile.apps.IconLoader
import dev.cesarmanzocode.ricemobile.apps.IconTreatment

/**
 * Provided once by the host (contract §6.1: `Rice.Home`/`Drawer` take only models/actions, no
 * services), so every rice can render icons without threading an [IconLoader] through models.
 */
val LocalIconLoader = compositionLocalOf<IconLoader> {
    error("LocalIconLoader not provided: wrap content in LauncherHost")
}

/**
 * Decorative when the label is already visible next to it (contentDescription=null avoids
 * duplicate TalkBack reads, contract §18.1); loads through [IconLoader] off the main thread and
 * falls back to the label's initial while loading or on failure (contract §9). [treatment] and
 * [plateShape]/[plateColor] are how each rice owns its own icon container per §18/§9 — Arctic's
 * circular translucent plate, Ember's cut-corner block, Ivory/Monochrome's monochrome layer —
 * without a shared visual widget dictating the look. [tint] only applies when the loaded result
 * is a true alpha mask (a real API 33+ monochrome layer); a desaturated fallback keeps its own
 * luminance and is never flattened to one color.
 */
@Composable
fun AppIcon(
    entry: AppEntry,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    treatment: IconTreatment = IconTreatment.Normal,
    tint: Color = Color.Unspecified,
    plateShape: Shape = CircleShape,
    plateColor: Color = Color.Unspecified,
) {
    val iconLoader = LocalIconLoader.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val sizePx = with(density) { size.roundToPx() }
    val densityDpi = context.resources.displayMetrics.densityDpi
    val cacheKey = IconKey(entry.key.component, entry.key.userSerial, entry.iconRevision, densityDpi, sizePx, treatment)

    var result by remember(cacheKey) { mutableStateOf(iconLoader.cached(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (result == null) {
            result = iconLoader.load(entry.key, entry.iconRevision, densityDpi, sizePx, treatment)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(plateShape)
            .background(plateColor.takeOrElse { MaterialTheme.colorScheme.surfaceVariant }),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = result
        if (loaded != null) {
            Image(
                bitmap = loaded.bitmap.asImageBitmap(),
                contentDescription = null,
                colorFilter = if (loaded.isAlphaMask) {
                    ColorFilter.tint(tint.takeOrElse { Color.Gray })
                } else {
                    null
                },
            )
        } else {
            Text(text = entry.label.trim().take(1).ifEmpty { "?" }.uppercase())
        }
    }
}
