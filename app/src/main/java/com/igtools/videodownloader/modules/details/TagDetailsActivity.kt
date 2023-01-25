package com.igtools.videodownloader.modules.details

import android.app.ProgressDialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.BuildConfig
import com.igtools.videodownloader.R
import com.igtools.videodownloader.api.ApiClient
import com.igtools.videodownloader.api.Urls
import com.igtools.videodownloader.base.BaseActivity
import com.igtools.videodownloader.databinding.ActivityTagDetailsBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.utils.Analytics
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.utils.PermissionUtils
import com.igtools.videodownloader.utils.getNullable
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URLEncoder

class TagDetailsActivity : BaseActivity<ActivityTagDetailsBinding>() {
    val TAG = "TagDetailsActivity"
    lateinit var adapter: MultiTypeAdapter

    lateinit var searchDialog: ProgressDialog
    lateinit var selectDialog: BottomDialog

    var mediaInfo = MediaModel()
    var recordInfo: Record? = null
    var code: String? = null
    var mInterstitialAd: InterstitialAd? = null
    var paths: HashMap<String, String> = HashMap()
    var needDownload = false
    var isDownloading = true

    var currentCount = 0
    var totalCount = 0
    val INDEX_TAG = 1
    var totalLen: Long = 0

    lateinit var myAlert: AlertDialog
    private val PERMISSION_REQ = 1024
    override fun getLayoutId(): Int {
        return R.layout.activity_tag_details
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
            if (code == null) {
                return@setOnClickListener
            }
            lifecycleScope.launch {

                val oldRecord = RecordDB.getInstance().recordDao().findByCode(code!!)
                if (oldRecord != null) {
                    Toast.makeText(
                        this@TagDetailsActivity,
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
            if (code == null) {
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val oldRecord = RecordDB.getInstance().recordDao().findByCode(code!!)
                if (recordInfo == null && oldRecord == null) {
                    Toast.makeText(
                        this@TagDetailsActivity,
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
                                    this@TagDetailsActivity,
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
                                    this@TagDetailsActivity,
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

            if (code == null) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val oldRecord = RecordDB.getInstance().recordDao().findByCode(code!!)
                if (recordInfo == null && oldRecord == null) {

                    Toast.makeText(
                        this@TagDetailsActivity,
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

    override fun initData() {
        getDataFromLocal()
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

        searchDialog = ProgressDialog(this)
        searchDialog.setMessage(getString(R.string.searching))
        searchDialog.setCancelable(false)

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
                } catch (e: FileNotFoundException) {
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


    private fun shareToInstagram(uri: Uri?, isVideo: Boolean) {
        if (uri == null) {
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = if (isVideo) "video/*" else "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.setPackage("com.instagram.android")
            startActivity(intent)
        } catch (e: Exception) {
            Analytics.sendException(
                "repost_fail",
                "repost_fail_" + Analytics.ERROR_KEY,
                e.message + ""
            )
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show()
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

                if (mediaInfo.mediaType == 8 && mediaInfo.resources.size == 0) {
                    //tag 列表从外侧获取不到sidecar的children
                    getDataFromServer2(1)
                } else if (mediaInfo.mediaType == 2 && mediaInfo.videoUrl == null) {
                    getDataFromServer2(2)
                } else {
                    updateUI()
                }

            } else {
                Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show()
            }

        }
        //获取记录
        intent.extras?.getString("record")?.let {
            recordInfo = gson.fromJson(it, Record::class.java)
        }

    }

    private fun getDatafromServer(type: Int) {
        val sourceUrl = if (type == 1) {
            "https://www.instagram.com/p/$code/"
        } else {
            "https://www.instagram.com/reel/$code/"
        }
        val embedUrl = sourceUrl + "embed/captioned"
        searchDialog.show()

        Thread {
            try {
                val doc: Document = Jsoup.connect(embedUrl).userAgent(Urls.USER_AGENT).get()
                //val doc = Jsoup.parse(html)
                Log.v(TAG, doc.title())
                val scripts = doc.getElementsByTag("script")
                //1.如果extra里面有数据，直接提取
                for (script in scripts) {

                    if (script.data().contains("shortcode_media")) {

                        val data = script.data()
                        val strs = data.split("'extra',")
                        val target = strs[1].substring(0, strs[1].length - 2)
                        Log.v(TAG, target)

                        val jsonObject = JsonParser().parse(target).asJsonObject
                        val shortcode_media = jsonObject["shortcode_media"].asJsonObject

                        mediaInfo = parseMedia(shortcode_media)

                        if (mediaInfo.mediaType == 8) {
                            var hasVideo = false
                            for (res in mediaInfo.resources) {
                                if (res.mediaType == 2) {
                                    hasVideo = true
                                    break
                                }
                            }
                            if (hasVideo) {
                                Analytics.sendEvent("use_a1", "media_type", "GraphSidecar")

                                getMediaData(sourceUrl)
                                return@Thread
                            }

                        }

                        runOnUiThread {

                            searchDialog.dismiss()
                            updateUI()

                        }

                        return@Thread
                    }
                }

                //2.如果extra里面是null

                val embed = doc.getElementsByClass("Embed")[0]
                val mediatype = embed.attr("data-media-type")
                if (mediatype == "GraphImage") {
                    mediaInfo = MediaModel()
                    mediaInfo.mediaType = 1
                    mediaInfo.code = code!!
                    parseImage(doc)

                    runOnUiThread {
                        searchDialog.dismiss()
                        updateUI()
                    }

                } else {
                    //如果extra里面是null，并且不是单个图片，或者是链接失效,则用原来的方法尝试获取
                    Analytics.sendEvent("use_a1", "media_type", mediatype)
                    getMediaData(sourceUrl)
                }

            } catch (e: Exception) {
                //私人账户
                Log.e(TAG, e.message + "")
                getMediaData(sourceUrl)
            }

        }.start()


    }

    private fun getDataFromServer2(type: Int) {
        val sourceUrl = if (type == 1) {
            "https://www.instagram.com/p/$code/"
        } else {
            "https://www.instagram.com/reel/$code/"
        }
        val embedUrl = sourceUrl + "embed/captioned"
        searchDialog.show()

        lifecycleScope.launch {
            try {

//                val headers = HashMap<String, String>()
//                headers["user-agent"] = Urls.USER_AGENT

                val res = ApiClient.getClient().getMediaData3(embedUrl)

                val html = res.body()!!.string()
                val doc: Document = Jsoup.parse(html)
                //2.如果extra里面是null

                val embed = doc.getElementsByClass("Embed")[0]
                val mediatype = embed.attr("data-media-type")
                if (mediatype == "GraphImage") {
                    mediaInfo = MediaModel()
                    mediaInfo.mediaType = 1
                    mediaInfo.code = code!!
                    parseImage(doc)

                    searchDialog.dismiss()
                    updateUI()

                } else if (mediatype == "GraphVideo") {
                    val embedVideo = doc.getElementsByClass("EmbedVideo")
                    if (embedVideo.size > 0) {
                        mediaInfo = MediaModel()
                        mediaInfo.mediaType = 2
                        mediaInfo.code = code!!
                        val videodiv = embedVideo[0]
                        val video = videodiv.getElementsByTag("video")[0]
                        mediaInfo.videoUrl = video.attr("src")
                        parseImage(doc)
                        searchDialog.dismiss()
                        updateUI()


                    } else {
                        //如果extra里面是null，并且不是单个图片，或者是链接失效,则用原来的方法尝试获取
                        Analytics.sendEvent("use_a1", "media_type", mediatype)
                        getMediaData(sourceUrl)
                    }
                } else {
                    //如果extra里面是null，并且不是单个图片，或者是链接失效,则用原来的方法尝试获取
                    Analytics.sendEvent("use_a1", "media_type", mediatype)
                    getMediaData(sourceUrl)
                }
            } catch (e: Exception) {

                getMediaData(sourceUrl)
            }

        }


    }


    private fun updateUI() {

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
            Glide.with(this@TagDetailsActivity).load(mediaInfo.thumbnailUrl)
                .into(mBinding.picture)
            mBinding.btnDownload.isEnabled = true
            //mBinding.btnDownload.setTextColor(resources!!.getColor(R.color.white))
            mBinding.tvTitle.text = mediaInfo.captionText

        }

        mBinding.username.text = mediaInfo.username
        Glide.with(this).load(mediaInfo.profilePicUrl).circleCrop().into(mBinding.avatar)

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

    private fun getMediaData(myUrl: String) {

        lifecycleScope.launch {

            try {
                val urlEncoded = handleUrl(myUrl)
                //val api = "https://app.scrapingbee.com/api/v1/?api_key=${BaseApplication.APIKEY}&url=$urlEncoded&render_js=false&premium_proxy=true&country_code=us"
                val api = "https://api.scrape.do?token=${BaseApplication.APIKEY}&url=$urlEncoded"
                val res = ApiClient.getClient().getMediaNew(api)

                val jsonObject = res.body()
                //Log.v(TAG, jsonObject.toString())
                if (res.code() == 200 && jsonObject != null) {

                    searchDialog.dismiss()
                    val shortcode_media =
                        jsonObject["graphql"].asJsonObject["shortcode_media"].asJsonObject
                    mediaInfo = parseMedia(shortcode_media)
                    updateUI()


                } else {
                    Toast.makeText(this@TagDetailsActivity, R.string.failed, Toast.LENGTH_SHORT)
                        .show()
                    searchDialog.dismiss()

                }

            } catch (e: Exception) {

                Log.e(TAG, e.message + "")
                Toast.makeText(this@TagDetailsActivity, R.string.network, Toast.LENGTH_SHORT).show()
                searchDialog.dismiss()
            }
        }
    }

    private fun parseMedia(shortcode_media: JsonObject): MediaModel {
        val mediaModel = MediaModel()

        val __typename = shortcode_media["__typename"].asString
        if (__typename == "GraphImage") {
            mediaModel.mediaType = 1
        } else if (__typename == "GraphVideo") {
            mediaModel.mediaType = 2
        } else {
            mediaModel.mediaType = 8
        }

        mediaModel.code = shortcode_media["shortcode"].asString
        mediaModel.pk = shortcode_media["id"].asString
        val edge_media_to_caption = shortcode_media["edge_media_to_caption"].asJsonObject
        val edges = edge_media_to_caption["edges"].asJsonArray
        if (edges.size() > 0) {
            mediaModel.captionText = edges[0].asJsonObject["node"].asJsonObject["text"].asString
        }
        mediaModel.videoUrl = shortcode_media.getNullable("video_url")?.asString
        mediaModel.thumbnailUrl = shortcode_media["display_url"].asString
        val owner = shortcode_media["owner"].asJsonObject
        mediaModel.profilePicUrl = owner["profile_pic_url"].asString
        mediaModel.username = owner["username"].asString
        if (shortcode_media.has("edge_sidecar_to_children")) {
            val edge_sidecar_to_children = shortcode_media["edge_sidecar_to_children"].asJsonObject
            val children = edge_sidecar_to_children["edges"].asJsonArray
            if (children.size() > 0) {
                for (child in children) {
                    val resource = MediaModel()
                    resource.pk = child.asJsonObject["node"].asJsonObject["id"].asString
                    resource.thumbnailUrl =
                        child.asJsonObject["node"].asJsonObject["display_url"].asString
                    resource.videoUrl =
                        child.asJsonObject["node"].asJsonObject.getNullable("video_url")?.asString
                    val typeName = child.asJsonObject["node"].asJsonObject["__typename"].asString
                    if (typeName == "GraphImage") {
                        resource.mediaType = 1
                    } else if (typeName == "GraphVideo") {
                        resource.mediaType = 2
                    } else {
                        resource.mediaType = 8
                    }
                    mediaModel.resources.add(resource)
                }
            }
        }

        return mediaModel

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
                Toast.makeText(this@TagDetailsActivity, R.string.download_start, Toast.LENGTH_SHORT)
                    .show()
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
                        "download_fail_" + Analytics.ERROR_KEY,
                        realCause.message + ""
                    )
                    Toast.makeText(
                        this@TagDetailsActivity,
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
                            val path = FileUtils.saveImageToAlbum(this@TagDetailsActivity, bitmap)
                            if (path != null) {
                                paths[task.url] = path
                            }
                            tempFile.delete()
                        }
                    } else {
                        tempFile.inputStream().use {
                            val path = FileUtils.saveVideoToAlbum(this@TagDetailsActivity, it)
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
                        this@TagDetailsActivity,
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
                        "download_fail_" + Analytics.ERROR_KEY,
                        realCause.message + ""
                    )
                    return
                }
                val tempFile = task.file
                if (tempFile != null && tempFile.exists()) {
                    if (task.getTag(INDEX_TAG) == 1) {

                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        if (bitmap != null) {
                            val path = FileUtils.saveImageToAlbum(this@TagDetailsActivity, bitmap)
                            if (path != null) {
                                paths[task.url] = path
                            }
                            tempFile.delete()
                        }

                    } else {
                        tempFile.inputStream().use {
                            val path = FileUtils.saveVideoToAlbum(this@TagDetailsActivity, it)
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
                    this@TagDetailsActivity,
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


    private fun handleUrl(url: String): String {
        val str = "$url?__a=1&__d=dis"
        return URLEncoder.encode(str, "utf-8")
    }

    private fun parseImage(doc: Document) {
        getUserInfo(doc)
        getCaption(doc)
        val imageUrl = doc.getElementsByClass("EmbeddedMediaImage")[0].attr("src")
        //Log.v(TAG, imageUrl)
        mediaInfo.thumbnailUrl = imageUrl
        Log.v(TAG, mediaInfo.toString())
    }

    private fun getUserInfo(doc: Document) {
        if (doc.getElementsByClass("Avatar").size > 0) {
            val a = doc.getElementsByClass("Avatar")[0]
            val img = a.getElementsByTag("img")[0]
            mediaInfo.profilePicUrl = img.attr("src")
        } else if (doc.getElementsByClass("CollabAvatar").size > 0) {
            val a = doc.getElementsByClass("CollabAvatar")[0]
            val img = a.getElementsByTag("img")[0]
            mediaInfo.profilePicUrl = img.attr("src")
        }

        val a2 = doc.getElementsByClass("HeaderText")[0]
        val span = a2.getElementsByTag("span")[0]
        mediaInfo.username = span.text()
    }


    private fun getCaption(doc: Document) {
        if (doc.getElementsByClass("Caption").size > 0) {
            val captionDiv = doc.getElementsByClass("Caption")[0]
            val userEle = captionDiv.child(0)
            val commentsEle = captionDiv.child(captionDiv.childrenSize() - 1)
            userEle.remove()
            commentsEle.remove()
            val content = captionDiv.text()
            Log.v(TAG, content)
            mediaInfo.captionText = content
        }
    }

    override fun onBackPressed() {

        if (mInterstitialAd != null) {
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