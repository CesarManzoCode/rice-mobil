package dev.cesarmanzocode.ricemobile.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.cesarmanzocode.ricemobile.ui.shared.LocalReducedMotion
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

private fun cacheKeyOf(spec: WallpaperSpec) = "${spec.assetPath}:${spec.assetRevision}"

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
 * UX overhaul §18: decodes [spec]'s asset into the shared cache ahead of time, off the main
 * thread — call this the instant a rice is *selected* (before the picker's own exit motion even
 * starts) so that by the time the new [WallpaperBackdrop] composes, `WallpaperBitmapCache.get`
 * already has it and paints the real wallpaper on the very first frame instead of a fallback
 * color. Best-effort: if the transition completes before decode does, [WallpaperBackdrop]'s own
 * fallback-to-bitmap crossfade below still hides the gap — this is never relied on as a guarantee.
 */
suspend fun prefetchWallpaper(context: Context, spec: WallpaperSpec, targetSize: IntSize) {
    val cacheKey = cacheKeyOf(spec)
    if (WallpaperBitmapCache.get(cacheKey) != null) return
    val decoded = withContext(Dispatchers.IO) {
        decodeSampled(context, spec.assetPath, targetSize.width, targetSize.height)
    }
    if (decoded != null) WallpaperBitmapCache.put(cacheKey, decoded)
}

private data class WallpaperLayer(val key: String, val fallbackColorArgb: Int, val image: ImageBitmap?)

/**
 * Paints a rice's own packaged wallpaper as an internal background (contract §8: "Home pinta ese
 * mismo asset como fondo interno"). Decodes off the main thread, downsampled to the viewport;
 * [WallpaperSpec.fallbackColorArgb] paints immediately so there is never a blank frame while it
 * decodes or if the asset ever fails to load (contract invariant 7: a failed asset never blocks
 * Home). UX overhaul §18: once the real bitmap is ready, [Crossfade] hands off from the fallback
 * color to it instead of popping in — the one flash this composable can control on its own
 * (the *other* half, a rice switch showing this fallback while the new asset decodes, is what
 * [prefetchWallpaper] exists to avoid happening in the first place).
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
    val reducedMotion = LocalReducedMotion.current
    val cacheKey = cacheKeyOf(spec)
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

    Crossfade(
        targetState = WallpaperLayer(cacheKey, spec.fallbackColorArgb, image),
        animationSpec = if (reducedMotion) snap() else tween(WALLPAPER_FADE_MS),
        label = "wallpaper-fallback-to-bitmap",
    ) { layer ->
        Box(modifier = modifier.fillMaxSize().background(Color(layer.fallbackColorArgb))) {
            layer.image?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            }
        }
    }
}

private const val WALLPAPER_FADE_MS = 180
