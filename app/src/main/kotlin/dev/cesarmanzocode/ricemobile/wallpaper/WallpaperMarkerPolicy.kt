package dev.cesarmanzocode.ricemobile.wallpaper

import dev.cesarmanzocode.ricemobile.rice.RiceId

/**
 * Pure rules around the `"id:assetRevision"` marker (contract §8), split out so they are
 * directly unit-testable without DataStore/Android: the marker is only ever committed for the
 * rice that is still persisted at commit time, and a foreground check compares the marker
 * against the currently persisted rice's expected value to decide whether to retry.
 */
object WallpaperMarkerPolicy {

    fun marker(riceId: RiceId, spec: WallpaperSpec): String = "${riceId.persisted}:${spec.assetRevision}"

    /** A wallpaper apply that completed for [completedRice] may only be persisted as the applied
     * marker while [persistedRice] still matches it; otherwise a later selection already won. */
    fun shouldPersist(persistedRice: RiceId, completedRice: RiceId): Boolean = persistedRice == completedRice

    /** True when [appliedMarker] does not match [riceId]/[spec] — e.g. the process died between
     * the rice commit and a successful wallpaper write, or no write ever succeeded. */
    fun needsRetry(appliedMarker: String?, riceId: RiceId, spec: WallpaperSpec): Boolean =
        appliedMarker != marker(riceId, spec)
}
