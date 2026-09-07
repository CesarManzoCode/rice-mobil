package dev.cesarmanzocode.ricemobile.preferences

import dev.cesarmanzocode.ricemobile.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteCodecTest {

    @Test
    fun `round trips a normal list in order`() {
        val favorites = listOf(
            AppKey(0L, "com.a/.Main"),
            AppKey(0L, "com.b/.Main"),
            AppKey(10L, "com.c/.Main"),
        )
        val encoded = FavoriteCodec.encode(favorites)
        assertEquals(favorites, FavoriteCodec.decode(encoded))
    }

    @Test
    fun `missing payload decodes to an empty list`() {
        assertEquals(emptyList<AppKey>(), FavoriteCodec.decode(null))
        assertEquals(emptyList<AppKey>(), FavoriteCodec.decode(""))
    }

    @Test
    fun `a line with an extra tab is dropped, not fatal`() {
        val payload = "0\tcom.a/.Main\n0\tcom.b/.Main\tExtra"
        assertEquals(listOf(AppKey(0L, "com.a/.Main")), FavoriteCodec.decode(payload))
    }

    @Test
    fun `a negative serial is dropped`() {
        val payload = "-1\tcom.a/.Main\n0\tcom.b/.Main"
        assertEquals(listOf(AppKey(0L, "com.b/.Main")), FavoriteCodec.decode(payload))
    }

    @Test
    fun `more than five lines are clamped to five`() {
        val favorites = (1..8).map { AppKey(0L, "com.app$it/.Main") }
        val encoded = favorites.joinToString("\n") { "${it.userSerial}\t${it.component}" }
        assertEquals(favorites.take(5), FavoriteCodec.decode(encoded))
    }

    @Test
    fun `encode already clamps to five before writing`() {
        val favorites = (1..8).map { AppKey(0L, "com.app$it/.Main") }
        assertEquals(favorites.take(5), FavoriteCodec.decode(FavoriteCodec.encode(favorites)))
    }

    @Test
    fun `a partially corrupt payload keeps the valid lines and drops the rest`() {
        val payload = listOf(
            "0\tcom.a/.Main",
            "not-a-serial\tcom.bad/.Main",
            "0\tmissing-slash",
            "0\t/OnlyClass",
            "0\tcom.only-package/",
            "0\tcom.b/.Main",
        ).joinToString("\n")
        assertEquals(
            listOf(AppKey(0L, "com.a/.Main"), AppKey(0L, "com.b/.Main")),
            FavoriteCodec.decode(payload),
        )
    }

    @Test
    fun `a duplicate key keeps only its first occurrence`() {
        val payload = "0\tcom.a/.Main\n0\tcom.b/.Main\n0\tcom.a/.Main"
        assertEquals(
            listOf(AppKey(0L, "com.a/.Main"), AppKey(0L, "com.b/.Main")),
            FavoriteCodec.decode(payload),
        )
    }
}
