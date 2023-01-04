package com.igtools.videodownloader.modules.home

import android.app.ProgressDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.R
import com.igtools.videodownloader.api.ApiClient
import com.igtools.videodownloader.api.Urls
import com.igtools.videodownloader.base.BaseFragment
import com.igtools.videodownloader.databinding.FragmentNewShortCodeBinding
import com.igtools.videodownloader.models.IntentEvent
import com.igtools.videodownloader.models.MediaModel
import com.igtools.videodownloader.models.Record
import com.igtools.videodownloader.modules.details.BlogDetailsActivity
import com.igtools.videodownloader.modules.web.WebActivity
import com.igtools.videodownloader.room.RecordDB
import com.igtools.videodownloader.utils.FileUtils
import com.igtools.videodownloader.utils.KeyboardUtils
import com.igtools.videodownloader.utils.UrlUtils
import com.igtools.videodownloader.utils.getNullable
import com.igtools.videodownloader.widgets.dialog.MyDialog
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

class NewShortCodeFragment : BaseFragment<FragmentNewShortCodeBinding>() {

    val TAG = "NewShortCodeFragment"

    lateinit var progressDialog: ProgressDialog
    lateinit var privateDialog: MyDialog
    lateinit var storyDialog: MyDialog
    var mInterstitialAd: InterstitialAd? = null
    var curMediaInfo: MediaModel? = null
    var curRecord: Record? = null
    var paths: HashMap<String, String> = HashMap()

    val imageUrl = "https://www.instagram.com/p/-vSJNUDKKD/embed/captioned/"
    val videoUrl = "https://www.instagram.com/reel/Cm6TeFHJ0xy/embed/captioned/"
    val sideUrl = "https://www.instagram.com/p/Cm8qU92ykVU/embed/captioned/"

    var downloadSuccess = true
    private val LOGIN_REQ = 1000

    override fun getLayoutId(): Int {
        return R.layout.fragment_new_short_code
    }

    override fun initView() {

        initDialog()
        initAds()
        mBinding.btnDownload.setOnClickListener {
            autoStart()
        }

        mBinding.btnPaste.setOnClickListener {
            handleCopy()
        }

        mBinding.container.setOnClickListener {
            if (curMediaInfo != null && curRecord != null) {
                startActivity(
                    Intent(requireContext(), BlogDetailsActivity::class.java)
                        .putExtra("content", gson.toJson(curMediaInfo))
                        .putExtra("flag", false)
                        .putExtra("record", gson.toJson(curRecord))
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.downloading),
                    Toast.LENGTH_SHORT
                ).show()
            }

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


    override fun initData() {
        //mBinding.webview.loadUrl(sideUrl)
    }

    //ui part
    private fun initDialog() {

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.searching))
        progressDialog.setCancelable(false)

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


    }

    //data part
    private fun autoStart() {
        val paramString = mBinding.etShortcode.text.toString()
        if (TextUtils.isEmpty(paramString)) {
            return
        }
        mBinding.etShortcode.clearFocus()
        KeyboardUtils.closeKeybord(mBinding.etShortcode, context)
        if (paramString.matches(Regex("(.*)instagram.com/p(.*)")) || paramString.matches(Regex("(.*)instagram.com/reel(.*)"))) {
            val url = emBedUrl()
            Log.v(TAG, url)

            lifecycleScope.launch {
                val record = RecordDB.getInstance().recordDao().findByUrl(paramString)
                if (record != null) {
                    //curMediaInfo = gson.fromJson(record.content, MediaModel::class.java)
                    Toast.makeText(requireContext(), getString(R.string.exist), Toast.LENGTH_SHORT)
                        .show()

                    return@launch
                }

                Log.v(TAG, "time1:${System.currentTimeMillis()}")

                loadData(url)
            }

        } else if (paramString.matches(Regex("(.*)instagram.com/stories/(.*)"))) {
            if (BaseApplication.cookie == null) {
                storyDialog.show()
            } else {
                getStoryData()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.invalid_url), Toast.LENGTH_SHORT)
                .show()
        }

    }


    private fun loadData(sourceUrl: String) {

        progressDialog.show()
        curMediaInfo = null
        paths.clear()
        curRecord = null
        downloadSuccess = true

        Thread {
            try {
                val doc: Document = Jsoup.connect(sourceUrl).userAgent(Urls.USER_AGENT).get()
                //val doc = Jsoup.parse(html)
                Log.v(TAG, doc.title())
                val scripts = doc.getElementsByTag("script")
                //1.如果extra里面有数据，直接提取
                for (script in scripts) {

                    if (script.data().contains("shortcode_media")) {

                        val data = script.data()
                        val strs = data.split("'extra',")
                        val target = strs[1].substring(0, strs[1].length - 2)
                        Log.v(TAG, target)

                        val jsonObject = JsonParser().parse(target).asJsonObject
                        val shortcode_media = jsonObject["shortcode_media"].asJsonObject

                        curMediaInfo = parseMedia(shortcode_media)

                        activity?.runOnUiThread {

                            progressDialog.dismiss()
                            showCurrent()
                            mInterstitialAd?.show(requireActivity())
                            mBinding.progressbar.visibility = View.VISIBLE
                            lifecycleScope.launch {
                                if (curMediaInfo?.mediaType == 8) {
                                    val all: List<Deferred<Unit>> = curMediaInfo!!.resources.map {
                                        async {
                                            download(it)
                                        }
                                    }

                                    all.awaitAll()
                                } else {
                                    download(curMediaInfo!!)
                                }

                                if (!downloadSuccess) {
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.download_failed),
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    mBinding.progressbar.visibility = View.INVISIBLE
                                    return@launch
                                }
                                mBinding.progressbar.visibility = View.INVISIBLE
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.download_finish),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                saveRecord()
                            }

                        }

                        return@Thread
                    }
                }

                //2.如果extra里面是null

                val embed = doc.getElementsByClass("Embed ")[0]
                val mediatype = embed.attr("data-media-type")
                if (mediatype == "GraphImage") {
                    curMediaInfo = MediaModel()
                    curMediaInfo?.mediaType = 1
                    curMediaInfo?.code = getShortCode() ?: ""
                    parseImage(doc)

                    activity?.runOnUiThread {

                        progressDialog.dismiss()
                        showCurrent()
                        mInterstitialAd?.show(requireActivity())
                        mBinding.progressbar.visibility = View.VISIBLE
                        lifecycleScope.launch {
                            download(curMediaInfo!!)
                            if (!downloadSuccess) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.download_failed),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                mBinding.progressbar.visibility = View.INVISIBLE
                                return@launch
                            }
                            mBinding.progressbar.visibility = View.INVISIBLE
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.download_finish),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            saveRecord()
                        }

                    }

                } else {

                    //如果extra里面是null，则用原来的方法尝试获取
                    val myUrl = mBinding.etShortcode.text.toString()
                    getMediaData(myUrl)

                }

            } catch (e: Exception) {
                //私人账户
                Log.e(TAG, e.message + "")
                activity?.runOnUiThread {
                    progressDialog.dismiss()
                    privateDialog.show()

                }
            }

        }.start()

    }

    private fun parseImage(doc: Document) {
        getUserInfo(doc)
        getCaption(doc)
        val imageUrl = doc.getElementsByClass("EmbeddedMediaImage")[0].attr("src")
        //Log.v(TAG, imageUrl)
        curMediaInfo?.thumbnailUrl = imageUrl
        Log.v(TAG, curMediaInfo.toString())
    }

    private fun getUserInfo(doc: Document) {
        if (doc.getElementsByClass("Avatar").size > 0) {
            val a = doc.getElementsByClass("Avatar")[0]
            val img = a.getElementsByTag("img")[0]
            curMediaInfo?.profilePicUrl = img.attr("src")
        } else if (doc.getElementsByClass("CollabAvatar").size > 0) {
            val a = doc.getElementsByClass("CollabAvatar")[0]
            val img = a.getElementsByTag("img")[0]
            curMediaInfo?.profilePicUrl = img.attr("src")
        }

        val a2 = doc.getElementsByClass("HeaderText")[0]
        val span = a2.getElementsByTag("span")[0]
        curMediaInfo?.username = span.text()
    }


    private fun getCaption(doc: Document) {
        if (doc.getElementsByClass("Caption").size > 0) {
            val captionDiv = doc.getElementsByClass("Caption")[0]
            val userEle = captionDiv.child(0)
            val commentsEle = captionDiv.child(captionDiv.childrenSize() - 1)
            userEle.remove()
            commentsEle.remove()
            val content = captionDiv.text()
            Log.v(TAG, content)
            curMediaInfo?.captionText = content
        }
    }


    private fun getMediaData(myUrl: String) {

        lifecycleScope.launch {

            try {
                val urlEncoded = handleUrl(myUrl)
                //val api = "https://app.scrapingbee.com/api/v1/?api_key=${BaseApplication.APIKEY}&url=$urlEncoded&render_js=false&premium_proxy=true&country_code=us"
                val api = "https://api.scrape.do?token=${BaseApplication.APIKEY}&url=$urlEncoded"
                val res = ApiClient.getClient().getMediaNew(api)

                val jsonObject = res.body()
                //Log.v(TAG, jsonObject.toString())
                if (res.code() == 200 && jsonObject != null) {
                    progressDialog.dismiss()
                    val shortcode_media =
                        jsonObject["graphql"].asJsonObject["shortcode_media"].asJsonObject
                    curMediaInfo = parseMedia(shortcode_media)
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
                        download(curMediaInfo!!)
                    }

                    if (!downloadSuccess) {
                        mBinding.progressbar.visibility = View.INVISIBLE
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.download_failed),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        return@launch
                    }

                    mBinding.progressbar.visibility = View.INVISIBLE
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.download_finish),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    saveRecord()

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

    suspend fun getMediaDataByCookie() {
        if (!progressDialog.isShowing) {
            progressDialog.show()
        }
        lifecycleScope.launch {
            //检查是否已存在

            try {
                val map: HashMap<String, String> = HashMap()
                map["Cookie"] = BaseApplication.cookie!!
                map["User-Agent"] = Urls.USER_AGENT

                val map2: HashMap<String, String> = HashMap()
                val shortCode = getShortCode() ?: return@launch
                map2["shortcode"] = shortCode

                val res = ApiClient.getClient()
                    .getMediaData(Urls.GRAPH_QL, map, Urls.QUERY_HASH, gson.toJson(map2))
                progressDialog.dismiss()
                val jsonObject = res.body()
                //Log.v(TAG, jsonObject.toString())
                if (res.code() == 200 && jsonObject != null) {
                    val shortcode_media =
                        jsonObject["data"].asJsonObject["shortcode_media"].asJsonObject
                    curMediaInfo = parseMedia(shortcode_media)
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
                        download(curMediaInfo!!)
                    }

                    if (!downloadSuccess) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.download_failed),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        mBinding.progressbar.visibility = View.INVISIBLE
                        return@launch
                    }

                    mBinding.progressbar.visibility = View.INVISIBLE
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.download_finish),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    saveRecord()

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

    private fun parseMedia(shortcode_media: JsonObject): MediaModel {
        val mediaModel = MediaModel()

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
                    val resource = MediaModel()
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

    /**
     * 获取story
     */
    private fun getStoryData() {
        paths.clear()
        curMediaInfo = null
        curRecord = null
        downloadSuccess = true
        lifecycleScope.launch {
            //检查是否已存在
            val myUrl = mBinding.etShortcode.text.toString()
            val record = RecordDB.getInstance().recordDao().findByUrl(myUrl)
            if (record != null) {
                //curMediaInfo = gson.fromJson(record.content, MediaModel::class.java)
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
                    download(curMediaInfo!!)

                    if (!downloadSuccess) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.download_failed),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        mBinding.progressbar.visibility = View.INVISIBLE
                        return@launch
                    }

                    saveRecord()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.download_finish),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    mBinding.progressbar.visibility = View.INVISIBLE

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
     * 刚搜索到先展示
     */
    private fun showCurrent() {
        mBinding.container.visibility = View.VISIBLE
        mBinding.username.text = curMediaInfo?.captionText
        if (curMediaInfo?.mediaType != 8) {
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
        } else {
            if (curMediaInfo?.resources?.size!! > 0) {
                Glide.with(requireContext()).load(curMediaInfo?.resources?.get(0)?.thumbnailUrl)
                    .placeholder(
                        ColorDrawable(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.gray_1
                            )
                        )
                    )
                    .into(mBinding.picture)
            }

        }

        Glide.with(requireContext()).load(curMediaInfo?.profilePicUrl).circleCrop()
            .placeholder(ColorDrawable(ContextCompat.getColor(requireContext(), R.color.gray_1)))
            .into(mBinding.avatar)
    }


    suspend fun saveRecord() {

        val url = mBinding.etShortcode.text.toString()
        curRecord =
            Record(
                null,
                gson.toJson(curMediaInfo),
                System.currentTimeMillis(),
                url,
                null,
                gson.toJson(paths)
            )
        RecordDB.getInstance().recordDao().insert(curRecord)

    }

    /**
     * 下载单个图片或视频
     */
    private suspend fun download(media: MediaModel) {

        if (media.mediaType == 1) {
            //image
            try {
                val responseBody = ApiClient.getClient4().downloadUrl(media.thumbnailUrl)
                withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeStream(responseBody.body()!!.byteStream())
                    val path = FileUtils.saveImageToAlbum(requireContext(), bitmap)
                    if (path != null) {
                        paths[media.thumbnailUrl] = path
                    }

                }
            } catch (e: Exception) {

                downloadSuccess = false
                sendToFirebase(e)

            }


        } else if (media.mediaType == 2) {
            //video
            if (media.videoUrl != null) {
                try {
                    val responseBody = ApiClient.getClient4().downloadUrl(media.videoUrl!!)
                    withContext(Dispatchers.IO) {
                        val path = FileUtils.saveVideoToAlbum(
                            requireContext(),
                            responseBody.body()!!.byteStream()
                        )
                        paths[media.videoUrl!!] = path!!
                    }
                } catch (e: Exception) {
                    downloadSuccess = false
                    sendToFirebase(e)

                }


            }

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

    private fun sendToFirebase(e: Exception) {
        val analytics = Firebase.analytics
        if (e.message != null) {
            analytics.logEvent("app_my_exception") {
                param("my_exception", e.message!!)
            }
        }

    }


    //method part
    private fun emBedUrl(): String {

        val url = mBinding.etShortcode.text.toString().split("?")[0]
        return url + "embed/captioned/"
    }

    private fun handleUrl(url: String): String {
        val urlEncoded: String

        val strs = url.split("?")
        val str = strs[0] + "?__a=1&__d=dis"
        urlEncoded = URLEncoder.encode(str, "utf-8")
        return urlEncoded
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

}