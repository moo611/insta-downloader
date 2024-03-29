package com.igtools.insta.videodownloader.views.home

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.igtools.insta.videodownloader.BaseApplication
import com.igtools.insta.videodownloader.BuildConfig
import com.igtools.insta.videodownloader.R
import com.igtools.insta.videodownloader.api.ApiClient
import com.igtools.insta.videodownloader.api.Urls
import com.igtools.insta.videodownloader.base.BaseFragment
import com.igtools.insta.videodownloader.databinding.FragmentLinkBinding
import com.igtools.insta.videodownloader.db.RecordDB
import com.igtools.insta.videodownloader.models.MediaModel
import com.igtools.insta.videodownloader.parser.LinkParser
import com.igtools.insta.videodownloader.parser.StoryParser
import com.igtools.insta.videodownloader.utils.KeyboardUtils
import com.igtools.insta.videodownloader.utils.PermissionUtils
import com.igtools.insta.videodownloader.utils.UrlUtils
import com.igtools.insta.videodownloader.views.details.DetailsActivity
import com.igtools.insta.videodownloader.views.web.WebActivity
import kotlinx.coroutines.launch


class LinkFragment : BaseFragment<FragmentLinkBinding>() {

    val TAG = "NewShortCodeFragment"

    lateinit var progressDialog: ProgressDialog
    lateinit var privateDialog: AlertDialog
    lateinit var storyDialog: AlertDialog

    //var mInterstitialAd: InterstitialAd? = null
    var curMediaInfo: MediaModel? = null

    private val linkParser = LinkParser()
    private val storyParser = StoryParser()
    private val LOGIN_REQ = 1000

    lateinit var myAlert: AlertDialog
    private val PERMISSION_REQ = 1024
    override fun getLayoutId(): Int {
        return R.layout.fragment_link
    }

    override fun initView() {

        initDialog()

        mBinding.btnDownload.setOnClickListener {

            autoStart()

        }

        mBinding.btnPaste.setOnClickListener {
            handleCopy()
        }



        mBinding.etLink.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

                if (mBinding.etLink.text.isNotEmpty()) {
                    mBinding.imgClear.visibility = View.VISIBLE
                } else {
                    mBinding.imgClear.visibility = View.INVISIBLE
                }

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        mBinding.imgClear.setOnClickListener {
            mBinding.etLink.setText("")
        }


        mBinding.imgCamera.setOnClickListener {

            val launchIntent =
                requireActivity().packageManager.getLaunchIntentForPackage("com.instagram.android")
            launchIntent?.let { startActivity(it) }
        }

    }


    override fun initData() {
        //mBinding.webview.loadUrl(sideUrl)
        mBinding.etLink.clearFocus()
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
        builder.setPositiveButton(
            R.string.ok
        ) { _, _ ->
            val url = "https://www.instagram.com/accounts/login"
            startActivityForResult(
                Intent(requireContext(), WebActivity::class.java).putExtra(
                    "url",
                    url
                ), LOGIN_REQ
            )
            storyDialog.dismiss()
        };
        //设置反面按钮
        builder.setNegativeButton(
            R.string.cancel
        ) { _, _ -> storyDialog.dismiss() };
        storyDialog = builder.create()


        val builder2 = AlertDialog.Builder(requireContext());
        builder2.setTitle(R.string.login);
        builder2.setMessage(R.string.long_text2);
        builder2.setIcon(R.mipmap.icon);
        //点击对话框以外的区域是否让对话框消失
        builder2.setCancelable(true);
        //设置正面按钮
        builder2.setPositiveButton(
            R.string.ok
        ) { _, _ ->
            val url = "https://www.instagram.com/accounts/login"
            startActivityForResult(
                Intent(requireContext(), WebActivity::class.java).putExtra(
                    "url",
                    url
                ), LOGIN_REQ
            )
            privateDialog.dismiss()
        };
        //设置反面按钮
        builder2.setNegativeButton(
            R.string.cancel
        ) { _, _ -> privateDialog.dismiss() };
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


    private fun handleCopy() {
        mBinding.btnPaste.post {
            val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.let {
                //fix null pointer
                mBinding.etLink.setText(it.text)
            }

        }

    }

    /**
     * 自动开始处理链接的功能。
     * 首先检查输入的链接是否为空，然后检查应用是否有读取和访问权限。
     * 如果链接有效且所需权限已授予，根据链接的类型（Instagram帖子、Instagram故事或其他）来处理和展示相应的媒体内容。
     */
    private fun autoStart() {
        val paramString = mBinding.etLink.text.toString()
        if (TextUtils.isEmpty(paramString)) {
            return
        }

        //permission check first
        if (!PermissionUtils.checkPermissionsForReadAndRight(requireActivity())) {
            PermissionUtils.requirePermissionsInFragment(this, PERMISSION_REQ)
            return
        }

        mBinding.etLink.clearFocus()
        mBinding.flParent.requestFocus()
        KeyboardUtils.closeKeybord(mBinding.etLink, context)

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
                getMediaData(url)

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

    /**
     * 通过给定的嵌入式URL获取媒体数据。
     * 该函数是挂起的，意味着它可以在协程中调用，不会阻塞UI线程。
     *
     * @param embedUrl 嵌入式URL，用于从服务器获取媒体数据。
     */
    private suspend fun getMediaData(embedUrl: String) {

        progressDialog.show()

        try {

            val res = ApiClient.getClient().getMediaData(embedUrl)

            val code = res.code()
            if (code == 200 && res.body() != null) {
                val html = res.body()!!.string()
                curMediaInfo = linkParser.parse(html)
                progressDialog.dismiss()
                jumpToDetails()
            } else if (code == 404) {

                if (BaseApplication.cookie == null) {
                    progressDialog.dismiss()
                    storyDialog.show()
                } else {
                    getMediaDataByCookie()
                }

            }

        } catch (e: Exception) {
            Log.e(TAG, e.message + "")

        }

    }

    /**
     * 使用cookie获取媒体数据的异步函数。
     * 该函数首先检查进度对话框是否正在显示，如果没有则显示它。
     * 然后构造一个包含cookie和用户代理的请求头，以及一个包含shortcode的查询参数，
     * 向服务器发送GET请求以获取媒体数据。成功收到响应后，解析响应数据，
     * 并根据解析结果跳转到详情页面或显示失败提示。
     */
    private suspend fun getMediaDataByCookie() {
        if (!progressDialog.isShowing) {
            progressDialog.show()
        }

        //检查是否已存在

        try {
            val map: HashMap<String, String> = HashMap()
            map["Cookie"] = BaseApplication.cookie!!
            map["User-Agent"] = Urls.USER_AGENT

            val map2: HashMap<String, String> = HashMap()
            val shortCode = getShortCode() ?: return
            map2["shortcode"] = shortCode

            val res = ApiClient.getClient()
                .getMediaData(
                    Urls.PUBLIC_API + "/graphql/query",
                    map,
                    Urls.QUERY_HASH,
                    gson.toJson(map2)
                )

            progressDialog.dismiss()


            val jsonObject = res.body()
            //Log.v(TAG, jsonObject.toString())
            if (res.code() == 200 && jsonObject != null) {
                val shortCodeMedia =
                    jsonObject["data"].asJsonObject["shortcode_media"].asJsonObject
                curMediaInfo = linkParser.parse(shortCodeMedia)
                jumpToDetails()

            } else {
                safeToast(R.string.failed)
            }

        } catch (e: Exception) {

            Log.e(TAG, e.message + "")
            safeToast(R.string.failed)
            progressDialog.dismiss()


        }


    }

    /**
     * 悬挂式获取故事数据的方法。
     * 该方法会尝试从服务器获取特定故事的信息，并处理获取结果。
     * 无参数和返回值，但会更新当前媒体信息（curMediaInfo）并可能跳转到详情页。
     */
    private suspend fun getStoryData() {

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
                curMediaInfo = storyParser.parse(jsonObject)
                jumpToDetails()

            } else {
                Log.e(TAG, res.errorBody()?.string() + "")
                safeToast(R.string.failed)
            }


        } catch (e: Exception) {
            Log.e(TAG, e.message + "")

            progressDialog.dismiss()
            safeToast(R.string.failed)

        }


    }


    private fun jumpToDetails() {
        if (curMediaInfo != null && mBinding.etLink.text.isNotEmpty()) {
            startActivity(
                Intent(requireContext(), DetailsActivity::class.java)
                    .putExtra("content", gson.toJson(curMediaInfo))
                    .putExtra("url", mBinding.etLink.text.toString())

            )

            curMediaInfo = null
            mBinding.etLink.setText("")
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQ && resultCode == RESULT_OK) {
            val url = mBinding.etLink.text.toString()

            lifecycleScope.launch {
                if (url.contains("stories")) {
                    getStoryData()
                } else {
                    getMediaDataByCookie()
                }

            }


        }
    }

    //method part
    private fun emBedUrl(): String {

        val url = mBinding.etLink.text.toString().split("?")[0]
        return url + "embed/captioned/"
    }

    private fun getShortCode(): String? {
        val shortCode: String?
        val url = mBinding.etLink.text.toString()
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



}