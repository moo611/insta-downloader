package com.igtools.videodownloader.service.home

import android.app.ProgressDialog
import android.content.Intent
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.fagaia.farm.base.BaseFragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.igtools.videodownloader.R
import com.igtools.videodownloader.service.details.BlogDetailsActivity
import com.igtools.videodownloader.service.web.WebActivity
import com.igtools.videodownloader.api.okhttp.Urls
import com.igtools.videodownloader.api.retrofit.ApiClient
import com.igtools.videodownloader.databinding.FragmentShortCodeBinding
import com.igtools.videodownloader.models.IntentEvent
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.models.ResourceModel
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.utils.*
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import kotlinx.android.synthetic.main.dialog_bottom.view.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File


/**
 * @Author: desong
 * @Date: 2022/7/21
 */

class ShortCodeFragment : BaseFragment<FragmentShortCodeBinding>() {

    lateinit var progressDialog: ProgressDialog
    lateinit var circularProgressDrawable: CircularProgressDrawable
    lateinit var glideListener: RequestListener<Drawable>
    lateinit var bottomDialog: BottomDialog
    lateinit var recentAdapter: RecentAdapter
    var TAG = "ShortCodeFragment"
    var mInterstitialAd: InterstitialAd? = null
    var curMediaInfo: MediaModel? = null

    private val LOGIN_REQ = 1000

    override fun getLayoutId(): Int {
        return R.layout.fragment_short_code
    }

    override fun initView() {
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


        mBinding.btnDownload.setOnClickListener {
            autoStart()
        }

        mBinding.container.setOnClickListener {
            val content = gson.toJson(curMediaInfo)
            startActivity(
                Intent(
                    requireContext(),
                    BlogDetailsActivity::class.java
                ).putExtra("content", content).putExtra("flag", false)
            )

        }

        mBinding.etShortcode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (mBinding.etShortcode.text.isNotEmpty()) {
                    mBinding.imgClear.visibility = View.VISIBLE
                } else {
                    mBinding.imgClear.visibility = View.INVISIBLE
                }

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        mBinding.imgClear.setOnClickListener {
            mBinding.etShortcode.setText("")
        }

        mBinding.adView.adListener = object : AdListener() {

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Code to be executed when an ad request fails.
                Log.e(TAG, adError.message)
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                mBinding.adView.visibility = View.VISIBLE
            }


        }

        glideListener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {

                mBinding.progressbar.visibility = View.INVISIBLE
                return false;
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {

                mBinding.progressbar.visibility = View.INVISIBLE
                return false;

            }

        }
    }

    override fun initData() {
        recentAdapter = RecentAdapter(requireContext())
        mBinding.rv.adapter = recentAdapter
        mBinding.rv.layoutManager = GridLayoutManager(requireContext(),4)
        getRecentData()
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
        mBinding.adView.loadAd(adRequest)
    }


    private fun autoStart() {
        Log.v(TAG, ShareUtils.getData("cookie") + "")
        mBinding.etShortcode.clearFocus()
        KeyboardUtils.closeKeybord(mBinding.etShortcode, context)
        val url = mBinding.etShortcode.text.toString()

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
                getStoryData()
            }

        } else {

            getMediaData()

        }


    }


    /**
     * 获取video,image,igtv,reel
     * 登录情况下
     */

    private fun getMediaData() {

        val shortCode = getShortCode()
        if (shortCode == null) {
            Toast.makeText(context, getString(R.string.parse_error), Toast.LENGTH_SHORT).show()
            return
        }
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
                val size = Urls.Cookies.size
                val random = (0 until size).random()
                map["Cookie"] = Urls.Cookies[random]
                val cookie = ShareUtils.getData("cookie")
                if (cookie != null && cookie.contains("sessionid")) {
                    map["Cookie"] = cookie
                }

                map["User-Agent"] = Urls.USER_AGENT

                val map2: HashMap<String, String> = HashMap()
                map2["shortcode"] = shortCode

                val res = ApiClient.getClient()
                    .getMediaData(Urls.GRAPH_QL, map, Urls.QUERY_HASH, gson.toJson(map2))
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
                    Toast.makeText(context, getString(R.string.download_finish), Toast.LENGTH_SHORT)
                        .show()
                    mInterstitialAd?.show(requireActivity())
                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_SHORT)
                        .show()
                }

            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                //Toast.makeText(context, getString(R.string.parse_error), Toast.LENGTH_SHORT).show()
                bottomDialog.show()
            }
        }
    }


    /**
     * 获取story
     */
    private fun getStoryData() {
        val pk = getShortCode()
        if (pk == null) {
            Toast.makeText(context, getString(R.string.parse_error), Toast.LENGTH_SHORT).show()
            return
        }
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

                val url = Urls.PRIVATE_API + "/media/" + pk + "/info"
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
                    Toast.makeText(context, getString(R.string.download_finish), Toast.LENGTH_SHORT)
                        .show()
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


    private fun updateUI() {

        mBinding.container.visibility = View.VISIBLE
        mBinding.progressbar.visibility = View.VISIBLE
        mBinding.username.text = curMediaInfo?.captionText
        Glide.with(requireContext()).load(curMediaInfo?.thumbnailUrl)
            .placeholder(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.gray_1)))
            .listener(glideListener)
            .into(mBinding.picture)
        Glide.with(requireContext()).load(curMediaInfo?.profilePicUrl).circleCrop()
            .placeholder(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.gray_1)))
            .into(mBinding.avatar)
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

            if (mediaModel.mediaType == 2) {
                val video_versions = item["video_versions"].asJsonArray
                if (video_versions.size() > 0) {
                    mediaModel.videoUrl = video_versions[0].asJsonObject["url"].asString
                }
            }

            if (jsonObject.has("caption")) {
                mediaModel.captionText = jsonObject["caption"].asJsonObject["text"].asString
            }

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
            FileUtils.saveFile(requireContext(), responseBody.body(), file, 1)

        } else if (media?.mediaType == 2) {
            //video
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!
                .absolutePath
            val file = File(dir, System.currentTimeMillis().toString() + ".mp4")
            if (media.videoUrl != null) {
                val responseBody = ApiClient.getClient().downloadUrl(media.videoUrl!!)
                FileUtils.saveFile(requireContext(), responseBody.body(), file, 2)

            }

        }
    }

    private fun getRecentData(){
        val medias:ArrayList<MediaModel> = ArrayList()
        var records: ArrayList<Record>
        lifecycleScope.launch {
            records = RecordDB.getInstance().recordDao().recent() as ArrayList<Record>
            Log.v(TAG, records.size.toString())
            for (record in records) {

                val mediaModel = gson.fromJson(record.content, MediaModel::class.java)
                medias.add(mediaModel)

            }
            recentAdapter.setDatas(medias)
            
        }
        
    }


    @Subscribe
    fun onKeywordReceive(intentEvent: IntentEvent) {

        val keyword = intentEvent.str
        mBinding.etShortcode.setText(keyword)
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

    private fun getShortCode(): String? {
        val shortCode: String?
        val url = mBinding.etShortcode.text.toString()
        shortCode = if (!url.contains("story")) {
            UrlUtils.extractMedia(url)
        } else {
            UrlUtils.extractStory(url)
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