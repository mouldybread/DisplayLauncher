package com.yourcompany.headlesslauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {
    private lateinit var webServer: LauncherWebServer
    private lateinit var appLauncher: AppLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize app launcher
        appLauncher = AppLauncher(this)

        // Start web server on port 9091
        webServer = LauncherWebServer(9091, appLauncher)
        webServer.start()

        setContent {
            MaterialTheme {
                LauncherUI(appLauncher)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webServer.stop()
    }
}

@Composable
fun LauncherUI(appLauncher: AppLauncher) {
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        installedApps = appLauncher.getInstalledApps()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Display Launcher",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Web Interface: http://[device-ip]:9091",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Installed Apps (${installedApps.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(installedApps) { app ->
                    AppListItem(app)
                }
            }
        }
    }
}

@Composable
fun AppListItem(app: AppInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
