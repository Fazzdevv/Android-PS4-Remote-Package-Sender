package com.fazzdev.ps4pkgsender

import android.app.Application
import com.fazzdev.ps4pkgsender.data.local.AppDatabase
import com.fazzdev.ps4pkgsender.data.network.RpiClient

class PkgSenderApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsManager: com.fazzdev.ps4pkgsender.data.local.SettingsManager
        private set

    val rpiClient: RpiClient by lazy {
        RpiClient()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        settingsManager = com.fazzdev.ps4pkgsender.data.local.SettingsManager(this)
    }

    companion object {
        lateinit var instance: PkgSenderApp
            private set
    }
}
