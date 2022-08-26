package com.igtools.igdownloader.fragments

import android.app.ProgressDialog
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
import android.webkit.URLUtil
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.igtools.igdownloader.R
import com.igtools.igdownloader.activities.VideoActivity
import com.igtools.igdownloader.adapter.MultiTypeAdapter
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.FragmentShortCodeBinding
import com.igtools.igdownloader.models.IntentEvent
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.models.Record
import com.igtools.igdownloader.models.ResourceModel
import com.igtools.igdownloader.room.RecordDB
import com.igtools.igdownloader.utils.DateUtils
import com.igtools.igdownloader.utils.FileUtils
import com.igtools.igdownloader.utils.KeyboardUtils
import com.igtools.igdownloader.utils.getNullable
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*


/**
 * @Author: desong
 * @Date: 2022/7/21
 */

class ShortCodeFragment : Fragment() {

    lateinit var progressDialog: ProgressDialog
    lateinit var binding: FragmentShortCodeBinding
    lateinit var adapter: MultiTypeAdapter
    var TAG = "ShortCodeFragment"

    var isDownloading = false
    var mInterstitialAd: InterstitialAd? = null
    var mediaInfo = MediaModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_short_code, container, false)
        initAds()
        initViews()
        setListeners()

        return binding.root;
    }

    private fun initAds() {
        val adRequest = AdRequest.Builder().build();

        InterstitialAd.load(requireContext(), "ca-app-pub-8609866682652024/8844989426", adRequest,
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
    }


    private fun initViews() {

        binding.tvDownload.isEnabled = false
        binding.tvSearch.isEnabled = false
        binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
        binding.tvSearch.setTextColor(requireContext().resources!!.getColor(R.color.black))

        adapter = MultiTypeAdapter(requireContext(), mediaInfo.resources)
        binding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(context))
            .setAdapter(adapter)
            .isAutoLoop(false)


        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)

    }


    private fun getMedia(url: String) {

        val isValid = URLUtil.isValidUrl(url)
        if (!isValid) {
            Toast.makeText(context, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()

        lifecycleScope.launch {

            try {
                val res = ApiClient.getClient().getMedia(url)
                progressDialog.dismiss()
                val code = res.code()
                if (code != 200) {

                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }
                val jsonObject = res.body()
                if (jsonObject != null) {
                    val data = jsonObject["data"].asJsonObject
                    parseData(data)
                    if (mediaInfo.mediaType == 8) {
                        if (mediaInfo.resources.size > 0) {
                            show("banner")
                            adapter.setDatas(mediaInfo.resources as List<ResourceModel?>?)
                            binding.tvDownload.isEnabled = true
                            binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.white))

                        }
                    } else {
                        show("picture")
                        Glide.with(requireContext()).load(mediaInfo.thumbnailUrl)
                            .into(binding.picture)
                        binding.tvDownload.isEnabled = true
                        binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.white))

                    }

                }

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun getStories(url: String) {

        progressDialog.show()

        lifecycleScope.launch {

            try {
                val res = ApiClient.getClient().getStory(url)

                val code = res.code()
                if (code != 200) {
                    progressDialog.dismiss()
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }
                val jsonObject = res.body()
                if (jsonObject != null) {
                    val data = jsonObject["data"].asJsonObject
                    parseData(data)
                    show("picture")
                    Glide.with(requireContext()).load(mediaInfo.thumbnailUrl).into(binding.picture)
                    binding.tvDownload.isEnabled = true
                    binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.white))

                }
                progressDialog.dismiss()

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }

        }

    }


    private fun setListeners() {

        binding.tvDownload.setOnClickListener {
            if (isDownloading) {
                Toast.makeText(requireContext(), R.string.downloading, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mInterstitialAd?.show(requireActivity())

            isDownloading = true
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
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

                Log.v(TAG, "finish")
                val record = Record()
                record.createdTime = DateUtils.getDate(Date())
                record.content = Gson().toJson(mediaInfo)

                RecordDB.getInstance().recordDao().insert(record)

                binding.progressBar.visibility = View.INVISIBLE
                isDownloading = false
                Toast.makeText(context, getString(R.string.download_finish), Toast.LENGTH_SHORT)
                    .show()


            }

        }
        binding.tvSearch.setOnClickListener {
            binding.etShortcode.clearFocus()
            KeyboardUtils.closeKeybord(binding.etShortcode, context)
            binding.tvDownload.isEnabled = false
            binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))

            val keyword = binding.etShortcode.text.toString()
            if (keyword.contains("stories")) {
                getStories(keyword)
            } else {
                getMedia(keyword)
            }

        }

        binding.imgPlay.setOnClickListener {

            if (mediaInfo.mediaType == 2) {
                startActivity(
                    Intent(requireContext(), VideoActivity::class.java)
                        .putExtra("url", mediaInfo.videoUrl)
                        .putExtra("thumbnailUrl", mediaInfo.thumbnailUrl)
                )
            }

        }

        binding.etShortcode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (binding.etShortcode.text.isNotEmpty()) {
                    binding.tvSearch.setTextColor(requireContext().resources!!.getColor(R.color.white))
                    binding.tvSearch.isEnabled = true
                    binding.imgClear.visibility = View.VISIBLE


                } else {
                    binding.tvSearch.setTextColor(requireContext().resources!!.getColor(R.color.black))
                    binding.tvSearch.isEnabled = false
                    binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))
                    binding.tvDownload.isEnabled = false
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

    private fun parseData(jsonObject: JsonObject) {

        val mediaType = jsonObject["media_type"].asInt
        Log.v(TAG, "mediaType:$mediaType")
        if (mediaType == 8) {

            mediaInfo.pk = jsonObject["pk"].asString
            mediaInfo.code = jsonObject["code"].asString
            mediaInfo.mediaType = jsonObject["media_type"].asInt
            mediaInfo.videoUrl = jsonObject.getNullable("video_url")?.asString
            mediaInfo.captionText = jsonObject["caption_text"].asString
            mediaInfo.username = jsonObject["user"].asJsonObject["username"].asString
            mediaInfo.profilePicUrl = jsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString

            val resources = jsonObject["resources"].asJsonArray
            mediaInfo.thumbnailUrl = resources[0].asJsonObject["thumbnail_url"].asString
            for (res in resources) {

                val resourceInfo = ResourceModel()
                resourceInfo.pk = res.asJsonObject["pk"].asString
                resourceInfo.mediaType = res.asJsonObject["media_type"].asInt
                resourceInfo.thumbnailUrl = res.asJsonObject["thumbnail_url"].asString
                resourceInfo.videoUrl = res.asJsonObject.getNullable("video_url")?.asString
                mediaInfo.resources.add(resourceInfo)
            }

        } else if (mediaType == 0 || mediaType == 1 || mediaType == 2) {


            mediaInfo.pk = jsonObject["pk"].asString
            mediaInfo.code = jsonObject["code"].asString
            mediaInfo.mediaType = jsonObject["media_type"].asInt
            mediaInfo.thumbnailUrl = jsonObject["thumbnail_url"].asString
            mediaInfo.videoUrl = jsonObject.getNullable("video_url")?.asString
            mediaInfo.captionText = jsonObject.getNullable("caption_text")?.asString
            mediaInfo.username = jsonObject["user"].asJsonObject["username"].asString
            mediaInfo.profilePicUrl = jsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString

        }

    }

    private suspend fun downloadMedia(media: ResourceModel?) {

        if (media?.mediaType == 1 || media?.mediaType == 0) {
            //image
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")
            val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl!!)
            withContext(Dispatchers.IO) {
                saveFile(responseBody.body(), file, 1)
            }
        } else if (media?.mediaType == 2) {
            //video
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            if (media.videoUrl != null) {
                val responseBody = ApiClient.getClient().downloadUrl(media.videoUrl!!)
                withContext(Dispatchers.IO) {
                    saveFile(responseBody.body(), file, 2)
                }
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
                FileUtils.saveImageToAlbum(requireContext(), bitmap, file.name)
            } else {
                FileUtils.saveVideoToAlbum(requireContext(), file)
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

    @Subscribe
    fun onKeywordReceive(intentEvent: IntentEvent) {

        val keyword = intentEvent.str
        binding.etShortcode.setText(keyword)

        binding.etShortcode.clearFocus()
        KeyboardUtils.closeKeybord(binding.etShortcode, context)
        binding.tvDownload.isEnabled = false
        binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))

        if (keyword.contains("stories")) {
            getStories(keyword)
        } else {
            getMedia(keyword)
        }


    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
    }
}