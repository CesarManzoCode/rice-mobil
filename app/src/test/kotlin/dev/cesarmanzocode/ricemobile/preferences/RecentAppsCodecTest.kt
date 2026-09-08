package dev.cesarmanzocode.ricemobile.preferences

import dev.cesarmanzocode.ricemobile.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentAppsCodecTest {

    @Test
    fun `round trips a normal list in order`() {
        val recents = listOf(AppKey(0L, "com.a/.Main"), AppKey(0L, "com.b/.Main"))
        assertEquals(recents, RecentAppsCodec.decode(RecentAppsCodec.encode(recents)))
    }

    @Test
    fun `missing payload decodes to an empty list`() {
        assertEquals(emptyList<AppKey>(), RecentAppsCodec.decode(null))
        assertEquals(emptyList<AppKey>(), RecentAppsCodec.decode(""))
    }

    @Test
    fun `more entries than the cap are clamped on decode`() {
        val keys = (1..(RecentAppsCodec.MAX_RECENTS + 3)).map { AppKey(0L, "com.app$it/.Main") }
        val payload = keys.joinToString("\n") { "${it.userSerial}\t${it.component}" }
        assertEquals(keys.take(RecentAppsCodec.MAX_RECENTS), RecentAppsCodec.decode(payload))
    }
}
