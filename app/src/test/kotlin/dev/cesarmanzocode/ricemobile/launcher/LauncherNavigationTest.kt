package dev.cesarmanzocode.ricemobile.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherNavigationTest {

    @Test
    fun `openDrawer switches screen without touching query`() {
        val state = TransientState(screen = LauncherScreen.Home, query = "chr")
        val next = LauncherNavigation.openDrawer(state)
        assertEquals(LauncherScreen.Drawer, next.screen)
        assertEquals("chr", next.query)
    }

    @Test
    fun `goHome on launch success clears query and message`() {
        val state = TransientState(
            screen = LauncherScreen.Drawer,
            query = "chr",
            message = UiMessage(1L, UiMessageType.AppLaunchFailed),
        )
        val next = LauncherNavigation.goHome(state)
        assertEquals(LauncherScreen.Home, next.screen)
        assertEquals("", next.query)
        assertNull(next.message)
    }

    @Test
    fun `launchFailed keeps drawer and query in place`() {
        val state = TransientState(screen = LauncherScreen.Drawer, query = "chr")
        val next = LauncherNavigation.launchFailed(state, messageId = 42L)
        assertEquals(LauncherScreen.Drawer, next.screen)
        assertEquals("chr", next.query)
        assertEquals(UiMessage(42L, UiMessageType.AppLaunchFailed), next.message)
    }

    @Test
    fun `resetToHome clears everything transient but keeps the role status`() {
        val state = TransientState(
            screen = LauncherScreen.Drawer,
            query = "chr",
            isDefaultHome = true,
            message = UiMessage(1L, UiMessageType.AppLaunchFailed),
        )
        val next = LauncherNavigation.resetToHome(state)
        assertEquals(TransientState(screen = LauncherScreen.Home, isDefaultHome = true), next)
    }

    @Test
    fun `resetToHome is idempotent when a Home intent repeats`() {
        val alreadyHome = TransientState(screen = LauncherScreen.Home, isDefaultHome = true)
        val next = LauncherNavigation.resetToHome(alreadyHome)
        assertEquals(alreadyHome, next)
    }

    @Test
    fun `back precedence- IME first, then drawer to home, then home is a no-op only when default`() {
        assertTrue(LauncherNavigation.imeBackHandlerEnabled(LauncherScreen.Drawer, imeVisible = true))
        assertTrue(!LauncherNavigation.imeBackHandlerEnabled(LauncherScreen.Home, imeVisible = true))

        assertTrue(LauncherNavigation.drawerBackHandlerEnabled(LauncherScreen.Drawer, imeVisible = false))
        assertTrue(!LauncherNavigation.drawerBackHandlerEnabled(LauncherScreen.Drawer, imeVisible = true))

        assertTrue(LauncherNavigation.homeNoOpBackHandlerEnabled(LauncherScreen.Home, isDefaultHome = true))
        assertTrue(!LauncherNavigation.homeNoOpBackHandlerEnabled(LauncherScreen.Home, isDefaultHome = false))
        assertTrue(!LauncherNavigation.homeNoOpBackHandlerEnabled(LauncherScreen.Drawer, isDefaultHome = true))
    }
}
