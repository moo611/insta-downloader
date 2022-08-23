package com.igtools.igdownloader.fragments

import android.app.ProgressDialog
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.igtools.igdownloader.R
import com.igtools.igdownloader.adapter.MultiTypeAdapter
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.FragmentShortCodeBinding
import com.igtools.igdownloader.models.IntentEvent
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.models.Record
import com.igtools.igdownloader.room.RecordDB
import com.igtools.igdownloader.utils.DateUtils
import com.igtools.igdownloader.utils.FileUtils
import com.igtools.igdownloader.utils.KeyboardUtils
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
    var medias: ArrayList<MediaModel> = ArrayList()
    var isDownloading = false
    var mInterstitialAd: InterstitialAd? = null

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

        InterstitialAd.load(requireContext(), "ca-app-pub-3940256099942544/1033173712", adRequest,
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

        adapter = MultiTypeAdapter(requireContext(), medias)
        binding.banner
            .addBannerLifecycleObserver(this)
            .setIndicator(CircleIndicator(context))
            .setAdapter(adapter)
            .isAutoLoop(false)


        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)

    }


    private fun getData(url: String) {

        val isValid = URLUtil.isValidUrl(url)
        if (!isValid){
            Toast.makeText(context,getString(R.string.invalid_url),Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()
        medias.clear()

        lifecycleScope.launch {

            try {
                val res = ApiClient.getClient().getShortCode(url)

                val code = res.code()
                if (code != 200) {
                    progressDialog.dismiss()
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }
                val jsonObject = res.body()
                if (jsonObject != null) {
                    parseData(jsonObject);
                    if (medias.size > 0) {
                        adapter.setDatas(medias as List<MediaModel?>?)

                        binding.tvDownload.isEnabled = true
                        binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.white))
                    }

                }
                progressDialog.dismiss()

            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun parseData(jsonObject: JsonObject) {

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
            if (isDownloading){
                Toast.makeText(requireContext(),R.string.downloading,Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mInterstitialAd?.show(requireActivity())

            isDownloading = true
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {

                val all: List<Deferred<Unit>> = medias.map {
                    async {
                        downloadMedia(it)
                    }
                }

                all.awaitAll()
                Log.v(TAG, "finish")
                val record = Record()
                record.createdTime = DateUtils.getDate(Date())
                record.content = Gson().toJson(medias)

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

            getData(binding.etShortcode.text.toString())
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

    private suspend fun downloadMedia(media: MediaModel?) {

        if (media?.mediaType == 1) {
            //image
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")
            val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl)
            withContext(Dispatchers.IO) {
                saveFile(responseBody.body(), file, 1)
            }
        } else {
            //video
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            val responseBody = ApiClient.getClient().downloadUrl(media?.videoUrl!!)

            withContext(Dispatchers.IO) {
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

    @Subscribe
    fun onKeywordReceive(intentEvent: IntentEvent){

        val keyword = intentEvent.str
        binding.etShortcode.setText(keyword)

        binding.etShortcode.clearFocus()
        KeyboardUtils.closeKeybord(binding.etShortcode, context)
        binding.tvDownload.isEnabled = false
        binding.tvDownload.setTextColor(requireContext().resources!!.getColor(R.color.black))


        getData(keyword)

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