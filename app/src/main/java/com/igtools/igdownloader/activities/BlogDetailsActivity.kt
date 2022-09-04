package com.igtools.igdownloader.activities

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.google.gson.reflect.TypeToken
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.MultiTypeAdapter
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.ActivityBlogDetailsBinding
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.models.Record
import com.igtools.igdownloader.models.ResourceModel
import com.igtools.igdownloader.room.RecordDB
import com.igtools.igdownloader.utils.DateUtils
import com.igtools.igdownloader.utils.FileUtils
import com.igtools.igdownloader.utils.getNullable
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class BlogDetailsActivity : AppCompatActivity() {

    val gson = Gson()
    val TAG = "BlogDetailsActivity"

    lateinit var binding: ActivityBlogDetailsBinding
    lateinit var adapter: MultiTypeAdapter
    lateinit var progressDialog: ProgressDialog
    var isBack = false
    var mediaInfo = MediaModel()

    var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_blog_details)
        initAds()
        initViews()
        setListeners()

        if (intent.extras?.getString("content") != null) {
            binding.btnDownload.visibility = View.INVISIBLE
            getDataFromLocal(intent.extras!!.getString("content")!!)

        }
        if (intent.extras!!.getBoolean("flag")) {
            binding.btnDownload.visibility = View.VISIBLE
        } else {
            binding.btnDownload.visibility = View.INVISIBLE
        }

    }


    private fun initAds() {
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


    private fun initViews() {

        binding.btnDownload.isEnabled = false
        adapter = MultiTypeAdapter(this, mediaInfo.resources)
        binding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(this))
            .setAdapter(adapter)
            .isAutoLoop(false)
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.downloading))
        progressDialog.setCancelable(false)
    }


    private fun setListeners() {

        binding.btnDownload.setOnClickListener {


            lifecycleScope.launch {

                val oldRecord = RecordDB.getInstance().recordDao().findById(mediaInfo.code)
                if (oldRecord != null) {
                    Toast.makeText(this@BlogDetailsActivity, getString(R.string.exist), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                progressDialog.show()
                val all: List<Deferred<Unit>> = mediaInfo.resources.map {
                    async {
                        downloadMedia(it)
                    }
                }

                all.awaitAll()
                //Log.v(TAG,"finish")
                val record =
                    Record(mediaInfo.code, Gson().toJson(mediaInfo), System.currentTimeMillis())
                RecordDB.getInstance().recordDao().insert(record)

                progressDialog.dismiss()
                isBack = false
                mInterstitialAd?.show(this@BlogDetailsActivity)

                Toast.makeText(
                    this@BlogDetailsActivity,
                    getString(R.string.download_finish),
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

        binding.picture.setOnClickListener {
            if (mediaInfo.mediaType == 2) {
                startActivity(
                    Intent(this, VideoActivity::class.java)
                        .putExtra("url", mediaInfo.videoUrl)
                        .putExtra("thumbnailUrl", mediaInfo.thumbnailUrl)
                )
            }

        }

        binding.imgBack.setOnClickListener {
            onBackPressed()
        }

    }


    private fun getDataFromLocal(content: String) {

        mediaInfo = gson.fromJson(content, MediaModel::class.java)
        if (mediaInfo.mediaType == 8) {

            if (mediaInfo.resources.size > 0) {
                show("album")
                adapter.setDatas(mediaInfo.resources as List<ResourceModel?>?)

                binding.btnDownload.isEnabled = true
                //binding.btnDownload.setTextColor(resources!!.getColor(R.color.white))
                binding.tvTitle.text = mediaInfo.captionText

            }
        } else {
            show("picture")
            Glide.with(this@BlogDetailsActivity).load(mediaInfo.thumbnailUrl)
                .into(binding.picture)
            binding.btnDownload.isEnabled = true
            //binding.btnDownload.setTextColor(resources!!.getColor(R.color.white))
            binding.tvTitle.text = mediaInfo.captionText

        }

        binding.username.text = mediaInfo.username
        Glide.with(this).load(mediaInfo.profilePicUrl).circleCrop().into(binding.avatar)

    }


    private suspend fun downloadMedia(media: ResourceModel?) {

        if (media?.mediaType == 1 || media?.mediaType == 0) {
            //image
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")

            try {
                val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl!!)
                withContext(Dispatchers.IO) {
                    saveFile(responseBody.body(), file, 1)
                }

            } catch (e: Error) {
                //errFlag = true
            }


        } else if (media?.mediaType == 2) {
            //video
            val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            try {
                val responseBody = ApiClient.getClient().downloadUrl(media.videoUrl!!)
                withContext(Dispatchers.IO) {
                    saveFile(responseBody.body(), file, 2)
                }

            } catch (e: Error) {
                // errFlag = true
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

    private fun show(flag: String) {

        if (flag == "picture") {
            binding.picture.visibility = View.VISIBLE
            binding.banner.visibility = View.INVISIBLE

            if (mediaInfo.mediaType == 0 || mediaInfo.mediaType == 1) {
                binding.imgPlay.visibility = View.INVISIBLE
            } else if (mediaInfo.mediaType == 2) {
                binding.imgPlay.visibility = View.VISIBLE
            }

        } else {
            binding.imgPlay.visibility = View.INVISIBLE
            binding.banner.visibility = View.VISIBLE
            binding.picture.visibility = View.INVISIBLE
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