package dev.cesarmanzocode.ricemobile.preferences

import dev.cesarmanzocode.ricemobile.apps.AppKey

/**
 * Same line format as [FavoriteCodec] but for the local, most-recent-first launch history
 * (Sprint 3 second pass §"RECENTES") — never Android UsageStats, never a system permission, never
 * scraping. Capped at [MAX_RECENTS] so the payload never grows unbounded; [RecentAppsRules] owns
 * the push-to-front/dedup logic that keeps the list within that cap.
 */
object RecentAppsCodec {
    const val MAX_RECENTS = 12

    fun encode(keys: List<AppKey>): String =
        keys.take(MAX_RECENTS).joinToString("\n") { AppKeyLineCodec.encodeLine(it) }

    fun decode(payload: String?): List<AppKey> = AppKeyLineCodec.decodeList(payload, MAX_RECENTS)
}
