package dev.cesarmanzocode.ricemobile.rice.violet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.Rice
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperSpec

/** Structure: Home favorites are a 1-2-2 cluster; Drawer is a sheet from the bottom. */
object VioletNightRice : Rice {
    override val id = RiceId.VioletNight
    override val wallpaper = WallpaperSpec(
        assetPath = "wallpapers/violet_night.png",
        assetRevision = 1,
        fallbackColorArgb = 0xFF241A38L,
    )
    override val motion = RiceMotion(drawerEnterMs = 340, drawerExitMs = 260)
    override val lightSystemBars = false

    @Composable
    override fun Home(model: HomeModel, actions: RiceActions, modifier: Modifier) {
        VioletHome(model = model, actions = actions, modifier = modifier)
    }

    @Composable
    override fun Drawer(model: DrawerModel, actions: RiceActions, modifier: Modifier) {
        VioletDrawer(model = model, actions = actions, modifier = modifier)
    }
}
