package com.tpn.displaylauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

data class AppInfo(
    val name: String,
    val packageName: String
)

class AppLauncher(val context: Context) {

    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .mapNotNull { appInfo ->
                try {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    AppInfo(appName, appInfo.packageName)
                } catch (e: Exception) {
                    null
                }
            }
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

    fun launchAppWithIntent(packageName: String, action: String? = null, data: String? = null, extras: Map<String, String>? = null): Boolean {
        return try {
            val intent = if (action != null) {
                Intent(action).apply {
                    if (data != null && data.isNotEmpty()) {
                        setData(Uri.parse(data))
                        setPackage(packageName)
                    } else {
                        // For MAIN action without data, target the launcher activity directly
                        if (action == Intent.ACTION_MAIN) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                // Add intent extras
                                extras?.forEach { (key, value) ->
                                    launchIntent.putExtra(key, value)
                                }
                                context.startActivity(launchIntent)
                                return true
                            }
                        }
                        setPackage(packageName)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    // Add intent extras
                    extras?.forEach { (key, value) ->
                        putExtra(key, value)
                    }
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    // Add intent extras
                    extras?.forEach { (key, value) ->
                        putExtra(key, value)
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
