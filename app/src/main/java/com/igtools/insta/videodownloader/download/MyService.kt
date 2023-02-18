package com.igtools.insta.videodownloader.download

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.igtools.insta.videodownloader.BaseApplication
import com.igtools.insta.videodownloader.MainActivity
import com.igtools.insta.videodownloader.R
import com.igtools.insta.videodownloader.download.DownloadFail
import com.igtools.insta.videodownloader.download.DownloadProgress
import com.igtools.insta.videodownloader.download.DownloadSuccess
import com.igtools.insta.videodownloader.models.MediaModel
import com.igtools.insta.videodownloader.models.Record
import com.igtools.insta.videodownloader.room.RecordDB
import com.igtools.insta.videodownloader.utils.Analytics
import com.igtools.insta.videodownloader.utils.FileUtils
import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.breakpoint.BlockInfo
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.listener.DownloadListener4
import com.liulishuo.okdownload.core.listener.assist.Listener4Assist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File


class MyService : Service() {

    companion object {
        var isDownloading = false
    }

    var data: MediaModel? = null
    var url: String? = null
    var code: String? = null
    val INDEX_TAG = 1

    val gson = Gson()
    val TAG = "MyService"
    var receiver = 1
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    val NOTIFICATION_ID = 1
    val CHANNLE_ID = "com.igtools"
    val CHANNEL_NAME = "insta.videodownloader"
    override fun onCreate() {
        super.onCreate()

    }


    private fun startForegroundService(){

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //创建NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNLE_ID,
                CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        //Android9.0会报Caused by: java.lang.SecurityException: Permission Denial:
        //android.permission.FOREGROUND_SERVICE---AndroidManifest.xml中需要申请此权限
        val notification = getNotification()

        startForeground(NOTIFICATION_ID, notification)

    }


    private fun getNotification(): Notification {

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)


        val builder: Notification.Builder = Notification.Builder(this)
            .setSmallIcon(R.mipmap.icon)
            .setContentTitle(getString(R.string.my_app_name))
            .setContentText(getString(R.string.downloading)) //标题和内容可以不加
            .setAutoCancel(true)
            .setProgress(0,0,true)
            .setContentIntent(pendingIntent)
        //设置Notification的ChannelID,否则不能正常显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNLE_ID)
        }

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isDownloading) {
            Toast.makeText(this, R.string.downloading, Toast.LENGTH_SHORT).show()
            return super.onStartCommand(intent, flags, startId)
        }
        intent?.extras?.let {
            val dataStr = it.getString("data")
            url = it.getString("url")
            receiver = it.getInt("receiver")
            code = it.getString("code")
            if (dataStr != null) {
                data = gson.fromJson(dataStr, MediaModel::class.java)

                isDownloading = true
                //开启前台service
                startForegroundService()

                if (data!!.mediaType == 8) {
                    downloadMultiple(data!!)
                } else {
                    Log.v(TAG,"-------download single-----")
                    downloadSingle(data!!)
                }
            }

        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun downloadSingle(mediaInfo: MediaModel) {

        val paths: HashMap<String, String> = HashMap()
        var totalLen: Long = 0

        val parentFile = createDirDownload()
        val task: DownloadTask
        if (mediaInfo.mediaType == 1) {
            task = DownloadTask.Builder(mediaInfo.thumbnailUrl, parentFile)
                .setConnectionCount(1)
                // the minimal interval millisecond for callback progress
                .setMinIntervalMillisCallbackProcess(16)
                // ignore the same task has already completed in the past.
                .setPassIfAlreadyCompleted(false)
                .build()
            task.addTag(INDEX_TAG, 1)
        } else {
            task = DownloadTask.Builder(mediaInfo.videoUrl!!, parentFile)
                .setConnectionCount(1)
                // the minimal interval millisecond for callback progress
                .setMinIntervalMillisCallbackProcess(16)
                // ignore the same task has already completed in the past.
                .setPassIfAlreadyCompleted(false)
                .build()
            task.addTag(INDEX_TAG, 2)
        }

        task.enqueue(object : DownloadListener4() {
            override fun taskStart(task: DownloadTask) {
                Toast.makeText(this@MyService, R.string.download_start, Toast.LENGTH_SHORT).show()
            }

            override fun connectStart(
                task: DownloadTask,
                blockIndex: Int,
                requestHeaderFields: MutableMap<String, MutableList<String>>
            ) {

            }

            override fun connectEnd(
                task: DownloadTask,
                blockIndex: Int,
                responseCode: Int,
                responseHeaderFields: MutableMap<String, MutableList<String>>
            ) {

            }

            override fun taskEnd(
                task: DownloadTask,
                cause: EndCause?,
                realCause: java.lang.Exception?,
                model: Listener4Assist.Listener4Model
            ) {

                Log.e(TAG, task.url)
                if (realCause != null) {

                    Analytics.sendException(
                        "download_fail",
                        "download_fail_" + Analytics.ERROR_KEY,
                        task.url
                    )
                    isDownloading = false

//                    mBinding.progressbar.visibility = View.INVISIBLE
//                    mBinding.progressbar.setValue(0f)
                    EventBus.getDefault().post(DownloadFail())
                    Toast.makeText(this@MyService, R.string.download_failed, Toast.LENGTH_SHORT)
                        .show()
                    stopForeground(true)
                    return
                }
                val tempFile = task.file
                if (tempFile != null && tempFile.exists()) {
                    if (task.getTag(INDEX_TAG) == 1) {
                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        if (bitmap != null) {
                            val path = FileUtils.saveImageToAlbum(BaseApplication.mContext, bitmap)
                            if (path != null) {
                                paths[task.url] = path
                            }
                            tempFile.delete()
                        }
                    } else {
                        tempFile.inputStream().use {
                            val path = FileUtils.saveVideoToAlbum(BaseApplication.mContext, it)
                            if (path != null) {
                                paths[task.url] = path
                            }
                        }
                        tempFile.delete()
                    }

                    scope.launch {
                        saveRecord(paths)
                    }
                    isDownloading = false
//                    mBinding.progressbar.visibility = View.INVISIBLE
//                    mBinding.progressbar.setValue(0f)

                    EventBus.getDefault().post(DownloadSuccess())

                    Toast.makeText(this@MyService, R.string.download_finish, Toast.LENGTH_SHORT)
                        .show()
                    stopForeground(true)
                }

            }


            override fun infoReady(
                task: DownloadTask?,
                info: BreakpointInfo,
                fromBreakpoint: Boolean,
                model: Listener4Assist.Listener4Model
            ) {
                totalLen = info.totalLength
            }

            override fun progressBlock(
                task: DownloadTask?,
                blockIndex: Int,
                currentBlockOffset: Long
            ) {

            }

            override fun progress(task: DownloadTask?, currentOffset: Long) {
                //Log.v(TAG, "current thread is：" + Thread.currentThread().name)
                val percent = (currentOffset.toFloat()) * 100 / totalLen
                //mBinding.progressbar.setValue(percent)
                EventBus.getDefault().post(DownloadProgress(receiver, percent))
            }

            override fun blockEnd(task: DownloadTask?, blockIndex: Int, info: BlockInfo?) {

            }

        })

    }

    private fun downloadMultiple(mediaInfo: MediaModel) {
        val paths: HashMap<String, String> = HashMap()
        val fileDir = createDirDownload()
        var currentCount = 0
        var totalCount = 0

        val builder = DownloadContext.QueueSet()
            .setMinIntervalMillisCallbackProcess(300)
            .commit()
        var validCount = 0
        for (res in mediaInfo.resources) {
            var url: String? = null
            if (res.mediaType == 1) {
                url = res.thumbnailUrl
            } else {
                if (res.videoUrl != null) {
                    url = res.videoUrl!!
                } else {
//                    Analytics.sendEvent(
//                        "video_url_null",
//                        "video_url_null_" + Analytics.EVENT_KEY,
//                        mBinding.etShortcode.text.toString()
//                    )
                }
            }
            if (url == null) {
                continue
            }

            val taskBuilder = DownloadTask.Builder(url, fileDir)
                .setConnectionCount(1)
                // the minimal interval millisecond for callback progress
                .setMinIntervalMillisCallbackProcess(16)
                // ignore the same task has already completed in the past.
                .setPassIfAlreadyCompleted(false)

            builder.bind(taskBuilder).addTag(INDEX_TAG, res.mediaType)
            validCount++
        }

        totalCount = validCount
        currentCount = 0

        val downloadContext = builder.setListener(object : DownloadContextListener {
            override fun taskEnd(
                context: DownloadContext,
                task: DownloadTask,
                cause: EndCause,
                realCause: java.lang.Exception?,
                remainCount: Int
            ) {
                Log.v(TAG, "task:$task is finished")

                currentCount++
                //mBinding.progressbar.setValue(currentCount.toFloat() * 100 / totalCount)
                EventBus.getDefault()
                    .post(DownloadProgress(receiver, currentCount.toFloat() * 100 / totalCount))
                if (realCause != null) {
                    Analytics.sendException(
                        "download_fail",
                        "download_fail_" + Analytics.ERROR_KEY,
                        task.url
                    )
                    //safeToast(R.string.download_failed)
                    return
                }
                val tempFile = task.file
                if (tempFile != null && tempFile.exists()) {
                    if (task.getTag(INDEX_TAG) == 1) {

                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        if (bitmap != null) {
                            val path = FileUtils.saveImageToAlbum(BaseApplication.mContext, bitmap)
                            if (path != null) {
                                paths[task.url] = path
                            }
                            tempFile.delete()
                        }

                    } else {
                        tempFile.inputStream().use {
                            val path = FileUtils.saveVideoToAlbum(BaseApplication.mContext, it)
                            if (path != null) {
                                paths[task.url] = path
                            }
                        }
                        tempFile.delete()
                    }
                }

            }

            override fun queueEnd(context: DownloadContext) {

                scope.launch {
                    saveRecord(paths)
                }
                isDownloading = false
//                mBinding.progressbar.visibility = View.INVISIBLE
//                mBinding.progressbar.setValue(0f)
                EventBus.getDefault().post(DownloadSuccess())
                Toast.makeText(this@MyService, R.string.download_finish, Toast.LENGTH_SHORT).show()
                stopForeground(true)

            }

        }).build()

        downloadContext?.start(null, false)

    }


    private fun createDirDownload(): File {

        val fileDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }

        return fileDir
    }

    suspend fun saveRecord(paths: HashMap<String, String>) {

        val curRecord =
            Record(
                null,
                gson.toJson(data),
                System.currentTimeMillis(),
                url,
                code,
                gson.toJson(paths)
            )
        RecordDB.getInstance().recordDao().insert(curRecord)

    }

    override fun onDestroy() {

        stopForeground(true)
        job.cancel()
        super.onDestroy()
    }
}