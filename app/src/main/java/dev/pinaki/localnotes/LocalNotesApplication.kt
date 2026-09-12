package dev.pinaki.localnotes

import android.app.Application
import dev.pinaki.localnotes.di.AppContainer
import dev.pinaki.localnotes.server.Server

class LocalNotesApplication : Application() {
    private var server: Server? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        startServer()
    }

    override fun onTerminate() {
        server?.close()
        server = null
        super.onTerminate()
    }

    private fun startServer() {
        AppContainer.getInstance().notesServer.startSilently()
    }

    companion object {
        private const val TAG = "LocalNotesServer"

        lateinit var instance: LocalNotesApplication
            private set
    }
}
