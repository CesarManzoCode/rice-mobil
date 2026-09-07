package dev.cesarmanzocode.ricemobile.apps

/**
 * A launchable identity: component + user serial. Never label, position, or package alone
 * (contract §5, invariant 1).
 */
data class AppKey(
    val userSerial: Long,
    val component: String, // ComponentName.flattenToString(), never flattenToShortString.
)

/** Package name derived from [AppKey.component] for an unavailable favorite slot (§5.2). */
val AppKey.packageNameGuess: String
    get() = component.substringBefore('/', missingDelimiterValue = component)

data class AppEntry(
    val key: AppKey,
    val packageName: String,
    val label: String,
    val normalizedLabel: String,
    val normalizedPackage: String,
    val iconRevision: Long,
    val available: Boolean = true,
)
