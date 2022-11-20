package com.igtools.videodownloader.service.home

import android.app.ProgressDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.URLUtil
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.igtools.videodownloader.base.BaseFragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonObject
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.R
import com.igtools.videodownloader.api.okhttp.Urls
import com.igtools.videodownloader.api.retrofit.ApiClient
import com.igtools.videodownloader.databinding.FragmentShortCodeBinding
import com.igtools.videodownloader.models.IntentEvent
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.models.ResourceModel
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.service.details.BlogDetailsActivity
import com.igtools.videodownloader.service.history.HistoryAdapter
import com.igtools.videodownloader.service.web.WebActivity
import com.igtools.videodownloader.utils.*
import com.igtools.videodownloader.widgets.dialog.BottomDialog
import com.igtools.videodownloader.widgets.dialog.CustomDialog
import com.igtools.videodownloader.widgets.dialog.MyDialog
import kotlinx.android.synthetic.main.dialog_bottom.view.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.net.URLEncoder


/**
 * @Author: desong
 * @Date: 2022/7/21
 */

class ShortCodeFragment : BaseFragment<FragmentShortCodeBinding>() {

    lateinit var progressDialog: ProgressDialog
    lateinit var privateDialog: MyDialog
    lateinit var storyDialog: MyDialog

    lateinit var recentAdapter: RecentAdapter
    lateinit var firebaseAnalytics: FirebaseAnalytics
    var TAG = "ShortCodeFragment"
    var mInterstitialAd: InterstitialAd? = null
    var curMediaInfo: MediaModel? = null
    var records: ArrayList<Record> = ArrayList()
    var paths = StringBuffer()
    private val LOGIN_REQ = 1000

    override fun getLayoutId(): Int {
        return R.layout.fragment_short_code
    }

    override fun initView() {
        initAds()
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)

        initDialog()


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
                mBinding.adcard.visibility = View.VISIBLE
            }


        }


    }

    private fun initDialog() {

        storyDialog = MyDialog(requireContext(), R.style.MyDialogTheme)
        val storyView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_remind, null)
        val title2 = storyView.findViewById<TextView>(R.id.title)
        title2.text = getString(R.string.long_text1)
        val tvLogin = storyView.findViewById<TextView>(R.id.tv_login)
        val tvCancel = storyView.findViewById<TextView>(R.id.tv_cancel)
        tvLogin.setOnClickListener {

            val url = "https://www.instagram.com/accounts/login"
            startActivityForResult(
                Intent(requireContext(), WebActivity::class.java).putExtra(
                    "url",
                    url
                ), LOGIN_REQ
            )
            storyDialog.dismiss()
        }
        tvCancel.setOnClickListener {
            storyDialog.dismiss()
        }
        storyDialog.setUpView(storyView)

        privateDialog = MyDialog(requireContext(), R.style.MyDialogTheme)
        val privateView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_remind, null)
        val title = privateView.findViewById<TextView>(R.id.title)
        title.text = getString(R.string.long_text2)
        val tvLogin2 = privateView.findViewById<TextView>(R.id.tv_login)
        val tvCancel2 = privateView.findViewById<TextView>(R.id.tv_cancel)
        tvLogin2.setOnClickListener {

            val url = "https://www.instagram.com/accounts/login"
            startActivityForResult(
                Intent(requireContext(), WebActivity::class.java).putExtra(
                    "url",
                    url
                ), LOGIN_REQ
            )
            privateDialog.dismiss()
        }
        tvCancel2.setOnClickListener {
            privateDialog.dismiss()
        }
        privateDialog.setUpView(privateView)

    }

    override fun initData() {
        firebaseAnalytics = Firebase.analytics
        recentAdapter = RecentAdapter(requireContext())
        mBinding.rv.adapter = recentAdapter
        mBinding.rv.layoutManager = GridLayoutManager(requireContext(), 4)
        recentAdapter.onItemClickListener = object : HistoryAdapter.OnItemClickListener {
            override fun onClick(position: Int) {
                val content = records[position].content

                startActivity(
                    Intent(
                        requireContext(),
                        BlogDetailsActivity::class.java
                    ).putExtra("content", content).putExtra("flag", false)
                )
            }

        }

    }

    override fun onResume() {
        super.onResume()

        getRecentData()
    }

    private fun initAds() {
        val adRequest = AdRequest.Builder().build();
        //inter
        InterstitialAd.load(requireContext(), "ca-app-pub-8609866682652024/3456228078", adRequest,
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

        mBinding.btnPaste.setOnClickListener {
            handleCopy()
        }
    }


    private fun autoStart() {

        mBinding.etShortcode.clearFocus()
        //8.1不加这句不行，还会弹出
        mBinding.flParent.requestFocus()
        KeyboardUtils.closeKeybord(mBinding.etShortcode, context)
        val url = mBinding.etShortcode.text.toString()

        val isValid = URLUtil.isValidUrl(url)
        if (!isValid) {
            Toast.makeText(requireContext(), getString(R.string.invalid_url), Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (url.contains("stories")) {

            if (BaseApplication.cookie == null) {
                storyDialog.show()
            } else {
                getStoryData()
            }

        } else {
            getMediaData()
        }


    }

    suspend fun getMediaDataByCookie() = withContext(Dispatchers.Main) {
        if (!progressDialog.isShowing){
            progressDialog.show()
        }
        lifecycleScope.launch {
            //检查是否已存在

            try {
                val map: HashMap<String, String> = HashMap()
                map["Cookie"] = BaseApplication.cookie!!
                map["User-Agent"] = Urls.USER_AGENT

                val map2: HashMap<String, String> = HashMap()
                map2["shortcode"] = getShortCode()!!

                val res = ApiClient.getClient()
                    .getMediaData(Urls.GRAPH_QL, map, Urls.QUERY_HASH, gson.toJson(map2))
                progressDialog.dismiss()
                val jsonObject = res.body()
                //Log.v(TAG, jsonObject.toString())
                if (res.code() == 200 && jsonObject != null) {

                    curMediaInfo = parseMedia2(jsonObject)
                    showCurrent()
                    mInterstitialAd?.show(requireActivity())
                    mBinding.progressbar.visibility = View.VISIBLE
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
                    mBinding.progressbar.visibility = View.INVISIBLE
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.download_finish),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    saveRecord()
                    getRecentData()

                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.not_found),
                        Toast.LENGTH_SHORT
                    ).show()

                }

            } catch (e: Exception) {
                mBinding.progressbar.visibility = View.INVISIBLE
                Log.e(TAG, e.message + "")
                context?.let {
                    Toast.makeText(it, getString(R.string.network), Toast.LENGTH_SHORT)
                        .show()
                }

                progressDialog.dismiss()

            }
        }

    }


    private fun getMediaData() {
        paths = StringBuffer()
        lifecycleScope.launch {
            //检查是否已存在
            val url = mBinding.etShortcode.text.toString()
            val record = RecordDB.getInstance().recordDao().findByUrl(url)

            if (record != null) {
//                curMediaInfo = gson.fromJson(record.content, MediaModel::class.java)
                getRecentData()
                Toast.makeText(requireContext(), getString(R.string.exist), Toast.LENGTH_SHORT)
                    .show()

                return@launch
            }
            progressDialog.show()

            try {
                val urlEncoded = handleUrl(url)
                //val api = "https://app.scrapingbee.com/api/v1/?api_key=${BaseApplication.APIKEY}&url=$urlEncoded&render_js=false&premium_proxy=true&country_code=us"
                val api = "http://api.scrape.do?token=${BaseApplication.APIKEY}&url=$urlEncoded"
                val res = ApiClient.getClient().getMediaNew(api)

                val jsonObject = res.body()
                //Log.v(TAG, jsonObject.toString())
                if (res.code() == 200 && jsonObject != null) {
                    progressDialog.dismiss()
                    curMediaInfo = parseMedia(jsonObject)
                    showCurrent()
                    mInterstitialAd?.show(requireActivity())
                    mBinding.progressbar.visibility = View.VISIBLE
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
                    mBinding.progressbar.visibility = View.INVISIBLE
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.download_finish),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    saveRecord()
                    getRecentData()

                } else {
                    if (BaseApplication.cookie == null) {
                        if (!requireActivity().isFinishing) {
                            privateDialog.show()
                        }
                        progressDialog.dismiss()
                    } else {
                        getMediaDataByCookie()
                    }

                }

            } catch (e: Exception) {
                mBinding.progressbar.visibility = View.INVISIBLE
                Log.e(TAG, e.message + "")
                context?.let {
                    Toast.makeText(it, getString(R.string.network), Toast.LENGTH_SHORT)
                        .show()
                }

                progressDialog.dismiss()

            }
        }
    }

    private fun handleUrl(url: String): String {
        val urlEncoded: String

        val strs = url.split("?")
        val str = strs[0] + "?__a=1&__d=dis"
        urlEncoded = URLEncoder.encode(str, "utf-8")
        return urlEncoded
    }


    /**
     * 获取story
     */
    private fun getStoryData() {
        paths = StringBuffer()
        lifecycleScope.launch {
            //检查是否已存在
            val myUrl = mBinding.etShortcode.text.toString()
            val record = RecordDB.getInstance().recordDao().findByUrl(myUrl)
            if (record != null) {
                //curMediaInfo = gson.fromJson(record.content, MediaModel::class.java)
                getRecentData()
                Toast.makeText(requireContext(), getString(R.string.exist), Toast.LENGTH_SHORT)
                    .show()

                return@launch
            }
            progressDialog.show()
            try {
                val map: HashMap<String, String> = HashMap()
                val cookie = BaseApplication.cookie
                map["Cookie"] = cookie!!
                map["User-Agent"] = Urls.USER_AGENT
                val pk = getShortCode()
                val url = Urls.PRIVATE_API + "/media/" + pk + "/info"
                val res = ApiClient.getClient()
                    .getStoryData(url, map)
                val code = res.code()
                val jsonObject = res.body()
                progressDialog.dismiss()
                if (code == 200 && jsonObject != null) {
                    curMediaInfo = parseStory(jsonObject)
                    showCurrent()
                    mInterstitialAd?.show(requireActivity())
                    mBinding.progressbar.visibility = View.VISIBLE
                    download(curMediaInfo)
                    saveRecord()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.download_finish),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    mBinding.progressbar.visibility = View.INVISIBLE
                    getRecentData()
                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }


            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                progressDialog.dismiss()
                context?.let {
                    Toast.makeText(
                        it,
                        getString(R.string.network),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }

    }

    /**
     * 刚搜索到先展示
     */
    private fun showCurrent() {
        mBinding.container.visibility = View.VISIBLE
        mBinding.username.text = curMediaInfo?.captionText
        Glide.with(requireContext()).load(curMediaInfo?.thumbnailUrl)
            .placeholder(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.gray_1)))
            .into(mBinding.picture)
        Glide.with(requireContext()).load(curMediaInfo?.profilePicUrl).circleCrop()
            .placeholder(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.gray_1)))
            .into(mBinding.avatar)
    }


    suspend fun saveRecord() {
        Log.v(TAG, "paths:" + paths)
        val url = mBinding.etShortcode.text.toString()
        val record =
            Record(
                null,
                gson.toJson(curMediaInfo),
                System.currentTimeMillis(),
                url,
                null,
                paths.toString()
            )
        RecordDB.getInstance().recordDao().insert(record)
    }


    private fun parseMedia(jsonObject: JsonObject): MediaModel {
        val mediaModel = MediaModel()
        val shortcode_media = jsonObject["graphql"].asJsonObject["shortcode_media"].asJsonObject
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


    private fun parseMedia2(jsonObject: JsonObject): MediaModel {
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

            val size = candidates.size()
            mediaModel.thumbnailUrl = candidates[size - 1].asJsonObject["url"].asString

            mediaModel.pk = item["pk"].asString

            if (mediaModel.mediaType == 2) {
                val video_versions = item["video_versions"].asJsonArray
                if (video_versions.size() > 0) {
                    mediaModel.videoUrl = video_versions[0].asJsonObject["url"].asString
                }
            }

            mediaModel.captionText =
                jsonObject.getNullable("caption")?.asJsonObject?.get("text")?.asString

        }

        return mediaModel

    }


    /**
     * 下载单个图片或视频
     */
    private suspend fun download(media: ResourceModel?) {

        if (media?.mediaType == 1) {
            //image
            try {
                val responseBody = ApiClient.getClient().downloadUrl(media.thumbnailUrl)
                withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeStream(responseBody.body()!!.byteStream())
                    val path = FileUtils.saveImageToAlbum(requireContext(), bitmap)
                    if (path != null) {
                        paths.append(path).append(",")
                    }

                }
            } catch (e: Exception) {
                context?.let {
                    Toast.makeText(it, "time out", Toast.LENGTH_SHORT).show()
                    sendToFirebase(e)
                }
            }


        } else if (media?.mediaType == 2) {
            //video
            if (media.videoUrl != null) {
                try {
                    val responseBody = ApiClient.getClient().downloadUrl(media.videoUrl!!)
                    withContext(Dispatchers.IO) {
                        val path = FileUtils.saveVideoToAlbum(
                            requireContext(),
                            responseBody.body()!!.byteStream()
                        )
                        paths.append(path).append(",")
                    }
                } catch (e: Exception) {
                    context?.let {
                        Toast.makeText(it, "time out", Toast.LENGTH_SHORT).show()
                        sendToFirebase(e)
                    }
                }


            }

        }
    }

    private fun getRecentData() {
        val medias: ArrayList<MediaModel> = ArrayList()
//        val igstory = MediaModel()
//        igstory.captionText = "StorySaver"
//        medias.add(igstory)
        lifecycleScope.launch {
            records = RecordDB.getInstance().recordDao().recent() as ArrayList<Record>
            Log.v(TAG, records.size.toString())
            if (records.size > 0) {
                mBinding.container2.visibility = View.VISIBLE
                for (record in records) {

                    val mediaModel = gson.fromJson(record.content, MediaModel::class.java)
                    medias.add(mediaModel)

                }

                curMediaInfo = gson.fromJson(records[0].content, MediaModel::class.java)
                mBinding.container.visibility = View.VISIBLE
                mBinding.username.text = curMediaInfo?.captionText
                Glide.with(requireContext()).load(curMediaInfo?.thumbnailUrl)
                    .placeholder(
                        ColorDrawable(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.gray_1
                            )
                        )
                    )
                    .into(mBinding.picture)
                Glide.with(requireContext()).load(curMediaInfo?.profilePicUrl).circleCrop()
                    .placeholder(
                        ColorDrawable(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.gray_1
                            )
                        )
                    )
                    .into(mBinding.avatar)

            }else{
                mBinding.container2.visibility = View.GONE
            }

            recentAdapter.setDatas(medias)
        }

    }


    @Subscribe
    fun onKeywordReceive(intentEvent: IntentEvent) {

        val keyword = intentEvent.str
        mBinding.etShortcode.setText(keyword)

        if (BaseApplication.autodownload) {
            autoStart()
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQ && resultCode == 200) {
            val url = mBinding.etShortcode.text.toString()
            if (url.contains("stories")) {
                getStoryData()
            } else {
                lifecycleScope.launch {
                    getMediaDataByCookie()
                }

            }

        }
    }

    private fun getShortCode(): String? {
        val shortCode: String?
        val url = mBinding.etShortcode.text.toString()
        shortCode = if (!url.contains("stories")) {
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

    fun handleCopy() {
        mBinding.btnPaste.post {
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.let {
                //fix null pointer
                mBinding.etShortcode.setText(it.text)
            }

        }

    }

    private fun sendToFirebase(e: Exception) {
        val analytics = Firebase.analytics
        if (e.message != null) {
            analytics.logEvent("app_my_exception") {
                param("my_exception", e.message!!)
            }
        }

    }
}