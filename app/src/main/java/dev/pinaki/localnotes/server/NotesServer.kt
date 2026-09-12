package dev.pinaki.localnotes.server

import android.util.Log

class NotesServer(
    private val server: Server,
    private val restControllers: List<RestController>,
    private val htmlCrudControllers: List<HtmlCrudController>,
) {
    fun startSilently() {
        runCatching {
            with(server) {
                restControllers.forEach(::registerController)
                htmlCrudControllers.forEach(::registerController)
                start()
                getNetworkAddresses().forEach { address ->
                    Log.i(TAG, "LocalNotes server: $address")
                }
            }

        }.onFailure { error ->
            Log.e(TAG, "Unable to start the LocalNotes server", error)
        }
    }

    companion object {
        private const val TAG = "NotesServer"
    }
}
