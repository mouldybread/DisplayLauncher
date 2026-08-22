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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {

    private lateinit var appLauncher: AppLauncher
    private lateinit var launcherService: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appLauncher = AppLauncher(this)
        appLauncher.cleanupOldApks()

        launcherService = Intent(this, LauncherService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(launcherService)
        } else {
            startService(launcherService)
        }

        setContent {
            LauncherUI()
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore exception
        }
        return "Offline"
    }

    @Composable
    fun LauncherUI() {
        val prefs = remember { getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) }
        var hasSeenUI by remember { mutableStateOf(prefs.getBoolean("has_seen_ui", false)) }
        var showUI by remember { mutableStateOf(false) }
        var lastTapTime by remember { mutableLongStateOf(0L) }
        var tapCount by remember { mutableIntStateOf(0) }
        var selectedIndex by remember { mutableIntStateOf(0) }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        val focusRequester = remember { FocusRequester() }

        val allApps = remember { mutableStateOf(appLauncher.getInstalledApps()) }
        val totalItems = 3 + allApps.value.size
        val ipAddress = remember { getLocalIpAddress() }

        LaunchedEffect(showUI) {
            if (showUI && !hasSeenUI) {
                prefs.edit().putBoolean("has_seen_ui", true).apply()
                hasSeenUI = true
            }
            if (showUI) {
                try {
                    focusRequester.requestFocus()
                } catch (e: Exception) {
                    // Ignore if node is not attached
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (showUI) Color(0xFF667eea) else Color.Black)
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (!showUI) {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastTapTime < 1000) {
                                        tapCount++
                                        if (tapCount >= 3) {
                                            showUI = true
                                            tapCount = 0
                                            selectedIndex = 0
                                            return@onKeyEvent true
                                        }
                                    } else {
                                        tapCount = 1
                                    }
                                    lastTapTime = currentTime
                                    true
                                } else {
                                    if (selectedIndex < 3) {
                                        selectedIndex = 3
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(0)
                                        }
                                    } else {
                                        selectedIndex = (selectedIndex + 1).coerceAtMost(totalItems - 1)
                                        coroutineScope.launch {
                                            listState.animateScrollToItem((selectedIndex - 3).coerceAtLeast(0))
                                        }
                                    }
                                    true
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                if (showUI) {
                                    if (selectedIndex == 3) {
                                        selectedIndex = 0
                                    } else if (selectedIndex > 3) {
                                        selectedIndex -= 1
                                        coroutineScope.launch {
                                            listState.animateScrollToItem((selectedIndex - 3).coerceAtLeast(0))
                                        }
                                    }
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (showUI && selectedIndex in 1..2) {
                                    selectedIndex -= 1
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (showUI && selectedIndex < 2) {
                                    selectedIndex += 1
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                if (showUI) {
                                    when (selectedIndex) {
                                        0 -> {
                                            try {
                                                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                                startActivity(intent)
                                            } catch (e: Exception) {
                                                val intent = Intent(Settings.ACTION_SETTINGS)
                                                startActivity(intent)
                                            }
                                            true
                                        }
                                        1 -> {
                                            startActivity(Intent(Settings.ACTION_SETTINGS))
                                            true
                                        }
                                        2 -> {
                                            showUI = false
                                            true
                                        }
                                        else -> {
                                            val appIndex = selectedIndex - 3
                                            if (appIndex in allApps.value.indices) {
                                                appLauncher.launchApp(allApps.value[appIndex].packageName)
                                                showUI = false
                                            }
                                            true
                                        }
                                    }
                                } else {
                                    false
                                }
                            }
                            KeyEvent.KEYCODE_BACK -> {
                                if (showUI) {
                                    showUI = false
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .then(
                    if (!showUI) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val tapRadius = 200f

                                val distance = kotlin.math.sqrt(
                                    (offset.x - centerX) * (offset.x - centerX) +
                                            (offset.y - centerY) * (offset.y - centerY)
                                )

                                if (distance <= tapRadius) {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastTapTime < 1000) {
                                        tapCount++
                                        if (tapCount >= 3) {
                                            showUI = true
                                            tapCount = 0
                                        }
                                    } else {
                                        tapCount = 1
                                    }
                                    lastTapTime = currentTime
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                .focusable()
        ) {
            if (!showUI && !hasSeenUI) {
                Text(
                    text = "Press DPAD DOWN 3x to show settings",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (showUI) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🚀 Display Launcher",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "http://$ipAddress:9091",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_SETTINGS)
                                        startActivity(intent)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.border(
                                    width = if (selectedIndex == 0) 3.dp else 0.dp,
                                    color = if (selectedIndex == 0) Color.White else Color.Transparent
                                ),
                                colors = if (selectedIndex == 0) {
                                    ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f))
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                Text("Set Default", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    startActivity(Intent(Settings.ACTION_SETTINGS))
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.border(
                                    width = if (selectedIndex == 1) 3.dp else 0.dp,
                                    color = if (selectedIndex == 1) Color.White else Color.Transparent
                                ),
                                colors = if (selectedIndex == 1) {
                                    ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f))
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                Text("Android Settings", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { showUI = false },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.border(
                                    width = if (selectedIndex == 2) 3.dp else 0.dp,
                                    color = if (selectedIndex == 2) Color.White else Color.Transparent
                                ),
                                colors = if (selectedIndex == 2) {
                                    ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f))
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                Text("Hide UI", fontSize = 13.sp)
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(allApps.value) { index, app ->
                            val itemIndex = index + 3
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(
                                            if (itemIndex == selectedIndex) Color.White.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (itemIndex == selectedIndex) 2.dp else 0.dp,
                                            color = if (itemIndex == selectedIndex) Color.White else Color.Transparent
                                        )
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.name,
                                            fontSize = 18.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = app.packageName,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = Color.White.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}