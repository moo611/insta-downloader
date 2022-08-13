package com.igtools.igdownloader.api.retrofit

import com.igtools.igdownloader.api.okhttp.Urls
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

object ApiClient {

    fun getClient(): ApiService {

        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
        builder.connectTimeout(60, TimeUnit.SECONDS)
        builder.writeTimeout(60, TimeUnit.SECONDS)
        builder.readTimeout(60, TimeUnit.SECONDS)

        val client = builder.build()

        return Retrofit.Builder().baseUrl(Urls.BASE_URL_PY)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiService::class.java)
    }


}