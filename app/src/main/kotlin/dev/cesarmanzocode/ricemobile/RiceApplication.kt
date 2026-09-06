package dev.cesarmanzocode.ricemobile

import android.app.Application

class RiceApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
