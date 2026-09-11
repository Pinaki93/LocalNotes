package dev.pinaki.localnotes

import android.app.Application
import android.util.Log
import dev.pinaki.localnotes.server.StaticFileServer

class LocalNotesApplication : Application() {
    private var staticFileServer: StaticFileServer? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        startStaticFileServer()
    }

    override fun onTerminate() {
        staticFileServer?.close()
        staticFileServer = null
        super.onTerminate()
    }

    private fun startStaticFileServer() {
        runCatching {
            StaticFileServer(StaticFileServer.DEFAULT_PORT).also {
                it.start()
                staticFileServer = it
                it.getNetworkAddresses().forEach { address ->
                    Log.i(TAG, "Static file server: $address")
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Unable to start the static file server", error)
        }
    }

    companion object {
        private const val TAG = "LocalNotesServer"

        lateinit var instance: LocalNotesApplication
            private set
    }
}
