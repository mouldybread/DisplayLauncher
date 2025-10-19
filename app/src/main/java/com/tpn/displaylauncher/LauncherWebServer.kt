package com.tpn.displaylauncher

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

class LauncherWebServer(port: Int, private val appLauncher: AppLauncher) : NanoHTTPD(port) {

    private val gson = Gson()
    private val TAG = "LauncherWebServer"

    override fun serve(session: IHTTPSession): Response {
        return try {
            val uri = session.uri
            val method = session.method

            Log.d(TAG, "Request: $method $uri")

            when {
                uri == "/" -> serveWebUI()
                uri == "/api/apps" && method == Method.GET -> getApps()
                uri == "/api/launch" && method == Method.POST -> launchApp(session)
                uri == "/api/launch-intent" && method == Method.POST -> launchAppWithIntent(session)
                uri == "/api/uninstall" && method == Method.POST -> uninstallApp(session)
                uri == "/api/upload-apk" && method == Method.POST -> uploadApk(session)
                uri == "/api/health" && method == Method.GET -> healthCheck()
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error serving request: ${e.message}", e)
            createJsonResponse(false, "Server error: ${e.message}")
        }
    }

    private fun healthCheck(): Response {
        return createJsonResponse(true, "Server is running")
    }

    private fun serveWebUI(): Response {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Display Launcher - Control Panel</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .container {
            max-width: 900px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
        }
        h1 { font-size: 28px; margin-bottom: 10px; }
        .subtitle { opacity: 0.9; font-size: 14px; }
        .content { padding: 30px; }
        .search-box {
            width: 100%;
            padding: 12px 20px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            font-size: 16px;
            margin-bottom: 20px;
            transition: border-color 0.3s;
        }
        .search-box:focus {
            outline: none;
            border-color: #667eea;
        }
        .upload-section {
            margin-bottom: 20px;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 8px;
            border: 1px solid #e0e0e0;
        }
        .upload-section h2 {
            font-size: 18px;
            margin-bottom: 12px;
            color: #333;
        }
        .file-input {
            display: block;
            width: 100%;
            padding: 10px;
            margin-bottom: 10px;
            border: 2px dashed #667eea;
            border-radius: 6px;
            background: white;
        }
        .app-list { display: grid; gap: 12px; }
        .app-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 16px 20px;
            background: #f8f9fa;
            border-radius: 8px;
            border: 1px solid #e0e0e0;
            transition: all 0.3s;
        }
        .app-item:hover {
            border-color: #667eea;
            box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
        }
        .app-info h3 {
            font-size: 16px;
            margin-bottom: 4px;
            color: #333;
        }
        .app-info p {
            font-size: 12px;
            color: #666;
        }
        .app-actions {
            display: flex;
            gap: 8px;
        }
        .launch-btn {
            padding: 10px 24px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 14px;
            font-weight: 600;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .launch-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        }
        .launch-btn:active {
            transform: translateY(0);
        }
        .uninstall-btn {
            padding: 10px 20px;
            background: #dc3545;
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 14px;
            font-weight: 600;
            transition: all 0.2s;
        }
        .uninstall-btn:hover {
            background: #c82333;
            transform: translateY(-2px);
        }
        .loading { text-align: center; padding: 40px; color: #666; }
        .message {
            padding: 12px;
            margin-bottom: 20px;
            border-radius: 6px;
            display: none;
        }
        .message.success { background: #d4edda; color: #155724; display: block; }
        .message.error { background: #f8d7da; color: #721c24; display: block; }
        .status-indicator {
            display: inline-block;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #28a745;
            margin-right: 8px;
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1><span class="status-indicator"></span>🚀 Display Launcher</h1>
            <p class="subtitle">Control Panel - Launch apps remotely via API</p>
        </div>
        <div class="content">
            <div id="message" class="message"></div>
            
            <div class="upload-section">
                <h2>📦 Install APK</h2>
                <input type="file" id="apkFile" accept=".apk" class="file-input">
                <button onclick="uploadApk()" class="launch-btn">Upload & Install</button>
            </div>
            
            <input type="text" id="searchBox" class="search-box" placeholder="Search apps...">
            <div id="appList" class="loading">Loading apps...</div>
        </div>
    </div>
    
    <script>
        let allApps = [];
        let healthCheckInterval;
        
        async function loadApps() {
            try {
                const response = await fetch('/api/apps');
                allApps = await response.json();
                renderApps(allApps);
                startHealthCheck();
            } catch (error) {
                document.getElementById('appList').innerHTML = '<p style="color: red;">Error loading apps. Server may be down.</p>';
                setTimeout(loadApps, 5000);
            }
        }
        
        function startHealthCheck() {
            if (healthCheckInterval) clearInterval(healthCheckInterval);
            healthCheckInterval = setInterval(async () => {
                try {
                    const response = await fetch('/api/health');
                    if (!response.ok) throw new Error('Health check failed');
                } catch (error) {
                    console.error('Server health check failed');
                }
            }, 30000);
        }
        
        function renderApps(apps) {
            const appList = document.getElementById('appList');
            if (apps.length === 0) {
                appList.innerHTML = '<p style="text-align: center; color: #666;">No apps found</p>';
                return;
            }
            
            appList.className = 'app-list';
            appList.innerHTML = apps.map(function(app) {
                return '<div class="app-item"><div class="app-info"><h3>' + app.name + '</h3><p>' + app.packageName + '</p></div><div class="app-actions"><button class="launch-btn" onclick="launchApp(\'' + app.packageName.replace(/'/g, "\\'") + '\', \'' + app.name.replace(/'/g, "\\'") + '\')">Launch</button><button class="uninstall-btn" onclick="uninstallApp(\'' + app.packageName.replace(/'/g, "\\'") + '\', \'' + app.name.replace(/'/g, "\\'") + '\')">Uninstall</button></div></div>';
            }).join('');
        }
        
        async function launchApp(packageName, appName) {
            try {
                const response = await fetch('/api/launch', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ packageName: packageName })
                });
                
                const result = await response.json();
                showMessage(result.success ? 'success' : 'error', 
                           result.success ? 'Launched ' + appName : result.message);
            } catch (error) {
                showMessage('error', 'Failed to launch app. Server may be down.');
            }
        }
        
        async function uninstallApp(packageName, appName) {
            if (!confirm('Uninstall ' + appName + '?')) return;
            
            try {
                const response = await fetch('/api/uninstall', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ packageName: packageName })
                });
                
                const result = await response.json();
                showMessage(result.success ? 'success' : 'error', result.message);
                
                if (result.success) {
                    setTimeout(loadApps, 2000);
                }
            } catch (error) {
                showMessage('error', 'Failed to uninstall app. Server may be down.');
            }
        }
        
        async function uploadApk() {
            const fileInput = document.getElementById('apkFile');
            if (!fileInput.files.length) {
                showMessage('error', 'Please select an APK file');
                return;
            }
            
            showMessage('success', 'Uploading APK...');
            
            const formData = new FormData();
            formData.append('file', fileInput.files[0]);
            
            try {
                const response = await fetch('/api/upload-apk', {
                    method: 'POST',
                    body: formData
                });
                
                const result = await response.json();
                showMessage(result.success ? 'success' : 'error', result.message);
                
                if (result.success) {
                    fileInput.value = '';
                    setTimeout(loadApps, 3000);
                }
            } catch (error) {
                showMessage('error', 'Failed to upload APK. Server may be down.');
            }
        }
        
        function showMessage(type, text) {
            const msg = document.getElementById('message');
            msg.className = 'message ' + type;
            msg.textContent = text;
            setTimeout(function() { msg.className = 'message'; }, 3000);
        }
        
        document.getElementById('searchBox').addEventListener('input', function(e) {
            const query = e.target.value.toLowerCase();
            const filtered = allApps.filter(function(app) {
                return app.name.toLowerCase().includes(query) || 
                       app.packageName.toLowerCase().includes(query);
            });
            renderApps(filtered);
        });
        
        loadApps();
    </script>
</body>
</html>
        """

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun getApps(): Response {
        return try {
            val apps = appLauncher.getInstalledApps()
            val json = gson.toJson(apps)
            newFixedLengthResponse(Response.Status.OK, "application/json", json)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting apps: ${e.message}", e)
            createJsonResponse(false, "Error: ${e.message}")
        }
    }

    private fun launchApp(session: IHTTPSession): Response {
        val map = HashMap<String, String>()
        try {
            session.parseBody(map)
            val body = map["postData"] ?: ""
            val jsonObject = gson.fromJson(body, JsonObject::class.java)
            val packageName = jsonObject.get("packageName")?.asString

            if (packageName.isNullOrEmpty()) {
                return createJsonResponse(false, "Package name is required")
            }

            val success = appLauncher.launchApp(packageName)
            return if (success) {
                createJsonResponse(true, "App launched successfully")
            } else {
                createJsonResponse(false, "Failed to launch app")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: ${e.message}", e)
            return createJsonResponse(false, "Error: ${e.message}")
        }
    }

    private fun launchAppWithIntent(session: IHTTPSession): Response {
        val map = HashMap<String, String>()
        try {
            session.parseBody(map)
            val body = map["postData"] ?: ""
            val jsonObject = gson.fromJson(body, JsonObject::class.java)

            val packageName = jsonObject.get("packageName")?.asString
            val action = jsonObject.get("action")?.asString
            val data = jsonObject.get("data")?.asString

            if (packageName.isNullOrEmpty()) {
                return createJsonResponse(false, "Package name is required")
            }

            val success = appLauncher.launchAppWithIntent(packageName, action, data)
            return if (success) {
                createJsonResponse(true, "App launched successfully with intent")
            } else {
                createJsonResponse(false, "Failed to launch app with intent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app with intent: ${e.message}", e)
            return createJsonResponse(false, "Error: ${e.message}")
        }
    }

    private fun uninstallApp(session: IHTTPSession): Response {
        val map = HashMap<String, String>()
        try {
            session.parseBody(map)
            val body = map["postData"] ?: ""
            val jsonObject = gson.fromJson(body, JsonObject::class.java)
            val packageName = jsonObject.get("packageName")?.asString

            if (packageName.isNullOrEmpty()) {
                return createJsonResponse(false, "Package name is required")
            }

            val success = appLauncher.uninstallApp(packageName)
            return if (success) {
                createJsonResponse(true, "Uninstall dialog opened")
            } else {
                createJsonResponse(false, "Failed to open uninstall dialog")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling app: ${e.message}", e)
            return createJsonResponse(false, "Error: ${e.message}")
        }
    }

    private fun uploadApk(session: IHTTPSession): Response {
        var apkFile: File? = null
        try {
            val files = HashMap<String, String>()
            session.parseBody(files)

            val tempFile = files["file"]
            if (tempFile == null) {
                return createJsonResponse(false, "No file uploaded")
            }

            val apkDir = File(appLauncher.context.cacheDir, "apk")
            if (!apkDir.exists()) apkDir.mkdirs()

            apkFile = File(apkDir, "uploaded_${System.currentTimeMillis()}.apk")
            File(tempFile).copyTo(apkFile, overwrite = true)

            File(tempFile).delete()

            val success = appLauncher.installApkFromFile(apkFile)

            return if (success) {
                createJsonResponse(true, "Install dialog opened for uploaded APK")
            } else {
                createJsonResponse(false, "Failed to open install dialog")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading APK: ${e.message}", e)
            apkFile?.delete()
            return createJsonResponse(false, "Error: ${e.message}")
        }
    }

    private fun createJsonResponse(success: Boolean, message: String): Response {
        val response = mapOf("success" to success, "message" to message)
        val json = gson.toJson(response)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }
}
