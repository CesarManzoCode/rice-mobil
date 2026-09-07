package dev.cesarmanzocode.ricemobile.apps

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryTest {

    @Test
    fun `maps declared platform categories directly`() {
        assertEquals(AppCategory.Communication, AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_SOCIAL, isSystemApp = false))
        assertEquals(AppCategory.Productivity, AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_PRODUCTIVITY, isSystemApp = false))
        assertEquals(AppCategory.Multimedia, AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_AUDIO, isSystemApp = false))
        assertEquals(AppCategory.Multimedia, AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_VIDEO, isSystemApp = false))
        assertEquals(AppCategory.Multimedia, AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_IMAGE, isSystemApp = false))
        assertEquals(AppCategory.News, AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_NEWS, isSystemApp = false))
        assertEquals(AppCategory.MapsTravel, AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_MAPS, isSystemApp = false))
        assertEquals(AppCategory.Games, AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_GAME, isSystemApp = false))
    }

    @Test
    fun `undeclared category falls back to Tools only for system apps`() {
        assertEquals(
            AppCategory.Tools,
            AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_UNDEFINED, isSystemApp = true),
        )
        assertEquals(
            AppCategory.Other,
            AppCategory.fromApplicationInfoCategory(ApplicationInfo.CATEGORY_UNDEFINED, isSystemApp = false),
        )
    }
}
