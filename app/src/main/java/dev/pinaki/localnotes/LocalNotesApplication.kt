package dev.pinaki.localnotes

import android.app.Application
import dev.pinaki.localnotes.di.AppContainer

class LocalNotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        AppContainer.getInstance().notesServer.stop()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: LocalNotesApplication
            private set
    }
}
