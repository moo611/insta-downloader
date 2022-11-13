package com.igtools.videodownloader.service.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.fagaia.farm.base.BaseActivity
import com.igtools.videodownloader.BaseApplication
import com.igtools.videodownloader.R
import com.igtools.videodownloader.api.okhttp.Urls
import com.igtools.videodownloader.databinding.ActivityWebBinding
import com.igtools.videodownloader.utils.ShareUtils

class WebActivity : BaseActivity<ActivityWebBinding>() {

    
    val TAG="WebActivity"
    var url:String?=null

    override fun getLayoutId(): Int {
        return R.layout.activity_web
    }

    override fun initView() {
        webViewSetting()
        mBinding.flBack.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        url = intent.extras?.getString("url")
        url?.let {
            mBinding.webview.loadUrl(it)

        }
    }
    @SuppressLint("JavascriptInterface")
    private fun webViewSetting() {

        mBinding.webview.addJavascriptInterface(this, "webview")
        val settings = mBinding.webview.settings
        settings.javaScriptEnabled = true

        settings.setDomStorageEnabled(true)
        settings.setAppCacheMaxSize(1024 * 1024 * 8)
        val appCachePath: String = getCacheDir().getAbsolutePath()
        settings.setAppCachePath(appCachePath)
        settings.setAllowFileAccess(true)
        settings.setAppCacheEnabled(true)
        settings.userAgentString = Urls.USER_AGENT
//        if (ShareUtils.getData("user-agent")==null){
//            ShareUtils.putData("user-agent",settings.userAgentString)
//        }

        mBinding.webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Log.v("HomeFragment", newProgress.toString())
                mBinding.progress.progress = newProgress
                if (newProgress == 100) {

                    mBinding.progress.visibility = View.INVISIBLE
                } else {
                    mBinding.progress.visibility = View.VISIBLE
                }
            }

        }


        mBinding.webview.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                mBinding.webview.visibility = View.VISIBLE

                val cookieManager = CookieManager.getInstance()
                val cookie = cookieManager.getCookie(url)

                Log.v(TAG,cookie+"")
                if (cookie!=null && cookie.contains("sessionid")){
                    ShareUtils.putData("cookie",cookie)
                    BaseApplication.cookie = cookie
                    setResult(200)
                    finish()
                }

            }


            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val statusCode = error!!.errorCode
                    Log.v(TAG, statusCode.toString())
//                    System.out.println("onReceivedHttpError code = " + statusCode);
                    if (statusCode == -2) {
                        mBinding.webview.visibility = View.GONE
                        //mBinding.imgEmpty.visibility = View.VISIBLE
                    }
                }
            }
        }
    }


}