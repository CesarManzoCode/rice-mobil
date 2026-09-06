package dev.cesarmanzocode.ricemobile.apps

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * Pure normalization, ordering and filtering rules for the app catalog (contract §5.1).
 * No Android types: testable as plain JVM unit tests.
 */
object AppSearch {

    private val combiningMarks = Regex("\\p{M}+")
    private val whitespace = Regex("\\s+")
    private const val MAX_QUERY_LENGTH = 200

    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val withoutMarks = combiningMarks.replace(decomposed, "")
        return withoutMarks.lowercase(Locale.ROOT).trim().replace(whitespace, " ")
    }

    fun order(entries: List<AppEntry>, locale: Locale = Locale.getDefault()): List<AppEntry> {
        val collator = Collator.getInstance(locale)
        return entries.sortedWith(
            compareBy(collator) { it.label }
                .thenBy { it.key.component }
                .thenBy { it.key.userSerial }
        )
    }

    fun clampQuery(query: String): String = query.take(MAX_QUERY_LENGTH)

    /** Empty query means "no filtering": callers should show [ordered] unchanged. */
    fun tokensOf(query: String): List<String> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()
        return normalizedQuery.split(" ").filter { it.isNotEmpty() }
    }

    fun matches(entry: AppEntry, tokens: List<String>): Boolean =
        tokens.all { token -> entry.normalizedLabel.contains(token) || entry.normalizedPackage.contains(token) }

    /** Empty query returns [ordered] unchanged; otherwise every token must match as a substring. */
    fun filter(ordered: List<AppEntry>, query: String): List<AppEntry> {
        val tokens = tokensOf(query)
        if (tokens.isEmpty()) return ordered
        return ordered.filter { entry -> matches(entry, tokens) }
    }
}
