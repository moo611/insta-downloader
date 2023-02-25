package com.igtools.insta.videodownloader.modules.home

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
import com.igtools.insta.videodownloader.BaseApplication
import com.igtools.insta.videodownloader.BuildConfig
import com.igtools.insta.videodownloader.R
import com.igtools.insta.videodownloader.api.ApiClient
import com.igtools.insta.videodownloader.api.Urls
import com.igtools.insta.videodownloader.base.BaseFragment
import com.igtools.insta.videodownloader.databinding.FragmentNewShortCodeBinding
import com.igtools.insta.videodownloader.download.DownloadFail
import com.igtools.insta.videodownloader.download.DownloadProgress
import com.igtools.insta.videodownloader.download.DownloadSuccess
import com.igtools.insta.videodownloader.download.MyService
import com.igtools.insta.videodownloader.models.IntentEvent
import com.igtools.insta.videodownloader.models.MediaModel
import com.igtools.insta.videodownloader.modules.details.BlogDetailsActivity
import com.igtools.insta.videodownloader.modules.web.WebActivity
import com.igtools.insta.videodownloader.room.RecordDB
import com.igtools.insta.videodownloader.utils.*
import com.igtools.insta.videodownloader.widgets.dialog.MyDialog
import com.liulishuo.okdownload.OkDownloadProvider
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder


class NewShortCodeFragment : BaseFragment<FragmentNewShortCodeBinding>() {

    val TAG = "NewShortCodeFragment"

    lateinit var progressDialog: ProgressDialog
    lateinit var privateDialog: AlertDialog
    lateinit var storyDialog: AlertDialog
    //var mInterstitialAd: InterstitialAd? = null
    var curMediaInfo: MediaModel? = null


    private val LOGIN_REQ = 1000

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
            if(MyService.isDownloading){
                Toast.makeText(requireContext(),R.string.downloading,Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(
                Intent(requireContext(), BlogDetailsActivity::class.java)
                    .putExtra("content", gson.toJson(curMediaInfo))
                    .putExtra("url", mBinding.etShortcode.text.toString())

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
        mBinding.imgCamera.setOnClickListener {

            val launchIntent =
                requireActivity().packageManager.getLaunchIntentForPackage("com.instagram.android")
            launchIntent?.let { startActivity(it) }
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


    override fun initData() {
        //mBinding.webview.loadUrl(sideUrl)
        mBinding.etShortcode.clearFocus()
        mBinding.flParent.requestFocus()
        KeyboardUtils.hideInputForce(requireActivity())

    }

    //ui part
    private fun initDialog() {

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage(getString(R.string.search_wait))
        progressDialog.setCancelable(false)

        //story dialog
        val builder = AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.login);
        builder.setMessage(R.string.long_text1);
        builder.setIcon(R.mipmap.icon);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);
        //设置正面按钮
        builder.setPositiveButton(R.string.ok, object :DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val url = "https://www.instagram.com/accounts/login"
                startActivityForResult(
                    Intent(requireContext(), WebActivity::class.java).putExtra(
                        "url",
                        url
                    ), LOGIN_REQ
                )
                storyDialog.dismiss()
            }

        });
        //设置反面按钮
        builder.setNegativeButton(R.string.cancel, object :DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
                storyDialog.dismiss()
            }

        });
        storyDialog = builder.create()


        val builder2 = AlertDialog.Builder(requireContext());
        builder2.setTitle(R.string.login);
        builder2.setMessage(R.string.long_text2);
        builder2.setIcon(R.mipmap.icon);
        //点击对话框以外的区域是否让对话框消失
        builder2.setCancelable(true);
        //设置正面按钮
        builder2.setPositiveButton(R.string.ok, object :DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val url = "https://www.instagram.com/accounts/login"
                startActivityForResult(
                    Intent(requireContext(), WebActivity::class.java).putExtra(
                        "url",
                        url
                    ), LOGIN_REQ
                )
                privateDialog.dismiss()
            }

        });
        //设置反面按钮
        builder2.setNegativeButton(R.string.cancel, object :DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
                privateDialog.dismiss()
            }

        });
        privateDialog = builder2.create()

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

        //banner
        mBinding.adView.loadAd(adRequest)


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

    //data part
    private fun autoStart() {
        val paramString = mBinding.etShortcode.text.toString()
        if (TextUtils.isEmpty(paramString)) {
            return
        }

        //permission check first
        if (!PermissionUtils.checkPermissionsForReadAndRight(requireActivity())) {
            PermissionUtils.requirePermissionsInFragment(this, PERMISSION_REQ)
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
                val sourceUrl = mBinding.etShortcode.text.toString()

                loadData(url, sourceUrl)


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


    private fun loadData(embedUrl: String, sourceUrl: String) {

        progressDialog.show()
        lifecycleScope.launch {

            try {
//                val headers = HashMap<String, String>()
//                headers["user-agent"] = Urls.USER_AGENT

                val res = ApiClient.getClient().getMediaData3(embedUrl)

                val html = res.body()!!.string()
                val doc: Document = Jsoup.parse(html)
                //1.先尝试从data里面获取
                val scripts = doc.getElementsByTag("script")
                for (script in scripts) {

                    if (script.data().contains("gql_data") && script.data()
                            .contains("shortcode_media")
                    ) {

                        var data = script.data()
                        data = data.replace("\\u0025", "%")
                        data = data.replace("\\", "");
                        data = data.split("\"gql_data\":")[1];
                        data = data.split("}\"}]],")[0]

                        val jsonObject = JsonParser().parse(data).asJsonObject
                        val shortcode_media = jsonObject["shortcode_media"].asJsonObject

                        curMediaInfo = parseMedia(shortcode_media)
                        //caption里面有unicode,没想到好办法转
                        getCaption(doc)

                        //2.如果sidecar里面有视频，通过这种方式会没有videoUrl
                        if (curMediaInfo!!.mediaType == 8) {
                            var hasVideo = false
                            for (res1 in curMediaInfo!!.resources) {
                                if (res1.mediaType == 2) {
                                    hasVideo = true
                                    break
                                }
                            }
                            if (hasVideo) {
                                Analytics.sendEvent("use_a1", "media_type", "GraphSidecar")
                                val myUrl = mBinding.etShortcode.text.toString()
                                getMediaData(myUrl)
                                return@launch
                            }

                        }

                        if (!isInvalidContext()) {
                            progressDialog.dismiss()
                        }

                        showCurrent()

                        mBinding.progressbar.visibility = View.VISIBLE

                        startDownloadService()

                        return@launch
                    }
                }
                //2.如果从数据获取不行，那么尝试从dom元素解析
                val embed = doc.getElementsByClass("Embed")[0]
                val mediatype = embed.attr("data-media-type")
                if (mediatype == "GraphImage") {
                    curMediaInfo = MediaModel()
                    curMediaInfo?.mediaType = 1
                    curMediaInfo?.code = getShortCode() ?: ""
                    parseImage(doc)


                    if (!isInvalidContext()) {
                        progressDialog.dismiss()
                    }

                    showCurrent()

                    mBinding.progressbar.visibility = View.VISIBLE

                    startDownloadService()

                } else {

                    //如果其他情况
                    Analytics.sendEvent("use_a1", "media_type", mediatype)
                    getMediaData(sourceUrl)
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                getMediaData(sourceUrl)
            }

        }

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

                    mBinding.progressbar.visibility = View.VISIBLE

                    startDownloadService()
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
                safeToast(R.string.failed)
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

                    mBinding.progressbar.visibility = View.VISIBLE

                    startDownloadService()
                } else {
                    safeToast(R.string.not_found)
                }

            } catch (e: Exception) {
                mBinding.progressbar.visibility = View.INVISIBLE
                mBinding.progressbar.setValue(0f)
                Log.e(TAG, e.message + "")
                safeToast(R.string.failed)
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

                    mBinding.progressbar.visibility = View.VISIBLE

                    startDownloadService()


                } else {
                    Log.e(TAG, res.errorBody()?.string() + "")
                    safeToast(R.string.failed)
                }


            } catch (e: Exception) {
                Log.e(TAG, e.message + "")
                if (!isInvalidContext()) {
                    progressDialog.dismiss()
                }

                safeToast(R.string.failed)

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
        curMediaInfo?.let {

            mBinding.container.visibility = View.VISIBLE
            mBinding.username.text = it.username
            mBinding.tvCaption.text = it.captionText
            if (it.mediaType != 8) {
                Glide.with(requireContext()).load(it.thumbnailUrl)
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
                if (it.resources.size > 0) {
                    Glide.with(requireContext()).load(it.resources[0].thumbnailUrl)
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

            Glide.with(requireContext()).load(it.profilePicUrl).circleCrop()
                .placeholder(
                    ColorDrawable(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.gray_1
                        )
                    )
                )
                .into(mBinding.avatar)
        }

    }


    @Subscribe
    fun onKeywordReceive(intentEvent: IntentEvent) {

        val keyword = intentEvent.str
        mBinding.etShortcode.setText(keyword)
        autoStart()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQ && resultCode == RESULT_OK) {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.v(TAG, "result")
        if (requestCode == PERMISSION_REQ) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    myAlert.show()
                    return
                }
            }
        }


    }


    private fun startDownloadService() {

        val intent = Intent(requireContext(), MyService::class.java)
        intent.putExtra("url", mBinding.etShortcode.text.toString())
        intent.putExtra("data", gson.toJson(curMediaInfo))
        intent.putExtra("receiver", 1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }

    }


    @Subscribe
    fun onDownloadSuccess(downloadSuccess: DownloadSuccess) {
        if (downloadSuccess.receiver == 1) {
            mBinding.progressbar.visibility = View.INVISIBLE
            mBinding.progressbar.setValue(0f)
        }
    }

    @Subscribe
    fun onDownloadFail(downloadFail: DownloadFail) {
        if (downloadFail.receiver == 1) {
            mBinding.progressbar.visibility = View.INVISIBLE
            mBinding.progressbar.setValue(0f)
        }
    }

    @Subscribe
    fun onDownloadProgress(downloadProgress: DownloadProgress) {
        if (downloadProgress.receiver == 1) {
            mBinding.progressbar.setValue(downloadProgress.progress)
        }
    }


}