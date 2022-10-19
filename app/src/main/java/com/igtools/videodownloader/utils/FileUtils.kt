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

    /**
     * 保存文件到本地
     */
    fun saveFile(
        c: Context,
        body: ResponseBody?,
        file: File,
        type: Int,
        downloadListener: OnDownloadListener?
    ) {
        if (body == null) {
            return
        }
        //var currentLength: Long = 0

        var input: InputStream? = null
        try {
            //var totalLength: Long = body.contentLength()
            //Log.v(TAG,"total len:$totalLength")
            input = body.byteStream()

            val fos = FileOutputStream(file)

            fos.use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    //currentLength += read
                    //Log.v(TAG,"current len:$currentLength")
                    output.write(buffer, 0, read)

                    //计算当前下载百分比，并经由回调传出
                    //downloadListener?.onDownloading(((100 * currentLength / totalLength).toInt()));
                    //当百分比为100时下载结束，调用结束回调，并传出下载后的本地路径
//                    if ((100 * currentLength / totalLength).toInt() == 100) {
//                        downloadListener?.onDownloadSuccess(file.absolutePath); //下载完成
//                    }

                }
                output.flush()
            }
            //存到相册
            if (type == 1) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                saveImageToAlbum(c, bitmap, file)
            } else {
                saveVideoToAlbum(c, file)
            }
            //Log.v(TAG, file.absolutePath)

        } catch (e: Exception) {
            Log.e("saveFile", e.toString())
        } finally {
            input?.close()
        }

    }


    fun saveImageToAlbum(c: Context, bitmap: Bitmap, file: File) {

        if (Build.VERSION.SDK_INT >= 29) {

            var fos: OutputStream? = null
            var imageUri: Uri? = null
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + BaseApplication.folderName
                )
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
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            imageUri?.let { contentResolver.update(it, contentValues, null, null) }

        } else {

            val insertImage: String =
                MediaStore.Images.Media.insertImage(c.contentResolver, bitmap, file.name, null)

            // 发送广播，通知刷新图库的显示
            c.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://$file.name")
                )
            )
        }

    }


    fun saveVideoToAlbum(c: Context, videoFile: File) {

        val uriSavedVideo: Uri?
        val resolver = c.contentResolver
        val valuesVideos = ContentValues()

        uriSavedVideo = if (Build.VERSION.SDK_INT >= 29) {
            valuesVideos.put(
                MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/" + BaseApplication.folderName
            )
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
            pfd.close()
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
    fun saveBitmap(bm: Bitmap, f: File, quality: Int): Boolean {
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
        fileDeleteListener: FileDeleteListener
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

                val count: Int = contentResolver.delete(uri, null, null)

                if (count > 0) {
                    fileDeleteListener.onSuccess()
                } else {
                    fileDeleteListener.onFailed(null)
                }
            } else {
                val isSuccess = File(imgPath).delete()
                if (isSuccess) {
                    fileDeleteListener.onSuccess()
                } else {
                    fileDeleteListener.onFailed(null)
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.message + "")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {

                fileDeleteListener.onFailed(e.userAction.actionIntent.intentSender)

            }
        } finally {
            cursor.close()
        }
    }


    fun deleteVideoUri(
        contentResolver: ContentResolver,
        videoPath: String,
        fileDeleteListener: FileDeleteListener
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

                val count: Int = contentResolver.delete(uri, null, null)

                if (count > 0) {
                    fileDeleteListener.onSuccess()
                } else {
                    fileDeleteListener.onFailed(null)
                }
            } else {
                val isSuccess = File(videoPath).delete()
                if (isSuccess) {
                    fileDeleteListener.onSuccess()
                } else {
                    fileDeleteListener.onFailed(null)
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, e.message + "")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {

                fileDeleteListener.onFailed(e.userAction.actionIntent.intentSender)

            }
        } finally {
            cursor.close()
        }
    }

    interface FileDeleteListener {
        fun onSuccess()
        fun onFailed(intentSender: IntentSender?)
    }

}