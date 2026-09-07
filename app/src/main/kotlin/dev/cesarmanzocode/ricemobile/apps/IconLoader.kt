package dev.cesarmanzocode.ricemobile.apps

import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.LruCache
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Contract §9: Monochrome/Ivory ask for a treated icon; Arctic/Ember/Violet keep full color and
 * only change the container/plate around it (that part is Compose-side, in `AppIcon`).
 */
enum class IconTreatment { Normal, Monochrome }

data class IconKey(
    val component: String,
    val userSerial: Long,
    val iconRevision: Long,
    val densityDpi: Int,
    val sizePx: Int,
    val treatment: IconTreatment = IconTreatment.Normal,
)

/**
 * Decode/raster/cache pipeline for launcher icons (contract §9). Never shares a mutable Drawable
 * across threads; at most two concurrent loads.
 *
 * [IconLoader.Result.isAlphaMask] is true only for a real API 33+ `AdaptiveIconDrawable.monochrome`
 * layer, rasterized as a white-on-transparent mask: the color it ends up as depends on which
 * rice/background it sits on, so tinting happens at Compose draw time (`AppIcon`), not baked in
 * here — one cached bitmap serves every rice that requests [IconTreatment.Monochrome]. When no
 * real monochrome layer exists, the fallback desaturates the *original* icon (`ColorMatrix`
 * saturation 0) so light/dark variation survives; that result is drawn as-is, never tinted, so a
 * multi-tone icon never collapses into a flat silhouette (contract §16 fallo previsible table).
 */
class IconLoader(
    private val launcherApps: LauncherApps,
    maxCacheBytes: Int,
) {
    data class Result(val bitmap: Bitmap, val isAlphaMask: Boolean)

    private val cache = object : LruCache<IconKey, Result>(maxCacheBytes) {
        override fun sizeOf(key: IconKey, value: Result): Int = value.bitmap.allocationByteCount
    }
    private val loadSemaphore = Semaphore(permits = 2)

    fun cached(key: IconKey): Result? = cache.get(key)

    suspend fun load(
        key: AppKey,
        iconRevision: Long,
        densityDpi: Int,
        sizePx: Int,
        treatment: IconTreatment = IconTreatment.Normal,
    ): Result? {
        val iconKey = IconKey(key.component, key.userSerial, iconRevision, densityDpi, sizePx, treatment)
        cache.get(iconKey)?.let { return it }
        return withContext(Dispatchers.IO) {
            loadSemaphore.withPermit {
                cache.get(iconKey)?.let { return@withPermit it }
                val result = decode(key, densityDpi, sizePx, treatment) ?: return@withPermit null
                cache.put(iconKey, result)
                result
            }
        }
    }

    private fun decode(key: AppKey, densityDpi: Int, sizePx: Int, treatment: IconTreatment): Result? {
        val component = android.content.ComponentName.unflattenFromString(key.component) ?: return null
        val info = runCatching {
            launcherApps.getActivityList(component.packageName, Process.myUserHandle())
                .firstOrNull { it.componentName == component }
        }.getOrNull() ?: return null
        val drawable = runCatching { info.getIcon(densityDpi) }.getOrNull() ?: return null
        return rasterize(drawable, sizePx, treatment)
    }

    private fun rasterize(source: Drawable, sizePx: Int, treatment: IconTreatment): Result? {
        val monoLayer = if (treatment == IconTreatment.Monochrome &&
            Build.VERSION.SDK_INT >= 33 &&
            source is AdaptiveIconDrawable
        ) {
            source.monochrome
        } else {
            null
        }

        if (monoLayer != null) {
            val mask = monoLayer.mutate()
            mask.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            val bitmap = createBitmap(sizePx, sizePx)
            mask.setBounds(0, 0, sizePx, sizePx)
            mask.draw(Canvas(bitmap))
            return Result(bitmap, isAlphaMask = true)
        }

        val drawable = source.mutate()
        if (treatment == IconTreatment.Monochrome) {
            drawable.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }

        val isAdaptive = source is AdaptiveIconDrawable
        val marginFraction = if (isAdaptive) 0f else LEGACY_MARGIN
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: sizePx
        val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: sizePx
        val available = sizePx * (1f - marginFraction * 2f)
        val scale = minOf(available / intrinsicWidth, available / intrinsicHeight)
        val drawWidth = (intrinsicWidth * scale).toInt()
        val drawHeight = (intrinsicHeight * scale).toInt()
        val left = (sizePx - drawWidth) / 2
        val top = (sizePx - drawHeight) / 2
        drawable.setBounds(left, top, left + drawWidth, top + drawHeight)
        drawable.draw(canvas)
        return Result(bitmap, isAlphaMask = false)
    }

    private companion object {
        /** Contract §9: "Legacy usa fit centrado con margen 12%; no estirar iconos rectangulares." */
        const val LEGACY_MARGIN = 0.12f
    }
}
