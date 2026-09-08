package dev.cesarmanzocode.ricemobile.apps

import android.content.pm.ApplicationInfo
import dev.cesarmanzocode.ricemobile.R

/**
 * Local, offline, deterministic classification (Sprint 3 second pass §"CATEGORÍAS"): derived only
 * from [ApplicationInfo.category], the same coarse taxonomy Android itself exposes to Settings —
 * no network, no heuristic guessing, no ML, no per-package allowlist to maintain. Most
 * third-party apps never declare a category and land in [Other]; that is a limitation of the
 * platform data, not a bug here, and no app is ever hidden or dropped from "Todas" because of it.
 */
enum class AppCategory(val labelRes: Int) {
    Communication(R.string.category_communication),
    Productivity(R.string.category_productivity),
    Multimedia(R.string.category_multimedia),
    News(R.string.category_news),
    MapsTravel(R.string.category_maps_travel),
    Games(R.string.category_games),
    Tools(R.string.category_tools),
    Other(R.string.category_other);

    companion object {
        /** [isSystemApp] only ever *adds* undeclared platform apps to [Tools]; it never removes
         * or reclassifies a category Android already declared explicitly. */
        fun fromApplicationInfoCategory(category: Int, isSystemApp: Boolean): AppCategory = when (category) {
            ApplicationInfo.CATEGORY_SOCIAL -> Communication
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> Productivity
            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> Multimedia
            ApplicationInfo.CATEGORY_NEWS -> News
            ApplicationInfo.CATEGORY_MAPS -> MapsTravel
            ApplicationInfo.CATEGORY_GAME -> Games
            else -> if (isSystemApp) Tools else Other
        }
    }
}
