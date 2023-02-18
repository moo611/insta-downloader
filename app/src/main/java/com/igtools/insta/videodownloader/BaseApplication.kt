package com.igtools.insta.videodownloader

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.gson.Gson
import com.igtools.insta.videodownloader.utils.ShareUtils

class BaseApplication : Application() {
    val TAG = "BaseApplication"
    val gson = Gson()

    companion object {
        lateinit var mContext: Context
        val folderName = "igtools-downloader"

        //var baseUrl = "http://34.221.49.20:3000"
        var serverIp = "http://54.190.47.148"
        var port1 = "3000"
        var port2 = "4000"
        var port3 = "5000"
        var APIKEY = "253e2765ab6b44c58a069af2621e5b30bc344db7f3b"
        var cookie: String? = null

        var firstLogin = true
        var showRating = true
        fun clear() {
            cookie = null
        }
    }

    override fun onCreate() {
        super.onCreate()

        mContext = applicationContext

        MobileAds.initialize(this)

//        ShareUtils.getData("apikey")?.let {
//            APIKEY = it
//        }
        ShareUtils.getData("cookie").let {
            cookie = it
        }

        ShareUtils.getDataBool("firstLogin").let {
            firstLogin = it
        }

        ShareUtils.getDataBool("showrating").let {
            showRating = it
        }
    }


}