package com.igtools.downloader

import android.app.Application
import android.content.Context

class BaseApplication : Application() {
    companion object {
        lateinit var mContext: Context
    }

    override fun onCreate() {
        super.onCreate()

        mContext = applicationContext

    }
}