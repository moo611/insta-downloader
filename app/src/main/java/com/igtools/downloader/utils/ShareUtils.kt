package com.igtools.downloader.utils

import android.content.SharedPreferences
import com.igtools.downloader.BaseApplication


object ShareUtils {

    private val sp:SharedPreferences = BaseApplication.mContext.getSharedPreferences("ig-downloader",0);


    fun getData(key:String):String?{

        return sp.getString(key,null)
    }

    fun putData(key: String,value:String){
        sp.edit().putString(key, value).apply()
    }

}