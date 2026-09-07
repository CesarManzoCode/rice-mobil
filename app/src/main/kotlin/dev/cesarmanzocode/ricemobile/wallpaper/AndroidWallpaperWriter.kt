package dev.cesarmanzocode.ricemobile.wallpaper

import android.app.WallpaperManager
import android.content.Context
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production [WallpaperWriter]: writes only `FLAG_SYSTEM`, never `FLAG_LOCK` (contract §8).
 * Reads the packaged asset from `assets/`, never the user's current wallpaper.
 */
class AndroidWallpaperWriter(
    private val context: Context,
    private val manager: WallpaperManager,
) : WallpaperWriter {
    override suspend fun write(spec: WallpaperSpec): WallpaperApplyResult = withContext(Dispatchers.IO) {
        if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) {
            return@withContext WallpaperApplyResult.Blocked
        }
        try {
            val wallpaperId = context.assets.open(spec.assetPath).use { input ->
                manager.setStream(input, null, false, WallpaperManager.FLAG_SYSTEM)
            }
            if (wallpaperId > 0) WallpaperApplyResult.Applied(wallpaperId) else WallpaperApplyResult.Failed
        } catch (_: IOException) {
            WallpaperApplyResult.Failed
        } catch (_: SecurityException) {
            WallpaperApplyResult.Blocked
        }
    }
}
