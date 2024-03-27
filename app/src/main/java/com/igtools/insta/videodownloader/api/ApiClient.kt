package com.igtools.insta.videodownloader.api

import android.util.Log
import com.igtools.insta.videodownloader.BaseApplication
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ApiClient {
    val TAG = "ApiClient"
    val baseUrl = "http://localhost:8080"
    fun getClient(): UrlService {

        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        builder.connectTimeout(60, TimeUnit.SECONDS)
        builder.writeTimeout(60, TimeUnit.SECONDS)
        builder.readTimeout(60, TimeUnit.SECONDS)

        val client = builder.build()
        return Retrofit.Builder().baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(UrlService::class.java)

    }

    fun getClient2(): UserService {

        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        builder.connectTimeout(60, TimeUnit.SECONDS)
        builder.writeTimeout(60, TimeUnit.SECONDS)
        builder.readTimeout(60, TimeUnit.SECONDS)

        val client = builder.build()
        return Retrofit.Builder().baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(UserService::class.java)
    }



}