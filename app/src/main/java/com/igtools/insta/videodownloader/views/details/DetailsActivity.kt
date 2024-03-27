package com.igtools.insta.videodownloader.views.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
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
import com.igtools.insta.videodownloader.download.MyService
import com.youth.banner.indicator.CircleIndicator
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

        adapter = MultiTypeAdapter(this, mediaInfo.resources)
        mBinding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(this))
            .setAdapter(adapter)
            .isAutoLoop(false)


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
            updateUI()

        }
        intent.extras?.getString("url")?.let {
            sourceUrl = it
        }
    }

    private fun updateUI(){
        if (mediaInfo.mediaType == 8) {

            if (mediaInfo.resources.size > 0) {
                show("album")
                adapter.setDatas(mediaInfo.resources as List<MediaModel?>?)

                mBinding.tvTitle.text = mediaInfo.captionText

            }
        } else {
            show("picture")
            Glide.with(this@DetailsActivity).load(mediaInfo.thumbnailUrl)
                .into(mBinding.picture)

            mBinding.tvTitle.text = mediaInfo.captionText

        }

        mBinding.username.text = mediaInfo.username
        Glide.with(this).load(mediaInfo.profilePicUrl).circleCrop().into(mBinding.avatar)

    }

    private fun startDownloadService() {
        mBinding.progressBar.visibility = View.VISIBLE
        val intent = Intent(this, MyService::class.java)
        intent.putExtra("url", sourceUrl)
        intent.putExtra("data", gson.toJson(mediaInfo))
        intent.putExtra("receiver", 1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

    }

    private fun repost(filePath: String?, isVideo: Boolean) {

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

        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        }
        super.onBackPressed()
    }

    @Subscribe
    fun onDownloadSuccess(downloadSuccess: DownloadSuccess){
        if (downloadSuccess.receiver == 1){
            mBinding.progressBar.visibility = View.INVISIBLE
        }
    }

    @Subscribe
    fun onDownloadFail(downloadFail: DownloadFail){
        if (downloadFail.receiver == 1){
            mBinding.progressBar.visibility = View.INVISIBLE
        }
    }

    @Subscribe
    fun onDownloading(downloadProgress: DownloadProgress){

        if (downloadProgress.receiver == 1){
            mBinding.progressBar.setValue(downloadProgress.progress)
        }

    }
}