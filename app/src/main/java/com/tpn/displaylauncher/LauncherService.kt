package com.tpn.displaylauncher

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class LauncherService : Service() {

    private var webServer: LauncherWebServer? = null
    private val TAG = "LauncherService"
    private var restartAttempts = 0
    private val maxRestartAttempts = 5

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "LauncherServiceChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForegroundService()
        startWebServer()

        // Monitor server health
        startServerMonitoring()

        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Display Launcher")
            .setContentText("Web server running on port 9091")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startWebServer() {
        try {
            stopWebServer()

            val appLauncher = AppLauncher(applicationContext)
            webServer = LauncherWebServer(9091, appLauncher)
            webServer?.start()

            Log.d(TAG, "Web server started successfully")
            restartAttempts = 0

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start web server: ${e.message}", e)

            // Attempt restart after delay
            if (restartAttempts < maxRestartAttempts) {
                restartAttempts++
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startWebServer()
                }, 5000) // Wait 5 seconds before retry
            } else {
                Log.e(TAG, "Max restart attempts reached. Service may need manual restart.")
            }
        }
    }

    private fun stopWebServer() {
        try {
            webServer?.stop()
            webServer = null
            Log.d(TAG, "Web server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping web server: ${e.message}", e)
        }
    }

    private fun startServerMonitoring() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                try {
                    // Check if server is still alive
                    if (webServer == null || !webServer!!.isAlive) {
                        Log.w(TAG, "Web server is not running. Attempting restart...")
                        startWebServer()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in server monitoring: ${e.message}", e)
                }

                // Check again in 60 seconds
                handler.postDelayed(this, 60000)
            }
        }

        // Start monitoring after 60 seconds
        handler.postDelayed(runnable, 60000)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Display Launcher Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the web server running"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        stopWebServer()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed, restarting service")

        // Restart the service
        val restartServiceIntent = Intent(applicationContext, this::class.java)
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
    }
}
