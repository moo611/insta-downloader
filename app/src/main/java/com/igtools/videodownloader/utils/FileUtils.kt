package com.igtools.videodownloader.utils

import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


object FileUtils {
    val TAG = "FileUtils"
    val folderName = "igtools.videodownloader"
    fun saveImageToAlbum(c: Context, bitmap: Bitmap): String? {
        var filePath: String? = null
        var imageUri: Uri?
        var fos: OutputStream?
        if (Build.VERSION.SDK_INT >= 29) {

//            val dir = c.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
//            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")
            val fileName = System.currentTimeMillis().toString() + ".jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
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
            imageUri?.let {
                filePath = it.toString()
                contentResolver.update(it, contentValues, null, null)
                Log.v(TAG, it.toString())
            }


        } else {

            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val subDir = File(dir, folderName)
            if (!subDir.exists()) {
                subDir.mkdirs()
            }
            val file = File(subDir, System.currentTimeMillis().toString() + ".jpg")
            //1.4.2 fix file not found bug in some device
            if (!file.exists()){
                file.createNewFile()
            }
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


    fun saveVideoToAlbum(c: Context, input: InputStream): String? {
        var filePath: String? = null
        if (Build.VERSION.SDK_INT >= 29) {

//            val dir = c.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
//            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            val fileName = System.currentTimeMillis().toString() + ".mp4"
            val resolver = c.contentResolver
            val valuesVideos = ContentValues()
            valuesVideos.put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/" + folderName
            )
            valuesVideos.put(MediaStore.Video.Media.TITLE, fileName)
            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
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

            uri?.let {
                filePath = it.toString()
                valuesVideos.clear()
                valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 0)
                c.contentResolver.update(uri, valuesVideos, null, null)
            }

        } else {
            val dir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val subDir = File(dir, folderName)
            if (!subDir.exists()) {
                subDir.mkdirs()
            }
            val file = File(subDir, System.currentTimeMillis().toString() + ".mp4")
            //1.4.2 fix file not found bug in some device
            if (!file.exists()){
                file.createNewFile()
            }
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

    fun shareImage(c: Context, path: String) {

        //兼容
        val uri: Uri = if (path.contains("content://")) {
            Uri.parse(path)
        } else {
            val file = File(path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    c, "com.igtools.videodownloader.fileprovider",
                    file
                );
            } else {
                Uri.fromFile(file);
            }

        }

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.type = "image/*"
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        val chooser = Intent.createChooser(sendIntent, "share to")
        c.startActivity(chooser)

    }

    fun shareVideo(c: Context, path: String) {

        //兼容
        val uri: Uri = if (path.contains("content://")) {
            Uri.parse(path)
        } else {
            val file = File(path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    c, "com.igtools.videodownloader.fileprovider",
                    file
                );
            } else {
                Uri.fromFile(file);
            }

        }

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.type = "video/mp4"
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        val chooser = Intent.createChooser(sendIntent, "share to")
        c.startActivity(chooser)

    }

    fun share(c: Context, text: String) {

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_TEXT, text)
        c.startActivity(Intent.createChooser(sendIntent, "share to"))
    }

    fun shareAll(c: Context, paths: ArrayList<String>) {

        val uris = ArrayList<Uri>()
        for (path in paths /* List of the files you want to send */) {
            val uri: Uri = if (path.contains("content://")) {
                Uri.parse(path)
            } else {
                val file = File(path)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        c, "com.igtools.videodownloader.fileprovider",
                        file
                    );
                } else {
                    Uri.fromFile(file);
                }

            }
            uris.add(uri)

        }
        val intent = Intent()
        intent.action = Intent.ACTION_SEND_MULTIPLE
        intent.putExtra(Intent.EXTRA_SUBJECT, "Here are some files.")
        intent.type = "*/*" /* This example is sharing jpeg images. */
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
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
                Log.v(TAG,uri.toString())
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
                Log.v(TAG,uri.toString())
                contentResolver.delete(uri, null, null)
            }
            file.delete()
        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.message + "")

        } finally {
            cursor.close()
        }
    }


}