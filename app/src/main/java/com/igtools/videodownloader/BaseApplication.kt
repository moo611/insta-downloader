package com.igtools.videodownloader

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds

class BaseApplication : Application() {
    val TAG = "BaseApplication"

    companion object {
        lateinit var mContext: Context


    }

    override fun onCreate() {
        super.onCreate()

        mContext = applicationContext

        MobileAds.initialize(this)
    }


}