package com.tpn.displaylauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

data class AppInfo(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean
)

class AppLauncher(private val context: Context) {

    @Suppress("QueryPermissionsNeeded")
    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        return packages
            .filter { it.packageName != context.packageName }
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { appInfo ->
                AppInfo(
                    name = packageManager.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.name }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            Log.d("AppLauncher", "Attempting to launch: $packageName")

            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)

                context.startActivity(launchIntent)
                Log.d("AppLauncher", "Successfully launched: $packageName")
                true
            } else {
                Log.e("AppLauncher", "No launch intent found for: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e("AppLauncher", "Failed to launch $packageName: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }
}