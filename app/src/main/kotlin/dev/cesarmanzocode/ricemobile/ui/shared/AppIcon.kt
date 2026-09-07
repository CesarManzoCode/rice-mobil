package dev.cesarmanzocode.ricemobile.ui.shared

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.IconKey
import dev.cesarmanzocode.ricemobile.apps.IconLoader

/**
 * Provided once by the host (contract §6.1: `Rice.Home`/`Drawer` take only models/actions, no
 * services), so every rice can render icons without threading an [IconLoader] through models.
 */
val LocalIconLoader = compositionLocalOf<IconLoader> {
    error("LocalIconLoader not provided: wrap content in LauncherHost")
}

/**
 * Decorative when the label is already visible next to it (contentDescription=null avoids
 * duplicate TalkBack reads, contract §18.1); loads through [IconLoader] off the main thread
 * and falls back to the label's initial while loading or on failure (contract §9).
 */
@Composable
fun AppIcon(
    entry: AppEntry,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val iconLoader = LocalIconLoader.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val sizePx = with(density) { size.roundToPx() }
    val densityDpi = context.resources.displayMetrics.densityDpi
    val cacheKey = IconKey(entry.key.component, entry.key.userSerial, entry.iconRevision, densityDpi, sizePx)

    var bitmap by remember(cacheKey) { mutableStateOf<Bitmap?>(iconLoader.cached(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (bitmap == null) {
            bitmap = iconLoader.load(entry.key, entry.iconRevision, densityDpi, sizePx)
        }
    }

    Box(
        modifier = modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(bitmap = loaded.asImageBitmap(), contentDescription = null)
        } else {
            Text(text = entry.label.trim().take(1).ifEmpty { "?" }.uppercase())
        }
    }
}
