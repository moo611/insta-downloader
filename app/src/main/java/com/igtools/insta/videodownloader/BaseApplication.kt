package com.igtools.insta.videodownloader

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.igtools.insta.videodownloader.utils.ShareUtils

class BaseApplication : Application() {
    val TAG = "BaseApplication"
    val gson = Gson()

    companion object {
        lateinit var mContext: Context

        var cookie: String? = null

        var firstLogin = true
        var showRating = true

    }

    override fun onCreate() {
        super.onCreate()

        mContext = applicationContext


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