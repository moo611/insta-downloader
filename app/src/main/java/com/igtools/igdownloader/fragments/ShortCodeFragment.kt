package com.igtools.igdownloader.fragments

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
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
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.dynamic.IFragmentWrapper
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.igtools.igdownloader.R
import com.igtools.igdownloader.activities.BlogDetailsActivity
import com.igtools.igdownloader.activities.VideoActivity
import com.igtools.igdownloader.activities.WebActivity
import com.igtools.igdownloader.adapter.MultiTypeAdapter
import com.igtools.igdownloader.api.okhttp.Urls
import com.igtools.igdownloader.api.retrofit.ApiClient
import com.igtools.igdownloader.databinding.FragmentShortCodeBinding
import com.igtools.igdownloader.models.IntentEvent
import com.igtools.igdownloader.models.MediaModel
import com.igtools.igdownloader.models.Record
import com.igtools.igdownloader.models.ResourceModel
import com.igtools.igdownloader.room.RecordDB
import com.igtools.igdownloader.utils.*
import com.igtools.igdownloader.widgets.dialog.BottomDialog
import com.youth.banner.indicator.CircleIndicator
import kotlinx.android.synthetic.main.dialog_bottom.view.*
import kotlinx.coroutines.*
import okhttp3.Dispatcher
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import tv.danmaku.ijk.media.player.MediaInfo
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap


/**
 * @Author: desong
 * @Date: 2022/7/21
 */

class ShortCodeFragment : Fragment() {

    lateinit var progressDialog: ProgressDialog
    lateinit var binding: FragmentShortCodeBinding
    lateinit var glideListener: RequestListener<Drawable>
    lateinit var bottomDialog: BottomDialog

    var TAG = "ShortCodeFragment"

    val gson = Gson()
    var mInterstitialAd: InterstitialAd? = null
    var curMediaInfo: MediaModel? = null
    lateinit var circularProgressDrawable: CircularProgressDrawable

    private val LOGIN_REQ = 1000

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
        //inter
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
        //banner
        binding.adView.loadAd(adRequest)
    }


    private fun initViews() {

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)

        circularProgressDrawable = CircularProgressDrawable(requireContext())
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()

        bottomDialog = BottomDialog(requireContext(), R.style.MyDialogTheme)
        val bottomView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bottom, null)
        bottomView.btn_login.setOnClickListener {

            val url = "https://www.instagram.com/accounts/login"
            startActivityForResult(
                Intent(requireContext(), WebActivity::class.java).putExtra(
                    "url",
                    url
                ), LOGIN_REQ
            )
            bottomDialog.dismiss()
        }
        bottomDialog.setContent(bottomView)

    }


    private fun autoStart() {
        Log.v(TAG, ShareUtils.getData("cookie") + "")
        binding.etShortcode.clearFocus()
        KeyboardUtils.closeKeybord(binding.etShortcode, context)
        val url = binding.etShortcode.text.toString()

        val isValid = URLUtil.isValidUrl(url)
        if (!isValid) {
            Toast.makeText(context, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
            return
        }


        if (url.contains("stories")) {
            val cookie = ShareUtils.getData("cookie")
            if (cookie == null) {
                bottomDialog.show()
            } else {
                val shortCode = getShortCode()
                getStoryData(shortCode)
            }

        } else {
            val cookie = ShareUtils.getData("cookie")
            if (cookie != null) {
                getMediaData()
            } else {
                getMediaOld()
            }

        }


    }

    private fun setListeners() {

        binding.btnDownload.setOnClickListener {
            autoStart()
        }

        binding.container.setOnClickListener {
            val content = gson.toJson(curMediaInfo)
            startActivity(
                Intent(
                    requireContext(),
                    BlogDetailsActivity::class.java
                ).putExtra("content", content).putExtra("flag", false)
            )

        }

        binding.etShortcode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (binding.etShortcode.text.isNotEmpty()) {
                    binding.imgClear.visibility = View.VISIBLE
                } else {
                    binding.imgClear.visibility = View.INVISIBLE
                }

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        binding.imgClear.setOnClickListener {
            binding.etShortcode.setText("")
        }

        binding.adView.adListener = object : AdListener() {

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Code to be executed when an ad request fails.
                Log.e(TAG, adError.message)
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                binding.adView.visibility = View.VISIBLE
            }


        }

        glideListener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {

                binding.progressbar.visibility = View.INVISIBLE
                return false;
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {

                binding.progressbar.visibility = View.INVISIBLE
                return false;

            }

        }

    }

    /**
     * 获取video,image,igtv,reel
     * 登录情况下
     */

    private fun getMediaData() {

        val shortCode = getShortCode()
        lifecycleScope.launch {
            //检查是否已存在
            val record = RecordDB.getInstance().recordDao().findById(shortCode)

            if (record != null) {
                curMediaInfo = gson.fromJson(record.content, MediaModel::class.java)
                updateUI()
                Toast.makeText(requireContext(), getString(R.string.exist), Toast.LENGTH_SHORT)
                    .show()

                return@launch
            }
            progressDialog.show()
            try {
                val map: HashMap<String, String> = HashMap()
                map["Cookie"] = Urls.Cookie
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
                    curMediaInfo = parseMedia(jsonObject)
                    saveRecord(shortCode)
                    updateUI()
                    if (curMediaInfo?.mediaType == 8) {
                        val all: List<Deferred<Unit>> = curMediaInfo!!.resources.map {
                            async {
                                download(it)
                            }
                        }

                        all.awaitAll()
                    } else {
                        download(curMediaInfo)
                    }
                    Toast.makeText(context,getString(R.string.download_finish),Toast.LENGTH_SHORT).show()
                    mInterstitialAd?.show(requireActivity())
                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                }

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(context, getString(R.string.parse_error), Toast.LENGTH_SHORT).show()

            }
        }
    }

    /**
     * 不登录情况下
     */
    private fun getMediaOld() {

        val shortCode = getShortCode()

        lifecycleScope.launch {
            val record = RecordDB.getInstance().recordDao().findById(shortCode)

            if (record != null) {
                curMediaInfo = gson.fromJson(record.content, MediaModel::class.java)
                updateUI()
                Toast.makeText(requireContext(), getString(R.string.exist), Toast.LENGTH_SHORT)
                    .show()

                return@launch
            }

            progressDialog.show()
            try {
                val res = ApiClient.getClient().getMedia(shortCode)
                val code = res.code()
                val jsonObject = res.body()

                progressDialog.dismiss()
                if (code == 200 && jsonObject != null) {
                    binding.container.visibility = View.VISIBLE
                    val data = jsonObject["data"].asJsonObject
                    curMediaInfo = parseMediaOld(data)

                    updateUI()
                    saveRecord(shortCode)
                    if (curMediaInfo?.mediaType == 8) {
                        val all: List<Deferred<Unit>> = curMediaInfo!!.resources.map {
                            async {
                                download(it)
                            }
                        }

                        all.awaitAll()
                    } else {
                        download(curMediaInfo)
                    }

                    Toast.makeText(context,getString(R.string.download_finish),Toast.LENGTH_SHORT).show()
                    mInterstitialAd?.show(requireActivity())
                } else {
                    handleError()

                }

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(context, getString(R.string.parse_error), Toast.LENGTH_SHORT).show()
            }


        }


    }


    /**
     * 获取story
     */
    private fun getStoryData(pk: String) {

        lifecycleScope.launch {
            //检查是否已存在
            val record = RecordDB.getInstance().recordDao().findById(pk)
            if (record != null) {
                curMediaInfo = gson.fromJson(record.content, MediaModel::class.java)
                updateUI()
                Toast.makeText(requireContext(), getString(R.string.exist), Toast.LENGTH_SHORT)
                    .show()

                return@launch
            }
            progressDialog.show()
            try {
                val map: HashMap<String, String> = HashMap()
                val cookie = ShareUtils.getData("cookie")
                map["Cookie"] = cookie!!
                map["User-Agent"] = Urls.USER_AGENT

                val url = Urls.STORY_INFO + "/media/" + pk + "/info"
                val res = ApiClient.getClient()
                    .getStoryData(url, map)
                val code = res.code()
                val jsonObject = res.body()
                progressDialog.dismiss()
                if (code == 200 && jsonObject != null) {
                    curMediaInfo = parseStory(jsonObject)

                    updateUI()
                    saveRecord(pk)
                    download(curMediaInfo)
                    Toast.makeText(context,getString(R.string.download_finish),Toast.LENGTH_SHORT).show()
                    mInterstitialAd?.show(requireActivity())
                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.not_found),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.parse_error),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

    }

    private fun handleError() {
        val cookie = ShareUtils.getData("cookie")
        if (cookie == null) {
            bottomDialog.show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.not_found), Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun updateUI() {

        binding.container.visibility = View.VISIBLE
        binding.progressbar.visibility = View.VISIBLE
        binding.username.text = curMediaInfo?.captionText
        Glide.with(requireContext()).load(curMediaInfo?.thumbnailUrl)
            .placeholder(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.gray_1)))
            .listener(glideListener)
            .into(binding.picture)
        Glide.with(requireContext()).load(curMediaInfo?.profilePicUrl).circleCrop()
            .placeholder(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.gray_1)))
            .into(binding.avatar)
    }

    private fun saveRecord(id: String) {
        lifecycleScope.launch {
            val record = Record(id, gson.toJson(curMediaInfo), System.currentTimeMillis())
            RecordDB.getInstance().recordDao().insert(record)
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

    private fun parseMediaOld(jsonObject: JsonObject): MediaModel {
        val mediaInfo = MediaModel()
        val mediaType = jsonObject["media_type"].asInt
        Log.v(TAG, "mediaType:$mediaType")
        if (mediaType == 8) {

            mediaInfo.pk = jsonObject["pk"].asString
            mediaInfo.code = jsonObject["code"].asString
            mediaInfo.mediaType = jsonObject["media_type"].asInt
            mediaInfo.videoUrl = jsonObject.getNullable("video_url")?.asString
            mediaInfo.captionText = jsonObject["caption_text"].asString
            mediaInfo.username = jsonObject["user"].asJsonObject["username"].asString
            mediaInfo.profilePicUrl =
                jsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString

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
            if (resources.size() > 0) {
                mediaInfo.thumbnailUrl = resources[0].asJsonObject["thumbnail_url"].asString
            }

        } else if (mediaType == 1 || mediaType == 2) {


            mediaInfo.pk = jsonObject["pk"].asString
            mediaInfo.code = jsonObject["code"].asString
            mediaInfo.mediaType = jsonObject["media_type"].asInt
            mediaInfo.thumbnailUrl = jsonObject["thumbnail_url"].asString
            mediaInfo.videoUrl = jsonObject.getNullable("video_url")?.asString
            mediaInfo.captionText = jsonObject.getNullable("caption_text")?.asString
            mediaInfo.username = jsonObject["user"].asJsonObject["username"].asString
            mediaInfo.profilePicUrl =
                jsonObject["user"].asJsonObject.getNullable("profile_pic_url")?.asString

        }

        return mediaInfo

    }

    private fun parseStory(jsonObject: JsonObject): MediaModel {
        val mediaModel = MediaModel()
        val items = jsonObject["items"].asJsonArray
        if (items.size() > 0) {
            val item = items[0].asJsonObject
            mediaModel.mediaType = item["media_type"].asInt
            mediaModel.code = item["code"].asString
            val user = item["user"].asJsonObject
            mediaModel.username = user["username"].asString
            mediaModel.profilePicUrl = user["profile_pic_url"].asString
            val image_versions2 = item["image_versions2"].asJsonObject
            val candidates = image_versions2["candidates"].asJsonArray
            if (candidates.size() > 0) {
                val size = candidates.size()
                mediaModel.thumbnailUrl = candidates[size - 1].asJsonObject["url"].asString
            }
            mediaModel.pk = item["pk"].asString
        }

        return mediaModel

    }


    /**
     * 下载单个图片或视频
     */
    private suspend fun download(media: ResourceModel?) = withContext(Dispatchers.IO) {

        if (media?.mediaType == 1) {
            //image
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".jpg")
            val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl!!)
            saveFile(responseBody.body(), file, 1)

        } else if (media?.mediaType == 2) {
            //video
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            if (media.videoUrl != null) {
                val responseBody = ApiClient.getClient().downloadUrl(media.videoUrl!!)
                saveFile(responseBody.body(), file, 2)

            }

        }
    }

    /**
     * 保存文件到本地
     */
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
    fun onKeywordReceive(intentEvent: IntentEvent) {

        val keyword = intentEvent.str
        binding.etShortcode.setText(keyword)
        if (ShareUtils.getData("isAuto") == null || ShareUtils.getData("isAuto").toBoolean()) {
            autoStart()
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQ && resultCode == 200) {
            autoStart()
        }
    }

    private fun getShortCode(): String {
        var shortCode = ""
        val url = binding.etShortcode.text.toString()
        if (!url.contains("story")) {
            shortCode = UrlUtils.extractMedia(url)
        } else {
            shortCode = UrlUtils.extractStory(url)
        }
        return shortCode
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