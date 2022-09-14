package com.igtools.igdownloader.activities

import android.app.ProgressDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
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
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.MultiTypeAdapter
import com.igtools.igdownloader.api.okhttp.Urls
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.ActivityTagBlogDetailsBinding
import com.igtools.igdownloader.models.IntentEvent
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.models.Record
import com.igtools.igdownloader.models.ResourceModel
import com.igtools.igdownloader.room.RecordDB
import com.igtools.igdownloader.utils.FileUtils
import com.igtools.igdownloader.utils.ShareUtils
import com.igtools.igdownloader.utils.UrlUtils
import com.igtools.igdownloader.utils.getNullable
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class TagBlogDetails : AppCompatActivity() {
    val gson = Gson()
    val TAG = "TagBlogDetails"

    lateinit var binding:ActivityTagBlogDetailsBinding
    lateinit var progressDialog: ProgressDialog
    var mediaInfo = MediaModel()
    var isBack = false
    lateinit var adapter: MultiTypeAdapter
    var mInterstitialAd: InterstitialAd? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_tag_blog_details)
        initAds()
        initViews()
        setListeners()

        if (intent.extras?.getString("content") != null) {
            binding.btnDownload.visibility = View.INVISIBLE
            getDataFromServer(intent.extras!!.getString("content")!!)

        }
        if (intent.extras!!.getBoolean("flag")) {
            binding.btnDownload.visibility = View.VISIBLE
        } else {
            binding.btnDownload.visibility = View.INVISIBLE
        }
    }

    private fun initAds(){
        val adRequest = AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-8609866682652024/8199213476", adRequest,
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

    private fun initViews(){
        adapter = MultiTypeAdapter(this, mediaInfo.resources)
        binding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(this))
            .setAdapter(adapter)
            .isAutoLoop(false)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)


    }

    private fun setListeners(){

        binding.btnDownload.setOnClickListener {
            lifecycleScope.launch {
                val oldRecord = RecordDB.getInstance().recordDao().findById(mediaInfo.code)
                if (oldRecord != null) {
                    Toast.makeText(this@TagBlogDetails, getString(R.string.exist), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                progressDialog.show()
                if (mediaInfo.mediaType==8){
                    val all: List<Deferred<Unit>> = mediaInfo.resources.map {
                        async {
                            download(it)
                        }
                    }

                    all.awaitAll()
                }else{
                    download(mediaInfo)
                }

                //Log.v(TAG,"finish")
                val record =
                    Record(mediaInfo.code, Gson().toJson(mediaInfo), System.currentTimeMillis())
                RecordDB.getInstance().recordDao().insert(record)

                progressDialog.dismiss()
                isBack = false
                mInterstitialAd?.show(this@TagBlogDetails)

                Toast.makeText(
                    this@TagBlogDetails,
                    getString(R.string.download_finish),
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

        binding.imgBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun getDataFromServer(content:String){
        mediaInfo = gson.fromJson(content, MediaModel::class.java)

        getMedia()

    }

    private fun getMedia() {
        val shortCode = mediaInfo.code
        lifecycleScope.launch {
            //检查是否已存在
            val record = RecordDB.getInstance().recordDao().findById(shortCode)

            if (record != null) {
                mediaInfo = gson.fromJson(record.content, MediaModel::class.java)
                updateUI()
                Toast.makeText(this@TagBlogDetails, getString(R.string.exist), Toast.LENGTH_SHORT)
                    .show()

                return@launch
            }
            progressDialog.show()
            try {
                val map: HashMap<String, String> = HashMap()
                val random = (0..2).random()
                map["Cookie"] = Urls.Cookies[random]
                val cookie = ShareUtils.getData("cookie")
                if (cookie != null && cookie.contains("sessionid")) {
                    map["Cookie"] = cookie
                }

                map["User-Agent"] = Urls.USER_AGENT

                val map2: HashMap<String, String> = HashMap()
                map2["shortcode"] = shortCode

                val res = ApiClient.getClient()
                    .getMediaData(Urls.MEDIA_INFO, map, Urls.QUERY_HASH, gson.toJson(map2))
                val code = res.code()
                val jsonObject = res.body()
                progressDialog.dismiss()
                if (code == 200 && jsonObject != null) {
                    mediaInfo = parseMedia(jsonObject)
                    saveRecord(shortCode)
                    updateUI()

                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(this@TagBlogDetails, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                }

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(this@TagBlogDetails, getString(R.string.parse_error), Toast.LENGTH_SHORT).show()

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

    private fun saveRecord(id: String) {
        lifecycleScope.launch {
            val record = Record(id, gson.toJson(mediaInfo), System.currentTimeMillis())
            RecordDB.getInstance().recordDao().insert(record)
        }

    }

    private fun updateUI() {

        if (mediaInfo.mediaType == 8) {
            binding.imgPlay.visibility = View.INVISIBLE
            binding.banner.visibility = View.VISIBLE
            binding.picture.visibility = View.INVISIBLE
            adapter.setDatas(mediaInfo.resources)

        } else if (mediaInfo.mediaType == 1){
            binding.picture.visibility = View.VISIBLE
            binding.banner.visibility = View.INVISIBLE
            binding.imgPlay.visibility = View.INVISIBLE
            Glide.with(this).load(mediaInfo.thumbnailUrl).placeholder(ColorDrawable(ContextCompat.getColor(this, R.color.gray_1))).into(binding.picture)
        }else if (mediaInfo.mediaType == 2){
            binding.picture.visibility = View.VISIBLE
            binding.banner.visibility = View.INVISIBLE
            binding.imgPlay.visibility = View.VISIBLE
            Glide.with(this).load(mediaInfo.thumbnailUrl).placeholder(ColorDrawable(ContextCompat.getColor(this, R.color.gray_1))).into(binding.picture)
        }

        Glide.with(this).load(mediaInfo.profilePicUrl)
            .placeholder(ColorDrawable(ContextCompat.getColor(this, R.color.gray_1)))
            .circleCrop().into(binding.avatar)
        binding.username.text = mediaInfo.username
        binding.tvTitle.text = mediaInfo.captionText

    }

    private suspend fun download(media: ResourceModel?) = withContext(Dispatchers.IO) {

        if (media?.mediaType == 1) {
            //image
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")
            val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl!!)
            saveFile(responseBody.body(), file, 1)

        } else if (media?.mediaType == 2) {
            //video
            val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            if (media.videoUrl != null) {
                val responseBody = ApiClient.getClient().downloadUrl(media.videoUrl!!)
                saveFile(responseBody.body(), file, 2)

            }

        }
    }

    private fun saveFile(body: ResponseBody?, file: File, type: Int) {
        if (body == null) {
            return
        }
        var input: InputStream? = null
        try {
            input = body.byteStream()

            val fos = FileOutputStream(file)
            fos.use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
            //存到相册
            if (type == 1) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                FileUtils.saveImageToAlbum(this, bitmap, file.name)
            } else {
                FileUtils.saveVideoToAlbum(this, file)
            }
            Log.v(TAG, file.absolutePath)

        } catch (e: Exception) {
            Log.e("saveFile", e.toString())
        } finally {
            input?.close()
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