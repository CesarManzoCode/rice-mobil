package dev.cesarmanzocode.ricemobile.rice.ivory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cesarmanzocode.ricemobile.rice.DrawerModel
import dev.cesarmanzocode.ricemobile.rice.HomeModel
import dev.cesarmanzocode.ricemobile.rice.Rice
import dev.cesarmanzocode.ricemobile.rice.RiceActions
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.rice.RiceMotion
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperSpec

/** Structure: Home favorites as a textual index; Drawer is an editorial index list. */
object IvoryPaperRice : Rice {
    override val id = RiceId.IvoryPaper
    override val wallpaper = WallpaperSpec(
        assetPath = "wallpapers/ivory_paper.png",
        assetRevision = 1,
        fallbackColorArgb = 0xFFF3ECDFL,
    )
    override val motion = RiceMotion(drawerEnterMs = 200, drawerExitMs = 160)
    override val lightSystemBars = true

    @Composable
    override fun Home(model: HomeModel, actions: RiceActions, modifier: Modifier) {
        IvoryHome(model = model, actions = actions, modifier = modifier)
    }

    @Composable
    override fun Drawer(model: DrawerModel, actions: RiceActions, modifier: Modifier) {
        IvoryDrawer(model = model, actions = actions, modifier = modifier)
    }
}
