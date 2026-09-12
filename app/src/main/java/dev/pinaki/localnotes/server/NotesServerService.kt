package dev.pinaki.localnotes.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.pinaki.localnotes.ContainerActivity
import dev.pinaki.localnotes.R
import dev.pinaki.localnotes.di.AppContainer
import java.util.concurrent.Executors

/** Owns the HTTP server independently of activities and keeps it foreground-visible. */
class NotesServerService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    private val notesServer by lazy { AppContainer.getInstance().notesServer }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            setEnabled(false)
            stopServerAndSelf()
            return START_NOT_STICKY
        }

        setEnabled(true)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(getString(R.string.server_notification_starting)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
        executor.execute {
            notesServer.start()
                .onSuccess { updateNotification(notesServer.addresses.value.joinToString()) }
                .onFailure {
                    Log.e(TAG, "Unable to run LocalNotes server", it)
                    setEnabled(false)
                    stopServerAndSelf()
                }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        notesServer.stop()
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopServerAndSelf() {
        notesServer.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(addresses: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(addresses))
    }

    private fun notification(content: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, ContainerActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopServer = PendingIntent.getService(
            this, 1,
            Intent(this, NotesServerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.server_notification_title))
            .setContentText(content)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.server_notification_stop), stopServer)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.server_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun setEnabled(enabled: Boolean) {
        getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        const val ACTION_START = "dev.pinaki.localnotes.server.START"
        const val ACTION_STOP = "dev.pinaki.localnotes.server.STOP"
        private const val TAG = "NotesServerService"
        private const val CHANNEL_ID = "local_notes_server"
        private const val NOTIFICATION_ID = 8080
        private const val PREFERENCES = "server_state"
        private const val KEY_ENABLED = "enabled"

        fun wasEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)
    }
}
