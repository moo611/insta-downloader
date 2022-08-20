package com.igtools.igdownloader.activities

import android.app.ProgressDialog
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
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.MultiTypeAdapter
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.ActivityBlogDetailsBinding
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.models.Record
import com.igtools.igdownloader.room.RecordDB
import com.igtools.igdownloader.utils.DateUtils
import com.igtools.igdownloader.utils.FileUtils
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
    lateinit var binding: ActivityBlogDetailsBinding
    lateinit var adapter: MultiTypeAdapter
    lateinit var progressDialog: ProgressDialog
    var TAG = "BlogDetailsActivity"
    var medias: ArrayList<MediaModel> = ArrayList()
    var isDownloading = false

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
            binding.btnDownload.visibility=View.VISIBLE
            getData(shortCode)

        } else if (intent.extras?.getString("content") != null) {
            binding.btnDownload.visibility=View.INVISIBLE
            getDataFromLocal(intent.extras!!.getString("content")!!)

        }


    }


    private fun initViews() {
        binding.btnDownload.isEnabled = false
        adapter = MultiTypeAdapter(this, medias)
        binding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(this))
            .setAdapter(adapter)
            .isAutoLoop(false)
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.downloading))
    }


    private fun setListeners() {

        binding.btnDownload.setOnClickListener {
            if (isDownloading){
                return@setOnClickListener
            }
            isDownloading = true
            progressDialog.show()
            lifecycleScope.launch {

                val all:List<Deferred<Unit>> = medias.map {
                    async {
                        downloadMedia(it)
                    }
                }

                all.awaitAll()
                //Log.v(TAG,"finish")
                val record = Record()
                record.createdTime = DateUtils.getDate(Date())
                record.content = Gson().toJson(medias)

                RecordDB.getInstance().recordDao().insert(record)

                progressDialog.dismiss()
                isDownloading = false
                Toast.makeText(this@BlogDetailsActivity, getString(R.string.download_finish), Toast.LENGTH_SHORT).show()


            }

        }
        binding.imgBack.setOnClickListener {
            finish()
        }

    }


    private fun getData(url: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val res = ApiClient.getClient().getShortCode(url)

                val code = res.code()
                if (code!=200){
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this@BlogDetailsActivity, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val jsonObject = res.body()
                if (jsonObject!=null){
                    parseData(jsonObject);
                    if (medias.size > 0) {
                        adapter.setDatas(medias as List<MediaModel?>?)

                        //enable download
                        binding.btnDownload.isEnabled = true
                        binding.btnDownload.setTextColor(resources!!.getColor(R.color.white))
                        binding.tvTitle.text = medias[0].title
                    }

                    binding.progressBar.visibility = View.INVISIBLE
                }
            }catch (e:Exception){
                binding.progressBar.visibility = View.INVISIBLE
                Toast.makeText(this@BlogDetailsActivity, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }


        }
    }

    private fun getDataFromLocal(content: String) {

        medias = gson.fromJson(content, genericType<ArrayList<MediaModel>>())
        if (medias.size > 0) {
            adapter.setDatas(medias as List<MediaModel?>?)

            //enable download
            binding.btnDownload.isEnabled = true
            binding.btnDownload.setTextColor(resources!!.getColor(R.color.white))
            binding.tvTitle.text = medias[0].title
        }

    }

    private fun parseData(jsonObject: JsonObject) {
        medias.clear()

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
                mediaInfo.avatar = data["user"].asJsonObject["profile_pic_url"].asString
                mediaInfo.username = data["user"].asJsonObject["username"].asString

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
            mediaInfo.avatar = data["user"].asJsonObject["profile_pic_url"].asString
            mediaInfo.username = data["user"].asJsonObject["username"].asString

            medias.add(mediaInfo)
        }


    }

    private suspend fun downloadMedia(media: MediaModel?) {

        if (media?.mediaType == 1) {
            //image
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")

            try {
                val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl)
                withContext(Dispatchers.IO) {
                    saveFile(responseBody.body(), file, 1)
                }

            } catch (e: Error) {
                //errFlag = true
            }


        } else {
            //video
            val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            try {
                val responseBody = ApiClient.getClient().downloadUrl(media?.videoUrl!!)
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
            Log.v(TAG,file.absolutePath)

        } catch (e: Exception) {
            Log.e("saveFile", e.toString())
        } finally {
            input?.close()
        }

    }

    inline fun <reified T> genericType() = object : TypeToken<T>() {}.type
}