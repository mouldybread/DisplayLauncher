package com.tpn.displaylauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri

data class AppInfo(
    val name: String,
    val packageName: String
)

class AppLauncher(private val context: Context) {

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

    fun launchAppWithIntent(packageName: String, action: String? = null, data: String? = null): Boolean {
        return try {
            val intent = if (action != null) {
                Intent(action).apply {
                    if (data != null) {
                        setData(Uri.parse(data))
                    }
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
