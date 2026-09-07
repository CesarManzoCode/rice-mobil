package dev.cesarmanzocode.ricemobile.rice

import dev.cesarmanzocode.ricemobile.apps.AppCategory
import dev.cesarmanzocode.ricemobile.apps.AppEntry
import dev.cesarmanzocode.ricemobile.apps.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerHierarchyTest {

    private fun entry(label: String, category: AppCategory = AppCategory.Other, component: String = "com.example.${label.lowercase()}/.Main") = AppEntry(
        key = AppKey(0L, component),
        packageName = component.substringBefore('/'),
        label = label,
        normalizedLabel = label.lowercase(),
        normalizedPackage = component.substringBefore('/').lowercase(),
        iconRevision = 0L,
        category = category,
    )

    @Test
    fun `categorize drops empty categories and keeps a fixed display order`() {
        val apps = listOf(
            entry("Chat", AppCategory.Communication),
            entry("Docs", AppCategory.Productivity),
            entry("Photos", AppCategory.Multimedia),
        )
        val groups = DrawerHierarchy.categorize(apps)
        assertEquals(listOf(AppCategory.Communication, AppCategory.Productivity, AppCategory.Multimedia), groups.map { it.category })
    }

    @Test
    fun `categorize never drops an app`() {
        val apps = listOf(entry("A", AppCategory.Games), entry("B", AppCategory.Other))
        val total = DrawerHierarchy.categorize(apps).sumOf { it.apps.size }
        assertEquals(apps.size, total)
    }

    @Test
    fun `groupByInitial buckets names without a leading letter under hash`() {
        val apps = listOf(entry("1Password"), entry("Alpha"), entry("Beta"))
        val groups = DrawerHierarchy.groupByInitial(apps)
        assertEquals(listOf("#", "A", "B"), groups.map { it.first })
    }
}
