package dev.cesarmanzocode.ricemobile.rice.ember

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.Rice
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperSpec

/** Structure: Home favorites in a matrix/block grid; Drawer is two columns of compact rows. */
object EmberForgeRice : Rice {
    override val id = RiceId.EmberForge
    override val wallpaper = WallpaperSpec(
        assetPath = "wallpapers/ember_forge.png",
        assetRevision = 1,
        fallbackColorArgb = 0xFF241512L,
    )
    override val motion = RiceMotion(drawerEnterMs = 170, drawerExitMs = 140)
    override val lightSystemBars = false

    @Composable
    override fun Home(model: HomeModel, actions: RiceActions, modifier: Modifier) {
        EmberHome(model = model, actions = actions, modifier = modifier)
    }

    @Composable
    override fun Drawer(model: DrawerModel, actions: RiceActions, modifier: Modifier) {
        EmberDrawer(model = model, actions = actions, modifier = modifier)
    }
}
