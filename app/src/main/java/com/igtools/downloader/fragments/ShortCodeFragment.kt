package com.igtools.downloader.fragments

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.igtools.downloader.R
import com.igtools.downloader.adapter.MultiTypeAdapter
import com.igtools.downloader.api.okhttp.OkhttpHelper
import com.igtools.downloader.api.okhttp.OkhttpListener
import com.igtools.downloader.api.okhttp.Urls
import com.igtools.downloader.api.retrofit.ApiClient
import com.igtools.downloader.databinding.FragmentShortCodeBinding
import com.igtools.downloader.models.MediaModel
import com.igtools.downloader.models.Record
import com.igtools.downloader.room.RecordDB
import com.igtools.downloader.utils.DateUtils
import com.igtools.downloader.utils.FileUtils
import com.igtools.downloader.utils.KeyboardUtils
import com.igtools.downloader.utils.RegexUtils
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import java.util.*


/**
 * @Author: desong
 * @Date: 2022/7/21
 */

class ShortCodeFragment : Fragment() {

    lateinit var binding: FragmentShortCodeBinding
    lateinit var adapter: MultiTypeAdapter
    var TAG = "ShortCodeFragment"
    var medias: ArrayList<MediaModel> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_short_code, container, false)

        initViews()
        setListeners()

        return binding.root;
    }

    override fun onResume() {
        super.onResume()


        binding.etShortcode.post {

            val intent = activity?.intent
            if (intent?.action == Intent.ACTION_SEND) {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                        // Update UI to reflect text being shared

                        val urls = RegexUtils.extractUrls(it)
                        binding.etShortcode.setText(urls[0])

                    }
                }
            } else {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val item = clipboard.primaryClip?.getItemAt(0)
                val pasteData = item?.text

                if (pasteData != null) {
                    binding.etShortcode.setText(pasteData)
                }
            }


        }

    }


    private fun initViews() {

        binding.tvDownload.isEnabled = false
        binding.tvPaste.isEnabled = false
        binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
        binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.black))
        binding.container.visibility = View.GONE
        adapter = MultiTypeAdapter(context, medias)
        binding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(context))
            .setAdapter(adapter)
            .isAutoLoop(false)


    }


    private fun getData(url: String) {

        //val url = "http://192.168.0.101:3000/api/mediainfo?url=$url"
        val api = Urls.SHORT_CODE + "?url=$url"
        OkhttpHelper.getInstance().getJson(api, object :
            OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject) {

                parseData(jsonObject);
                if (medias.size > 0) {
                    adapter.setDatas(medias)
//                    binding.banner.currentItem=medias.size
//                    adapter.notifyItemChanged(medias.size)
                    //progressAdapter.update(progressList)

                    //enable download
                    binding.tvDownload.isEnabled = true
                    binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.white))
                }

                binding.progressBar.visibility = View.INVISIBLE

            }

            override fun onFail(message: String?) {
                binding.progressBar.visibility = View.INVISIBLE
                Toast.makeText(context, message + "", Toast.LENGTH_SHORT).show()
            }

        })

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

    private fun setListeners() {

        binding.tvDownload.setOnClickListener {

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

                RecordDB.getInstance().recordDao().insert(record)

                binding.progressBar.visibility = View.INVISIBLE

                Toast.makeText(context, getString(R.string.download_finish), Toast.LENGTH_SHORT).show()


            }

        }
        binding.tvPaste.setOnClickListener {
            binding.etShortcode.clearFocus()
            KeyboardUtils.closeKeybord(binding.etShortcode, context)
            binding.tvDownload.isEnabled = false
            binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
            binding.progressBar.visibility = View.VISIBLE
            binding.container.visibility = View.VISIBLE
            getData(binding.etShortcode.text.toString())
        }

        binding.etShortcode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (binding.etShortcode.text.isNotEmpty()) {
                    binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.white))
                    binding.tvPaste.isEnabled = true
                    binding.imgClear.visibility = View.VISIBLE
                } else {
                    binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.home_unselect_color))
                    binding.tvPaste.isEnabled = false
                    binding.imgClear.visibility = View.INVISIBLE
                }

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        binding.imgClear.setOnClickListener {

            binding.etShortcode.setText("")

        }

    }

    private suspend fun downloadMedia(media: MediaModel) {

        if (media.mediaType == 1) {
            //image
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
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
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            try {
                val responseBody = media.videoUrl?.let { ApiClient.getClient().downloadUrl(it) }
                responseBody?.body()?.let { saveFile(it, file, 2) }

            } catch (e: Error) {
                //errFlag = true
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
                FileUtils.saveImageToAlbum(requireContext(), bitmap, file.name)
            } else {
                FileUtils.saveVideoToAlbum(requireContext(), file)
            }
            Log.v(TAG,file.absolutePath)

        } catch (e: Exception) {
            Log.e("saveFile", e.toString())
        } finally {
            input?.close()
        }

    }


}