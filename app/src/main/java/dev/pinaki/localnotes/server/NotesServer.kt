package dev.pinaki.localnotes.server

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotesServer(
    private val context: Context,
    private val serverFactory: () -> Server,
    private val restControllers: List<RestController>,
    private val htmlCrudControllers: List<HtmlCrudController>,
) {
    private var server: Server? = null
    private val mutableIsRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = mutableIsRunning.asStateFlow()
    private val mutableAddresses = MutableStateFlow<List<String>>(emptyList())
    val addresses: StateFlow<List<String>> = mutableAddresses.asStateFlow()

    fun requestStart(): Result<Unit> = runCatching {
        ContextCompat.startForegroundService(
            context,
            Intent(context, NotesServerService::class.java)
                .setAction(NotesServerService.ACTION_START),
        )
    }.map { }

    fun requestStop() {
        context.startService(
            Intent(context, NotesServerService::class.java)
                .setAction(NotesServerService.ACTION_STOP),
        )
    }

    @Synchronized
    internal fun start(): Result<Unit> {
        if (server != null) return Result.success(Unit)
        return runCatching {
            val newServer = serverFactory()
            try {
                with(newServer) {
                    restControllers.forEach(::registerController)
                    htmlCrudControllers.forEach(::registerController)
                    start()
                    val activeAddresses = getNetworkAddresses()
                    activeAddresses.forEach { address ->
                        Log.i(TAG, "LocalNotes server: $address")
                    }
                    mutableAddresses.value = activeAddresses
                }
                server = newServer
                mutableIsRunning.value = true
            } catch (error: Throwable) {
                newServer.close()
                throw error
            }
        }
    }

    @Synchronized
    internal fun stop() {
        server?.close()
        server = null
        mutableIsRunning.value = false
        mutableAddresses.value = emptyList()
    }

    companion object {
        private const val TAG = "NotesServer"
    }
}
