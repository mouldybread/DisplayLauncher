package com.tpn.displaylauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.FileProvider
import java.io.File

class InstallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apkPath = intent.getStringExtra("apkPath")
        if (apkPath != null) {
            val apkFile = File(apkPath)
            if (apkFile.exists()) {
                val apkUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    apkFile
                )

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(installIntent)

                // Removed the 5-second timer.
                // Temporary files are safely cleaned up later by AppLauncher.cleanupOldApks().
            }
        }

        finish()
    }
}