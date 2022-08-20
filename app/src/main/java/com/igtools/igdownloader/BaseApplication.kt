package com.igtools.igdownloader

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

class BaseApplication : Application() {
    val TAG = "BaseApplication"

    companion object {
        lateinit var mContext: Context

        var isAuto = true
        var baseUrl = ""

    }

    override fun onCreate() {
        super.onCreate()

        mContext = applicationContext
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    baseUrl = remoteConfig.getString("ip")
                    Log.v(TAG, baseUrl)
                } else {
                    Log.e(TAG,"fetch failed")
                }

            }

    }


}