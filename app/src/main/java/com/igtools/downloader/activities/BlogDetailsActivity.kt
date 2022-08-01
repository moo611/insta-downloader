package com.igtools.downloader.activities

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.gson.JsonObject
import com.igtools.downloader.R
import com.igtools.downloader.adapter.MultiTypeAdapter
import com.igtools.downloader.api.OkhttpHelper
import com.igtools.downloader.api.OkhttpListener
import com.igtools.downloader.api.OnDownloadListener
import com.igtools.downloader.api.Urls
import com.igtools.downloader.databinding.ActivityBlogDetailsBinding
import com.igtools.downloader.models.MediaModel
import com.igtools.downloader.utils.FileUtils
import com.youth.banner.indicator.CircleIndicator
import java.io.File

class BlogDetailsActivity : AppCompatActivity() {

    var errFlag = false
    lateinit var binding: ActivityBlogDetailsBinding
    lateinit var adapter: MultiTypeAdapter
    var TAG = "ShortCodeFragment"
    var progressList: ArrayList<Int> = ArrayList()
    var medias: ArrayList<MediaModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blog_details)


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
            Thread {
                for (media in medias) {
                    downloadMedia(media, medias.indexOf(media))
                }

                while (true) {
                    if (errFlag) {
                        break
                    }
                    var cnt = 0
                    for (index in progressList) {
                        if (index == 100) {
                            cnt++
                        }
                    }
                    if (cnt == progressList.size) {
                        break
                    }
                }
                runOnUiThread {
                    binding.progressBar.visibility = View.INVISIBLE
                    if (errFlag) {
                        Toast.makeText(
                            this@BlogDetailsActivity,
                            "download failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@BlogDetailsActivity,
                            "download finished",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }

            }.start()

        }
        binding.imgBack.setOnClickListener {
            finish()
        }

    }


    private fun getData(url: String) {
        binding.progressBar.visibility = View.VISIBLE
        //val url = "http://192.168.0.101:3000/api/mediainfo?url=$url"
        val api = Urls.SHORT_CODE + "?url=$url"
        OkhttpHelper.getInstance().getJson(api, object : OkhttpListener {
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

    private fun downloadMedia(media: MediaModel, index: Int) {

        if (media.mediaType == 1) {
            //image
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")
            OkhttpHelper.getInstance()
                .download(media.thumbnailUrl, file, object : OnDownloadListener {
                    override fun onDownloadSuccess(path: String?) {

                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        FileUtils.saveImageToAlbum(this@BlogDetailsActivity, bitmap, file.name)

                    }

                    override fun onDownloading(progress: Int) {
                        progressList[index] = progress
                    }

                    override fun onDownloadFailed(message: String?) {
                        errFlag = true
                    }

                })

        } else {
            //video
            val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            OkhttpHelper.getInstance().download(media.videoUrl, file, object : OnDownloadListener {
                override fun onDownloadSuccess(path: String?) {

                    FileUtils.saveVideoToAlbum(this@BlogDetailsActivity, file)

                }

                override fun onDownloading(progress: Int) {
                    progressList[index] = progress
                }

                override fun onDownloadFailed(message: String?) {

                    errFlag = true
                }

            })

        }

    }

}