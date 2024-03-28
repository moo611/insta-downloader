package com.igtools.insta.videodownloader.views.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.igtools.insta.videodownloader.R
import com.igtools.insta.videodownloader.base.BaseActivity
import com.igtools.insta.videodownloader.databinding.ActivityDetailsBinding

import com.igtools.insta.videodownloader.models.MediaModel
import com.igtools.insta.videodownloader.db.RecordDB
import com.igtools.insta.videodownloader.download.DownloadFail
import com.igtools.insta.videodownloader.download.DownloadProgress
import com.igtools.insta.videodownloader.download.DownloadSuccess
import com.igtools.insta.videodownloader.download.DownloadService
import com.youth.banner.indicator.CircleIndicator
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

class DetailsActivity : BaseActivity<ActivityDetailsBinding>() {

    val TAG = "BlogDetailsActivity"
    lateinit var adapter: MultiTypeAdapter
    var mediaInfo = MediaModel()
    var sourceUrl: String? = null
    var mInterstitialAd: InterstitialAd? = null


    override fun getLayoutId(): Int {
        return R.layout.activity_details
    }

    override fun initView() {
        initAds()

        mBinding.imgBack.setOnClickListener {
            onBackPressed()
        }

        mBinding.imgCaption.setOnClickListener {
            val text = mediaInfo.captionText
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
        }

        mBinding.imgDownload.setOnClickListener {

            startDownloadService()

        }

        mBinding.imgRepost.setOnClickListener {

            if (sourceUrl == null) {
                Toast.makeText(
                    this@DetailsActivity,
                    R.string.file_not_found,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val recordInfo = RecordDB.getInstance().recordDao().findByUrl(sourceUrl!!)

                if (recordInfo == null) {
                    Toast.makeText(
                        this@DetailsActivity,
                        R.string.file_not_found,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                //从本地获取
                if (mediaInfo.mediaType == 8) {

                    val selectIndex = mBinding.banner.currentItem
                    Log.v(TAG, selectIndex.toString())
                    val media = mediaInfo.resources[selectIndex]
                    if (media.mediaType == 1) {
                        val paths = recordInfo.paths
                        val pathMap = gson.fromJson(paths, HashMap::class.java)
                        val filePath = pathMap[media.thumbnailUrl] as? String

                        repost(filePath, false)
                    } else if (media.mediaType == 2) {
                        val paths = recordInfo.paths
                        val pathMap = gson.fromJson(paths, HashMap::class.java)
                        val filePath = pathMap[media.videoUrl] as? String
                        repost(filePath, true)
                    }

                } else {
                    if (mediaInfo.mediaType == 1) {

                        val paths = recordInfo.paths
                        val pathMap = gson.fromJson(paths, HashMap::class.java)
                        val filePath = pathMap[mediaInfo.thumbnailUrl] as? String

                        repost(filePath, false)

                    } else if (mediaInfo.mediaType == 2) {

                        val paths = recordInfo.paths
                        val pathMap = gson.fromJson(paths, HashMap::class.java)
                        val filePath = pathMap[mediaInfo.videoUrl] as? String
                        repost(filePath, true)
                    }
                }


            }


        }

    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)

    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }


    override fun initData() {

        intent.extras?.getString("content")?.let {
            mediaInfo = gson.fromJson(it, MediaModel::class.java)
            initBaseInfo()
            when (mediaInfo.mediaType) {
                1 -> {
                    initPicture()
                }
                2 -> {
                    initVideo()
                }
                else -> {
                    initBanner()
                }
            }

        }
        intent.extras?.getString("url")?.let {
            sourceUrl = it
        }
    }


    /**
     * 启动下载服务。
     * 该方法首先检查当前媒体信息的类型和播放器状态，如果是在播放视频，则暂停视频。
     * 接着，设置进度条可见，并创建一个意图来启动下载服务，将下载的URL、媒体信息和一个标识符传递给服务。
     * 根据Android版本的不同，使用startForegroundService或startService来启动服务。
     */
    private fun startDownloadService() {

        // 如果是视频类型且播放器正在播放，则暂停视频
        if (mediaInfo.mediaType == 2 && player.isInPlayingState) {
            player.onVideoPause()
        }

        // 显示进度条
        mBinding.progressBar.visibility = View.VISIBLE
        // 创建并配置Intent以启动下载服务
        val intent = Intent(this, DownloadService::class.java)
        intent.putExtra("url", sourceUrl)
        intent.putExtra("data", gson.toJson(mediaInfo))
        intent.putExtra("receiver", 1)
        // 根据Android版本选择启动服务的方式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

    }

    /**
     * 重新发布下载完成的文件到Instagram。
     * @param filePath 文件路径，可以是本地文件路径或content:// URI。
     * @param isVideo 指示文件是否为视频。
     * 该方法首先检查文件路径是否为空，然后根据文件路径的格式和Android版本获取文件的Uri。
     * 如果Uri获取成功，则调用shareToInstagram方法分享到Instagram；如果获取失败，则显示文件未找到的提示。
     */
    private fun repost(filePath: String?, isVideo: Boolean) {

        // 检查文件路径是否有效
        if (filePath != null) {
            val uri: Uri = if (filePath.contains("content://")) {
                Uri.parse(filePath)
            } else {
                val file = File(filePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        this, "com.igtools.insta.videodownloader.fileprovider",
                        file
                    );
                } else {
                    Uri.fromFile(file);
                }

            }
            // 分享到Instagram
            shareToInstagram(uri, isVideo)
            //bottomDialog.dismiss()
        } else {
            // 文件路径无效时的处理
            Toast.makeText(
                this,
                getString(R.string.file_not_found),
                Toast.LENGTH_SHORT
            ).show()
            //bottomDialog.dismiss()
        }


    }


    /**
     * 分享文件到Instagram。
     * @param uri 文件的Uri。
     * @param isVideo 指示文件是否为视频。
     * 该方法检查Uri是否为空，如果不为空，则创建一个分享Intent，设置分享类型和Uri，并指定Intent的包名为Instagram。
     * 最后启动分享Intent。
     */
    private fun shareToInstagram(uri: Uri?, isVideo: Boolean) {
        // 检查Uri是否有效
        if (uri == null) {
            return
        }
        try {
            // 创建分享Intent并配置类型和Uri
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = if (isVideo) "video/*" else "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.setPackage("com.instagram.android")
            // 启动分享Intent
            startActivity(intent)
        } catch (e: Exception) {
            // 分享失败时的处理
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show()
        }
    }


    private fun initAds() {
        val adRequest = AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-8609866682652024/2073437875", adRequest,
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


    /**
     * 初始化基础信息，包括标题、用户名和头像。
     */
    private fun initBaseInfo() {

        mBinding.tvTitle.text = mediaInfo.captionText // 设置标题文本
        mBinding.username.text = mediaInfo.username // 设置用户名文本
        Glide.with(this).load(mediaInfo.profilePicUrl).circleCrop()
            .into(mBinding.avatar) // 加载并设置圆形头像

    }

    /**
     * 初始化轮播图。
     */
    private fun initBanner() {
        mBinding.banner.visibility = View.VISIBLE // 设置轮播图可见

        // 创建并设置适配器
        adapter = MultiTypeAdapter(this, mediaInfo.resources)
        mBinding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(this)) // 设置指示器
            .setAdapter(adapter) // 设置适配器
            .isAutoLoop(false) // 禁止自动循环

        adapter.setDatas(mediaInfo.resources as List<MediaModel?>?) // 设置适配器数据源
    }


    /**
     * 初始化图片展示。
     */
    private fun initPicture() {
        mBinding.picture.visibility = View.VISIBLE // 设置图片可见
        Glide.with(this@DetailsActivity).load(mediaInfo.thumbnailUrl)
            .into(mBinding.picture) // 加载并设置图片
    }

    /**
     * 初始化视频播放器。
     */
    private fun initVideo() {

        mBinding.player.visibility = View.VISIBLE // 设置播放器视图可见

        // 设置视频封面
        val imageView = ImageView(this)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP // 设置封面图片缩放类型
        Glide.with(this).load(mediaInfo.thumbnailUrl).placeholder(
            ColorDrawable(
                ContextCompat.getColor(
                    this, R.color.gray_1
                )
            )
        ).into(imageView) // 加载并设置封面图片
        mBinding.player.thumbImageView = imageView // 将封面图片设置给播放器

        // 设置视频播放
        if (mediaInfo.videoUrl != null) {
            mBinding.player.setUp(mediaInfo.videoUrl, true, null) // 设置视频播放源
            mBinding.player.startPlayLogic() // 启动播放逻辑
        }

        mBinding.player.backButton.visibility = View.GONE // 隐藏返回按钮
    }


    override fun onResume() {
        super.onResume()

        mBinding.player.onVideoResume()
    }

    override fun onPause() {
        super.onPause()

        mBinding.player.onVideoPause()

    }

    override fun onBackPressed() {

        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        }
        super.onBackPressed()
    }

    /**
     * 处理下载成功的事件。
     * @param downloadSuccess 包含下载成功信息的事件对象，其中可能包括接收者的标识。
     */
    @Subscribe
    fun onDownloadSuccess(downloadSuccess: DownloadSuccess) {
        // 如果事件指定的接收者为1，则隐藏进度条
        if (downloadSuccess.receiver == 1) {
            mBinding.progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * 处理下载失败的事件。
     * @param downloadFail 包含下载失败信息的事件对象，其中可能包括接收者的标识。
     */
    @Subscribe
    fun onDownloadFail(downloadFail: DownloadFail) {
        // 如果事件指定的接收者为1，则隐藏进度条
        if (downloadFail.receiver == 1) {
            mBinding.progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * 处理下载进度更新的事件。
     * @param downloadProgress 包含下载进度信息的事件对象，其中可能包括接收者的标识和当前下载进度。
     */
    @Subscribe
    fun onDownloading(downloadProgress: DownloadProgress) {
        // 如果事件指定的接收者为1，则更新进度条的进度
        if (downloadProgress.receiver == 1) {
            mBinding.progressBar.setValue(downloadProgress.progress)
        }
    }

}