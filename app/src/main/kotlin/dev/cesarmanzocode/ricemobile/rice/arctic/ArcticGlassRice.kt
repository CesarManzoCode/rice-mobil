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

/** Floating dock (contract §18.3): a light centered clock in the upper half, wide empty air, and
 * a floating horizontal glass dock near the thumb. Drawer is a nearly-full rounded panel with a
 * grid inside. Glass V1 = translucency + border + short shadow, never a real backdrop blur. */
object ArcticGlassRice : Rice {
    override val id = RiceId.ArcticGlass
    override val wallpaper = WallpaperSpec(
        assetPath = "wallpapers/arctic_glass.webp",
        assetRevision = 2,
        fallbackColorArgb = 0xFF071B2AL,
    )
    override val motion = RiceMotion.Arctic
    override val lightSystemBars = false

    @Composable
    override fun Home(model: HomeModel, actions: RiceActions, modifier: Modifier) {
        ArcticHome(model = model, actions = actions, modifier = modifier)
    }

    @Composable
    override fun Drawer(model: DrawerModel, actions: RiceActions, modifier: Modifier) {
        ArcticDrawer(model = model, actions = actions, modifier = modifier)
    }
}
