package dev.cesarmanzocode.ricemobile.rice.arctic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.Rice
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperSpec

/** Structure: Home is a floating horizontal dock; Drawer is a grid inside a panel. */
object ArcticGlassRice : Rice {
    override val id = RiceId.ArcticGlass
    override val wallpaper = WallpaperSpec(
        assetPath = "wallpapers/arctic_glass.png",
        assetRevision = 1,
        fallbackColorArgb = 0xFFD8E6EEL,
    )
    override val motion = RiceMotion(drawerEnterMs = 280, drawerExitMs = 220)
    override val lightSystemBars = true

    @Composable
    override fun Home(model: HomeModel, actions: RiceActions, modifier: Modifier) {
        ArcticHome(model = model, actions = actions, modifier = modifier)
    }

    @Composable
    override fun Drawer(model: DrawerModel, actions: RiceActions, modifier: Modifier) {
        ArcticDrawer(model = model, actions = actions, modifier = modifier)
    }
}
