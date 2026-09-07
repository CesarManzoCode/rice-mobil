package dev.cesarmanzocode.ricemobile.preferences

import dev.cesarmanzocode.ricemobile.apps.AppKey

enum class FavoriteToggleResult { Added, Removed, LimitReached }

/**
 * Pure favorite-list rules (contract §5.2/§11): max 5, order of addition, remove-then-add
 * goes to the end, a sixth addition is rejected without touching the list. No Android types,
 * so this is a plain JVM unit-test target.
 */
object FavoriteRules {
    const val MAX_FAVORITES = 5

    fun toggle(current: List<AppKey>, key: AppKey): Pair<List<AppKey>, FavoriteToggleResult> = when {
        key in current -> current.filterNot { it == key } to FavoriteToggleResult.Removed
        current.size >= MAX_FAVORITES -> current to FavoriteToggleResult.LimitReached
        else -> (current + key) to FavoriteToggleResult.Added
    }

    fun remove(current: List<AppKey>, key: AppKey): List<AppKey> = current.filterNot { it == key }
}
