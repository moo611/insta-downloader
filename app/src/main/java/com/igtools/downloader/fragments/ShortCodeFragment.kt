package com.igtools.downloader.fragments

import android.graphics.BitmapFactory
import android.os.Build
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
import com.google.gson.JsonObject
import com.igtools.downloader.R
import com.igtools.downloader.adapter.MultiTypeAdapter
import com.igtools.downloader.api.OkhttpHelper
import com.igtools.downloader.api.OkhttpListener
import com.igtools.downloader.api.OnDownloadListener
import com.igtools.downloader.databinding.FragmentShortCodeBinding
import com.igtools.downloader.models.MediaModel
import com.igtools.downloader.utils.FileUtils
import com.igtools.downloader.utils.KeyboardUtils
import com.youth.banner.indicator.CircleIndicator
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger


/**
 * @Author: desong
 * @Date: 2022/7/21
 */

class ShortCodeFragment : Fragment() {

    lateinit var binding: FragmentShortCodeBinding
    lateinit var adapter: MultiTypeAdapter
    var TAG = "ShortCodeFragment"
    var count = AtomicInteger(0)
    var latch: CountDownLatch?=null
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

    private fun initViews() {

        binding.tvDownload.isEnabled = false
        binding.tvPaste.isEnabled = false
        binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
        binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.black))

        adapter = MultiTypeAdapter(context, medias)
        binding.banner.addBannerLifecycleObserver(this).setIndicator(CircleIndicator(context))
            .setAdapter(adapter).isAutoLoop(false)


    }


    private fun getData(url: String) {
        val url = "http://192.168.0.101:3000/api/mediainfo?url=$url"
        OkhttpHelper.getInstance().getJson(url, object : OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject) {

                parseData(jsonObject);
                if (medias.size > 0) {
                    adapter.setDatas(medias)

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

            medias.add(mediaInfo)
        }


    }

    private fun setListeners() {

        binding.tvDownload.setOnClickListener {
            latch = CountDownLatch(medias.size)
            count.set(0)
            for (media in medias) {
                downloadMedia(media)
            }
            latch?.await();
            Log.v(TAG,count.get().toString());
            Toast.makeText(context,"download finished",Toast.LENGTH_SHORT).show()
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
                } else {
                    binding.tvPaste.setTextColor(requireContext().resources!!.getColor(R.color.black))
                    binding.tvPaste.isEnabled = false
                }

            }

            override fun afterTextChanged(s: Editable?) {
                //TODO("Not yet implemented")
            }

        })

    }

    private fun downloadMedia(media: MediaModel) {

        if (media.mediaType == 1) {
            //image
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")
            OkhttpHelper.getInstance()
                .download(media.thumbnailUrl, file, object : OnDownloadListener {
                    override fun onDownloadSuccess(path: String?) {

                        count.getAndIncrement()

                        val btm = BitmapFactory.decodeFile(path)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            FileUtils.saveImage29(context, btm)
                        } else {
                            FileUtils.saveImage(context, btm, file.name)
                        }
                        latch?.countDown();


                    }

                    override fun onDownloading(progress: Int) {

                    }

                    override fun onDownloadFailed(message: String?) {
                        latch?.countDown();
                        activity?.runOnUiThread {
                            Toast.makeText(context, message + "", Toast.LENGTH_SHORT).show()
                        }
                    }

                })

        } else {
            //video
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            OkhttpHelper.getInstance().download(media.videoUrl, file, object : OnDownloadListener {
                override fun onDownloadSuccess(path: String?) {
                    count.getAndIncrement()
                    FileUtils.saveVideo(context, file)
                    latch?.countDown();

                }

                override fun onDownloading(progress: Int) {

                }

                override fun onDownloadFailed(message: String?) {

                    latch?.countDown();
                    activity?.runOnUiThread {
                        Toast.makeText(context, message + "", Toast.LENGTH_SHORT).show()
                    }
                }

            })

        }

    }


}