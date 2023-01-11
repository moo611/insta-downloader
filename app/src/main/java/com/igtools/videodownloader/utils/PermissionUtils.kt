package com.igtools.videodownloader.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.igtools.videodownloader.BuildConfig
import com.igtools.videodownloader.R

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
        return true
    }

    fun requirePermissionsReadAndWrite(activity: Activity, requestCode: Int) {
        //如果已经多次拒绝过或选择了不再提示，fix弹窗不显示问题

        ActivityCompat.requestPermissions(
            activity, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            requestCode
        )
    }


    fun requirePermissionsInFragment(fragment: Fragment, requestCode: Int) {
        //如果已经多次拒绝过或选择了不再提示，fix弹窗不显示问题

        fragment.requestPermissions(arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),requestCode)

    }
}