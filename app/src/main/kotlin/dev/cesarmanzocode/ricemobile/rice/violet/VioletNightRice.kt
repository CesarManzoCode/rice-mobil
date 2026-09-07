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

/** Scenic cluster + sheet (contract §18.6): the wallpaper is the protagonist, the clock sits
 * small in the top-end corner, favorites form a 1-2-2 cluster in the lower half, and the Drawer
 * is a real bottom sheet that leaves the wallpaper visible above it — structurally unlike any of
 * the other four. */
object VioletNightRice : Rice {
    override val id = RiceId.VioletNight
    override val wallpaper = WallpaperSpec(
        assetPath = "wallpapers/violet_night.webp",
        assetRevision = 2,
        fallbackColorArgb = 0xFF100C24L,
    )
    override val motion = RiceMotion.Violet
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
