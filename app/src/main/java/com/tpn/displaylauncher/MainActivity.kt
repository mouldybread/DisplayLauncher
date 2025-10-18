package com.tpn.displaylauncher

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private var showUI by mutableStateOf(false)
    private var centerButtonPressCount = 0
    private var lastCenterButtonPressTime = 0L
    private var hasShownUIOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        hasShownUIOnce = prefs.getBoolean("has_shown_ui", false)

        val serviceIntent = Intent(this, LauncherService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                HeadlessLauncher(AppLauncher(this), showUI, hasShownUIOnce) {
                    showUI = false
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastCenterButtonPressTime < 500) {
                centerButtonPressCount++
                if (centerButtonPressCount >= 2) {
                    showUI = !showUI
                    centerButtonPressCount = 0

                    if (!hasShownUIOnce && showUI) {
                        hasShownUIOnce = true
                        val prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("has_shown_ui", true).apply()
                    }

                    return true
                }
            } else {
                centerButtonPressCount = 0
            }

            lastCenterButtonPressTime = currentTime
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun HeadlessLauncher(appLauncher: AppLauncher, showUI: Boolean, hasShownUIOnce: Boolean, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (showUI) {
            LauncherUI(appLauncher, onDismiss)
        } else if (!hasShownUIOnce) {
            Text(
                text = "Press center button 3x to show settings",
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        installedApps = appLauncher.getInstalledApps()
        isLoading = false
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus exception
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Text(
            text = "Display Launcher",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Web Interface: http://[device-ip]:9091",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF90CAF9),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var isFocused1 by remember { mutableStateOf(false) }
            var isFocused2 by remember { mutableStateOf(false) }
            var isFocused3 by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused1 = it.isFocused }
                    .border(
                        width = if (isFocused1) 3.dp else 0.dp,
                        color = if (isFocused1) Color(0xFF64B5F6) else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .focusable(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFocused1) Color(0xFF1E88E5) else Color(0xFF1976D2),
                    contentColor = Color.White
                )
            ) {
                Text("Set Default Launcher")
            }

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused2 = it.isFocused }
                    .border(
                        width = if (isFocused2) 3.dp else 0.dp,
                        color = if (isFocused2) Color(0xFF64B5F6) else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .focusable(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFocused2) Color(0xFF1E88E5) else Color(0xFF1976D2),
                    contentColor = Color.White
                )
            ) {
                Text("Android Settings")
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused3 = it.isFocused }
                    .border(
                        width = if (isFocused3) 3.dp else 0.dp,
                        color = if (isFocused3) Color(0xFF64B5F6) else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .focusable(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFocused3) Color(0xFF616161) else Color(0xFF424242),
                    contentColor = Color.White
                )
            ) {
                Text("Hide UI")
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color(0xFF424242)
        )

        Text(
            text = "Installed Apps (${installedApps.size})",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE0E0E0),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF90CAF9))
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
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color(0xFF64B5F6) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .focusable(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2C2C2C) else Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFocused) Color(0xFF64B5F6) else Color(0xFFE0E0E0)
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}
