package dev.cesarmanzocode.ricemobile.preferences

import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.rice.FavoriteSlot
import dev.cesarmanzocode.ricemobile.rice.buildFavoriteSlots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoriteRulesTest {

    private fun key(component: String, serial: Long = 0L) = AppKey(serial, component)

    @Test
    fun `adding builds order of addition`() {
        var favorites = emptyList<AppKey>()
        val a = key("com.a/.Main")
        val b = key("com.b/.Main")
        favorites = FavoriteRules.toggle(favorites, a).first
        favorites = FavoriteRules.toggle(favorites, b).first
        assertEquals(listOf(a, b), favorites)
    }

    @Test
    fun `a sixth addition is rejected without touching the list`() {
        var favorites = (1..5).map { key("com.app$it/.Main") }
        val sixth = key("com.app6/.Main")
        val (next, result) = FavoriteRules.toggle(favorites, sixth)
        assertEquals(favorites, next)
        assertEquals(FavoriteToggleResult.LimitReached, result)
    }

    @Test
    fun `removing then re-adding moves the key to the end`() {
        val a = key("com.a/.Main")
        val b = key("com.b/.Main")
        var favorites = listOf(a, b)
        favorites = FavoriteRules.toggle(favorites, a).first // remove a
        favorites = FavoriteRules.toggle(favorites, a).first // re-add a
        assertEquals(listOf(b, a), favorites)
    }

    @Test
    fun `two aliases of the same package are distinct favorites`() {
        val alias1 = key("com.app/.MainAlias1")
        val alias2 = key("com.app/.MainAlias2")
        var favorites = emptyList<AppKey>()
        favorites = FavoriteRules.toggle(favorites, alias1).first
        favorites = FavoriteRules.toggle(favorites, alias2).first
        assertEquals(listOf(alias1, alias2), favorites)
    }

    @Test
    fun `toggle on an existing key removes it and reports Removed`() {
        val a = key("com.a/.Main")
        val (next, result) = FavoriteRules.toggle(listOf(a), a)
        assertEquals(emptyList<AppKey>(), next)
        assertEquals(FavoriteToggleResult.Removed, result)
    }

    @Test
    fun `explicit remove is a no-op when the key is absent`() {
        val a = key("com.a/.Main")
        val next = FavoriteRules.remove(emptyList(), a)
        assertEquals(emptyList<AppKey>(), next)
    }

    @Test
    fun `an absent favorite becomes an unavailable slot, never dropped`() {
        val present = key("com.a/.Main")
        val uninstalled = key("com.b/.Main")
        val favorites = listOf(present, uninstalled)
        val catalog = listOf(entry(present, "A"))

        val slots = buildFavoriteSlots(favorites, catalog)

        assertEquals(2, slots.size)
        assertEquals(FavoriteSlot(present, catalog[0]), slots[0])
        assertEquals(uninstalled, slots[1].key)
        assertNull(slots[1].app)
    }

    @Test
    fun `an empty or loading catalog snapshot never drops favorites`() {
        val favorites = listOf(key("com.a/.Main"), key("com.b/.Main"))
        val slots = buildFavoriteSlots(favorites, emptyList())
        assertEquals(favorites, slots.map { it.key })
        assertEquals(listOf(null, null), slots.map { it.app })
    }

    private fun entry(key: AppKey, label: String) = AppEntry(
        key = key,
        packageName = key.component.substringBefore('/'),
        label = label,
        normalizedLabel = label.lowercase(),
        normalizedPackage = key.component.substringBefore('/').lowercase(),
        iconRevision = 0L,
    )
}
