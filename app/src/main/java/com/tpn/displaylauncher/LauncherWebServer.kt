package com.tpn.displaylauncher

import fi.iki.elonen.NanoHTTPD
import com.google.gson.Gson
import com.google.gson.JsonObject

class LauncherWebServer(port: Int, private val appLauncher: AppLauncher) : NanoHTTPD(port) {

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return when {
            uri == "/" -> serveWebUI()
            uri == "/api/apps" && method == Method.GET -> getApps()
            uri == "/api/launch" && method == Method.POST -> launchApp(session)
            uri == "/api/launch-intent" && method == Method.POST -> launchAppWithIntent(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }
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
        .loading { text-align: center; padding: 40px; color: #666; }
        .message {
            padding: 12px;
            margin-bottom: 20px;
            border-radius: 6px;
            display: none;
        }
        .message.success { background: #d4edda; color: #155724; display: block; }
        .message.error { background: #f8d7da; color: #721c24; display: block; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 Display Launcher</h1>
            <p class="subtitle">Control Panel - Launch apps remotely via API</p>
        </div>
        <div class="content">
            <div id="message" class="message"></div>
            <input type="text" id="searchBox" class="search-box" placeholder="Search apps...">
            <div id="appList" class="loading">Loading apps...</div>
        </div>
    </div>
    
    <script>
        let allApps = [];
        
        async function loadApps() {
            try {
                const response = await fetch('/api/apps');
                allApps = await response.json();
                renderApps(allApps);
            } catch (error) {
                document.getElementById('appList').innerHTML = '<p style="color: red;">Error loading apps</p>';
            }
        }
        
        function renderApps(apps) {
            const appList = document.getElementById('appList');
            if (apps.length === 0) {
                appList.innerHTML = '<p style="text-align: center; color: #666;">No apps found</p>';
                return;
            }
            
            appList.className = 'app-list';
            appList.innerHTML = apps.map(function(app) {
                return '<div class="app-item"><div class="app-info"><h3>' + app.name + '</h3><p>' + app.packageName + '</p></div><button class="launch-btn" onclick="launchApp(\'' + app.packageName.replace(/'/g, "\\'") + '\', \'' + app.name.replace(/'/g, "\\'") + '\')">Launch</button></div>';
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
                showMessage('error', 'Failed to launch app');
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
        val apps = appLauncher.getInstalledApps()
        val json = gson.toJson(apps)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
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
            return createJsonResponse(false, "Error: ${e.message}")
        }
    }

    private fun createJsonResponse(success: Boolean, message: String): Response {
        val response = mapOf("success" to success, "message" to message)
        val json = gson.toJson(response)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }
}
