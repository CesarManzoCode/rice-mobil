package dev.cesarmanzocode.ricemobile.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bounded to two decoded bitmaps at a time (contract §19: "transición máximo 2 activos"),
 * downsampled to the viewport rather than the full 1440x3200 asset, so a rice switch never
 * accumulates ARGB buffers. Recency-ordered eviction: the oldest entry is dropped once a third
 * distinct key is requested.
 */
private object WallpaperBitmapCache {
    private const val MAX_ENTRIES = 2
    private val entries = object : LinkedHashMap<String, Bitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
            size > MAX_ENTRIES
    }

    @Synchronized
    fun get(key: String): Bitmap? = entries[key]

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        entries[key] = bitmap
    }
}

private fun decodeSampled(context: Context, assetPath: String, targetWidthPx: Int, targetHeightPx: Int): Bitmap? =
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidthPx && bounds.outHeight / (sample * 2) >= targetHeightPx) {
            sample *= 2
        }
        context.assets.open(assetPath).use { stream ->
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

/**
 * Paints a rice's own packaged wallpaper as an internal background (contract §8: "Home pinta ese
 * mismo asset como fondo interno"). Decodes off the main thread, downsampled to the viewport;
 * [WallpaperSpec.fallbackColorArgb] paints immediately so there is never a blank frame while it
 * decodes or if the asset ever fails to load (contract invariant 7: a failed asset never blocks Home).
 */
@Composable
fun WallpaperBackdrop(
    spec: WallpaperSpec,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val fallback = Color(spec.fallbackColorArgb)
    val cacheKey = "${spec.assetPath}:${spec.assetRevision}"
    var image by remember(cacheKey) {
        mutableStateOf<ImageBitmap?>(WallpaperBitmapCache.get(cacheKey)?.asImageBitmap())
    }

    LaunchedEffect(cacheKey) {
        if (image != null) return@LaunchedEffect
        val targetWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        val targetHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
        val decoded = withContext(Dispatchers.IO) {
            decodeSampled(context, spec.assetPath, targetWidthPx, targetHeightPx)
        }
        if (decoded != null) {
            WallpaperBitmapCache.put(cacheKey, decoded)
            image = decoded.asImageBitmap()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(fallback)) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}
