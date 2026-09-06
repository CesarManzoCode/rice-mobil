package dev.cesarmanzocode.ricemobile.apps

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    private fun entry(
        label: String,
        component: String = "com.example.${label.lowercase()}/.Main",
        userSerial: Long = 0L,
        packageName: String = component.substringBefore('/'),
    ) = AppEntry(
        key = AppKey(userSerial, component),
        packageName = packageName,
        label = label,
        normalizedLabel = AppSearch.normalize(label),
        normalizedPackage = AppSearch.normalize(packageName),
        iconRevision = 0L,
    )

    @Test
    fun `normalize strips accents case and collapses whitespace`() {
        assertEquals("musica", AppSearch.normalize("Música"))
        assertEquals("google play store", AppSearch.normalize("  Google   Play  Store  "))
        assertEquals("cafe", AppSearch.normalize("CAFÉ"))
    }

    @Test
    fun `normalize handles unicode and emoji without crashing`() {
        assertEquals("app 😀", AppSearch.normalize("App 😀"))
        assertEquals("naive", AppSearch.normalize("naïve"))
    }

    @Test
    fun `normalize of blank label is empty`() {
        assertEquals("", AppSearch.normalize(""))
        assertEquals("", AppSearch.normalize("   "))
    }

    @Test
    fun `empty query returns the ordered list unchanged`() {
        val entries = listOf(entry("Zeta"), entry("Alpha"))
        assertEquals(entries, AppSearch.filter(entries, ""))
        assertEquals(entries, AppSearch.filter(entries, "   "))
    }

    @Test
    fun `filter matches an accented query against the normalized label`() {
        val musica = entry("Musica")
        val results = AppSearch.filter(listOf(musica), "MÚSICA")
        assertEquals(listOf(musica), results)
    }

    @Test
    fun `filter requires every token to match as a substring`() {
        val chrome = entry("Google Chrome")
        val maps = entry("Google Maps")
        val results = AppSearch.filter(listOf(chrome, maps), "google chr")
        assertEquals(listOf(chrome), results)
    }

    @Test
    fun `filter matches package name as well as label`() {
        val target = entry(label = "Weird Name", component = "com.example.target/.Main", packageName = "com.example.target")
        val other = entry(label = "Other", component = "com.example.other/.Main", packageName = "com.example.other")
        val results = AppSearch.filter(listOf(target, other), "target")
        assertEquals(listOf(target), results)
    }

    @Test
    fun `filter with no matches returns empty list`() {
        val entries = listOf(entry("Alpha"), entry("Beta"))
        assertTrue(AppSearch.filter(entries, "zzz-nonexistent").isEmpty())
    }

    @Test
    fun `order sorts by collated label then component then userSerial`() {
        val zeta = entry("Zeta")
        val alpha = entry("Alpha")
        val accentedCafe = entry("Café", component = "com.example.cafe/.Main")
        val ordered = AppSearch.order(listOf(zeta, alpha, accentedCafe), locale = Locale.US)
        assertEquals(listOf(alpha, accentedCafe, zeta), ordered)
    }

    @Test
    fun `order breaks ties on equal labels by component then userSerial`() {
        val sameLabelA = entry("Same", component = "com.example.a/.Main")
        val sameLabelB = entry("Same", component = "com.example.b/.Main")
        val sameLabelBSerial1 = sameLabelB.copy(key = AppKey(1L, sameLabelB.key.component))
        val ordered = AppSearch.order(listOf(sameLabelBSerial1, sameLabelB, sameLabelA), locale = Locale.US)
        assertEquals(listOf(sameLabelA, sameLabelB, sameLabelBSerial1), ordered)
    }

    @Test
    fun `clampQuery limits input length without touching results list`() {
        val longQuery = "a".repeat(500)
        assertEquals(200, AppSearch.clampQuery(longQuery).length)
    }
}
