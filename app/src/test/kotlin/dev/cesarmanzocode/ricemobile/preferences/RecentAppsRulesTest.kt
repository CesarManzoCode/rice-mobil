package dev.cesarmanzocode.ricemobile.preferences

import dev.cesarmanzocode.ricemobile.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentAppsRulesTest {

    private fun key(component: String, serial: Long = 0L) = AppKey(serial, component)

    @Test
    fun `opening an app moves it to the front`() {
        val a = key("com.a/.Main")
        val b = key("com.b/.Main")
        var recents = listOf(a, b)
        recents = RecentAppsRules.recordOpen(recents, b)
        assertEquals(listOf(b, a), recents)
    }

    @Test
    fun `reopening the same app does not duplicate it`() {
        val a = key("com.a/.Main")
        var recents = emptyList<AppKey>()
        recents = RecentAppsRules.recordOpen(recents, a)
        recents = RecentAppsRules.recordOpen(recents, a)
        assertEquals(listOf(a), recents)
    }

    @Test
    fun `history never grows past the cap`() {
        var recents = emptyList<AppKey>()
        for (i in 1..(RecentAppsCodec.MAX_RECENTS + 5)) {
            recents = RecentAppsRules.recordOpen(recents, key("com.app$i/.Main"))
        }
        assertEquals(RecentAppsCodec.MAX_RECENTS, recents.size)
        // Most-recent-first: the last opened app is at the front, the oldest ones fell off.
        assertEquals(key("com.app${RecentAppsCodec.MAX_RECENTS + 5}/.Main"), recents.first())
    }
}
