package com.tpn.displaylauncher

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class LauncherService : Service() {

    private var webServer: LauncherWebServer? = null
    private var restartAttempts = 0
    private val maxRestartAttempts = 5

    // Class-level handler and runnable to prevent duplicate loops and memory leaks
    private val monitoringHandler = Handler(Looper.getMainLooper())
    private val monitoringRunnable = object : Runnable {
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
            monitoringHandler.postDelayed(this, 60000)
        }
    }

    companion object {
        private const val TAG = "LauncherService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "LauncherServiceChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Some devices disable BootReceiver behind the app's back, which takes it out
    // of the BOOT_COMPLETED resolution set and stops the app ever starting at
    // boot. A shell cannot undo that for an app that is not test-only, and
    // reinstalling loses the app's adb key, but the app may set its own
    // components, so it is repaired here on every service start.
    private fun ensureBootReceiverEnabled() {
        try {
            val receiver = ComponentName(this, BootReceiver::class.java)
            // Anything that is not enabled, rather than the one disabled state:
            // DISABLED_USER and DISABLED_UNTIL_USED keep it out of the resolution
            // set just as surely, and DEFAULT means the manifest value, enabled.
            val state = packageManager.getComponentEnabledSetting(receiver)

            if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED &&
                state != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {

                packageManager.setComponentEnabledSetting(
                    receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                Log.i(TAG, "BootReceiver was disabled by OS. Re-enabled it.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not check or re-enable BootReceiver", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        ensureBootReceiverEnabled()
        createNotificationChannel()

        // Start server monitoring once upon service creation
        startServerMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForegroundService()
        startWebServer()

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

        try {
            // On Android 13+ (API 33), check if POST_NOTIFICATIONS is granted.
            // Note: We still call startForeground() regardless, as the OS requires it
            // to prevent foreground service timeout crashes, but the OS will safely
            // suppress the visual notification if the runtime permission is denied.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission is not granted. Service will run, but notification display will be suppressed.")
            }

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
        }
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
                Handler(Looper.getMainLooper()).postDelayed({
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
        // Clear any existing callbacks just in case, then start monitoring after 60 seconds
        monitoringHandler.removeCallbacks(monitoringRunnable)
        monitoringHandler.postDelayed(monitoringRunnable, 60000)
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

        // Clean up handler callbacks to prevent leaks
        monitoringHandler.removeCallbacks(monitoringRunnable)
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