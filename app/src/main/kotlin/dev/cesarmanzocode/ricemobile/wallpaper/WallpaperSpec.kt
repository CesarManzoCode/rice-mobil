package dev.cesarmanzocode.ricemobile.wallpaper

/**
 * A rice's packaged wallpaper (contract §8). [assetPath] is relative to `assets/`.
 * [assetRevision] changes only when the asset bytes change; the applied marker is
 * `"<riceId>:<assetRevision>"`. [fallbackColorArgb] paints Home's own background before/while
 * the asset decodes, and is a plain ARGB Long rather than a Compose Color so this type has no
 * UI dependency and can be used from [dev.cesarmanzocode.ricemobile.wallpaper.WallpaperController]
 * tests without Android.
 */
data class WallpaperSpec(
    val assetPath: String,
    val assetRevision: Int,
    val fallbackColorArgb: Long,
)
