package dev.cesarmanzocode.ricemobile

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual DI holder scoped to the application process. Instances are added here as
 * their owning sprint introduces them (apps.* in S1, preferences/wallpaper in S2).
 */
class AppContainer(private val applicationContext: Context) {

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
