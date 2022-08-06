package com.igtools.downloader.activities

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.igtools.downloader.R
import com.igtools.downloader.adapter.MultiTypeAdapter
import com.igtools.downloader.api.okhttp.OkhttpHelper
import com.igtools.downloader.api.okhttp.OkhttpListener
import com.igtools.downloader.api.okhttp.OnDownloadListener
import com.igtools.downloader.api.okhttp.Urls
import com.igtools.downloader.api.retrofit.ApiClient
import com.igtools.downloader.databinding.ActivityBlogDetailsBinding
import com.igtools.downloader.models.MediaModel
import com.igtools.downloader.models.Record
import com.igtools.downloader.room.RecordDB
import com.igtools.downloader.utils.DateUtils
import com.igtools.downloader.utils.FileUtils
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class BlogDetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityBlogDetailsBinding
    lateinit var adapter: MultiTypeAdapter
    var TAG = "BlogDetailsActivity"
    var progressList: ArrayList<Int> = ArrayList()
    var medias: ArrayList<MediaModel> = ArrayList()
    val gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_blog_details)

        initViews()
        setListeners()

        val shortCode = intent.extras?.getString("shortCode")
        if (shortCode != null) {

            getData(shortCode)

        } else if (intent.extras?.getString("content") != null) {

            getDataFromLocal(intent.extras!!.getString("content")!!)

        }


    }


    private fun initViews() {
        adapter = MultiTypeAdapter(this, medias)
        binding.banner.addBannerLifecycleObserver(this).setIndicator(CircleIndicator(this))
            .setAdapter(adapter).isAutoLoop(false)

    }


    private fun setListeners() {

        binding.btnDownload.setOnClickListener {

            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {

                val all:List<Deferred<Unit>> = medias.map {
                    async {
                        downloadMedia(it)
                    }
                }

                all.awaitAll()
                Log.v(TAG,"finish")
                val record = Record()
                record.createdTime = DateUtils.getDate(Date())
                record.content = Gson().toJson(medias)
                lifecycleScope.launch {
                    RecordDB.getInstance().recordDao().insert(record)
                }
                binding.progressBar.visibility = View.INVISIBLE

                Toast.makeText(this@BlogDetailsActivity, "download finished", Toast.LENGTH_SHORT).show()


            }

        }
        binding.imgBack.setOnClickListener {
            finish()
        }

    }


    private fun getData(url: String) {
        binding.progressBar.visibility = View.VISIBLE
        //val url = "http://192.168.0.101:3000/api/mediainfo?url=$url"
        val api = Urls.SHORT_CODE + "?url=$url"
        OkhttpHelper.getInstance().getJson(api, object :
            OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject) {

                parseData(jsonObject);
                if (medias.size > 0) {
                    adapter.setDatas(medias)

                    //enable download
                    binding.btnDownload.isEnabled = true
                    binding.btnDownload.setTextColor(resources!!.getColor(R.color.white))
                }

                binding.progressBar.visibility = View.INVISIBLE

            }

            override fun onFail(message: String?) {
                binding.progressBar.visibility = View.INVISIBLE
                Toast.makeText(this@BlogDetailsActivity, message + "", Toast.LENGTH_SHORT).show()
            }

        })

    }

    private fun getDataFromLocal(content: String) {

        medias = gson.fromJson(content, genericType<ArrayList<MediaModel>>())

    }

    private fun parseData(jsonObject: JsonObject) {
        medias.clear()
        progressList.clear()
        val data = jsonObject["data"].asJsonObject
        val mediaType = data["media_type"].asInt

        if (mediaType == 8) {

            val resources = data["resources"].asJsonArray
            for (res in resources) {

                val mediaInfo = MediaModel()
                mediaInfo.mediaType = res.asJsonObject["media_type"].asInt
                mediaInfo.thumbnailUrl = res.asJsonObject["thumbnail_url"].asString
                if (!res.asJsonObject["video_url"].isJsonNull) {
                    mediaInfo.videoUrl = res.asJsonObject["video_url"]?.asString
                }
                if (!data["caption_text"].isJsonNull) {
                    mediaInfo.title = data["caption_text"]?.asString
                }
                progressList.add(0)
                medias.add(mediaInfo)
            }
        } else if (mediaType == 1 || mediaType == 2) {

            val mediaInfo = MediaModel()
            mediaInfo.mediaType = mediaType
            mediaInfo.thumbnailUrl = data["thumbnail_url"].asString
            if (!data["video_url"].isJsonNull) {
                mediaInfo.videoUrl = data["video_url"]?.asString
                //mediaInfo.videoUrl = "https://ssyttest.oss-cn-hangzhou.aliyuncs.com/test/myvideo.mp4"
            }
            if (!data["caption_text"].isJsonNull) {
                mediaInfo.title = data["caption_text"]?.asString
            }
            progressList.add(0)
            medias.add(mediaInfo)
        }


    }

    private suspend fun downloadMedia(media: MediaModel) {

        if (media.mediaType == 1) {
            //image
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")

            try {
                val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl)
                responseBody.body()?.let { saveFile(it, file, 1) }

            } catch (e: Error) {
                //errFlag = true
            }


        } else {
            //video
            val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            try {
                val responseBody = media.videoUrl?.let { ApiClient.getClient().downloadUrl(it) }
                responseBody?.body()?.let { saveFile(it, file, 2) }

            } catch (e: Error) {
               // errFlag = true
            }

        }

    }

    private fun saveFile(body: ResponseBody, file: File, type: Int) {

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
            Log.v(TAG,file.absolutePath)

        } catch (e: Exception) {
            Log.e("saveFile", e.toString())
        } finally {
            input?.close()
        }

    }

    inline fun <reified T> genericType() = object : TypeToken<T>() {}.type
}