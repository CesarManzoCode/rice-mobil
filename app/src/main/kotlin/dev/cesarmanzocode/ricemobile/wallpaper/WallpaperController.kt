package dev.cesarmanzocode.ricemobile.wallpaper

import dev.cesarmanzocode.ricemobile.rice.RiceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WallpaperApplyResult {
    data class Applied(val wallpaperId: Int) : WallpaperApplyResult
    data object Blocked : WallpaperApplyResult
    data object Failed : WallpaperApplyResult
}

/** The actual write, isolated behind an interface so [WallpaperController] is a pure JVM unit
 * under test: production wires [dev.cesarmanzocode.ricemobile.wallpaper.AndroidWallpaperWriter]. */
fun interface WallpaperWriter {
    suspend fun write(spec: WallpaperSpec): WallpaperApplyResult
}

sealed interface WallpaperControllerStatus {
    data object Idle : WallpaperControllerStatus
    data class Applying(val riceId: RiceId) : WallpaperControllerStatus
    data class Failed(val riceId: RiceId) : WallpaperControllerStatus
}

/**
 * Single serial writer for the system wallpaper (contract §8). Desired selections go through a
 * conflated channel: a rapid A -> B -> C selection can skip B but always finishes writing C,
 * never in parallel. [onApplied] persists the `"id:assetRevision"` marker; it is only invoked
 * after a successful write and receives [RiceId] so the caller can refuse to commit the marker
 * once that rice is no longer the persisted one.
 */
class WallpaperController(
    private val writer: WallpaperWriter,
    private val onApplied: suspend (RiceId, String) -> Unit,
    scope: CoroutineScope,
) {
    private val _status = MutableStateFlow<WallpaperControllerStatus>(WallpaperControllerStatus.Idle)
    val status: StateFlow<WallpaperControllerStatus> = _status.asStateFlow()

    private val desired = Channel<Pair<RiceId, WallpaperSpec>>(Channel.CONFLATED)

    @Suppress("unused")
    private val job: Job = scope.launch { consume() }

    fun request(riceId: RiceId, spec: WallpaperSpec) {
        desired.trySend(riceId to spec)
    }

    private suspend fun consume() {
        for ((riceId, spec) in desired) {
            _status.value = WallpaperControllerStatus.Applying(riceId)
            when (val result = writer.write(spec)) {
                is WallpaperApplyResult.Applied -> {
                    onApplied(riceId, WallpaperMarkerPolicy.marker(riceId, spec))
                    _status.value = WallpaperControllerStatus.Idle
                }
                WallpaperApplyResult.Blocked, WallpaperApplyResult.Failed -> {
                    _status.value = WallpaperControllerStatus.Failed(riceId)
                }
            }
        }
    }
}
