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
import com.igtools.insta.videodownloader.models.MediaModel
import com.igtools.insta.videodownloader.db.Record
import com.igtools.insta.videodownloader.db.RecordDB
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


class DownloadService : Service() {

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

    /**
     * 下载单个媒体文件。
     *
     * @param mediaInfo 媒体模型，包含媒体的详细信息，如视频URL和缩略图URL。
     */
    private fun downloadSingle(mediaInfo: MediaModel) {

        // 初始化存储下载文件路径的哈希表和总长度
        val paths: HashMap<String, String> = HashMap()
        var totalLen: Long = 0

        // 创建下载文件的父目录
        val parentFile = createDirDownload()
        // 根据媒体类型构建下载任务
        val task: DownloadTask
        if (mediaInfo.mediaType == 1) {
            // 如果是缩略图，则使用缩略图URL构建下载任务
            task = DownloadTask.Builder(mediaInfo.thumbnailUrl, parentFile)
                .setConnectionCount(1)
                // 设置进度回调的最小间隔时间
                .setMinIntervalMillisCallbackProcess(16)
                // 不忽略已经完成的相同任务
                .setPassIfAlreadyCompleted(false)
                .build()
            task.addTag(INDEX_TAG, 1)
        } else {
            // 如果是视频，则使用视频URL构建下载任务
            task = DownloadTask.Builder(mediaInfo.videoUrl!!, parentFile)
                .setConnectionCount(1)
                // 设置进度回调的最小间隔时间
                .setMinIntervalMillisCallbackProcess(16)
                // 不忽略已经完成的相同任务
                .setPassIfAlreadyCompleted(false)
                .build()
            task.addTag(INDEX_TAG, 2)
        }

        // 添加下载监听器
        task.enqueue(object : DownloadListener4() {
            // 下载开始时的处理
            override fun taskStart(task: DownloadTask) {
                Toast.makeText(this@DownloadService, R.string.download_start, Toast.LENGTH_SHORT).show()
            }

            // 连接开始时的处理
            override fun connectStart(
                task: DownloadTask,
                blockIndex: Int,
                requestHeaderFields: MutableMap<String, MutableList<String>>
            ) {

            }

            // 连接结束时的处理
            override fun connectEnd(
                task: DownloadTask,
                blockIndex: Int,
                responseCode: Int,
                responseHeaderFields: MutableMap<String, MutableList<String>>
            ) {

            }

            // 下载结束时的处理
            override fun taskEnd(
                task: DownloadTask,
                cause: EndCause?,
                realCause: java.lang.Exception?,
                model: Listener4Assist.Listener4Model
            ) {
                // 如果下载失败，则记录日志、取消下载状态、发送下载失败事件并停止服务
                if (realCause != null) {
                    isDownloading = false

                    EventBus.getDefault().post(DownloadFail())
                    Toast.makeText(this@DownloadService, R.string.download_failed, Toast.LENGTH_SHORT)
                        .show()
                    stopForeground(true)
                    return
                }

                // 处理下载成功的逻辑，包括保存文件到相册或视频目录，更新记录，并发送下载成功事件
                val tempFile = task.file
                if (tempFile != null && tempFile.exists()) {
                    when (task.getTag(INDEX_TAG)) {
                        1 -> {
                            // 处理缩略图下载
                            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                            if (bitmap != null) {
                                val path = FileUtils.saveImageToAlbum(BaseApplication.mContext, bitmap)
                                if (path != null) {
                                    paths[task.url] = path
                                }
                                tempFile.delete()
                            }
                        }
                        else -> {
                            // 处理视频下载
                            tempFile.inputStream().use {
                                val path = FileUtils.saveVideoToAlbum(BaseApplication.mContext, it)
                                if (path != null) {
                                    paths[task.url] = path
                                }
                            }
                            tempFile.delete()
                        }
                    }

                    // 保存下载记录
                    scope.launch {
                        saveRecord(paths)
                    }
                    isDownloading = false

                    EventBus.getDefault().post(DownloadSuccess())

                    Toast.makeText(this@DownloadService, R.string.download_finish, Toast.LENGTH_SHORT)
                        .show()
                    stopForeground(true)
                }
            }

            // 准备下载信息时的处理，更新总长度
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

            // 下载进度更新时的处理，发送下载进度事件
            override fun progress(task: DownloadTask?, currentOffset: Long) {
                val percent = (currentOffset.toFloat()) * 100 / totalLen
                //mBinding.progressbar.setValue(percent)
                EventBus.getDefault().post(DownloadProgress(receiver, percent))
            }

            // 单个下载块结束时的处理
            override fun blockEnd(task: DownloadTask?, blockIndex: Int, info: BlockInfo?) {

            }

        })

    }


    /**
     * 下载多个媒体文件。
     *
     * @param mediaInfo 包含要下载的媒体资源信息的MediaModel对象。
     */
    private fun downloadMultiple(mediaInfo: MediaModel) {
        // 初始化用于存储下载文件路径的哈希表
        val paths: HashMap<String, String> = HashMap()
        // 创建下载文件的目录
        val fileDir = createDirDownload()
        // 初始化当前已下载文件数量和总文件数量
        var currentCount = 0
        var totalCount = 0

        // 设置下载任务的间隔回调时间并提交
        val builder = DownloadContext.QueueSet()
            .setMinIntervalMillisCallbackProcess(300)
            .commit()
        // 初始化有效下载任务计数器
        var validCount = 0
        // 遍历媒体资源列表，为每个资源创建下载任务
        for (res in mediaInfo.resources) {
            // 根据媒体类型获取下载URL
            var url: String? = null
            if (res.mediaType == 1) {
                url = res.thumbnailUrl
            } else {
                if (res.videoUrl != null) {
                    url = res.videoUrl!!
                }
            }
            // 如果无法获取URL，则跳过当前资源
            if (url == null) {
                continue
            }

            // 构建下载任务设置，并绑定到队列构建器
            val taskBuilder = DownloadTask.Builder(url, fileDir)
                .setConnectionCount(1)
                .setMinIntervalMillisCallbackProcess(16)
                .setPassIfAlreadyCompleted(false)
            builder.bind(taskBuilder).addTag(INDEX_TAG, res.mediaType)
            // 增加有效任务计数
            validCount++
        }

        // 更新总文件数和当前已下载文件数
        totalCount = validCount
        currentCount = 0

        // 设置下载监听器并启动下载
        val downloadContext = builder.setListener(object : DownloadContextListener {
            // 当下载任务结束时的处理逻辑
            override fun taskEnd(
                context: DownloadContext,
                task: DownloadTask,
                cause: EndCause,
                realCause: java.lang.Exception?,
                remainCount: Int
            ) {
                // 记录任务完成信息
                Log.v(TAG, "task:$task is finished")
                // 更新当前已下载文件数
                currentCount++
                // 发送下载进度事件
                EventBus.getDefault()
                    .post(DownloadProgress(receiver, currentCount.toFloat() * 100 / totalCount))
                // 如果下载失败，则不进行后续处理
                if (realCause != null) {
                    return
                }
                // 处理下载完成的文件，根据文件类型保存到相应目录
                val tempFile = task.file
                if (tempFile != null && tempFile.exists()) {
                    if (task.getTag(INDEX_TAG) == 1) {
                        // 处理图片文件
                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        if (bitmap != null) {
                            val path = FileUtils.saveImageToAlbum(BaseApplication.mContext, bitmap)
                            if (path != null) {
                                paths[task.url] = path
                            }
                            // 删除临时文件
                            tempFile.delete()
                        }

                    } else {
                        // 处理视频文件
                        tempFile.inputStream().use {
                            val path = FileUtils.saveVideoToAlbum(BaseApplication.mContext, it)
                            if (path != null) {
                                paths[task.url] = path
                            }
                        }
                        // 删除临时文件
                        tempFile.delete()
                    }
                }

            }

            // 当所有下载任务结束时的处理逻辑
            override fun queueEnd(context: DownloadContext) {
                // 保存下载记录
                scope.launch {
                    saveRecord(paths)
                }
                // 更新下载状态
                isDownloading = false
                // 发送下载成功事件
                EventBus.getDefault().post(DownloadSuccess())
                // 显示下载完成提示
                Toast.makeText(this@DownloadService, R.string.download_finish, Toast.LENGTH_SHORT).show()
                // 停止前台服务
                stopForeground(true)

            }

        }).build()

        // 启动下载上下文
        downloadContext?.start(null, false)

    }


    /**
     * 创建一个用于下载的文件目录。
     * 该函数首先获取外部文件目录下指定的下载目录路径，如果该目录不存在，则创建该目录。
     *
     * @return 返回创建好的或已存在的下载文件目录的File对象。
     */
    private fun createDirDownload(): File {

        val fileDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }

        return fileDir
    }

    /**
     * 保存记录到数据库。
     * 该函数会将当前的数据、时间戳、请求的URL、状态码以及路径信息打包成一个记录对象，并将其保存到数据库中。
     *
     * @param paths 一个包含路径信息的HashMap，键为路径名称，值为路径地址。
     * @suspend 该函数是一个挂起函数，可以在协程中调用，不会阻塞主线程。
     */
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