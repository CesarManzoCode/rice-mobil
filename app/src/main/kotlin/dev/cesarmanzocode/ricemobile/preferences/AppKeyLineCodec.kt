package dev.cesarmanzocode.ricemobile.preferences

import dev.cesarmanzocode.ricemobile.apps.AppKey

/**
 * Shared line format for every `serial<TAB>component` list persisted in DataStore (favorites,
 * recent apps): one parser/encoder so [FavoriteCodec] and `RecentAppsCodec` can't drift on
 * validation. Order-preserving (`LinkedHashSet`, never a `Set`/sorted collection), first
 * occurrence wins on duplicates, malformed lines are dropped rather than fatal.
 */
internal object AppKeyLineCodec {
    fun encodeLine(key: AppKey): String = "${key.userSerial}\t${key.component}"

    fun decodeList(payload: String?, max: Int): List<AppKey> {
        if (payload.isNullOrEmpty()) return emptyList()
        val seen = LinkedHashSet<AppKey>()
        for (line in payload.split("\n")) {
            parseLine(line)?.let { seen.add(it) }
        }
        return seen.take(max)
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
