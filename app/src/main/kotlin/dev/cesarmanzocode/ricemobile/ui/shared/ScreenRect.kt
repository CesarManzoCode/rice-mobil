package dev.cesarmanzocode.ricemobile.ui.shared

/**
 * Window-relative bounds of a pressed item, captured once via `onGloballyPositioned` so a
 * long-press context menu can anchor itself to the exact item that spawned it (UX overhaul: "no
 * más DropdownMenu genérico sin posición"). Deliberately a plain data class with no Compose/Android
 * type — [dev.cesarmanzocode.ricemobile.launcher.LauncherNavigation] stays a pure JVM unit under
 * test, the same reasoning that already keeps [dev.cesarmanzocode.ricemobile.apps.AppKey] Android-free.
 */
data class ScreenRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f

    companion object {
        val Zero = ScreenRect(0f, 0f, 0f, 0f)
    }
}
