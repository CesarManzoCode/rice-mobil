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

/** Editorial page (contract §18.5): a dateline first, a serif hour at mid-height with generous
 * air, favorites as a numbered textual index (no icons on Home by editorial decision), and a
 * Drawer that reads as an alphabetical index rather than an app grid. */
object IvoryPaperRice : Rice {
    override val id = RiceId.IvoryPaper
    override val wallpaper = WallpaperSpec(
        assetPath = "wallpapers/ivory_paper.webp",
        assetRevision = 3,
        fallbackColorArgb = 0xFFF3EBDDL,
    )
    override val motion = RiceMotion.Ivory
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
