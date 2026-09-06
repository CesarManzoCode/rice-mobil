package dev.cesarmanzocode.ricemobile.apps

/**
 * A launchable identity: component + user serial. Never label, position, or package alone
 * (contract §5, invariant 1).
 */
data class AppKey(
    val userSerial: Long,
    val component: String, // ComponentName.flattenToString(), never flattenToShortString.
)

data class AppEntry(
    val key: AppKey,
    val packageName: String,
    val label: String,
    val normalizedLabel: String,
    val normalizedPackage: String,
    val iconRevision: Long,
    val available: Boolean = true,
)
