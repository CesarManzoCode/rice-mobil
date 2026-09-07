package dev.cesarmanzocode.ricemobile.preferences

import dev.cesarmanzocode.ricemobile.apps.AppKey

/**
 * Pure recent-history rule (Sprint 3 second pass): opening an app moves it to the front and drops
 * any older occurrence of the same identity; the list never grows past
 * [RecentAppsCodec.MAX_RECENTS]. No Android types, so this is a plain JVM unit-test target like
 * [dev.cesarmanzocode.ricemobile.preferences.FavoriteRules].
 */
object RecentAppsRules {
    fun recordOpen(current: List<AppKey>, key: AppKey): List<AppKey> =
        (listOf(key) + current.filterNot { it == key }).take(RecentAppsCodec.MAX_RECENTS)
}
