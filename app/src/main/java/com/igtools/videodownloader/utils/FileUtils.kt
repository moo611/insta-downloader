package com.igtools.videodownloader.utils

import android.app.RecoverableSecurityException
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore

import android.util.Log
import androidx.core.content.FileProvider
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.api.okhttp.OnDownloadListener
import okhttp3.ResponseBody
import java.io.*


object FileUtils {
    val TAG = "FileUtils"
    val folderName = "igtools.videodownloader"
    fun saveImageToAlbum(c: Context, bitmap: Bitmap): String {
        val filePath: String
        var imageUri: Uri?
        var fos: OutputStream?
        if (Build.VERSION.SDK_INT >= 29) {

            val dir = c.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + folderName
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
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
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            imageUri?.let { contentResolver.update(it, contentValues, null, null) }
            filePath = file.absolutePath
        } else {

            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val subDir = File(dir, folderName)
            if (!subDir.exists()) {
                subDir.mkdir()
            }
            val file = File(subDir, System.currentTimeMillis().toString() + ".jpg")
            fos = FileOutputStream(file)
            fos.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it);
            }

            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(file)
            mediaScanIntent.data = contentUri
            c.sendBroadcast(mediaScanIntent)
            filePath = file.absolutePath
        }
        return filePath
    }


    fun saveVideoToAlbum(c: Context, input: InputStream): String {
        val filePath: String
        if (Build.VERSION.SDK_INT >= 29) {

            val dir = c.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")

            val resolver = c.contentResolver
            val valuesVideos = ContentValues()
            valuesVideos.put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/" + folderName
            )
            valuesVideos.put(MediaStore.Video.Media.TITLE, file.name)
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            valuesVideos.put(
                MediaStore.Video.Media.DATE_ADDED,
                System.currentTimeMillis() / 1000
            )
            valuesVideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 1)

            val collection =
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, valuesVideos)

            val pfd: ParcelFileDescriptor?
            try {
                pfd = c.contentResolver.openFileDescriptor(uri!!, "w")
                val out = FileOutputStream(pfd!!.fileDescriptor)

                val buf = ByteArray(8192)
                var len: Int
                while (input.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
                out.close()
                input.close()
                pfd.close()
            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
            }
            filePath = file.absolutePath
            valuesVideos.clear()
            valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 0)
            c.contentResolver.update(uri!!, valuesVideos, null, null)

        } else {
            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val subDir = File(dir, folderName)
            if (!subDir.exists()) {
                subDir.mkdir()
            }
            val file = File(subDir, System.currentTimeMillis().toString() + ".mp4")

            val fos = FileOutputStream(file)

            fos.use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }

            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(file)
            mediaScanIntent.data = contentUri
            c.sendBroadcast(mediaScanIntent)
            filePath = file.absolutePath
        }
        return filePath
    }

    fun share(c: Context, file: File) {

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                c, "com.igtools.videodownloader.fileprovider",
                file
            );
        } else {
            Uri.fromFile(file);
        }
        sendIntent.type = "image/*"
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        c.startActivity(Intent.createChooser(sendIntent, "share to"))

    }

    fun shareVideo(c: Context, file: File) {

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND

        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                c, "com.igtools.videodownloader.fileprovider",
                file
            );
        } else {
            Uri.fromFile(file);
        }

        sendIntent.type = "video/mp4"
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        c.startActivity(Intent.createChooser(sendIntent, "share to"))

    }

    fun share(c: Context, text: String) {

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_TEXT, text)
        c.startActivity(Intent.createChooser(sendIntent, "share to"))
    }

    fun shareAll(c: Context, paths: List<String>) {

        val files = ArrayList<Uri>()
        for (path in paths /* List of the files you want to send */) {
            val file = File(path)

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    c, "com.igtools.videodownloader.fileprovider",
                    file
                );
            } else {
                Uri.fromFile(file);
            }
            files.add(uri)

        }
        val intent = Intent()
        intent.action = Intent.ACTION_SEND_MULTIPLE
        intent.putExtra(Intent.EXTRA_SUBJECT, "Here are some files.")
        intent.type = "*/*" /* This example is sharing jpeg images. */
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        c.startActivity(Intent.createChooser(intent, "share to"))
    }


    /**
     * 删除文件
     * @return
     */
    fun deleteImageUri(
        contentResolver: ContentResolver,
        imgPath: String,
    ) {
        val file = File(imgPath)
        val cursor: Cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            MediaStore.MediaColumns.DISPLAY_NAME + "=?",
            arrayOf(file.name),
            null
        ) ?: return

        try {
            if (cursor.moveToFirst()) {
                val id: Long = cursor.getLong(0)
                val contentUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri: Uri = ContentUris.withAppendedId(contentUri, id)

                contentResolver.delete(uri, null, null)
            }
            file.delete()

        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.message + "")

        } finally {
            cursor.close()
        }
    }


    fun deleteVideoUri(
        contentResolver: ContentResolver,
        videoPath: String,
    ) {
        val file = File(videoPath)
        val cursor: Cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media._ID),
            MediaStore.MediaColumns.DISPLAY_NAME + "=?",
            arrayOf(file.name),
            null
        ) ?: return
        try {
            if (cursor.moveToFirst()) {
                val id: Long = cursor.getLong(0)
                val contentUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val uri: Uri = ContentUris.withAppendedId(contentUri, id)
                contentResolver.delete(uri, null, null)
            }
            file.delete()
        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.message + "")

        } finally {
            cursor.close()
        }
    }

    interface FileDeleteListener {
        fun onSuccess()
        fun onFailed(intentSender: IntentSender?)
    }

}