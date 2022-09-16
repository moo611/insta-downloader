package com.igtools.videodownloader.utils

import android.content.SharedPreferences
import com.igtools.videodownloader.BaseApplication


object ShareUtils {

    private val sp:SharedPreferences = BaseApplication.mContext.getSharedPreferences("ig-downloader",0);

    fun getEdit():SharedPreferences.Editor{
        return sp.edit()
    }

    fun getData(key:String):String?{

        return sp.getString(key,null)
    }

    fun putData(key: String,value:String){
        sp.edit().putString(key, value).apply()
    }

}