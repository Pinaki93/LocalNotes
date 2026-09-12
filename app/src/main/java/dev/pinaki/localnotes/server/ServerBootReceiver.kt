package dev.pinaki.localnotes.server

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Restores a server the user left enabled after the device finishes booting. */
class ServerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && NotesServerService.wasEnabled(context)) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, NotesServerService::class.java)
                    .setAction(NotesServerService.ACTION_START),
            )
        }
    }
}
