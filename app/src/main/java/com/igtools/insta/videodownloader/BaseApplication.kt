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
        var showRating = true

    }

    override fun onCreate() {
        super.onCreate()

        mContext = applicationContext


        ShareUtils.getDataString("cookie")?.let {
            cookie = it
        }

        ShareUtils.getDataBool("show_rating").let {
            showRating = it
        }
    }


}