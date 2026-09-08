package dev.cesarmanzocode.ricemobile.rice.monochrome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.Rice
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperSpec

/** Austere typographic block (contract §18.2): rigid start-aligned column, an oversized clock,
 * favorites as a linear list, a single-list drawer with rules. No dock, no cards, no color. */
object MonochromeRice : Rice {
    override val id = RiceId.Monochrome
    override val wallpaper = WallpaperSpec(
        assetPath = "wallpapers/monochrome.webp",
        assetRevision = 3,
        fallbackColorArgb = 0xFF0A0A0AL,
    )
    override val motion = RiceMotion.Monochrome
    override val lightSystemBars = false

    @Composable
    override fun Home(model: HomeModel, actions: RiceActions, modifier: Modifier) {
        MonochromeHome(model = model, actions = actions, modifier = modifier)
    }

    @Composable
    override fun Drawer(model: DrawerModel, actions: RiceActions, modifier: Modifier) {
        MonochromeDrawer(model = model, actions = actions, modifier = modifier)
    }
}
