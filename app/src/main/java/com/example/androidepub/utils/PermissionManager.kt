package com.example.androidepub.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class PermissionManager(private val activity: Activity) {

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1001
    }

    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+)
            Environment.isExternalStorageManager()
        } else {
            // For Android 10 and below
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ (API 30+), we need to use the MANAGE_EXTERNAL_STORAGE permission
            // This requires sending the user to a system settings page
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${activity.packageName}".toUri()
            )
            activity.startActivity(intent)
        } else {
            // For Android 10 and below
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }

    fun handlePermissionResult(requestCode: Int, grantResults: IntArray): Boolean {
        return when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            }
            else -> false
        }
    }
}
