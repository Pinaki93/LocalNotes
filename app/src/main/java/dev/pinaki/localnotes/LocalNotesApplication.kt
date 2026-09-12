package dev.pinaki.localnotes

import android.app.Application

class LocalNotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LocalNotesApplication
            private set
    }
}
