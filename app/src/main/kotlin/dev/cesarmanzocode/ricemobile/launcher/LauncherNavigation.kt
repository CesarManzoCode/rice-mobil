package dev.cesarmanzocode.ricemobile.launcher

/**
 * Pure navigation/transient-state rules (contract §7), extracted from [LauncherViewModel] so
 * they are testable as plain JVM unit tests without an Android/repository dependency.
 */
object LauncherNavigation {

    fun openDrawer(state: TransientState): TransientState =
        state.copy(screen = LauncherScreen.Drawer)

    /** A successful launch, or leaving the drawer normally, clears query/menu/message. */
    fun goHome(state: TransientState): TransientState =
        state.copy(screen = LauncherScreen.Home, query = "", message = null)

    /** A Home intent resets everything transient, even if it repeats (contract §3.5/§7). */
    fun resetToHome(state: TransientState): TransientState =
        TransientState(screen = LauncherScreen.Home, isDefaultHome = state.isDefaultHome)

    /** A failed launch keeps the drawer/query in place and only surfaces a message. */
    fun launchFailed(state: TransientState, messageId: Long): TransientState =
        state.copy(message = UiMessage(messageId, UiMessageType.AppLaunchFailed))

    // Back precedence (contract §7): IME closes first, then Drawer -> Home, then Home is a
    // no-op only while this app actually holds the Home role.

    fun imeBackHandlerEnabled(screen: LauncherScreen, imeVisible: Boolean): Boolean =
        screen == LauncherScreen.Drawer && imeVisible

    fun drawerBackHandlerEnabled(screen: LauncherScreen, imeVisible: Boolean): Boolean =
        screen == LauncherScreen.Drawer && !imeVisible

    fun homeNoOpBackHandlerEnabled(screen: LauncherScreen, isDefaultHome: Boolean): Boolean =
        screen == LauncherScreen.Home && isDefaultHome
}
