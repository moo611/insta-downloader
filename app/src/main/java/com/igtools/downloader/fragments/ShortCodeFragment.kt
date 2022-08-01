package com.igtools.downloader.fragments

import android.content.res.Resources
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
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.igtools.downloader.R
import com.igtools.downloader.adapter.BlogAdapter
import com.igtools.downloader.adapter.MultiTypeAdapter
import com.igtools.downloader.adapter.ProgressAdapter
import com.igtools.downloader.api.OkhttpHelper
import com.igtools.downloader.api.OkhttpListener
import com.igtools.downloader.api.OnDownloadListener
import com.igtools.downloader.api.Urls
import com.igtools.downloader.databinding.FragmentShortCodeBinding
import com.igtools.downloader.models.MediaModel
import com.igtools.downloader.utils.FileUtils
import com.igtools.downloader.utils.KeyboardUtils
import com.igtools.downloader.widgets.dialog.ProgressDialog
import com.youth.banner.indicator.CircleIndicator
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger


/**
 * @Author: desong
 * @Date: 2022/7/21
 */

class ShortCodeFragment : Fragment() {
    
    
    lateinit var progressAdapter: ProgressAdapter
    lateinit var binding: FragmentShortCodeBinding
    lateinit var adapter: MultiTypeAdapter
    var TAG = "ShortCodeFragment"
    lateinit var progressDialog: ProgressDialog
    var medias: ArrayList<MediaModel> = ArrayList()
    var progressList: ArrayList<Int> = ArrayList()
    
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


        progressDialog = ProgressDialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.view_download, null)

        progressAdapter = ProgressAdapter(context,progressList)
        val rv = view.findViewById<RecyclerView>(R.id.rv)
        rv.adapter = progressAdapter
        rv.layoutManager = LinearLayoutManager(context)

        progressDialog.setView(view)

    }


    private fun getData(url: String) {
        //val url = "http://192.168.0.101:3000/api/mediainfo?url=$url"
        val api = Urls.SHORT_CODE + "?url=$url"
        OkhttpHelper.getInstance().getJson(api, object : OkhttpListener {
            override fun onSuccess(jsonObject: JsonObject) {

                parseData(jsonObject);
                if (medias.size > 0) {
                    adapter.setDatas(medias)
                    progressAdapter.update(progressList)

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

    private fun setListeners() {

        binding.tvDownload.setOnClickListener {
            //TODO 不显示
            progressDialog.show()
            for (media in medias) {
                downloadMedia(media,medias.indexOf(media))
            }

            //Toast.makeText(context, "download finished", Toast.LENGTH_SHORT).show()
        }
        binding.tvPaste.setOnClickListener {
            binding.etShortcode.clearFocus()
            KeyboardUtils.closeKeybord(binding.etShortcode, context)
            binding.tvDownload.isEnabled = false
            binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
            binding.progressBar.visibility = View.VISIBLE
            //binding.container.visibility = View.VISIBLE
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

    private fun downloadMedia(media: MediaModel,index: Int) {

        if (media.mediaType == 1) {
            //image
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")
            OkhttpHelper.getInstance()
                .download(media.thumbnailUrl, file, object : OnDownloadListener {
                    override fun onDownloadSuccess(path: String?) {


                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        FileUtils.saveImageToAlbum(context!!, bitmap, file.name)

                    }

                    override fun onDownloading(progress: Int) {
                        progressList[index] = progress

                        activity?.runOnUiThread{
                            progressAdapter.update(progressList)
                        }
                    }

                    override fun onDownloadFailed(message: String?) {

                        activity?.runOnUiThread {
                            Toast.makeText(context, message + "", Toast.LENGTH_SHORT).show()
                        }
                    }

                })

        } else {
            //video
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            OkhttpHelper.getInstance().download(media.videoUrl, file, object : OnDownloadListener {
                override fun onDownloadSuccess(path: String?) {

                    FileUtils.saveVideoToAlbum(context!!, file)

                }

                override fun onDownloading(progress: Int) {
                    progressList[index] = progress

                    activity?.runOnUiThread{
                        progressAdapter.update(progressList)
                    }
                }

                override fun onDownloadFailed(message: String?) {

                    activity?.runOnUiThread {
                        Toast.makeText(context, message + "", Toast.LENGTH_SHORT).show()
                    }
                }

            })

        }

    }


}