package com.igtools.downloader.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StrictMode
import android.provider.MediaStore
import java.io.*

object FileUtils {


    fun saveImageToAlbum(c: Context, bitmap: Bitmap, fileName: String) {

        if (Build.VERSION.SDK_INT >= 29) {

            var fos: OutputStream? = null
            var imageUri: Uri? = null
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            //use application context to get contentResolver
            val contentResolver = c.contentResolver

            contentResolver.also { resolver ->
                imageUri =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }

            fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }

            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            imageUri?.let { contentResolver.update(it, contentValues, null, null) }

        } else {

            val insertImage: String =
                MediaStore.Images.Media.insertImage(c.contentResolver, bitmap, fileName, null)

            // 发送广播，通知刷新图库的显示
            c.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://$fileName")
                )
            )
        }

    }


    fun saveVideoToAlbum(c: Context, videoFile: File) {

        val uriSavedVideo: Uri?
        val resolver = c.contentResolver
        val valuesVideos = ContentValues()

        uriSavedVideo = if (Build.VERSION.SDK_INT >= 29) {
            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            valuesVideos.put(MediaStore.Video.Media.TITLE, videoFile.name)
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            valuesVideos.put(
                MediaStore.Video.Media.DATE_ADDED,
                System.currentTimeMillis() / 1000
            )
            val collection =
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            resolver.insert(collection, valuesVideos)
        } else {
            valuesVideos.put(MediaStore.Video.Media.TITLE, videoFile.name)
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            valuesVideos.put(
                MediaStore.Video.Media.DATE_ADDED,
                System.currentTimeMillis() / 1000
            )
            valuesVideos.put(MediaStore.Video.Media.DATA, videoFile.absolutePath)
            c.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                valuesVideos
            )
        }

        if (Build.VERSION.SDK_INT >= 29) {
            valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val pfd: ParcelFileDescriptor?
        try {
            pfd = c.contentResolver.openFileDescriptor(uriSavedVideo!!, "w")
            val out = FileOutputStream(pfd!!.fileDescriptor)
            val `in` = FileInputStream(videoFile)
            val buf = ByteArray(8192)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            out.close()
            `in`.close()
            pfd!!.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= 29) {
            valuesVideos.clear()
            valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 0)
            c.contentResolver.update(uriSavedVideo!!, valuesVideos, null, null)
        }

    }


    /** * 将图片存到本地  */
    fun saveBitmap(bm: Bitmap, f:File,quality:Int):Boolean{
        try {
            val out = FileOutputStream(f)
            bm.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.flush()
            out.close()
            return true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    fun share(c:Context,file:File){
        checkFileUriExposure()
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        val uri = Uri.fromFile(file)
        sendIntent.type = "image/*"
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        c.startActivity(Intent.createChooser(sendIntent, "share to"))

    }

    fun share(c:Context,text:String){
        checkFileUriExposure()
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_TEXT,text)
        c.startActivity(Intent.createChooser(sendIntent, "share to"))
    }

    /**
     * 分享前必须执行本代码，主要用于兼容SDK18以上的系统
     */
    private fun checkFileUriExposure() {
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()
    }

}