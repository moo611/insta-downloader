package com.igtools.igdownloader.api.retrofit

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.igtools.igdownloader.BaseApplication
import com.igtools.igdownloader.api.okhttp.Urls
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

object ApiClient {
    val TAG = "ApiClient"
    fun getClient(): ApiService {

        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        builder.connectTimeout(60, TimeUnit.SECONDS)
        builder.writeTimeout(60, TimeUnit.SECONDS)
        builder.readTimeout(60, TimeUnit.SECONDS)

        val client = builder.build()
        BaseApplication.baseUrl = Urls.BASE_URL_PY
        Log.v(TAG,BaseApplication.baseUrl)
        return Retrofit.Builder().baseUrl(BaseApplication.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiService::class.java)
    }


}