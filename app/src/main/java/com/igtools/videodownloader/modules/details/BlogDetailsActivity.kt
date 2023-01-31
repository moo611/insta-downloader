package com.igtools.videodownloader.modules.details

import android.app.ProgressDialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.igtools.videodownloader.base.BaseActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.igtools.videodownloader.BuildConfig
import com.igtools.videodownloader.R
import com.igtools.videodownloader.api.ApiClient
import com.igtools.videodownloader.databinding.ActivityBlogDetailsBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.utils.Analytics
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.utils.PermissionUtils
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.breakpoint.BlockInfo
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.DownloadListener4
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
import com.liulishuo.okdownload.core.listener.assist.Listener4Assist
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

class BlogDetailsActivity : BaseActivity<ActivityBlogDetailsBinding>() {

    val TAG = "BlogDetailsActivity"
    lateinit var adapter: MultiTypeAdapter

    lateinit var selectDialog: BottomDialog

    var mediaInfo = MediaModel()
    var recordInfo: Record? = null
    var code: String? = null
    var mInterstitialAd: InterstitialAd? = null
    var paths: HashMap<String, String> = HashMap()
    var needDownload: Boolean = false
    var isDownloading = true
    var currentCount = 0
    var totalCount = 0
    val INDEX_TAG = 1
    var totalLen: Long = 0

    lateinit var myAlert: AlertDialog
    private val PERMISSION_REQ = 1024

    override fun getLayoutId(): Int {
        return R.layout.activity_blog_details
    }

    override fun initView() {
        initAds()
        initDialog()
        mBinding.btnDownload.isEnabled = false
        adapter = MultiTypeAdapter(this, mediaInfo.resources)
        mBinding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(this))
            .setAdapter(adapter)
            .isAutoLoop(false)

        intent?.extras?.getBoolean("need_download")?.let {
            needDownload = it
        }

        if (needDownload) {
            mBinding.btnDownload.visibility = View.VISIBLE
        } else {
            mBinding.btnDownload.visibility = View.INVISIBLE
        }

        mBinding.btnDownload.setOnClickListener {

            //check permission first
            if (!PermissionUtils.checkPermissionsForReadAndRight(this)) {
                PermissionUtils.requirePermissionsReadAndWrite(this, PERMISSION_REQ)
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val oldRecord = if(code==null) null else RecordDB.getInstance().recordDao().findByCode(code!!)
                if (oldRecord != null) {
                    Toast.makeText(
                        this@BlogDetailsActivity,
                        getString(R.string.exist),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                isDownloading = true

                mBinding.progressBar.visibility = View.VISIBLE

                if (mediaInfo.mediaType == 8) {
                    downloadMultiple(mediaInfo)
                } else {
                    downloadSingle(mediaInfo)
                }

            }

        }

        mBinding.imgWall.setOnClickListener {
            lifecycleScope.launch {
                val oldRecord = if(code==null) null else RecordDB.getInstance().recordDao().findByCode(code!!)
                if (recordInfo == null && oldRecord == null) {
                    Toast.makeText(
                        this@BlogDetailsActivity,
                        getString(R.string.download_first),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                } else {
                    //从user列表重复进入的情况
                    if (recordInfo == null) {
                        recordInfo = oldRecord
                    }
                    if (Build.VERSION.SDK_INT >= 24) {
                        selectDialog.show()
                    } else {
                        if (mediaInfo.mediaType == 8) {
                            val selectIndex = mBinding.banner.currentItem
                            if (mediaInfo.resources.size > 0 && mediaInfo.resources[selectIndex].mediaType == 1) {
                                val media = mediaInfo.resources[selectIndex]
                                val paths = recordInfo!!.paths
                                val pathMap = gson.fromJson(paths, HashMap::class.java)
                                val filePath = pathMap[media.thumbnailUrl] as? String
                                addWallPaperUnder24(filePath)

                            } else {
                                Toast.makeText(
                                    this@BlogDetailsActivity,
                                    getString(R.string.unsupport),
                                    Toast.LENGTH_SHORT
                                ).show()

                            }
                        } else {
                            if (mediaInfo.mediaType == 1) {
                                val paths = recordInfo!!.paths
                                val pathMap = gson.fromJson(paths, HashMap::class.java)
                                val filePath = pathMap[mediaInfo.thumbnailUrl] as? String
                                addWallPaperUnder24(filePath)
                            } else {
                                Toast.makeText(
                                    this@BlogDetailsActivity,
                                    getString(R.string.unsupport),
                                    Toast.LENGTH_SHORT
                                ).show()

                            }
                        }

                    }

                }
            }

        }

        mBinding.picture.setOnClickListener {
            if (mediaInfo.mediaType == 2) {
                startActivity(
                    Intent(this, VideoActivity::class.java)
                        .putExtra("url", mediaInfo.videoUrl)
                        .putExtra("thumbnailUrl", mediaInfo.thumbnailUrl)
                )
            }

        }

        mBinding.imgBack.setOnClickListener {
            onBackPressed()
        }

        mBinding.imgRepost.setOnClickListener {
            lifecycleScope.launch {
                val oldRecord = if(code==null) null else RecordDB.getInstance().recordDao().findByCode(code!!)
                if (recordInfo == null && oldRecord == null) {

                    Toast.makeText(
                        this@BlogDetailsActivity,
                        getString(R.string.download_first),
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    //从tag列表重复进入的情况
                    if (recordInfo == null) {
                        recordInfo = oldRecord
                    }
                    //从本地获取
                    if (mediaInfo.mediaType == 8) {

                        val selectIndex = mBinding.banner.currentItem
                        Log.v(TAG, selectIndex.toString())
                        val media = mediaInfo.resources[selectIndex]
                        if (media.mediaType == 1) {
                            val paths = recordInfo!!.paths
                            val pathMap = gson.fromJson(paths, HashMap::class.java)
                            val filePath = pathMap[media.thumbnailUrl] as? String

                            repost(filePath, false)
                        } else if (media.mediaType == 2) {
                            val paths = recordInfo!!.paths
                            val pathMap = gson.fromJson(paths, HashMap::class.java)
                            val filePath = pathMap[media.videoUrl] as? String
                            repost(filePath, true)
                        }

                    } else {
                        if (mediaInfo.mediaType == 1) {

                            val paths = recordInfo!!.paths
                            val pathMap = gson.fromJson(paths, HashMap::class.java)
                            val filePath = pathMap[mediaInfo.thumbnailUrl] as? String

                            repost(filePath, false)

                        } else if (mediaInfo.mediaType == 2) {

                            val paths = recordInfo!!.paths
                            val pathMap = gson.fromJson(paths, HashMap::class.java)
                            val filePath = pathMap[mediaInfo.videoUrl] as? String
                            repost(filePath, true)
                        }
                    }
                }

            }


        }

    }

    private fun initDialog() {

        myAlert = AlertDialog.Builder(this)
            .setMessage(getString(R.string.need_permission))
            .setPositiveButton(
                R.string.settings
            ) { dialog, _ ->
                val intent = Intent();
                intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS";
                intent.data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                startActivity(intent);
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, _ -> dialog.dismiss() }
            .create()

        selectDialog = BottomDialog(this, R.style.MyDialogTheme)
        val selectView = LayoutInflater.from(this).inflate(R.layout.dialog_select, null)

        val llBoth: LinearLayout = selectView.findViewById(R.id.ll_both)
        val llLock: LinearLayout = selectView.findViewById(R.id.ll_lockscreen)
        val llWallPaper: LinearLayout = selectView.findViewById(R.id.ll_wallpaper)

        selectDialog.setContent(selectView)

        llBoth.setOnClickListener {
            if (mediaInfo.mediaType == 8) {
                val selectIndex = mBinding.banner.currentItem
                if (mediaInfo.resources.size > 0 && mediaInfo.resources[selectIndex].mediaType == 1) {
                    val media = mediaInfo.resources[selectIndex]
                    val paths = recordInfo!!.paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[media.thumbnailUrl] as? String
                    addWallPaper(filePath, 0)

                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }
            } else {
                if (mediaInfo.mediaType == 1) {
                    val paths = recordInfo!!.paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[mediaInfo.thumbnailUrl] as? String
                    addWallPaper(filePath, 0)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }

            selectDialog.dismiss()
        }

        llWallPaper.setOnClickListener {
            if (mediaInfo.mediaType == 8) {
                val selectIndex = mBinding.banner.currentItem
                if (mediaInfo.resources.size > 0 && mediaInfo.resources[selectIndex].mediaType == 1) {
                    val media = mediaInfo.resources[selectIndex]
                    val paths = recordInfo!!.paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[media.thumbnailUrl] as? String
                    addWallPaper(filePath, 1)

                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }
            } else {
                if (mediaInfo.mediaType == 1) {
                    val paths = recordInfo!!.paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[mediaInfo.thumbnailUrl] as? String
                    addWallPaper(filePath, 1)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
            selectDialog.dismiss()
        }
        llLock.setOnClickListener {

            if (mediaInfo.mediaType == 8) {
                val selectIndex = mBinding.banner.currentItem
                if (mediaInfo.resources.size > 0 && mediaInfo.resources[selectIndex].mediaType == 1) {
                    val media = mediaInfo.resources[selectIndex]
                    val paths = recordInfo!!.paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[media.thumbnailUrl] as? String
                    addWallPaper(filePath, 2)

                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }

            } else {

                if (mediaInfo.mediaType == 1) {
                    val paths = recordInfo!!.paths
                    val pathMap = gson.fromJson(paths, HashMap::class.java)
                    val filePath = pathMap[mediaInfo.thumbnailUrl] as? String
                    addWallPaper(filePath, 2)
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.unsupport),
                        Toast.LENGTH_SHORT
                    ).show()

                }

            }

            selectDialog.dismiss()
        }

    }

    override fun initData() {

        getDataFromLocal()
    }

    fun repost(filePath: String?, isVideo: Boolean) {
        Analytics.sendEvent("repost", "repost", "1")
        if (filePath != null) {
            val uri: Uri = if (filePath.contains("content://")) {
                Uri.parse(filePath)
            } else {
                val file = File(filePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        this, "com.igtools.videodownloader.fileprovider",
                        file
                    );
                } else {
                    Uri.fromFile(file);
                }

            }
            shareToInstagram(uri, isVideo)
            //bottomDialog.dismiss()
        } else {
            Toast.makeText(
                this,
                getString(R.string.file_not_found),
                Toast.LENGTH_SHORT
            ).show()
            //bottomDialog.dismiss()
        }


    }

    private fun addWallPaper(filePath: String?, status: Int) {

        Analytics.sendEvent("add_wallpaper", "add_wallpaper", "1")
        if (Build.VERSION.SDK_INT >= 24) {
            if (filePath != null) {
                try {
                    val ios: InputStream = if (filePath.contains("content://")) {
                        val uri = Uri.parse(filePath)
                        contentResolver.openInputStream(uri)!!
                    } else {
                        File(filePath).inputStream()
                    }

                    val myWallpaperManager = WallpaperManager.getInstance(this);
                    when (status) {
                        0 -> {
                            myWallpaperManager.setStream(
                                ios,
                                null,
                                true,
                                WallpaperManager.FLAG_SYSTEM
                            );
                            myWallpaperManager.setStream(
                                ios,
                                null,
                                true,
                                WallpaperManager.FLAG_LOCK
                            );
                            Toast.makeText(
                                this,
                                getString(R.string.add_successfully),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        1 -> {
                            myWallpaperManager.setStream(
                                ios,
                                null,
                                true,
                                WallpaperManager.FLAG_SYSTEM
                            );
                            Toast.makeText(
                                this,
                                getString(R.string.add_successfully),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            myWallpaperManager.setStream(
                                ios,
                                null,
                                true,
                                WallpaperManager.FLAG_LOCK
                            );
                            Toast.makeText(
                                this,
                                getString(R.string.add_successfully),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }catch (e:FileNotFoundException){
                    Toast.makeText(
                        this,
                        getString(R.string.file_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.file_not_found),
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

    }

    private fun addWallPaperUnder24(filePath: String?) {
        Analytics.sendEvent("add_wallpaper", "add_wallpaper", "1")
        if (filePath != null) {
            val intent = Intent("android.intent.action.ATTACH_DATA")
            intent.addCategory("android.intent.category.DEFAULT")
            val str = "image/*"

            val uri = if (filePath.contains("content://")) {
                Uri.parse(filePath)
            } else {
                Uri.fromFile(File(filePath))
            }

            intent.setDataAndType(uri, str)
            intent.putExtra("mimeType", str)
            startActivity(Intent.createChooser(intent, "Set As:"))
        } else {
            Toast.makeText(
                this,
                getString(R.string.file_not_found),
                Toast.LENGTH_SHORT
            ).show()
        }

    }


    private fun shareToInstagram(uri: Uri?, isVideo: Boolean){
        if (uri == null) {
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = if (isVideo) "video/*" else "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.setPackage("com.instagram.android")
            startActivity(intent)
        }catch (e:Exception){
            Analytics.sendException("repost_fail","repost_fail_"+Analytics.ERROR_KEY,e.message+"")
            Toast.makeText(this,R.string.file_not_found,Toast.LENGTH_SHORT).show()
        }
    }


    private fun initAds() {
        val adRequest = AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-8609866682652024/5709188020", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(p0: InterstitialAd) {
                    super.onAdLoaded(p0)
                    mInterstitialAd = p0
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    mInterstitialAd = null;
                }
            })

        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                mInterstitialAd = null
                finish()

            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                mInterstitialAd = null
                finish()

            }

        }
    }


    private fun getDataFromLocal() {
        val content = intent.extras?.getString("content")
        //fix content == null
        if (content == null) {
            Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show()
        } else {
            val res = gson.fromJson(content, MediaModel::class.java)
            if (res != null) {
                mediaInfo = gson.fromJson(content, MediaModel::class.java)
                code = mediaInfo.code
                if (mediaInfo.mediaType == 8) {

                    if (mediaInfo.resources.size > 0) {
                        show("album")
                        adapter.setDatas(mediaInfo.resources as List<MediaModel?>?)

                        mBinding.btnDownload.isEnabled = true
                        //mBinding.btnDownload.setTextColor(resources!!.getColor(R.color.white))
                        mBinding.tvTitle.text = mediaInfo.captionText

                    }
                } else {
                    show("picture")
                    Glide.with(this@BlogDetailsActivity).load(mediaInfo.thumbnailUrl)
                        .into(mBinding.picture)
                    mBinding.btnDownload.isEnabled = true
                    //mBinding.btnDownload.setTextColor(resources!!.getColor(R.color.white))
                    mBinding.tvTitle.text = mediaInfo.captionText

                }

                mBinding.username.text = mediaInfo.username
                Glide.with(this).load(mediaInfo.profilePicUrl).circleCrop().into(mBinding.avatar)
            } else {
                Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show()
            }

        }

        intent.extras?.getString("record")?.let {
            recordInfo = gson.fromJson(it, Record::class.java)
        }

    }


    private fun downloadSingle(mediaInfo: MediaModel) {
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
                Toast.makeText(
                    this@BlogDetailsActivity,
                    R.string.download_start,
                    Toast.LENGTH_SHORT
                ).show()
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

                Log.e(TAG, realCause?.message + "")
                if (realCause != null) {

                    Analytics.sendException(
                        "download_fail",
                        "download_fail_"+Analytics.ERROR_KEY,
                        realCause.message + ""
                    )
                    Toast.makeText(
                        this@BlogDetailsActivity,
                        R.string.download_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                val tempFile = task.file
                if (tempFile != null && tempFile.exists()) {
                    if (task.getTag(INDEX_TAG) == 1) {
                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        if (bitmap != null) {
                            val path = FileUtils.saveImageToAlbum(this@BlogDetailsActivity, bitmap)
                            if (path != null) {
                                paths[task.url] = path
                            }
                            tempFile.delete()
                        }
                    } else {
                        tempFile.inputStream().use {
                            val path = FileUtils.saveVideoToAlbum(this@BlogDetailsActivity, it)
                            if (path != null) {
                                paths[task.url] = path
                            }
                        }
                        tempFile.delete()
                    }

                    lifecycleScope.launch {
                        saveRecord()
                    }
                    isDownloading = false
                    mBinding.progressBar.visibility = View.INVISIBLE
                    mBinding.progressBar.setValue(0f)
                    Toast.makeText(
                        this@BlogDetailsActivity,
                        R.string.download_finish,
                        Toast.LENGTH_SHORT
                    ).show()

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
                Log.v(TAG, "current thread is：" + Thread.currentThread().name)
                val percent = (currentOffset.toFloat()) * 100 / totalLen
                mBinding.progressBar.setValue(percent)
            }

            override fun blockEnd(task: DownloadTask?, blockIndex: Int, info: BlockInfo?) {

            }

        })

    }

    private fun downloadMultiple(mediaInfo: MediaModel) {

        val fileDir = createDirDownload()

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
                    Analytics.sendEvent(
                        "video_url_null",
                        "video_url_null_" + Analytics.EVENT_KEY,
                        code!!
                    )
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
                Log.e(TAG, realCause?.message + "")
                currentCount++
                mBinding.progressBar.setValue(currentCount.toFloat() * 100 / totalCount)
                if (realCause != null) {
                    Analytics.sendException(
                        "download_fail",
                        "download_fail_"+Analytics.ERROR_KEY,
                        realCause.message + ""
                    )
                    return
                }
                val tempFile = task.file
                if (tempFile != null && tempFile.exists()) {
                    if (task.getTag(INDEX_TAG) == 1) {

                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        //bitmap may be null
                        if (bitmap != null) {
                            val path = FileUtils.saveImageToAlbum(this@BlogDetailsActivity, bitmap)
                            if (path != null) {
                                paths[task.url] = path
                            }
                            tempFile.delete()
                        }

                    } else {
                        tempFile.inputStream().use {
                            val path = FileUtils.saveVideoToAlbum(this@BlogDetailsActivity, it)
                            if (path != null) {
                                paths[task.url] = path
                            }
                        }
                        tempFile.delete()
                    }
                }

            }

            override fun queueEnd(context: DownloadContext) {

                lifecycleScope.launch {
                    saveRecord()
                }
                isDownloading = false
                mBinding.progressBar.visibility = View.INVISIBLE
                mBinding.progressBar.setValue(0f)
                Toast.makeText(
                    this@BlogDetailsActivity,
                    R.string.download_finish,
                    Toast.LENGTH_SHORT
                ).show()


            }

        }).build()

        downloadContext?.start(null, false)

    }

    suspend fun saveRecord() {

        recordInfo =
            Record(
                null,
                gson.toJson(mediaInfo),
                System.currentTimeMillis(),
                null,
                code,
                gson.toJson(paths)
            )
        RecordDB.getInstance().recordDao().insert(recordInfo)

    }

    fun createDirDownload(): File {

        val fileDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }

        return fileDir
    }

    private fun show(flag: String) {

        if (flag == "picture") {
            mBinding.picture.visibility = View.VISIBLE
            mBinding.banner.visibility = View.INVISIBLE

            if (mediaInfo.mediaType == 0 || mediaInfo.mediaType == 1) {
                mBinding.imgPlay.visibility = View.INVISIBLE
            } else if (mediaInfo.mediaType == 2) {
                mBinding.imgPlay.visibility = View.VISIBLE
            }

        } else {
            mBinding.imgPlay.visibility = View.INVISIBLE
            mBinding.banner.visibility = View.VISIBLE
            mBinding.picture.visibility = View.INVISIBLE
        }

    }

    override fun onBackPressed() {

        if(mInterstitialAd != null){
            mInterstitialAd?.show(this)
        }
        super.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQ) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    myAlert.show()
                    return
                }
            }
        }

    }

}