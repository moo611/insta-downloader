package com.igtools.igdownloader.utils

import android.content.SharedPreferences
import com.igtools.igdownloader.BaseApplication


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