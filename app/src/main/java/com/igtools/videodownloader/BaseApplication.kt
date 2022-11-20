package com.igtools.videodownloader

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.gson.Gson
import com.igtools.videodownloader.api.retrofit.MyConfig
import com.igtools.videodownloader.api.retrofit.MyCookie
import com.igtools.videodownloader.utils.ShareUtils

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
        var APIKEY = ""
        var cookie: String? = null

        //ui
        var tagUpdate = true
        var userUpdate = true
        var autodownload = true
        var firstLogin = true
        fun clear() {
            cookie = null
        }
    }

    override fun onCreate() {
        super.onCreate()

        mContext = applicationContext

        MobileAds.initialize(this)

        ShareUtils.getData("apikey")?.let {
            APIKEY = it
        }
        ShareUtils.getData("cookie").let {
            cookie = it
        }
        ShareUtils.getDataBool("tag-update").let {
            tagUpdate = it
        }
        ShareUtils.getDataBool("user-update").let {
            userUpdate = it
        }
        ShareUtils.getDataBool("firstLogin").let {
            firstLogin = it
        }
        ShareUtils.getDataBool("autodownload").let {
            autodownload = it
        }
    }


}