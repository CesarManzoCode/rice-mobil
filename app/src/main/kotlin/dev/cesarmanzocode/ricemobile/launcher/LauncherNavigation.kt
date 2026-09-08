package dev.cesarmanzocode.ricemobile.launcher

import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.ui.shared.ScreenRect

/**
 * Pure navigation/transient-state rules (contract §7), extracted from [LauncherViewModel] so
 * they are testable as plain JVM unit tests without an Android/repository dependency.
 */
object LauncherNavigation {

    fun openDrawer(state: TransientState): TransientState =
        state.copy(screen = LauncherScreen.Drawer)

    /** Picker always opens from Home with a cleared query, even reached via the Drawer (§7). */
    fun openPicker(state: TransientState): TransientState =
        state.copy(screen = LauncherScreen.RicePicker, query = "", appMenu = null)

    /** A successful launch, or leaving the drawer/picker normally, clears query/menu/message. */
    fun goHome(state: TransientState): TransientState =
        state.copy(screen = LauncherScreen.Home, query = "", appMenu = null, message = null)

    /** A Home intent resets everything transient, even if it repeats (contract §3.5/§7). Wallpaper
     * status is not part of this state (it is owned by the wallpaper controller), so an
     * in-flight apply is never cancelled by pressing Home. */
    fun resetToHome(state: TransientState): TransientState =
        TransientState(isDefaultHome = state.isDefaultHome)

    /** A failed launch keeps the drawer/query in place and only surfaces a message. */
    fun launchFailed(state: TransientState, messageId: Long): TransientState =
        state.copy(message = UiMessage(messageId, UiMessageType.AppLaunchFailed))

    fun favoriteLimitReached(state: TransientState, messageId: Long): TransientState =
        state.copy(message = UiMessage(messageId, UiMessageType.FavoriteLimitReached))

    fun showAppMenu(state: TransientState, key: AppKey, anchor: ScreenRect): TransientState =
        state.copy(appMenu = AppMenuRequest(key, anchor))

    fun dismissAppMenu(state: TransientState): TransientState = state.copy(appMenu = null)

    // Back precedence (contract §7): menu closes first, then IME, then Picker/Drawer -> Home,
    // then Home is a no-op only while this app actually holds the Home role.

    fun menuBackHandlerEnabled(appMenu: AppMenuRequest?): Boolean = appMenu != null

    fun imeBackHandlerEnabled(screen: LauncherScreen, imeVisible: Boolean): Boolean =
        screen == LauncherScreen.Drawer && imeVisible

    fun drawerBackHandlerEnabled(screen: LauncherScreen, imeVisible: Boolean): Boolean =
        screen == LauncherScreen.Drawer && !imeVisible

    fun pickerBackHandlerEnabled(screen: LauncherScreen): Boolean = screen == LauncherScreen.RicePicker

    fun homeNoOpBackHandlerEnabled(screen: LauncherScreen, isDefaultHome: Boolean): Boolean =
        screen == LauncherScreen.Home && isDefaultHome
}
