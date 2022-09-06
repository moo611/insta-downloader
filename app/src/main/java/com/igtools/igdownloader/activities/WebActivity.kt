package com.igtools.igdownloader.activities

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.igtools.igdownloader.R
import com.igtools.igdownloader.databinding.ActivityWebBinding
import com.igtools.igdownloader.utils.ShareUtils

class WebActivity : AppCompatActivity() {

    lateinit var binding: ActivityWebBinding
    val TAG="WebActivity"
    var url:String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //沉浸式状态栏
        if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.statusBarColor = Color.TRANSPARENT
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_web)

        webViewSetting()

        url = intent.extras?.getString("url")
        url?.let {
            binding.webview.loadUrl(it)

        }



    }

    @SuppressLint("JavascriptInterface")
    private fun webViewSetting() {

        binding.webview.addJavascriptInterface(this, "webview")
        val settings = binding.webview.settings
        settings.javaScriptEnabled = true

        settings.setDomStorageEnabled(true)
        settings.setAppCacheMaxSize(1024 * 1024 * 8)
        val appCachePath: String = getCacheDir().getAbsolutePath()
        settings.setAppCachePath(appCachePath)
        settings.setAllowFileAccess(true)
        settings.setAppCacheEnabled(true)

//        if (ShareUtils.getData("user-agent")==null){
//            ShareUtils.putData("user-agent",settings.userAgentString)
//        }

        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Log.v("HomeFragment", newProgress.toString())
                binding.progress.progress = newProgress
                if (newProgress == 100) {

                    binding.progress.visibility = View.INVISIBLE
                } else {
                    binding.progress.visibility = View.VISIBLE
                }
            }

        }


        binding.webview.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.webview.visibility = View.VISIBLE

                val cookieManager = CookieManager.getInstance()
                val cookie = cookieManager.getCookie(url)

                Log.v(TAG,cookie+"")
                if (cookie!=null && cookie.contains("sessionid")){
                    ShareUtils.putData("cookie",cookie)
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
                        binding.webview.visibility = View.GONE
                        //binding.imgEmpty.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}