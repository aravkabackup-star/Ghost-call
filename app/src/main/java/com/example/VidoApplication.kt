package com.example

import android.app.Application
import com.example.data.local.VidoDatabase
import com.example.data.preferences.VidoPreferences
import com.example.data.repository.GhostCallRepository
import com.example.notification.NotificationHelper

class VidoApplication : Application() {

    lateinit var database: VidoDatabase
        private set

    lateinit var repository: GhostCallRepository
        private set

    lateinit var preferences: VidoPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = VidoDatabase.getDatabase(this)
        repository = GhostCallRepository(database.ghostCallDao(), database.callHistoryDao())
        preferences = VidoPreferences(this)

        NotificationHelper.createNotificationChannels(this)
    }

    companion object {
        lateinit var instance: VidoApplication
            private set
    }
}
