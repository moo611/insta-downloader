package com.igtools.igdownloader

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

class BaseApplication : Application() {
    val TAG = "BaseApplication"

    companion object {
        lateinit var mContext: Context

        //var baseUrl = "http://34.221.49.20:3000"
        var serverIp = "http://34.221.49.20"
        var port1 = "3000"
        var port2 = "4000"
        var port3 = "5000"
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
                Log.v(TAG,"task success")
                serverIp = remoteConfig.getString("server_ip")
                port1 = remoteConfig.getString("port1")
                port2 = remoteConfig.getString("port2")
                port3 = remoteConfig.getString("port3")
                Log.v(TAG, serverIp)
                Log.v(TAG, port1)
                Log.v(TAG, port2)
                Log.v(TAG, port3)
            } else {
                Log.e(TAG, "fetch failed")
            }

        }
        MobileAds.initialize(this)
    }


}