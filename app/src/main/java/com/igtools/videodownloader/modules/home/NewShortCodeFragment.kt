package com.igtools.videodownloader.modules.home

import android.app.ProgressDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Environment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.BuildConfig
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
import com.igtools.videodownloader.utils.*
import com.igtools.videodownloader.widgets.dialog.MyDialog
import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.breakpoint.BlockInfo
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.DownloadListener4
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
import com.liulishuo.okdownload.core.listener.assist.Listener4Assist
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
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
    var totalLen: Long = 0
    var isDownloading = false
    private val LOGIN_REQ = 1000
    var currentCount = 0
    var totalCount = 0
    val INDEX_TAG = 1
    lateinit var myAlert: AlertDialog
    private val PERMISSION_REQ = 1024
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
            if (!isDownloading) {
                startActivity(
                    Intent(requireContext(), BlogDetailsActivity::class.java)
                        .putExtra("content", gson.toJson(curMediaInfo))
                        .putExtra("need_download", false)
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
        mBinding.imgCamera.setOnClickListener {

            val launchIntent =
                requireActivity().packageManager.getLaunchIntentForPackage("com.instagram.android")
            launchIntent?.let { startActivity(it) }
        }


        //version 52 fix scrollview edittext focus bug
//        val view = mBinding.scrollview
//        view.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS)
//        view.isFocusable = true
//        view.isFocusableInTouchMode = true
//        view.setOnTouchListener { v, event ->
//            v.requestFocusFromTouch()
//            false
//        }
        //the code above will cause another problem, the background color will get changed.so son't use it

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
        mBinding.etShortcode.clearFocus()
        mBinding.flParent.requestFocus()
        KeyboardUtils.hideInputForce(requireActivity())

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

        //权限dialog
        myAlert = AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.need_permission))
            .setPositiveButton(
                R.string.settings
            ) { dialog, _ ->
                val intent = Intent();
                intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS";
                intent.data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                startActivity(intent);
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, _ -> dialog.dismiss() }
            .create()
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

        //permission check first
        if (!PermissionUtils.checkPermissionsForReadAndRight(requireActivity())){
            PermissionUtils.requirePermissionsInFragment(this,PERMISSION_REQ)
            return
        }

        mBinding.etShortcode.clearFocus()
        mBinding.flParent.requestFocus()
        KeyboardUtils.closeKeybord(mBinding.etShortcode, context)

        lifecycleScope.launch {
            val record = RecordDB.getInstance().recordDao().findByUrl(paramString)
            if (record != null) {
                //curMediaInfo = gson.fromJson(record.content, MediaModel::class.java)
                Toast.makeText(requireContext(), getString(R.string.exist), Toast.LENGTH_SHORT)
                    .show()

                return@launch
            }

            if (paramString.matches(Regex("(.*)instagram.com/p(.*)")) || paramString.matches(Regex("(.*)instagram.com/reel(.*)"))) {
                val url = emBedUrl()
                Log.v(TAG, url)
                loadData(url)
            } else if (paramString.matches(Regex("(.*)instagram.com/stories/(.*)"))) {
                if (BaseApplication.cookie == null) {
                    storyDialog.show()
                } else {
                    getStoryData()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.invalid_url),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }

        }

    }


    private fun loadData(sourceUrl: String) {

        progressDialog.show()
        clearData()

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
                            if (!isInvalidContext()) {
                                progressDialog.dismiss()
                            }

                            showCurrent()
                            mInterstitialAd?.show(requireActivity())
                            mBinding.progressbar.visibility = View.VISIBLE

                            isDownloading = true
                            if (curMediaInfo?.mediaType == 8) {
                                downloadMultiple(curMediaInfo!!)
                            } else {
                                downloadSingle(curMediaInfo!!)
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
                        if (!isInvalidContext()) {
                            progressDialog.dismiss()
                        }

                        showCurrent()
                        mInterstitialAd?.show(requireActivity())
                        mBinding.progressbar.visibility = View.VISIBLE

                        isDownloading = true
                        downloadSingle(curMediaInfo!!)

                    }

                } else {

                    //如果extra里面是null，则用原来的方法尝试获取
                    Analytics.sendEvent("use_a1", "media_type", mediatype)
                    val myUrl = mBinding.etShortcode.text.toString()
                    getMediaData(myUrl)

                }

            } catch (e: Exception) {
                //私人账户
                Log.e(TAG, e.message + "")
                activity?.runOnUiThread {
                    if (!isInvalidContext()) {
                        progressDialog.dismiss()
                        privateDialog.show()
                    }

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
                    if (!isInvalidContext()) {
                        progressDialog.dismiss()
                    }

                    val shortcode_media =
                        jsonObject["graphql"].asJsonObject["shortcode_media"].asJsonObject
                    curMediaInfo = parseMedia(shortcode_media)
                    showCurrent()
                    mInterstitialAd?.show(requireActivity())
                    mBinding.progressbar.visibility = View.VISIBLE

                    isDownloading = true
                    if (curMediaInfo?.mediaType == 8) {
                        downloadMultiple(curMediaInfo!!)
                    } else {
                        downloadSingle(curMediaInfo!!)
                    }
                } else {
                    if (BaseApplication.cookie == null) {
                        if (!isInvalidContext()) {
                            privateDialog.show()
                            progressDialog.dismiss()
                        }
                    } else {
                        getMediaDataByCookie()
                    }

                }

            } catch (e: Exception) {
                mBinding.progressbar.visibility = View.INVISIBLE
                mBinding.progressbar.setValue(0f)
                Log.e(TAG, e.message + "")
                safeToast(R.string.network)
                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }

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
                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }

                val jsonObject = res.body()
                //Log.v(TAG, jsonObject.toString())
                if (res.code() == 200 && jsonObject != null) {
                    val shortcode_media =
                        jsonObject["data"].asJsonObject["shortcode_media"].asJsonObject
                    curMediaInfo = parseMedia(shortcode_media)
                    showCurrent()
                    mInterstitialAd?.show(requireActivity())
                    mBinding.progressbar.visibility = View.VISIBLE

                    isDownloading = true
                    if (curMediaInfo?.mediaType == 8) {
                        downloadMultiple(curMediaInfo!!)
                    } else {
                        downloadSingle(curMediaInfo!!)
                    }
                } else {
                    safeToast(R.string.not_found)
                }

            } catch (e: Exception) {
                mBinding.progressbar.visibility = View.INVISIBLE
                mBinding.progressbar.setValue(0f)
                Log.e(TAG, e.message + "")
                safeToast(R.string.network)
                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }


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
        clearData()
        lifecycleScope.launch {

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
                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }

                if (code == 200 && jsonObject != null) {
                    curMediaInfo = parseStory(jsonObject)
                    showCurrent()
                    mInterstitialAd?.show(requireActivity())
                    mBinding.progressbar.visibility = View.VISIBLE
                    isDownloading = true

                    downloadSingle(curMediaInfo!!)


                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    safeToast(R.string.failed)
                }


            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }

                safeToast(R.string.network)

            }
        }

    }

    private fun clearData() {
        currentCount = 0
        totalCount = 0
        totalLen = 0
        paths.clear()
        curMediaInfo = null
        curRecord = null
        isDownloading = false

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

                isDownloading = false
                e.message?.let {
                    Analytics.sendException("app_my_exception", Analytics.ERROR_KEY, it)
                }

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
                    isDownloading = false
                    e.message?.let {
                        Analytics.sendException("app_my_exception", Analytics.ERROR_KEY, it)
                    }

                }


            }

        }
    }


    private fun downloadSingle(mediaInfo: MediaModel) {
        val parentFile = createDirDownload()
        val task: DownloadTask
        if (mediaInfo.mediaType == 1) {
            task = DownloadTask.Builder(mediaInfo.thumbnailUrl, parentFile)
                .setConnectionCount(1)
                // the minimal interval millisecond for callback progress
                .setMinIntervalMillisCallbackProcess(16)
                // ignore the same task has already completed in the past.
                .setPassIfAlreadyCompleted(false)
                .build()
            task.addTag(INDEX_TAG, 1)
        } else {
            task = DownloadTask.Builder(mediaInfo.videoUrl!!, parentFile)
                .setConnectionCount(1)
                // the minimal interval millisecond for callback progress
                .setMinIntervalMillisCallbackProcess(16)
                // ignore the same task has already completed in the past.
                .setPassIfAlreadyCompleted(false)
                .build()
            task.addTag(INDEX_TAG, 2)
        }

        task.enqueue(object : DownloadListener4() {
            override fun taskStart(task: DownloadTask) {
                Toast.makeText(requireContext(), R.string.download_start, Toast.LENGTH_SHORT).show()
            }

            override fun connectStart(
                task: DownloadTask,
                blockIndex: Int,
                requestHeaderFields: MutableMap<String, MutableList<String>>
            ) {

            }

            override fun connectEnd(
                task: DownloadTask,
                blockIndex: Int,
                responseCode: Int,
                responseHeaderFields: MutableMap<String, MutableList<String>>
            ) {

            }

            override fun taskEnd(
                task: DownloadTask,
                cause: EndCause?,
                realCause: java.lang.Exception?,
                model: Listener4Assist.Listener4Model
            ) {

                Log.e(TAG, realCause?.message + "")
                if (realCause != null){

                    Analytics.sendException("download_fail",Analytics.ERROR_KEY, realCause.message+"")
                    safeToast(R.string.download_failed)
                    return
                }
                val tempFile = task.file
                if (tempFile != null && tempFile.exists()){
                    if (task.getTag(INDEX_TAG) == 1) {
                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        if (bitmap != null) {
                            val path = FileUtils.saveImageToAlbum(BaseApplication.mContext, bitmap)
                            if (path != null) {
                                paths[task.url] = path
                            }
                            tempFile.delete()
                        }
                    } else {
                        tempFile.inputStream().use {
                            val path = FileUtils.saveVideoToAlbum(BaseApplication.mContext, it)
                            if (path != null) {
                                paths[task.url] = path
                            }
                        }
                        tempFile.delete()
                    }

                    lifecycleScope.launch {
                        saveRecord()
                    }
                    isDownloading = false
                    mBinding.progressbar.visibility = View.INVISIBLE
                    mBinding.progressbar.setValue(0f)
                    safeToast(R.string.download_finish)
                }

            }


            override fun infoReady(
                task: DownloadTask?,
                info: BreakpointInfo,
                fromBreakpoint: Boolean,
                model: Listener4Assist.Listener4Model
            ) {
                totalLen = info.totalLength
            }

            override fun progressBlock(
                task: DownloadTask?,
                blockIndex: Int,
                currentBlockOffset: Long
            ) {

            }

            override fun progress(task: DownloadTask?, currentOffset: Long) {
                Log.v(TAG, "current thread is：" + Thread.currentThread().name)
                val percent = (currentOffset.toFloat()) * 100 / totalLen
                mBinding.progressbar.setValue(percent)
            }

            override fun blockEnd(task: DownloadTask?, blockIndex: Int, info: BlockInfo?) {

            }

        })

    }

    private fun downloadMultiple(mediaInfo: MediaModel) {

        val fileDir = createDirDownload()

        val builder = DownloadContext.QueueSet()
            .setMinIntervalMillisCallbackProcess(300)
            .commit()

        for (res in mediaInfo.resources) {
            val url = if (res.mediaType == 1) {
                res.thumbnailUrl
            } else {
                res.videoUrl!!
            }

            val taskBuilder = DownloadTask.Builder(url, fileDir)
                .setConnectionCount(1)
                // the minimal interval millisecond for callback progress
                .setMinIntervalMillisCallbackProcess(16)
                // ignore the same task has already completed in the past.
                .setPassIfAlreadyCompleted(false)

            builder.bind(taskBuilder).addTag(INDEX_TAG, res.mediaType)

        }

        totalCount = mediaInfo.resources.size
        currentCount = 0

        val downloadContext = builder.setListener(object : DownloadContextListener {
            override fun taskEnd(
                context: DownloadContext,
                task: DownloadTask,
                cause: EndCause,
                realCause: java.lang.Exception?,
                remainCount: Int
            ) {
                Log.e(TAG, realCause?.message + "")

                if (realCause != null){
                    Analytics.sendException("download_fail",Analytics.ERROR_KEY, realCause.message+"")
                    //safeToast(R.string.download_failed)
                    return
                }
                val tempFile = task.file
                if(tempFile != null && tempFile.exists()){
                    if (task.getTag(INDEX_TAG) == 1) {

                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        if (bitmap != null) {
                            val path = FileUtils.saveImageToAlbum(BaseApplication.mContext, bitmap)
                            if (path != null) {
                                paths[task.url] = path
                            }
                            tempFile.delete()
                        }

                    } else {
                        tempFile.inputStream().use {
                            val path = FileUtils.saveVideoToAlbum(BaseApplication.mContext, it)
                            if (path != null) {
                                paths[task.url] = path
                            }
                        }
                        tempFile.delete()
                    }
                }

            }

            override fun queueEnd(context: DownloadContext) {
                if (currentCount != totalCount){
                    safeToast(R.string.download_failed)
                }else{
                    lifecycleScope.launch {
                        saveRecord()
                    }
                    isDownloading = false
                    mBinding.progressbar.visibility = View.INVISIBLE
                    mBinding.progressbar.setValue(0f)
                    safeToast(R.string.download_finish)
                }

            }

        }).build()

        downloadContext?.start(object : DownloadListener1() {
            override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {

            }

            override fun taskEnd(
                task: DownloadTask,
                cause: EndCause,
                realCause: java.lang.Exception?,
                model: Listener1Assist.Listener1Model
            ) {
                Log.v(TAG, "task end---")
                currentCount += 1
                mBinding.progressbar.setValue(currentCount.toFloat() * 100 / totalCount)
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {

            }

            override fun connected(
                task: DownloadTask,
                blockCount: Int,
                currentOffset: Long,
                totalLength: Long
            ) {

            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {

            }

        }, false)

    }


    fun createDirDownload(): File {

        val fileDir = activity?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }

        return fileDir
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


    override fun onDetach() {
        super.onDetach()

        Log.v(TAG, "on detach")

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.v(TAG,"result")
        if (requestCode == PERMISSION_REQ){
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    myAlert.show()
                    return
                }
            }
        }


    }
}