package com.igtools.igdownloader

import android.app.Application
import android.content.Context

class BaseApplication : Application() {
    companion object {
        lateinit var mContext: Context

        var isAuto = true

    }

    override fun onCreate() {
        super.onCreate()

        mContext = applicationContext

    }


}