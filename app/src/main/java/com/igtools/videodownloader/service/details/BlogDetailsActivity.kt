package com.igtools.videodownloader.service.details

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.Toast
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
import com.igtools.videodownloader.api.retrofit.ApiClient
import com.igtools.videodownloader.databinding.ActivityBlogDetailsBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.models.ResourceModel
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.utils.FileUtils
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.*

class BlogDetailsActivity : BaseActivity<ActivityBlogDetailsBinding>() {

    val TAG = "BlogDetailsActivity"
    lateinit var adapter: MultiTypeAdapter
    lateinit var progressDialog: ProgressDialog

    var isBack = false
    var mediaInfo = MediaModel()
    var code: String? = null
    var mInterstitialAd: InterstitialAd? = null
    var paths = StringBuffer()

    override fun getLayoutId(): Int {
        return R.layout.activity_blog_details
    }

    override fun initView() {
        initAds()
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

        if (intent.extras!!.getBoolean("flag")) {
            mBinding.btnDownload.visibility = View.VISIBLE
        } else {
            mBinding.btnDownload.visibility = View.INVISIBLE
        }

        mBinding.btnDownload.setOnClickListener {
            isBack = false
            mInterstitialAd?.show(this@BlogDetailsActivity)

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

                //Log.v(TAG,"finish")
                val record =
                    Record(
                        null,
                        Gson().toJson(mediaInfo),
                        System.currentTimeMillis(),
                        null,
                        code,
                        paths.toString()
                    )
                RecordDB.getInstance().recordDao().insert(record)

                progressDialog.dismiss()

                Toast.makeText(
                    this@BlogDetailsActivity,
                    getString(R.string.download_finish),
                    Toast.LENGTH_SHORT
                ).show()

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

    }

    override fun initData() {

        getDataFromLocal()
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
                adapter.setDatas(mediaInfo.resources as List<ResourceModel?>?)

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

    }


    private suspend fun downloadMedia(media: ResourceModel?) {

        if (media?.mediaType == 1) {
            //image

            try {
                val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl)
                withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeStream(responseBody.body()!!.byteStream())
                    val path = FileUtils.saveImageToAlbum(this@BlogDetailsActivity, bitmap)
                    if (path!=null){
                        paths.append(path).append(",")
                    }

                }
            }catch (e:Exception){
                Toast.makeText(this, "time out", Toast.LENGTH_SHORT).show()
                sendToFirebase(e)
            }

        } else if (media?.mediaType == 2) {
            //video
            media.videoUrl?.let {
                try {
                    val responseBody = ApiClient.getClient().downloadUrl(it)
                    withContext(Dispatchers.IO) {
                        val path = FileUtils.saveVideoToAlbum(
                            this@BlogDetailsActivity,
                            responseBody.body()!!.byteStream()
                        )
                        paths.append(path).append(",")

                    }
                }catch (e:Exception){
                    Toast.makeText(this, "time out", Toast.LENGTH_SHORT).show()
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
        if (mInterstitialAd == null) {
            finish()
        } else {
            mInterstitialAd?.show(this)
        }


    }

    private fun sendToFirebase(e:Exception){
        val analytics = Firebase.analytics
        if (e.message!=null){
            analytics.logEvent("app_my_exception"){
                param("my_exception", e.message!!)
            }
        }

    }


}