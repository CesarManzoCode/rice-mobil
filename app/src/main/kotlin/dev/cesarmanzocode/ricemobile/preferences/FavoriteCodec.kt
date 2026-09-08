package dev.cesarmanzocode.ricemobile.preferences

import dev.cesarmanzocode.ricemobile.apps.AppKey

/**
 * Exact codec from contract §11: up to five lines `serial<TAB>component`, using full flattened
 * components (`package/Class`). No `stringSet` (it would destroy order). Pure JVM, no Android
 * types. A malformed line is dropped, not fatal; a missing payload decodes to an empty list.
 */
object FavoriteCodec {
    private const val MAX_FAVORITES = FavoriteRules.MAX_FAVORITES

    fun encode(favorites: List<AppKey>): String =
        favorites.take(MAX_FAVORITES).joinToString("\n") { AppKeyLineCodec.encodeLine(it) }

    fun decode(payload: String?): List<AppKey> = AppKeyLineCodec.decodeList(payload, MAX_FAVORITES)
}
