package com.tpn.displaylauncher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the foreground service
        val serviceIntent = Intent(this, LauncherService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            MaterialTheme {
                HeadlessLauncher(AppLauncher(this))
            }
        }
    }
}

@Composable
fun HeadlessLauncher(appLauncher: AppLauncher) {
    var showUI by remember { mutableStateOf(false) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Reset tap count after timeout
    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            delay(1000) // 1 second timeout
            tapCount = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val currentTime = System.currentTimeMillis()

                        // Check if tap is in center region (middle 30% of screen)
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val tapRadius = size.width * 0.15f

                        val distance = kotlin.math.sqrt(
                            (offset.x - centerX) * (offset.x - centerX) +
                                    (offset.y - centerY) * (offset.y - centerY)
                        )

                        if (distance <= tapRadius) {
                            // Valid center tap
                            if (currentTime - lastTapTime < 500) { // 500ms between taps
                                tapCount++
                                if (tapCount >= 3) {
                                    showUI = !showUI
                                    tapCount = 0
                                }
                            } else {
                                tapCount = 1
                            }
                            lastTapTime = currentTime
                        }
                    }
                )
            }
    ) {
        if (showUI) {
            LauncherUI(appLauncher) {
                showUI = false
            }
        } else {
            // Show subtle hint in center
            Text(
                text = "Tap center 3x to show settings",
                color = Color.Gray.copy(alpha = 0.3f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun LauncherUI(appLauncher: AppLauncher, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        installedApps = appLauncher.getInstalledApps()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Display Launcher",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Web Interface: http://[device-ip]:9091",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Set Default")
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Hide UI")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
