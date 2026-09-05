package com.fazzdev.ps4pkgsender.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fazzdev.ps4pkgsender.R
import com.fazzdev.ps4pkgsender.data.server.PackageHttpServer
import com.fazzdev.ps4pkgsender.ui.MainActivity
import com.fazzdev.ps4pkgsender.util.ByteFormatter

class TransferForegroundService : Service() {

    companion object {
        private const val TAG = "TransferForegroundService"
        const val CHANNEL_ID = "ps4_pkg_transfer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SERVER = "ACTION_START_SERVER"
        const val ACTION_STOP_SERVER = "ACTION_STOP_SERVER"
        const val EXTRA_PORT = "EXTRA_PORT"

        fun start(context: Context, port: Int = 8080) {
            val intent = Intent(context, TransferForegroundService::class.java).apply {
                action = ACTION_START_SERVER
                putExtra(EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TransferForegroundService::class.java).apply {
                action = ACTION_STOP_SERVER
            }
            context.startService(intent)
        }
    }

    private val binder = LocalBinder()
    var httpServer: PackageHttpServer? = null
        private set

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    inner class LocalBinder : Binder() {
        fun getService(): TransferForegroundService = this@TransferForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVER -> {
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                startServerAndLocks(port)
            }
            ACTION_STOP_SERVER -> {
                stopServerAndLocks()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startServerAndLocks(port: Int) {
        // Start foreground with persistent notification
        val notification = buildNotification("Server aktif di port $port (Menunggu permintaan PS4)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Acquire Wi-Fi High Performance Lock (PRD 6.9)
        acquireLocks()

        // Initialize and start HTTP server if not already running
        if (httpServer == null || httpServer?.port != port) {
            httpServer?.stop()
            httpServer = PackageHttpServer(applicationContext, port)
        }

        if (httpServer?.isServerRunning() != true) {
            try {
                httpServer?.start()
                Log.i(TAG, "Local HTTP server running on port $port")
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting HTTP server", e)
                updateNotification("Gagal menyalakan server di port $port: ${e.message}")
            }
        }
    }

    private fun acquireLocks() {
        try {
            // Wi-Fi Lock with WIFI_MODE_FULL_HIGH_PERF (PRD 6.9: keeps Wi-Fi fast even with screen off)
            if (wifiLock == null || !wifiLock!!.isHeld) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wifiManager?.createWifiLock(
                    lockMode,
                    "PS4PkgSender:WifiLock"
                )?.apply {
                    setReferenceCounted(false)
                    acquire()
                    Log.i(TAG, "WifiLock mode $lockMode acquired")
                }
            }

            // WakeLock with PARTIAL_WAKE_LOCK (keeps CPU alive)
            if (wakeLock == null || !wakeLock!!.isHeld) {
                val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "PS4PkgSender:WakeLock"
                )?.apply {
                    setReferenceCounted(false)
                    acquire(12 * 60 * 60 * 1000L) // 12 hours max safety timeout
                    Log.i(TAG, "WakeLock acquired")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed acquiring locks", e)
        }
    }

    private fun releaseLocks() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                Log.i(TAG, "WifiLock released")
            }
        } catch (_: Exception) {}
        wifiLock = null

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(TAG, "WakeLock released")
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun stopServerAndLocks() {
        httpServer?.stop()
        releaseLocks()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TransferForegroundService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PS4 PKG Sender")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hentikan", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopServerAndLocks()
        super.onDestroy()
    }
}
