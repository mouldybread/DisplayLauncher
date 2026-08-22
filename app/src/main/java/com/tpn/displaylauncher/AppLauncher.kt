package com.tpn.displaylauncher

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import java.io.File

data class AppInfo(
    val name: String,
    val packageName: String
)

class AppLauncher(val context: Context) {

    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager

        // Query for apps that have a launcher activity (includes user-installed + system launchable apps)
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)

        return resolveInfos
            .mapNotNull { resolveInfo ->
                try {
                    val activityInfo = resolveInfo.activityInfo
                    val appName = activityInfo.loadLabel(packageManager).toString()
                    val packageName = activityInfo.packageName
                    AppInfo(appName, packageName)
                } catch (e: Exception) {
                    null
                }
            }
            .distinctBy { it.packageName } // Remove duplicates if an app has multiple launcher activities
            .sortedBy { it.name.lowercase() }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun launchAppWithIntent(
        packageName: String,
        action: String? = null,
        data: String? = null,
        extras: Map<String, Any>? = null
    ): Boolean {
        return try {
            val intent = if (action != null) {
                Intent(action).apply {
                    if (!data.isNullOrEmpty()) {
                        setData(data.toUri())
                        setPackage(packageName)
                    } else {
                        // For MAIN action without data, target the launcher activity directly
                        if (action == Intent.ACTION_MAIN) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                // Add intent extras with native type matching
                                extras?.forEach { (key, value) ->
                                    putIntentExtra(launchIntent, key, value)
                                }
                                context.startActivity(launchIntent)
                                return true
                            }
                        }
                        setPackage(packageName)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    extras?.forEach { (key, value) ->
                        putIntentExtra(this, key, value)
                    }
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    extras?.forEach { (key, value) ->
                        putIntentExtra(this, key, value)
                    }
                }
            }

            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("AppLauncher", "Failed to launch with intent: ${e.message}", e)
            false
        }
    }

    // Helper to safely attach native primitive types to an Intent
    private fun putIntentExtra(intent: Intent, key: String, value: Any) {
        when (value) {
            is Boolean -> intent.putExtra(key, value)
            is Int -> intent.putExtra(key, value)
            is Long -> intent.putExtra(key, value)
            is Float -> intent.putExtra(key, value)
            is Double -> intent.putExtra(key, value)
            is String -> intent.putExtra(key, value)
            else -> intent.putExtra(key, value.toString())
        }
    }

    fun uninstallApp(packageName: String): Boolean {
        return try {
            val intent = Intent(context, UninstallActivity::class.java).apply {
                putExtra("packageName", packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.e("DisplayLauncher", "Uninstall error: ${e.message}")
            false
        }
    }

    fun installApkFromFile(apkFile: File): Boolean {
        return try {
            val intent = Intent(context, InstallActivity::class.java).apply {
                putExtra("apkPath", apkFile.absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.e("DisplayLauncher", "Install error: ${e.message}")
            try {
                apkFile.delete()
            } catch (e: Exception) {
                // Ignore
            }
            false
        }
    }

    fun cleanupOldApks() {
        try {
            val apkDir = File(context.cacheDir, "apk")
            if (apkDir.exists()) {
                val now = System.currentTimeMillis()
                apkDir.listFiles()?.forEach { file ->
                    // Delete files older than 10 minutes
                    if (now - file.lastModified() > 600000) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}