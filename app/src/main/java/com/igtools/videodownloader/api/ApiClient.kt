package com.igtools.videodownloader.api

import android.util.Log
import com.igtools.videodownloader.BaseApplication
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ApiClient {
    val TAG = "ApiClient"

    const val proxyPort = 16732 //your proxy port

    const val proxyHost = "170.106.148.245"
//    const val username = "moo611_area-RU_city-moscow"
//    const val password = "1234"

    fun getClient(): UrlService {

        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        builder.followRedirects(false)
        builder.followSslRedirects(false)
        builder.connectTimeout(30, TimeUnit.SECONDS)
        builder.writeTimeout(30, TimeUnit.SECONDS)
        builder.readTimeout(30, TimeUnit.SECONDS)

        val client = builder.build()
        val baseUrl = BaseApplication.serverIp+":"+BaseApplication.port1
        Log.v(TAG,baseUrl)
        return Retrofit.Builder().baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(UrlService::class.java)

    }

    fun getClient2(): UserService {

        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        builder.connectTimeout(30, TimeUnit.SECONDS)
        builder.writeTimeout(30, TimeUnit.SECONDS)
        builder.readTimeout(30, TimeUnit.SECONDS)

        val client = builder.build()
        //BaseApplication.baseUrl = Urls.BASE_URL_PY
        val baseUrl = BaseApplication.serverIp + ":" + BaseApplication.port2
        Log.v(TAG, baseUrl)
        return Retrofit.Builder().baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(UserService::class.java)
    }

    fun getClient3(): TagService {

        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        builder.connectTimeout(30, TimeUnit.SECONDS)
        builder.writeTimeout(30, TimeUnit.SECONDS)
        builder.readTimeout(30, TimeUnit.SECONDS)

        val client = builder.build()
        //BaseApplication.baseUrl = Urls.BASE_URL_PY
        val baseUrl = BaseApplication.serverIp + ":" + BaseApplication.port3
        Log.v(TAG, baseUrl)
        return Retrofit.Builder().baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(TagService::class.java)
    }


    fun getClient4(): DownloadService {

        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        builder.connectTimeout(2, TimeUnit.MINUTES)
        builder.writeTimeout(2, TimeUnit.MINUTES)
        builder.readTimeout(2, TimeUnit.MINUTES)

        val client = builder.build()
        //BaseApplication.baseUrl = Urls.BASE_URL_PY
        val baseUrl = BaseApplication.serverIp + ":" + BaseApplication.port3
        Log.v(TAG, baseUrl)
        return Retrofit.Builder().baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(DownloadService::class.java)
    }


}