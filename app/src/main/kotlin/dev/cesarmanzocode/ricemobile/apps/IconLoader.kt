package dev.cesarmanzocode.ricemobile.apps

import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.LruCache
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

data class IconKey(
    val component: String,
    val userSerial: Long,
    val iconRevision: Long,
    val densityDpi: Int,
    val sizePx: Int,
)

/**
 * Decode/raster/cache pipeline for launcher icons (contract §9, basic S1 shape: no
 * monochrome/legacy treatment yet, that lands in S3). Never shares a mutable Drawable
 * across threads; at most two concurrent loads.
 */
class IconLoader(
    private val launcherApps: LauncherApps,
    maxCacheBytes: Int,
) {
    private val cache = object : LruCache<IconKey, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: IconKey, value: Bitmap): Int = value.allocationByteCount
    }
    private val loadSemaphore = Semaphore(permits = 2)

    fun cached(key: IconKey): Bitmap? = cache.get(key)

    suspend fun load(key: AppKey, iconRevision: Long, densityDpi: Int, sizePx: Int): Bitmap? {
        val iconKey = IconKey(key.component, key.userSerial, iconRevision, densityDpi, sizePx)
        cache.get(iconKey)?.let { return it }
        return withContext(Dispatchers.IO) {
            loadSemaphore.withPermit {
                cache.get(iconKey)?.let { return@withPermit it }
                val bitmap = decode(key, densityDpi, sizePx) ?: return@withPermit null
                cache.put(iconKey, bitmap)
                bitmap
            }
        }
    }

    private fun decode(key: AppKey, densityDpi: Int, sizePx: Int): Bitmap? {
        val component = android.content.ComponentName.unflattenFromString(key.component) ?: return null
        val user = runCatching {
            launcherApps.getActivityList(component.packageName, Process.myUserHandle())
                .firstOrNull { it.componentName == component }
        }.getOrNull() ?: return null
        val drawable = runCatching { user.getIcon(densityDpi) }.getOrNull() ?: return null
        return rasterize(drawable, sizePx)
    }

    private fun rasterize(source: Drawable, sizePx: Int): Bitmap? {
        val drawable = source.mutate()
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: sizePx
        val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: sizePx
        val scale = minOf(sizePx.toFloat() / intrinsicWidth, sizePx.toFloat() / intrinsicHeight)
        val drawWidth = (intrinsicWidth * scale).toInt()
        val drawHeight = (intrinsicHeight * scale).toInt()
        val left = (sizePx - drawWidth) / 2
        val top = (sizePx - drawHeight) / 2
        drawable.setBounds(left, top, left + drawWidth, top + drawHeight)
        drawable.draw(canvas)
        return bitmap
    }
}
