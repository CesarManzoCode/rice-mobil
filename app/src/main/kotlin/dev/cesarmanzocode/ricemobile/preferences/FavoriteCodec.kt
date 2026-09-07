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
        favorites.take(MAX_FAVORITES).joinToString("\n") { "${it.userSerial}\t${it.component}" }

    fun decode(payload: String?): List<AppKey> {
        if (payload.isNullOrEmpty()) return emptyList()
        val seen = LinkedHashSet<AppKey>()
        for (line in payload.split("\n")) {
            parseLine(line)?.let { seen.add(it) } // LinkedHashSet: keeps first occurrence.
        }
        return seen.take(MAX_FAVORITES)
    }

    private fun parseLine(line: String): AppKey? {
        if (line.isEmpty()) return null
        val tabIndex = line.indexOf('\t')
        if (tabIndex <= 0 || tabIndex == line.lastIndex) return null
        if (line.indexOf('\t', tabIndex + 1) != -1) return null // extra tab: reject the line.

        val serial = line.substring(0, tabIndex).toLongOrNull() ?: return null
        if (serial < 0) return null

        val component = line.substring(tabIndex + 1)
        val slash = component.indexOf('/')
        if (slash <= 0 || slash == component.lastIndex) return null
        if (component.indexOf('/', slash + 1) != -1) return null // second '/': malformed.
        val packageName = component.substring(0, slash)
        val className = component.substring(slash + 1)
        if (packageName.isBlank() || className.isBlank()) return null

        return AppKey(userSerial = serial, component = component)
    }
}
