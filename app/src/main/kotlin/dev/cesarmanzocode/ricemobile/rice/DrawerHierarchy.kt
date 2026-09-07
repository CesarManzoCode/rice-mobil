package dev.cesarmanzocode.ricemobile.rice

import androidx.compose.runtime.Immutable
import dev.cesarmanzocode.ricemobile.apps.AppCategory
import dev.cesarmanzocode.ricemobile.apps.AppEntry

/** One non-empty category and the apps currently classified into it (§"CATEGORÍAS"). [apps] is a
 * freshly-derived, never-mutated snapshot (see [DrawerHierarchy.categorize]), so this is honestly
 * immutable: marking it lets a category tile skip recomposition when its own group is unchanged. */
@Immutable
data class CategoryGroup(val category: AppCategory, val apps: List<AppEntry>)

/**
 * What a Drawer shows before "Todas las apps" (Sprint 3 second pass §"CAMBIO DE JERARQUÍA"):
 * búsqueda -> recientes -> categorías -> escape completo. Pure/derivable from the full catalog so
 * every rice computes the same groups and only differs in how it draws them (contract §6.2: rice
 * owns structure, not data derivation). Each rice keeps its own [DrawerView] as local composable
 * state — the host's route enum never needs to know about this internal browsing.
 */
sealed interface DrawerView {
    data object Browse : DrawerView
    data class Category(val category: AppCategory) : DrawerView
    data object AllApps : DrawerView
}

object DrawerHierarchy {
    /** Fixed, deterministic display order; empty categories are simply absent — never rendered
     * as a hollow placeholder — and "Otros" always sits last. */
    private val DISPLAY_ORDER = listOf(
        AppCategory.Communication,
        AppCategory.Productivity,
        AppCategory.Multimedia,
        AppCategory.MapsTravel,
        AppCategory.News,
        AppCategory.Games,
        AppCategory.Tools,
        AppCategory.Other,
    )

    fun categorize(apps: List<AppEntry>): List<CategoryGroup> {
        val byCategory = apps.groupBy { it.category }
        return DISPLAY_ORDER.mapNotNull { category ->
            byCategory[category]?.takeIf { it.isNotEmpty() }?.let { CategoryGroup(category, it) }
        }
    }

    /** Consecutive same-initial groups over already Collator-sorted [apps] (contract §5.1); a
     * name without a leading letter falls into "#". Shared so every rice's "Todas las apps"
     * escape hatch groups identically. */
    fun groupByInitial(apps: List<AppEntry>): List<Pair<String, List<AppEntry>>> {
        val groups = LinkedHashMap<String, MutableList<AppEntry>>()
        for (entry in apps) {
            val first = entry.label.trim().firstOrNull()
            val key = if (first != null && first.isLetter()) first.uppercaseChar().toString() else "#"
            groups.getOrPut(key) { mutableListOf() }.add(entry)
        }
        return groups.map { it.key to it.value }
    }
}
