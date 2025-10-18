package com.tpn.displaylauncher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class UninstallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra("packageName")
        if (packageName != null) {
            val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(uninstallIntent)
        }

        // Close this activity immediately
        finish()
    }
}
