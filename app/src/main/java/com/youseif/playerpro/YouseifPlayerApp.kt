package com.youseif.playerpro

import android.app.Application
import com.youseif.playerpro.data.local.AppDatabase
import com.youseif.playerpro.data.repository.SettingsRepository
import com.youseif.playerpro.data.repository.SourceRepository

class YouseifPlayerApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var sourceRepository: SourceRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        sourceRepository = SourceRepository(database.sourceDao())
        settingsRepository = SettingsRepository(this)
    }
}
