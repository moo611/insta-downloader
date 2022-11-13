package com.igtools.videodownloader.service.details

import android.app.ProgressDialog
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fagaia.farm.base.BaseActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.R
import com.igtools.videodownloader.api.okhttp.Urls
import com.igtools.videodownloader.api.retrofit.ApiClient
import com.igtools.videodownloader.databinding.ActivityTagBlogDetailsBinding
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.models.ResourceModel
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.utils.ShareUtils
import com.igtools.videodownloader.utils.getNullable
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.*
import java.io.File

class TagBlogDetails : BaseActivity<ActivityTagBlogDetailsBinding>() {

    val TAG = "TagBlogDetails"

    lateinit var progressDialog: ProgressDialog
    lateinit var progressDialog2: ProgressDialog
    lateinit var adapter: MultiTypeAdapter

    var mediaInfo = MediaModel()
    var isBack = false
    var paths = StringBuffer()
    var code: String? = null
    var mInterstitialAd: InterstitialAd? = null

    override fun getLayoutId(): Int {
        return R.layout.activity_tag_blog_details
    }

    override fun initView() {
        initAds()
        adapter = MultiTypeAdapter(this, mediaInfo.resources)
        mBinding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(this))
            .setAdapter(adapter)
            .isAutoLoop(false)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)

        progressDialog2 = ProgressDialog(this)
        progressDialog2.setMessage(getString(R.string.downloading))
        progressDialog2.setCancelable(false)


        mBinding.btnDownload.setOnClickListener {
            isBack = false
            mInterstitialAd?.show(this@TagBlogDetails)
            lifecycleScope.launch {
                val oldRecord = RecordDB.getInstance().recordDao().findByCode(code!!)
                if (oldRecord != null) {
                    Toast.makeText(
                        this@TagBlogDetails,
                        getString(R.string.exist),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                progressDialog2.show()
                if (mediaInfo.mediaType == 8) {
                    val all: List<Deferred<Unit>> = mediaInfo.resources.map {
                        async {
                            download(it)
                        }
                    }

                    all.awaitAll()
                } else {
                    download(mediaInfo)
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

                progressDialog2.dismiss()
                Toast.makeText(
                    this@TagBlogDetails,
                    getString(R.string.download_finish),
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

        mBinding.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun initData() {
        intent.extras?.getBoolean("flag")?.let {
            if (it) {
                mBinding.btnDownload.visibility = View.VISIBLE
                getDataFromServer()
            } else {
                mBinding.btnDownload.visibility = View.INVISIBLE
                getDataFromLocal()
            }
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
        val content = intent.extras!!.getString("content")
        mediaInfo = gson.fromJson(content, MediaModel::class.java)
        code = mediaInfo.code
    }

    private fun getDataFromServer() {
        code = intent.extras!!.getString("code")
        getMedia()

    }

    private fun getMedia() {

        lifecycleScope.launch {
            //检查是否已存在
            val record = RecordDB.getInstance().recordDao().findByCode(code!!)

            if (record != null) {
                mediaInfo = gson.fromJson(record.content, MediaModel::class.java)
                updateUI()
                return@launch
            }
            progressDialog.show()
            try {
                val map: HashMap<String, String> = HashMap()
                map["Cookie"] = BaseApplication.cookie!!
                map["User-Agent"] = Urls.USER_AGENT

                val map2: HashMap<String, String> = HashMap()
                map2["shortcode"] = code!!

                val res = ApiClient.getClient()
                    .getMediaData(Urls.GRAPH_QL, map, Urls.QUERY_HASH, gson.toJson(map2))
                val code = res.code()
                val jsonObject = res.body()
                progressDialog.dismiss()
                if (code == 200 && jsonObject != null) {
                    mediaInfo = parseMedia(jsonObject)
                    updateUI()

                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(
                        this@TagBlogDetails,
                        getString(R.string.not_found),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(
                    this@TagBlogDetails,
                    getString(R.string.parse_error),
                    Toast.LENGTH_SHORT
                ).show()

            }
        }

    }


    private fun parseMedia(jsonObject: JsonObject): MediaModel {
        val mediaModel = MediaModel()
        val shortcode_media = jsonObject["data"].asJsonObject["shortcode_media"].asJsonObject
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
                    val resource = ResourceModel()
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

    private fun updateUI() {

        if (mediaInfo.mediaType == 8) {
            mBinding.imgPlay.visibility = View.INVISIBLE
            mBinding.banner.visibility = View.VISIBLE
            mBinding.picture.visibility = View.INVISIBLE
            adapter.setDatas(mediaInfo.resources)

        } else if (mediaInfo.mediaType == 1) {
            mBinding.picture.visibility = View.VISIBLE
            mBinding.banner.visibility = View.INVISIBLE
            mBinding.imgPlay.visibility = View.INVISIBLE
            Glide.with(this).load(mediaInfo.thumbnailUrl)
                .placeholder(ColorDrawable(ContextCompat.getColor(this, R.color.gray_1)))
                .into(mBinding.picture)
        } else if (mediaInfo.mediaType == 2) {
            mBinding.picture.visibility = View.VISIBLE
            mBinding.banner.visibility = View.INVISIBLE
            mBinding.imgPlay.visibility = View.VISIBLE
            Glide.with(this).load(mediaInfo.thumbnailUrl)
                .placeholder(ColorDrawable(ContextCompat.getColor(this, R.color.gray_1)))
                .into(mBinding.picture)
        }

        Glide.with(this).load(mediaInfo.profilePicUrl)
            .placeholder(ColorDrawable(ContextCompat.getColor(this, R.color.gray_1)))
            .circleCrop().into(mBinding.avatar)
        mBinding.username.text = mediaInfo.username
        mBinding.tvTitle.text = mediaInfo.captionText

    }

    private suspend fun download(media: ResourceModel?) {

        if (media?.mediaType == 1) {
            //image
            val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl)
            withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeStream(responseBody.body()!!.byteStream())
                val path = FileUtils.saveImageToAlbum(this@TagBlogDetails, bitmap)
                if (path!=null){
                    paths.append(path).append(",")
                }

            }

        } else if (media?.mediaType == 2) {
            //video

            media.videoUrl?.let {
                val responseBody = ApiClient.getClient().downloadUrl(it)
                withContext(Dispatchers.IO) {
                    val path = FileUtils.saveVideoToAlbum(this@TagBlogDetails, responseBody.body()!!.byteStream())
                    paths.append(path).append(",")
                }
            }
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


}