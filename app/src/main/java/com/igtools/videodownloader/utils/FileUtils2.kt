package com.igtools.videodownloader.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream


object FileUtils2 {
    val TAG="FileUtils2"
    fun saveImageToLocal(c: Context, bitmap: Bitmap): Uri? {

        var uri: Uri? = null
        try {
            val dir = c.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
            uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    c, "com.igtools.videodownloader.fileprovider",
                    file
                );
            } else {
                Uri.fromFile(file);
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message + "")
        }

        return uri
    }

}