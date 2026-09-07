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

/** Industrial matrix (contract §18.4): a compressed two-column header, favorites as 1+2x2 blocks
 * with cut corners, and a dense two-column Drawer of compact horizontal rows. */
object EmberForgeRice : Rice {
    override val id = RiceId.EmberForge
    override val wallpaper = WallpaperSpec(
        assetPath = "wallpapers/ember_forge.webp",
        assetRevision = 2,
        fallbackColorArgb = 0xFF171411L,
    )
    override val motion = RiceMotion.Ember
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
