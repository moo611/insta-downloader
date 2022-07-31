package com.igtools.downloader.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

object PermissionUtils {
    fun checkPermissionsForReadAndRight(activity: Activity): Boolean {
        val read: Int
        val write: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            read = activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            write = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return read == PackageManager.PERMISSION_GRANTED &&
                    write == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    fun requirePermissionsReadAndWrite(activity: Activity?, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity!!, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            requestCode
        )
    }
}