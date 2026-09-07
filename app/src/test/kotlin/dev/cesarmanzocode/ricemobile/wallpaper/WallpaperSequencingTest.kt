package dev.cesarmanzocode.ricemobile.wallpaper

import dev.cesarmanzocode.ricemobile.rice.RiceId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun spec(id: String) = WallpaperSpec(assetPath = id, assetRevision = 1, fallbackColorArgb = 0L)

class WallpaperSequencingTest {

    @Test
    fun `a rapid A-B-C selection finishes writing C, possibly skipping B`() = runTest {
        val written = mutableListOf<String>()
        val applied = mutableListOf<Pair<RiceId, String>>()
        val releaseA = CompletableDeferred<Unit>()

        val writer = WallpaperWriter { requestedSpec ->
            if (requestedSpec.assetPath == "A") releaseA.await()
            written.add(requestedSpec.assetPath)
            WallpaperApplyResult.Applied(1)
        }
        val controller = WallpaperController(
            writer = writer,
            onApplied = { riceId, marker -> applied.add(riceId to marker) },
            scope = backgroundScope,
        )

        controller.request(RiceId.Monochrome, spec("A"))
        runCurrent() // consumer picks up A and blocks inside writer.write

        controller.request(RiceId.ArcticGlass, spec("B"))
        controller.request(RiceId.EmberForge, spec("C")) // conflates over B: never written
        runCurrent() // nothing runnable yet: consumer is still parked on releaseA.await()

        releaseA.complete(Unit)
        runCurrent() // A's write completes, then the consumer drains straight to C

        assertEquals(listOf("A", "C"), written)
        assertEquals(
            listOf(RiceId.Monochrome to "monochrome:1", RiceId.EmberForge to "ember_forge:1"),
            applied,
        )
    }

    @Test
    fun `a failed write never invokes onApplied`() = runTest {
        var appliedCalls = 0
        val writer = WallpaperWriter { WallpaperApplyResult.Failed }
        val controller = WallpaperController(
            writer = writer,
            onApplied = { _, _ -> appliedCalls++ },
            scope = backgroundScope,
        )

        controller.request(RiceId.Monochrome, spec("A"))
        runCurrent()

        assertEquals(0, appliedCalls)
        assertEquals(WallpaperControllerStatus.Failed(RiceId.Monochrome), controller.status.value)
    }

    @Test
    fun `a blocked write never invokes onApplied either`() = runTest {
        var appliedCalls = 0
        val writer = WallpaperWriter { WallpaperApplyResult.Blocked }
        val controller = WallpaperController(
            writer = writer,
            onApplied = { _, _ -> appliedCalls++ },
            scope = backgroundScope,
        )

        controller.request(RiceId.Monochrome, spec("A"))
        runCurrent()

        assertEquals(0, appliedCalls)
    }

    @Test
    fun `marker is only persisted while the completing rice is still the persisted one`() {
        assertTrue(WallpaperMarkerPolicy.shouldPersist(RiceId.Monochrome, RiceId.Monochrome))
        assertFalse(WallpaperMarkerPolicy.shouldPersist(RiceId.VioletNight, RiceId.Monochrome))
    }

    @Test
    fun `a stale or missing marker after simulated process death requires a retry`() {
        val current = RiceId.EmberForge
        val currentSpec = spec("ember")

        // Simulated death before the wallpaper write ever committed a marker.
        assertTrue(WallpaperMarkerPolicy.needsRetry(appliedMarker = null, current, currentSpec))

        // Simulated death with an old marker left over from a previous rice.
        assertTrue(WallpaperMarkerPolicy.needsRetry(appliedMarker = "monochrome:1", current, currentSpec))

        // Marker already matches: no retry needed.
        val matchingMarker = WallpaperMarkerPolicy.marker(current, currentSpec)
        assertFalse(WallpaperMarkerPolicy.needsRetry(matchingMarker, current, currentSpec))
    }
}
