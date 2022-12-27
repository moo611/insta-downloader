package com.igtools.videodownloader.modules.details

import android.app.ProgressDialog
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
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
import com.igtools.videodownloader.R
import com.igtools.videodownloader.api.ApiClient
import com.igtools.videodownloader.databinding.ActivityBlogDetailsBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.*
import java.io.File

class BlogDetailsActivity : BaseActivity<ActivityBlogDetailsBinding>() {

    val TAG = "BlogDetailsActivity"
    lateinit var adapter: MultiTypeAdapter
    lateinit var progressDialog: ProgressDialog
    lateinit var selectDialog: BottomDialog
    var isBack = false
    var mediaInfo = MediaModel()
    var recordInfo: Record? = null
    var code: String? = null
    var mInterstitialAd: InterstitialAd? = null
    var paths: HashMap<String, String> = HashMap()
    var flag: Boolean = false
    var downloadSuccess = true
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
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.downloading))
        progressDialog.setCancelable(false)

        flag = intent.extras!!.getBoolean("flag")
        if (flag) {
            mBinding.btnDownload.visibility = View.VISIBLE
        } else {
            mBinding.btnDownload.visibility = View.INVISIBLE
        }

        mBinding.btnDownload.setOnClickListener {
            isBack = false
            mInterstitialAd?.show(this@BlogDetailsActivity)
            downloadSuccess = true
            lifecycleScope.launch {

                val oldRecord = RecordDB.getInstance().recordDao().findByCode(code!!)
                if (oldRecord != null) {
                    Toast.makeText(
                        this@BlogDetailsActivity,
                        getString(R.string.exist),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                progressDialog.show()
                if (mediaInfo.mediaType == 8) {
                    val all: List<Deferred<Unit>> = mediaInfo.resources.map {
                        async {
                            downloadMedia(it)
                        }
                    }

                    all.awaitAll()
                } else {
                    downloadMedia(mediaInfo)
                }

                if (!downloadSuccess){
                    Toast.makeText(
                        this@BlogDetailsActivity,
                        getString(R.string.download_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    progressDialog.dismiss()
                    return@launch
                }

                //Log.v(TAG,"finish")
                recordInfo =
                    Record(
                        null,
                        Gson().toJson(mediaInfo),
                        System.currentTimeMillis(),
                        null,
                        code,
                        gson.toJson(paths)
                    )
                RecordDB.getInstance().recordDao().insert(recordInfo)

                progressDialog.dismiss()

                Toast.makeText(
                    this@BlogDetailsActivity,
                    getString(R.string.download_finish),
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

        mBinding.imgWall.setOnClickListener {

            if (recordInfo == null) {
                Toast.makeText(this, getString(R.string.download_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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
                        addWallPaperUnder24(filePath)
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.unsupport),
                            Toast.LENGTH_SHORT
                        ).show()

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

            if (flag && recordInfo == null) {

                Toast.makeText(this, getString(R.string.download_first), Toast.LENGTH_SHORT).show()

            } else {
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

    private fun initDialog() {

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

        if (filePath != null) {
            val file = File(filePath)
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    this, "com.igtools.videodownloader.fileprovider",
                    file
                );
            } else {
                Uri.fromFile(file);
            }
            shareFileToInstagram(uri, isVideo)
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

    fun addWallPaper(filePath: String?, status: Int) {
        if (Build.VERSION.SDK_INT >= 24) {
            if (filePath != null) {
                val file = File(filePath)
                val myWallpaperManager = WallpaperManager.getInstance(this);
                when (status) {
                    0 -> {
                        myWallpaperManager.setStream(
                            file.inputStream(),
                            null,
                            true,
                            WallpaperManager.FLAG_SYSTEM
                        );
                        myWallpaperManager.setStream(
                            file.inputStream(),
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
                            file.inputStream(),
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
                            file.inputStream(),
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

            } else {
                Toast.makeText(
                    this,
                    getString(R.string.file_not_found),
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

    }

    fun addWallPaperUnder24(filePath: String?) {

        if (filePath != null) {
            val intent = Intent("android.intent.action.ATTACH_DATA")
            intent.addCategory("android.intent.category.DEFAULT")
            val str = "image/*"
            intent.setDataAndType(Uri.fromFile(File(filePath)), str)
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

    private fun shareFileToInstagram(uri: Uri?, isVideo: Boolean) {
        if (uri == null) {
            return
        }
        val feedIntent = Intent(Intent.ACTION_SEND)
        feedIntent.type = if (isVideo) "video/*" else "image/*"
        feedIntent.putExtra(Intent.EXTRA_STREAM, uri)
        feedIntent.setPackage("com.instagram.android")
        val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY")
        storiesIntent.setDataAndType(uri, if (isVideo) "mp4" else "jpg")
        storiesIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        storiesIntent.setPackage("com.instagram.android")
        grantUriPermission(
            "com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val chooserIntent = Intent.createChooser(feedIntent, "share to")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(storiesIntent))
        startActivity(chooserIntent)
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
                if (isBack) {
                    finish()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                mInterstitialAd = null
                if (isBack) {
                    finish()
                }
            }

        }
    }


    private fun getDataFromLocal() {
        val content = intent.extras!!.getString("content")!!
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

        intent.extras?.getString("record")?.let {
            recordInfo = gson.fromJson(it, Record::class.java)
        }

    }


    private suspend fun downloadMedia(media: MediaModel) {

        if (media.mediaType == 1) {
            //image

            try {
                val responseBody = ApiClient.getClient4().downloadUrl(media.thumbnailUrl)
                withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeStream(responseBody.body()!!.byteStream())
                    val path = FileUtils.saveImageToAlbum(this@BlogDetailsActivity, bitmap)
                    if (path != null) {
                        paths[media.thumbnailUrl] = path
                    }

                }
            } catch (e: Exception) {
                downloadSuccess = false
                sendToFirebase(e)
            }

        } else if (media.mediaType == 2) {
            //video
            media.videoUrl?.let {
                try {
                    val responseBody = ApiClient.getClient4().downloadUrl(it)
                    withContext(Dispatchers.IO) {
                        val path = FileUtils.saveVideoToAlbum(
                            this@BlogDetailsActivity,
                            responseBody.body()!!.byteStream()
                        )
                        paths[it] = path!!

                    }
                } catch (e: Exception) {
                    downloadSuccess = false
                    sendToFirebase(e)
                }

            }

        }

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
        super.onBackPressed()
        isBack = true
//        if (mInterstitialAd == null) {
//            finish()
//        } else {
//            mInterstitialAd?.show(this)
//        }
        finish()

    }

    private fun sendToFirebase(e: Exception) {
        val analytics = Firebase.analytics
        if (e.message != null) {
            analytics.logEvent("app_my_exception") {
                param("my_exception", e.message!!)
            }
        }

    }


}